"""The Linux installer retains its site key through the existing Java properties path."""

import importlib.util
import os
from pathlib import Path
import secrets
import tempfile
import unittest
from unittest.mock import patch

import yaml

ROOT = Path(__file__).resolve().parents[2]


class LinuxInstallerKeyTest(unittest.TestCase):
    def test_fresh_and_existing_installs_keep_the_same_property_key(self):
        source = ROOT / "install/installerTemplate/linux/setup_OpenELIS.py"
        spec = importlib.util.spec_from_file_location("linux_installer", source)
        installer = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(installer)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            config = root / "config"
            config.mkdir()
            deployment = root / "deployment"
            deployment.mkdir()
            installer.CONFIG_DIR = str(config) + "/"
            installer.OE_VAR_DIR = str(deployment) + "/"
            installer.INSTALLER_TEMPLATE_DIR = str(source.parent / "templates") + "/"
            installer.DB_HOST = "database.example.test"
            installer.SECRETS_DIR = str(root / "secrets") + "/"
            site_key = secrets.token_hex(32) + r"\key"
            with patch.object(installer, "getpass", side_effect=[site_key, site_key]):
                installer.get_set_encryption_key()
            key_file = config / "ENCRYPTION_KEY"
            original = key_file.read_bytes()
            self.assertEqual(site_key, installer.ENCRYPTION_KEY)
            with patch.object(installer, "getpass", side_effect=AssertionError("must retain existing key")):
                installer.get_set_encryption_key()
            self.assertEqual(original, key_file.read_bytes())
            previous_directory = Path.cwd()
            try:
                os.chdir(root)
                installer.create_docker_compose_file()
                with patch.object(installer.os, "chown"):
                    installer.create_properties_files()
            finally:
                os.chdir(previous_directory)
            rendered = (deployment / "docker-compose.yml").read_text()
            compose = yaml.safe_load(rendered)
            mounts = compose["services"]["oe.openelis.org"]["secrets"]
            self.assertTrue(any(item["source"] == "common.properties" for item in mounts))
            self.assertFalse(any(item["source"] == "encryption.key" for item in mounts))
            properties = (root / "secrets/common.properties").read_text()
            self.assertIn("encryption.general.password=" + site_key, properties)
            self.assertNotIn(site_key, rendered)
            self.assertEqual(original, key_file.read_bytes())


if __name__ == "__main__":
    unittest.main()
