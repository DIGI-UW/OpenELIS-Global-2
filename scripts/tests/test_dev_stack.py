import importlib.machinery
import importlib.util
import os
import pathlib
import re
import subprocess
import tempfile
import unittest
from unittest.mock import MagicMock, patch


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "dev-stack"


def load_dev_stack():
    loader = importlib.machinery.SourceFileLoader("dev_stack", str(SCRIPT_PATH))
    spec = importlib.util.spec_from_loader(loader.name, loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


class DevStackContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dev_stack = load_dev_stack()

    def test_project_identity_is_deterministic_per_worktree(self):
        first = self.dev_stack.make_context(REPO_ROOT)
        second = self.dev_stack.make_context(REPO_ROOT)

        self.assertEqual(first.project, second.project)
        self.assertRegex(first.project, r"^oe2-[a-z0-9-]+-[0-9a-f]{8}-dev$")

    def test_full_harness_is_the_only_compose_path(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        self.assertEqual(
            context.compose_files[-1],
            REPO_ROOT
            / "projects"
            / "analyzer-harness"
            / "docker-compose.worktree.yml",
        )

        self.assertIn(
            REPO_ROOT
            / "projects"
            / "analyzer-harness"
            / "docker-compose.letsencrypt.yml",
            context.compose_files,
        )

    def test_compose_commands_are_scoped_to_the_worktree_project(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        command = self.dev_stack.compose_command(context, "ps")

        self.assertEqual(command[:4], ["docker", "compose", "-p", context.project])
        self.assertIn(str(context.compose_files[-1]), command)
        self.assertEqual(command[-1], "ps")

    def test_analyzer_environment_namespaces_dynamic_networks(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        environment = self.dev_stack.build_environment(context)

        self.assertEqual(environment["ANALYZER_NETWORK_NAMESPACE"], context.project)
        self.assertRegex(environment["ANALYZER_SUBNET_PREFIX"], r"^10\.(?:[6-9][0-9]|1[0-9]{2}|2[01][0-9]|22[0-3])$")
        self.assertEqual(environment["OE_UAT_SCENARIOS_ENABLED"], "true")

    def test_localhost_uses_random_loopback_ingress_and_self_signed_tls(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        with patch.dict(
            self.dev_stack.os.environ,
            {
                "LETSENCRYPT_DOMAIN": "localhost",
                "DEV_STACK_INGRESS_BIND_ADDRESS": "127.0.0.1",
                "DEV_STACK_HTTP_PORT": "0",
                "DEV_STACK_HTTPS_PORT": "0",
                "DEV_STACK_TLS": "self-signed",
            },
        ):
            environment = self.dev_stack.build_environment(context)

        self.assertEqual(environment["DEV_STACK_INGRESS_BIND_ADDRESS"], "127.0.0.1")
        self.assertEqual(environment["DEV_STACK_HTTP_PORT"], "0")
        self.assertEqual(environment["DEV_STACK_HTTPS_PORT"], "0")
        self.assertEqual(environment["DEV_STACK_TLS"], "self-signed")

    def test_domain_uses_public_standard_ingress_and_letsencrypt(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        with patch.dict(
            self.dev_stack.os.environ,
            {"LETSENCRYPT_DOMAIN": "dev.example.org"},
            clear=True,
        ):
            environment = self.dev_stack.build_environment(context)

        self.assertEqual(environment["DEV_STACK_INGRESS_BIND_ADDRESS"], "0.0.0.0")
        self.assertEqual(environment["DEV_STACK_HTTP_PORT"], "80")
        self.assertEqual(environment["DEV_STACK_HTTPS_PORT"], "443")
        self.assertEqual(environment["DEV_STACK_TLS"], "letsencrypt")

    def test_json_requests_verify_https_certificates_by_default(self):
        response = MagicMock()
        response.__enter__.return_value = response
        response.status = 200
        response.read.return_value = b"{}"

        with patch.object(self.dev_stack, "urlopen", return_value=response) as open_url:
            self.dev_stack.request_json("https://dev.example.org/status")

        self.assertIsNone(open_url.call_args.kwargs["context"])

    def test_json_requests_disable_verification_only_when_explicit(self):
        response = MagicMock()
        response.__enter__.return_value = response
        response.status = 200
        response.read.return_value = b"{}"

        with patch.object(self.dev_stack, "urlopen", return_value=response) as open_url:
            self.dev_stack.request_json(
                "https://localhost/status", verify_tls=False
            )

        self.assertEqual(open_url.call_args.kwargs["context"].verify_mode, 0)

    def test_endpoint_parser_accepts_ipv4_and_ipv6_compose_output(self):
        self.assertEqual(self.dev_stack.parse_published_port("127.0.0.1:49152"), 49152)
        self.assertEqual(self.dev_stack.parse_published_port("[::1]:49153"), 49153)

    def test_volume_removal_requires_explicit_confirmation(self):
        with self.assertRaisesRegex(ValueError, "--yes"):
            self.dev_stack.validate_down_options(remove_volumes=True, confirmed=False)

        self.dev_stack.validate_down_options(remove_volumes=True, confirmed=True)

    def test_bootstrap_includes_every_full_harness_submodule(self):
        self.assertEqual(
            self.dev_stack.required_submodules(),
            (
                "dataexport",
                "plugins",
                "tools/analyzer-mock-server",
                "tools/openelis-analyzer-bridge",
            ),
        )

    def test_submodule_bootstrap_repairs_empty_checkouts_without_forcing_dirty_ones(self):
        repair_mode = self.dev_stack.submodule_repair_mode

        self.assertEqual(repair_mode("expected", "actual", " D file", False), "force")
        self.assertEqual(repair_mode("expected", "actual", "", True), "checkout")
        self.assertIsNone(repair_mode("expected", "expected", "", True))
        with self.assertRaisesRegex(RuntimeError, "local changes"):
            repair_mode("expected", "actual", " M file", True)

    def test_frontend_build_override_is_deterministic(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        self.assertTrue(
            self.dev_stack.frontend_dependencies_changed(
                context, {"DEV_STACK_BUILD_FRONTEND": "true"}
            )
        )
        self.assertFalse(
            self.dev_stack.frontend_dependencies_changed(
                context, {"DEV_STACK_BUILD_FRONTEND": "false"}
            )
        )

    def test_frontend_build_detects_non_mounted_runtime_inputs(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        with patch.object(
            self.dev_stack.subprocess,
            "run",
            return_value=subprocess.CompletedProcess([], 0),
        ) as run:
            self.dev_stack.frontend_dependencies_changed(context, {})

        command = run.call_args.args[0]
        self.assertIn("frontend/vite.config.ts", command)
        self.assertIn("frontend/index.html", command)
        self.assertIn("frontend/tsconfig.json", command)
        self.assertIn("frontend/.npmrc", command)

    def test_run_java21_honors_selected_java_21_home(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            java_home = root / "selected-java"
            java_bin = java_home / "bin"
            java_bin.mkdir(parents=True)
            java = java_bin / "java"
            java.write_text(
                "#!/bin/sh\n"
                "echo '    java.specification.version = 21' >&2\n",
                encoding="utf-8",
            )
            java.chmod(0o755)
            environment = os.environ.copy()
            environment["JAVA_HOME"] = str(java_home)
            environment["SDKMAN_DIR"] = str(root / "missing-sdkman")
            environment.pop("JAVA_HOME_21", None)

            result = subprocess.run(
                [
                    str(REPO_ROOT / "scripts" / "run-java21"),
                    "sh",
                    "-c",
                    'printf "%s" "$JAVA_HOME"',
                ],
                env=environment,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout, str(java_home))

    def test_proxy_resolves_replaceable_services_through_docker_dns(self):
        for config_name in ("nginx.conf.template", "nginx.conf"):
            with self.subTest(config_name=config_name):
                proxy = (REPO_ROOT / "volume" / "nginx" / config_name).read_text()

                self.assertIn("resolver 127.0.0.11", proxy)
                self.assertEqual(proxy.count('set $frontend_upstream "frontend.openelis.org";'), 2)
                self.assertEqual(len(re.findall(r"proxy_pass\s+http://\$frontend_upstream;", proxy)), 2)
                self.assertEqual(proxy.count('set $oe_upstream "oe.openelis.org";'), 4)
                self.assertEqual(len(re.findall(r"proxy_pass\s+https://\$oe_upstream:8443;", proxy)), 4)
                if config_name == "nginx.conf.template":
                    self.assertIn('set $bridge_upstream "bridge.openelis.org";', proxy)
                    self.assertIn("proxy_pass https://$bridge_upstream:8443;", proxy)

    def test_proxy_configuration_is_validated_and_reloaded(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = self.dev_stack.build_environment(context)

        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.reload_proxy(context, environment)

        commands = [call.args[0] for call in run.call_args_list]
        self.assertEqual(commands[0][-5:], ["exec", "-T", "proxy", "nginx", "-t"])
        self.assertEqual(
            commands[1][-6:], ["exec", "-T", "proxy", "nginx", "-s", "reload"]
        )

    def test_backend_is_recreated_to_remount_the_current_war(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = self.dev_stack.build_environment(context)

        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.remount_application_artifact(context, environment)

        command = run.call_args.args[0]
        self.assertEqual(
            command[-5:],
            ["up", "-d", "--no-deps", "--force-recreate", "oe.openelis.org"],
        )

    def test_full_harness_scenarios_cover_each_transport_without_ids(self):
        scenarios = self.dev_stack.ANALYZER_SCENARIOS

        self.assertEqual(len(scenarios), 9)
        self.assertEqual({scenario[2] for scenario in scenarios}, {"generic-astm", "generic-hl7", "generic-file"})
        self.assertTrue(all(scenario[3] for scenario in scenarios))
        self.assertFalse(any(isinstance(value, int) for scenario in scenarios for value in scenario[:4]))


if __name__ == "__main__":
    unittest.main()
