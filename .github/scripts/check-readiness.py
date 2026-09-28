#!/usr/bin/env python3
"""Require a real HTTP 200 JSON health response before accepting a deployment."""

import argparse
import json
import math
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, url):
        return None


def validate_contract(url, timeout=300, interval=5, request_timeout=10, key="status", **_options):
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("Readiness URL must be HTTP(S) without embedded credentials")
    if not key or any(not part for part in key.split(".")):
        raise ValueError("Readiness requires a JSON field name")
    if any(not math.isfinite(value) or value <= 0 for value in (timeout, interval, request_timeout)):
        raise ValueError("Readiness timeout and polling intervals must be positive")


def probe(url, key="status", expected="UP", insecure=False, timeout=10):
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("Readiness URL must be HTTP(S) without embedded credentials")
    context = ssl._create_unverified_context() if insecure else ssl.create_default_context()
    opener = urllib.request.build_opener(NoRedirect(), urllib.request.HTTPSHandler(context=context))
    try:
        request = urllib.request.Request(url, headers={"Accept": "application/json"})
        with opener.open(request, timeout=timeout) as response:
            if response.status != 200:
                return False, f"HTTP {response.status}"
            content_type = response.headers.get_content_type()
            if content_type != "application/json" and not content_type.endswith("+json"):
                return False, f"Expected JSON, received {content_type}"
            body = response.read(65537)
            if len(body) > 65536:
                return False, "Health response exceeds 64 KiB"
            value = json.loads(body)
            for part in key.split("."):
                if not isinstance(value, dict) or part not in value:
                    return False, f"Missing health field: {key}"
                value = value[part]
            if type(value) is not type(expected) or value != expected:
                return False, f"Health field {key} does not equal the expected value"
            return True, "HTTP 200 and expected JSON health value"
    except urllib.error.HTTPError as error:
        return False, f"HTTP {error.code}"
    except (urllib.error.URLError, TimeoutError, OSError, ValueError) as error:
        return False, type(error).__name__


def wait_until_ready(url, timeout=300, interval=5, request_timeout=10, key="status", expected="UP",
                     insecure=False, clock=time.monotonic, sleep=time.sleep, check=probe):
    validate_contract(url, timeout, interval, request_timeout, key)
    deadline = clock() + timeout
    attempts = 0
    reason = "No response before deadline"
    while clock() < deadline:
        attempts += 1
        ready, reason = check(url, key, expected, insecure, min(request_timeout, deadline - clock()))
        print(f"Readiness attempt {attempts}: {reason}", flush=True)
        if ready:
            return {"ready": True, "attempts": attempts, "reason": reason}
        remaining = deadline - clock()
        if remaining > 0:
            sleep(min(interval, remaining))
    return {"ready": False, "attempts": attempts, "reason": reason}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", required=True)
    parser.add_argument("--timeout", type=float, default=300)
    parser.add_argument("--interval", type=float, default=5)
    parser.add_argument("--request-timeout", type=float, default=10)
    parser.add_argument("--json-key", default="status")
    parser.add_argument("--expected-value", default='"UP"', help="Expected value encoded as JSON")
    parser.add_argument("--insecure", action="store_true", help="Explicitly allow a self-signed TLS certificate")
    parser.add_argument("--output", help="Write the readiness outcome as JSON")
    args = parser.parse_args()
    try:
        expected = json.loads(args.expected_value)
        report = wait_until_ready(args.url, args.timeout, args.interval, args.request_timeout,
                                  args.json_key, expected, args.insecure)
    except ValueError as error:
        parser.error(str(error))
    if args.output:
        with open(args.output, "w", encoding="utf-8") as output:
            json.dump(report, output)
    return 0 if report["ready"] else 1


if __name__ == "__main__":
    sys.exit(main())
