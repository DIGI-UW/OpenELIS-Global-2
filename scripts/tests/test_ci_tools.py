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
    def test_independent_checks_run_after_a_lane_check_fails(self):
        runner = (ROOT / "scripts/run-ci-checks.sh").read_text()
        start = runner.index("run_job() {") if "run_job() {" in runner else runner.index("run_backend() {")
        functions = runner[start:runner.index("\nrun_e2e_step() {")]
        for lane, expected in (
            ("backend", ("backend-format", "deployment-contract", "agent-assets", "backend-build")),
            ("frontend", ("frontend-static", "frontend-image", "i18n-duplicates", "i18n-source")),
        ):
            with self.subTest(lane=lane), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "checkouts" / lane / "frontend").mkdir(parents=True)
                (root / "checkouts" / lane / "dataexport").mkdir()
                scripts = root / "checkouts" / lane / "scripts"
                scripts.mkdir()
                shim = '#!/bin/sh\ncase "$*" in *spotless:check*|test) exit 9;; esac\nexit 0\n'
                for name in ("npm", "npx", "docker", "python3", "node"):
                    (root / name).write_text(shim)
                    (root / name).chmod(0o755)
                (scripts / "run-java21").write_text(shim)
                (scripts / "run-java21").chmod(0o755)
                (scripts / "ci").mkdir()
                (scripts / "ci/validate-agent-assets.sh").write_text("exit 0\n")
                env = os.environ.copy()
                env.update(PATH=directory + os.pathsep + env["PATH"])
                script = '''ARTIFACT_DIR="$1"
NODE22_BIN="$1/node"
SPECKIT_CHANGED=true
CI_CANDIDATE_IMAGE_PREFIX=owned-test
OE_CI_BASE_SHA=base
OE_CI_SOURCE_BRANCH=feature
HEAD_SHA=candidate
''' + functions + f"\nrun_{lane}\n"
                result = subprocess.run(["bash", "-ec", script, "test", directory],
                                        env=env, capture_output=True, text=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(
                    {path.stem: path.read_text().strip() for path in root.glob("*.status")},
                    {name: "FAIL" if index == 0 else "PASS" for index, name in enumerate(expected)},
                )

    def test_browser_stack_uses_its_workflows_resource_and_scenario_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "scripts").mkdir()
            (root / ".env").touch()
            runner = root / "scripts/run-e2e-like-ci.sh"
            runner.write_text((ROOT / "scripts/run-e2e-like-ci.sh").read_text())
            docker = root / "docker"
            docker.write_text(
                '#!/bin/sh\n'
                'printf "%s\\n" "$@" > "$PARITY_COMMAND_FILE"\n'
                'printf "%s" "$OE_UAT_SCENARIOS_ENABLED" > "$PARITY_SCENARIO_FILE"\n'
                'exit 73\n'
            )
            docker.chmod(0o755)
            for suite, memory_limits, scenarios in (
                ("core", True, "true"),
                ("cypress-core", False, "false"),
                ("cypress-independent", False, "false"),
            ):
                with self.subTest(suite=suite):
                    command_file = root / "command.txt"
                    scenario_file = root / "scenario.txt"
                    env = os.environ.copy()
                    env.update(
                        PATH=directory + os.pathsep + env["PATH"],
                        PARITY_COMMAND_FILE=str(command_file),
                        PARITY_SCENARIO_FILE=str(scenario_file),
                        OE_UAT_SCENARIOS_ENABLED="true",
                    )
                    result = subprocess.run(
                        ["bash", str(runner), "--suite", suite, "--no-build"],
                        cwd=root, env=env, capture_output=True, text=True,
                    )
                    self.assertEqual(result.returncode, 73, result.stderr)
                    command = command_file.read_text().splitlines()
                    self.assertIn(str(root / "build.docker-compose.worktree.yml"), command)
                    self.assertEqual(
                        str(root / ".github/ci/ci.memory-limits.yml") in command,
                        memory_limits,
                    )
                    self.assertEqual(scenario_file.read_text(), scenarios)

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
