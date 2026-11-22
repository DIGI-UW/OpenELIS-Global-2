#!/usr/bin/env python3
"""Verify persisted reporting jobs after an observed local process interruption."""

import argparse
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import ssl
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.csrf = ""
        self.http = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),
            urllib.request.HTTPSHandler(context=ssl._create_unverified_context()),
        )

    def call(self, path, body=None, *, raw=False, form=False):
        data = None
        if body is not None:
            data = urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode()
        request = urllib.request.Request(
            self.base + path,
            data=data,
            headers={
                "Content-Type": "application/x-www-form-urlencoded" if form else "application/json",
                "X-CSRF-Token": self.csrf,
            },
        )
        with self.http.open(request, timeout=10) as response:
            payload = response.read()
            return payload if raw else json.loads(payload)

    def login(self):
        self.call("/ValidateLogin?apiCall=true", {
            "loginName": os.environ.get("TEST_USER", "admin"),
            "password": os.environ.get("TEST_PASS", "adminADMIN!"),
        }, form=True)
        session = self.call("/session")
        assert session["authenticated"], "The fixture user did not authenticate"
        self.csrf = session["csrf"]


def docker(*arguments):
    return subprocess.check_output(["docker", *arguments], text=True).strip()


def inspect(name):
    return json.loads(docker("inspect", name))[0]


def verify(args):
    assert urllib.parse.urlparse(args.base_url).hostname in {"localhost", "127.0.0.1", "::1"}
    evidence = args.interruption.parent
    before = json.loads(args.interruption.read_text())
    assert before["stoppedProcess"]["Running"] is False
    assert before["stoppedProcess"]["ExitCode"] == 137
    assert before["beforeProcess"]["Running"] is True
    app = inspect(args.app_container)
    database = inspect(args.db_container)
    assert app["State"]["Running"] and database["State"]["Running"]
    assert app["State"]["StartedAt"] > before["stoppedProcess"]["FinishedAt"]
    assert database["Id"] == before["databaseContainer"], "The database was replaced"
    assert app["Config"]["Labels"]["com.docker.compose.project"] == args.project
    assert database["Config"]["Labels"]["com.docker.compose.project"] == args.project
    client = Client(args.base_url)
    deadline = time.monotonic() + args.timeout
    while True:
        state = inspect(args.app_container)
        assert state["Id"] == app["Id"] and state["State"]["Running"], "Application stopped or changed"
        try:
            if "authenticated" in client.call("/session"):
                break
        except (OSError, ValueError):
            pass
        if time.monotonic() >= deadline:
            raise TimeoutError("Readiness observation expired; inspect the same container before resuming")
        time.sleep(2)
    client.login()
    prefix = "/rest/reports/data-export/jobs/"
    baseline = client.call(prefix + before["baseline"]["id"] + "/download", raw=True)
    assert baseline == (evidence / "baseline.csv").read_bytes(), "Previously completed CSV changed"
    expected = {
        job["id"]: "FAILED" if job["state"] == "GENERATING" else "READY"
        for job in before["jobs"]
    }
    assert "FAILED" in expected.values() and "READY" in expected.values()
    observations = []
    while True:
        jobs = [client.call(prefix + job["id"]) for job in before["jobs"]]
        observations.append({"at": time.time(), "states": {j["id"]: j["state"] for j in jobs}})
        for job in jobs:
            original = next(j for j in before["jobs"] if j["id"] == job["id"])
            assert job["request"] == original["request"], "A frozen request changed during restart"
            if job["state"] != "READY":
                try:
                    client.call(prefix + job["id"] + "/download", raw=True)
                except urllib.error.HTTPError as error:
                    assert error.code == 409, (job["id"], error.code)
                else:
                    raise AssertionError("Incomplete output was downloadable")
        if all(job["state"] == expected[job["id"]] for job in jobs):
            break
        assert time.monotonic() < deadline, "Recovery observation expired; do not restart without inspecting state"
        time.sleep(2)
    for job in jobs:
        if job["state"] == "FAILED":
            assert job["failureCode"] == "reporting.job.interrupted"
        else:
            queued_csv = client.call(prefix + job["id"] + "/download", raw=True)
            assert queued_csv == baseline and job["rowCount"] == 2
            (evidence / "queued-after-restart.csv").write_bytes(queued_csv)
    for path in before["partialPaths"]:
        docker("exec", args.app_container, "test", "!", "-e", path)
    failed = next(job for job in jobs if job["state"] == "FAILED")
    retried = client.call(prefix + failed["id"] + "/retry", {"clientRequestId": "restart-retry-" + uuid.uuid4().hex})
    assert retried["parentId"] == failed["id"] and retried["request"] == failed["request"]
    while retried["state"] in {"QUEUED", "GENERATING"}:
        assert time.monotonic() < deadline, "Retry did not finish within the observation window"
        time.sleep(.5)
        retried = client.call(prefix + retried["id"])
    assert retried["state"] == "READY"
    retry_csv = client.call(prefix + retried["id"] + "/download", raw=True)
    assert retry_csv == baseline
    (evidence / "retry-after-restart.csv").write_bytes(retry_csv)
    result = {
        "applicationContainer": app["Id"],
        "applicationStartedAt": app["State"]["StartedAt"],
        "databaseContainerRetained": database["Id"],
        "baselineJobId": before["baseline"]["id"],
        "baselineCsvSha256": hashlib.sha256(baseline).hexdigest(),
        "recoveredJobs": jobs,
        "retry": retried,
        "observations": observations,
        "partialFilesRemoved": before["partialPaths"],
        "status": "passed",
    }
    (evidence / "verified.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": "passed", "states": expected, "retryId": retried["id"], "evidence": str(evidence)}), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("interruption", type=Path)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--app-container", required=True)
    parser.add_argument("--db-container", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--timeout", type=int, default=600)
    verify(parser.parse_args())
