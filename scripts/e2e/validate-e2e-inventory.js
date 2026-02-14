#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const {
  REPO_ROOT,
  DEFAULT_OUTPUT,
  collectInventory,
} = require("./export-e2e-inventory");

function toPosix(filePath) {
  return filePath.split(path.sep).join("/");
}

function resolveInventoryPath(args) {
  const inventoryArgIndex = args.findIndex((arg) => arg === "--inventory");
  if (inventoryArgIndex >= 0 && args[inventoryArgIndex + 1]) {
    return path.resolve(args[inventoryArgIndex + 1]);
  }
  return DEFAULT_OUTPUT;
}

function loadInventory(inventoryPath) {
  if (!fs.existsSync(inventoryPath)) {
    throw new Error(
      `Inventory file not found: ${toPosix(path.relative(REPO_ROOT, inventoryPath))}`,
    );
  }
  const raw = fs.readFileSync(inventoryPath, "utf8");
  return JSON.parse(raw);
}

function listDuplicates(items) {
  const counts = new Map();
  for (const item of items) {
    counts.set(item, (counts.get(item) || 0) + 1);
  }
  return [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([item, count]) => ({ item, count }));
}

function validateInventory(inventoryPath) {
  const sourceInventory = collectInventory();
  const declaredInventory = loadInventory(inventoryPath);

  const sourceActiveSpecs = new Set(
    sourceInventory.specs.filter((spec) => spec.activeSpec).map((spec) => spec.specPath),
  );
  const declaredSpecs = declaredInventory.specs.map((spec) => spec.specPath);
  const declaredActiveSpecs = new Set(
    declaredInventory.specs
      .filter((spec) => spec.activeSpec)
      .map((spec) => spec.specPath),
  );

  const missingActiveSpecs = [...sourceActiveSpecs]
    .filter((specPath) => !declaredActiveSpecs.has(specPath))
    .sort((a, b) => a.localeCompare(b));

  const staleActiveSpecs = [...declaredActiveSpecs]
    .filter((specPath) => !sourceActiveSpecs.has(specPath))
    .sort((a, b) => a.localeCompare(b));

  const duplicateSpecs = listDuplicates(declaredSpecs);

  const ok =
    missingActiveSpecs.length === 0 &&
    staleActiveSpecs.length === 0 &&
    duplicateSpecs.length === 0;

  const result = {
    ok,
    inventoryPath: toPosix(path.relative(REPO_ROOT, inventoryPath)),
    sourceActiveSpecCount: sourceActiveSpecs.size,
    inventorySpecCount: declaredInventory.specs.length,
    inventoryActiveSpecCount: declaredActiveSpecs.size,
    missingActiveSpecs,
    staleActiveSpecs,
    duplicateSpecs,
  };

  return result;
}

function printResult(result) {
  if (result.ok) {
    console.log(
      `PASS: inventory covers all active specs (${result.sourceActiveSpecCount} active specs).`,
    );
    return;
  }

  console.error("FAIL: inventory validation failed.");
  if (result.missingActiveSpecs.length > 0) {
    console.error(
      `Missing active specs (${result.missingActiveSpecs.length}):\n  - ${result.missingActiveSpecs.join("\n  - ")}`,
    );
  }
  if (result.staleActiveSpecs.length > 0) {
    console.error(
      `Stale active specs in inventory (${result.staleActiveSpecs.length}):\n  - ${result.staleActiveSpecs.join("\n  - ")}`,
    );
  }
  if (result.duplicateSpecs.length > 0) {
    const formatted = result.duplicateSpecs
      .map((entry) => `${entry.item} (x${entry.count})`)
      .join("\n  - ");
    console.error(`Duplicate spec entries:\n  - ${formatted}`);
  }
}

if (require.main === module) {
  const inventoryPath = resolveInventoryPath(process.argv.slice(2));
  try {
    const result = validateInventory(inventoryPath);
    printResult(result);
    process.exit(result.ok ? 0 : 1);
  } catch (error) {
    console.error(`ERROR: ${error.message}`);
    process.exit(1);
  }
}

module.exports = {
  validateInventory,
};
