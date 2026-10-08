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


if __name__ == "__main__":
    unittest.main()
