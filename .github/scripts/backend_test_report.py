#!/usr/bin/env python3
"""Merge Surefire reports from the backend test shards into one summary.

It lists failures, errors and skipped tests with their reasons, prints the
minutes each shard spent, and fails when a shard did not run every test class
it was planned to run or when a class ran in two shards. With --baseline it
also compares each test's outcome against the single full run of the same
commit, which is how the shards are validated before they become required.
"""

import argparse
import importlib.util
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

_spec = importlib.util.spec_from_file_location("backend_test_shards", Path(__file__).with_name("backend_test_shards.py"))
shards_module = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(shards_module)

OUTCOMES = ("failure", "error", "skipped")


@dataclass
class Suite:
    name: str
    seconds: float
    cases: dict = field(default_factory=dict)


def read_reports(directory):
    suites = {}
    for report in sorted(Path(directory).rglob("TEST-*.xml")):
        root = ET.parse(report).getroot()
        suite = Suite(name=root.get("name", report.stem[len("TEST-") :]), seconds=float(root.get("time") or 0))
        for case in root.iter("testcase"):
            outcome, message = "passed", ""
            for kind in OUTCOMES:
                element = case.find(kind)
                if element is not None:
                    outcome = kind
                    message = (element.get("message") or element.get("type") or "").strip().splitlines()
                    message = message[0] if message else ""
                    break
            key = f"{case.get('classname', suite.name)}#{case.get('name')}"
            suite.cases[key] = (outcome, message)
        suites[suite.name] = suite
    return suites


def merge_outcomes(results):
    merged = {}
    for suites in results.values():
        for suite in suites.values():
            merged.update({key: outcome for key, (outcome, _) in suite.cases.items()})
    return merged


def completeness(results, planned):
    issues = []
    seen = {}
    for shard, suites in results.items():
        if not suites:
            issues.append(f"shard {shard} produced no test reports")
            continue
        for name in suites:
            if name in seen:
                issues.append(f"{name} ran in shard {seen[name]} and shard {shard}")
            seen[name] = shard
        expected = {test_class.name for test_class in planned.get(shard, []) if test_class.runnable}
        missing = sorted(expected - set(suites))
        if missing:
            issues.append(f"shard {shard} did not run {len(missing)} planned classes: {', '.join(missing[:10])}")
    return issues


def compare(shard_outcomes, baseline_outcomes):
    differences = []
    for key in sorted(set(shard_outcomes) | set(baseline_outcomes)):
        mine, theirs = shard_outcomes.get(key, "absent"), baseline_outcomes.get(key, "absent")
        if mine != theirs:
            differences.append((key, theirs, mine))
    return differences


def render(results, issues, differences, baseline_used):
    lines = ["## Backend test shards", "", "| Shard | Classes | Tests | Failed | Errors | Skipped | Minutes |", "|---|--:|--:|--:|--:|--:|--:|"]
    problems, skipped = [], []
    for shard, suites in results.items():
        counts = {"passed": 0, "failure": 0, "error": 0, "skipped": 0}
        for suite in suites.values():
            for key, (outcome, message) in suite.cases.items():
                counts[outcome] += 1
                if outcome in ("failure", "error"):
                    problems.append(f"- `{key}` ({outcome}, shard {shard}): {message}")
                elif outcome == "skipped":
                    skipped.append(f"- `{key}` (shard {shard}): {message or 'no reason given'}")
        tests = sum(counts.values())
        minutes = sum(suite.seconds for suite in suites.values()) / 60
        lines.append(f"| {shard} | {len(suites)} | {tests} | {counts['failure']} | {counts['error']} | {counts['skipped']} | {minutes:.1f} |")
    for title, items in (("Failures and errors", problems), ("Completeness problems", [f"- {issue}" for issue in issues]), ("Skipped tests", skipped)):
        lines += ["", f"### {title} ({len(items)})", ""] + (items or ["None."])
    if baseline_used:
        rows = [f"- `{key}`: single run {theirs}, shards {mine}" for key, theirs, mine in differences]
        lines += ["", f"### Differences from the single full run ({len(rows)})", ""] + (rows or ["None: every test had the same outcome."])
    return "\n".join(lines) + "\n", problems


def parse_shard_argument(value):
    shard, separator, directory = value.partition("=")
    if not separator or not shard or not directory:
        raise argparse.ArgumentTypeError(f"expected SHARD=DIRECTORY, got {value!r}")
    return shard, directory


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", default=".", help="repository root (default: current directory)")
    parser.add_argument("--shard", action="append", type=parse_shard_argument, required=True, metavar="SHARD=DIR")
    parser.add_argument("--baseline", metavar="DIR", help="reports of the single full run of the same commit")
    parser.add_argument("--summary", metavar="FILE", help="append the Markdown summary to this file")
    parser.add_argument("--strict", action="store_true", help="exit 1 on failures, errors or completeness problems")
    args = parser.parse_args(argv)

    config = shards_module.load_config(args.repo)
    planned = shards_module.plan(shards_module.discover(args.repo), config["boundaries"])
    results = {shard: read_reports(directory) for shard, directory in args.shard}
    issues = completeness(results, planned)
    differences = []
    if args.baseline:
        shared = {shard: suites for shard, suites in results.items() if shard != shards_module.ISOLATED}
        differences = compare(merge_outcomes(shared), merge_outcomes({"baseline": read_reports(args.baseline)}))
    summary, problems = render(results, issues, differences, bool(args.baseline))
    print(summary)
    if args.summary:
        with open(args.summary, "a", encoding="utf-8") as handle:
            handle.write(summary)

    for issue in issues:
        print(f"::{'error' if args.strict else 'warning'}::{issue}")
    if differences:
        print(f"::warning::{len(differences)} tests had a different outcome in the shards than in the single full run")
    if args.strict and (issues or problems):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
