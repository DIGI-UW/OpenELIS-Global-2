"""Validate publication and E2E status contracts."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]


class PublicationWorkflowTest(unittest.TestCase):
    def test_request_builder_passes_configured_namespace_and_rejects_invalid_input(self):
        workflow = yaml.load((ROOT / ".github/workflows/publish-images.yml").read_text(), Loader=yaml.BaseLoader)
        step = next(step for step in workflow["jobs"]["deploy-testing"]["steps"]
                    if step.get("name") == "Validate deployment request")
        images = {service: "test-org/openelis-global-2" + suffix + "@sha256:" + "b" * 64
                  for service, suffix in [("oe.openelis.org", ""), ("db.openelis.org", "-database"),
                                          ("fhir.openelis.org", "-fhir"), ("frontend.openelis.org", "-frontend"),
                                          ("proxy", "-proxy")]}
        env = {**os.environ, "DEPLOY_HOST": "testing.example.org", "DEPLOY_USER": "ubuntu", "DEPLOY_PORT": "22",
               "DOCKERHUB_NAMESPACE": "test-org", "DEPLOY_PATH": "/srv/openelis-docker", "GITHUB_RUN_ID": "123",
               "GITHUB_RUN_ATTEMPT": "2", "READINESS_URL": "https://testing.example.org/health",
               "READINESS_TIMEOUT": "300", "READINESS_JSON_KEY": "status", "READINESS_EXPECTED": '"UP"',
               "IMAGE_MANIFEST": json.dumps({"appSha": "a" * 40, "appBranch": "develop", "images": images})}
        for overrides in ({}, {"IMAGE_MANIFEST": "null"}, {"DOCKERHUB_NAMESPACE": "wrong-org"},
                          {"DEPLOY_HOST": "bad;command"}):
            with self.subTest(overrides=overrides), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / ".github").mkdir()
                (root / ".github/scripts").symlink_to(ROOT / ".github/scripts", target_is_directory=True)
                result = subprocess.run(["bash", "-c", step["run"]], cwd=root, env={**env, **overrides},
                                        capture_output=True, text=True)
                if overrides:
                    self.assertNotEqual(0, result.returncode)
                    self.assertFalse((root / "request.json").exists())
                else:
                    self.assertEqual(0, result.returncode, result.stderr)
                    request = json.loads((root / "request.json").read_text())
                    self.assertEqual("test-org", request["dockerhub_namespace"])
                    self.assertEqual(images, request["manifest"]["images"])

    def test_publication_does_not_require_an_external_workflow_interface(self):
        workflow = yaml.load((ROOT / ".github/workflows/publish-images.yml").read_text(), Loader=yaml.BaseLoader)
        for name, job in workflow["jobs"].items():
            with self.subTest(job=name):
                if "uses" in job:
                    self.assertTrue(job["uses"].startswith("./.github/workflows/"),
                                    "External workflow inputs must not prevent publication from loading")

    def test_reporter_posts_the_build_status_consumed_by_publication(self):
        workflow = yaml.load((ROOT / ".github/workflows/e2e-tests.yml").read_text(), Loader=yaml.BaseLoader)
        for job in ("set-pending-status", "report-status"):
            script = workflow["jobs"][job]["steps"][0]["with"]["script"]
            script = script.replace("${{ github.event.workflow_run.id }}", "123")
            script = script.replace("${{ github.event.workflow_run.run_attempt }}", "2")
            script = script.replace("${{ needs.e2e-gate.result }}", "success")
            script = script.replace("${{ needs.setup.outputs.artifact_source_mode }}", "cross_run")
            script = script.replace("${{ needs.setup.outputs.is_fork }}", "false")
            harness = """
const assert = require('node:assert/strict');
const posted = [];
const context = {serverUrl:'https://github.com', repo:{owner:'test',repo:'test'}, runId:456};
const github = {rest:{repos:{createCommitStatus:async status => {
  posted.push(status);
  if (status.context === process.env.FAIL_CONTEXT) throw new Error('status API unavailable');
}}}};
(async () => {
let failure;
try {
""" + script + """
} catch (error) { failure = error; }
assert.deepEqual(posted.map(status => status.context),
  ['03 Checkpoint - E2E', '03 Checkpoint - E2E / build-123-2']);
assert.ok(posted.every(status => status.state === process.env.EXPECTED_STATE));
assert.equal(Boolean(failure), Boolean(process.env.FAIL_CONTEXT));
})().catch(error => { console.error(error); process.exitCode = 1; });
"""
            for failed_context in ("", "03 Checkpoint - E2E", "03 Checkpoint - E2E / build-123-2"):
                with self.subTest(job=job, failed_context=failed_context):
                    subprocess.run([os.environ.get("NODE_BINARY", "node"), "-e", harness], check=True,
                                   env={**os.environ, "FAIL_CONTEXT": failed_context,
                                        "EXPECTED_STATE": "pending" if job == "set-pending-status" else "success"})


if __name__ == "__main__":
    unittest.main()
