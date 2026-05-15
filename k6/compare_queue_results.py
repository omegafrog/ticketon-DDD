#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


def load_summary(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as file:
        return json.load(file)


def metric_value(summary: dict, metric_name: str, field: str):
    return summary.get("metrics", {}).get(metric_name, {}).get("values", {}).get(field)


def format_number(value, digits: int = 2) -> str:
    if value is None:
        return "n/a"
    return f"{float(value):.{digits}f}"


def format_delta(queue_value, direct_value, digits: int = 2) -> str:
    if queue_value is None or direct_value is None:
        return "n/a"
    delta = float(queue_value) - float(direct_value)
    sign = "+" if delta >= 0 else ""
    return f"{sign}{delta:.{digits}f}"


def format_ratio(queue_value, direct_value, digits: int = 2) -> str:
    if queue_value is None or direct_value in (None, 0):
        return "n/a"
    return f"{float(queue_value) / float(direct_value):.{digits}f}x"


def print_row(label: str, queue_value, direct_value, digits: int = 2) -> None:
    print(
        f"{label:<28}"
        f" direct={format_number(direct_value, digits):>10}"
        f" queue={format_number(queue_value, digits):>10}"
        f" delta={format_delta(queue_value, direct_value, digits):>10}"
        f" ratio={format_ratio(queue_value, direct_value, digits):>8}"
    )


def main() -> int:
    if len(sys.argv) != 3:
        print(
            "Usage: python3 k6/compare_queue_results.py "
            "<direct-summary.json> <queue-summary.json>",
            file=sys.stderr,
        )
        return 1

    direct_path = Path(sys.argv[1])
    queue_path = Path(sys.argv[2])

    direct = load_summary(str(direct_path))
    queue = load_summary(str(queue_path))

    print("Queue Impact Comparison")
    print(f"Direct summary: {direct_path}")
    print(f"Queue summary : {queue_path}")
    print("")

    print_row(
        "completed_users",
        metric_value(queue, "completed_users", "count"),
        metric_value(direct, "completed_users", "count"),
        0,
    )
    print_row(
        "http_req_duration p95",
        metric_value(queue, "http_req_duration", "p(95)"),
        metric_value(direct, "http_req_duration", "p(95)"),
    )
    print_row(
        "target_req_time_ms p95",
        metric_value(queue, "target_req_time_ms", "p(95)"),
        metric_value(direct, "target_req_time_ms", "p(95)"),
    )
    print_row(
        "time_to_target_ms p95",
        metric_value(queue, "time_to_target_ms", "p(95)"),
        metric_value(direct, "time_to_target_ms", "p(95)"),
    )
    print_row(
        "http_req_failed rate",
        metric_value(queue, "http_req_failed", "rate"),
        metric_value(direct, "http_req_failed", "rate"),
        4,
    )
    print_row(
        "target_success_rate",
        metric_value(queue, "target_success_rate", "rate"),
        metric_value(direct, "target_success_rate", "rate"),
        4,
    )
    print_row(
        "target_requests",
        metric_value(queue, "target_requests", "count"),
        metric_value(direct, "target_requests", "count"),
        0,
    )
    print_row(
        "queue_wait_time_ms p95",
        metric_value(queue, "queue_wait_time_ms", "p(95)"),
        metric_value(direct, "queue_wait_time_ms", "p(95)"),
    )
    print_row(
        "polling_current_time p95",
        metric_value(queue, "polling_current_time_ms", "p(95)"),
        metric_value(direct, "polling_current_time_ms", "p(95)"),
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
