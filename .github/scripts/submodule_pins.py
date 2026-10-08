"""Reports whether the Bridge and mock submodule pins are on their repositories' default branches.

OpenELIS builds the Bridge and analyzer mock images from these pins. Exits non-zero when a pin
is a commit that is not on its repository's default branch or its URL changes repositories.
"""

import argparse
import base64
import configparser
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request

CHECKED = ("tools/openelis-analyzer-bridge", "tools/analyzer-mock-server")


def checked_pins(ls_tree, gitmodules):
    """(path, pinned commit, owner/repo) for each checked submodule."""
    commits = {}
    for line in ls_tree.splitlines():
        meta, _, path = line.partition("\t")
        parts = meta.split()
        if len(parts) == 3 and parts[1] == "commit":
            commits[path] = parts[2]
    config = configparser.ConfigParser()
    config.read_string(re.sub(r"^\s+", "", gitmodules, flags=re.MULTILINE))
    urls = {config[section]["path"]: config[section]["url"] for section in config.sections() if "path" in config[section]}
    checked = []
    for path in CHECKED:
        if path not in commits or path not in urls:
            raise ValueError(f"{path} is not a submodule of this checkout")
        match = re.fullmatch(
            r"(?:https://github\.com/|git@github\.com:|ssh://git@github\.com/)"
            r"([A-Za-z0-9_-]+/[A-Za-z0-9_.-]+)/?",
            urls[path],
            flags=re.IGNORECASE,
        )
        if not match:
            raise ValueError(f"{path} must use a GitHub HTTPS or SSH repository URL")
        checked.append((path, commits[path], match.group(1).removesuffix(".git")))
    return checked


def merged_status(status):
    """GitHub's compare status from the default branch to the pin: merged when nothing is ahead."""
    return status in ("identical", "behind")


def unmerged(checked, on_default_branch):
    return [(path, sha, repository) for path, sha, repository in checked if not on_default_branch(repository, sha)]


def _github(path, token, data=None):
    request = urllib.request.Request(
        f"https://api.github.com/{path}",
        data=json.dumps(data).encode() if data is not None else None,
        headers={"Accept": "application/vnd.github+json", "Content-Type": "application/json"},
    )
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def on_default_branch_via_github(token):
    def check(repository, sha):
        default = _github(f"repos/{repository}", token)["default_branch"]
        return merged_status(_github(f"repos/{repository}/compare/{default}...{sha}", token)["status"])

    return check


def trusted_gitmodules(base_ref):
    """The .gitmodules of the base branch, which a pull request cannot change; this checkout's otherwise.

    A pull request's own .gitmodules could point a checked path at another repository whose
    default branch holds the pin, so repository identity comes from the branch it targets.
    """
    if not base_ref:
        with open(".gitmodules", encoding="utf-8") as handle:
            return handle.read()
    subprocess.run(["git", "fetch", "-q", "--depth=1", "origin", base_ref], check=True, capture_output=True)
    return subprocess.run(
        ["git", "show", "FETCH_HEAD:.gitmodules"], check=True, capture_output=True, text=True
    ).stdout


def validate_repositories(ls_tree, gitmodules, trusted):
    checked = checked_pins(ls_tree, trusted)
    actual = checked_pins(ls_tree, gitmodules)
    for (path, _, repository), (_, _, actual_repository) in zip(checked, actual):
        if repository.casefold() != actual_repository.casefold():
            raise ValueError(f"{path} must reference {repository}; repository changes are not allowed")
    return checked


def main():
    ls_tree = subprocess.run(["git", "ls-tree", "-r", "HEAD"], check=True, capture_output=True, text=True).stdout
    try:
        trusted = trusted_gitmodules(os.environ.get("BASE_REF"))
        gitmodules = subprocess.run(
            ["git", "show", "HEAD:.gitmodules"], check=True, capture_output=True, text=True
        ).stdout
        checked = validate_repositories(ls_tree, gitmodules, trusted)
    except ValueError as error:
        print(f"Submodule validation failed: {error}")
        return 1
    failures = unmerged(checked, on_default_branch_via_github(os.environ.get("GITHUB_TOKEN")))
    for path, sha, repository in checked:
        state = "NOT MERGED" if (path, sha, repository) in failures else "merged"
        print(f"{path} -> {repository}@{sha[:12]}: {state}")
    if failures:
        print(
            "\nA pin marked NOT MERGED is a commit that is not on its repository's default branch. "
            "A commit reaches the default branch when the pull request containing it is merged with a "
            "merge commit; a squash merge leaves it off."
        )
        return 1
    return 0


STATUS_CONTEXT = "Validation / Submodule pins"


def pr_snapshot(repository, sha, token):
    """Read only Git tree/blob data at one immutable PR commit; never check out PR code."""
    trees = {}

    def entry(path):
        tree_sha = sha
        parts = path.split("/")
        for index, part in enumerate(parts):
            if tree_sha not in trees:
                tree = _github(f"repos/{repository}/git/trees/{tree_sha}", token)
                if tree.get("truncated"):
                    raise ValueError("GitHub returned an incomplete Git tree")
                trees[tree_sha] = {item["path"]: item for item in tree["tree"]}
            item = trees[tree_sha].get(part)
            if item is None:
                raise ValueError(f"Missing {path} in PR commit")
            if index < len(parts) - 1:
                if item["type"] != "tree":
                    raise ValueError(f"{path} has a non-directory parent")
                tree_sha = item["sha"]
        return item

    modules = entry(".gitmodules")
    if modules["type"] != "blob" or modules["mode"] not in ("100644", "100755"):
        raise ValueError(".gitmodules must be a regular file")
    blob = _github(f"repos/{repository}/git/blobs/{modules['sha']}", token)
    if blob["encoding"] != "base64":
        raise ValueError("Unexpected .gitmodules encoding")
    gitmodules = base64.b64decode(blob["content"]).decode("utf-8")
    lines = []
    for path in CHECKED:
        pin = entry(path)
        if pin["mode"] != "160000" or pin["type"] != "commit":
            raise ValueError(f"{path} is not a submodule of this checkout")
        lines.append(f"160000 commit {pin['sha']}\t{path}")
    return "\n".join(lines), gitmodules


def check_pull_request(repository, number, token, trusted, run_url, dry_run=False):
    pr = _github(f"repos/{repository}/pulls/{number}", token)
    sha = pr["head"]["sha"]
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Invalid PR head commit")

    def publish(state, description):
        print(f"{STATUS_CONTEXT}: {sha} {state}: {description}")
        if not dry_run:
            _github(f"repos/{repository}/statuses/{sha}", token, {
                "context": STATUS_CONTEXT, "state": state,
                "description": description[:140], "target_url": run_url,
            })

    publish("pending", "Checking analyzer submodule repositories and upstream commits")
    try:
        base = (pr.get("stack") or {}).get("base", {}).get("ref") or pr["base"]["ref"]
        if base != "develop":
            publish("success", "Not applicable: PR does not target develop or a develop-based stack")
            return 0
        ls_tree, gitmodules = pr_snapshot(repository, sha, token)
        checked = validate_repositories(ls_tree, gitmodules, trusted)
        failures = unmerged(checked, on_default_branch_via_github(token))
        if failures:
            raise ValueError("Unmerged upstream pin: " + ", ".join(f"{path}@{pin[:12]}" for path, pin, _ in failures))
    except urllib.error.HTTPError as error:
        publish("failure", f"GitHub API HTTP {error.code}; could not verify upstream pins")
        return 1
    except (ValueError, KeyError, configparser.Error, urllib.error.URLError) as error:
        publish("failure", str(error))
        return 1
    publish("success", "Analyzer repositories match develop and both pins are merged upstream")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pull-request", type=int)
    parser.add_argument("--dry-run", action="store_true", help="Read and validate without publishing commit statuses")
    args = parser.parse_args()
    if args.pull_request is not None:
        with open(".gitmodules", encoding="utf-8") as handle:
            trusted = handle.read()
        repository = os.environ["GITHUB_REPOSITORY"]
        run_url = f"https://github.com/{repository}/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}"
        sys.exit(check_pull_request(repository, args.pull_request, os.environ.get("GITHUB_TOKEN"), trusted, run_url, args.dry_run))
    sys.exit(main())
