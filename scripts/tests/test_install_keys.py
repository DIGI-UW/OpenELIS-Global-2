"""Exercise fresh installation, restart, and explicit adoption with real keys and stores."""

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
        # Relocate paths and the TLS group to this host; container tests separately
        # exercise the production group 8443 and actual consumer identities.
        script = (ROOT / "scripts/certs/ensure-install-keys.sh").read_text()
        script = script.replace("STORES=/etc/openelis-global", f'STORES="{self.stores}"')
        script = script.replace("PROXY_CERT=/etc/ssl/certs/apache-selfsigned.crt", f'PROXY_CERT="{self.proxy}"')
        script = script.replace("PROXY_KEY=/etc/ssl/private/apache-selfsigned.key", f'PROXY_KEY="{self.proxy_key}"')
        script = script.replace("chown 0:8443", f"chown {os.getuid()}:{os.getgid()}")
        script = script.replace("chown 0:0", f"chown {os.getuid()}:{os.getgid()}")
        self.script = self.root / "initialize.sh"
        self.script.write_text(script)
        self.env = dict(os.environ, KEYSTORE_PW="kspass", TRUSTSTORE_PW="tspass")
        self.env.pop("OE_ENCRYPTION_PASSWORD", None)

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

    def upgrade(self, adopt=False):
        return self.command("sh", self.script, *(["--upgrade-existing"] if adopt else []))

    def assert_refused_unchanged(self, *arguments):
        before = {p: p.read_bytes() for p in self.stores.rglob("*") if p.is_file()}
        before.update({p: p.read_bytes() for p in (self.proxy, self.proxy_key) if p.is_file()})
        result = subprocess.run(["sh", str(self.script), *arguments], env=self.env,
                                capture_output=True, text=True, timeout=60)
        self.assertNotEqual(0, result.returncode, result.stdout)
        for path, contents in before.items():
            self.assertEqual(contents, path.read_bytes(), str(path))
        after = {p for p in self.stores.rglob("*") if p.is_file()}
        self.assertEqual({p for p in before if p not in (self.proxy, self.proxy_key)}, after)
        return result.stderr

    def adopt_legacy(self):
        self.assert_refused_unchanged()
        self.assert_refused_unchanged("--upgrade-existing")
        self.env["OE_ENCRYPTION_PASSWORD"] = "synthetic-legacy-encryption-password"
        self.upgrade(adopt=True)
        self.assertEqual(self.env["OE_ENCRYPTION_PASSWORD"],
                         (self.stores / "install/encryption.key").read_text())

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
        self.adopt_legacy()
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
        self.adopt_legacy()
        self.assertNotEqual(self.fingerprint(old), self.fingerprint(install / "server.crt"))
        self.assert_stores_match_install()

    def test_repository_dockerfile_certificate_is_replaced(self):
        self.check_shared_upgrade("DIGI-UW")

    def test_published_image_certificate_is_replaced(self):
        self.check_shared_upgrade("I-TECH-UW")

    def test_site_certificate_and_key_are_preserved(self):
        old, key = self.install_old_stores("/C=US/ST=WA/L=Seattle/O=DIGI-UW/OU=Lab/CN=lab.example")
        before = {p: p.read_bytes() for p in self.stores.iterdir()}
        self.adopt_legacy()
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
        self.adopt_legacy()
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
        self.assert_refused_unchanged()
        self.upgrade(adopt=True)
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

    def test_restart_refuses_each_missing_security_file(self):
        self.upgrade()
        paths = [self.stores / name for name in (
            "keystore", "client_facing_keystore", "truststore", "install/server.key",
            "install/server.crt", "install/encryption.key")]
        paths += [self.proxy, self.proxy_key]
        for path in paths:
            with self.subTest(file=path.name):
                original = path.read_bytes()
                path.unlink()
                self.assert_refused_unchanged()
                self.assertFalse(path.exists())
                path.write_bytes(original)

    def test_empty_store_volume_with_existing_proxy_is_not_a_fresh_install(self):
        self.upgrade()
        for path in self.stores.iterdir():
            shutil.rmtree(path) if path.is_dir() else path.unlink()
        self.assert_refused_unchanged()

    def test_private_modes_and_independent_encryption_key(self):
        self.upgrade()
        for name, mode in (("keystore", 0o640), ("client_facing_keystore", 0o640),
                           ("truststore", 0o644), ("install", 0o700),
                           ("install/server.key", 0o600), ("install/encryption.key", 0o600)):
            self.assertEqual(mode, (self.stores / name).stat().st_mode & 0o777, name)
        key = (self.stores / "install/encryption.key").read_text().strip()
        self.assertRegex(key, r"^[0-9a-f]{64}$")
        self.assertNotEqual(key, self.env["KEYSTORE_PW"])

    def test_wrong_client_store_password_and_mismatched_proxy_fail_unchanged(self):
        self.upgrade()
        self.env["OTHER_PASSWORD"] = "other-fixture-password"
        self.command("openssl", "pkcs12", "-export", "-inkey", self.stores / "install/server.key",
                     "-in", self.stores / "install/server.crt", "-out", self.stores / "client_facing_keystore",
                     "-passout", "env:OTHER_PASSWORD")
        self.assertIn("client_facing_keystore", self.assert_refused_unchanged())
        shutil.copyfile(self.stores / "keystore", self.stores / "client_facing_keystore")
        _, wrong_key = self.certificate("/CN=unrelated", "wrong")
        shutil.copyfile(wrong_key, self.proxy_key)
        self.assertIn("Proxy key", self.assert_refused_unchanged())

    def test_interrupted_initialization_is_not_retried(self):
        self.upgrade()
        (self.stores / "install/.initializing").touch()
        self.assertIn("interrupted", self.assert_refused_unchanged())

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
