import importlib.util
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest
from unittest.mock import patch

import test_readiness

spec = importlib.util.spec_from_file_location("deployment", Path(__file__).with_name("deploy-published-testing.py"))
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


def write_bundle(path, files):
    with tarfile.open(path, "w:gz") as bundle:
        for name, content in files.items():
            data = content.encode()
            info = tarfile.TarInfo(name)
            info.size = len(data)
            bundle.addfile(info, io.BytesIO(data))


class DeploymentTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name).resolve()
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
        self.bundle = self.root / "deploy-bundle.tgz"
        write_bundle(self.bundle, {name: "# " + name + "\n" for name in deployment.BUNDLE_FILES})
        self.request = {"manifest": {"appSha": self.sha, "appBranch": "develop", "images": {
            service: "itechuw/" + repository + "@sha256:" + "b" * 64 for service, repository in deployment.SERVICES.items()}},
            "deploy_path": str(self.root), "run_id": "36125008939-1",
            "readiness": {"url": "http://127.0.0.1:1/health", "timeout": 0.02, "interval": 0.01}}
        self.wrong_image = False
        self.branch_head = self.sha
        self.commands = []
        self.port_owner = deployment.PROJECT
        self.delivery = []

    def command(self, args, _cwd, capture=False):
        self.commands.append(args)
        if args[:2] == ["git", "ls-remote"]:
            return self.branch_head + "\trefs/heads/develop\n"
        if args[:2] == ["docker", "ps"]:
            return "proxy-id"
        if args == ["docker", "inspect", "proxy-id"]:
            return json.dumps([{"Config": {"Labels": {"com.docker.compose.project": self.port_owner}}}])
        if "ps" in args:
            return "container-id"
        if args[:2] == ["docker", "inspect"]:
            return json.dumps([{"State": {"Running": True}, "Image": "wrong" if self.wrong_image else "sha256:actual"}])
        if args[:3] == ["docker", "image", "inspect"]:
            return json.dumps([{"Id": "sha256:actual"}])
        return ""

    def verify_delivery(self, api_base, mock_url, accession, **_):
        self.delivery.append((api_base, mock_url, accession))
        return {"accession": accession, "rows": 2}

    def deploy(self, run=None, **patches):
        run = run or self.command
        with patch.object(deployment, "run", side_effect=run), patch.object(deployment.subprocess, "run"), \
                patch.object(deployment, "verify_analyzer_delivery",
                             side_effect=patches.get("delivery", self.verify_delivery)):
            deployment.deploy(self.request, self.diagnostics, self.bundle)

    def ready_health(self):
        test_readiness.ReadinessTest.setUpClass()
        self.addCleanup(test_readiness.ReadinessTest.tearDownClass)
        test_readiness.ReadinessTest.server.response = (200, "application/json", b'{"status": "UP"}')
        self.request["readiness"].update(url=test_readiness.ReadinessTest.url, timeout=1)

    def compose_calls(self, verb):
        return [args for args in self.commands if args[:2] == ["docker", "compose"] and verb in args]

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

    def test_competing_stack_is_rejected_before_anything_changes(self):
        for owner in ("openelis-docker", None):
            with self.subTest(owner=owner):
                self.port_owner = owner
                self.commands.clear()
                with self.assertRaisesRegex(ValueError, "another Docker stack"):
                    self.deploy()
                self.assertFalse(self.compose_calls("pull") or self.compose_calls("up"))
                self.assertFalse((self.root / "releases").exists())
                self.assertEqual(self.previous_override, self.override.read_text())

    def test_stack_owner_is_rechecked_after_pull(self):
        def change_owner(args, cwd, capture=False):
            result = self.command(args, cwd, capture)
            if "pull" in args:
                self.port_owner = "openelis-docker"
            return result
        with self.assertRaisesRegex(ValueError, "another Docker stack"):
            self.deploy(change_owner)
        self.assertFalse(self.compose_calls("up"))
        self.assertEqual(self.previous_override, self.override.read_text())

    def test_invalid_readiness_contract_is_rejected_before_deployment(self):
        self.request["readiness"]["timeout"] = 0
        with patch.object(deployment, "run") as command:
            with self.assertRaises(ValueError):
                deployment.deploy(self.request, self.diagnostics, self.bundle)
            command.assert_not_called()

    def test_old_build_cannot_overwrite_a_newer_deployment(self):
        self.branch_head = "c" * 40
        with self.assertRaisesRegex(ValueError, "obsolete deployment"):
            self.deploy()
        self.assertFalse(self.compose_calls("pull") or self.compose_calls("up"))
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
                with self.assertRaisesRegex(ValueError, "obsolete deployment"):
                    self.deploy(advance_during_pull)
                self.assertFalse(self.compose_calls("up"))
                self.assertEqual("previous-ready", json.loads(self.target.read_text())["deploymentId"])
                if existing_override:
                    self.assertEqual(self.previous_override, self.override.read_text())
                else:
                    self.assertFalse(self.override.exists())

    def test_failed_image_pull_preserves_the_previous_override(self):
        def fail_pull(args, cwd, capture=False):
            result = self.command(args, cwd, capture)
            if "pull" in args:
                raise deployment.subprocess.CalledProcessError(1, args)
            return result

        with self.assertRaises(deployment.subprocess.CalledProcessError):
            self.deploy(fail_pull)
        self.assertFalse(self.compose_calls("up"))
        self.assertEqual(self.previous_override, self.override.read_text())
        self.assertEqual("previous-ready", json.loads(self.target.read_text())["deploymentId"])

    def test_bundle_missing_a_required_file_is_rejected_before_pull(self):
        write_bundle(self.bundle, {"docker-compose.yml": "services: {}\n"})
        with self.assertRaisesRegex(ValueError, "bundle is missing"):
            self.deploy()
        self.assertFalse(self.compose_calls("pull"))
        self.assertFalse((self.root / "releases" / self.sha).exists())

    def test_release_runs_the_commits_own_compose_under_the_fixed_project(self):
        (self.root / "docker-compose.site.yml").write_text("services: {}\n")
        self.ready_health()
        self.deploy()
        release = self.root / "releases" / self.sha
        up = self.compose_calls("up")[0]
        self.assertEqual(["docker", "compose", "-p", deployment.PROJECT, "--project-directory", str(release),
                          "--env-file", str(self.root / ".env"),
                          "-f", str(release / "docker-compose.yml"),
                          "-f", str(release / "docker-compose.analyzers.yml"),
                          "-f", str(self.root / "docker-compose.site.yml"),
                          "-f", str(self.override), "up", "-d"], up)
        self.assertEqual("# docker-compose.analyzers.yml\n", (release / "docker-compose.analyzers.yml").read_text())
        lucene = release / "volume/lucene"
        self.assertTrue(lucene.is_symlink())
        self.assertEqual(self.root / "lucene", lucene.resolve())

    def test_site_overlay_is_optional(self):
        self.ready_health()
        self.deploy()
        self.assertNotIn(str(self.root / "docker-compose.site.yml"), self.compose_calls("up")[0])

    def test_failed_startup_withdraws_stale_ready_identity_and_keeps_it_in_diagnostics(self):
        with patch.object(deployment.subprocess, "run") as diagnostics, \
                patch.object(deployment, "run", side_effect=self.command):
            with self.assertRaisesRegex(RuntimeError, "did not become ready"):
                deployment.deploy(self.request, self.diagnostics, self.bundle)
        self.assertFalse(self.target.exists())
        self.assertEqual("previous-ready",
                         json.loads((self.diagnostics / "previous-target.json").read_text())["deploymentId"])
        self.assertFalse(json.loads((self.diagnostics / "readiness.json").read_text())["ready"])
        self.assertTrue((self.diagnostics / "compose-status.txt").exists())
        self.assertEqual(self.previous_override, (self.diagnostics / "previous-images.json").read_text())
        log_command = next(call.args[0] for call in diagnostics.call_args_list if "logs" in call.args[0])
        self.assertTrue(set(deployment.SERVICES).union(deployment.ANALYZER_SERVICES).issubset(log_command))
        self.assertFalse(any(args[:1] == ["env"] for args in self.commands))
        self.assertIn("keep-existing-value", (self.root / ".env").read_text())

    def test_wrong_running_image_cannot_publish_ready(self):
        self.wrong_image = True
        with self.assertRaisesRegex(ValueError, "does not match"):
            self.deploy()
        self.assertFalse(self.target.exists())

    def test_ready_deployment_seeds_analyzers_then_proves_delivery(self):
        self.ready_health()
        self.deploy()
        release = self.root / "releases" / self.sha
        seed = next(args for args in self.commands if args[:1] == ["env"])
        self.assertIn("BASE_URL=http://127.0.0.1:" + str(test_readiness.ReadinessTest.server.server_port), seed)
        self.assertIn("MOCK_URL=" + deployment.DEFAULT_MOCK_URL, seed)
        self.assertEqual(["bash", str(release / "projects/analyzer-harness/seed-analyzers.sh"),
                          "--ensure-connections", "--no-mock-network", "--activate"], seed[-5:])
        self.assertLess(self.commands.index(self.compose_calls("up")[0]), self.commands.index(seed))
        self.assertEqual([("http://127.0.0.1:" + str(test_readiness.ReadinessTest.server.server_port)
                           + "/api/OpenELIS-Global/rest", deployment.DEFAULT_MOCK_URL, "DEV01900361250089391")],
                         self.delivery)
        target = json.loads(self.target.read_text())
        self.assertEqual(self.sha, target["appSha"])
        self.assertEqual(str(release), target["release"])
        self.assertEqual(5, len(target["images"]))
        self.assertEqual("DEV01900361250089391", target["verification"]["analyzerDelivery"]["accession"])

    def test_failed_delivery_proof_withdraws_ready_identity(self):
        self.ready_health()

        def lost(*_args, **_kwargs):
            raise RuntimeError("analyzer result DEV01900361250089391 never reached OpenELIS")

        with self.assertRaisesRegex(RuntimeError, "never reached OpenELIS"):
            self.deploy(delivery=lost)
        self.assertFalse(self.target.exists())
        self.assertFalse(any(args[:3] == ["docker", "image", "prune"] for args in self.commands))

    def test_success_keeps_only_the_current_and_previous_release(self):
        releases = self.root / "releases"
        for name in ("old", "previous"):
            (releases / name).mkdir(parents=True)
        (self.target.parent / "target.json").write_text(json.dumps({"release": str(releases / "previous")}))
        self.ready_health()
        self.deploy()
        self.assertEqual({self.sha, "previous"}, {path.name for path in releases.iterdir()})
        self.assertIn(["docker", "image", "prune", "--all", "--force"], self.commands)

    def test_smoke_accession_is_a_valid_unique_accession(self):
        self.assertEqual("DEV01900361250089391", deployment.smoke_accession("36125008939-1"))
        self.assertEqual(20, len(deployment.smoke_accession("9" * 30 + "-12")))
        self.assertNotEqual(deployment.smoke_accession("1-1"), deployment.smoke_accession("1-2"))


class AnalyzerDeliveryTest(unittest.TestCase):
    def setUp(self):
        self.calls = []
        self.rows = []

    def http(self, method, url, body=None):
        self.calls.append((method, url, body))
        if url.endswith("/simulate/astm/genexpert_astm"):
            return {"status": "completed", "pushed": 1}
        if url.endswith("/analyzer/analyzers"):
            return {"analyzers": [{"id": 7, "name": "QuantStudio 5"},
                                  {"id": 2, "name": deployment.SMOKE_ANALYZER}]}
        if url.endswith("/AnalyzerResults?id=2"):
            return {"resultList": self.rows}
        raise AssertionError(url)

    def test_pushes_to_the_analyzers_own_listener_and_waits_for_its_rows(self):
        polls = []

        def arrive_on_second_poll(_seconds):
            polls.append(_seconds)
            self.rows = [{"accessionNumber": "DEV01900000000000011", "rawTestCode": "HIV-VL"}]

        report = deployment.verify_analyzer_delivery("https://oe/rest", "http://mock", "DEV01900000000000011",
                                                     http=self.http, sleep=arrive_on_second_poll, timeout=5)
        push = self.calls[0]
        self.assertEqual(("POST", "http://mock/simulate/astm/genexpert_astm",
                          {"destination": deployment.SMOKE_DESTINATION, "sample_id": "DEV01900000000000011"}), push)
        self.assertEqual({"accession": "DEV01900000000000011", "rows": 1}, report)
        self.assertEqual(1, len(polls))

    def test_result_that_never_arrives_fails_with_the_last_response(self):
        with self.assertRaisesRegex(RuntimeError, "never reached OpenELIS"):
            deployment.verify_analyzer_delivery("https://oe/rest", "http://mock", "DEV01900000000000011",
                                                http=self.http, sleep=lambda _s: None, timeout=0)

    def test_mock_that_did_not_push_fails_before_polling(self):
        def refused(method, url, body=None):
            self.calls.append((method, url, body))
            return {"status": "failed", "pushed": 0, "error": "connection refused"}

        with self.assertRaisesRegex(RuntimeError, "connection refused"):
            deployment.verify_analyzer_delivery("https://oe/rest", "http://mock", "DEV01900000000000011",
                                                http=refused, sleep=lambda _s: None, timeout=5)
        self.assertEqual(1, len(self.calls))


if __name__ == "__main__":
    unittest.main()
