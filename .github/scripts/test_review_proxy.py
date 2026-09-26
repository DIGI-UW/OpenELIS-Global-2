"""Exercise optional Review hooks against the actual proxy configurations.

Uses isolated local Docker containers and generated test certificates. It never
connects to an OpenELIS deployment or to the public Review service.
"""

import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import ssl
import subprocess
import tempfile
import time
import unittest
import uuid


ROOT = Path(__file__).resolve().parents[2]
IMAGE = os.environ.get("NGINX_TEST_IMAGE", "nginx:1.27-alpine")
PROFILES = {
    "development": "volume/nginx/nginx.conf",
    "production": "volume/nginx/nginx-prod.conf",
    "installer": "install/installerTemplate/linux/templates/nginx.conf",
}
HTML = b"<html><head><title>OpenELIS fixture</title></head><body>Application</body></html>"
SCRIPT = '<script src="/__review/test-widget.js" data-instance="fixture"></script>'
SUBMIT = "/api/OpenELIS-Global/__review/uat-fixture/submissions"


def run(*args, check=True):
    return subprocess.run(args, text=True, capture_output=True, check=check)


class ReviewMountTest(unittest.TestCase):
    def test_effective_compose_mounts(self):
        with tempfile.TemporaryDirectory(prefix="oe-review-compose-") as directory:
            root = Path(directory)
            env = root / ".env"
            env.write_text((ROOT / ".env.example").read_text())
            # Representative installer answers; no live installation is read.
            template = (ROOT / "install/installerTemplate/linux/templates/docker-compose.yml").read_text()
            values = {"timezone": "UTC", "db_host": "db.example.test", "db_port": "5432"}
            def substitute(match):
                key = match[1]
                if key in values:
                    return values[key]
                if key.endswith("_dir"):
                    return str(root / key) + "/"
                return "fixture-" + key.replace("_", "-")
            installer = root / "installer.yml"
            installer.write_text(re.sub(r"\[%\s*(\w+)\s*%\]", substitute, template))
            profiles = [
                (ROOT / "dev.docker-compose.yml",),
                (ROOT / "dev.docker-compose.yml", ROOT / "docker-compose.letsencrypt.yml"),
                (ROOT / "docker-compose.yml",),
                (installer,),
            ]
            for files in profiles:
                for override in (None, str(root / "custom-review")):
                    with self.subTest(files=files, override=override):
                        environment = dict(os.environ)
                        environment.pop("REVIEW_CONFIG_DIR", None)
                        if override:
                            environment["REVIEW_CONFIG_DIR"] = override
                        command = ["docker", "compose", "--env-file", str(env)]
                        for path in files:
                            command += ["-f", str(path)]
                        result = subprocess.run(command + ["config", "--format", "json", "--no-env-resolution"],
                                                capture_output=True, text=True, env=environment)
                        self.assertEqual(0, result.returncode, result.stderr)
                        services = json.loads(result.stdout)["services"]
                        mounts = [(name, volume) for name, service in services.items()
                                  for volume in service.get("volumes", [])
                                  if volume.get("target") == "/etc/nginx/review"]
                        self.assertEqual(1, len(mounts))
                        name, mount = mounts[0]
                        self.assertEqual("proxy.openelis.org" if files == (installer,) else "proxy", name)
                        self.assertEqual("bind", mount["type"])
                        self.assertTrue(mount["read_only"])
                        expected = override or ("/var/lib/openelis-global/review" if files == (installer,)
                                                else str(ROOT / "volume/review"))
                        self.assertEqual(expected, mount["source"])


class ReviewProxyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="oe-review-proxy-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.root = Path(cls.temp.name)
        cls.network = "oe-review-test-" + uuid.uuid4().hex[:12]
        run("docker", "network", "create", cls.network)
        cls.addClassCleanup(run, "docker", "network", "rm", cls.network)
        cert = cls.root / "cert.crt"
        key = cls.root / "cert.key"
        run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
            "-keyout", str(key), "-out", str(cert), "-days", "1", "-subj", "/CN=localhost")
        (cls.root / "key_pass").write_text("unused-test-password\n")
        cls.cert_mounts = []
        for source, destinations in {
            cert: ["/etc/nginx/certs/apache-selfsigned.crt", "/etc/nginx/serverCerts/cert.crt"],
            key: ["/etc/nginx/keys/apache-selfsigned.key", "/etc/nginx/serverCerts/cert.key"],
            cls.root / "key_pass": ["/etc/nginx/private/key_pass"],
        }.items():
            for destination in destinations:
                cls.cert_mounts += ["-v", f"{source}:{destination}:ro"]
        upstream = cls.root / "upstream.conf"
        upstream.write_text("""events {} http {
            server { listen 80;
                location / { default_type text/html; return 200 '""" + HTML.decode() + """'; }
                location /asset.js { default_type application/javascript; return 200 'window.fixture = true;'; }
            }
            server { listen 8443 ssl;
                ssl_certificate /etc/nginx/certs/apache-selfsigned.crt;
                ssl_certificate_key /etc/nginx/keys/apache-selfsigned.key;
                location / { default_type application/json; return 200 '{"fixture":"application-session"}'; }
            }
        }""")
        cls.upstream = cls.network + "-upstream"
        run("docker", "run", "-d", "--name", cls.upstream, "--network", cls.network,
            "--network-alias", "frontend.openelis.org", "--network-alias", "oe.openelis.org",
            "-v", f"{upstream}:/etc/nginx/nginx.conf:ro", *cls.cert_mounts, IMAGE)
        cls.addClassCleanup(run, "docker", "rm", "-f", cls.upstream)

    def start_proxy(self, config, review_dir, name):
        result = run("docker", "run", "-d", "--network", self.network, "--name", name,
                     "-p", "127.0.0.1::443", "-v", f"{config}:/etc/nginx/nginx.conf:ro",
                     "-v", f"{review_dir}:/etc/nginx/review:ro", *self.cert_mounts, IMAGE)
        self.addCleanup(run, "docker", "rm", "-f", name)
        inspect = json.loads(run("docker", "inspect", name).stdout)[0]
        port = int(inspect["NetworkSettings"]["Ports"]["443/tcp"][0]["HostPort"])
        return name, port, result.stdout.strip()

    def request(self, proxy, path="/", host="fixture.test", method="GET"):
        # Only generated localhost certificates are exempted from verification.
        connection = http.client.HTTPSConnection("127.0.0.1", proxy[1], timeout=2,
                                                context=ssl._create_unverified_context())
        try:
            connection.request(method, path, headers={"Host": host})
            response = connection.getresponse()
            return response.status, response.getheader("Content-Type"), response.read()
        finally:
            connection.close()

    def await_response(self, proxy, predicate, **kwargs):
        deadline = time.monotonic() + 10
        last = None
        while time.monotonic() < deadline:
            try:
                last = self.request(proxy, **kwargs)
                if predicate(last):
                    return last
            except (OSError, http.client.HTTPException) as error:
                last = str(error)
            time.sleep(0.1)
        logs = run("docker", "logs", proxy[0], check=False)
        self.fail(f"Response did not settle: {last}\n{logs.stderr}")

    def reload(self, proxy):
        master = str(json.loads(run("docker", "inspect", proxy[0]).stdout)[0]["State"]["Pid"])
        def workers():
            # Parent ids remain accurate when an emulated image hides process titles.
            listing = run("docker", "top", proxy[0], "-eo", "pid,ppid,args").stdout
            return {line.split()[0] for line in listing.splitlines()
                    if len(line.split()) > 1 and line.split()[1] == master}

        previous_workers = workers()
        self.assertTrue(previous_workers)
        run("docker", "exec", proxy[0], "nginx", "-t")
        run("docker", "exec", proxy[0], "nginx", "-s", "reload")
        # Reload is asynchronous: old workers finish their current connections.
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            current = workers()
            if current and previous_workers.isdisjoint(current):
                return
            time.sleep(0.1)
        self.fail(f"Nginx did not retire workers after reload: {previous_workers}")

    def exercise_profile(self, profile):
        root = self.root / profile
        root.mkdir()
        review = root / "review"
        review.mkdir()
        config = ROOT / PROFILES[profile]
        original = config.read_bytes()
        # The reference runs the same config with the two optional includes absent.
        baseline = root / "baseline.conf"
        baseline.write_text(re.sub(r"^.*include /etc/nginx/review/active/.*\n", "",
                                   original.decode(), flags=re.MULTILINE))
        reference = self.start_proxy(baseline, review, self.network + "-" + profile + "-baseline")
        candidate = self.start_proxy(config, review, self.network + "-" + profile)
        hosts = ["fixture.test", "storage.openelis-global.org"] if profile == "development" else ["fixture.test"]
        for proxy in (reference, candidate):
            self.await_response(proxy, lambda r: r[0] == 200 and r[2] == HTML)
        before = json.loads(run("docker", "inspect", candidate[0], self.upstream).stdout)
        before_identity = [(v["Id"], v["State"]["StartedAt"]) for v in before]
        endpoints = [("/", "GET"), ("/asset.js", "GET"), ("/api/OpenELIS-Global/session", "GET"), (SUBMIT, "POST")]
        baseline_responses = {(host, path, method): self.request(reference, path, host, method)
                              for host in hosts for path, method in endpoints}
        for (host, path, method), expected in baseline_responses.items():
            self.assertEqual(expected, self.request(candidate, path, host, method))

        release = review / "releases" / "fixture"
        (release / "html").mkdir(parents=True)
        (release / "server").mkdir()
        (release / "html" / "review.conf").write_text(
            'proxy_set_header Accept-Encoding "";\nsub_filter_once on;\n'
            f"sub_filter '</head>' '{SCRIPT}</head>';\n")
        (release / "server" / "review.conf").write_text(
            f'location = {SUBMIT} {{ default_type application/json; return 401 \'{{"review":"fixture"}}\'; }}\n')
        active = review / "active"
        active.symlink_to("releases/fixture", target_is_directory=True)
        for _ in range(2):
            self.reload(candidate)
            for host in hosts:
                page = self.await_response(candidate, lambda r: SCRIPT.encode() in r[2], host=host)
                self.assertEqual(1, page[2].count(SCRIPT.encode()))
                self.assertEqual(HTML.replace(b"</head>", SCRIPT.encode() + b"</head>"), page[2])
                self.assertEqual((401, "application/json", b'{"review":"fixture"}'),
                                 self.request(candidate, SUBMIT, host, "POST"))
                for path in ("/asset.js", "/api/OpenELIS-Global/session"):
                    self.assertEqual(baseline_responses[host, path, "GET"], self.request(candidate, path, host))

        # Invalid optional configuration must not interrupt the already running app.
        (release / "html" / "review.conf").write_text("invalid_review_directive;\n")
        invalid = run("docker", "exec", candidate[0], "nginx", "-t", check=False)
        self.assertNotEqual(0, invalid.returncode)
        self.assertIn(SCRIPT.encode(), self.request(candidate)[2])

        active.unlink()
        for _ in range(2):
            self.reload(candidate)
            self.await_response(candidate, lambda r: r[2] == HTML)
            for (host, path, method), expected in baseline_responses.items():
                self.assertEqual(expected, self.request(candidate, path, host, method))
        after = json.loads(run("docker", "inspect", candidate[0], self.upstream).stdout)
        self.assertEqual(before_identity, [(v["Id"], v["State"]["StartedAt"]) for v in after])
        self.assertEqual(hashlib.sha256(original).hexdigest(), hashlib.sha256(config.read_bytes()).hexdigest())

    def test_development_hooks(self):
        self.exercise_profile("development")

    def test_production_hooks(self):
        self.exercise_profile("production")

    def test_installer_hooks(self):
        self.exercise_profile("installer")


if __name__ == "__main__":
    unittest.main()
