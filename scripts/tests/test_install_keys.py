"""Run the install-key upgrade against real certificates and PKCS12 stores."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class InstallKeysTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.stores = self.root / "stores"
        self.stores.mkdir()
        self.proxy = self.root / "proxy.crt"
        self.proxy_key = self.root / "proxy.key"
        # Only relocate the production paths; execute the production script unchanged.
        script = (ROOT / "scripts/certs/ensure-install-keys.sh").read_text()
        script = script.replace("STORES=/etc/openelis-global", f'STORES="{self.stores}"')
        script = script.replace("PROXY_CERT=/etc/ssl/certs/apache-selfsigned.crt", f'PROXY_CERT="{self.proxy}"')
        script = script.replace("PROXY_KEY=/etc/ssl/private/apache-selfsigned.key", f'PROXY_KEY="{self.proxy_key}"')
        self.script = self.root / "upgrade.sh"
        self.script.write_text(script)
        self.env = dict(os.environ, KEYSTORE_PW="kspass", TRUSTSTORE_PW="tspass")

    def command(self, *args, input=None):
        result = subprocess.run([str(arg) for arg in args], input=input, text=True,
                                capture_output=True, env=self.env, timeout=60)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout + result.stderr if args[0] == "sh" else result.stdout

    def certificate(self, subject, name="old"):
        cert, key = self.root / f"{name}.crt", self.root / f"{name}.key"
        self.command("openssl", "req", "-x509", "-nodes", "-days", "365", "-newkey", "rsa:2048",
                     "-keyout", key, "-out", cert, "-subj", subject,
                     "-addext", "subjectAltName=DNS:*.openelis.org")
        return cert, key

    def install_old_stores(self, subject):
        # The certificate/store recipe from frontend/Dockerfile.certs before ade822d97a.
        # Both historical image subjects must be recognized.
        cert, key = self.certificate(subject)
        self.command("openssl", "pkcs12", "-inkey", key, "-in", cert, "-export",
                     "-out", self.stores / "keystore", "-passout", "env:KEYSTORE_PW")
        shutil.copyfile(self.stores / "keystore", self.stores / "client_facing_keystore")
        self.command("keytool", "-importcert", "-alias", "oeCert", "-file", cert,
                     "-storetype", "pkcs12", "-keystore", self.stores / "truststore",
                     "-storepass:env", "TRUSTSTORE_PW", "-noprompt")
        shutil.copyfile(cert, self.proxy)
        shutil.copyfile(key, self.proxy_key)
        return cert, key

    def fingerprint(self, cert):
        return self.command("openssl", "x509", "-noout", "-fingerprint", "-sha256", "-in", cert)

    def upgrade(self):
        return self.command("sh", self.script)

    def assert_stores_match_install(self):
        cert = self.stores / "install/server.crt"
        expected = self.fingerprint(cert)
        for store in ("keystore", "client_facing_keystore"):
            pem = self.command("openssl", "pkcs12", "-in", self.stores / store,
                               "-passin", "env:KEYSTORE_PW", "-nokeys", "-clcerts")
            actual = self.command("openssl", "x509", "-noout", "-fingerprint", "-sha256", input=pem)
            self.assertEqual(expected, actual)
        pem = self.command("keytool", "-exportcert", "-rfc", "-alias", "oeCert",
                           "-keystore", self.stores / "truststore", "-storetype", "pkcs12",
                           "-storepass:env", "TRUSTSTORE_PW")
        self.assertEqual(expected, self.command("openssl", "x509", "-noout", "-fingerprint", "-sha256", input=pem))
        self.assertEqual(expected, self.fingerprint(self.proxy))
        self.assertEqual(self.command("openssl", "pkey", "-in", self.stores / "install/server.key", "-pubout"),
                         self.command("openssl", "x509", "-in", cert, "-pubkey", "-noout"))

    def check_shared_upgrade(self, organization):
        old, key = self.install_old_stores(f"/C=US/ST=WA/L=Seattle/O={organization}/OU=DIGI/CN=localhost")
        # A paired service's separate trust entry must survive rebuilding oeCert.
        trusted, _ = self.certificate("/O=Partner/CN=paired-service", "partner")
        self.command("keytool", "-importcert", "-noprompt", "-alias", "partner", "-file", trusted,
                     "-keystore", self.stores / "truststore", "-storetype", "pkcs12",
                     "-storepass:env", "TRUSTSTORE_PW")
        self.upgrade()
        self.assertNotEqual(self.fingerprint(old), self.fingerprint(self.stores / "install/server.crt"))
        self.assertNotEqual(self.command("openssl", "pkey", "-in", key, "-pubout"),
                            self.command("openssl", "pkey", "-in", self.stores / "install/server.key", "-pubout"))
        self.assert_stores_match_install()
        self.command("keytool", "-list", "-alias", "partner", "-keystore", self.stores / "truststore",
                     "-storepass:env", "TRUSTSTORE_PW")
        stable = {p: p.read_bytes() for p in (self.stores / "install").iterdir()}
        self.upgrade()
        for path, contents in stable.items():
            self.assertEqual(contents, path.read_bytes(), path.name)

    def test_fresh_install_with_custom_passwords_and_repeat_start(self):
        self.env.update(KEYSTORE_PW="custom-keystore-123", TRUSTSTORE_PW="custom-truststore-456")
        self.upgrade()
        self.assert_stores_match_install()
        before = (self.stores / "install/server.key").read_bytes()
        self.upgrade()
        self.assertEqual(before, (self.stores / "install/server.key").read_bytes())
        self.assert_stores_match_install()

    def test_previously_extracted_shared_install_key_is_replaced(self):
        old, key = self.install_old_stores("/C=US/ST=WA/L=Seattle/O=DIGI-UW/OU=DIGI/CN=localhost")
        install = self.stores / "install"
        install.mkdir()
        shutil.copyfile(old, install / "server.crt")
        shutil.copyfile(key, install / "server.key")
        self.upgrade()
        self.assertNotEqual(self.fingerprint(old), self.fingerprint(install / "server.crt"))
        self.assert_stores_match_install()

    def test_repository_dockerfile_certificate_is_replaced(self):
        self.check_shared_upgrade("DIGI-UW")

    def test_published_image_certificate_is_replaced(self):
        self.check_shared_upgrade("I-TECH-UW")

    def test_site_certificate_and_key_are_preserved(self):
        old, key = self.install_old_stores("/C=US/ST=WA/L=Seattle/O=DIGI-UW/OU=Lab/CN=lab.example")
        before = {p: p.read_bytes() for p in self.stores.iterdir()}
        self.upgrade()
        self.assertEqual(self.fingerprint(old), self.fingerprint(self.stores / "install/server.crt"))
        self.assertEqual(self.command("openssl", "pkey", "-in", key, "-pubout"),
                         self.command("openssl", "pkey", "-in", self.stores / "install/server.key", "-pubout"))
        for path, contents in before.items():
            self.assertEqual(contents, path.read_bytes())
        self.assert_stores_match_install()

    def test_a_separate_proxy_certificate_is_preserved(self):
        self.install_old_stores("/C=US/ST=WA/L=Seattle/O=DIGI-UW/OU=DIGI/CN=localhost")
        cert, key = self.certificate("/O=Site/CN=lab.example", "proxy-issued")
        shutil.copyfile(cert, self.proxy)
        shutil.copyfile(key, self.proxy_key)
        self.upgrade()
        self.assertEqual(cert.read_bytes(), self.proxy.read_bytes())
        self.assertEqual(key.read_bytes(), self.proxy_key.read_bytes())

    def check_rotated_site_key(self, missing_truststore=False):
        self.upgrade()
        encryption = (self.stores / "install/encryption.key").read_bytes()
        proxy = self.proxy.read_bytes()
        client_store = (self.stores / "client_facing_keystore").read_bytes()
        cert, key = self.certificate("/O=Site/CN=rotated.lab.example", "rotated")
        self.command("openssl", "pkcs12", "-inkey", key, "-in", cert, "-export",
                     "-out", self.stores / "keystore", "-passout", "env:KEYSTORE_PW")
        if missing_truststore:
            (self.stores / "truststore").unlink()
        else:
            partner, _ = self.certificate("/O=Partner/CN=partner.example", "partner")
            self.command("keytool", "-importcert", "-noprompt", "-alias", "partner", "-file", partner,
                         "-keystore", self.stores / "truststore", "-storetype", "pkcs12",
                         "-storepass:env", "TRUSTSTORE_PW")
        keystore = (self.stores / "keystore").read_bytes()
        self.upgrade()
        self.assertEqual(self.fingerprint(cert), self.fingerprint(self.stores / "install/server.crt"))
        self.assertEqual(self.command("openssl", "pkey", "-in", key, "-pubout"),
                         self.command("openssl", "pkey", "-in", self.stores / "install/server.key", "-pubout"))
        trusted = self.command("keytool", "-exportcert", "-rfc", "-alias", "oeCert",
                               "-keystore", self.stores / "truststore", "-storetype", "pkcs12",
                               "-storepass:env", "TRUSTSTORE_PW")
        self.assertEqual(self.fingerprint(cert), self.command("openssl", "x509", "-noout",
                         "-fingerprint", "-sha256", input=trusted))
        if not missing_truststore:
            self.command("keytool", "-list", "-alias", "partner", "-keystore", self.stores / "truststore",
                         "-storepass:env", "TRUSTSTORE_PW")
        self.assertEqual(keystore, (self.stores / "keystore").read_bytes())
        self.assertEqual(client_store, (self.stores / "client_facing_keystore").read_bytes())
        self.assertEqual(proxy, self.proxy.read_bytes())
        self.assertEqual(encryption, (self.stores / "install/encryption.key").read_bytes())
        stable = {p: p.read_bytes() for p in (self.stores / "install").iterdir()}
        self.upgrade()
        for path, contents in stable.items():
            self.assertEqual(contents, path.read_bytes(), path.name)

    def test_rotated_site_keystore_updates_install_and_existing_trust(self):
        self.check_rotated_site_key()

    def test_rotated_site_keystore_recreates_missing_trust(self):
        self.check_rotated_site_key(missing_truststore=True)

    def test_changed_store_password_fails_without_modifying_existing_material(self):
        self.upgrade()
        before = {p: p.read_bytes() for p in self.stores.rglob("*") if p.is_file()}
        for setting in ("KEYSTORE_PW", "TRUSTSTORE_PW"):
            with self.subTest(setting=setting):
                changed = dict(self.env, **{setting: "wrong-password"})
                result = subprocess.run(["sh", str(self.script)], env=changed,
                                        capture_output=True, text=True, timeout=60)
                self.assertNotEqual(0, result.returncode)
                self.assertIn(setting, result.stderr)
                for path, contents in before.items():
                    self.assertEqual(contents, path.read_bytes(), str(path))


if __name__ == "__main__":
    unittest.main()
