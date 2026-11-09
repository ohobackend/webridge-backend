#!/usr/bin/env python3
"""
Python vs Java API Staging Response Parity Comparison Tool
Usage:
    python scripts/compare_api.py --py http://localhost:8000 --java http://localhost:8080 --token <jwt_token>
"""

import argparse
import json
import sys
import urllib.request
import urllib.error


def make_request(base_url, path, method="GET", headers=None, data=None):
    url = f"{base_url}{path}"
    req_headers = {"User-Agent": "ParityCheck/1.0"}
    if headers:
        req_headers.update(headers)

    body_bytes = None
    if data:
        req_headers["Content-Type"] = "application/json"
        body_bytes = json.dumps(data).encode("utf-8")

    req = urllib.request.Request(url, data=body_bytes, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            content = resp.read().decode("utf-8")
            return resp.status, content
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8") if e.fp else ""
        return e.code, content
    except Exception as e:
        return "CONN_ERR", str(e)


def normalize_json(raw_text):
    try:
        data = json.loads(raw_text)
        # Sort keys recursively
        return json.dumps(data, sort_keys=True, indent=2)
    except Exception:
        return raw_text


def compare_endpoint(name, method, path, py_url, java_url, headers=None, data=None):
    print(f"Testing [{method}] {path} ({name})... ", end="", flush=True)

    py_status, py_body = make_request(py_url, path, method, headers, data)
    java_status, java_body = make_request(java_url, path, method, headers, data)

    if py_status == java_status and java_status != "CONN_ERR":
        print(f"\033[92m[PASS] (Status: {java_status})\033[0m")
        return True
    else:
        print(f"\033[91m[FAIL] (Python: {py_status}, Java: {java_status})\033[0m")
        return False


def main():
    parser = argparse.ArgumentParser(description="WeBridge API Parity Comparison")
    parser.add_argument("--py", default="http://localhost:8000", help="Django base URL")
    parser.add_argument("--java", default="http://localhost:8080", help="Spring Boot base URL")
    parser.add_argument("--token", default="", help="JWT Bearer token for protected endpoints")
    args = parser.parse_args()

    print("=" * 60)
    print(" WeBridge API Parity Comparison (Python vs Java)")
    print("=" * 60)
    print(f"Django: {args.py}")
    print(f"Java:   {args.java}\n")

    endpoints = [
        ("Ping", "GET", "/api/v1/ping/"),
        ("Health", "GET", "/api/v1/health/"),
        ("Current Terms", "GET", "/api/v1/terms/current/"),
    ]

    passed = 0
    failed = 0

    for name, method, path in endpoints:
        ok = compare_endpoint(name, method, path, args.py, args.java)
        if ok:
            passed += 1
        else:
            failed += 1

    if args.token:
        auth_headers = {"Authorization": f"Bearer {args.token}"}
        auth_endpoints = [
            ("User Profile", "GET", "/api/v1/user/me/"),
            ("Feedback List", "GET", "/api/v1/feedback/"),
            ("Scan List", "GET", "/api/v1/scans/"),
        ]
        for name, method, path in auth_endpoints:
            ok = compare_endpoint(name, method, path, args.py, args.java, headers=auth_headers)
            if ok:
                passed += 1
            else:
                failed += 1
    else:
        print("\n\033[93m[Notice] Pass --token <jwt_token> to test authenticated endpoints.\033[0m")

    print("\n" + "=" * 60)
    print(f" Summary: Passed={passed}, Failed={failed}")
    print("=" * 60)


if __name__ == "__main__":
    main()
