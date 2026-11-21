"""Exercise the build-to-test revision contract using the actual workflow steps."""

import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[2]
HEAD = "1" * 40
MERGE = "2" * 40


def workflow(name):
    return yaml.safe_load((ROOT / ".github/workflows" / name).read_text())


def step(job, name):
    return next(s for s in job["steps"] if s.get("name") == name)


def render(text, values):
    # Only the property lookups and fallback operator used by these steps.
    def value(match):
        for key in match.group(1).split("||"):
            result = values[key.strip()]
            if result:
                return result
        return ""
    return re.sub(r"\$\{\{\s*(.*?)\s*\}\}", value, text)


class E2ERevisionTest(unittest.TestCase):
    def test_image_and_tests_use_the_same_immutable_revision(self):
        build = workflow("e2e-playwright.yml")["jobs"]["shared-build"]
        context = workflow("e2e-tests.yml")["jobs"]["setup"]
        cases = [
            ("pull_request", False, "ghcr"),
            ("pull_request", True, "fork-handoff"),
            ("pull_request", True, "ghcr"),
            ("push", False, "ghcr"),
            ("release", False, "ghcr"),
            ("workflow_dispatch", False, "ghcr"),
        ]
        for event, fork, transfer in cases:
            with self.subTest(event=event, fork=fork, transfer=transfer), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                values = {
                    "github.event_name": event,
                    "github.event.pull_request.number": "42" if event == "pull_request" else "",
                    "github.event.pull_request.head.sha": HEAD if event == "pull_request" else "",
                    "github.event.pull_request.head.repo.fork": str(fork).lower(),
                    "github.sha": MERGE if event == "pull_request" else HEAD,
                    "github.event.workflow_run.head_sha": HEAD,
                    "github.event.workflow_run.id": "123",
                    "steps.scope.outputs.skip": "false",
                    "steps.scope.outputs.skip_description": "",
                    "steps.download_context.outcome": "success",
                    "steps.download_transfer.outcome": "success",
                }
                def execute(script):
                    result = subprocess.run(
                        ["bash", "-eu", "-c", render(script, values).replace("/tmp/", str(root) + "/")],
                        cwd=ROOT, env={**os.environ, "GITHUB_OUTPUT": str(root / "output")},
                        capture_output=True, text=True,
                    )
                    self.assertEqual(0, result.returncode, result.stderr)
                execute(step(build, "Prepare build context metadata (early)")["run"])
                (root / "e2e-build-context-core").rename(root / "e2e-build-context")
                (root / "e2e-transfer-state").mkdir()
                (root / "e2e-transfer-state/image_transfer").write_text(transfer)
                execute(step(context, "Parse build context")["run"])
                outputs = dict(line.split("=", 1) for line in (root / "output").read_text().splitlines())
                checkout = step(build, "Checkout code")["with"].get("ref", "${{ github.sha }}")
                self.assertEqual(HEAD, render(checkout, values), "image build must use the PR head, not its merge ref")
                self.assertEqual(HEAD, outputs["checkout_ref"], "tests must use the same immutable commit")
                self.assertEqual(HEAD, outputs["status_sha"], "status must still appear on the PR head")
                self.assertEqual("same_run" if transfer == "fork-handoff" else "cross_run",
                                 outputs["artifact_source_mode"])
                self.assertEqual("123", outputs["build_run_id"])
                self.assertEqual("03 Checkpoint - E2E", outputs["checkpoint_context"])


if __name__ == "__main__":
    unittest.main()
