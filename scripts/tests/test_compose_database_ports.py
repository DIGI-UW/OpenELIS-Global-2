"""Check the effective Compose network boundary, including layered harnesses."""

import json
from pathlib import Path
import shutil
import secrets
import subprocess
import unittest

from test_dev_stack import load_dev_stack

ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(shutil.which("docker"), "Docker Compose is required for configuration rendering")
class ComposeDatabasePortsTest(unittest.TestCase):
    def test_database_is_internal_in_deployments_and_loopback_only_in_development(self):
        launcher = load_dev_stack()
        context = launcher.make_context(ROOT)
        environment = launcher.build_environment(context)
        environment.update({name: secrets.token_urlsafe(24) for name in (
            "OE_DB_PASSWORD", "ADMIN_PASSWORD", "POSTGRES_PASSWORD")})
        configurations = {
            "development": launcher.compose_command(context, "config", "--no-env-resolution", "--format", "json"),
        }
        for base in ("docker-compose.yml", "build.docker-compose.yml"):
            for analyzers in (False, True):
                command = ["docker", "compose", "-f", base]
                if analyzers:
                    command += ["-f", "docker-compose.analyzers.yml"]
                configurations[f"{base}-analyzers={analyzers}"] = command + [
                    "config", "--no-env-resolution", "--format", "json"]
        configurations["analyzer-ci"] = [
            "docker", "compose", "-f", "build.docker-compose.yml", "-f",
            "projects/analyzer-harness/docker-compose.base.yml", "-f",
            ".github/ci/ci.analyzer-harness.yml", "config", "--no-env-resolution", "--format", "json"]

        for stack, command in configurations.items():
            with self.subTest(stack=stack):
                result = subprocess.run(command, cwd=ROOT, env=environment, text=True,
                                        capture_output=True, timeout=30)
                self.assertEqual(0, result.returncode, result.stderr)
                database = json.loads(result.stdout)["services"]["db.openelis.org"]
                ports = database.get("ports", [])
                if stack in ("development", "analyzer-ci"):
                    self.assertTrue(ports, "Developer tools must retain local database access")
                    for port in ports:
                        self.assertEqual("127.0.0.1", port.get("host_ip"), stack)
                        self.assertEqual(5432, port["target"])
                else:
                    self.assertEqual([], ports, "Deployment databases must not publish a host port")


if __name__ == "__main__":
    unittest.main()
