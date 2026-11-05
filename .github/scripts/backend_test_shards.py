#!/usr/bin/env python3
"""Plan the backend Surefire test classes into deterministic CI shards.

The integration suite shares one PostgreSQL database per JVM and most fixtures
commit, so a class can depend on what the classes before it left behind. The
shards therefore keep Surefire's alphabetical order: each one is a contiguous
range of the sorted class names, cut at the boundaries in
.github/ci/backend-test-shards.json, and starts from a freshly migrated
database, exactly like the single full run. Classes annotated
@IsolatedDatabase start their own PostgreSQL and run in a separate shard.
"""

import argparse
import bisect
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

TEST_ROOT = Path("src/test/java")
CONFIG = Path(".github/ci/backend-test-shards.json")
ISOLATED = "isolated"
MODES = ("off", "shadow", "required")

SUREFIRE_DEFAULT_INCLUDE = re.compile(r"^(Test.*|.*Test|.*Tests|.*TestCase)\.java$")
CREATES_CONTAINER = re.compile(r"new\s+(PostgreSQLContainer|GenericContainer)\b")
ISOLATED_MARKER = re.compile(r"^\s*@(org\.openelisglobal\.)?IsolatedDatabase\b", re.MULTILINE)
TEST_ANNOTATION = re.compile(r"@(org\.junit\.)?Test\b")
ABSTRACT_CLASS = re.compile(r"\babstract\s+class\b")


@dataclass(frozen=True)
class TestClass:
    name: str
    path: str
    isolated: bool
    creates_container: bool
    runnable: bool


def discover(repo):
    root = Path(repo) / TEST_ROOT
    classes = []
    for source in root.rglob("*.java"):
        if not SUREFIRE_DEFAULT_INCLUDE.match(source.name):
            continue
        relative = source.relative_to(root).as_posix()
        text = source.read_text(encoding="utf-8", errors="replace")
        classes.append(
            TestClass(
                name=relative[: -len(".java")].replace("/", "."),
                path=relative,
                isolated=bool(ISOLATED_MARKER.search(text)),
                creates_container=bool(CREATES_CONTAINER.search(text)),
                runnable=bool(TEST_ANNOTATION.search(text)) and not ABSTRACT_CLASS.search(text),
            )
        )
    return sorted(classes, key=lambda test_class: test_class.name)


def load_config(repo):
    config = json.loads((Path(repo) / CONFIG).read_text(encoding="utf-8"))
    mode = config.get("mode")
    if mode not in MODES:
        raise ValueError(f"mode must be one of {', '.join(MODES)}, not {mode!r}")
    boundaries = config.get("boundaries")
    if not isinstance(boundaries, list) or not boundaries or not all(isinstance(b, str) and b for b in boundaries):
        raise ValueError("boundaries must be a non-empty list of class names")
    if boundaries != sorted(set(boundaries)):
        raise ValueError("boundaries must be sorted and unique")
    forks = config.get("isolatedForks", 1)
    if not isinstance(forks, int) or forks < 1:
        raise ValueError("isolatedForks must be a positive integer")
    return {"mode": mode, "boundaries": boundaries, "isolatedForks": forks}


def shard_ids(boundaries):
    return [str(index) for index in range(1, len(boundaries) + 2)]


def plan(classes, boundaries):
    shards = {shard: [] for shard in shard_ids(boundaries)}
    shards[ISOLATED] = []
    for test_class in classes:
        if test_class.isolated:
            shards[ISOLATED].append(test_class)
        else:
            shards[str(bisect.bisect_right(boundaries, test_class.name) + 1)].append(test_class)
    return shards


def problems(classes, boundaries):
    found = []
    for test_class in classes:
        if test_class.creates_container and not test_class.isolated:
            found.append(
                f"{test_class.path} starts its own container; annotate the class with "
                "@IsolatedDatabase so it runs in the isolated shard"
            )
    for shard, members in plan(classes, boundaries).items():
        if not any(member.runnable for member in members):
            found.append(f"shard {shard} has no runnable test class; adjust {CONFIG}")
    return found


def write_patterns(path, members):
    lines = [member.path for member in members]
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text("".join(f"{line}\n" for line in lines), encoding="utf-8")
    return len(lines)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=".", help="repository root (default: current directory)")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check", help="validate the markers and the shard boundaries, print the plan")
    commands.add_parser("mode", help="print the configured mode")
    commands.add_parser("matrix", help="print the shared-database shard ids as a JSON array")
    commands.add_parser("forks", help="print how many JVMs the isolated shard uses")
    includes = commands.add_parser("includes", help="write a Surefire includesFile for one shard")
    includes.add_argument("--shard", required=True)
    includes.add_argument("--output", required=True)
    excludes = commands.add_parser("excludes-isolated", help="write a Surefire excludesFile of the isolated classes")
    excludes.add_argument("--output", required=True)
    args = parser.parse_args(argv)

    config = load_config(args.repo)
    if args.command == "mode":
        print(config["mode"])
        return 0
    if args.command == "matrix":
        print(json.dumps(shard_ids(config["boundaries"])))
        return 0
    if args.command == "forks":
        print(config["isolatedForks"])
        return 0

    classes = discover(args.repo)
    issues = problems(classes, config["boundaries"])
    for issue in issues:
        print(f"::error::{issue}", file=sys.stderr)
    if issues:
        return 1

    shards = plan(classes, config["boundaries"])
    if args.command == "check":
        for shard, members in shards.items():
            runnable = sum(1 for member in members if member.runnable)
            first = members[0].name if members else "-"
            print(f"shard {shard}: {len(members)} classes ({runnable} with tests), first {first}")
        return 0
    if args.command == "includes":
        if args.shard not in shards:
            print(f"::error::unknown shard {args.shard}; known: {', '.join(shards)}", file=sys.stderr)
            return 2
        count = write_patterns(args.output, shards[args.shard])
        print(f"shard {args.shard}: {count} classes -> {args.output}")
        return 0
    if args.command == "excludes-isolated":
        count = write_patterns(args.output, shards[ISOLATED])
        print(f"{count} isolated classes -> {args.output}")
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
