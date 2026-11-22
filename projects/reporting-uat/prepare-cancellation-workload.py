#!/usr/bin/env python3
"""Prepare repeatable cancellation UAT in the dedicated synthetic reporting stack."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def docker(*args, **kwargs):
    return subprocess.check_output(["docker", *args], **kwargs)


def prepare(args):
    app, database = [json.loads(docker("inspect", name))[0]
                     for name in (args.app_container, args.db_container)]
    for container in (app, database):
        assert container["State"]["Running"]
        assert container["Config"]["Labels"]["com.docker.compose.project"] == args.project
    identity = None
    if args.project == "reporting-uat":
        assert args.identity, "The public reporting deployment identity is required"
        identity = json.loads(args.identity.read_text())
        assert identity["instance"] == "reporting" and identity["state"] == "ready"

    def sql(query):
        return docker("exec", args.db_container, "psql", "-X", "-v", "ON_ERROR_STOP=1",
                      "-U", "clinlims", "-d", "clinlims", "-Atc", query, text=True).strip()

    assert sql("SELECT count(*) FROM clinlims.reporting_export_job "
               "WHERE state IN ('QUEUED','GENERATING')") == "0", "Wait for active exports to finish"
    args.output.mkdir(parents=True, exist_ok=False)
    before = {"appContainer": app["Id"], "databaseContainer": database["Id"],
              "appStartedAt": app["State"]["StartedAt"], "deployment": identity,
              "fixtureSha256": hashlib.sha256(args.fixture.read_bytes()).hexdigest()}
    (args.output / "before.json").write_text(json.dumps(before, indent=2) + "\n")
    with (args.output / "before.pg_dump").open("wb") as backup:
        subprocess.run(["docker", "exec", args.db_container, "pg_dump", "-U", "clinlims",
                        "-d", "clinlims", "-Fc"], stdout=backup, check=True)
    with args.fixture.open("rb") as fixture, (args.output / "seed.log").open("wb") as log:
        subprocess.run(["docker", "exec", "-i", args.db_container, "psql", "-X",
                        "-v", "ON_ERROR_STOP=1", "-U", "clinlims", "-d", "clinlims"],
                       stdin=fixture, stdout=log, stderr=subprocess.STDOUT, check=True)

    # Independent counts and relationships qualify setup; the browser workflow
    # subsequently compares every CSV row and value with its own fixture oracle.
    observed = json.loads(sql("""
        SELECT json_build_object('results', count(*), 'resultIdentities', count(DISTINCT r.fhir_uuid),
            'specimens', count(DISTINCT si.id), 'analyses', count(DISTINCT a.id),
            'accessions', count(DISTINCT s.accession_number),
            'wrongPeriod', count(*) FILTER (WHERE si.collection_date < timestamptz '2026-05-07 00:00:00+00'
                OR si.collection_date >= timestamptz '2026-05-08 00:00:00+00'),
            'wrongAccession', count(*) FILTER (WHERE s.accession_number NOT LIKE 'RPT50K-V1-%'))
        FROM clinlims.result r JOIN clinlims.analysis a ON a.id=r.analysis_id
        JOIN clinlims.sample_item si ON si.id=a.sampitem_id
        JOIN clinlims.sample s ON s.id=si.samp_id
        WHERE r.fhir_uuid::text LIKE '47950000-0000-4000-8004-%'
    """))
    assert observed == {"results": 50000, "resultIdentities": 50000, "specimens": 5001,
                        "analyses": 10001, "accessions": 5001, "wrongPeriod": 0, "wrongAccession": 0}, observed
    after = json.loads(docker("inspect", args.app_container))[0]
    assert after["Id"] == app["Id"] and after["State"]["StartedAt"] == app["State"]["StartedAt"]
    assert after["State"]["Running"]
    receipt = {**before, "fixture": str(args.fixture), "counts": observed,
               "status": "seeded and verified; browser qualification remains separate"}
    (args.output / "verified.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"counts": observed, "evidence": str(args.output), "applicationRetained": app["Id"]}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, choices=["reporting-mvp-iteration1", "reporting-uat"])
    parser.add_argument("--app-container", required=True)
    parser.add_argument("--db-container", required=True)
    parser.add_argument("--fixture", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--identity", type=Path)
    prepare(parser.parse_args())
