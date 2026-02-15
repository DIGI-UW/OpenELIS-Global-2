#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_OUTPUT = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/runtime-metrics.json",
);

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) {
      continue;
    }
    const key = token.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      args[key] = true;
      continue;
    }
    if (args[key]) {
      args[key] = `${args[key]},${next}`;
    } else {
      args[key] = next;
    }
    i += 1;
  }
  return args;
}

function parseInputList(raw) {
  if (!raw) {
    return [];
  }
  return String(raw)
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => path.resolve(value));
}

function safeReadJson(filePath) {
  if (!fs.existsSync(filePath)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch (error) {
    return null;
  }
}

function aggregateByFramework(results) {
  const aggregated = {};
  for (const result of results) {
    if (!result || !result.framework) {
      continue;
    }

    if (!aggregated[result.framework]) {
      aggregated[result.framework] = {
        files: 0,
        totalTests: 0,
        passed: 0,
        failed: 0,
        skipped: 0,
        flaky: 0,
        durationMs: 0,
      };
    }

    const bucket = aggregated[result.framework];
    bucket.files += 1;
    bucket.totalTests += Number(result.summary?.total || 0);
    bucket.passed += Number(result.summary?.passed || 0);
    bucket.failed += Number(result.summary?.failed || 0);
    bucket.skipped += Number(result.summary?.skipped || 0);
    bucket.flaky += Number(result.summary?.flaky || 0);
    bucket.durationMs += Number(result.summary?.durationMs || 0);
  }
  return aggregated;
}

function buildMetricsPayload(inputs, results, budgets) {
  const byFramework = aggregateByFramework(results);

  const budgetStatus = {};
  for (const framework of Object.keys(byFramework)) {
    const actual = byFramework[framework].durationMs;
    const budget = budgets[framework] || null;
    budgetStatus[framework] = {
      budgetMs: budget,
      actualMs: actual,
      withinBudget: Number.isFinite(budget) ? actual <= budget : null,
    };
  }

  return {
    generatedAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/export-runtime-metrics.js",
    inputs: inputs.map((filePath) =>
      path.relative(REPO_ROOT, filePath).split(path.sep).join("/"),
    ),
    frameworks: byFramework,
    budgetStatus,
  };
}

function writeOutput(outputPath, payload) {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const inputs = parseInputList(args.input);
  if (inputs.length === 0) {
    throw new Error(
      "At least one --input file is required (comma-separated values supported).",
    );
  }

  const results = inputs
    .map((filePath) => safeReadJson(filePath))
    .filter((payload) => payload !== null);

  if (results.length === 0) {
    throw new Error("No readable normalized result inputs were found.");
  }

  const budgets = {
    playwright: args["budget-ms-playwright"]
      ? Number(args["budget-ms-playwright"])
      : null,
    cypress: args["budget-ms-cypress"] ? Number(args["budget-ms-cypress"]) : null,
  };
  const outputPath = args.output ? path.resolve(args.output) : DEFAULT_OUTPUT;
  const payload = buildMetricsPayload(inputs, results, budgets);
  writeOutput(outputPath, payload);

  console.log(
    `Runtime metrics written to ${path
      .relative(REPO_ROOT, outputPath)
      .split(path.sep)
      .join("/")}`,
  );
}

module.exports = {
  buildMetricsPayload,
  aggregateByFramework,
};
