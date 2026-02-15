#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_CUTOFF = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/cutoff-scope.json",
);
const DEFAULT_OUTPUT_MD = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/m8a-gate-check.md",
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
    args[key] = next;
    i += 1;
  }
  return args;
}

function readJson(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Cutoff snapshot not found: ${filePath}`);
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function renderMarkdown(snapshot) {
  const lines = [];
  lines.push("# M8a Parity Gate Check");
  lines.push("");
  lines.push(`Generated at: ${new Date().toISOString()}`);
  lines.push("");
  lines.push("## Scope");
  lines.push("");
  lines.push(`- Commit SHA: ${snapshot.commitSha || "N/A"}`);
  lines.push(
    `- Scoped non-skipped Cypress scenarios: **${snapshot.scope?.scenarioCount ?? 0}**`,
  );
  lines.push("");
  lines.push("## Gate Result");
  lines.push("");
  lines.push(`- Gate pass: **${snapshot.gateEvaluation?.pass ? "YES" : "NO"}**`);
  lines.push(
    `- Blocking rows: **${snapshot.gateEvaluation?.blockingRows?.length || 0}**`,
  );
  lines.push("");
  lines.push("## Blocking Rows (first 50)");
  lines.push("");
  lines.push("| Scenario ID | Risk | Domain | Status | Legacy Spec | Playwright Spec |");
  lines.push("| --- | --- | --- | --- | --- | --- |");
  const rows = snapshot.gateEvaluation?.blockingRows || [];
  for (const row of rows.slice(0, 50)) {
    lines.push(
      `| ${row.scenarioId} | ${row.riskTier} | ${row.domain} | ${row.parityStatus} | ${row.legacySpec} | ${row.playwrightSpec} |`,
    );
  }
  if (rows.length > 50) {
    lines.push("");
    lines.push(`Additional rows omitted: ${rows.length - 50}`);
  }
  lines.push("");
  return `${lines.join("\n")}\n`;
}

function writeText(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content, "utf8");
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  const cutoffPath = args.cutoff ? path.resolve(args.cutoff) : DEFAULT_CUTOFF;
  const outputPath = args.output ? path.resolve(args.output) : DEFAULT_OUTPUT_MD;

  const snapshot = readJson(cutoffPath);
  writeText(outputPath, renderMarkdown(snapshot));

  console.log(
    `M8a gate report written to ${path
      .relative(REPO_ROOT, outputPath)
      .split(path.sep)
      .join("/")}`,
  );

  if (!snapshot?.gateEvaluation?.pass) {
    console.error(
      `M8a gate failed: ${snapshot.gateEvaluation?.blockingRows?.length || 0} blocking rows remain.`,
    );
    process.exit(1);
  }
}

module.exports = {
  renderMarkdown,
};
