"""Validate publication and E2E status contracts."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]


# The github-script step of a job, whichever position it has among the steps.
def github_script(job):
    return next(step for step in job["steps"] if "with" in step and "script" in step["with"])["with"]["script"]


class PublicationWorkflowTest(unittest.TestCase):
    def test_request_builder_passes_configured_namespace_and_rejects_invalid_input(self):
        workflow = yaml.load((ROOT / ".github/workflows/publish-images.yml").read_text(), Loader=yaml.BaseLoader)
        step = next(step for step in workflow["jobs"]["deploy-testing"]["steps"]
                    if step.get("name") == "Validate deployment request")
        images = {service: "test-org/openelis-global-2" + suffix + "@sha256:" + "b" * 64
                  for service, suffix in [("oe.openelis.org", ""), ("db.openelis.org", "-database"),
                                          ("fhir.openelis.org", "-fhir"), ("frontend.openelis.org", "-frontend"),
                                          ("proxy", "-proxy"), ("openelis-analyzer-bridge", "-analyzer-bridge"),
                                          ("astm-simulator", "-analyzer-mock")]}
        env = {**os.environ, "DEPLOY_HOST": "testing.example.org", "DEPLOY_USER": "ubuntu", "DEPLOY_PORT": "22",
               "DOCKERHUB_NAMESPACE": "test-org", "SITE_PATH": "/srv/openelis-testing", "GITHUB_RUN_ID": "123",
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
                    self.assertEqual("/srv/openelis-testing", request["deploy_path"])
                    self.assertEqual(images, request["manifest"]["images"])

    def test_publication_does_not_require_an_external_workflow_interface(self):
        workflow = yaml.load((ROOT / ".github/workflows/publish-images.yml").read_text(), Loader=yaml.BaseLoader)
        for name, job in workflow["jobs"].items():
            with self.subTest(job=name):
                if "uses" in job:
                    self.assertTrue(job["uses"].startswith("./.github/workflows/"),
                                    "External workflow inputs must not prevent publication from loading")

    def test_publication_takes_no_runner_for_builds_it_cannot_publish(self):
        # A pull request build has nothing to publish, and a cancelled build
        # never gets its E2E checkpoint. Both are decided in job conditions, so
        # no runner is taken; a regression here would queue two jobs per pull
        # request build again.
        workflow = yaml.load((ROOT / ".github/workflows/publish-images.yml").read_text(), Loader=yaml.BaseLoader)
        jobs = workflow["jobs"]
        self.assertIn("github.event.workflow_run.event != 'pull_request'", jobs["setup"]["if"])
        self.assertIn("github.event_name == 'workflow_dispatch'", jobs["setup"]["if"])
        self.assertIn("github.event.workflow_run.conclusion != 'cancelled'", jobs["wait-for-e2e-checkpoint"]["if"])
        self.assertIn("needs.setup.result != 'skipped'", jobs["deployment-summary"]["if"])

    def test_reporter_posts_the_build_status_consumed_by_publication(self):
        workflow = yaml.load((ROOT / ".github/workflows/e2e-tests.yml").read_text(), Loader=yaml.BaseLoader)
        for job in ("set-pending-status", "report-status"):
            script = github_script(workflow["jobs"][job])
            script = script.replace("${{ github.event.workflow_run.id }}", "123")
            script = script.replace("${{ github.event.workflow_run.run_attempt }}", "2")
            script = script.replace("${{ needs.e2e-gate.result }}", "success")
            script = script.replace("${{ needs.setup.outputs.artifact_source_mode }}", "cross_run")
            script = script.replace("${{ needs.setup.outputs.is_fork }}", "false")
            script = script.replace("${{ needs.setup.outputs.skip_kind }}", "")
            script = script.replace("${{ toJSON(needs.setup.outputs.skip_description) }}", '""')
            script = script.replace("${{ needs.setup.outputs.stack_top }}", "false")
            script = script.replace("${{ needs.setup.outputs.stack_number }}", "")
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
                    subprocess.run([os.environ.get("NODE_BINARY", "node"), "-e", harness], check=True, cwd=ROOT,
                                   env={**os.environ, "FAIL_CONTEXT": failed_context,
                                        "EXPECTED_STATE": "pending" if job == "set-pending-status" else "success"})

    def test_reporter_holds_lower_stack_members_until_the_top_reports_to_them(self):
        workflow = yaml.load((ROOT / ".github/workflows/e2e-tests.yml").read_text(), Loader=yaml.BaseLoader)
        script = github_script(workflow["jobs"]["report-status"])
        # The fake API: posted statuses are printed as JSON; the stack's open pull
        # requests and each compare result come from the environment.
        harness = """
const posted = [];
const context = {serverUrl:'https://github.com', repo:{owner:'test',repo:'test'}, runId:456};
const core = {info: () => {}};
const members = JSON.parse(process.env.MEMBERS || '[]');
const compares = JSON.parse(process.env.COMPARES || '{}');
const github = {
  paginate: async () => members,
  rest: {
    pulls: {list: 'list'},
    repos: {
      createCommitStatus: async status => { posted.push(status); },
      compareCommitsWithBasehead: async ({basehead}) => ({data: {status: compares[basehead.split('...')[0]] || 'diverged'}}),
    },
  },
};
(async () => {
__SCRIPT__
process.stdout.write(JSON.stringify(posted));
})().catch(error => { console.error(error); process.exitCode = 1; });
"""
        main = "03 Checkpoint - E2E"
        build = "03 Checkpoint - E2E / build-123-2"
        passed = "All Playwright, Cypress, and analyzer-harness suites passed"
        waiting = "Stacked PR 3 of 20 (stack #4533); waiting for the top of the stack to run E2E"
        members = json.dumps([
            {"number": 4531, "head": {"sha": "low1"}, "stack": {"number": 4533, "position": 1, "size": 20}},
            {"number": 4540, "head": {"sha": "stale"}, "stack": {"number": 4533, "position": 10, "size": 20}},
            {"number": 4552, "head": {"sha": "topsha"}, "stack": {"number": 4533, "position": 20, "size": 20}},
        ])
        compares = json.dumps({"low1": "ahead", "stale": "diverged"})
        cases = [
            ("documentation-only pull request",
             {"gate": "success", "mode": "skipped", "kind": "docs", "description": '"Documentation-only change; E2E suites skipped"',
              "top": "false", "stack": "", "members": "[]"},
             [("topsha", main, "success", "Documentation-only change; E2E suites skipped"),
              ("topsha", build, "success", "Documentation-only change; E2E suites skipped")]),
            ("pull request below the top of a stack",
             {"gate": "success", "mode": "skipped", "kind": "stack", "description": json.dumps(waiting),
              "top": "false", "stack": "", "members": "[]"},
             [("topsha", main, "pending", waiting), ("topsha", build, "success", waiting)]),
            ("top of a stack passes",
             {"gate": "success", "mode": "cross_run", "kind": "", "description": '""',
              "top": "true", "stack": "4533", "members": members},
             [("topsha", main, "success", passed), ("topsha", build, "success", passed),
              ("low1", main, "success", f"Top of stack #4533: {passed}")]),
            ("top of a stack fails",
             {"gate": "failure", "mode": "cross_run", "kind": "", "description": '""',
              "top": "true", "stack": "4533", "members": members},
             [("topsha", main, "failure", "One or more E2E suites failed"),
              ("topsha", build, "failure", "One or more E2E suites failed"),
              ("low1", main, "failure", "Top of stack #4533: One or more E2E suites failed")]),
        ]
        for label, values, expected in cases:
            with self.subTest(case=label):
                rendered = (script
                            .replace("${{ github.event.workflow_run.id }}", "123")
                            .replace("${{ github.event.workflow_run.run_attempt }}", "2")
                            .replace("${{ needs.setup.outputs.status_sha }}", "topsha")
                            .replace("${{ needs.setup.outputs.is_fork }}", "false")
                            .replace("${{ needs.e2e-gate.result }}", values["gate"])
                            .replace("${{ needs.setup.outputs.artifact_source_mode }}", values["mode"])
                            .replace("${{ needs.setup.outputs.skip_kind }}", values["kind"])
                            .replace("${{ toJSON(needs.setup.outputs.skip_description) }}", values["description"])
                            .replace("${{ needs.setup.outputs.stack_top }}", values["top"])
                            .replace("${{ needs.setup.outputs.stack_number }}", values["stack"]))
                self.assertNotIn("${{", rendered, "every workflow expression in the reporter must be substituted here")
                result = subprocess.run([os.environ.get("NODE_BINARY", "node"), "-e", harness.replace("__SCRIPT__", rendered)],
                                        check=True, cwd=ROOT, capture_output=True, text=True,
                                        env={**os.environ, "MEMBERS": values["members"], "COMPARES": compares})
                posted = [(s["sha"], s["context"], s["state"], s["description"]) for s in json.loads(result.stdout)]
                self.assertEqual(posted, expected)


if __name__ == "__main__":
    unittest.main()
