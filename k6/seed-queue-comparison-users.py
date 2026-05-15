#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request


BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
USER_COUNT = int(os.getenv("USER_COUNT", "1000"))
USER_EMAIL_PREFIX = os.getenv("USER_EMAIL_PREFIX", "user")
LOGIN_EMAIL_DOMAIN = os.getenv("LOGIN_EMAIL_DOMAIN", "example.com")
DEFAULT_PASSWORD = os.getenv("DEFAULT_PASSWORD", "password123!")
START_INDEX = int(os.getenv("START_INDEX", "1"))
TIMEOUT_SECONDS = int(os.getenv("HTTP_TIMEOUT_SECONDS", "10"))


def register(index: int) -> tuple[bool, str]:
    email = f"{USER_EMAIL_PREFIX}{index}@{LOGIN_EMAIL_DOMAIN}"
    payload = {
        "email": email,
        "password": DEFAULT_PASSWORD,
        "name": f"K6 User {index}",
        "age": 20,
        "sex": "MALE",
        "phoneNum": f"010-{index // 10000:04d}-{index % 10000:04d}",
        "location": "SEOUL",
    }
    request = urllib.request.Request(
        f"{BASE_URL}/api/v1/auth/register",
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={"Content-Type": "application/json"},
    )

    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return response.status in (200, 201, 202), f"{email} created"
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        lowered = body.lower()
        if error.code in (400, 409) and any(word in lowered for word in ("already", "exists", "이미")):
            return True, f"{email} already exists"
        return False, f"{email} failed: status={error.code} body={body[:300]}"


def main() -> int:
    failures = 0
    for index in range(START_INDEX, START_INDEX + USER_COUNT):
        ok, message = register(index)
        print(message)
        if not ok:
            failures += 1

    if failures:
        print(f"Completed with {failures} failures.", file=sys.stderr)
        return 1

    print(
        f"Seeded {USER_COUNT} users with prefix={USER_EMAIL_PREFIX}, "
        f"domain={LOGIN_EMAIL_DOMAIN}, password={DEFAULT_PASSWORD}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
