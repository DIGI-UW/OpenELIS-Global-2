"""Reports whether the Bridge and mock submodule pins are on their repositories' default branches.

OpenELIS builds the Bridge and analyzer mock images from these pins. Exits non-zero when a pin
is a commit that is not on its repository's default branch.
"""

import configparser
import json
import os
import re
import subprocess
import sys
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
        repository = re.sub(r"^(git@github\.com:|https://github\.com/)", "", urls[path])
        checked.append((path, commits[path], re.sub(r"\.git$", "", repository)))
    return checked


def merged_status(status):
    """GitHub's compare status from the default branch to the pin: merged when nothing is ahead."""
    return status in ("identical", "behind")


def unmerged(checked, on_default_branch):
    return [(path, sha, repository) for path, sha, repository in checked if not on_default_branch(repository, sha)]


def _github(path, token):
    request = urllib.request.Request(f"https://api.github.com/{path}", headers={"Accept": "application/vnd.github+json"})
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def on_default_branch_via_github(token):
    def check(repository, sha):
        default = _github(f"repos/{repository}", token)["default_branch"]
        return merged_status(_github(f"repos/{repository}/compare/{default}...{sha}", token)["status"])

    return check


def main():
    ls_tree = subprocess.run(["git", "ls-tree", "-r", "HEAD"], check=True, capture_output=True, text=True).stdout
    with open(".gitmodules", encoding="utf-8") as handle:
        checked = checked_pins(ls_tree, handle.read())
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


if __name__ == "__main__":
    sys.exit(main())
