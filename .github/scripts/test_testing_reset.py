"""Reset lifecycle, ownership, pinned-image and failure checks; no clinical SQL fixtures."""

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zoneinfo import ZoneInfo
import datetime
import yaml

spec = importlib.util.spec_from_file_location("testing_reset", Path(__file__).with_name("reset-testing.py"))
resetter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resetter)


class TestingResetTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.site = Path(self.temporary.name).resolve()
        self.sha = "a" * 40
        self.release = self.site / "releases" / self.sha
        seed = self.release / resetter.deployment.SEED_SCRIPT
        seed.parent.mkdir(parents=True)
        seed.write_text("# supported setup script\n")
        (self.release / resetter.deployment.BASELINE_CATALOG).mkdir()
        self.state = self.site / ".openelis-ci"
        self.state.mkdir()
        (self.site / ".env").write_text("TEST_PASS=private-local-setting\n")
        (self.site / "configuration/backend").mkdir(parents=True)
        (self.site / "lucene").mkdir()
        (self.site / "configuration/backend/tester-upload.csv").write_text("old override")
        (self.site / "lucene/old-index").touch()
        self.target = {"instance": "testing", "state": "ready", "appBranch": "develop", "appSha": self.sha,
                       "release": str(self.release), "images": {"oe.openelis.org": {"imageId": "sha256:oe"}},
                       "verification": {"url": "https://testing.example/health", "analyzerDelivery": {"rows": 2}}}
        resetter.deployment.write_json(self.state / "target.json", self.target)
        self.config = {"services": {"oe.openelis.org": {"volumes": [
            {"type": "bind", "source": str(self.site / "configuration/backend"),
             "target": "/var/lib/openelis-global/configuration/backend"},
            {"type": "bind", "source": str(self.site / "lucene"), "target": "/var/lib/lucene_index"}]},
            "certs": {}, "openelis-analyzer-bridge": {}}, "volumes": {
                key: {"name": "testing_" + key} for key in resetter.DATA_VOLUMES}}
        self.commands = []
        self.foreign_volume = False
        self.wrong_image = False
        self.fail_seed = False

    def command(self, args, cwd, capture=False, **kwargs):
        self.commands.append(args)
        if args[:3] == ["docker", "volume", "inspect"]:
            key = args[-1].removeprefix("testing_")
            return json.dumps([{"Labels": {"com.docker.compose.project": "other" if self.foreign_volume else resetter.deployment.PROJECT,
                                           "com.docker.compose.volume": key}}])
        if args[:2] == ["docker", "inspect"]:
            image = "sha256:other" if self.wrong_image else "sha256:" + args[-1]
            return json.dumps([{"Image": image, "Config": {"Labels": {"com.docker.compose.project": resetter.deployment.PROJECT}}}])
        if args[:2] == ["docker", "compose"]:
            if args[-3:] == ["config", "--format", "json"]:
                return json.dumps(self.config)
            if "ps" in args:
                return "oe" if args[-1] == "oe.openelis.org" else args[-1]
        if args[:1] == ["env"] and self.fail_seed:
            raise RuntimeError("baseline setup failed")
        return ""

    def reset(self, **kwargs):
        with patch.object(resetter.deployment, "run", side_effect=self.command), \
                patch.object(resetter.deployment, "require_stack_owner"), \
                patch.object(resetter.readiness, "wait_until_ready", return_value={"ready": True}):
            resetter.reset(self.site, **kwargs)

    def test_reset_recreates_only_data_and_reuses_actual_images_then_runs_normal_setup(self):
        self.reset()
        stop = next(args for args in self.commands if args[-1:] == ["stop"])
        remove = next(args for args in self.commands if args[-2:] == ["rm", "--force"])
        self.assertNotIn("--volumes", remove)
        self.assertFalse(any("down" in args for args in self.commands))
        deletion = next(args for args in self.commands if args[:3] == ["docker", "volume", "rm"])
        self.assertEqual({"testing_" + key for key in resetter.DATA_VOLUMES}, set(deletion[3:]))
        up = next(args for args in self.commands if "up" in args)
        self.assertEqual(["up", "-d", "--pull", "never", "--no-build"], up[-5:])
        clean = [args for args in self.commands if args[:2] == ["docker", "run"]]
        self.assertEqual(2, len(clean))
        self.assertTrue(all("--entrypoint" in args and "sha256:oe" in args for args in clean))
        seed = next(args for args in self.commands if args[:1] == ["env"])
        self.assertEqual(["--ensure-connections", "--no-mock-network"], seed[-2:])
        self.assertNotIn("--activate", seed)
        self.assertLess(self.commands.index(stop), self.commands.index(remove))
        self.assertLess(self.commands.index(remove), self.commands.index(up))
        self.assertLess(self.commands.index(up), self.commands.index(seed))
        status = json.loads((self.state / "reset-status.json").read_text())
        self.assertEqual("ready", status["state"])
        self.assertEqual(self.sha, status["appSha"])
        self.assertTrue(status["lastSuccessfulReset"])
        self.assertNotIn("analyzerDelivery", json.loads((self.state / "target.json").read_text())["verification"])
        self.assertIn("private-local-setting", (self.site / ".env").read_text())
        self.assertNotIn("private-local-setting", (self.state / "public/index.html").read_text())

    def test_foreign_volume_or_wrong_deployed_image_stops_before_deletion(self):
        for field in ("foreign_volume", "wrong_image"):
            with self.subTest(field=field):
                setattr(self, field, True)
                self.commands.clear()
                with self.assertRaises(ValueError):
                    self.reset()
                self.assertFalse(any("stop" in args or "rm" in args for args in self.commands))
                setattr(self, field, False)

    def test_overlay_cannot_relocate_catalog_but_external_certificate_mounts_are_preserved(self):
        self.config["services"]["certs"]["volumes"] = [
            {"type": "bind", "source": "/srv/certificates", "target": "/etc/nginx/certs/"}]
        self.reset()
        self.assertFalse(any("/srv/certificates" in " ".join(args) for args in self.commands if "rm" in args))
        self.config["services"]["oe.openelis.org"]["volumes"][0]["source"] = "/srv/clinical-catalog"
        self.commands.clear()
        with self.assertRaises(ValueError):
            self.reset()
        self.assertFalse(any("stop" in args for args in self.commands))

    def test_failed_setup_marks_baseline_failed_and_retains_last_success(self):
        self.reset()
        previous = json.loads((self.state / "reset-status.json").read_text())["lastSuccessfulReset"]
        self.fail_seed = True
        with self.assertRaisesRegex(RuntimeError, "baseline setup failed"):
            self.reset()
        status = json.loads((self.state / "reset-status.json").read_text())
        self.assertEqual("failed", status["state"])
        self.assertEqual(previous, status["lastSuccessfulReset"])
        target = json.loads((self.state / "target.json").read_text())
        self.assertEqual("reset-failed", target["state"])
        self.assertNotIn("analyzerDelivery", target["verification"])

    def test_skip_next_is_consumed_only_by_scheduled_reset(self):
        marker = self.state / "skip-next-reset"
        marker.touch()
        self.reset(scheduled=True)
        self.assertFalse(marker.exists())
        self.assertEqual([], self.commands)
        self.assertEqual("skipped", json.loads((self.state / "reset-status.json").read_text())["state"])
        marker.touch()
        self.reset()
        self.assertTrue(marker.exists())

    def test_schedule_is_six_pm_pacific_in_both_seasons_without_catch_up(self):
        timer = (Path(__file__).parents[1] / "systemd/openelis-testing-reset.timer").read_text()
        self.assertIn("OnCalendar=*-*-* 18:00:00 America/Los_Angeles", timer)
        self.assertIn("Persistent=false", timer)
        pacific = ZoneInfo("America/Los_Angeles")
        for month, utc_hour in ((1, 2), (7, 1)):
            local = datetime.datetime(2026, month, 5, 18, tzinfo=pacific)
            self.assertEqual(utc_hour, local.astimezone(datetime.timezone.utc).hour)

    def test_published_stack_reuses_harness_initializer_before_app_startup(self):
        root = Path(__file__).resolve().parents[2]
        overlay = yaml.safe_load((root / "docker-compose.analyzers.yml").read_text())
        initializer = overlay["services"]["harness-catalog-init"]
        self.assertEqual({"file": "./projects/analyzer-harness/docker-compose.base.yml",
                          "service": "harness-catalog-init"}, initializer["extends"])
        self.assertIn("./projects/analyzer-harness/config-templates:/templates:ro", initializer["volumes"])
        self.assertEqual("service_completed_successfully",
                         overlay["services"]["oe.openelis.org"]["depends_on"]["harness-catalog-init"]["condition"])
        for domain in ("tests", "test-results"):
            files = list((root / resetter.deployment.BASELINE_CATALOG / domain).glob("*.csv"))
            self.assertTrue(files)
            self.assertTrue(all(str(path.relative_to(root)) in resetter.deployment.BUNDLE_FILES for path in files))


if __name__ == "__main__":
    unittest.main()
