#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_OUTPUT = path.join(
  REPO_ROOT,
  "specs/201-e2e-playwright-risk-parity/artifacts/inventory.json",
);

function toPosix(filePath) {
  return filePath.split(path.sep).join("/");
}

function listFilesRecursive(dirPath, predicate) {
  if (!fs.existsSync(dirPath)) {
    return [];
  }

  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      files.push(...listFilesRecursive(fullPath, predicate));
      continue;
    }
    if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }
  return files;
}

function stripComments(content) {
  return content
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/^\s*\/\/.*$/gm, "");
}

function countMatches(content, regex) {
  const matches = content.match(regex);
  return matches ? matches.length : 0;
}

function classifyDomain(framework, specPath) {
  const normalized = specPath.toLowerCase();

  if (framework === "cypress") {
    if (normalized.includes("/admine2e/")) {
      return "admin";
    }
    if (normalized.includes("storage")) {
      return "storage";
    }
    if (normalized.includes("analyzer")) {
      return "analyzer";
    }
    if (
      normalized.includes("patient") ||
      normalized.includes("order") ||
      normalized.includes("result") ||
      normalized.includes("validation") ||
      normalized.includes("nonconform") ||
      normalized.includes("workplan") ||
      normalized.includes("report")
    ) {
      return "clinical";
    }
    return "core";
  }

  if (framework === "playwright") {
    if (normalized.includes("analyzer")) {
      return "analyzer";
    }
    if (
      normalized.includes("auth") ||
      normalized.includes("nav") ||
      normalized.includes("side")
    ) {
      return "auth-nav";
    }
    if (normalized.includes("dashboard")) {
      return "dashboard";
    }
    return "other";
  }

  return "unknown";
}

function cypressCounts(content) {
  const source = stripComments(content);
  const active = countMatches(source, /\b(?:it|specify)\s*\(/g);
  const skipped =
    countMatches(source, /\b(?:it|specify)\s*\.\s*skip\s*\(/g) +
    countMatches(source, /\bxit\s*\(/g);

  return {
    total: active + skipped,
    active,
    skipped,
    fixme: 0,
  };
}

function playwrightCounts(content) {
  const source = stripComments(content);
  const active = countMatches(source, /\btest\s*\(/g);
  const skipped = countMatches(source, /\btest\s*\.\s*skip\s*\(/g);
  const fixme = countMatches(source, /\btest\s*\.\s*fixme\s*\(/g);

  return {
    total: active + skipped + fixme,
    active,
    skipped,
    fixme,
  };
}

function collectFrameworkSpecs(framework, directory, predicate, counter) {
  const absDir = path.join(REPO_ROOT, directory);
  const files = listFilesRecursive(absDir, predicate);
  files.sort((a, b) => a.localeCompare(b));

  return files.map((absPath) => {
    const content = fs.readFileSync(absPath, "utf8");
    const testCounts = counter(content);
    const frontendRelative = toPosix(path.relative(path.join(REPO_ROOT, "frontend"), absPath));
    return {
      framework,
      specPath: frontendRelative,
      domain: classifyDomain(framework, frontendRelative),
      testCounts,
      activeSpec: testCounts.active > 0,
    };
  });
}

function summarizeSpecs(specs) {
  const frameworks = {};
  const domains = {};

  for (const spec of specs) {
    if (!frameworks[spec.framework]) {
      frameworks[spec.framework] = {
        specs: 0,
        activeSpecs: 0,
        testsTotal: 0,
        testsActive: 0,
        testsSkipped: 0,
        testsFixme: 0,
      };
    }
    const frameworkSummary = frameworks[spec.framework];
    frameworkSummary.specs += 1;
    frameworkSummary.activeSpecs += spec.activeSpec ? 1 : 0;
    frameworkSummary.testsTotal += spec.testCounts.total;
    frameworkSummary.testsActive += spec.testCounts.active;
    frameworkSummary.testsSkipped += spec.testCounts.skipped;
    frameworkSummary.testsFixme += spec.testCounts.fixme;

    if (!domains[spec.domain]) {
      domains[spec.domain] = {
        specs: 0,
        testsTotal: 0,
        testsActive: 0,
        testsSkipped: 0,
      };
    }
    const domainSummary = domains[spec.domain];
    domainSummary.specs += 1;
    domainSummary.testsTotal += spec.testCounts.total;
    domainSummary.testsActive += spec.testCounts.active;
    domainSummary.testsSkipped += spec.testCounts.skipped;
  }

  const totals = {
    specs: specs.length,
    activeSpecs: specs.filter((spec) => spec.activeSpec).length,
    testsTotal: specs.reduce((sum, spec) => sum + spec.testCounts.total, 0),
    testsActive: specs.reduce((sum, spec) => sum + spec.testCounts.active, 0),
    testsSkipped: specs.reduce((sum, spec) => sum + spec.testCounts.skipped, 0),
    testsFixme: specs.reduce((sum, spec) => sum + spec.testCounts.fixme, 0),
  };

  return {
    frameworks,
    domains,
    totals,
  };
}

function collectInventory() {
  const cypressSpecs = collectFrameworkSpecs(
    "cypress",
    "frontend/cypress/e2e",
    (filePath) => filePath.endsWith(".cy.js"),
    cypressCounts,
  );

  const playwrightSpecs = collectFrameworkSpecs(
    "playwright",
    "frontend/playwright/tests",
    (filePath) => filePath.endsWith(".ts"),
    playwrightCounts,
  );

  const specs = [...cypressSpecs, ...playwrightSpecs].sort((a, b) => {
    if (a.framework !== b.framework) {
      return a.framework.localeCompare(b.framework);
    }
    return a.specPath.localeCompare(b.specPath);
  });

  return {
    generatedAt: new Date().toISOString(),
    generatedBy: "scripts/e2e/export-e2e-inventory.js",
    sourceRoots: {
      cypress: "frontend/cypress/e2e/**/*.cy.js",
      playwright: "frontend/playwright/tests/**/*.ts",
    },
    summary: summarizeSpecs(specs),
    specs,
  };
}

function resolveOutputPath(args) {
  const outputArgIndex = args.findIndex((arg) => arg === "--output");
  if (outputArgIndex >= 0 && args[outputArgIndex + 1]) {
    return path.resolve(args[outputArgIndex + 1]);
  }
  return DEFAULT_OUTPUT;
}

function writeInventory(outputPath) {
  const inventory = collectInventory();
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(inventory, null, 2)}\n`, "utf8");

  console.log(`Wrote inventory to ${toPosix(path.relative(REPO_ROOT, outputPath))}`);
  console.log(
    `Framework summary: Cypress specs=${inventory.summary.frameworks.cypress?.specs || 0}, Playwright specs=${inventory.summary.frameworks.playwright?.specs || 0}`,
  );
  console.log(
    `Total tests: ${inventory.summary.totals.testsTotal} (active=${inventory.summary.totals.testsActive}, skipped=${inventory.summary.totals.testsSkipped}, fixme=${inventory.summary.totals.testsFixme})`,
  );
}

if (require.main === module) {
  const outputPath = resolveOutputPath(process.argv.slice(2));
  writeInventory(outputPath);
}

module.exports = {
  REPO_ROOT,
  DEFAULT_OUTPUT,
  collectInventory,
  writeInventory,
};
