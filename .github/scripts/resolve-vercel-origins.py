#!/usr/bin/env python3
import json
import os
import sys
import urllib.parse
import urllib.request


def request_json(path, token, team_id="", params=None):
    query = dict(params or {})
    if team_id:
        query["teamId"] = team_id

    url = f"https://api.vercel.com{path}"
    if query:
        url = f"{url}?{urllib.parse.urlencode(query)}"

    req = urllib.request.Request(url, headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
    })
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response)


def normalize_origin(domain):
    if not domain:
        return None
    domain = domain.strip().rstrip("/")
    if not domain:
        return None
    if domain.startswith("http://") or domain.startswith("https://"):
        return domain
    return f"https://{domain}"


def main():
    token = os.environ.get("VERCEL_TOKEN", "").strip()
    project = os.environ.get("VERCEL_PROJECT_ID", "").strip() or os.environ.get("VERCEL_PROJECT_NAME", "").strip()
    team_id = os.environ.get("VERCEL_TEAM_ID", "").strip()

    if not token or not project:
        print("")
        return 0

    origins = []

    try:
        domains = request_json(f"/v9/projects/{urllib.parse.quote(project, safe='')}/domains", token, team_id)
        for domain in domains.get("domains", []):
            origin = normalize_origin(domain.get("name") or domain.get("domain"))
            if origin and origin not in origins:
                origins.append(origin)
    except Exception as exc:
        print(f"warning: failed to read Vercel project domains: {exc}", file=sys.stderr)

    try:
        deployments = request_json(
            "/v6/deployments",
            token,
            team_id,
            {"projectId": project, "target": "production", "limit": "1"},
        )
        for deployment in deployments.get("deployments", []):
            origin = normalize_origin(deployment.get("url"))
            if origin and origin not in origins:
                origins.append(origin)
    except Exception as exc:
        print(f"warning: failed to read Vercel production deployment: {exc}", file=sys.stderr)

    print(",".join(origins))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
