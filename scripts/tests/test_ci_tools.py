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


class BrowserPlanTest(unittest.TestCase):
    def test_browser_lane_executes_workflow_jobs_and_reports_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            (Path(directory) / "checkouts/e2e").mkdir(parents=True)
            subprocess.run(["python3", str(ROOT / "scripts/ci/e2e-plan.py"), directory],
                           check=True, cwd=ROOT)
            rows = [line.split("\t") for line in
                    (Path(directory) / "e2e-jobs.tsv").read_text().splitlines()]
            runner = (ROOT / "scripts/run-ci-checks.sh").read_text()
            body = runner[runner.index("run_e2e() {"):runner.index("\npids=()")]
            # Exercise the real shell loop without building images or starting stacks.
            script = '''run_e2e_step() {
  printf '%s\n' "$*"
  # Browser tools can read stdin; they must not consume the job inventory.
  [[ "$1" != core-playwright-1 ]] || cat >/dev/null
  [[ "$1" != analyzer-2 ]]
}
ARTIFACT_DIR="$1"
''' + body + "\nrun_e2e\n"
            result = subprocess.run(["bash", "-c", script, "test", directory],
                                    cwd=ROOT, input="", capture_output=True, text=True)
            self.assertEqual(result.returncode, 1, result.stderr)
            commands = result.stdout.splitlines()[3:]
            self.assertEqual([line.split()[0] for line in commands], [row[0] for row in rows])
            analyzer = next(line for line in commands if line.startswith("analyzer-1 "))
            self.assertIn("--project harness-foundational --project harness-demo", analyzer)
            self.assertIn("--shard 1/2", analyzer)


if __name__ == "__main__":
    unittest.main()
