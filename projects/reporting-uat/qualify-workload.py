#!/usr/bin/env python3
"""Qualify the real reporting worker with 50,000 public synthetic results."""

import argparse
from collections import Counter
import csv
from datetime import datetime, timezone
import hashlib
import io
import json
from pathlib import Path
import re
import runpy
import signal
import subprocess
import time
from urllib.parse import urlparse
from urllib.error import HTTPError
import uuid


ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / "src/test/resources/fixtures/reporting-workload-50000.sql"
Client = runpy.run_path(str(Path(__file__).with_name("verify-restart.py")))["Client"]
JOBS = "/rest/reports/data-export/jobs"
SMALL_CSV = b"\xef\xbb\xbfAccession Number,Viral Load\r\nREPORTING-MVP-REPEAT,450\r\nREPORTING-MVP-REPEAT,450\r\n"


def docker(*arguments):
    return subprocess.check_output(["docker", *arguments], text=True).strip()


def inspect(name):
    return json.loads(docker("inspect", name))[0]


def expected_reading(index):
    """Independent oracle: adjacent equal readings retain separate occurrences."""
    if index > 40000:
        return 5001, str(100 + ((index - 40001) // 2) % 50), "30"
    specimen = (index - 1) // 8 + 1
    position = (index - 1) % 8
    return specimen, str(specimen % 1000 + (position % 4) // 2), "30" if position < 4 else "90"


def duration(job):
    parse = lambda value: datetime.fromisoformat(value.replace("Z", "+00:00"))
    return (parse(job["completedAt"]) - parse(job["startedAt"])).total_seconds()


def qualify(args):
    assert urlparse(args.base_url).hostname in {"localhost", "127.0.0.1", "::1"}
    app, db = inspect(args.app_container), inspect(args.db_container)
    for container in (app, db):
        assert container["State"]["Running"]
        assert container["Config"]["Labels"]["com.docker.compose.project"] == args.project
    args.output.mkdir(parents=True, exist_ok=False)

    def save(name, value):
        (args.output / name).write_text(json.dumps(value, indent=2) + "\n")

    def sql(query, *, administrator=False):
        return docker("exec", args.db_container, "psql", "-X", "-q", "-v", "ON_ERROR_STOP=1",
                      "-U", "postgres" if administrator else "clinlims", "-d", "clinlims", "-Atc", query)

    assert sql("select count(*) from clinlims.reporting_export_job where state in ('QUEUED','GENERATING')") == "0"
    if args.seed:
        with FIXTURE.open("rb") as fixture, (args.output / "seed.log").open("wb") as log:
            subprocess.run(["docker", "exec", "-i", args.db_container, "psql", "-X", "-q",
                            "-v", "ON_ERROR_STOP=1", "-U", "clinlims", "-d", "clinlims"],
                           stdin=fixture, stdout=log, stderr=subprocess.STDOUT, check=True)
    identities_csv = sql("COPY (select r.id, si.id, substring(r.fhir_uuid::text from 25)::integer "
                         "from clinlims.result r join clinlims.analysis a on a.id=r.analysis_id "
                         "join clinlims.sample_item si on si.id=a.sampitem_id "
                         "where r.fhir_uuid::text like '47950000-0000-4000-8004-%' "
                         "order by r.id) TO STDOUT WITH CSV")
    (args.output / "identities.csv").write_text(identities_csv + "\n")
    identities = {result: (specimen, int(index)) for result, specimen, index in csv.reader(io.StringIO(identities_csv))}
    assert len(identities) == 50000
    assert {index for _, index in identities.values()} == set(range(1, 50001))
    assert len({specimen for specimen, _ in identities.values()}) == 5001
    test_id = sql("select id from clinlims.test where guid='b50d156e-0f6f-40cd-921c-4e831602a623' and is_active='Y'")
    assert test_id.isdigit()
    client, ordinary = Client(args.base_url), Client(args.base_url)
    client.login()
    ordinary.login()

    java_pids = docker("exec", args.app_container, "sh", "-c",
                       'for p in /proc/[0-9]*/comm; do if [ "$(cat "$p")" = java ]; then echo "${p%/comm}"; fi; done').splitlines()
    assert len(java_pids) == 1 and re.fullmatch(r"/proc/[0-9]+", java_pids[0])
    java = java_pids[0]

    def memory():
        status = docker("exec", args.app_container, "cat", java + "/status")
        return {key: int(value) for key, value in re.findall(r"^(VmRSS|VmHWM):\s+(\d+) kB$", status, re.M)}

    # Linux documents value 5 as resetting only the resident-memory peak counter.
    # The application is not restarted and its heap/data are not reset.
    docker("exec", args.app_container, "sh", "-c", "printf '5' > " + java + "/clear_refs")
    baseline_memory = memory()
    logging = json.loads(sql("select json_build_object('value',current_setting('log_statement'),'inAutoConf',"
                             "exists(select 1 from pg_file_settings where name='log_statement' "
                             "and sourcefile like '%postgresql.auto.conf' and applied))", administrator=True))
    assert logging["value"] in {"none", "ddl", "mod", "all"}
    save("logging-before.json", logging)
    started = datetime.now(timezone.utc).isoformat()
    jobs, observations, read_errors = [], [], []
    deadline = time.monotonic() + args.timeout

    def configure_logging(statement, expected):
        sql(statement, administrator=True)
        sql("SELECT pg_reload_conf()", administrator=True)
        reload_deadline = time.monotonic() + 5
        while sql("SHOW log_statement", administrator=True) != expected:
            assert time.monotonic() < reload_deadline, "Database logging reload was not observed"
            time.sleep(.1)

    def submit(layout, small=False):
        variables = ["accessionNumber", "test:" + test_id] if small else (
            ["accessionNumber", "specimenId", "test:" + test_id, "test:" + test_id + ":resultedToValidatedMinutes"]
            if layout == "SPREADSHEET" else
            ["accessionNumber", "specimenId", "resultId", "resultValue", "resultedToValidatedMinutes"])
        day = "2026-05-05" if small else "2026-05-07"
        return client.call(JOBS, {"schemaVersion": 1, "reportType": "SAMPLE_TESTING", "layout": layout,
            "selectedVariables": variables, "filterSpec": {"dateFrom": day, "dateTo": day,
                "labSectionIds": [], "testIds": [test_id], "resultStatuses": ["FINALIZED"]},
            "clientRequestId": "workload-" + uuid.uuid4().hex})

    # Statement logging exposes repeated cursor fetches. It is temporary and
    # restored even if a correctness check, observation timeout or interrupt fails.
    try:
        configure_logging("ALTER SYSTEM SET log_statement = 'all'", "all")
        jobs = [submit("SPREADSHEET"), submit("RESULT_LIST")]
        jobs.extend(submit("SPREADSHEET", small=True) for _ in range(3))
        save("submitted.json", jobs)
        ids = ",".join("'" + str(uuid.UUID(job["id"])) + "'" for job in jobs)

        def atomic_states():
            # A single database statement avoids counting two different instants
            # while the worker finishes one job and starts the next.
            return json.loads(sql("select json_object_agg(id,state) from clinlims.reporting_export_job "
                                  "where id in (" + ids + ")"))

        before_limit = atomic_states()
        save("admission-active-jobs.json", before_limit)
        assert len(before_limit) == 5 and all(state in {"QUEUED", "GENERATING"} for state in before_limit.values()), \
            "Work completed before the concurrent-limit observation; do not report a limit failure"
        try:
            submit("SPREADSHEET", small=True)
        except HTTPError as error:
            assert error.code == 429, error.code
            admission_status = error.code
        else:
            raise AssertionError("Sixth concurrent report was accepted despite the five-job limit")
        while True:
            current = [client.call(JOBS + "/" + job["id"]) for job in jobs]
            states = {job["id"]: job["state"] for job in current}
            simultaneous = atomic_states()
            assert sum(state == "GENERATING" for state in simultaneous.values()) <= 1
            reads = {}
            for path in ["/session", JOBS + "?page=0", "/rest/reports/data-export/variables?reportType=SAMPLE_TESTING&layout=SPREADSHEET"]:
                before = time.monotonic()
                try:
                    body = ordinary.call(path)
                    if path == "/session":
                        assert body["authenticated"]
                    reads[path] = time.monotonic() - before
                except Exception as error:
                    read_errors.append({"path": path, "error": str(error)})
            observations.append({"at": datetime.now(timezone.utc).isoformat(), "states": states,
                                 "atomicStates": simultaneous, "memoryKiB": memory(), "readSeconds": reads})
            save("observations.json", observations)
            print(json.dumps({"states": Counter(states.values()), "memoryKiB": observations[-1]["memoryKiB"]}), flush=True)
            if all(state not in {"QUEUED", "GENERATING"} for state in states.values()):
                jobs = current
                break
            assert time.monotonic() < deadline, "Observation limit reached; inspect these jobs instead of restarting"
            time.sleep(1)
    finally:
        if logging["inAutoConf"]:
            restore = "ALTER SYSTEM SET log_statement = '" + logging["value"] + "'"
        else:
            restore = "ALTER SYSTEM RESET log_statement"
        configure_logging(restore, logging["value"])
        save("logging-restored.json", {"restoredValue": logging["value"], "originalOverrideRetained": logging["inAutoConf"]})

    assert not read_errors, read_errors
    assert all(job["state"] == "READY" for job in jobs), [(j["id"], j["state"], j.get("failureCode")) for j in jobs]
    save("completed.json", jobs)
    fetch_log = subprocess.check_output(["docker", "logs", "--since", started, args.db_container],
                                        stderr=subprocess.STDOUT, text=True)
    cursor_fetches = Counter()
    for line in fetch_log.splitlines():
        if "execute fetch from " in line and "collection_date" in line and "order by" in line:
            match = re.search(r"execute fetch from ([^:]+):", line)
            process = re.search(r"\[([0-9]+)\]", line)
            if match and process:
                cursor_fetches[process.group(1) + "/" + match.group(1)] += 1
    save("cursor-fetches.json", dict(cursor_fetches))
    # Two 50,000-row streams at 250 rows per fetch require 199 follow-up fetches
    # each, with a possible additional empty fetch to observe end-of-stream.
    large_streams = [count for count in cursor_fetches.values() if count >= 199]
    assert len(large_streams) == 2 and all(count <= 200 for count in large_streams), dict(cursor_fetches)

    expected_sheet = Counter()
    for specimen_id, index in identities.values():
        specimen, value, turnaround = expected_reading(index)
        expected_sheet[(f"RPT50K-V1-{specimen:05d}", specimen_id, value, turnaround)] += 1
    verified = []
    for position, job in enumerate(jobs):
        data = client.call(JOBS + "/" + job["id"] + "/download", raw=True)
        name = ("spreadsheet" if position == 0 else "result-list" if position == 1 else f"ordinary-{position - 1}") + ".csv"
        (args.output / name).write_bytes(data)
        assert data.startswith(b"\xef\xbb\xbf") and data.endswith(b"\r\n")
        rows = list(csv.reader(io.StringIO(data.decode("utf-8-sig"))))
        if position == 0:
            assert rows[0] == ["Accession Number", "Specimen ID", "Viral Load", "Viral Load — Resulted to Validated (min)"]
            assert Counter(map(tuple, rows[1:])) == expected_sheet
        elif position == 1:
            assert rows[0] == ["Accession Number", "Specimen ID", "Result ID", "Result Value", "Resulted to Validated (min)"]
            observed_ids = set()
            for accession, specimen_id, result_id, value, turnaround in rows[1:]:
                assert result_id in identities and result_id not in observed_ids
                expected_specimen_id, index = identities[result_id]
                specimen, expected_value, expected_turnaround = expected_reading(index)
                assert (accession, specimen_id, value, turnaround) == (
                    f"RPT50K-V1-{specimen:05d}", expected_specimen_id, expected_value, expected_turnaround)
                observed_ids.add(result_id)
            assert observed_ids == set(identities)
        else:
            assert data == SMALL_CSV
        expected_count = 50000 if position < 2 else 2
        assert len(rows) - 1 == job["rowCount"] == expected_count
        verified.append({"jobId": job["id"], "layout": job["request"]["layout"], "rowCount": job["rowCount"],
                         "generationSeconds": duration(job), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})

    after = inspect(args.app_container)
    assert after["Id"] == app["Id"] and after["State"]["Running"]
    assert after["RestartCount"] == app["RestartCount"]
    assert inspect(args.db_container)["Id"] == db["Id"]
    latencies = sorted(value for sample in observations for value in sample["readSeconds"].values())
    java_options = next((value.split("=", 1)[1] for value in app["Config"]["Env"] if value.startswith("CATALINA_OPTS=")), "")
    heap = re.findall(r"(?:^|\s)(-Xmx\S+)", java_options)
    machine = json.loads(docker("info", "--format", '{"architecture":"{{.Architecture}}","cpus":{{.NCPU}},"memoryBytes":{{.MemTotal}}}'))
    war = next(mount["Source"] for mount in app["Mounts"] if mount["Destination"].endswith("/OpenELIS-Global.war"))
    result = {"status": "passed", "fixtureSha256": hashlib.sha256(FIXTURE.read_bytes()).hexdigest(),
              "appContainer": app["Id"], "appImage": app["Image"], "dbContainer": db["Id"],
              "appWarSha256": hashlib.sha256(Path(war).read_bytes()).hexdigest(), "dockerEnvironment": machine,
              "configuredJavaHeap": heap, "dbImage": db["Config"]["Image"],
              "startedAt": started, "javaProcess": java, "baselineMemoryKiB": baseline_memory,
              "peakResidentKiB": max(sample["memoryKiB"]["VmHWM"] for sample in observations),
              "readCount": len(latencies), "maxReadSeconds": max(latencies),
              "p95ReadSeconds": latencies[int((len(latencies) - 1) * .95)],
              "maxGeneratingObserved": max(sum(s == "GENERATING" for s in sample["atomicStates"].values()) for sample in observations),
              "sixthConcurrentSubmissionStatus": admission_status, "fetchSize": 250,
              "largeStreamFollowupFetches": large_streams, "csvs": verified,
              "measurementNote": "Java resident high-water counter reset before the run. Query logging was enabled for cursor evidence, so timings include its overhead. No app/database restart or record deletion."}
    save("verified.json", result)
    print(json.dumps(result, indent=2), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--app-container", required=True)
    parser.add_argument("--db-container", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed", action="store_true")
    parser.add_argument("--timeout", type=int, default=900)
    def interrupted(*_):
        raise SystemExit("Interrupted")

    signal.signal(signal.SIGTERM, interrupted)
    qualify(parser.parse_args())
