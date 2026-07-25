import fs from "fs";
import path from "path";

const projectRoot = process.cwd();
const configPath = path.resolve(projectRoot, "playwright.config.ts");
const testsRoot = path.resolve(projectRoot, "playwright/tests");
const harnessUiAuthPath = path.resolve(testsRoot, "harness-ui-auth.setup.ts");
const demoPresentationPath = path.resolve(
  projectRoot,
  "playwright/helpers/demo-presentation.ts",
);

const BUCKETS = [
  "CORE_DEMO_TESTS",
  "CORE_FOUNDATIONAL_TESTS",
  "HARNESS_DEMO_TESTS",
  "HARNESS_FOUNDATIONAL_TESTS",
  "HARNESS_MANUAL_ONLY_TESTS",
];

function normalizePattern(pattern) {
  return pattern.replace(/^\*\*\//, "");
}

function matchesPattern(specPath, rawPattern) {
  const pattern = normalizePattern(rawPattern);
  if (pattern.endsWith("/**/*.spec.ts")) {
    const prefix = pattern.replace("/**/*.spec.ts", "/");
    return specPath.startsWith(prefix) && specPath.endsWith(".spec.ts");
  }
  if (pattern.includes("*")) {
    const escaped = pattern.replace(/[.+^${}()|[\]\\]/g, "\\$&");
    const regexBody = escaped.replace(/\*\*/g, ".*").replace(/\*/g, "[^/]*");
    return new RegExp(`^${regexBody}$`).test(specPath);
  }
  return specPath === pattern;
}

function collectSpecFiles(dir, acc = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      collectSpecFiles(fullPath, acc);
    } else if (entry.isFile() && fullPath.endsWith(".spec.ts")) {
      acc.push(path.relative(testsRoot, fullPath).replaceAll(path.sep, "/"));
    }
  }
  return acc;
}

function extractPatterns(configContent, variableName) {
  const blockMatch = configContent.match(
    new RegExp(`const\\s+${variableName}\\s*=\\s*\\[(.*?)\\];`, "s"),
  );
  if (!blockMatch) {
    return [];
  }
  return [...blockMatch[1].matchAll(/"([^"]+)"/g)].map((m) =>
    m[1].replace(/^\*\*\//, ""),
  );
}

const configContent = fs.readFileSync(configPath, "utf8");
const bucketMatchers = Object.fromEntries(
  BUCKETS.map((bucket) => [bucket, extractPatterns(configContent, bucket)]),
);

const violations = [];
const allSpecs = collectSpecFiles(testsRoot);

for (const specPath of allSpecs) {
  const matchingBuckets = BUCKETS.filter((bucket) =>
    bucketMatchers[bucket].some((pattern) => matchesPattern(specPath, pattern)),
  );

  if (matchingBuckets.length === 0) {
    violations.push(`${specPath} -> unassigned (no bucket match)`);
  } else if (matchingBuckets.length > 1) {
    violations.push(
      `${specPath} -> ambiguous (matches multiple buckets: ${matchingBuckets.join(", ")})`,
    );
  }
}

const harnessUiAuth = fs.existsSync(harnessUiAuthPath)
  ? fs.readFileSync(harnessUiAuthPath, "utf8")
  : "";
const demoPresentation = fs.existsSync(demoPresentationPath)
  ? fs.readFileSync(demoPresentationPath, "utf8")
  : "";
const harnessUiDependencyCount = (
  configContent.match(/dependencies: \["harness-ui-setup"\]/g) || []
).length;
const harnessUiStorageCount = (
  configContent.match(
    /storageState: "playwright\/\.auth\/harness-ui-user\.json"/g,
  ) || []
).length;

if (!configContent.includes('name: "harness-ui-setup"')) {
  violations.push(
    "harness demo projects require a visible-login setup project",
  );
}
if (harnessUiDependencyCount < 2 || harnessUiStorageCount < 2) {
  violations.push(
    "harness-demo and harness-demo-video must use the visible-login storage state",
  );
}
if (!harnessUiAuth) {
  violations.push("playwright/tests/harness-ui-auth.setup.ts is missing");
} else {
  if (
    /\brequest\b|page\.request|waitForResponse|expect\.poll/.test(harnessUiAuth)
  ) {
    violations.push(
      "harness UI authentication must not use request fixtures, response polling, or API probes",
    );
  }
  if (
    !harnessUiAuth.includes('page.goto("/login"') ||
    !harnessUiAuth.includes('page.locator("#loginName")') ||
    !harnessUiAuth.includes('page.locator("#password")')
  ) {
    violations.push(
      "harness UI authentication must drive the visible OpenELIS login form",
    );
  }
}
if (!demoPresentation) {
  violations.push("playwright/helpers/demo-presentation.ts is missing");
} else if (/fullPage:\s*true/.test(demoPresentation)) {
  violations.push(
    "demo evidence must use viewport screenshots; full-page capture duplicates fixed application chrome",
  );
}

if (violations.length > 0) {
  console.error("Playwright bucket guard failed:");
  for (const violation of violations) {
    console.error(`  - ${violation}`);
  }
  process.exit(1);
}

console.log("Playwright bucket guard passed.");
