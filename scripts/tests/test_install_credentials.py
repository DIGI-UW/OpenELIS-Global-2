"""Exercise credential startup behavior without printing credential contents."""

import os
from pathlib import Path
import subprocess
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class InstallCredentialsTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.secrets = self.root / "secrets"
        self.secrets.mkdir()
        self.runtime = self.root / "runtime"
        self.env = dict(os.environ)
        self.env.pop("OE_DB_PASSWORD", None)
        self.env.pop("OE_INITIAL_ADMIN_PASSWORD_FILE", None)

    def entrypoint(self, assertions=""):
        script = (ROOT / "install/docker-entrypoint.sh").read_text().split("# The install's own key", 1)[0]
        script = script.replace("/run/secrets", str(self.secrets)).replace("/run/openelis-secrets", str(self.runtime))
        # Container acceptance separately verifies the actual UID and GID.
        script = script.replace("-o 0 -g 8443", "").replace("-o 8443 -g 8443", "")
        return subprocess.run(["sh", "-c", script + assertions], env=self.env,
                              capture_output=True, text=True, timeout=10)

    def test_database_secret_is_not_a_jvm_argument_or_log(self):
        password = "Private db 'quote' $dollar ! spaces"
        (self.secrets / "datasource.password").write_text(password + "\n")
        self.env["EXPECTED_PASSWORD"] = password
        self.env["CATALINA_OPTS"] = "-Xmx512m"
        result = self.entrypoint('\n[ "$OE_DB_PASSWORD" = "$EXPECTED_PASSWORD" ] && [ "$CATALINA_OPTS" = "-Xmx512m" ]\n')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn(password, result.stdout + result.stderr)

    def test_empty_database_secret_stops_startup(self):
        (self.secrets / "datasource.password").write_text("")
        result = self.entrypoint()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Database password file is empty", result.stderr)

    def test_initial_secret_is_copied_with_private_permissions(self):
        password = "PrivateAdmin123!"
        (self.secrets / "initial-admin-password").write_text(password + "\r\n")
        result = self.entrypoint('\n[ -n "$OE_INITIAL_ADMIN_PASSWORD_FILE" ]\n')
        self.assertEqual(0, result.returncode, result.stderr)
        copied = self.runtime / "initial-admin-password"
        self.assertEqual((self.secrets / "initial-admin-password").read_bytes(), copied.read_bytes())
        self.assertEqual(0o400, copied.stat().st_mode & 0o777)
        self.assertEqual(0o750, self.runtime.stat().st_mode & 0o777)
        self.assertNotIn(password, result.stdout + result.stderr)

    def test_absent_initial_secret_is_left_for_database_aware_application_check(self):
        result = self.entrypoint('\n[ -z "${OE_INITIAL_ADMIN_PASSWORD_FILE:-}" ]\n')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(self.runtime.exists())

    @unittest.skipUnless(shutil.which("docker"), "Docker Compose is required")
    def test_production_compose_rejects_each_missing_database_password(self):
        names = ("OE_DB_PASSWORD", "ADMIN_PASSWORD", "POSTGRES_PASSWORD")
        for missing in names:
            with self.subTest(missing=missing):
                env = dict(self.env, **{name: "Provided123!" for name in names})
                env[missing] = ""
                result = subprocess.run(
                    ["docker", "compose", "--env-file", ".env.example", "-f", "docker-compose.yml", "config", "--quiet"],
                    cwd=ROOT, env=env, capture_output=True, text=True, timeout=20)
                self.assertNotEqual(0, result.returncode)
                self.assertIn("Set " + missing + " in .env", result.stderr)
                self.assertNotIn("Provided123!", result.stdout + result.stderr)

    def test_database_initializer_requires_explicit_credentials(self):
        for application, administrator in [("", ""), ("Provided123!", "")]:
            env = dict(self.env, DB_PASSWORD=application, DB_SUPERUSER_PASSWORD=administrator)
            result = subprocess.run(["bash", str(ROOT / "db/dbInit/init.sh")], env=env,
                                    capture_output=True, text=True, timeout=10)
            self.assertNotEqual(0, result.returncode)
            self.assertNotIn("Provided123!", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
