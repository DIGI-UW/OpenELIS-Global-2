import fs from "fs";
import path from "path";

const projectRoot = process.cwd();
const configPath = path.resolve(projectRoot, "playwright.config.ts");
const testsRoot = path.resolve(projectRoot, "playwright/tests");

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

if (violations.length > 0) {
  console.error("Playwright bucket guard failed:");
  for (const violation of violations) {
    console.error(`  - ${violation}`);
  }
  process.exit(1);
}

console.log("Playwright bucket guard passed.");
