"""Hello add-on service: receives OpenELIS events and answers its UI panel.

Standard library only. Environment (from the compose snippet OpenELIS prints
when the add-on is installed):
  OPENELIS_API_URL         e.g. https://oe.openelis.org:8443/api/OpenELIS-Global/rest/addon-api/v1
  OPENELIS_API_KEY         service key (oeak_...), for event handling
  OPENELIS_WEBHOOK_SECRET  whsec_..., to verify event signatures
  OPENELIS_CA_FILE         CA certificate of the OpenELIS backend (optional, see README)
"""
import base64
import hashlib
import hmac
import json
import os
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

API = os.environ["OPENELIS_API_URL"].rstrip("/")
API_KEY = os.environ["OPENELIS_API_KEY"]
SECRET = base64.b64decode(os.environ["OPENELIS_WEBHOOK_SECRET"].removeprefix("whsec_"))
TOLERANCE = 300  # seconds
seen = {}  # webhook-id -> time, for de-duplication (delivery is at least once)

if os.environ.get("OPENELIS_CA_FILE"):
    TLS = ssl.create_default_context(cafile=os.environ["OPENELIS_CA_FILE"])
else:
    print("WARNING: OPENELIS_CA_FILE not set, not verifying the OpenELIS certificate", flush=True)
    TLS = ssl._create_unverified_context()


def api(path, credential):
    req = urllib.request.Request(API + path, headers={"Authorization": "Bearer " + credential})
    with urllib.request.urlopen(req, context=TLS, timeout=10) as r:
        return json.load(r)


def summary(acc, credential):
    results = api("/orders/" + urllib.parse.quote(acc) + "/results", credential)["results"]
    validated = sum(1 for r in results if r["status"] == "Finalized")
    return {"accessionNumber": acc, "total": len(results), "validated": validated}


def signature_ok(headers, body):
    msg_id, ts, sigs = headers.get("webhook-id"), headers.get("webhook-timestamp"), headers.get("webhook-signature")
    if not (msg_id and ts and sigs) or abs(time.time() - int(ts)) > TOLERANCE:
        return False
    mac = hmac.new(SECRET, f"{msg_id}.{ts}.".encode() + body, hashlib.sha256).digest()
    expected = "v1," + base64.b64encode(mac).decode()
    return any(hmac.compare_digest(expected, s) for s in sigs.split())


class Handler(BaseHTTPRequestHandler):
    def reply(self, code, payload):
        data = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        if self.path != "/events":
            return self.reply(404, {})
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if not signature_ok(self.headers, body):
            return self.reply(401, {"error": "bad signature"})
        msg_id = self.headers["webhook-id"]
        if msg_id in seen:
            return self.reply(200, {"duplicate": True})
        seen[msg_id] = time.time()
        event = json.loads(body)
        acc = event["data"].get("accessionNumber")
        if event["type"] == "result.validated" and acc:
            print(event["type"], summary(acc, API_KEY), flush=True)
        self.reply(200, {"ok": True})

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        if url.path == "/health":
            return self.reply(200, {"ok": True})
        if url.path != "/api/summary":
            return self.reply(404, {})
        token = self.headers.get("Authorization", "").removeprefix("Bearer ").strip()
        acc = urllib.parse.parse_qs(url.query).get("acc", [""])[0]
        try:
            self.reply(200, summary(acc, token))
        except urllib.error.HTTPError as e:
            self.reply(e.code, {"error": "openelis said " + str(e.code)})


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
