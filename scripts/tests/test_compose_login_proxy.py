"""Keep the bundled proxy address and login trust setting in agreement."""
import ipaddress
import json
import os
import secrets
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]

class ComposeLoginProxyTest(unittest.TestCase):
    def test_published_and_built_stacks_trust_only_their_proxy(self):
        properties = dict(line.split("=", 1) for line in
                          (ROOT / "volume/properties/common.properties").read_text().splitlines()
                          if line and not line.startswith("#") and "=" in line)
        trusted = ipaddress.ip_network(properties["org.openelisglobal.login.throttle.trustedProxies"])
        self.assertEqual(32, trusted.prefixlen)
        environment = dict(os.environ)
        for name in ("POSTGRES_PASSWORD", "OE_DB_PASSWORD", "ADMIN_PASSWORD"):
            environment[name] = secrets.token_urlsafe(24)
        for base in ["docker-compose.yml", "build.docker-compose.yml"]:
            for extras in [[], ["docker-compose.analyzers.yml"]]:
                command = ["docker", "compose", "--env-file", ".env.example", "-f", base]
                for extra in extras:
                    command.extend(["-f", extra])
                result = subprocess.run(command + ["config", "--no-env-resolution", "--format", "json"],
                                        cwd=ROOT, env=environment,
                                        capture_output=True, text=True)
                self.assertEqual(0, result.returncode, result.stderr)
                services = json.loads(result.stdout)["services"]
                proxy_networks = services["proxy"]["networks"]
                self.assertEqual(["default"], list(proxy_networks))
                self.assertEqual(str(trusted.network_address), proxy_networks["default"]["ipv4_address"])
                for name, service in services.items():
                    if name == "proxy":
                        continue
                    for network in service.get("networks", {}).values():
                        if network and network.get("ipv4_address"):
                            self.assertNotIn(ipaddress.ip_address(network["ipv4_address"]), trusted)

    def test_analyzer_proxy_can_reach_bridge_in_all_harness_modes(self):
        harness = "projects/analyzer-harness/"
        environment = dict(os.environ, LETSENCRYPT_DOMAIN="example.invalid",
                           CI_PARITY_IMAGE_PREFIX="proxy-contract", CI_PARITY_COMPOSE_PROJECT="proxy-contract")
        for name in ("POSTGRES_PASSWORD", "OE_DB_PASSWORD", "ADMIN_PASSWORD"):
            environment[name] = secrets.token_urlsafe(24)
        variants = [
            [harness + name for name in ["docker-compose.dev.yml", "docker-compose.base.yml",
             "docker-compose.analyzer-test.yml", "docker-compose.letsencrypt.yml", "docker-compose.worktree.yml"]],
            ["build.docker-compose.yml", harness + "docker-compose.base.yml", ".github/ci/ci.analyzer-harness.yml"],
            ["build.docker-compose.yml", harness + "docker-compose.base.yml", ".github/ci/ci.analyzer-harness.yml",
             harness + "docker-compose.ci-parity-isolated.yml"],
        ]
        for files in variants:
            with self.subTest(files=files):
                command = ["docker", "compose", "--env-file", ".env.example"]
                for name in files:
                    command.extend(["-f", name])
                result = subprocess.run(command + ["config", "--no-env-resolution", "--format", "json"],
                                        cwd=ROOT, env=environment, capture_output=True, text=True)
                self.assertEqual(0, result.returncode, result.stderr)
                services = json.loads(result.stdout)["services"]
                proxy_networks = services["proxy"]["networks"]
                bridge_networks = services["openelis-analyzer-bridge"]["networks"]
                shared = set(proxy_networks) & set(bridge_networks)
                self.assertTrue(shared, "Nginx must have a network route to the Bridge upstream")
                aliases = [alias for network in shared for alias in
                           (bridge_networks[network] or {}).get("aliases", [])]
                self.assertIn("bridge.openelis.org", aliases)

if __name__ == "__main__":
    unittest.main()
