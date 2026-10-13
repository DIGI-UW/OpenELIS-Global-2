import base64
import copy
import json
import urllib.error
import yaml
import importlib.util
import io
from contextlib import redirect_stdout
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location("submodule_pins", Path(__file__).with_name("submodule_pins.py"))
pins = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pins)

LS_TREE = (
    "160000 commit 8c64750685aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\ttools/analyzer-mock-server\n"
    "160000 commit 5583a7274ebbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\ttools/openelis-analyzer-bridge\n"
    "160000 commit bc1c0d4967cccccccccccccccccccccccccccccccc\tdataexport\n"
    "100644 blob 1111111111111111111111111111111111111111\tpom.xml\n"
)

GITMODULES = """
[submodule "tools/openelis-analyzer-bridge"]
	path = tools/openelis-analyzer-bridge
	url = https://github.com/DIGI-UW/openelis-analyzer-bridge.git
	branch = develop
[submodule "tools/analyzer-mock-server"]
	path = tools/analyzer-mock-server
	url = git@github.com:DIGI-UW/analyzer-mock-server
"""


class SubmodulePinsTest(unittest.TestCase):
    def test_reads_only_the_checked_submodules_and_their_repositories(self):
        checked = pins.checked_pins(LS_TREE, GITMODULES)

        self.assertEqual(
            checked,
            [
                ("tools/openelis-analyzer-bridge", "5583a7274ebbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "DIGI-UW/openelis-analyzer-bridge"),
                ("tools/analyzer-mock-server", "8c64750685aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "DIGI-UW/analyzer-mock-server"),
            ],
        )

    def test_a_pin_not_on_the_default_branch_is_reported(self):
        merged = {"5583a7274ebbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}
        checked = pins.checked_pins(LS_TREE, GITMODULES)

        unmerged = pins.unmerged(checked, lambda repository, sha: sha in merged)

        self.assertEqual([path for path, _, _ in unmerged], ["tools/analyzer-mock-server"])

    def test_identical_and_behind_mean_merged(self):
        self.assertTrue(pins.merged_status("identical"))
        self.assertTrue(pins.merged_status("behind"))
        self.assertFalse(pins.merged_status("ahead"))
        self.assertFalse(pins.merged_status("diverged"))

    def test_a_checked_submodule_missing_from_the_tree_is_an_error(self):
        with self.assertRaises(ValueError):
            pins.checked_pins(LS_TREE.replace("tools/analyzer-mock-server", "tools/renamed"), GITMODULES)

    def run_validation(self, gitmodules, statuses=("identical", "behind")):
        responses = []
        for status in statuses:
            responses.extend([{"default_branch": "develop"}, {"status": status}])
        output = io.StringIO()
        with patch.object(pins, "trusted_gitmodules", return_value=GITMODULES), patch.object(
            pins.subprocess, "run", side_effect=[Mock(stdout=LS_TREE), Mock(stdout=gitmodules)]
        ), patch.object(pins, "_github", side_effect=responses) as github, redirect_stdout(output):
            result = pins.main()
        return result, output.getvalue(), github

    def test_redirected_repository_is_rejected_before_checking_pins(self):
        for repository in ("DIGI-UW/openelis-analyzer-bridge", "DIGI-UW/analyzer-mock-server"):
            with self.subTest(repository=repository):
                result, output, github = self.run_validation(GITMODULES.replace(repository, "someone/else"))
                self.assertEqual(1, result)
                self.assertIn(repository, output)
                github.assert_not_called()

    def test_equivalent_https_and_ssh_urls_are_accepted(self):
        for url in (
            "https://github.com/DIGI-UW/openelis-analyzer-bridge",
            "git@github.com:DIGI-UW/openelis-analyzer-bridge.git",
            "ssh://git@github.com/DIGI-UW/openelis-analyzer-bridge.git",
            "https://github.com/digi-uw/OPENELIS-ANALYZER-BRIDGE.git",
        ):
            with self.subTest(url=url):
                result, _, github = self.run_validation(
                    GITMODULES.replace("https://github.com/DIGI-UW/openelis-analyzer-bridge.git", url)
                )
                self.assertEqual(0, result)
                self.assertEqual("repos/DIGI-UW/openelis-analyzer-bridge", github.call_args_list[0].args[0])

    def test_other_hosts_and_ambiguous_urls_are_rejected(self):
        for url in (
            "https://github.com.example.org/DIGI-UW/openelis-analyzer-bridge.git",
            "https://github.com@evil.example/DIGI-UW/openelis-analyzer-bridge.git",
            "https://example.org/DIGI-UW/openelis-analyzer-bridge.git",
            "../openelis-analyzer-bridge.git",
            "https://github.com/DIGI-UW/openelis-analyzer-bridge.git?redirect=elsewhere",
        ):
            with self.subTest(url=url):
                result, output, github = self.run_validation(
                    GITMODULES.replace("https://github.com/DIGI-UW/openelis-analyzer-bridge.git", url)
                )
                self.assertEqual(1, result)
                self.assertIn("tools/openelis-analyzer-bridge", output)
                github.assert_not_called()

    def test_unchanged_repositories_still_enforce_merged_pins(self):
        for statuses, expected in (
            (("identical", "behind"), 0),
            (("ahead", "behind"), 1),
            (("identical", "diverged"), 1),
        ):
            with self.subTest(statuses=statuses):
                result, output, github = self.run_validation(GITMODULES, statuses)
                self.assertEqual(expected, result)
                self.assertEqual(4, github.call_count)
                self.assertEqual(expected == 1, "NOT MERGED" in output)

    def test_repositories_come_from_the_base_branch_not_the_pull_request(self):
        with tempfile.TemporaryDirectory() as repo:
            def git(*args):
                subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True)

            git("init", "-q", "-b", "develop")
            git("config", "user.email", "ci@example.org")
            git("config", "user.name", "ci")
            Path(repo, ".gitmodules").write_text(GITMODULES)
            git("add", ".gitmodules")
            for path, sha, _ in pins.checked_pins(LS_TREE, GITMODULES):
                git("update-index", "--add", "--cacheinfo", "160000", sha.ljust(40, "0"), path)
            git("commit", "-q", "-m", "base")
            git("remote", "add", "origin", repo)
            git("checkout", "-q", "-b", "pull-request")
            Path(repo, ".gitmodules").write_text(GITMODULES.replace("DIGI-UW/openelis-analyzer-bridge", "someone/else"))
            git("add", ".gitmodules")
            git("commit", "-q", "-m", "redirect the Bridge")

            here = os.getcwd()
            os.chdir(repo)
            try:
                trusted = pins.trusted_gitmodules("develop")
                untrusted = pins.trusted_gitmodules(None)
                output = io.StringIO()
                with patch.dict(os.environ, {"BASE_REF": "develop"}), patch.object(
                    pins, "_github", side_effect=AssertionError("Redirected URLs must fail before API requests")
                ), redirect_stdout(output):
                    result = pins.main()
            finally:
                os.chdir(here)

        self.assertIn("DIGI-UW/openelis-analyzer-bridge", trusted)
        self.assertNotIn("someone/else", trusted)
        self.assertIn("someone/else", untrusted)
        self.assertEqual(1, result)
        self.assertIn("tools/openelis-analyzer-bridge must reference DIGI-UW/openelis-analyzer-bridge", output.getvalue())


class TrustedPullRequestTest(unittest.TestCase):
    repository = "DIGI-UW/OpenELIS-Global-2"
    sha = "a" * 40
    run_url = "https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/123"

    def setUp(self):
        self.prefix = f"repos/{self.repository}"
        self.responses = {
            f"{self.prefix}/pulls/4655": {
                "head": {"sha": self.sha, "repo": {"full_name": "contributor/fork"}},
                "base": {"ref": "develop"}, "stack": None,
            },
            f"{self.prefix}/git/trees/{self.sha}": {"tree": [
                {"path": ".gitmodules", "sha": "modules", "type": "blob", "mode": "100644"},
                {"path": "tools", "sha": "tools", "type": "tree", "mode": "040000"},
                # A PR may replace/delete policy files: none may be fetched or executed.
                {"path": ".github", "sha": "untrusted-policy", "type": "tree", "mode": "040000"},
            ]},
            f"{self.prefix}/git/blobs/modules": {
                "encoding": "base64", "content": base64.b64encode(GITMODULES.encode()).decode(),
            },
            f"{self.prefix}/git/trees/tools": {"tree": []},
        }
        for path, sha, repository in pins.checked_pins(LS_TREE, GITMODULES):
            self.responses[f"{self.prefix}/git/trees/tools"]["tree"].append({
                "path": path.split("/")[-1], "sha": sha, "type": "commit", "mode": "160000",
            })
            self.responses[f"repos/{repository}"] = {"default_branch": "develop"}
            self.responses[f"repos/{repository}/compare/develop...{sha}"] = {"status": "behind"}
        self.posts = []
        self.reads = []

    def api(self, path, token, data=None):
        if data is not None:
            self.posts.append((path, data))
            return {}
        self.reads.append(path)
        value = self.responses[path]
        if isinstance(value, Exception):
            raise value
        return copy.deepcopy(value)

    def run_check(self, dry_run=False):
        with patch.object(pins, "_github", side_effect=self.api), patch.object(
            pins.subprocess, "run", side_effect=AssertionError("PR validation must not execute Git or PR code")
        ), redirect_stdout(io.StringIO()):
            return pins.check_pull_request(self.repository, 4655, "token", GITMODULES, self.run_url, dry_run)

    def test_fork_head_is_read_as_data_and_success_is_posted_to_its_exact_commit(self):
        self.assertEqual(0, self.run_check())
        self.assertEqual(["pending", "success"], [data["state"] for _, data in self.posts])
        for path, data in self.posts:
            self.assertEqual(f"{self.prefix}/statuses/{self.sha}", path)
            self.assertEqual(pins.STATUS_CONTEXT, data["context"])
            self.assertEqual(self.run_url, data["target_url"])
        self.assertNotIn(f"{self.prefix}/git/trees/untrusted-policy", self.reads)
        self.assertEqual(1, self.reads.count(f"{self.prefix}/git/trees/tools"))

    def test_modified_pr_policy_cannot_make_an_unmerged_pin_pass(self):
        compare = next(path for path in self.responses if "/compare/" in path)
        self.responses[compare] = {"status": "ahead"}
        self.assertEqual(1, self.run_check())
        self.assertEqual("failure", self.posts[-1][1]["state"])
        self.assertIn("Unmerged upstream pin", self.posts[-1][1]["description"])
        self.assertFalse(any("untrusted-policy" in path for path in self.reads))

    def test_redirected_url_fails_without_comparing_upstream(self):
        modules = GITMODULES.replace("DIGI-UW/openelis-analyzer-bridge", "someone/else")
        self.responses[f"{self.prefix}/git/blobs/modules"]["content"] = base64.b64encode(modules.encode()).decode()
        self.assertEqual(1, self.run_check())
        self.assertEqual("failure", self.posts[-1][1]["state"])
        self.assertFalse(any("/compare/" in path for path in self.reads))

    def test_develop_based_stack_is_checked_even_when_immediate_base_is_feature_branch(self):
        pr = self.responses[f"{self.prefix}/pulls/4655"]
        pr["base"]["ref"] = "feature/parent"
        pr["stack"] = {"base": {"ref": "develop"}}
        self.assertEqual(0, self.run_check())
        self.assertTrue(any("/compare/" in path for path in self.reads))

    def test_other_target_branches_get_explicit_not_applicable_result(self):
        self.responses[f"{self.prefix}/pulls/4655"]["base"]["ref"] = "release"
        self.assertEqual(0, self.run_check())
        self.assertIn("Not applicable", self.posts[-1][1]["description"])
        self.assertEqual([f"{self.prefix}/pulls/4655"], self.reads)

    def test_api_errors_fail_closed_with_diagnostics(self):
        compare = next(path for path in self.responses if "/compare/" in path)
        for code in (401, 403, 404, 429, 500):
            with self.subTest(code=code):
                self.responses[compare] = urllib.error.HTTPError("https://api.github.com/compare", code, "error", {}, None)
                self.assertEqual(1, self.run_check())
                self.assertEqual("failure", self.posts[-1][1]["state"])
                self.assertIn(f"HTTP {code}", self.posts[-1][1]["description"])
                self.responses[compare].close()
        self.responses[compare] = urllib.error.URLError("connection unavailable")
        self.assertEqual(1, self.run_check())
        self.assertEqual("failure", self.posts[-1][1]["state"])

    def test_missing_pin_incomplete_tree_and_symlinked_modules_fail_closed(self):
        original = copy.deepcopy(self.responses)
        for corruption in ("missing pin", "truncated", "symlink"):
            with self.subTest(corruption=corruption):
                self.responses = copy.deepcopy(original)
                if corruption == "missing pin":
                    self.responses[f"{self.prefix}/git/trees/tools"]["tree"].pop()
                elif corruption == "truncated":
                    self.responses[f"{self.prefix}/git/trees/{self.sha}"]["truncated"] = True
                else:
                    self.responses[f"{self.prefix}/git/trees/{self.sha}"]["tree"][0]["mode"] = "120000"
                self.assertEqual(1, self.run_check())
                self.assertEqual("failure", self.posts[-1][1]["state"])

    def test_dry_run_checks_pins_without_writing_statuses(self):
        self.assertEqual(0, self.run_check(dry_run=True))
        self.assertEqual([], self.posts)
        self.assertTrue(any("/compare/" in path for path in self.reads))

    def test_policy_workflow_executes_only_develop_code(self):
        workflow = Path(__file__).parents[1] / "workflows" / "submodule-pins.yml"
        config = yaml.load(workflow.read_text(), Loader=yaml.BaseLoader)
        self.assertIn("pull_request_target", config["on"])
        self.assertNotIn("pull_request", config["on"])
        steps = config["jobs"]["validate"]["steps"]
        checkouts = [step for step in steps if step.get("uses", "").startswith("actions/checkout@")]
        self.assertEqual(1, len(checkouts))
        self.assertEqual("develop", checkouts[0]["with"]["ref"])
        self.assertEqual("false", checkouts[0]["with"]["persist-credentials"])
        self.assertNotIn("submodules", checkouts[0]["with"])
        self.assertEqual({"contents": "read", "pull-requests": "read", "statuses": "write"}, config["permissions"])

    def test_github_transport_posts_json_with_authorization(self):
        response = Mock()
        response.__enter__ = Mock(return_value=io.StringIO('{"state":"success"}'))
        response.__exit__ = Mock(return_value=False)
        with patch.object(pins.urllib.request, "urlopen", return_value=response) as request:
            self.assertEqual({"state": "success"}, pins._github("repos/owner/repo/statuses/sha", "test-token", {"state": "success"}))
        sent = request.call_args.args[0]
        self.assertEqual("POST", sent.get_method())
        self.assertEqual("https://api.github.com/repos/owner/repo/statuses/sha", sent.full_url)
        self.assertEqual("Bearer test-token", sent.get_header("Authorization"))
        self.assertEqual({"state": "success"}, json.loads(sent.data))


if __name__ == "__main__":
    unittest.main()
