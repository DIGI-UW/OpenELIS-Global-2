#!/usr/bin/env python3
"""Resolve local browser jobs from the GitHub workflows, without a second inventory."""
import json
from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parents[2]


def write_plan(destination):
    workflows = ROOT / ".github/workflows"
    jobs = yaml.safe_load((workflows / "e2e-authoritative-reusable.yml").read_text())["jobs"]
    reusable = yaml.safe_load((workflows / "e2e-playwright-reusable.yml").read_text())
    defaults = reusable.get("on", reusable.get(True))["workflow_call"]["inputs"]
    default_shards = defaults["shard_indices"]["default"]
    resolved = {}
    rows = []
    for name, job in jobs.items():
        workflow = job.get("uses", "")
        if workflow.endswith("e2e-playwright-reusable.yml"):
            config = job["with"]
            suite = "analyzer" if config["setup_analyzer_harness"] else "core"
            prefix = "analyzer" if suite == "analyzer" else "core-playwright"
            shards = json.loads(config.get("shard_indices", default_shards))
            if shards != list(range(1, len(shards) + 1)) or not shards:
                raise ValueError(f"Unsupported shard indices in {name}: {shards}")
            resolved[name] = {"projects": config["projects"], "shards": shards}
            rows.extend((f"{prefix}-{index}", suite, config["projects"], f"{index}/{len(shards)}")
                        for index in shards)
        elif workflow.endswith("e2e-cypress-deprecated.yml"):
            cypress = yaml.safe_load((workflows / "e2e-cypress-deprecated.yml").read_text())
            groups = cypress["jobs"]["e2e-cypress"]["strategy"]["matrix"]["include"]
            resolved[name] = {"shards": [group["shard"] for group in groups]}
            rows.extend((f'cypress-{group["shard"]}', f'cypress-{group["shard"]}', "-", "-")
                        for group in groups)
        elif workflow:
            raise ValueError(f"No local executor for {name}: {workflow}")
    names = [row[0] for row in rows]
    if not names or len(names) != len(set(names)):
        raise ValueError("Missing or duplicate browser jobs in workflow plan")
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "resolved-e2e-plan.json").write_text(json.dumps(resolved, indent=2) + "\n")
    (destination / "e2e-jobs.tsv").write_text("".join("\t".join(row) + "\n" for row in rows))
    lanes = ["backend", "frontend", "e2e", "e2e-scope", "shared-build", "e2e-frontend-deps", *names]
    (destination / "expected-lanes.txt").write_text("\n".join(lanes) + "\n")


if __name__ == "__main__":
    write_plan(Path(sys.argv[1]))
