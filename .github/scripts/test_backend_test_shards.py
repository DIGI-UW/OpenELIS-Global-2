import importlib.util
import io
import json
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

spec = importlib.util.spec_from_file_location("backend_test_shards", Path(__file__).with_name("backend_test_shards.py"))
shards = importlib.util.module_from_spec(spec)
spec.loader.exec_module(shards)

report_spec = importlib.util.spec_from_file_location("backend_test_report", Path(__file__).with_name("backend_test_report.py"))
report = importlib.util.module_from_spec(report_spec)
report_spec.loader.exec_module(report)

SHARED_TEST = "package {package};\n\npublic class {name} {{\n    @Test\n    public void works() {{}}\n}}\n"
ISOLATED_TEST = (
    "package {package};\n\nimport org.openelisglobal.IsolatedDatabase;\n\n@IsolatedDatabase\npublic class {name} {{\n"
    "    @Test\n    public void migrates() {{\n        new PostgreSQLContainer<>(\"postgres:14.4\");\n    }}\n}}\n"
)


class Repository:
    def __init__(self, root, boundaries=("org.openelisglobal.m",), mode="shadow"):
        self.root = Path(root)
        self.write_config(boundaries=list(boundaries), mode=mode)

    def write_config(self, **config):
        path = self.root / shards.CONFIG
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({"isolatedForks": 2, **config}), encoding="utf-8")

    def add(self, name, template=SHARED_TEST, text=None):
        package, _, simple = name.rpartition(".")
        path = self.root / shards.TEST_ROOT / Path(*name.split(".")).with_suffix(".java")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text or template.format(package=package, name=simple), encoding="utf-8")
        return path


def surefire_report(directory, suite, cases):
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    body = []
    for name, outcome, message in cases:
        inner = "" if outcome == "passed" else f'<{outcome} message="{message}"/>'
        body.append(f'<testcase name="{name}" classname="{suite}" time="0.1">{inner}</testcase>')
    (directory / f"TEST-{suite}.xml").write_text(
        f'<testsuite name="{suite}" time="{len(cases)}" tests="{len(cases)}">{"".join(body)}</testsuite>', encoding="utf-8"
    )


class BackendTestShardsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.repository = Repository(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def run_cli(self, *args):
        output, errors = io.StringIO(), io.StringIO()
        with redirect_stdout(output), redirect_stderr(errors):
            code = shards.main(["--repo", self.directory.name, *args])
        return code, output.getvalue(), errors.getvalue()

    def test_discovers_only_surefire_default_includes_in_alphabetical_order(self):
        for name in ("org.openelisglobal.b.BTest", "org.openelisglobal.a.TestA", "org.openelisglobal.a.ATests",
                     "org.openelisglobal.a.ACase", "org.openelisglobal.a.AHelper", "org.openelisglobal.c.CTestCase"):
            self.repository.add(name)

        names = [test_class.name for test_class in shards.discover(self.directory.name)]

        self.assertEqual(names, ["org.openelisglobal.a.ATests", "org.openelisglobal.a.TestA",
                                 "org.openelisglobal.b.BTest", "org.openelisglobal.c.CTestCase"])

    def test_abstract_classes_and_classes_without_tests_are_not_runnable(self):
        self.repository.add("org.openelisglobal.a.BaseTest", text="package a;\npublic abstract class BaseTest {\n  @Test public void x() {}\n}\n")
        self.repository.add("org.openelisglobal.a.EmptyTest", text="package a;\npublic class EmptyTest {}\n")
        self.repository.add("org.openelisglobal.a.RealTest")

        runnable = {test_class.name: test_class.runnable for test_class in shards.discover(self.directory.name)}

        self.assertEqual(runnable, {"org.openelisglobal.a.BaseTest": False, "org.openelisglobal.a.EmptyTest": False,
                                    "org.openelisglobal.a.RealTest": True})

    def test_contiguous_ranges_keep_every_shared_class_exactly_once_and_isolated_apart(self):
        names = ["org.openelisglobal.a.ATest", "org.openelisglobal.l.LTest", "org.openelisglobal.m.MTest",
                 "org.openelisglobal.z.ZTest"]
        for name in names:
            self.repository.add(name)
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST)

        planned = shards.plan(shards.discover(self.directory.name), ["org.openelisglobal.m"])

        self.assertEqual({shard: [c.name for c in members] for shard, members in planned.items()}, {
            "1": ["org.openelisglobal.a.ATest", "org.openelisglobal.l.LTest"],
            "2": ["org.openelisglobal.m.MTest", "org.openelisglobal.z.ZTest"],
            "isolated": ["org.openelisglobal.b.MigrationTest"],
        })

    def test_a_class_named_exactly_like_a_boundary_starts_the_next_shard(self):
        self.repository.add("org.openelisglobal.a.ATest")
        self.repository.add("org.openelisglobal.m.MTest")

        planned = shards.plan(shards.discover(self.directory.name), ["org.openelisglobal.m.MTest"])

        self.assertEqual([c.name for c in planned["2"]], ["org.openelisglobal.m.MTest"])

    def test_check_fails_when_a_class_starts_a_container_without_the_marker(self):
        self.repository.add("org.openelisglobal.a.ATest")
        self.repository.add("org.openelisglobal.z.ZTest")
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST.replace("@IsolatedDatabase\n", ""))

        code, _, errors = self.run_cli("check")

        self.assertEqual(code, 1)
        self.assertIn("org/openelisglobal/b/MigrationTest.java starts its own container", errors)

    def test_check_fails_when_a_shard_would_run_nothing(self):
        self.repository.add("org.openelisglobal.z.ZTest")
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST)

        code, _, errors = self.run_cli("check")

        self.assertEqual(code, 1)
        self.assertIn("shard 1 has no runnable test class", errors)

    def test_includes_and_excludes_files_list_source_paths_for_surefire(self):
        self.repository.add("org.openelisglobal.a.ATest")
        self.repository.add("org.openelisglobal.z.ZTest")
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST)
        output = Path(self.directory.name) / "target"

        self.assertEqual(self.run_cli("includes", "--shard", "2", "--output", str(output / "two.txt"))[0], 0)
        self.assertEqual(self.run_cli("excludes-isolated", "--output", str(output / "isolated.txt"))[0], 0)

        self.assertEqual((output / "two.txt").read_text(), "org/openelisglobal/z/ZTest.java\n")
        self.assertEqual((output / "isolated.txt").read_text(), "org/openelisglobal/b/MigrationTest.java\n")

    def test_unknown_shard_is_refused(self):
        self.repository.add("org.openelisglobal.a.ATest")
        self.repository.add("org.openelisglobal.z.ZTest")
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST)

        code, _, errors = self.run_cli("includes", "--shard", "9", "--output", "unused.txt")

        self.assertEqual(code, 2)
        self.assertIn("unknown shard 9", errors)

    def test_config_values_and_validation(self):
        self.assertEqual(self.run_cli("matrix")[1].strip(), '["1", "2"]')
        self.assertEqual(self.run_cli("mode")[1].strip(), "shadow")
        self.assertEqual(self.run_cli("forks")[1].strip(), "2")
        for config, message in (
            ({"mode": "fast", "boundaries": ["a"]}, "mode must be one of"),
            ({"mode": "shadow", "boundaries": ["b", "a"]}, "sorted and unique"),
            ({"mode": "shadow", "boundaries": []}, "non-empty list"),
            ({"mode": "shadow", "boundaries": ["a"], "isolatedForks": 0}, "positive integer"),
        ):
            self.repository.write_config(**config)
            with self.assertRaisesRegex(ValueError, message):
                shards.load_config(self.directory.name)

    def test_repository_markers_and_boundaries_are_valid(self):
        repository = Path(__file__).resolve().parents[2]
        if not (repository / shards.TEST_ROOT).is_dir():
            self.skipTest("backend sources are not checked out")

        code = shards.main(["--repo", str(repository), "check"])

        self.assertEqual(code, 0)


class BackendTestReportTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.repository = Repository(self.root)
        self.repository.add("org.openelisglobal.a.ATest")
        self.repository.add("org.openelisglobal.z.ZTest")
        self.repository.add("org.openelisglobal.b.MigrationTest", template=ISOLATED_TEST)

    def tearDown(self):
        self.directory.cleanup()

    def run_report(self, *args):
        output = io.StringIO()
        with redirect_stdout(output):
            code = report.main(["--repo", self.directory.name, *args])
        return code, output.getvalue()

    def shard_reports(self, shard_one_cases=None):
        surefire_report(self.root / "r1", "org.openelisglobal.a.ATest", shard_one_cases or [("works", "passed", "")])
        surefire_report(self.root / "r2", "org.openelisglobal.z.ZTest", [("works", "passed", "")])
        surefire_report(self.root / "ri", "org.openelisglobal.b.MigrationTest", [("migrates", "passed", "")])
        return ["--shard", f"1={self.root / 'r1'}", "--shard", f"2={self.root / 'r2'}", "--shard", f"isolated={self.root / 'ri'}"]

    def test_complete_green_shards_pass_strict_mode(self):
        code, output = self.run_report("--strict", *self.shard_reports())

        self.assertEqual(code, 0)
        self.assertIn("### Completeness problems (0)", output)

    def test_a_planned_class_that_did_not_run_fails_strict_mode(self):
        arguments = self.shard_reports()
        (self.root / "r2" / "TEST-org.openelisglobal.z.ZTest.xml").unlink()
        surefire_report(self.root / "r2", "org.openelisglobal.y.Other", [("works", "passed", "")])

        code, output = self.run_report("--strict", *arguments)

        self.assertEqual(code, 1)
        self.assertIn("shard 2 did not run 1 planned classes: org.openelisglobal.z.ZTest", output)

    def test_a_shard_without_reports_and_a_class_run_twice_are_reported(self):
        arguments = self.shard_reports()
        surefire_report(self.root / "r2", "org.openelisglobal.a.ATest", [("works", "passed", "")])
        empty = self.root / "empty"
        empty.mkdir()

        code, output = self.run_report("--strict", *arguments, "--shard", f"3={empty}")

        self.assertEqual(code, 1)
        self.assertIn("org.openelisglobal.a.ATest ran in shard 1 and shard 2", output)
        self.assertIn("shard 3 produced no test reports", output)

    def test_failures_fail_strict_mode_and_skipped_tests_keep_their_reason(self):
        arguments = self.shard_reports([("works", "failure", "expected 2 but was 3"), ("later", "skipped", "OGC-1 flaky")])

        code, output = self.run_report("--strict", *arguments)

        self.assertEqual(code, 1)
        self.assertIn("`org.openelisglobal.a.ATest#works` (failure, shard 1): expected 2 but was 3", output)
        self.assertIn("`org.openelisglobal.a.ATest#later` (shard 1): OGC-1 flaky", output)

    def test_without_strict_problems_are_warnings_only(self):
        arguments = self.shard_reports([("works", "error", "boom")])

        code, _ = self.run_report(*arguments)

        self.assertEqual(code, 0)

    def test_baseline_comparison_lists_tests_whose_outcome_differs(self):
        arguments = self.shard_reports([("works", "error", "boom")])
        surefire_report(self.root / "base", "org.openelisglobal.a.ATest", [("works", "passed", "")])
        surefire_report(self.root / "base", "org.openelisglobal.z.ZTest", [("works", "passed", "")])
        summary = self.root / "summary.md"

        code, output = self.run_report(*arguments, "--baseline", str(self.root / "base"), "--summary", str(summary))

        self.assertEqual(code, 0)
        self.assertIn("### Differences from the single full run (1)", output)
        self.assertIn("`org.openelisglobal.a.ATest#works`: single run passed, shards error", output)
        self.assertNotIn("MigrationTest#migrates", output.split("### Differences")[1])
        self.assertIn("## Backend test shards", summary.read_text())


if __name__ == "__main__":
    unittest.main()
