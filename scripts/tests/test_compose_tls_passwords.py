"""Render each supported stack and check its shared TLS store password wiring."""

import json
from pathlib import Path
import shutil
import subprocess
import unittest
import xml.etree.ElementTree as ET

from test_dev_stack import load_dev_stack


ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(shutil.which("docker"), "Docker Compose is required for configuration rendering")
class ComposeTlsPasswordsTest(unittest.TestCase):
    def test_generation_and_consumers_share_default_and_custom_passwords(self):
        launcher = load_dev_stack()
        context = launcher.make_context(ROOT)
        for key, trust in [("kspass", "tspass"), ("fixture-key-password", "fixture-trust-password")]:
            environment = launcher.build_environment(context)
            environment.update(SSL_KEYSTORE_PASSWORD=key, SSL_TRUSTSTORE_PASSWORD=trust,
                               OE_DB_PASSWORD="fixture-app-password", ADMIN_PASSWORD="fixture-db-admin-password",
                               POSTGRES_PASSWORD="fixture-postgres-password")
            configurations = {
                "development": launcher.compose_command(context, "config", "--no-env-resolution", "--format", "json"),
                "published": ["docker", "compose", "-f", "docker-compose.yml", "config", "--no-env-resolution", "--format", "json"],
                "build": ["docker", "compose", "-f", "build.docker-compose.yml", "config", "--no-env-resolution", "--format", "json"],
                "published-analyzers": ["docker", "compose", "-f", "docker-compose.yml", "-f",
                                        "docker-compose.analyzers.yml", "config", "--no-env-resolution", "--format", "json"],
                "build-analyzers": ["docker", "compose", "-f", "build.docker-compose.yml", "-f",
                                    "docker-compose.analyzers.yml", "config", "--no-env-resolution", "--format", "json"],
                "analyzer-ci": ["docker", "compose", "-f", "build.docker-compose.yml", "-f",
                                "projects/analyzer-harness/docker-compose.base.yml", "-f",
                                ".github/ci/ci.analyzer-harness.yml", "config", "--no-env-resolution", "--format", "json"],
            }
            for stack, command in configurations.items():
                with self.subTest(stack=stack, custom=key != "kspass"):
                    result = subprocess.run(command, cwd=ROOT, env=environment, text=True,
                                            capture_output=True, timeout=30)
                    self.assertEqual(0, result.returncode, result.stderr)
                    services = json.loads(result.stdout)["services"]
                    self.assertEqual(key, services["certs"]["environment"]["KEYSTORE_PW"])
                    self.assertEqual(trust, services["certs"]["environment"]["TRUSTSTORE_PW"])
                    oe = services["oe.openelis.org"]["environment"]
                    self.assertEqual(key, oe["SSL_KEYSTORE_PASSWORD"])
                    self.assertEqual(trust, oe["SSL_TRUSTSTORE_PASSWORD"])
                    self.assertNotIn("-Doe.ssl.keystorepassword=", oe["CATALINA_OPTS"])
                    fhir = services["fhir.openelis.org"]["environment"]
                    if stack == "development":
                        # This source harness still uses the upstream HAPI development image.
                        self.assertIn("-Dhapi.ssl.keystorepassword=" + key, fhir["CATALINA_OPTS"])
                    else:
                        self.assertEqual(key, fhir["SSL_KEYSTORE_PASSWORD"])
                        self.assertEqual(trust, fhir["SSL_TRUSTSTORE_PASSWORD"])
                        self.assertNotIn("Password=", fhir.get("JAVA_OPTS", ""))
                        self.assertNotIn("password=", fhir.get("CATALINA_OPTS", ""))
                    if stack in ("development", "analyzer-ci"):
                        self.assertEqual(key, oe["SERVER_SSL_KEY_STORE_PASSWORD"])
                        self.assertEqual(key, oe["SERVER_SSL_KEY_PASSWORD"])
                        self.assertEqual(trust, oe["SERVER_SSL_TRUST_STORE_PASSWORD"])
                    bridge = services.get("openelis-analyzer-bridge")
                    if bridge:
                        if stack in ("development", "analyzer-ci"):
                            self.assertIn("8443", bridge.get("group_add", []))
                        self.assertEqual(key, bridge["environment"]["SERVER_SSL_KEY_STORE_PASSWORD"])
                        self.assertEqual(trust, bridge["environment"]["BRIDGE_TRUSTSTORE_PASSWORD"])
                    # The mounted connector must actually consume the configured JVM property.
                    mounts = services["oe.openelis.org"].get("volumes", [])
                    connector = next((v["source"] for v in mounts
                                      if v.get("target") == "/usr/local/tomcat/conf/server.xml"),
                                     ROOT / "tomcat/oe_server.xml")
                    certificate = ET.parse(connector).find(".//Certificate")
                    self.assertEqual("${oe.ssl.keystorepassword}", certificate.get("certificateKeystorePassword"))


if __name__ == "__main__":
    unittest.main()
