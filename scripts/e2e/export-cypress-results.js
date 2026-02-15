#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_INPUT = path.join(
  REPO_ROOT,
  "frontend/cypress/results/cypress-raw.json",
);
const DEFAULT_OUTPUT = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/cypress-results.normalized.json",
);

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const current = argv[i];
    if (!current.startsWith("--")) {
      continue;
    }
    const key = current.slice(2);
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

function extractJsonPayload(rawText) {
  const trimmed = rawText.trim();
  if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
    return trimmed;
  }

  const markers = ['{"stats"', '{\n  "stats"'];
  let start = -1;
  for (const marker of markers) {
    start = rawText.indexOf(marker);
    if (start >= 0) {
      break;
    }
  }

  if (start < 0) {
    throw new Error("Unable to locate Cypress JSON payload in reporter output.");
  }

  const end = rawText.lastIndexOf("}");
  if (end < start) {
    throw new Error("Malformed Cypress reporter output: closing brace not found.");
  }

  return rawText.slice(start, end + 1);
}

function readCypressReporterOutput(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Cypress raw reporter output not found: ${filePath}`);
  }

  const rawText = fs.readFileSync(filePath, "utf8");
  const jsonPayload = extractJsonPayload(rawText);
  return JSON.parse(jsonPayload);
}

function normalizeSpecPath(value) {
  if (!value || typeof value !== "string") {
    return null;
  }

  const normalized = value.split(path.sep).join("/");
  const marker = "/frontend/";
  const markerIndex = normalized.indexOf(marker);
  if (markerIndex >= 0) {
    return normalized.slice(markerIndex + marker.length);
  }
  return normalized;
}

function pushTests(target, tests, status) {
  if (!Array.isArray(tests)) {
    return;
  }

  for (const test of tests) {
    const file = normalizeSpecPath(test.file);
    const title = test.fullTitle || test.title || "untitled";
    target.push({
      id: `${file || "unknown-file"}::${title}`,
      framework: "cypress",
      file,
      title,
      status,
      durationMs: Number.isFinite(test.duration) ? test.duration : 0,
      legacyScenarioId: null,
      riskTier: null,
      domain: null,
      failureMessage: status === "failed" ? test.err?.message || null : null,
    });
  }
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
  const tests = [];
  pushTests(tests, raw.passes, "passed");
  pushTests(tests, raw.failures, "failed");
  pushTests(tests, raw.pending, "skipped");

  return {
    generatedAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/export-cypress-results.js",
    framework: "cypress",
    source: path.relative(REPO_ROOT, metadata.inputPath).split(path.sep).join("/"),
    shard: metadata.shard || null,
    run: {
      workflow: process.env.GITHUB_WORKFLOW || null,
      runId: process.env.GITHUB_RUN_ID || null,
      runAttempt: process.env.GITHUB_RUN_ATTEMPT || null,
      sha: process.env.GITHUB_SHA || null,
    },
    cypressStats: raw.stats || null,
    summary: summarizeTests(tests),
    tests,
  };
}

function writeOutput(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const inputPath = args.input ? path.resolve(args.input) : DEFAULT_INPUT;
  const outputPath = args.output ? path.resolve(args.output) : DEFAULT_OUTPUT;
  const shard = args.shard || null;

  const raw = readCypressReporterOutput(inputPath);
  const normalized = normalizeResults(raw, { inputPath, shard });
  writeOutput(outputPath, normalized);

  console.log(
    `Cypress normalized results written to ${path
      .relative(REPO_ROOT, outputPath)
      .split(path.sep)
      .join("/")}`,
  );
  console.log(
    `Summary: total=${normalized.summary.total}, passed=${normalized.summary.passed}, failed=${normalized.summary.failed}, skipped=${normalized.summary.skipped}`,
  );
}

module.exports = {
  normalizeResults,
  readCypressReporterOutput,
  extractJsonPayload,
};
