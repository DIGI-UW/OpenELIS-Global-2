import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

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

    def test_repositories_come_from_the_base_branch_not_the_pull_request(self):
        with tempfile.TemporaryDirectory() as repo:
            def git(*args):
                subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True)

            git("init", "-q", "-b", "develop")
            git("config", "user.email", "ci@example.org")
            git("config", "user.name", "ci")
            Path(repo, ".gitmodules").write_text(GITMODULES)
            git("add", ".gitmodules")
            git("commit", "-q", "-m", "base")
            git("remote", "add", "origin", repo)
            git("checkout", "-q", "-b", "pull-request")
            Path(repo, ".gitmodules").write_text(GITMODULES.replace("DIGI-UW/openelis-analyzer-bridge", "someone/else"))
            git("commit", "-q", "-am", "redirect the Bridge")

            here = os.getcwd()
            os.chdir(repo)
            try:
                trusted = pins.trusted_gitmodules("develop")
                untrusted = pins.trusted_gitmodules(None)
            finally:
                os.chdir(here)

        self.assertIn("DIGI-UW/openelis-analyzer-bridge", trusted)
        self.assertNotIn("someone/else", trusted)
        self.assertIn("someone/else", untrusted)


if __name__ == "__main__":
    unittest.main()
