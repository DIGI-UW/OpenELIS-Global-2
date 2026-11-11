import contextlib
import http.server
import importlib.util
import io
from pathlib import Path
import threading
import unittest

spec = importlib.util.spec_from_file_location("readiness", Path(__file__).with_name("check-readiness.py"))
readiness = importlib.util.module_from_spec(spec)
spec.loader.exec_module(readiness)


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        code, content_type, body = self.server.response
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_args):
        pass


class ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}/health"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def test_only_successful_json_health_is_accepted(self):
        cases = [
            (200, "application/json", b'{"status":"UP"}', True),
            (200, "application/json", b'{\n "status": "UP"\n}', True),
            (200, "text/html", b'<html>expected "status":"UP"</html>', False),
            (200, "application/json", b'<html>"status":"UP"</html>', False),
            (302, "application/json", b'{"status":"UP"}', False),
            (500, "application/json", b'{"status":"UP"}', False),
            (200, "application/json", b'{"status":"DOWN"}', False),
            (200, "application/json", b'{}', False),
        ]
        for code, content_type, body, expected in cases:
            with self.subTest(code=code, content_type=content_type, body=body):
                self.server.response = (code, content_type, body)
                self.assertEqual(expected, readiness.probe(self.url)[0])

    def test_contract_can_use_a_nested_boolean_health_field(self):
        self.server.response = (200, "application/health+json", b'{"database":{"ready":true}}')
        self.assertTrue(readiness.probe(self.url, "database.ready", True)[0])

    def test_failure_stops_at_the_deadline(self):
        clock = [0]
        requests = []

        def check(*args):
            requests.append(args[-1])
            return False, "HTTP 503"

        def sleep(seconds):
            clock[0] += seconds

        with contextlib.redirect_stdout(io.StringIO()):
            result = readiness.wait_until_ready(self.url, timeout=3, interval=2, request_timeout=10,
                                                clock=lambda: clock[0], sleep=sleep, check=check)
        self.assertFalse(result["ready"])
        self.assertEqual(3, clock[0])
        self.assertEqual([3, 1], requests)

    def test_retry_can_succeed(self):
        outcomes = iter([(False, "HTTP 503"), (True, "ready")])
        with contextlib.redirect_stdout(io.StringIO()):
            report = readiness.wait_until_ready(self.url, check=lambda *_args: next(outcomes), sleep=lambda _: None)
        self.assertTrue(report["ready"])
        self.assertEqual(2, report["attempts"])

    def test_invalid_timeout_is_rejected(self):
        with self.assertRaises(ValueError):
            readiness.wait_until_ready(self.url, timeout=0)


if __name__ == "__main__":
    unittest.main()
