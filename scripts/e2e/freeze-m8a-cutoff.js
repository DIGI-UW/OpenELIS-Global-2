#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_INVENTORY = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/inventory.json",
);
const DEFAULT_PARITY_MATRIX = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/parity-matrix.csv",
);
const DEFAULT_OUTPUT = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/cutoff-scope.json",
);

const BLOCKING_PARITY_STATES = new Set(["LEGACY_ONLY", "GAP", "PARTIAL"]);

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
    args[key] = next;
    i += 1;
  }
  return args;
}

function normalizePath(value) {
  if (!value || typeof value !== "string") {
    return null;
  }
  return value.replace(/\\/g, "/").replace(/^frontend\//, "");
}

function readJson(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`JSON file not found: ${filePath}`);
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function parseCsvRows(csvText) {
  const lines = csvText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length < 2) {
    return [];
  }

  const header = lines[0].split(",");
  return lines.slice(1).map((line) => {
    const columns = line.split(",");
    const row = {};
    for (let i = 0; i < header.length; i += 1) {
      row[header[i]] = columns[i] || "";
    }
    return row;
  });
}

function buildCutoffSnapshot({
  inventory,
  parityRows,
  headSha,
  cypressRunId,
  playwrightRunId,
}) {
  const activeCypressSpecs = (inventory.specs || [])
    .filter(
      (spec) => spec.framework === "cypress" && Number(spec?.testCounts?.active || 0) > 0,
    )
    .map((spec) => normalizePath(spec.specPath))
    .filter(Boolean);

  const activeSpecSet = new Set(activeCypressSpecs);
  const scopedRows = parityRows.filter((row) =>
    activeSpecSet.has(normalizePath(row.legacy_spec)),
  );

  const statusCounts = {};
  const riskCounts = {};
  const blockingRows = [];

  for (const row of scopedRows) {
    const status = row.parity_status || "UNKNOWN";
    statusCounts[status] = (statusCounts[status] || 0) + 1;

    const risk = row.risk_tier || "UNKNOWN";
    riskCounts[risk] = (riskCounts[risk] || 0) + 1;

    if (BLOCKING_PARITY_STATES.has(status)) {
      blockingRows.push({
        scenarioId: row.scenario_id,
        riskTier: risk,
        domain: row.domain || "unknown",
        parityStatus: status,
        legacySpec: normalizePath(row.legacy_spec),
        playwrightSpec: normalizePath(row.playwright_target_spec),
      });
    }
  }

  return {
    frozenAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/freeze-m8a-cutoff.js",
    cutoffPolicy:
      "Current non-skipped Cypress coverage scope based on active Cypress specs in inventory artifact.",
    commitSha: headSha || null,
    runReferences: {
      cypress: cypressRunId
        ? {
            runId: cypressRunId,
            url: `https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/${cypressRunId}`,
          }
        : null,
      playwright: playwrightRunId
        ? {
            runId: playwrightRunId,
            url: `https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/${playwrightRunId}`,
          }
        : null,
    },
    scope: {
      activeCypressSpecs,
      scenarioCount: scopedRows.length,
      scenarioIds: scopedRows.map((row) => row.scenario_id),
      statusCounts,
      riskCounts,
    },
    gateEvaluation: {
      pass: blockingRows.length === 0,
      blockingStatusSet: Array.from(BLOCKING_PARITY_STATES),
      blockingRows,
    },
  };
}

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const inventoryPath = args.inventory
    ? path.resolve(args.inventory)
    : DEFAULT_INVENTORY;
  const matrixPath = args["parity-matrix"]
    ? path.resolve(args["parity-matrix"])
    : DEFAULT_PARITY_MATRIX;
  const outputPath = args.output ? path.resolve(args.output) : DEFAULT_OUTPUT;

  const inventory = readJson(inventoryPath);
  const matrixRows = parseCsvRows(fs.readFileSync(matrixPath, "utf8"));
  const snapshot = buildCutoffSnapshot({
    inventory,
    parityRows: matrixRows,
    headSha: args["head-sha"] || process.env.GITHUB_SHA || null,
    cypressRunId: args["cypress-run-id"] || null,
    playwrightRunId: args["playwright-run-id"] || null,
  });
  writeJson(outputPath, snapshot);

  console.log(
    `Cutoff scope snapshot written to ${path
      .relative(REPO_ROOT, outputPath)
      .split(path.sep)
      .join("/")}`,
  );
  console.log(
    `Scoped scenarios=${snapshot.scope.scenarioCount}, blockingRows=${snapshot.gateEvaluation.blockingRows.length}, gatePass=${snapshot.gateEvaluation.pass}`,
  );
}

module.exports = {
  buildCutoffSnapshot,
  parseCsvRows,
};
