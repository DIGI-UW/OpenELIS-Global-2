#!/usr/bin/env python3
"""Qualify crash isolation with two real workers on a disposable local stack."""

import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import uuid


API = "/rest/reports/data-export"
EXPECTED_CSV = (
    b"\xef\xbb\xbfAccession Number,Viral Load\r\n"
    b"REPORTING-MVP-REPEAT,450\r\nREPORTING-MVP-REPEAT,450\r\n"
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--primary", required=True)
    parser.add_argument("--peer", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--peer-url", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    for url in (args.base_url, args.peer_url):
        assert urllib.parse.urlparse(url).hostname in {"localhost", "127.0.0.1", "::1"}
    assert args.primary != args.peer
    helper_path = Path(__file__).with_name("verify-restart.py")
    spec = importlib.util.spec_from_file_location("reporting_restart", helper_path)
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)

    def inspect(name):
        return helper.inspect(name)

    def sql(query):
        return helper.docker(
            "exec", args.database, "psql", "-X", "-v", "ON_ERROR_STOP=1",
            "-U", "clinlims", "-d", "clinlims", "-Atc", query,
        )

    before = {name: inspect(name) for name in (args.primary, args.peer, args.database)}
    for process in before.values():
        assert process["State"]["Running"]
        assert process["Config"]["Labels"]["com.docker.compose.project"] == args.project
    for name, url in ((args.primary, args.base_url), (args.peer, args.peer_url)):
        port = urllib.parse.urlparse(url).port
        published = before[name]["NetworkSettings"]["Ports"].get("8443/tcp") or []
        assert any(p["HostPort"] == str(port) for p in published), "URL must use this application's published port"

    def mount(process, destination):
        return next(m["Source"] for m in process["Mounts"] if m["Destination"] == destination)

    for destination in (
        "/usr/local/tomcat/webapps/OpenELIS-Global.war",
        "/var/lib/openelis-global/reporting",
    ):
        assert mount(before[args.primary], destination) == mount(before[args.peer], destination)
    assert before[args.primary]["Image"] == before[args.peer]["Image"]
    assert mount(before[args.primary], "/var/lib/openelis-global/logs") != mount(
        before[args.peer], "/var/lib/openelis-global/logs"
    )
    args.output.mkdir(parents=True, exist_ok=False)

    def record(name, value):
        (args.output / name).write_text(json.dumps(value, indent=2) + "\n")

    record("processes-before.json", {
        name: {"id": p["Id"], "startedAt": p["State"]["StartedAt"], "image": p["Image"]}
        for name, p in before.items()
    })
    clients = [helper.Client(args.base_url), helper.Client(args.peer_url)]
    deadline = time.monotonic() + 900
    for name, client in zip((args.primary, args.peer), clients):
        while True:
            process = inspect(name)
            assert process["Id"] == before[name]["Id"] and process["State"]["Running"]
            try:
                if "authenticated" in client.call("/session"):
                    break
            except (OSError, ValueError):
                pass
            assert time.monotonic() < deadline, "Observe the same container; do not restart it"
            time.sleep(3)
        client.login()
    client = clients[0]
    assert sql("select count(*) from clinlims.reporting_export_job "
               "where state in ('QUEUED','GENERATING')") == "0"

    def submit():
        return client.call(API + "/jobs", {
            "schemaVersion": 1, "reportType": "SAMPLE_TESTING", "layout": "SPREADSHEET",
            "selectedVariables": ["accessionNumber", "test:174"],
            "filterSpec": {"dateFrom": "2026-05-05", "dateTo": "2026-05-05",
                           "labSectionIds": [], "testIds": [], "resultStatuses": ["FINALIZED"]},
            "clientRequestId": "isolation-" + str(uuid.uuid4()),
        })

    def job(id):
        return client.call(API + "/jobs/" + id)

    def wait_ready(id):
        deadline = time.monotonic() + 90
        while True:
            current = job(id)
            if current["state"] not in {"QUEUED", "GENERATING"}:
                assert current["state"] == "READY", current
                return current
            assert time.monotonic() < deadline, "Report did not reach READY"
            time.sleep(.5)

    def download(id, caller=client):
        data = caller.call(API + "/jobs/" + id + "/download", raw=True)
        assert data == EXPECTED_CSV
        return data

    baseline = wait_ready(submit()["id"])
    (args.output / "baseline.csv").write_bytes(download(baseline["id"]))
    download(baseline["id"], clients[1])
    stall_name = "reporting-isolation-" + uuid.uuid4().hex
    stall_log = (args.output / "stall.log").open("w")
    stall = subprocess.Popen(
        ["docker", "exec", "-i", "-e", "PGAPPNAME=" + stall_name, args.database,
         "psql", "-X", "-v", "ON_ERROR_STOP=1", "-U", "clinlims", "-d", "clinlims"],
        stdin=subprocess.PIPE, stdout=stall_log, stderr=subprocess.STDOUT, text=True,
    )
    observations = []
    jobs = []
    lock_released = False

    def release_stall():
        nonlocal lock_released
        if not lock_released:
            sql("select pg_cancel_backend(pid) from pg_stat_activity "
                "where application_name='" + stall_name + "'")
            stall.wait(timeout=10)
            lock_released = True

    def audit(name, ids):
        text = helper.docker("exec", name, "cat", "/var/lib/openelis-global/logs/openELIS.log")
        events = []
        for line in text.splitlines():
            if "REPORTING_AUDIT " not in line:
                continue
            entry = json.loads(line.split("REPORTING_AUDIT ", 1)[1])
            if entry["targetId"] in ids:
                events.append(entry)
        return events

    def snapshot():
        ids = ",".join("'" + str(uuid.UUID(j["id"])) + "'" for j in jobs)
        return json.loads(sql(
            "select json_agg(j order by submitted_at) from (select id,state,worker_id,"
            "extract(epoch from lease_until) as lease,request_json,output_cleaned_at,"
            "submitted_at from clinlims.reporting_export_job where id in (" + ids + ")) j"
        ))

    def partial_exists(row):
        path = "/var/lib/openelis-global/reporting/" + row["id"] + "." + row["worker_id"] + ".part"
        result = subprocess.run(["docker", "exec", args.primary, "test", "-f", path])
        return result.returncode == 0

    try:
        # The bounded read stall holds no changes and releases in finally. Its
        # ceiling exceeds the unchanged five-minute worker lease, making both
        # an active renewal and natural abandoned-lease expiry observable.
        stall.stdin.write("BEGIN; SET LOCAL lock_timeout='5s'; "
                          "LOCK TABLE clinlims.result IN ACCESS EXCLUSIVE MODE; "
                          "SELECT 'REPORTING_STALL_READY'; SELECT pg_sleep(450); ROLLBACK;\n")
        stall.stdin.close()
        deadline = time.monotonic() + 10
        while "REPORTING_STALL_READY" not in (args.output / "stall.log").read_text():
            assert stall.poll() is None and time.monotonic() < deadline
            time.sleep(.2)
        jobs = [submit() for _ in range(3)]
        record("submitted.json", jobs)
        deadline = time.monotonic() + 30
        while True:
            initial = snapshot()
            generating = [j for j in initial if j["state"] == "GENERATING"]
            queued = [j for j in initial if j["state"] == "QUEUED"]
            if len(generating) == 2 and len(queued) == 1 and all(partial_exists(j) for j in generating):
                break
            assert time.monotonic() < deadline, "Expected one generating export per process and one queued"
            time.sleep(.5)
        assert len({j["worker_id"] for j in generating}) == 2
        ids = {j["id"] for j in jobs}
        primary_events, peer_events = audit(args.primary, ids), audit(args.peer, ids)
        peer_claims = [e for e in peer_events if e["action"] == "STARTED"]
        primary_claims = [e for e in primary_events if e["action"] == "STARTED"]
        assert len(peer_claims) == len(primary_claims) == 1
        dead = next(j for j in generating if j["id"] == peer_claims[0]["targetId"])
        live = next(j for j in generating if j["id"] == primary_claims[0]["targetId"])
        assert dead["worker_id"] != live["worker_id"]
        for row in generating:
            try:
                download(row["id"])
            except urllib.error.HTTPError as error:
                assert error.code == 409
            else:
                raise AssertionError("Generating output was downloadable")
        record("ownership-before-kill.json", {
            "initial": initial, "primaryAudit": primary_events, "peerAudit": peer_events,
            "liveJob": live["id"], "abandonedJob": dead["id"], "queuedJob": queued[0]["id"],
        })
        current_peer = inspect(args.peer)
        assert current_peer["Id"] == before[args.peer]["Id"] and current_peer["State"]["Running"]
        subprocess.run(["docker", "kill", "--signal", "KILL", args.peer], check=True)
        stopped = inspect(args.peer)
        assert not stopped["State"]["Running"] and stopped["State"]["ExitCode"] == 137
        record("peer-killed.json", {"id": stopped["Id"], "state": stopped["State"]})
        print(json.dumps({"phase": "peer killed", "live": live["id"], "abandoned": dead["id"]}), flush=True)
        deadline = time.monotonic() + 360
        last_report = 0
        while True:
            assert stall.poll() is None, "Read stall ended before isolation was established"
            primary = inspect(args.primary)
            assert primary["Id"] == before[args.primary]["Id"] and primary["State"]["Running"]
            assert primary["State"]["StartedAt"] == before[args.primary]["State"]["StartedAt"]
            assert not inspect(args.peer)["State"]["Running"]
            rows = {r["id"]: r for r in snapshot()}
            observed_live, observed_dead = rows[live["id"]], rows[dead["id"]]
            assert observed_live["state"] == "GENERATING" and partial_exists(observed_live)
            assert rows[queued[0]["id"]]["state"] == "QUEUED"
            assert all(rows[r["id"]]["request_json"] == r["request_json"] for r in initial)
            assert job(live["id"])["state"] == "GENERATING"
            observations.append({"at": time.time(), "liveLease": observed_live["lease"],
                                 "deadLease": observed_dead["lease"], "deadState": observed_dead["state"]})
            if observed_dead["state"] == "FAILED" and observed_dead["output_cleaned_at"] is not None:
                assert not partial_exists(observed_dead)
                assert observed_live["lease"] > dead["lease"] + 240
                break
            assert observed_dead["state"] in {"GENERATING", "FAILED"}
            assert time.monotonic() < deadline, "Abandoned worker was not recovered within its lease plus allowance"
            if time.monotonic() - last_report >= 30:
                print(json.dumps({"phase": "live lease renewing", "observations": len(observations),
                                  "deadState": observed_dead["state"]}), flush=True)
                last_report = time.monotonic()
            time.sleep(5)
        assert len({o["liveLease"] for o in observations}) >= 3
        release_stall()
        for id in (live["id"], queued[0]["id"]):
            wait_ready(id)
            download(id)
        parent = job(dead["id"])
        assert parent["state"] == "FAILED" and parent["failureCode"] == "reporting.job.interrupted"
        retry = client.call(API + "/jobs/" + dead["id"] + "/retry", {
            "clientRequestId": "isolation-retry-" + str(uuid.uuid4()),
        })
        assert retry["parentId"] == dead["id"] and retry["request"] == parent["request"]
        wait_ready(retry["id"])
        download(retry["id"])
        assert job(dead["id"])["state"] == "FAILED"
        download(baseline["id"])
        assert inspect(args.database)["Id"] == before[args.database]["Id"]
        events = audit(args.primary, ids | {retry["id"]})
        interrupted = [e for e in events if e["action"] == "INTERRUPTED"]
        assert len(interrupted) == 1 and interrupted[0]["targetId"] == dead["id"]
        record("verified.json", {
            "baseline": baseline["id"], "liveJob": live["id"], "abandonedJob": dead["id"],
            "queuedJob": queued[0]["id"], "retryJob": retry["id"],
            "primaryContainer": before[args.primary]["Id"], "peerContainer": before[args.peer]["Id"],
            "databaseContainer": before[args.database]["Id"], "leaseSecondsUnchanged": 300,
            "renewalObservations": len(observations), "liveOutputRetained": True,
            "abandonedOutputRemoved": True, "queuedAndLiveCompleted": True,
            "retryPreservedFrozenRequest": True, "baselineBytesUnchanged": True,
            "csvSha256": hashlib.sha256(EXPECTED_CSV).hexdigest(), "primaryAudit": events,
        })
        print(json.dumps({"phase": "verified", "receipt": str(args.output / "verified.json")}), flush=True)
    finally:
        record("lease-observations.json", observations)
        release_stall()
        stall_log.close()


if __name__ == "__main__":
    main()
