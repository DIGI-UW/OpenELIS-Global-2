#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_MATRIX = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/parity-matrix.csv",
);
const DEFAULT_OUTPUT_JSON = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/parity-report.json",
);
const DEFAULT_OUTPUT_MD = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/parity-report.md",
);

const INFRA_PATTERNS = [
  /err_connection_refused/i,
  /could not verify that this server is running/i,
  /baseurl/i,
  /econnrefused/i,
  /timed out waiting for/i,
];

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

function parseList(raw) {
  if (!raw) {
    return [];
  }
  return String(raw)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => path.resolve(item));
}

function normalizePath(value) {
  if (!value || typeof value !== "string") {
    return null;
  }
  const normalized = value.split(path.sep).join("/");
  if (normalized.startsWith("frontend/")) {
    return normalized.slice("frontend/".length);
  }
  return normalized.replace(/^\.\//, "");
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

function statusRank(status) {
  switch ((status || "").toLowerCase()) {
    case "failed":
      return 5;
    case "flaky":
      return 4;
    case "unknown":
      return 3;
    case "skipped":
      return 2;
    case "passed":
      return 1;
    default:
      return 0;
  }
}

function aggregateSpecStatus(normalizedRuns) {
  const testsBySpec = new Map();

  for (const run of normalizedRuns) {
    const tests = Array.isArray(run?.tests) ? run.tests : [];
    for (const test of tests) {
      const specPath = normalizePath(test.file);
      if (!specPath) {
        continue;
      }
      if (!testsBySpec.has(specPath)) {
        testsBySpec.set(specPath, []);
      }
      testsBySpec.get(specPath).push(test);
    }
  }

  const summaryBySpec = {};
  for (const [specPath, tests] of testsBySpec.entries()) {
    let resolvedStatus = "unknown";
    let maxRank = -1;
    const failureMessages = [];
    const stats = {
      total: tests.length,
      passed: 0,
      failed: 0,
      skipped: 0,
      flaky: 0,
      unknown: 0,
    };

    for (const test of tests) {
      const status = (test.status || "unknown").toLowerCase();
      if (Object.prototype.hasOwnProperty.call(stats, status)) {
        stats[status] += 1;
      } else {
        stats.unknown += 1;
      }

      const rank = statusRank(status);
      if (rank > maxRank) {
        maxRank = rank;
        resolvedStatus = status;
      }
      if (test.failureMessage) {
        failureMessages.push(String(test.failureMessage));
      }
    }

    summaryBySpec[specPath] = {
      status: resolvedStatus,
      stats,
      failureMessages,
    };
  }

  return summaryBySpec;
}

function parseCsvRows(csvText) {
  const lines = csvText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length <= 1) {
    return [];
  }

  const header = lines[0].split(",");
  return lines.slice(1).map((line) => {
    const columns = line.split(",");
    const row = {};
    header.forEach((key, index) => {
      row[key] = columns[index] || "";
    });
    return row;
  });
}

function classifyFailure(row) {
  const cypressStatus = row.cypressStatus;
  const playwrightStatus = row.playwrightStatus;
  const messages = [...row.cypressMessages, ...row.playwrightMessages];
  const hasInfraError = messages.some((message) =>
    INFRA_PATTERNS.some((pattern) => pattern.test(message)),
  );

  if (hasInfraError) {
    return "infrastructure/setup";
  }

  if (cypressStatus !== playwrightStatus) {
    return "parity_divergence";
  }

  if (cypressStatus === "failed" || playwrightStatus === "failed") {
    return "test_assertion";
  }

  return "none";
}

function compareFromMatrix(rows, cypressBySpec, playwrightBySpec) {
  const comparisons = [];
  for (const row of rows) {
    const legacySpec = normalizePath(row.legacy_spec);
    const playwrightSpec = normalizePath(row.playwright_target_spec);

    const cypressSummary = (legacySpec && cypressBySpec[legacySpec]) || null;
    const playwrightSummary =
      (playwrightSpec && playwrightBySpec[playwrightSpec]) || null;

    const comparison = {
      scenarioId: row.scenario_id,
      riskTier: row.risk_tier || "UNKNOWN",
      domain: row.domain || "unknown",
      legacySpec,
      playwrightSpec,
      parityStatusBaseline: row.parity_status || "UNKNOWN",
      cypressStatus: cypressSummary?.status || "missing",
      playwrightStatus: playwrightSummary?.status || "missing",
      cypressMessages: cypressSummary?.failureMessages || [],
      playwrightMessages: playwrightSummary?.failureMessages || [],
    };
    comparison.failureClass = classifyFailure(comparison);
    comparison.isDivergent =
      comparison.cypressStatus !== comparison.playwrightStatus ||
      comparison.cypressStatus === "missing" ||
      comparison.playwrightStatus === "missing";
    comparisons.push(comparison);
  }
  return comparisons;
}

function summarizeComparisons(comparisons) {
  const summary = {
    scenarios: comparisons.length,
    divergent: 0,
    missingInPlaywright: 0,
    missingInCypress: 0,
    failureClassCounts: {
      "infrastructure/setup": 0,
      parity_divergence: 0,
      test_assertion: 0,
      none: 0,
    },
    byRiskTier: {},
  };

  for (const row of comparisons) {
    if (row.isDivergent) {
      summary.divergent += 1;
    }
    if (row.playwrightStatus === "missing") {
      summary.missingInPlaywright += 1;
    }
    if (row.cypressStatus === "missing") {
      summary.missingInCypress += 1;
    }

    if (!summary.failureClassCounts[row.failureClass]) {
      summary.failureClassCounts[row.failureClass] = 0;
    }
    summary.failureClassCounts[row.failureClass] += 1;

    if (!summary.byRiskTier[row.riskTier]) {
      summary.byRiskTier[row.riskTier] = {
        total: 0,
        divergent: 0,
      };
    }
    summary.byRiskTier[row.riskTier].total += 1;
    if (row.isDivergent) {
      summary.byRiskTier[row.riskTier].divergent += 1;
    }
  }

  return summary;
}

function readRuntimeMetrics(runtimePath) {
  if (!runtimePath) {
    return null;
  }
  return safeReadJson(path.resolve(runtimePath));
}

function renderMarkdown(summary, comparisons, runtimeMetrics) {
  const divergentRows = comparisons.filter((row) => row.isDivergent);
  const topRows = divergentRows.slice(0, 50);

  const lines = [];
  lines.push("# E2E Parity Report");
  lines.push("");
  lines.push(`Generated at: ${new Date().toISOString()}`);
  lines.push("");
  lines.push("## Summary");
  lines.push("");
  lines.push(`- Total mapped scenarios: **${summary.scenarios}**`);
  lines.push(`- Divergent scenarios: **${summary.divergent}**`);
  lines.push(`- Missing in Playwright: **${summary.missingInPlaywright}**`);
  lines.push(`- Missing in Cypress: **${summary.missingInCypress}**`);
  lines.push("");
  lines.push("### Failure Classification");
  lines.push("");
  lines.push(
    `- infrastructure/setup: ${summary.failureClassCounts["infrastructure/setup"] || 0}`,
  );
  lines.push(
    `- parity_divergence: ${summary.failureClassCounts.parity_divergence || 0}`,
  );
  lines.push(
    `- test_assertion: ${summary.failureClassCounts.test_assertion || 0}`,
  );
  lines.push(`- none: ${summary.failureClassCounts.none || 0}`);
  lines.push("");
  lines.push("### Risk-Tier Divergence");
  lines.push("");
  lines.push("| Risk Tier | Total | Divergent |");
  lines.push("| --- | ---: | ---: |");
  for (const [riskTier, values] of Object.entries(summary.byRiskTier)) {
    lines.push(`| ${riskTier} | ${values.total} | ${values.divergent} |`);
  }

  if (runtimeMetrics) {
    lines.push("");
    lines.push("### Runtime Metrics");
    lines.push("");
    lines.push("| Framework | Duration (ms) | Budget (ms) | Within Budget |");
    lines.push("| --- | ---: | ---: | --- |");
    for (const framework of Object.keys(runtimeMetrics.budgetStatus || {})) {
      const budget = runtimeMetrics.budgetStatus[framework];
      lines.push(
        `| ${framework} | ${budget.actualMs ?? "NA"} | ${budget.budgetMs ?? "NA"} | ${budget.withinBudget === null ? "N/A" : budget.withinBudget} |`,
      );
    }
  }

  lines.push("");
  lines.push("## Divergence Details (first 50)");
  lines.push("");
  lines.push(
    "| Scenario ID | Risk | Domain | Cypress | Playwright | Failure Class |",
  );
  lines.push("| --- | --- | --- | --- | --- | --- |");
  for (const row of topRows) {
    lines.push(
      `| ${row.scenarioId} | ${row.riskTier} | ${row.domain} | ${row.cypressStatus} | ${row.playwrightStatus} | ${row.failureClass} |`,
    );
  }

  if (divergentRows.length > topRows.length) {
    lines.push("");
    lines.push(
      `Additional divergent rows omitted: ${divergentRows.length - topRows.length}`,
    );
  }

  lines.push("");
  return `${lines.join("\n")}\n`;
}

function writeFile(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const cypressFiles = parseList(args.cypress);
  const playwrightFiles = parseList(args.playwright);
  const parityMatrixPath = args["parity-matrix"]
    ? path.resolve(args["parity-matrix"])
    : DEFAULT_MATRIX;
  const outputJsonPath = args["output-json"]
    ? path.resolve(args["output-json"])
    : DEFAULT_OUTPUT_JSON;
  const outputMdPath = args["output-md"]
    ? path.resolve(args["output-md"])
    : DEFAULT_OUTPUT_MD;

  if (!fs.existsSync(parityMatrixPath)) {
    throw new Error(`Parity matrix not found: ${parityMatrixPath}`);
  }

  const cypressRuns = cypressFiles
    .map((filePath) => safeReadJson(filePath))
    .filter((payload) => payload !== null);
  const playwrightRuns = playwrightFiles
    .map((filePath) => safeReadJson(filePath))
    .filter((payload) => payload !== null);

  const cypressBySpec = aggregateSpecStatus(cypressRuns);
  const playwrightBySpec = aggregateSpecStatus(playwrightRuns);

  const matrixRows = parseCsvRows(fs.readFileSync(parityMatrixPath, "utf8"));
  const comparisons = compareFromMatrix(matrixRows, cypressBySpec, playwrightBySpec);
  const summary = summarizeComparisons(comparisons);
  const runtimeMetrics = readRuntimeMetrics(args["runtime-metrics"]);

  const report = {
    generatedAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/compare-e2e-results.js",
    inputs: {
      parityMatrix: path.relative(REPO_ROOT, parityMatrixPath).split(path.sep).join("/"),
      cypress: cypressFiles.map((item) =>
        path.relative(REPO_ROOT, item).split(path.sep).join("/"),
      ),
      playwright: playwrightFiles.map((item) =>
        path.relative(REPO_ROOT, item).split(path.sep).join("/"),
      ),
      runtimeMetrics: args["runtime-metrics"] || null,
    },
    summary,
    scenarioComparisons: comparisons,
  };

  writeFile(outputJsonPath, `${JSON.stringify(report, null, 2)}\n`);
  writeFile(outputMdPath, renderMarkdown(summary, comparisons, runtimeMetrics));

  console.log(
    `Parity report written to ${path
      .relative(REPO_ROOT, outputMdPath)
      .split(path.sep)
      .join("/")}`,
  );
  console.log(
    `Summary: scenarios=${summary.scenarios}, divergent=${summary.divergent}, missingInPlaywright=${summary.missingInPlaywright}, missingInCypress=${summary.missingInCypress}`,
  );
}

module.exports = {
  compareFromMatrix,
  summarizeComparisons,
  aggregateSpecStatus,
};
