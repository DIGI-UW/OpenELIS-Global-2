"""Exercise deployment transport with real certificates and HTTP servers."""

import base64
import http.server
import importlib.util
import os
from pathlib import Path
import shutil
import ssl
import subprocess
import tempfile
import threading
import unittest
import urllib.error
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("deployment_tls", Path(__file__).with_name("deploy-published-testing.py"))
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


class HttpsServer:
    def __enter__(self):
        self.directory = tempfile.TemporaryDirectory()
        self.cert = Path(self.directory.name) / "server.pem"
        key = self.cert.with_suffix(".key")
        subprocess.run(["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                        "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost",
                        "-keyout", str(key), "-out", str(self.cert)], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.requests = []
        self.response = (200, {"Content-Type": "application/json"}, b'{"status":"UP"}')
        fixture = self

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                fixture.requests.append(dict(self.headers))
                code, headers, body = fixture.response
                self.send_response(code)
                for name, value in headers.items():
                    self.send_header(name, value)
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *_args):
                pass

        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(self.cert, key)
        self.server.socket = context.wrap_socket(self.server.socket, server_side=True)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = f"https://localhost:{self.server.server_port}"
        return self

    def __exit__(self, *_args):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.directory.cleanup()


@unittest.skipUnless(shutil.which("openssl"), "OpenSSL is required for real TLS fixtures")
class DeploymentTlsTest(unittest.TestCase):
    def test_trusted_certificate_preserves_authenticated_json_requests(self):
        with HttpsServer() as server, patch.dict(os.environ, {"SSL_CERT_FILE": str(server.cert)}):
            self.assertEqual({"status": "UP"}, deployment.http_json("GET", server.url + "/data",
                             username="test-user", password="synthetic-password", auth_origin=server.url))
            expected = "Basic " + base64.b64encode(b"test-user:synthetic-password").decode()
            self.assertEqual([expected], [request.get("Authorization") for request in server.requests])

    def test_untrusted_certificate_stops_before_sending_credentials(self):
        with HttpsServer() as server, HttpsServer() as other, \
                patch.dict(os.environ, {"SSL_CERT_FILE": str(other.cert)}):
            with self.assertRaises(urllib.error.URLError):
                deployment.http_json("GET", server.url + "/data", auth_origin=server.url)
            self.assertEqual([], server.requests)

    def test_hostname_mismatch_stops_before_sending_credentials(self):
        with HttpsServer() as server, patch.dict(os.environ, {"SSL_CERT_FILE": str(server.cert)}):
            wrong_host = server.url.replace("localhost", "127.0.0.1")
            with self.assertRaises(urllib.error.URLError):
                deployment.http_json("GET", wrong_host + "/data", auth_origin=wrong_host)
            self.assertEqual([], server.requests)

    def test_authenticated_redirect_is_not_followed(self):
        with HttpsServer() as server, HttpsServer() as other, \
                patch.dict(os.environ, {"SSL_CERT_FILE": str(server.cert)}):
            server.response = (302, {"Location": other.url + "/steal"}, b"")
            with self.assertRaises(urllib.error.HTTPError) as error:
                deployment.http_json("GET", server.url + "/data", auth_origin=server.url)
            self.assertEqual(302, error.exception.code)
            self.assertEqual(1, len(server.requests))
            self.assertEqual([], other.requests)

    def test_plain_http_cannot_receive_deployment_credentials(self):
        with patch.object(deployment.urllib.request, "build_opener") as opener:
            with self.assertRaisesRegex(ValueError, "verified HTTPS"):
                deployment.http_json("GET", "http://localhost/data", auth_origin="http://localhost")
            opener.assert_not_called()

    def test_other_origin_and_unauthenticated_requests_do_not_receive_credentials(self):
        with HttpsServer() as server, patch.dict(os.environ, {"SSL_CERT_FILE": str(server.cert)}):
            deployment.http_json("GET", server.url + "/data", auth_origin="https://different.example")
            deployment.http_json("GET", server.url + "/data")
            self.assertEqual([None, None], [request.get("Authorization") for request in server.requests])

    @unittest.skipUnless(shutil.which("curl"), "curl is required to exercise the seed script")
    def test_seed_script_rejects_an_untrusted_server_before_sending_credentials(self):
        with HttpsServer() as server, HttpsServer() as other, tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            script = root / "projects/analyzer-harness/seed-analyzers.sh"
            script.parent.mkdir(parents=True)
            shutil.copy2(Path(__file__).resolve().parents[2] / "projects/analyzer-harness/seed-analyzers.sh", script)
            result = subprocess.run(["bash", str(script), "--ensure-connections", "--no-mock-network"],
                                    env={**os.environ, "BASE_URL": server.url, "DEV_STACK_TLS": "verified",
                                         "CURL_CA_BUNDLE": str(other.cert), "TEST_USER": "synthetic-user",
                                         "TEST_PASS": "synthetic-password"},
                                    capture_output=True, text=True, timeout=30)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("HTTP 000", result.stderr)
            self.assertEqual([], server.requests)


if __name__ == "__main__":
    unittest.main()
