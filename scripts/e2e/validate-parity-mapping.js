#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_MATRIX = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/parity-matrix.csv",
);
const DEFAULT_INVENTORY = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/inventory.json",
);

const ALLOWED_RISK_TIERS = new Set(["P0", "P1", "P2"]);
const ALLOWED_PARITY_STATUS = new Set([
  "LEGACY_ONLY",
  "GAP",
  "PARTIAL",
  "PASS",
  "EXCEPTION_APPROVED",
]);

function toPosix(filePath) {
  return filePath.split(path.sep).join("/");
}

function resolveArg(args, key, fallback) {
  const index = args.findIndex((arg) => arg === key);
  if (index >= 0 && args[index + 1]) {
    return path.resolve(args[index + 1]);
  }
  return fallback;
}

function hasFlag(args, key) {
  return args.includes(key);
}

function readInventory(inventoryPath) {
  if (!fs.existsSync(inventoryPath)) {
    throw new Error(
      `Inventory file missing: ${toPosix(path.relative(REPO_ROOT, inventoryPath))}`,
    );
  }
  return JSON.parse(fs.readFileSync(inventoryPath, "utf8"));
}

function readCsv(matrixPath) {
  if (!fs.existsSync(matrixPath)) {
    throw new Error(
      `Parity matrix missing: ${toPosix(path.relative(REPO_ROOT, matrixPath))}`,
    );
  }
  const raw = fs.readFileSync(matrixPath, "utf8").trim();
  if (!raw) {
    throw new Error("Parity matrix is empty.");
  }

  const lines = raw.split(/\r?\n/);
  const header = lines[0].split(",");
  const rows = lines.slice(1).map((line, lineIndex) => {
    const values = line.split(",");
    const row = {};
    header.forEach((column, index) => {
      row[column] = (values[index] || "").trim();
    });
    row.__line = lineIndex + 2;
    return row;
  });

  return { header, rows };
}

function validateRequiredColumns(header) {
  const required = [
    "scenario_id",
    "risk_tier",
    "domain",
    "legacy_spec",
    "playwright_target_spec",
    "parity_status",
    "gap_class",
    "owner",
    "milestone_target",
    "reliability_id",
    "notes",
  ];
  const missing = required.filter((column) => !header.includes(column));
  return missing;
}

function appendValidationNotes(rows, noteToken) {
  for (const row of rows) {
    if (row.risk_tier !== "P0" && row.risk_tier !== "P1") {
      continue;
    }
    const existing = row.notes || "";
    if (existing.includes(noteToken)) {
      continue;
    }
    row.notes = existing ? `${existing}|${noteToken}` : noteToken;
  }
}

function writeCsv(matrixPath, header, rows) {
  const lines = [header.join(",")];
  for (const row of rows) {
    const line = header.map((column) => row[column] || "").join(",");
    lines.push(line);
  }
  fs.writeFileSync(matrixPath, `${lines.join("\n")}\n`, "utf8");
}

function validateMatrix(matrix, inventory) {
  const errors = [];
  const warnings = [];

  const requiredColumnsMissing = validateRequiredColumns(matrix.header);
  if (requiredColumnsMissing.length > 0) {
    errors.push(
      `Matrix missing required columns: ${requiredColumnsMissing.join(", ")}`,
    );
    return { ok: false, errors, warnings, summary: null };
  }

  const activeLegacySpecs = new Set(
    inventory.specs
      .filter((spec) => spec.framework === "cypress" && spec.activeSpec)
      .map((spec) => spec.specPath),
  );

  const matrixSpecs = matrix.rows.map((row) => row.legacy_spec);
  const matrixSpecSet = new Set(matrixSpecs);
  const missingActiveSpecs = [...activeLegacySpecs]
    .filter((specPath) => !matrixSpecSet.has(specPath))
    .sort((a, b) => a.localeCompare(b));

  if (missingActiveSpecs.length > 0) {
    errors.push(
      `Missing active Cypress specs in matrix (${missingActiveSpecs.length}): ${missingActiveSpecs.join("; ")}`,
    );
  }

  const duplicateSpecs = [...new Set(matrixSpecs)].filter(
    (spec) => matrixSpecs.filter((candidate) => candidate === spec).length > 1,
  );
  if (duplicateSpecs.length > 0) {
    errors.push(`Duplicate legacy_spec rows found: ${duplicateSpecs.join("; ")}`);
  }

  const scenarioIds = matrix.rows.map((row) => row.scenario_id);
  const duplicateScenarioIds = [...new Set(scenarioIds)].filter(
    (id) => scenarioIds.filter((candidate) => candidate === id).length > 1,
  );
  if (duplicateScenarioIds.length > 0) {
    errors.push(`Duplicate scenario_id rows found: ${duplicateScenarioIds.join("; ")}`);
  }

  const p0p1Rows = matrix.rows.filter(
    (row) => row.risk_tier === "P0" || row.risk_tier === "P1",
  );
  if (p0p1Rows.length === 0) {
    errors.push("No P0/P1 rows found in parity matrix.");
  }

  for (const row of matrix.rows) {
    if (!ALLOWED_RISK_TIERS.has(row.risk_tier)) {
      errors.push(
        `Invalid risk_tier '${row.risk_tier}' on line ${row.__line} (${row.legacy_spec})`,
      );
    }
    if (!ALLOWED_PARITY_STATUS.has(row.parity_status)) {
      errors.push(
        `Invalid parity_status '${row.parity_status}' on line ${row.__line} (${row.legacy_spec})`,
      );
    }
  }

  const requiredForP0P1 = [
    "scenario_id",
    "risk_tier",
    "legacy_spec",
    "playwright_target_spec",
    "parity_status",
    "owner",
    "milestone_target",
  ];
  for (const row of p0p1Rows) {
    const missingFields = requiredForP0P1.filter((field) => !row[field]);
    if (missingFields.length > 0) {
      errors.push(
        `Missing required fields on line ${row.__line} (${row.legacy_spec}): ${missingFields.join(", ")}`,
      );
    }
  }

  const extraSpecs = matrix.rows
    .map((row) => row.legacy_spec)
    .filter((specPath) => !activeLegacySpecs.has(specPath));
  if (extraSpecs.length > 0) {
    warnings.push(
      `Matrix contains ${extraSpecs.length} non-active legacy specs (allowed): ${extraSpecs.join("; ")}`,
    );
  }

  const summary = {
    activeLegacySpecCount: activeLegacySpecs.size,
    matrixRowCount: matrix.rows.length,
    p0p1RowCount: p0p1Rows.length,
    missingActiveSpecCount: missingActiveSpecs.length,
    warningCount: warnings.length,
  };

  return {
    ok: errors.length === 0,
    errors,
    warnings,
    summary,
  };
}

function run() {
  const args = process.argv.slice(2);
  const matrixPath = resolveArg(args, "--matrix", DEFAULT_MATRIX);
  const inventoryPath = resolveArg(args, "--inventory", DEFAULT_INVENTORY);
  const writeNotes = hasFlag(args, "--write-notes");
  const noteDate = new Date().toISOString().slice(0, 10);
  const noteToken = `T026_PASS_${noteDate}`;

  const inventory = readInventory(inventoryPath);
  const matrix = readCsv(matrixPath);
  const result = validateMatrix(matrix, inventory);

  if (!result.ok) {
    console.error("FAIL: parity mapping validation failed.");
    for (const error of result.errors) {
      console.error(`- ${error}`);
    }
    for (const warning of result.warnings) {
      console.error(`WARN: ${warning}`);
    }
    process.exit(1);
  }

  if (writeNotes) {
    appendValidationNotes(matrix.rows, noteToken);
    writeCsv(matrixPath, matrix.header, matrix.rows);
    console.log(
      `Updated notes for P0/P1 rows with token '${noteToken}' in ${toPosix(
        path.relative(REPO_ROOT, matrixPath),
      )}`,
    );
  }

  console.log("PASS: parity mapping validation succeeded.");
  console.log(
    `Summary: activeLegacySpecs=${result.summary.activeLegacySpecCount}, matrixRows=${result.summary.matrixRowCount}, p0p1Rows=${result.summary.p0p1RowCount}`,
  );
  for (const warning of result.warnings) {
    console.log(`WARN: ${warning}`);
  }
}

if (require.main === module) {
  try {
    run();
  } catch (error) {
    console.error(`ERROR: ${error.message}`);
    process.exit(1);
  }
}
