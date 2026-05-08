#!/usr/bin/env python3
"""
Seed data for purchase k6 tests.

This script prepares:
1. Purchase-test events in MySQL.
2. Login-capable test users through the auth register/login API.
3. ENTRY_TOKEN / ENTRY_EVENT Redis keys required by PurchaseCommandController.

It uses only Python standard library plus local Docker Compose services.
Run from the repository root:

  python3 k6/seed-purchase-test-data.py --users 1000 --events 1

Common overrides:
  BASE_URL=http://localhost:8080 python3 k6/seed-purchase-test-data.py --users 5000
  python3 k6/seed-purchase-test-data.py --event-ids event-a,event-b --users 2000
"""

from __future__ import annotations

import argparse
import base64
import csv
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
DEFAULT_COMPOSE_FILE = os.environ.get("COMPOSE_FILE", "docker/docker-compose.yml")
DEFAULT_MYSQL_SERVICE = os.environ.get("MYSQL_SERVICE", "mysql-master")
DEFAULT_REDIS_SERVICE = os.environ.get("REDIS_SERVICE", "redis")


@dataclass(frozen=True)
class SeedUser:
    index: int
    email: str
    password: str
    user_id: str
    access_token: str
    entry_auth_token: str
    event_id: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed initial data for purchase k6 tests.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Gateway/Auth base URL.")
    parser.add_argument("--users", type=int, default=int(os.environ.get("USERS", "100")))
    parser.add_argument("--events", type=int, default=int(os.environ.get("EVENTS", "1")))
    parser.add_argument(
        "--event-ids",
        default=os.environ.get("EVENT_IDS", ""),
        help="Comma-separated event ids. If omitted, deterministic k6-purchase-event-N ids are used.",
    )
    parser.add_argument("--email-domain", default=os.environ.get("LOGIN_EMAIL_DOMAIN", "example.com"))
    parser.add_argument("--email-prefix", default=os.environ.get("LOGIN_EMAIL_PREFIX", "user"))
    parser.add_argument("--password", default=os.environ.get("PASSWORD", "password123"))
    parser.add_argument("--amount", type=int, default=int(os.environ.get("AMOUNT", "10000")))
    parser.add_argument("--entry-token-prefix", default=os.environ.get("ENTRY_TOKEN_PREFIX", "k6-purchase-entry"))
    parser.add_argument("--redis-ttl-seconds", type=int, default=int(os.environ.get("REDIS_TTL_SECONDS", "7200")))
    parser.add_argument("--compose-file", default=DEFAULT_COMPOSE_FILE)
    parser.add_argument("--mysql-service", default=DEFAULT_MYSQL_SERVICE)
    parser.add_argument("--redis-service", default=DEFAULT_REDIS_SERVICE)
    parser.add_argument("--mysql-user", default=os.environ.get("MYSQL_USER", "root"))
    parser.add_argument("--mysql-password", default=os.environ.get("MYSQL_PASSWORD", "password"))
    parser.add_argument("--mysql-db", default=os.environ.get("MYSQL_DB", "ticketon"))
    parser.add_argument("--workers", type=int, default=int(os.environ.get("SEED_WORKERS", "20")))
    parser.add_argument(
        "--output",
        default=os.environ.get("OUTPUT", "k6/.generated/purchase-users.csv"),
        help="CSV output path containing email/userId/eventId/entryAuthToken/orderId.",
    )
    parser.add_argument("--skip-mysql", action="store_true", help="Do not seed event rows.")
    parser.add_argument("--skip-users", action="store_true", help="Do not register/login users.")
    parser.add_argument("--skip-redis", action="store_true", help="Do not write entry token Redis keys.")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    if args.users <= 0:
        raise SystemExit("--users must be greater than 0")
    if args.events <= 0 and not args.event_ids:
        raise SystemExit("--events must be greater than 0 when --event-ids is omitted")
    if args.amount <= 0:
        raise SystemExit("--amount must be greater than 0")
    if args.workers <= 0:
        raise SystemExit("--workers must be greater than 0")
    return args


def resolve_event_ids(args: argparse.Namespace) -> list[str]:
    if args.event_ids.strip():
        event_ids = [value.strip() for value in args.event_ids.split(",") if value.strip()]
        if not event_ids:
            raise SystemExit("--event-ids was provided but no valid ids were found")
        return list(dict.fromkeys(event_ids))

    return [f"k6-purchase-event-{idx:03d}" for idx in range(1, args.events + 1)]


def run_compose_exec(
    compose_file: str,
    service: str,
    command: list[str],
    *,
    input_bytes: bytes | None = None,
    check: bool = True,
) -> subprocess.CompletedProcess[bytes]:
    cmd = ["docker", "compose", "-f", compose_file, "exec", "-T", service, *command]
    completed = subprocess.run(
        cmd,
        input=input_bytes,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if check and completed.returncode != 0:
        raise RuntimeError(
            "Command failed: {}\nstdout:\n{}\nstderr:\n{}".format(
                " ".join(cmd),
                completed.stdout.decode(errors="replace"),
                completed.stderr.decode(errors="replace"),
            )
        )
    return completed


def sql_quote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def seed_events(args: argparse.Namespace, event_ids: list[str]) -> None:
    values: list[str] = []
    for event_id in event_ids:
        title = f"K6 Purchase Test Event {event_id}"
        values.append(
            "("
            f"{sql_quote(event_id)},"
            f"{sql_quote(title)},"
            "'https://example.com/k6-purchase.png',"
            "0,"
            "'k6 purchase seed',"
            "'k6 purchase seed event',"
            "DATE_SUB(NOW(), INTERVAL 1 DAY),"
            "DATE_ADD(NOW(), INTERVAL 7 DAY),"
            "DATE_ADD(NOW(), INTERVAL 14 DAY),"
            "DATE_ADD(NOW(), INTERVAL 15 DAY),"
            "0,"
            "false,"
            f"{args.amount},"
            f"{args.amount},"
            "'OPEN',"
            "1,"
            "'k6-manager',"
            "1,"
            "false,"
            "NOW(),"
            "NOW(),"
            "0,"
            "0"
            ")"
        )

    sql = f"""
INSERT INTO event (
  id,
  title,
  thumbnail_url,
  ageLimit,
  restrictions,
  description,
  booking_start,
  booking_end,
  event_start,
  event_end,
  view_count,
  seat_selectable,
  min_price,
  max_price,
  status,
  event_category_id,
  manager_id,
  seat_layout_id,
  deleted,
  created_at,
  modified_at,
  version,
  sales_version
)
VALUES
  {", ".join(values)}
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  thumbnail_url = VALUES(thumbnail_url),
  restrictions = VALUES(restrictions),
  description = VALUES(description),
  booking_start = VALUES(booking_start),
  booking_end = VALUES(booking_end),
  event_start = VALUES(event_start),
  event_end = VALUES(event_end),
  view_count = 0,
  seat_selectable = VALUES(seat_selectable),
  min_price = VALUES(min_price),
  max_price = VALUES(max_price),
  status = 'OPEN',
  event_category_id = VALUES(event_category_id),
  manager_id = VALUES(manager_id),
  seat_layout_id = VALUES(seat_layout_id),
  deleted = false,
  modified_at = NOW(),
  sales_version = 0;
""".strip() + "\n"

    mysql_cmd = [
        "mysql",
        f"-u{args.mysql_user}",
        f"-p{args.mysql_password}",
        args.mysql_db,
    ]
    run_compose_exec(
        args.compose_file,
        args.mysql_service,
        mysql_cmd,
        input_bytes=sql.encode(),
    )


def http_json(method: str, url: str, payload: dict[str, Any] | None = None, timeout: int = 15) -> tuple[int, dict[str, str], str]:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read().decode(errors="replace")
            return response.status, dict(response.headers.items()), body
    except urllib.error.HTTPError as error:
        body = error.read().decode(errors="replace")
        return error.code, dict(error.headers.items()), body


def register_user(base_url: str, index: int, email: str, password: str) -> None:
    payload = {
        "email": email,
        "password": password,
        "name": f"k6-user-{index:06d}",
        "age": 20,
        "sex": "M",
        "phoneNum": f"010{index:08d}"[-11:],
        "location": "SEOUL",
    }
    status, _, body = http_json("POST", f"{base_url}/api/v1/auth/register", payload)
    if status in (200, 201, 202, 409):
        return

    lower_body = body.lower()
    if status in (400, 422) and ("already" in lower_body or "exist" in lower_body or "중복" in body or "존재" in body):
        return

    raise RuntimeError(f"register failed: email={email}, status={status}, body={body[:500]}")


def normalize_bearer(value: str) -> str:
    token = (value or "").strip()
    if token.lower().startswith("bearer "):
        return token[7:].strip()
    return token


def decode_jwt_payload(token: str) -> dict[str, Any]:
    try:
        payload_part = token.split(".")[1]
    except IndexError as exc:
        raise RuntimeError("login response did not contain a JWT access token") from exc

    padded = payload_part + "=" * (-len(payload_part) % 4)
    decoded = base64.urlsafe_b64decode(padded.encode()).decode()
    return json.loads(decoded)


def login_user(base_url: str, email: str, password: str) -> tuple[str, str]:
    status, headers, body = http_json("POST", f"{base_url}/api/v1/auth/login", {"email": email, "password": password})
    if status != 200:
        raise RuntimeError(f"login failed: email={email}, status={status}, body={body[:500]}")

    header_lookup = {key.lower(): value for key, value in headers.items()}
    access_token = normalize_bearer(header_lookup.get("authorization", ""))

    if not access_token:
        try:
            parsed = json.loads(body)
            access_token = normalize_bearer(str(parsed.get("data") or ""))
        except json.JSONDecodeError:
            pass

    if not access_token:
        raise RuntimeError(f"login response did not include an access token: email={email}")

    claims = decode_jwt_payload(access_token)
    user_id = str(claims.get("userId") or "")
    if not user_id:
        raise RuntimeError(f"JWT did not include userId claim: email={email}")
    return access_token, user_id


def prepare_user(args: argparse.Namespace, index: int, event_ids: list[str]) -> SeedUser:
    email = f"{args.email_prefix}{index}@{args.email_domain}"
    event_id = event_ids[(index - 1) % len(event_ids)]

    register_user(args.base_url, index, email, args.password)
    access_token, user_id = login_user(args.base_url, email, args.password)

    entry_auth_token = f"{args.entry_token_prefix}-{index:06d}-{int(time.time())}"
    return SeedUser(
        index=index,
        email=email,
        password=args.password,
        user_id=user_id,
        access_token=access_token,
        entry_auth_token=entry_auth_token,
        event_id=event_id,
    )


def resp_command(*parts: str) -> bytes:
    encoded = [str(part).encode() for part in parts]
    chunks = [f"*{len(encoded)}\r\n".encode()]
    for part in encoded:
        chunks.append(f"${len(part)}\r\n".encode())
        chunks.append(part)
        chunks.append(b"\r\n")
    return b"".join(chunks)


def seed_entry_tokens(args: argparse.Namespace, users: list[SeedUser]) -> None:
    commands = bytearray()
    for user in users:
        token_key = f"ENTRY_TOKEN:{user.user_id}"
        event_key = f"ENTRY_EVENT:{user.user_id}"
        if args.redis_ttl_seconds > 0:
            commands.extend(resp_command("SETEX", token_key, str(args.redis_ttl_seconds), user.entry_auth_token))
            commands.extend(resp_command("SETEX", event_key, str(args.redis_ttl_seconds), user.event_id))
        else:
            commands.extend(resp_command("SET", token_key, user.entry_auth_token))
            commands.extend(resp_command("SET", event_key, user.event_id))

    completed = run_compose_exec(
        args.compose_file,
        args.redis_service,
        ["redis-cli", "--pipe"],
        input_bytes=bytes(commands),
    )
    output = completed.stdout.decode(errors="replace")
    if "errors: 0" not in output:
        raise RuntimeError(f"redis-cli --pipe reported possible errors:\n{output}")


def write_output(args: argparse.Namespace, users: list[SeedUser], event_ids: list[str]) -> None:
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with output_path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "index",
                "email",
                "password",
                "userId",
                "eventId",
                "entryAuthToken",
                "amount",
                "orderId",
                "paymentKey",
                "provider",
            ],
        )
        writer.writeheader()
        for user in users:
            writer.writerow(
                {
                    "index": user.index,
                    "email": user.email,
                    "password": user.password,
                    "userId": user.user_id,
                    "eventId": user.event_id,
                    "entryAuthToken": user.entry_auth_token,
                    "amount": args.amount,
                    "orderId": f"k6-purchase-order-{user.index:06d}",
                    "paymentKey": f"k6-payment-key-{user.index:06d}",
                    "provider": "TOSS",
                }
            )

    env_path = output_path.with_suffix(".env")
    env_path.write_text(
        "\n".join(
            [
                f"EVENT_IDS={','.join(event_ids)}",
                f"AMOUNT={args.amount}",
                f"LOGIN_EMAIL_DOMAIN={args.email_domain}",
                f"USERS={len(users)}",
                f"PURCHASE_SEED_CSV={output_path}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )


def main() -> int:
    args = parse_args()
    event_ids = resolve_event_ids(args)

    print(f"[seed] base_url={args.base_url}")
    print(f"[seed] event_ids={','.join(event_ids)}")
    print(f"[seed] users={args.users}")

    if not args.skip_mysql:
        print("[seed] seeding MySQL event rows...")
        seed_events(args, event_ids)

    users: list[SeedUser] = []
    if not args.skip_users:
        print("[seed] registering/logging in users...")
        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            futures = [executor.submit(prepare_user, args, index, event_ids) for index in range(1, args.users + 1)]
            for future in as_completed(futures):
                user = future.result()
                users.append(user)
                if args.verbose or len(users) % 100 == 0 or len(users) == args.users:
                    print(f"[seed] prepared users: {len(users)}/{args.users}")

        users.sort(key=lambda user: user.index)

    if users and not args.skip_redis:
        print("[seed] writing ENTRY_TOKEN / ENTRY_EVENT Redis keys...")
        seed_entry_tokens(args, users)

    if users:
        write_output(args, users, event_ids)
        print(f"[seed] wrote {args.output}")
        print(f"[seed] wrote {Path(args.output).with_suffix('.env')}")
    else:
        print("[seed] user output skipped because --skip-users was used")

    print("[seed] done")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise SystemExit(130)
    except Exception as exc:
        print(f"[seed] failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
