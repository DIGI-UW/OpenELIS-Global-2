#!/usr/bin/env python3
"""Deploy a develop commit's own compose stack with its published images."""

import argparse
import base64
import datetime
import fcntl
import importlib.util
import json
import pathlib
import re
import shutil
import ssl
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.parse
import urllib.request


APP_REPOSITORY = "https://github.com/DIGI-UW/OpenELIS-Global-2.git"
PROJECT = "openelis-testing"
SERVICES = {
    "oe.openelis.org": "openelis-global-2",
    "db.openelis.org": "openelis-global-2-database",
    "fhir.openelis.org": "openelis-global-2-fhir",
    "frontend.openelis.org": "openelis-global-2-frontend",
    "proxy": "openelis-global-2-proxy",
}
ANALYZER_SERVICES = ("openelis-analyzer-bridge", "astm-simulator")
SEED_SCRIPT = "projects/analyzer-harness/seed-analyzers.sh"
BUNDLE_FILES = (
    "docker-compose.yml",
    "docker-compose.analyzers.yml",
    "volume/properties/common.properties",
    "volume/openelis-analyzer-bridge/configuration.yml",
    SEED_SCRIPT,
    "projects/analyzer-harness/seed-mvp-traffic.sh",
)
DEFAULT_MOCK_URL = "http://127.0.0.1:8085"
SMOKE_ANALYZER = "Cepheid GeneXpert (ASTM Mode)"
SMOKE_DESTINATION = "tcp://openelis-analyzer-bridge:9600"
TEST_USER = "admin"
TEST_PASS = "adminADMIN!"


def validate_manifest(manifest, namespace="itechuw"):
    if not isinstance(namespace, str) or not re.fullmatch(r"[a-z0-9][a-z0-9_-]*", namespace):
        raise ValueError("Invalid DockerHub namespace")
    if not isinstance(manifest, dict):
        raise ValueError("Manifest must be a JSON object")
    sha = manifest.get("appSha")
    if not isinstance(sha, str) or not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Manifest must identify the tested application commit")
    if manifest.get("appBranch") != "develop":
        raise ValueError("Testing requires a develop image manifest")
    images = manifest.get("images")
    if not isinstance(images, dict) or set(images) != set(SERVICES):
        raise ValueError("Manifest must include exactly the five application images")
    for service, repository in SERVICES.items():
        reference = images[service]
        if not isinstance(reference, str) or not re.fullmatch(
                re.escape(namespace + "/" + repository) + r"@sha256:[0-9a-f]{64}", reference):
            raise ValueError(f"{service} must use a published DockerHub image digest")
    return manifest


def run(args, cwd, capture=False):
    return subprocess.run(args, cwd=cwd, check=True, text=True,
                          stdout=subprocess.PIPE if capture else None).stdout


def require_current_candidate(sha, cwd):
    head = run(["git", "ls-remote", APP_REPOSITORY, "refs/heads/develop"], cwd, True).split()
    if len(head) != 2 or head != [sha, "refs/heads/develop"]:
        raise ValueError("Candidate is no longer develop HEAD; refusing an obsolete deployment")


def write_json(path, value):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def require_stack_owner(site_dir):
    containers = run(["docker", "ps", "--filter", "publish=80", "--filter", "publish=443",
                      "--format", "{{.ID}}"], site_dir, True).split()
    if not containers:
        return
    for container in json.loads(run(["docker", "inspect", *containers], site_dir, True)):
        labels = container.get("Config", {}).get("Labels") or {}
        if labels.get("com.docker.compose.project") != PROJECT:
            raise ValueError("Ports 80/443 belong to another Docker stack; an explicit cutover is required")


def unpack_release(bundle, site_dir, sha):
    releases = site_dir / "releases"
    releases.mkdir(exist_ok=True)
    release = releases / sha
    with tempfile.TemporaryDirectory(prefix="unpack-", dir=releases) as staging:
        staging = pathlib.Path(staging)
        with tarfile.open(bundle) as archive:
            archive.extractall(staging, filter="data")
        missing = [name for name in BUNDLE_FILES if not (staging / name).is_file()]
        if missing:
            raise ValueError("Deployment bundle is missing " + ", ".join(missing))
        # The search index is runtime state; every release shares the site's copy.
        lucene = site_dir / "lucene"
        lucene.mkdir(exist_ok=True)
        (staging / "volume/lucene").symlink_to(lucene, target_is_directory=True)
        if release.exists():
            shutil.rmtree(release)
        staging.rename(release)
        staging.mkdir()
    return release


def compose_command(site_dir, release, override=None):
    command = ["docker", "compose", "-p", PROJECT, "--project-directory", str(release),
               "--env-file", str(site_dir / ".env"),
               "-f", str(release / "docker-compose.yml"), "-f", str(release / "docker-compose.analyzers.yml")]
    site_overlay = site_dir / "docker-compose.site.yml"
    if site_overlay.is_file():
        command += ["-f", str(site_overlay)]
    return command + (["-f", str(override)] if override else [])


def smoke_accession(run_id):
    digits = re.sub(r"\D", "", run_id)
    return "DEV019" + digits.zfill(14)[-14:]


def http_json(method, url, body=None):
    token = base64.b64encode(f"{TEST_USER}:{TEST_PASS}".encode()).decode()
    request = urllib.request.Request(url, method=method, data=None if body is None else json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json", "Authorization": "Basic " + token})
    with urllib.request.urlopen(request, timeout=30, context=ssl._create_unverified_context()) as response:
        return json.load(response)


def verify_analyzer_delivery(api_base, mock_url, accession, http=http_json, sleep=time.sleep, timeout=120):
    pushed = http("POST", mock_url + "/simulate/astm/genexpert_astm",
                  {"destination": SMOKE_DESTINATION, "sample_id": accession})
    if pushed.get("pushed") != 1:
        raise RuntimeError(f"Mock did not deliver the smoke result: {pushed}")
    analyzers = http("GET", api_base + "/analyzer/analyzers").get("analyzers", [])
    analyzer_id = next(item["id"] for item in analyzers if item.get("name") == SMOKE_ANALYZER)
    deadline = time.monotonic() + timeout
    while True:
        response = http("GET", f"{api_base}/AnalyzerResults?id={analyzer_id}")
        rows = [row for row in response.get("resultList", []) if row.get("accessionNumber") == accession]
        if rows:
            return {"accession": accession, "rows": len(rows)}
        if time.monotonic() >= deadline:
            raise RuntimeError(f"analyzer result {accession} never reached OpenELIS: {response}")
        sleep(3)


def prune_releases(site_dir, current, previous):
    keep = {current.resolve()} | ({pathlib.Path(previous).resolve()} if previous else set())
    for release in (site_dir / "releases").iterdir():
        if release.resolve() not in keep:
            shutil.rmtree(release)


def deploy(request, diagnostics, bundle):
    manifest = validate_manifest(request["manifest"], request.get("dockerhub_namespace", "itechuw"))
    spec = importlib.util.spec_from_file_location("readiness", pathlib.Path(__file__).with_name("check-readiness.py"))
    readiness = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(readiness)
    contract = request["readiness"]
    readiness.validate_contract(**contract)
    site_dir = pathlib.Path(request["deploy_path"]).resolve()
    if not (site_dir / ".env").is_file():
        raise ValueError("The server must have an existing configured .env")
    state_dir = site_dir / ".openelis-ci"
    override = state_dir / "deployment-images.json"
    target_path = state_dir / "target.json"
    origin = "{0.scheme}://{0.netloc}".format(urllib.parse.urlsplit(contract["url"]))
    mock_url = request.get("mock_url", DEFAULT_MOCK_URL)
    release = None
    try:
        require_current_candidate(manifest["appSha"], site_dir)
        require_stack_owner(site_dir)
        state_dir.mkdir(exist_ok=True)
        release = unpack_release(bundle, site_dir, manifest["appSha"])
        # Stage on the same filesystem so promotion is atomic.
        with tempfile.TemporaryDirectory(prefix="candidate-", dir=state_dir) as candidate_dir:
            candidate_override = pathlib.Path(candidate_dir) / override.name
            write_json(candidate_override, {"services": {service: {"image": image}
                                                        for service, image in manifest["images"].items()}})
            run(compose_command(site_dir, release, candidate_override) + ["pull"], site_dir)
            require_current_candidate(manifest["appSha"], site_dir)
            require_stack_owner(site_dir)
            if override.is_file():
                shutil.copy2(override, diagnostics / "previous-images.json")
            candidate_override.replace(override)
        previous_release = None
        if target_path.is_file():
            shutil.copy2(target_path, diagnostics / "previous-target.json")
            previous_release = json.loads(target_path.read_text(encoding="utf-8")).get("release")
            target_path.unlink()
        compose = compose_command(site_dir, release, override)
        run(compose + ["up", "-d"], site_dir)
        images = {}
        for service, reference in manifest["images"].items():
            container = run(compose + ["ps", "-q", service], site_dir, True).strip()
            if not container or "\n" in container:
                raise ValueError(f"Expected one running container for {service}")
            actual = json.loads(run(["docker", "inspect", container], site_dir, True))[0]
            expected = json.loads(run(["docker", "image", "inspect", reference], site_dir, True))[0]
            if not actual["State"]["Running"] or actual["Image"] != expected["Id"]:
                raise ValueError(f"Running {service} does not match the published image")
            images[service] = {"reference": reference, "imageId": actual["Image"]}
        report = readiness.wait_until_ready(**contract)
        write_json(diagnostics / "readiness.json", report)
        if not report["ready"]:
            raise RuntimeError("Application did not become ready; inspect deployment diagnostics")
        run(["env", "BASE_URL=" + origin, "MOCK_URL=" + mock_url,
             "bash", str(release / SEED_SCRIPT), "--ensure-connections", "--no-mock-network", "--activate"],
            release)
        delivery = verify_analyzer_delivery(origin + "/api/OpenELIS-Global/rest", mock_url,
                                            smoke_accession(request["run_id"]))
        target = {"instance": "testing", "state": "ready", "appSha": manifest["appSha"],
                  "appBranch": manifest["appBranch"], "release": str(release), "images": images,
                  "deploymentId": request["run_id"],
                  "deployedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  "verification": {"readiness": report, "url": contract["url"], "analyzerDelivery": delivery}}
        write_json(target_path, target)
        write_json(diagnostics / "target.json", target)
        prune_releases(site_dir, release, previous_release)
        run(["docker", "image", "prune", "--all", "--force"], site_dir)
        print(f"Testing is ready at {manifest['appSha']}", flush=True)
    finally:
        if release is None:
            (diagnostics / "compose-status.txt").write_text("No release was unpacked.\n", encoding="utf-8")
        else:
            status_compose = compose_command(site_dir, release)
            for filename, args in [("compose-status.txt", ["ps", "--all"]),
                                   ("service-logs.txt", ["logs", "--no-color", "--tail", "250",
                                                         *SERVICES, *ANALYZER_SERVICES])]:
                with (diagnostics / filename).open("w", encoding="utf-8") as output:
                    subprocess.run(status_compose + args, cwd=site_dir, stdout=output,
                                   stderr=subprocess.STDOUT, check=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("request", type=pathlib.Path)
    args = parser.parse_args()
    run_dir = args.request.resolve().parent
    diagnostics = run_dir / "diagnostics"
    diagnostics.mkdir(exist_ok=True)
    try:
        request = json.loads(args.request.read_text(encoding="utf-8"))
        with open("/tmp/openelis-testing-deploy.lock", "w", encoding="utf-8") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            deploy(request, diagnostics, run_dir / "deploy-bundle.tgz")
    except Exception as error:
        (diagnostics / "failure.txt").write_text(str(error) + "\n", encoding="utf-8")
        print(f"Deployment failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
