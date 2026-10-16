import contextlib
import hashlib
import importlib.machinery
import importlib.util
import io
import os
import pathlib
import re
import shutil
import subprocess
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "dev-stack"


def load_dev_stack():
    loader = importlib.machinery.SourceFileLoader("dev_stack", str(SCRIPT_PATH))
    spec = importlib.util.spec_from_loader(loader.name, loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


def isolated_git_environment(home):
    environment = os.environ.copy()
    environment.update(
        {
            "HOME": str(home),
            "GIT_CONFIG_GLOBAL": os.devnull,
            "GIT_CONFIG_NOSYSTEM": "1",
            "GIT_AUTHOR_NAME": "Dev Stack Test",
            "GIT_AUTHOR_EMAIL": "dev-stack@example.org",
            "GIT_COMMITTER_NAME": "Dev Stack Test",
            "GIT_COMMITTER_EMAIL": "dev-stack@example.org",
            "GIT_CONFIG_COUNT": "1",
            "GIT_CONFIG_KEY_0": "protocol.file.allow",
            "GIT_CONFIG_VALUE_0": "always",
        }
    )
    return environment


class SubmoduleFixture:
    """A parent repository pinning tools/sub to the first of two commits."""

    path = "tools/sub"

    def __init__(self, root):
        self.environment = isolated_git_environment(root)
        origin = root / "sub-origin"
        origin.mkdir()
        self.git(origin, "init", "-q")
        (origin / "source.txt").write_text("pinned\n")
        self.git(origin, "add", "source.txt")
        self.git(origin, "commit", "-q", "-m", "pinned")
        self.pinned = self.git(origin, "rev-parse", "HEAD")
        (origin / "source.txt").write_text("local work\n")
        self.git(origin, "commit", "-q", "-am", "local work")
        self.local = self.git(origin, "rev-parse", "HEAD")

        self.repo_root = root / "parent"
        self.repo_root.mkdir()
        self.git(self.repo_root, "init", "-q")
        self.git(self.repo_root, "submodule", "add", "-q", str(origin), self.path)
        self.git(self.submodule, "checkout", "-q", self.pinned)
        self.git(self.repo_root, "add", self.path)
        self.git(self.repo_root, "commit", "-q", "-m", "pin submodule")
        self.context = SimpleNamespace(repo_root=self.repo_root)

    @property
    def submodule(self):
        return self.repo_root / self.path

    def git(self, cwd, *arguments):
        return subprocess.run(
            ["git", *arguments],
            cwd=cwd,
            env=self.environment,
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()

    def head(self):
        return self.git(self.submodule, "rev-parse", "HEAD")


class DevStackSubmoduleTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dev_stack = load_dev_stack()

    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.fixture = SubmoduleFixture(pathlib.Path(directory.name))

    def ensure_submodules(self):
        printed = io.StringIO()
        with (
            patch.object(
                self.dev_stack, "required_submodules", return_value=(self.fixture.path,)
            ),
            contextlib.redirect_stdout(printed),
        ):
            self.dev_stack.ensure_submodules(
                self.fixture.context, self.fixture.environment
            )
        return printed.getvalue()

    def test_submodule_at_another_commit_is_built_as_checked_out_and_reported(self):
        self.fixture.git(self.fixture.submodule, "checkout", "-q", self.fixture.local)

        printed = self.ensure_submodules()

        self.assertEqual(self.fixture.head(), self.fixture.local)
        self.assertEqual(
            printed,
            f"tools/sub: checked out {self.fixture.local[:7]} "
            f"(recorded pin {self.fixture.pinned[:7]})\n",
        )

    def test_submodule_with_uncommitted_changes_is_left_alone_and_reported(self):
        (self.fixture.submodule / "source.txt").write_text("uncommitted\n")

        printed = self.ensure_submodules()

        self.assertEqual(self.fixture.head(), self.fixture.pinned)
        self.assertEqual(
            (self.fixture.submodule / "source.txt").read_text(), "uncommitted\n"
        )
        self.assertEqual(
            printed,
            f"tools/sub: checked out {self.fixture.pinned[:7]} "
            f"(recorded pin {self.fixture.pinned[:7]}), with uncommitted changes\n",
        )

    def test_empty_submodule_is_initialized_at_the_recorded_pin(self):
        self.fixture.git(
            self.fixture.repo_root, "submodule", "deinit", "-q", "-f", self.fixture.path
        )
        self.assertEqual(os.listdir(self.fixture.submodule), [])

        printed = self.ensure_submodules()

        self.assertEqual(self.fixture.head(), self.fixture.pinned)
        self.assertEqual(
            (self.fixture.submodule / "source.txt").read_text(), "pinned\n"
        )
        self.assertEqual(printed, "")

    def test_harness_bootstrap_preserves_checked_out_commits_and_local_edits(self):
        fixture = self.fixture
        paths = ("tools/openelis-analyzer-bridge", "tools/analyzer-mock-server")
        origin = fixture.repo_root.parent / "sub-origin"
        for path in paths:
            fixture.git(fixture.repo_root, "submodule", "add", "-q", str(origin), path)
            fixture.git(fixture.repo_root / path, "checkout", "-q", fixture.pinned)
            fixture.git(fixture.repo_root, "add", path)
        fixture.git(fixture.repo_root, "commit", "-q", "-m", "pin analyzers")
        bridge = fixture.repo_root / paths[0]
        mock = fixture.repo_root / paths[1]
        fixture.git(bridge, "checkout", "-q", fixture.local)
        (mock / "source.txt").write_text("uncommitted mock edit\n")
        project_dir = fixture.repo_root / "projects" / "analyzer-harness"
        project_dir.mkdir(parents=True)
        shutil.copy2(
            REPO_ROOT / "projects" / "analyzer-harness" / "bootstrap.sh",
            project_dir / "bootstrap.sh",
        )
        context = SimpleNamespace(repo_root=fixture.repo_root, project_dir=project_dir)
        environment = {**fixture.environment, "LETSENCRYPT_DOMAIN": "localhost"}

        with patch.object(self.dev_stack, "required_submodules", return_value=paths):
            self.dev_stack.ensure_submodules(context, environment)
        self.dev_stack.bootstrap_analyzer_harness(
            context, environment, skip_submodules=True
        )

        self.assertEqual(fixture.git(bridge, "rev-parse", "HEAD"), fixture.local)
        self.assertEqual((bridge / "source.txt").read_text(), "local work\n")
        self.assertEqual(fixture.git(mock, "rev-parse", "HEAD"), fixture.pinned)
        self.assertEqual((mock / "source.txt").read_text(), "uncommitted mock edit\n")
        self.assertTrue((project_dir / "volume" / "menu" / "menu_config.json").is_file())

    def test_clean_submodule_at_the_recorded_pin_is_silent(self):
        printed = self.ensure_submodules()

        self.assertEqual(self.fixture.head(), self.fixture.pinned)
        self.assertEqual(printed, "")

    def test_only_submodules_that_differ_from_the_pin_are_returned(self):
        with patch.object(self.dev_stack, "required_submodules", return_value=(self.fixture.path,)):
            with contextlib.redirect_stdout(io.StringIO()):
                clean = self.dev_stack.ensure_submodules(self.fixture.context, self.fixture.environment)
                (self.fixture.submodule / "source.txt").write_text("uncommitted\n")
                changed = self.dev_stack.ensure_submodules(self.fixture.context, self.fixture.environment)

        self.assertEqual(clean, ())
        self.assertEqual(changed, (self.fixture.path,))


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

    def test_explicit_uat_scenario_setting_overrides_isolated_default(self):
        context = self.dev_stack.make_context(REPO_ROOT)

        with patch.dict(
            self.dev_stack.os.environ,
            {"OE_UAT_SCENARIOS_ENABLED": "false"},
            clear=True,
        ):
            environment = self.dev_stack.build_environment(context)

        self.assertEqual(environment["OE_UAT_SCENARIOS_ENABLED"], "false")

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

    def test_localhost_defaults_to_trusted_local_ca(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        with patch.dict(self.dev_stack.os.environ, {"LETSENCRYPT_DOMAIN": "localhost"}, clear=True), patch.object(self.dev_stack, "read_dotenv", return_value={}):
            environment = self.dev_stack.build_environment(context)
        self.assertEqual("local-ca", environment["DEV_STACK_TLS"])

    def test_local_trust_install_is_limited_to_system_store(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = {"DEV_STACK_TLS": "local-ca", "LETSENCRYPT_DOMAIN": "localhost"}
        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.ensure_local_tls_trust(context, environment)
        run.assert_called_once_with(
            ["mkcert", "-install"],
            cwd=REPO_ROOT,
            environment={**environment, "TRUST_STORES": "system"},
        )
        self.assertNotIn("TRUST_STORES", environment)

    def test_other_tls_modes_do_not_change_host_trust(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        for tls in ("self-signed", "letsencrypt"):
            with self.subTest(tls=tls), patch.object(self.dev_stack, "run") as run:
                self.dev_stack.ensure_local_tls_trust(context, {"DEV_STACK_TLS": tls})
            run.assert_not_called()

    def test_up_stops_before_build_when_trust_setup_fails(self):
        environment = {"DEV_STACK_TLS": "local-ca", "LETSENCRYPT_DOMAIN": "localhost"}
        with (
            patch.object(self.dev_stack, "ensure_local_env"),
            patch.object(self.dev_stack, "build_environment", return_value=environment),
            patch.object(self.dev_stack, "doctor"),
            patch.object(self.dev_stack, "run", side_effect=subprocess.CalledProcessError(1, "mkcert")),
            patch.object(self.dev_stack, "ensure_submodules") as submodules,
            patch.object(self.dev_stack, "build_application") as build,
            patch.object(self.dev_stack, "start_services") as start,
        ):
            with self.assertRaisesRegex(RuntimeError, "interactive terminal"):
                self.dev_stack.main(["up"])
        submodules.assert_not_called()
        build.assert_not_called()
        start.assert_not_called()

    def test_local_tls_covers_loopback_and_never_copies_or_installs_ca(self):
        with tempfile.TemporaryDirectory() as temporary:
            context = self.dev_stack.make_context(pathlib.Path(temporary))
            environment = {"LETSENCRYPT_DOMAIN": "localhost", "DEV_STACK_TLS": "local-ca"}
            def fake_run(command, **kwargs):
                if command[0] == "mkcert":
                    pathlib.Path(command[command.index("-key-file") + 1]).write_text("test-only key placeholder")
                return SimpleNamespace(stdout="")
            with patch.object(self.dev_stack, "run", side_effect=fake_run) as run:
                self.dev_stack.configure_tls(context, environment)
            commands = [call.args[0] for call in run.call_args_list]
            self.assertEqual(["localhost", "127.0.0.1", "::1"], commands[0][-3:])
            self.assertNotIn("-install", commands[0])
            copies = [command for command in commands if "cp" in command]
            self.assertEqual(2, len(copies))
            self.assertEqual("proxy:/etc/nginx/certs/apache-selfsigned.crt", copies[0][-1])
            self.assertEqual("proxy:/etc/nginx/keys/apache-selfsigned.key", copies[1][-1])
            self.assertTrue(all(context.project in command for command in copies))
            self.assertFalse(any("rootCA" in part for command in commands for part in command))
            self.assertEqual(0o600, (context.repo_root / ".dev-stack/tls/localhost-key.pem").stat().st_mode & 0o777)

    def test_trusted_tls_fails_closed_on_certificate_errors(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        with patch.object(self.dev_stack, "endpoint_environment", return_value={"BASE_URL": "https://localhost:12345"}), patch.object(self.dev_stack, "run", side_effect=subprocess.CalledProcessError(60, "curl")) as run:
            with self.assertRaisesRegex(RuntimeError, "HTTPS verification failed"):
                self.dev_stack.verify_tls(context, {"DEV_STACK_TLS": "local-ca"})
        command = run.call_args.args[0]
        self.assertNotIn("--insecure", command)
        self.assertNotIn("-k", command)
        self.assertNotIn("--retry-all-errors", command)
        self.assertEqual("https://localhost:12345/", command[-1])

    def test_local_ca_refuses_public_domain(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        with patch.object(self.dev_stack, "run") as run:
            with self.assertRaisesRegex(ValueError, "loopback"):
                self.dev_stack.configure_tls(context, {"LETSENCRYPT_DOMAIN": "example.org", "DEV_STACK_TLS": "local-ca"})
        run.assert_not_called()

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
                "tools/analyzer-mock-server",
                "tools/openelis-analyzer-bridge",
            ),
        )

    def test_submodule_initialization_forces_an_empty_failed_checkout(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / "dataexport").mkdir()
            (root / "dataexport" / ".git").write_text("gitdir: missing\n")
            context = SimpleNamespace(repo_root=root)
            failed = subprocess.CalledProcessError(128, ["git", "submodule"])

            with patch.object(
                self.dev_stack, "run", side_effect=[failed, None]
            ) as run:
                self.dev_stack.initialize_submodule(context, {}, "dataexport")

            self.assertEqual(run.call_count, 2)
            self.assertEqual(
                run.call_args_list[1].args[0],
                [
                    "git",
                    "submodule",
                    "update",
                    "--init",
                    "--recursive",
                    "--checkout",
                    "--force",
                    "dataexport",
                ],
            )

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

    def test_playwright_defaults_to_core_app_and_preserves_test_selection(self):
        command = self.dev_stack.playwright_command(
            ["playwright/tests/foundational/core/example.spec.ts"]
        )

        self.assertEqual(
            command,
            [
                "npm",
                "run",
                "pw:test",
                "--",
                "--project=core-app",
                "playwright/tests/foundational/core/example.spec.ts",
            ],
        )

    def test_playwright_preserves_explicit_project_and_options(self):
        command = self.dev_stack.playwright_command(
            ["--project=setup", "--headed"]
        )

        self.assertEqual(
            command,
            ["npm", "run", "pw:test", "--", "--project=setup", "--headed"],
        )

    def test_playwright_parser_forwards_native_options_without_separator(self):
        arguments = self.dev_stack.parse_args(
            [
                "playwright",
                "--project=core-app",
                "playwright/tests/foundational/core/example.spec.ts",
                "--headed",
            ]
        )

        self.assertEqual(
            arguments.playwright_args,
            [
                "--project=core-app",
                "playwright/tests/foundational/core/example.spec.ts",
                "--headed",
            ],
        )

    def test_playwright_uses_explicit_remote_url_without_stack_discovery(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = {"BASE_URL": "https://amr.openelis-global.org"}

        with patch.object(self.dev_stack, "endpoint_environment") as endpoints:
            base_url = self.dev_stack.playwright_base_url(context, environment)

        self.assertEqual(base_url, "https://amr.openelis-global.org")
        endpoints.assert_not_called()

    def test_playwright_discovers_current_worktree_url_when_not_explicit(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = {}

        with patch.object(
            self.dev_stack,
            "endpoint_environment",
            return_value={"BASE_URL": "https://localhost:49152"},
        ):
            base_url = self.dev_stack.playwright_base_url(context, environment)

        self.assertEqual(base_url, "https://localhost:49152")

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

    def test_image_build_refreshes_backend_runtime_without_rebuilding_the_mounted_war(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = {}
        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.build_images(context, environment)
        command = run.call_args.args[0]
        self.assertEqual(command[-5:],
                         ["build", "frontend.openelis.org", "oe.openelis.org", "astm-simulator", "openelis-analyzer-bridge"])
        self.assertEqual(environment["DEV_STACK_BACKEND_IMAGE"], f"{context.project}-backend:dev")
        self.assertEqual(environment["DEV_STACK_BRIDGE_IMAGE"], f"{context.project}-analyzer-bridge:dev")
        compose = (context.project_dir / "docker-compose.dev.yml").read_text()
        self.assertIn("target: dev-runtime", compose)
        self.assertIn("../../target/OpenELIS-Global.war:", compose)

    def test_backend_running_probe_is_scoped_to_the_worktree(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = self.dev_stack.build_environment(context)

        with patch.object(
            self.dev_stack,
            "run",
            return_value=subprocess.CompletedProcess([], 0, stdout="container-id\n"),
        ) as run:
            running = self.dev_stack.service_is_running(
                context, environment, "oe.openelis.org"
            )

        self.assertTrue(running)
        self.assertEqual(
            run.call_args.args[0][-5:],
            ["ps", "--status", "running", "-q", "oe.openelis.org"],
        )

    def test_start_services_recreates_only_a_previously_running_backend(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = self.dev_stack.build_environment(context)

        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.start_services(
                context, environment, refresh_running_backend=True
            )

        commands = [call.args[0] for call in run.call_args_list]
        self.assertEqual(commands[0][-4:], ["up", "-d", "--no-build", "--remove-orphans"])
        self.assertEqual(
            commands[1][-6:],
            ["up", "-d", "--no-build", "--no-deps", "--force-recreate", "oe.openelis.org"],
        )

    def test_start_services_does_not_recreate_backend_on_initial_start(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        environment = self.dev_stack.build_environment(context)

        with patch.object(self.dev_stack, "run") as run:
            self.dev_stack.start_services(
                context, environment, refresh_running_backend=False
            )

        self.assertEqual(len(run.call_args_list), 1)
        self.assertIn(
            "../../target/OpenELIS-Global.war:/usr/local/tomcat/webapps/OpenELIS-Global.war",
            (
                REPO_ROOT
                / "projects"
                / "analyzer-harness"
                / "docker-compose.dev.yml"
            ).read_text(),
        )

    def test_up_recreates_a_running_backend_exactly_once_after_build(self):
        with (
            patch.object(self.dev_stack, "doctor"),
            patch.object(self.dev_stack, "ensure_local_tls_trust"),
            patch.object(self.dev_stack, "ensure_local_env"),
            patch.object(self.dev_stack, "ensure_submodules"),
            patch.object(self.dev_stack, "bootstrap_analyzer_harness"),
            patch.object(self.dev_stack, "service_is_running", return_value=True),
            patch.object(self.dev_stack, "heal_war_mount"),
            patch.object(self.dev_stack, "build_application"),
            patch.object(self.dev_stack, "build_images"),
            patch.object(self.dev_stack, "pull_images"),
            patch.object(self.dev_stack, "reload_proxy"),
            patch.object(self.dev_stack, "configure_tls"),
            patch.object(self.dev_stack, "verify_tls"),
            patch.object(self.dev_stack, "print_endpoints"),
            patch.object(self.dev_stack, "run") as run,
        ):
            self.dev_stack.main(["up", "--no-wait", "--no-scenarios"])

        recreates = [
            call.args[0]
            for call in run.call_args_list
            if "--force-recreate" in call.args[0]
        ]
        self.assertEqual(len(recreates), 1)
        self.assertEqual(
            recreates[0][-6:],
            ["up", "-d", "--no-build", "--no-deps", "--force-recreate", "oe.openelis.org"],
        )

    def test_harness_refreshes_repository_owned_menu_configuration(self):
        bootstrap = (
            REPO_ROOT / "projects" / "analyzer-harness" / "bootstrap.sh"
        ).read_text()

        self.assertIn(
            'cp "$ROOT_VOLUME/menu/menu_config.json" '
            '"$HARNESS_VOLUME/menu/menu_config.json"',
            bootstrap,
        )

    def test_scenarios_delegate_to_existing_harness_without_resetting_data(self):
        context = self.dev_stack.make_context(REPO_ROOT)
        endpoints = {
            "BASE_URL": "https://localhost:49152",
            "MOCK_SIMULATOR_URL": "http://localhost:49153",
            "ANALYZER_BRIDGE_URL": "https://localhost:49154",
        }
        for tls in ("self-signed", "letsencrypt"):
            with self.subTest(tls=tls):
                environment = {"DEV_STACK_TLS": tls}
                with (
                    patch.object(self.dev_stack, "endpoint_environment", return_value=endpoints),
                    patch.object(self.dev_stack, "run") as run,
                    patch.object(self.dev_stack, "urlopen", side_effect=AssertionError("Use the harness seeder")),
                ):
                    self.dev_stack.initialize_harness_scenarios(context, environment)

                run.assert_called_once_with(
                    ["bash", str(REPO_ROOT / "projects/analyzer-harness/seed-analyzers.sh"), "--ensure-connections"],
                    cwd=REPO_ROOT,
                    environment={
                        "DEV_STACK_TLS": tls,
                        "BASE_URL": endpoints["BASE_URL"],
                        "MOCK_URL": endpoints["MOCK_SIMULATOR_URL"],
                        "BRIDGE_ADMIN_URL": endpoints["ANALYZER_BRIDGE_URL"],
                    },
                )
                self.assertEqual(environment, {"DEV_STACK_TLS": tls})


class DevStackPublishedImagesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dev_stack = load_dev_stack()

    def setUp(self):
        self.context = self.dev_stack.make_context(REPO_ROOT)
        self.environment = self.dev_stack.build_environment(self.context)

    def pull(self, changed_submodules=(), run=None, differences=(), backend_differences=()):
        printed = io.StringIO()
        with (
            patch.object(self.dev_stack, "run", side_effect=run) as mocked_run,
            patch.object(self.dev_stack, "frontend_image_differences", return_value=list(differences)),
            patch.object(self.dev_stack, "backend_image_differences", return_value=list(backend_differences)),
            contextlib.redirect_stdout(printed),
            contextlib.redirect_stderr(printed),
        ):
            self.dev_stack.pull_images(self.context, self.environment, changed_submodules)
        return [call.args[0] for call in mocked_run.call_args_list], printed.getvalue()

    def test_compose_files_run_the_published_development_images(self):
        harness = REPO_ROOT / "projects" / "analyzer-harness"
        compose = (harness / "docker-compose.dev.yml").read_text() + (harness / "docker-compose.worktree.yml").read_text()
        for _, variable, repository, _ in self.dev_stack.APPLICATION_IMAGES:
            with self.subTest(variable=variable):
                self.assertIn(f"image: ${{{variable}:-itechuw/{repository}:develop}}", compose)

    def test_environment_selects_the_published_images_by_default(self):
        self.assertEqual(self.environment["DEV_STACK_BACKEND_IMAGE"], "itechuw/openelis-global-2-dev:develop")
        self.assertEqual(
            self.environment["DEV_STACK_FRONTEND_IMAGE"], "itechuw/openelis-global-2-frontend-dev:develop"
        )

    def test_published_images_honor_namespace_and_tag_overrides(self):
        with patch.dict(os.environ, {"DEV_STACK_IMAGE_NAMESPACE": "example", "DEV_STACK_IMAGE_TAG": "release-3.2"}):
            environment = self.dev_stack.build_environment(self.context)

        self.assertEqual(environment["DEV_STACK_BACKEND_IMAGE"], "example/openelis-global-2-dev:release-3.2")

    def test_up_pulls_every_published_image_and_builds_none(self):
        commands, printed = self.pull()

        self.assertEqual(
            commands,
            [
                ["docker", "pull", "itechuw/openelis-global-2-frontend-dev:develop"],
                ["docker", "pull", "itechuw/openelis-global-2-dev:develop"],
                ["docker", "pull", "itechuw/openelis-global-2-analyzer-mock:develop"],
                ["docker", "pull", "itechuw/openelis-global-2-analyzer-bridge:develop"],
            ],
        )
        self.assertEqual(printed, "")

    def test_changed_submodule_is_reported_but_never_built(self):
        commands, printed = self.pull(changed_submodules=("tools/openelis-analyzer-bridge",))

        self.assertFalse([command for command in commands if "build" in command])
        self.assertIn("openelis-analyzer-bridge runs the published", printed)
        self.assertIn("Rerun with --build", printed)
        self.assertNotIn("astm-simulator runs the published", printed)

    def test_frontend_files_that_differ_from_the_image_are_reported_but_never_built(self):
        commands, printed = self.pull(differences=["package-lock.json"])

        self.assertFalse([command for command in commands if "build" in command])
        self.assertIn("frontend/package-lock.json differ", printed)
        self.assertIn("Rerun with --build", printed)

    def test_frontend_differences_compare_the_image_files_with_the_checkout(self):
        digests = {
            name: hashlib.sha256((REPO_ROOT / "frontend" / name).read_bytes()).hexdigest()
            for name in self.dev_stack.FRONTEND_IMAGE_INPUTS
        }
        for stale in (None, "package-lock.json"):
            with self.subTest(stale=stale):
                listing = "\n".join(
                    f"{'0' * 64 if name == stale else digest}  /app/{name}" for name, digest in digests.items()
                )
                with patch.object(
                    self.dev_stack, "run", return_value=subprocess.CompletedProcess([], 0, stdout=listing)
                ) as run:
                    differences = self.dev_stack.frontend_image_differences(
                        self.context, self.environment, "image:tag"
                    )
                self.assertEqual(differences, [stale] if stale else [])
                self.assertEqual(run.call_args.args[0][:6], ["docker", "run", "--rm", "--entrypoint", "sha256sum", "image:tag"])

    def test_unreadable_frontend_image_counts_as_different(self):
        with patch.object(self.dev_stack, "run", side_effect=subprocess.CalledProcessError(1, "docker")):
            differences = self.dev_stack.frontend_image_differences(self.context, self.environment, "image:tag")

        self.assertEqual(differences, list(self.dev_stack.FRONTEND_IMAGE_INPUTS))

    def test_failed_pull_without_a_copy_on_the_machine_points_to_build(self):
        def run(command, **_):
            raise subprocess.CalledProcessError(1, command)

        with self.assertRaisesRegex(RuntimeError, "--build"):
            self.pull(run=run)

    def test_failed_pull_keeps_the_copy_already_on_the_machine(self):
        def run(command, **_):
            if command[:2] == ["docker", "pull"]:
                raise subprocess.CalledProcessError(1, command)
            return subprocess.CompletedProcess(command, 0, stdout="")

        commands, printed = self.pull(run=run)

        self.assertFalse([command for command in commands if "build" in command])
        self.assertIn("using the copy already on this machine", printed)

    def up(self, *arguments):
        with (
            patch.object(self.dev_stack, "doctor"),
            patch.object(self.dev_stack, "ensure_local_tls_trust"),
            patch.object(self.dev_stack, "ensure_local_env"),
            patch.object(self.dev_stack, "ensure_submodules", return_value=("tools/analyzer-mock-server",)),
            patch.object(self.dev_stack, "bootstrap_analyzer_harness"),
            patch.object(self.dev_stack, "service_is_running", return_value=False),
            patch.object(self.dev_stack, "heal_war_mount"),
            patch.object(self.dev_stack, "build_application") as build_application,
            patch.object(self.dev_stack, "build_images") as build_images,
            patch.object(self.dev_stack, "pull_images") as pull_images,
            patch.object(self.dev_stack, "record_images") as record_images,
            patch.object(self.dev_stack, "start_services"),
            patch.object(self.dev_stack, "reload_proxy"),
            patch.object(self.dev_stack, "configure_tls"),
            patch.object(self.dev_stack, "verify_tls"),
            patch.object(self.dev_stack, "print_endpoints"),
        ):
            self.dev_stack.main(["up", "--no-wait", "--no-scenarios", *arguments])
        record_images.assert_called_once()
        return build_application, build_images, pull_images

    def test_up_builds_the_war_and_pulls_the_published_images_by_default(self):
        build_application, build_images, pull_images = self.up()

        build_application.assert_called_once()
        build_images.assert_not_called()
        self.assertEqual(pull_images.call_args.args[2], ("tools/analyzer-mock-server",))

    def test_build_flag_builds_the_images_from_the_checkout_instead_of_pulling(self):
        build_application, build_images, pull_images = self.up("--build")

        build_application.assert_called_once()
        build_images.assert_called_once()
        pull_images.assert_not_called()

    def test_backend_image_inputs_that_differ_are_reported_but_never_built(self):
        commands, printed = self.pull(backend_differences=["install"])

        self.assertFalse([command for command in commands if "build" in command])
        self.assertIn("install differ from the commit itechuw/openelis-global-2-dev:develop was built from", printed)
        self.assertIn("Rerun with --build", printed)

    def test_backend_differences_compare_the_checkout_with_the_image_revision(self):
        def run(command, **_):
            if command[:3] == ["docker", "image", "inspect"]:
                return subprocess.CompletedProcess(command, 0, stdout="abc123\n")
            return subprocess.CompletedProcess(
                command, 0, stdout="install/docker-entrypoint.sh\nDockerfile.dev\n"
            )

        with patch.object(self.dev_stack, "run", side_effect=run) as mocked:
            differences = self.dev_stack.backend_image_differences(self.context, self.environment, "image:tag")

        self.assertEqual(differences, ["Dockerfile.dev", "install"])
        self.assertEqual(
            mocked.call_args.args[0],
            ["git", "diff", "--name-only", "abc123", "--", *self.dev_stack.BACKEND_IMAGE_INPUTS],
        )

    def test_backend_differences_are_silent_without_a_known_revision(self):
        for revision, git_fails in (("<no value>", False), ("", False), ("abc123", True)):
            def run(command, **_):
                if command[0] == "git" and git_fails:
                    raise subprocess.CalledProcessError(128, command)
                return subprocess.CompletedProcess(command, 0, stdout=revision if command[0] == "docker" else "install/x")

            with self.subTest(revision=revision, git_fails=git_fails), patch.object(self.dev_stack, "run", side_effect=run):
                self.assertEqual(
                    self.dev_stack.backend_image_differences(self.context, self.environment, "image:tag"), []
                )

    def test_skip_build_reuses_the_images_the_last_up_ran(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        context = self.context._replace(repo_root=pathlib.Path(directory.name))
        built = dict(self.environment)
        for _, variable, _, name in self.dev_stack.APPLICATION_IMAGES:
            built[variable] = f"{context.project}-{name}:dev"
        self.dev_stack.record_images(context, built)

        restarted = dict(self.environment)
        with patch.dict(os.environ, {}, clear=False):
            for _, variable, _, _ in self.dev_stack.APPLICATION_IMAGES:
                os.environ.pop(variable, None)
            self.dev_stack.reuse_recorded_images(context, restarted)

        self.assertEqual(restarted["DEV_STACK_BACKEND_IMAGE"], f"{context.project}-backend:dev")
        self.assertEqual(restarted["DEV_STACK_BRIDGE_IMAGE"], f"{context.project}-analyzer-bridge:dev")

    def test_explicit_image_variable_wins_over_the_recorded_image(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        context = self.context._replace(repo_root=pathlib.Path(directory.name))
        self.dev_stack.record_images(context, {**self.environment, "DEV_STACK_BACKEND_IMAGE": "recorded:dev"})
        environment = {**self.environment, "DEV_STACK_BACKEND_IMAGE": "explicit:tag"}

        with patch.dict(os.environ, {"DEV_STACK_BACKEND_IMAGE": "explicit:tag"}):
            self.dev_stack.reuse_recorded_images(context, environment)

        self.assertEqual(environment["DEV_STACK_BACKEND_IMAGE"], "explicit:tag")

    def test_skip_build_and_build_cannot_be_combined(self):
        self.assertTrue(self.dev_stack.parse_args(["up", "--build"]).build)
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            self.dev_stack.parse_args(["up", "--skip-build", "--build"])


if __name__ == "__main__":
    unittest.main()
