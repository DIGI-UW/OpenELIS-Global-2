"""The analyzer overlay must run the Bridge and mock releases the submodules pin."""

from pathlib import Path
import subprocess
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]
PINNED_IMAGES = {
    "openelis-analyzer-bridge": ("tools/openelis-analyzer-bridge",
                                 "https://github.com/DIGI-UW/openelis-analyzer-bridge.git"),
    "astm-simulator": ("tools/analyzer-mock-server",
                       "https://github.com/DIGI-UW/analyzer-mock-server.git"),
}


def submodule_commit(path):
    entry = subprocess.run(["git", "ls-tree", "HEAD", path], cwd=ROOT, check=True,
                           capture_output=True, text=True).stdout.split()
    return entry[2]


def release_tags_at(url, commit):
    lines = subprocess.run(["git", "ls-remote", "--tags", url], check=True,
                           capture_output=True, text=True).stdout.splitlines()
    tags = set()
    for line in lines:
        sha, ref = line.split("\t")
        if sha == commit:
            tags.add(ref.removeprefix("refs/tags/").removesuffix("^{}").removeprefix("v"))
    return tags


class AnalyzerOverlayPinTest(unittest.TestCase):
    def test_overlay_images_are_the_releases_at_the_submodule_pins(self):
        overlay = yaml.safe_load((ROOT / "docker-compose.analyzers.yml").read_text())
        for service, (path, url) in PINNED_IMAGES.items():
            with self.subTest(service=service):
                tag = overlay["services"][service]["image"].rsplit(":", 1)[1]
                commit = submodule_commit(path)
                self.assertIn(tag, release_tags_at(url, commit),
                              f"{service} runs {tag}, but {path} pins {commit}; "
                              "pin a released commit and use its tag")


if __name__ == "__main__":
    unittest.main()
