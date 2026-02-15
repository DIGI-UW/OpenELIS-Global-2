#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_INPUT = path.join(REPO_ROOT, "frontend/playwright-report/results.json");
const DEFAULT_OUTPUT = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/playwright-results.normalized.json",
);

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const value = argv[i];
    if (!value.startsWith("--")) {
      continue;
    }
    const key = value.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      args[key] = true;
      continue;
    }
    args[key] = next;
    i += 1;
  }
  return args;
}

function readJson(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Playwright results input not found: ${filePath}`);
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function getAnnotation(annotations, key) {
  if (!Array.isArray(annotations)) {
    return null;
  }
  const entry = annotations.find((annotation) => annotation?.type === key);
  return entry?.description || null;
}

function deriveStatus(test) {
  const outcome = (test?.outcome || "").toLowerCase();
  if (outcome === "expected") {
    return "passed";
  }
  if (outcome === "skipped") {
    return "skipped";
  }
  if (outcome === "flaky") {
    return "flaky";
  }
  if (outcome === "unexpected") {
    return "failed";
  }

  const results = Array.isArray(test?.results) ? test.results : [];
  const statuses = results
    .map((result) => (result?.status || "").toLowerCase())
    .filter(Boolean);
  if (statuses.includes("failed") || statuses.includes("timedout")) {
    return "failed";
  }
  if (statuses.includes("skipped")) {
    return "skipped";
  }
  if (statuses.includes("passed")) {
    return "passed";
  }
  return "unknown";
}

function collectErrorMessage(test) {
  const results = Array.isArray(test?.results) ? test.results : [];
  for (const result of results) {
    const message =
      result?.error?.message ||
      result?.error?.stack ||
      result?.errors?.[0]?.message ||
      null;
    if (message) {
      return String(message);
    }
  }
  return null;
}

function collectDurationMs(test) {
  const results = Array.isArray(test?.results) ? test.results : [];
  return results.reduce(
    (sum, result) => sum + (Number.isFinite(result?.duration) ? result.duration : 0),
    0,
  );
}

function flattenSuites(suites, parentTitles = [], fallbackFile = null, rows = []) {
  if (!Array.isArray(suites)) {
    return rows;
  }

  for (const suite of suites) {
    const suiteTitle = suite?.title || "";
    const nextTitles = suiteTitle ? [...parentTitles, suiteTitle] : [...parentTitles];
    const suiteFile = suite?.file || fallbackFile;

    if (Array.isArray(suite?.specs)) {
      for (const spec of suite.specs) {
        const specTitle = spec?.title || "untitled";
        const titlePath = [...nextTitles, specTitle].filter(Boolean);
        const tests = Array.isArray(spec?.tests) ? spec.tests : [];

        for (const test of tests) {
          const legacyScenarioId = getAnnotation(test.annotations, "legacy-scenario");
          const row = {
            id: legacyScenarioId || `${suiteFile || "unknown-file"}::${titlePath.join(" › ")}`,
            framework: "playwright",
            file: suiteFile || null,
            projectName: test?.projectName || null,
            title: specTitle,
            titlePath,
            status: deriveStatus(test),
            durationMs: collectDurationMs(test),
            legacyScenarioId,
            riskTier: getAnnotation(test.annotations, "risk-tier"),
            domain: getAnnotation(test.annotations, "domain"),
            failureMessage: collectErrorMessage(test),
          };
          rows.push(row);
        }
      }
    }

    if (Array.isArray(suite?.suites)) {
      flattenSuites(suite.suites, nextTitles, suiteFile, rows);
    }
  }

  return rows;
}

function summarizeTests(tests) {
  const summary = {
    total: tests.length,
    passed: 0,
    failed: 0,
    skipped: 0,
    flaky: 0,
    unknown: 0,
    durationMs: 0,
  };

  for (const test of tests) {
    if (Object.prototype.hasOwnProperty.call(summary, test.status)) {
      summary[test.status] += 1;
    } else {
      summary.unknown += 1;
    }
    summary.durationMs += Number.isFinite(test.durationMs) ? test.durationMs : 0;
  }

  return summary;
}

function normalizeResults(raw, metadata) {
  const tests = flattenSuites(raw?.suites || []);
  return {
    generatedAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/export-playwright-results.js",
    framework: "playwright",
    source: path.relative(REPO_ROOT, metadata.inputPath).split(path.sep).join("/"),
    shard: metadata.shard || null,
    run: {
      workflow: process.env.GITHUB_WORKFLOW || null,
      runId: process.env.GITHUB_RUN_ID || null,
      runAttempt: process.env.GITHUB_RUN_ATTEMPT || null,
      sha: process.env.GITHUB_SHA || null,
    },
    summary: summarizeTests(tests),
    tests,
  };
}

function writeOutput(outputPath, payload) {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const inputPath = args.input ? path.resolve(args.input) : DEFAULT_INPUT;
  const outputPath = args.output ? path.resolve(args.output) : DEFAULT_OUTPUT;
  const shard = args.shard || null;

  const raw = readJson(inputPath);
  const normalized = normalizeResults(raw, { inputPath, shard });
  writeOutput(outputPath, normalized);

  console.log(
    `Playwright normalized results written to ${path
      .relative(REPO_ROOT, outputPath)
      .split(path.sep)
      .join("/")}`,
  );
  console.log(
    `Summary: total=${normalized.summary.total}, passed=${normalized.summary.passed}, failed=${normalized.summary.failed}, skipped=${normalized.summary.skipped}, flaky=${normalized.summary.flaky}`,
  );
}

module.exports = {
  normalizeResults,
  flattenSuites,
  summarizeTests,
};
