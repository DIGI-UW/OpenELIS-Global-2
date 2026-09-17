import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import test_readiness

spec = importlib.util.spec_from_file_location("deployment", Path(__file__).with_name("deploy-published-testing.py"))
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


class DeploymentTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / ".env").write_text("SERVER_SECRET=keep-existing-value\n")
        self.diagnostics = self.root / "diagnostics"
        self.diagnostics.mkdir()
        self.target = self.root / ".openelis-ci/target.json"
        self.target.parent.mkdir(parents=True)
        self.target.write_text('{"deploymentId":"previous-ready"}')
        self.override = self.target.with_name("deployment-images.json")
        self.previous_override = json.dumps({"services": {
            service: {"image": "itechuw/" + repository + "@sha256:" + "d" * 64}
            for service, repository in deployment.SERVICES.items()}})
        self.override.write_text(self.previous_override)
        self.sha = "a" * 40
        self.request = {"manifest": {"appSha": self.sha, "appBranch": "develop", "images": {
            service: "itechuw/" + repository + "@sha256:" + "b" * 64 for service, repository in deployment.SERVICES.items()}},
            "deploy_path": str(self.root), "run_id": "123-1",
            "readiness": {"url": "http://127.0.0.1:1/health", "timeout": 0.02, "interval": 0.01}}
        self.wrong_image = False
        self.branch_head = self.sha
        self.commands = []
        self.port_owner = str(self.root)

    def command(self, args, _cwd, capture=False):
        self.commands.append(args)
        if args[:2] == ["git", "ls-remote"]:
            return self.branch_head + "\trefs/heads/develop\n"
        if args == ["git", "rev-parse", "HEAD"]:
            return self.sha
        if args[:2] == ["docker", "ps"]:
            return "proxy-id"
        if args == ["docker", "inspect", "proxy-id"]:
            return json.dumps([{"Config": {"Labels": {
                "com.docker.compose.project.working_dir": self.port_owner}}}])
        if "ps" in args:
            return "container-id"
        if args[:2] == ["docker", "inspect"]:
            return json.dumps([{"State": {"Running": True}, "Image": "wrong" if self.wrong_image else "sha256:actual"}])
        if args[:3] == ["docker", "image", "inspect"]:
            return json.dumps([{"Id": "sha256:actual"}])
        return ""

    def test_mutable_missing_and_foreign_images_are_rejected(self):
        manifest = self.request["manifest"]
        for value in ["itechuw/openelis-global-2:develop", "attacker/image@sha256:" + "b" * 64]:
            manifest["images"]["oe.openelis.org"] = value
            with self.assertRaises(ValueError):
                deployment.validate_manifest(manifest)
        del manifest["images"]["oe.openelis.org"]
        with self.assertRaises(ValueError):
            deployment.validate_manifest(manifest)

    def test_null_and_wrong_manifest_types_are_validation_errors(self):
        valid = self.request["manifest"]
        invalid = [None, [], {**valid, "appSha": None}, {**valid, "appSha": 123},
                   {**valid, "images": None}, {**valid, "images": list(deployment.SERVICES)},
                   {**valid, "images": {**valid["images"], "proxy": None}}]
        for manifest in invalid:
            with self.subTest(manifest=manifest), self.assertRaises(ValueError):
                deployment.validate_manifest(manifest)

    def test_configured_namespace_is_required_for_all_images(self):
        manifest = self.request["manifest"]
        manifest["images"] = {service: reference.replace("itechuw/", "test-org/")
                              for service, reference in manifest["images"].items()}
        self.assertEqual(manifest, deployment.validate_manifest(manifest, "test-org"))
        with self.assertRaises(ValueError):
            deployment.validate_manifest(manifest)
        manifest["images"]["proxy"] = "itechuw/openelis-global-2-proxy@sha256:" + "b" * 64
        with self.assertRaises(ValueError):
            deployment.validate_manifest(manifest, "test-org")

    def test_competing_stack_is_rejected_before_updating_configuration(self):
        for owner in ("/other/analyzer-harness", None):
            with self.subTest(owner=owner):
                self.port_owner = owner
                self.commands.clear()
                with patch.object(deployment, "run", side_effect=self.command), patch.object(deployment.subprocess, "run"):
                    with self.assertRaisesRegex(ValueError, "another Docker stack"):
                        deployment.deploy(self.request, self.diagnostics)
                self.assertFalse(any("fetch" in command or "pull" in command or "up" in command
                                     for command in self.commands))
                self.assertEqual(self.previous_override, self.override.read_text())

    def test_stack_owner_is_rechecked_after_pull(self):
        def change_owner(args, cwd, capture=False):
            result = self.command(args, cwd, capture)
            if "pull" in args:
                self.port_owner = "/other/analyzer-harness"
            return result
        with patch.object(deployment, "run", side_effect=change_owner), patch.object(deployment.subprocess, "run"):
            with self.assertRaisesRegex(ValueError, "another Docker stack"):
                deployment.deploy(self.request, self.diagnostics)
        self.assertFalse(any("up" in command for command in self.commands))
        self.assertEqual(self.previous_override, self.override.read_text())

    def test_invalid_readiness_contract_is_rejected_before_deployment(self):
        self.request["readiness"]["timeout"] = 0
        with patch.object(deployment, "run") as command:
            with self.assertRaises(ValueError):
                deployment.deploy(self.request, self.diagnostics)
            command.assert_not_called()

    def test_old_build_cannot_overwrite_a_newer_deployment(self):
        self.branch_head = "c" * 40
        with patch.object(deployment, "run", side_effect=self.command), patch.object(deployment.subprocess, "run"):
            with self.assertRaisesRegex(ValueError, "obsolete deployment"):
                deployment.deploy(self.request, self.diagnostics)
        self.assertFalse(any("up" in command or "pull" in command for command in self.commands))
        self.assertEqual("previous-ready", json.loads(self.target.read_text())["deploymentId"])
        self.assertTrue((self.diagnostics / "compose-status.txt").exists())

    def test_candidate_is_rechecked_after_image_pull_before_restart(self):
        def advance_during_pull(args, cwd, capture=False):
            result = self.command(args, cwd, capture)
            if "pull" in args:
                self.branch_head = "c" * 40
            return result

        for existing_override in (True, False):
            with self.subTest(existing_override=existing_override):
                if not existing_override:
                    self.override.unlink()
                self.branch_head = self.sha
                self.commands.clear()
                with patch.object(deployment, "run", side_effect=advance_during_pull), patch.object(deployment.subprocess, "run"):
                    with self.assertRaisesRegex(ValueError, "obsolete deployment"):
                        deployment.deploy(self.request, self.diagnostics)
                self.assertFalse(any("up" in command for command in self.commands))
                self.assertEqual("previous-ready", json.loads(self.target.read_text())["deploymentId"])
                if existing_override:
                    self.assertEqual(self.previous_override, self.override.read_text())
                else:
                    self.assertFalse(self.override.exists())
                self.assertEqual({"target.json"} | ({"deployment-images.json"} if existing_override else set()),
                                 {path.name for path in self.target.parent.iterdir()})

    def test_failed_image_pull_preserves_the_previous_override(self):
        def fail_pull(args, cwd, capture=False):
            result = self.command(args, cwd, capture)
            if "pull" in args:
                raise deployment.subprocess.CalledProcessError(1, args)
            return result

        with patch.object(deployment, "run", side_effect=fail_pull), patch.object(deployment.subprocess, "run"):
            with self.assertRaises(deployment.subprocess.CalledProcessError):
                deployment.deploy(self.request, self.diagnostics)
        self.assertFalse(any("up" in command for command in self.commands))
        self.assertEqual(self.previous_override, self.override.read_text())
        self.assertEqual("previous-ready", json.loads(self.target.read_text())["deploymentId"])
        self.assertEqual({"target.json", "deployment-images.json"},
                         {path.name for path in self.target.parent.iterdir()})

    def test_failed_startup_withdraws_stale_ready_identity_and_keeps_it_in_diagnostics(self):
        with patch.object(deployment, "run", side_effect=self.command), patch.object(deployment.subprocess, "run") as diagnostics:
            with self.assertRaisesRegex(RuntimeError, "did not become ready"):
                deployment.deploy(self.request, self.diagnostics)
        self.assertFalse(self.target.exists())
        self.assertEqual("previous-ready",
                         json.loads((self.diagnostics / "previous-target.json").read_text())["deploymentId"])
        self.assertFalse(json.loads((self.diagnostics / "readiness.json").read_text())["ready"])
        self.assertTrue((self.diagnostics / "compose-status.txt").exists())
        self.assertTrue((self.diagnostics / "service-logs.txt").exists())
        self.assertEqual(self.previous_override, (self.diagnostics / "previous-images.json").read_text())
        log_command = next(call.args[0] for call in diagnostics.call_args_list if "logs" in call.args[0])
        self.assertTrue(set(deployment.SERVICES).issubset(log_command))
        self.assertIn("keep-existing-value", (self.root / ".env").read_text())

    def test_wrong_running_image_cannot_publish_ready(self):
        self.wrong_image = True
        with patch.object(deployment, "run", side_effect=self.command), patch.object(deployment.subprocess, "run"):
            with self.assertRaisesRegex(ValueError, "does not match"):
                deployment.deploy(self.request, self.diagnostics)
        self.assertFalse(self.target.exists())

    def test_ready_identity_requires_both_running_digests_and_health(self):
        test_readiness.ReadinessTest.setUpClass()
        self.addCleanup(test_readiness.ReadinessTest.tearDownClass)
        test_readiness.ReadinessTest.server.response = (200, "application/json", b'{"status": "UP"}')
        self.request["readiness"].update(url=test_readiness.ReadinessTest.url, timeout=1)
        expected_override = {"services": {service: {"image": reference}
                                          for service, reference in self.request["manifest"]["images"].items()}}

        def check_selected_images(args, cwd, capture=False):
            if "pull" in args or "up" in args:
                selected_override = Path(args[args.index("-f", 4) + 1])
                self.assertEqual(expected_override, json.loads(selected_override.read_text()))
                if "pull" in args:
                    self.assertEqual(self.previous_override, self.override.read_text())
                else:
                    self.assertEqual(self.override, selected_override)
            return self.command(args, cwd, capture)

        with patch.object(deployment, "run", side_effect=check_selected_images), patch.object(deployment.subprocess, "run"):
            deployment.deploy(self.request, self.diagnostics)
        target = json.loads(self.target.read_text())
        self.assertEqual(self.sha, target["appSha"])
        self.assertEqual(5, len(target["images"]))
        self.assertTrue(target["verification"]["readiness"]["ready"])
        self.assertEqual(expected_override, json.loads(self.override.read_text()))
        self.assertEqual({"target.json", "deployment-images.json"},
                         {path.name for path in self.target.parent.iterdir()})


class InfrastructureUpdateTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.upstream = self.root / "upstream"
        self.upstream.mkdir()
        self.git(self.upstream, "init", "-b", "main")
        (self.upstream / ".env").write_text("OE_CERTS_PATH=upstream-default\n")
        (self.upstream / "docker-compose.yml").write_text("services: {}\n")
        self.commit(self.upstream, "initial")
        self.app = self.root / "app"
        self.git(self.root, "clone", str(self.upstream), str(self.app))
        self.env = self.app / ".env"
        self.local_env = b"OE_CERTS_PATH=/site/certs\r\nSERVER_SECRET=preserve-exactly\r\n"
        self.env.write_bytes(self.local_env)
        self.env.chmod(0o640)
        self.state = self.app / ".openelis-ci"
        self.state.mkdir()
        (self.upstream / ".env").write_text("OE_CERTS_PATH=new-default\nNEW_DEFAULT=true\n")
        (self.upstream / "docker-compose.yml").write_text("services: {}\n# updated\n")
        self.commit(self.upstream, "new defaults")

    def git(self, directory, *args):
        return subprocess.check_output(["git", "-c", "user.name=Deployment Test", "-c",
                                        "user.email=deployment@example.invalid", "-c", "commit.gpgsign=false",
                                        *args], cwd=directory, text=True, stderr=subprocess.PIPE,
                                       env={**os.environ, "GIT_CONFIG_GLOBAL": "/dev/null", "GIT_CONFIG_NOSYSTEM": "1"})

    def commit(self, directory, message):
        self.git(directory, "add", ".env", "docker-compose.yml")
        self.git(directory, "commit", "-m", message)

    def test_fast_forward_preserves_site_env_when_upstream_changes_it(self):
        deployment.update_infrastructure(self.app, self.state)
        self.assertEqual(self.local_env, self.env.read_bytes())
        self.assertEqual(0o640, self.env.stat().st_mode & 0o777)
        self.assertEqual(self.git(self.upstream, "rev-parse", "HEAD"), self.git(self.app, "rev-parse", "HEAD"))
        self.assertIn("# updated", (self.app / "docker-compose.yml").read_text())
        self.assertFalse((self.state / "server.env.backup").exists())
        deployment.update_infrastructure(self.app, self.state)
        self.assertEqual(self.local_env, self.env.read_bytes())

    def test_merge_failure_restores_env_and_preserves_other_local_edits(self):
        (self.app / "docker-compose.yml").write_text("services: {}\n# local change\n")
        with self.assertRaises(subprocess.CalledProcessError):
            deployment.update_infrastructure(self.app, self.state)
        self.assertEqual(self.local_env, self.env.read_bytes())
        self.assertIn("# local change", (self.app / "docker-compose.yml").read_text())
        self.assertFalse((self.state / "server.env.backup").exists())

    def test_interrupted_update_backup_is_never_overwritten(self):
        backup = self.state / "server.env.backup"
        backup.write_bytes(b"previous configuration")
        with self.assertRaisesRegex(ValueError, "interrupted update"), patch.object(deployment, "run") as run:
            deployment.update_infrastructure(self.app, self.state)
        run.assert_not_called()
        self.assertEqual(b"previous configuration", backup.read_bytes())
        self.assertEqual(self.local_env, self.env.read_bytes())


if __name__ == "__main__":
    unittest.main()
