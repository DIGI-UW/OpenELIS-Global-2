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

function findJsonBlockEnd(rawText, start) {
  let depth = 0;
  let inString = false;
  let escaping = false;

  for (let index = start; index < rawText.length; index += 1) {
    const char = rawText[index];

    if (inString) {
      if (escaping) {
        escaping = false;
        continue;
      }
      if (char === "\\") {
        escaping = true;
        continue;
      }
      if (char === '"') {
        inString = false;
      }
      continue;
    }

    if (char === '"') {
      inString = true;
      continue;
    }

    if (char === "{") {
      depth += 1;
      continue;
    }

    if (char === "}") {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }

  throw new Error("Malformed Cypress reporter output: closing brace not found.");
}

function extractJsonPayloads(rawText) {
  const payloads = [];
  const seenStarts = new Set();
  const startRegex = /\{\s*"stats"/g;
  let match;

  while ((match = startRegex.exec(rawText)) !== null) {
    const start = match.index;
    if (seenStarts.has(start)) {
      continue;
    }
    seenStarts.add(start);

    const end = findJsonBlockEnd(rawText, start);
    payloads.push(rawText.slice(start, end + 1));
    startRegex.lastIndex = end + 1;
  }

  return payloads;
}

function extractJsonPayload(rawText) {
  const payloads = extractJsonPayloads(rawText);
  if (payloads.length === 0) {
    throw new Error("Unable to locate Cypress JSON payload in reporter output.");
  }
  return payloads[0];
}

function parseSpecOrder(rawText) {
  const match = rawText.match(/Running tests in custom order:\s*\[([\s\S]*?)\]\s*/);
  if (!match?.[1]) {
    return [];
  }

  const specPaths = [];
  const seen = new Set();
  const regex = /'([^']+)'/g;
  let token;
  while ((token = regex.exec(match[1])) !== null) {
    const normalized = normalizeSpecPath(token[1]);
    if (!normalized || seen.has(normalized)) {
      continue;
    }
    seen.add(normalized);
    specPaths.push(normalized);
  }
  return specPaths;
}

function readCypressReporterOutput(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Cypress raw reporter output not found: ${filePath}`);
  }

  const rawText = fs.readFileSync(filePath, "utf8");
  const jsonPayloads = extractJsonPayloads(rawText);
  if (jsonPayloads.length === 0) {
    throw new Error("Unable to locate Cypress JSON payload in reporter output.");
  }

  const parsedPayloads = jsonPayloads.map((payload) => JSON.parse(payload));
  const aggregate = {
    stats: null,
    tests: [],
    pending: [],
    failures: [],
    passes: [],
  };

  for (const payload of parsedPayloads) {
    aggregate.tests.push(...(Array.isArray(payload?.tests) ? payload.tests : []));
    aggregate.pending.push(...(Array.isArray(payload?.pending) ? payload.pending : []));
    aggregate.failures.push(...(Array.isArray(payload?.failures) ? payload.failures : []));
    aggregate.passes.push(...(Array.isArray(payload?.passes) ? payload.passes : []));
  }

  const firstStats = parsedPayloads[0]?.stats || {};
  const lastStats = parsedPayloads[parsedPayloads.length - 1]?.stats || {};
  const duration = parsedPayloads.reduce(
    (sum, payload) => sum + (Number.isFinite(payload?.stats?.duration) ? payload.stats.duration : 0),
    0,
  );
  aggregate.stats = {
    suites: parsedPayloads.length,
    tests: aggregate.tests.length,
    passes: aggregate.passes.length,
    pending: aggregate.pending.length,
    failures: aggregate.failures.length,
    start: firstStats.start || null,
    end: lastStats.end || null,
    duration,
  };

  aggregate.__specOrder = parseSpecOrder(rawText);
  return aggregate;
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

function buildSpecCoverage(raw) {
  const specPaths = Array.isArray(raw?.__specOrder) ? raw.__specOrder : [];
  if (specPaths.length === 0) {
    return [];
  }

  const shardStatus = Number(raw?.stats?.failures || 0) > 0 ? "failed" : "passed";
  return specPaths.map((file) => ({
    file,
    status: shardStatus,
    failureMessage: null,
  }));
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
  if (raw.__parseError) {
    tests.push({
      id: "cypress-export::parse-error",
      framework: "cypress",
      file: null,
      title: "Unable to parse Cypress reporter output",
      status: "unknown",
      durationMs: 0,
      legacyScenarioId: null,
      riskTier: null,
      domain: null,
      failureMessage: raw.__parseError,
    });
  } else {
    pushTests(tests, raw.passes, "passed");
    pushTests(tests, raw.failures, "failed");
    pushTests(tests, raw.pending, "skipped");
  }

  const specCoverage = buildSpecCoverage(raw);

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
    parseError: raw.__parseError || null,
    specCoverage,
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

  let raw;
  try {
    raw = readCypressReporterOutput(inputPath);
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Unknown parse failure";
    console.warn(`WARNING: failed to parse Cypress reporter output: ${message}`);
    raw = {
      stats: null,
      passes: [],
      failures: [],
      pending: [],
      __parseError: message,
    };
  }
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
  extractJsonPayloads,
};
