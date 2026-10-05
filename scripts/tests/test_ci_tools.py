import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class DockerEndpointTest(unittest.TestCase):
    def test_api_clients_follow_cli_endpoint_precedence(self):
        with tempfile.TemporaryDirectory() as directory:
            docker = Path(directory) / "docker"
            docker.write_text(
                '#!/bin/sh\n'
                'if [ "$1" = info ]; then exit 0; fi\n'
                'if [ "$3" = chosen ]; then echo unix:///chosen.sock; '
                'else echo unix:///active.sock; fi\n'
            )
            docker.chmod(0o755)
            for overrides, expected in (
                ({}, "unix:///active.sock"),
                ({"DOCKER_HOST": "tcp://explicit:2375"}, "tcp://explicit:2375"),
                ({"DOCKER_CONTEXT": "chosen", "DOCKER_HOST": "unix:///stale.sock"}, "unix:///chosen.sock"),
            ):
                with self.subTest(overrides=overrides):
                    env = os.environ.copy()
                    env.pop("DOCKER_HOST", None)
                    env.pop("DOCKER_CONTEXT", None)
                    env.update(overrides)
                    env["PATH"] = directory + os.pathsep + env["PATH"]
                    result = subprocess.run(
                        ["bash", "-ec", 'source "$1"; configure_docker_environment; printf "%s" "$DOCKER_HOST"',
                         "bash", str(ROOT / "scripts/ci/docker-env.sh")],
                        env=env, capture_output=True, text=True, check=True,
                    )
                    self.assertEqual(result.stdout, expected)

    def test_hook_setup_uses_each_checkout_without_redirecting_other_worktrees(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "first"
            second = Path(directory) / "second"
            root.mkdir()
            subprocess.run(["git", "init", str(root)], check=True, capture_output=True)
            hooks = root / ".githooks"
            hooks.mkdir()
            hook = hooks / "pre-commit"
            hook.write_text('#!/bin/sh\nprintf "%s" "$PWD" > hook-used.txt\n')
            hook.chmod(0o755)
            subprocess.run(["git", "add", ".githooks"], cwd=root, check=True)
            subprocess.run(["bash", str(ROOT / ".githooks/setup.sh")], cwd=root,
                           check=True, capture_output=True)
            commit = ["git", "-c", "user.name=Tooling test", "-c", "user.email=test@example.invalid",
                      "commit", "--allow-empty", "-m", "test hook"]
            subprocess.run(commit, cwd=root, check=True, capture_output=True)
            subprocess.run(["git", "worktree", "add", "-b", "second", str(second)],
                           cwd=root, check=True, capture_output=True)
            subprocess.run(["bash", str(ROOT / ".githooks/setup.sh")], cwd=second,
                           check=True, capture_output=True)
            for checkout in (root, second):
                subprocess.run(commit, cwd=checkout, check=True, capture_output=True)
                self.assertEqual((checkout / "hook-used.txt").read_text(), str(checkout.resolve()))


if __name__ == "__main__":
    unittest.main()
