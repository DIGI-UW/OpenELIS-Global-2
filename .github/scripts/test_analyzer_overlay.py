"""The analyzer overlay runs the Bridge and mock built from the submodule pins."""

from pathlib import Path
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]
SUBMODULES = {
    "openelis-analyzer-bridge": "./tools/openelis-analyzer-bridge",
    "astm-simulator": "./tools/analyzer-mock-server",
}


def services(path):
    return yaml.safe_load((ROOT / path).read_text())["services"]


class AnalyzerOverlayImageTest(unittest.TestCase):
    def test_overlay_runs_the_images_published_from_the_tested_submodule_builds(self):
        overlay = services("docker-compose.analyzers.yml")
        built = services(".github/ci/ci.analyzer-harness.yml")
        workflow = yaml.safe_load((ROOT / ".github/workflows/publish-images.yml").read_text())
        published = {entry["compose_service"]: entry
                     for entry in workflow["jobs"]["publish-images"]["strategy"]["matrix"]["include"]}
        core_tag = services("docker-compose.yml")["oe.openelis.org"]["image"].strip().rsplit(":", 1)[1]
        for service, context in SUBMODULES.items():
            with self.subTest(service=service):
                self.assertEqual(context, built[service]["build"]["context"])
                entry = published[service]
                self.assertEqual(built[service]["image"], entry["compose_image"])
                self.assertEqual(f"itechuw/openelis-global-2-{entry['dockerhub_suffix']}:{core_tag}",
                                 overlay[service]["image"])


if __name__ == "__main__":
    unittest.main()
