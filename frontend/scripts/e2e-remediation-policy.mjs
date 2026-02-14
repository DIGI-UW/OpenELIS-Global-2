#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, "..");
const POLICY_PATH = path.join(
  FRONTEND_ROOT,
  "cypress",
  "remediation-policy.json",
);

function parseArgs(argv) {
  const [command = "", ...rest] = argv;
  const options = {};

  for (let i = 0; i < rest.length; i += 1) {
    const token = rest[i];
    if (!token.startsWith("--")) {
      continue;
    }

    const [rawKey, inlineValue] = token.slice(2).split("=");
    const key = rawKey.trim();
    if (!key) {
      continue;
    }

    if (inlineValue !== undefined) {
      options[key] = inlineValue;
      continue;
    }

    const nextToken = rest[i + 1];
    if (nextToken && !nextToken.startsWith("--")) {
      options[key] = nextToken;
      i += 1;
      continue;
    }

    options[key] = "true";
  }

  return { command, options };
}

function normalizeBranchName(input) {
  if (!input) return "";
  return input.trim().replace(/^refs\/heads\//, "");
}

function resolveBranchName(optionBranch) {
  return normalizeBranchName(
    optionBranch || process.env.GITHUB_HEAD_REF || process.env.GITHUB_REF_NAME,
  );
}

function loadPolicy() {
  const raw = fs.readFileSync(POLICY_PATH, "utf8");
  return JSON.parse(raw);
}

function branchEntries(policy, branchName) {
  return policy.branchPolicies?.[branchName]?.entries || [];
}

function normalizeSpecList(specsArg) {
  if (!specsArg) return [];
  return specsArg
    .split(/[\n,]/)
    .map((spec) => spec.trim())
    .filter(Boolean);
}

function countMatches(content, regex) {
  const matches = content.match(regex);
  return matches ? matches.length : 0;
}

function countSpecTests(filePath, pattern) {
  if (!filePath || !fs.existsSync(filePath)) {
    return 0;
  }
  const content = fs.readFileSync(filePath, "utf8");
  return countMatches(content, pattern);
}

function runFilter(branchName, specsArg) {
  const policy = loadPolicy();
  const excluded = new Set(
    branchEntries(policy, branchName).map((entry) => entry.cypressSpec),
  );
  const filteredSpecs = normalizeSpecList(specsArg).filter(
    (spec) => !excluded.has(spec),
  );
  process.stdout.write(filteredSpecs.join(","));
}

function parityRow(entry) {
  const cypressPath = path.join(FRONTEND_ROOT, entry.cypressSpec);
  const playwrightPath = entry.playwrightSpec
    ? path.join(FRONTEND_ROOT, entry.playwrightSpec)
    : null;

  const cypressExists = fs.existsSync(cypressPath);
  const playwrightExists = playwrightPath
    ? fs.existsSync(playwrightPath)
    : false;

  const cypressTests = countSpecTests(
    cypressPath,
    /^\s*it(?:\.only|\.skip)?\s*\(/gm,
  );
  const playwrightTests = playwrightPath
    ? countSpecTests(playwrightPath, /^\s*test(?:\.only|\.fixme)?\s*\(/gm)
    : 0;

  const minimumPlaywrightTests = Number(entry.minimumPlaywrightTests || 0);
  const isMigrated = entry.status === "migrated_to_playwright";

  let parityOk = true;
  const failures = [];
  if (isMigrated) {
    if (!entry.playwrightSpec) {
      parityOk = false;
      failures.push("missing playwrightSpec mapping");
    }
    if (!playwrightExists) {
      parityOk = false;
      failures.push("playwright spec file missing");
    }
    if (playwrightTests < minimumPlaywrightTests) {
      parityOk = false;
      failures.push(
        `playwright tests ${playwrightTests} < minimum ${minimumPlaywrightTests}`,
      );
    }
  }

  const ratio =
    cypressExists && cypressTests > 0
      ? (playwrightTests / cypressTests).toFixed(2)
      : "n/a";

  return {
    ...entry,
    cypressExists,
    cypressTests,
    playwrightExists,
    playwrightTests,
    minimumPlaywrightTests,
    ratio,
    parityOk,
    failures,
  };
}

function runParity(branchName) {
  const policy = loadPolicy();
  const entries = branchEntries(policy, branchName);

  if (entries.length === 0) {
    console.log(
      `No E2E remediation policy entries for branch '${branchName || "unknown"}'.`,
    );
    return 0;
  }

  const rows = entries.map((entry) => parityRow(entry));
  const migratedRows = rows.filter(
    (row) => row.status === "migrated_to_playwright",
  );
  const migratedPass = migratedRows.filter((row) => row.parityOk).length;
  const migratedFail = migratedRows.length - migratedPass;
  const pendingMigration = rows.filter(
    (row) => row.status === "quarantined_pending_migration",
  ).length;

  console.log(`E2E remediation parity report for '${branchName}'`);
  console.log(
    "status | cypress spec | cypress it() | playwright spec | playwright test() | min | ratio | parity",
  );
  console.log(
    "------ | ------------ | ------------ | --------------- | ----------------- | --- | ----- | ------",
  );

  rows.forEach((row) => {
    const playwrightSpec = row.playwrightSpec || "-";
    const parityLabel = row.parityOk
      ? "ok"
      : row.failures.join("; ").replace(/\|/g, "/");
    console.log(
      `${row.status} | ${row.cypressSpec} | ${row.cypressTests} | ${playwrightSpec} | ${row.playwrightTests} | ${row.minimumPlaywrightTests} | ${row.ratio} | ${parityLabel}`,
    );
  });

  console.log("");
  console.log(
    `Summary: migrated=${migratedRows.length}, migrated_pass=${migratedPass}, migrated_fail=${migratedFail}, pending_migration=${pendingMigration}`,
  );

  if (migratedFail > 0) {
    return 1;
  }
  return 0;
}

function main() {
  const { command, options } = parseArgs(process.argv.slice(2));
  const branchName = resolveBranchName(options.branch);

  if (!command || !["filter", "parity"].includes(command)) {
    console.error(
      "Usage:\n  node scripts/e2e-remediation-policy.mjs filter --branch <name> --specs <csv>\n  node scripts/e2e-remediation-policy.mjs parity --branch <name>",
    );
    process.exit(2);
  }

  if (command === "filter") {
    runFilter(branchName, options.specs || "");
    return;
  }

  const exitCode = runParity(branchName);
  process.exit(exitCode);
}

main();
