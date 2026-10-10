#!/usr/bin/env python3
"""Restore the testing site's baseline using its currently deployed release."""

import argparse
import datetime
import fcntl
import importlib.util
import json
import os
import pathlib
import re
import sys
import tempfile
import urllib.parse


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, pathlib.Path(__file__).with_name(filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


deployment = load_module("deployment", "deploy-published-testing.py")
readiness = load_module("readiness", "check-readiness.py")
DATA_VOLUMES = {"db-data", "bridge-state", "analyzer-imports", "reporting-data"}
LOCK = "/tmp/openelis-testing-deploy.lock"


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def publish_status(site, report):
    state = site / ".openelis-ci"
    previous = state / "reset-status.json"
    if previous.exists():
        last = json.loads(previous.read_text())
        report.setdefault("lastSuccessfulReset", last.get("lastSuccessfulReset"))
    if report["state"] == "ready":
        report["lastSuccessfulReset"] = report["finishedAt"]
    deployment.publish_baseline_status(site, report)


def reset_plan(site):
    target = json.loads((site / ".openelis-ci/target.json").read_text())
    if target.get("instance") != "testing" or target.get("appBranch") != "develop":
        raise ValueError("Reset requires a recorded testing deployment of develop")
    sha = target.get("appSha", "")
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Deployment has no valid source revision")
    release = pathlib.Path(target["release"])
    if release.is_symlink() or release.resolve() != (site / "releases" / sha).resolve():
        raise ValueError("Recorded release is outside the testing site")
    if not (release / deployment.SEED_SCRIPT).is_file() or not (site / ".env").is_file():
        raise ValueError("Deployed release or site configuration is incomplete")
    catalog = release / deployment.BASELINE_CATALOG
    if not catalog.is_dir():
        raise ValueError("Release has no versioned testing baseline catalog")
    deployment.require_stack_owner(site)
    override = site / ".openelis-ci/deployment-images.json"
    compose = deployment.compose_command(site, release, override)
    config = json.loads(deployment.run(compose + ["config", "--format", "json"], site, True))
    volumes = []
    for key in sorted(DATA_VOLUMES):
        definition = config.get("volumes", {}).get(key)
        if not definition or definition.get("external"):
            raise ValueError(f"Reset requires project-owned volume {key}")
        name = definition["name"]
        metadata = json.loads(deployment.run(["docker", "volume", "inspect", name], site, True))[0]
        labels = metadata.get("Labels") or {}
        if labels.get("com.docker.compose.project") != deployment.PROJECT or labels.get("com.docker.compose.volume") != key:
            raise ValueError(f"Volume {name} does not belong to this testing stack")
        volumes.append(name)
    # Refuse an overlay that relocates reset data into unowned host storage.
    for service in config["services"].values():
        for mount in service.get("volumes", []):
            if mount.get("type") == "bind" and not mount.get("read_only", False):
                path = pathlib.Path(mount["source"]).resolve()
                infrastructure = mount.get("target") in {"/etc/nginx/certs", "/etc/nginx/certs/",
                                                         "/etc/nginx/keys", "/etc/nginx/keys/"}
                if not infrastructure and not path.is_relative_to(site):
                    raise ValueError(f"Writable bind mount is outside testing: {path}")
    directories = [site / "configuration/backend", site / "lucene"]
    expected_binds = {"/var/lib/openelis-global/configuration/backend": directories[0],
                      "/var/lib/lucene_index": directories[1]}
    for destination, expected in expected_binds.items():
        matches = [mount for mount in config["services"]["oe.openelis.org"].get("volumes", [])
                   if mount.get("target") == destination]
        if len(matches) != 1 or matches[0].get("type") != "bind" or pathlib.Path(matches[0]["source"]).resolve() != expected:
            raise ValueError(f"Unexpected baseline storage at {destination}")
        if expected.is_symlink() or not expected.is_dir():
            raise ValueError(f"Baseline directory is missing or is a symlink: {expected}")
    # Record every actual image, including Bridge, mock and certificate generator.
    # The reset must not resolve mutable tags or fetch new software.
    images = {}
    for service in config["services"]:
        ids = deployment.run(compose + ["ps", "--all", "-q", service], site, True).split()
        if len(ids) != 1:
            raise ValueError(f"Expected one deployed container for {service}")
        container = json.loads(deployment.run(["docker", "inspect", ids[0]], site, True))[0]
        if (container.get("Config", {}).get("Labels") or {}).get("com.docker.compose.project") != deployment.PROJECT:
            raise ValueError(f"Container {service} belongs to another project")
        images[service] = container["Image"]
        deployment.run(["docker", "image", "inspect", container["Image"]], site, True)
        if service in target["images"] and images[service] != target["images"][service]["imageId"]:
            raise ValueError(f"Running {service} differs from the recorded deployment")
    contract = target.get("readinessContract") or {"url": target["verification"]["url"]}
    readiness.validate_contract(**contract)
    return {"target": target, "release": release, "compose": compose, "volumes": volumes,
            "directories": directories, "catalog": catalog, "images": images, "readiness": contract}


def reset(site, scheduled=False):
    state = site / ".openelis-ci"
    if scheduled and (state / "skip-next-reset").exists():
        (state / "skip-next-reset").unlink()
        publish_status(site, {"state": "skipped", "finishedAt": now(), "message": "Skipped once by operator request"})
        return
    plan = reset_plan(site)
    report = {"state": "resetting", "startedAt": now(), "appSha": plan["target"]["appSha"]}
    publish_status(site, report)
    try:
        settings = deployment.site_settings(site, plan["release"])
        origin = "{0.scheme}://{0.netloc}".format(urllib.parse.urlsplit(plan["readiness"]["url"]))
        # Data reset does not require deleting a network shared with site tools.
        deployment.run(plan["compose"] + ["stop"], site)
        deployment.run(plan["compose"] + ["rm", "--force"], site)
        deployment.run(["docker", "volume", "rm", *plan["volumes"]], site)
        for directory in plan["directories"]:
            deployment.run(["docker", "run", "--rm", "--network", "none", "--user", "0", "--entrypoint", "sh",
                            "--mount", f"type=bind,src={directory},dst=/reset",
                            plan["images"]["oe.openelis.org"], "-c",
                            "find /reset -mindepth 1 -delete"], site)
        with tempfile.TemporaryDirectory(prefix="reset-", dir=state) as temporary:
            override = pathlib.Path(temporary) / "images.json"
            deployment.write_json(override, {"services": {service: {"image": image}
                                                         for service, image in plan["images"].items()}})
            deployment.run(plan["compose"] + ["-f", str(override), "up", "-d", "--pull", "never", "--no-build"], site)
        health = readiness.wait_until_ready(**plan["readiness"])
        if not health["ready"]:
            raise RuntimeError("Application did not become ready after baseline reset")
        deployment.run(["env", "BASE_URL=" + origin, "MOCK_URL=" + settings["MOCK_URL"], "bash",
                        str(plan["release"] / deployment.SEED_SCRIPT), "--ensure-connections",
                        "--no-mock-network"], plan["release"],
                       env={**os.environ, **settings})
        report.update(state="ready", finishedAt=now(), message="Baseline loaded through normal configuration and setup APIs; new analyzers await ordinary review and activation")
        target = plan["target"]
        target["lastResetAt"] = report["finishedAt"]
        # Prior delivery proof refers to discarded data. CI owns workflow validation.
        target["verification"].pop("analyzerDelivery", None)
        target["verification"]["readiness"] = health
        target["verification"]["baselineInitializedAt"] = report["finishedAt"]
        deployment.write_json(state / "target.json", target)
        publish_status(site, report)
    except Exception as error:
        report.update(state="failed", finishedAt=now(), message="Reset failed; inspect journalctl -u openelis-testing-reset.service")
        target = plan["target"]
        target["state"] = "reset-failed"
        target["verification"].pop("analyzerDelivery", None)
        deployment.write_json(state / "target.json", target)
        publish_status(site, report)
        raise error


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["reset", "plan", "status", "skip-next"])
    parser.add_argument("site", type=pathlib.Path)
    parser.add_argument("--yes", action="store_true", help="Discard this testing site's synthetic data")
    parser.add_argument("--scheduled", action="store_true")
    args = parser.parse_args()
    site = args.site.resolve()
    state = site / ".openelis-ci"
    if not state.is_dir():
        parser.error("Site has no testing deployment state")
    if args.action == "reset" and not args.yes:
        parser.error("Reset discards synthetic data; pass --yes")
    try:
        with open(LOCK, "w", encoding="utf-8") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            if args.action == "reset":
                reset(site, args.scheduled)
            elif args.action == "plan":
                plan = reset_plan(site)
                print(json.dumps({"appSha": plan["target"]["appSha"], "volumes": plan["volumes"],
                                  "directories": [str(path) for path in plan["directories"]]}, indent=2))
            elif args.action == "status":
                print((state / "reset-status.json").read_text())
            else:
                (state / "skip-next-reset").touch()
                print("The next scheduled reset will be skipped once")
    except Exception as error:
        print(f"Testing reset failed: {error}", file=sys.stderr)
        if args.action == "reset" and not isinstance(error, BlockingIOError):
            publish_status(site, {"state": "failed", "finishedAt": now(),
                                  "message": "Reset failed; inspect journalctl -u openelis-testing-reset.service"})
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
