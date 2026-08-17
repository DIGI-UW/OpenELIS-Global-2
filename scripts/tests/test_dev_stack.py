import importlib.machinery
import importlib.util
import pathlib
import unittest
from unittest.mock import patch


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

    def test_full_harness_scenarios_cover_each_transport_without_ids(self):
        scenarios = self.dev_stack.ANALYZER_SCENARIOS

        self.assertEqual(len(scenarios), 9)
        self.assertEqual({scenario[2] for scenario in scenarios}, {"generic-astm", "generic-hl7", "generic-file"})
        self.assertTrue(all(scenario[3] for scenario in scenarios))
        self.assertFalse(any(isinstance(value, int) for scenario in scenarios for value in scenario[:4]))


if __name__ == "__main__":
    unittest.main()
