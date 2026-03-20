import fs from "fs";
import path from "path";

const root = path.resolve(process.cwd(), "playwright");
const violations = [];

function collectAllTestFiles(dir, acc = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      collectAllTestFiles(full, acc);
    } else if (full.endsWith(".spec.ts")) {
      acc.push(full);
    }
  }
  return acc;
}

function patternsFromConfig(varName) {
  const configPath = path.resolve(process.cwd(), "playwright.config.ts");
  const content = fs.readFileSync(configPath, "utf8");
  const blockMatch = content.match(
    new RegExp(`const\\s+${varName}\\s*=\\s*\\[(.*?)\\];`, "s"),
  );
  if (!blockMatch) return [];
  return [...blockMatch[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]);
}

const allowedDemoPatterns = [
  ...patternsFromConfig("CORE_DEMO_TESTS"),
  ...patternsFromConfig("HARNESS_DEMO_TESTS"),
];

const allSpecFiles = collectAllTestFiles(path.resolve(root, "tests"));
const demoSpecFiles = allSpecFiles.filter((file) =>
  allowedDemoPatterns.some((pattern) => {
    const specName = pattern.replace("**/", "");
    if (specName.includes("*")) {
      const re = new RegExp(
        "^" +
          specName.replace(/[.+^${}()|[\]\\]/g, "\\$&").replace(/\*/g, ".*") +
          "$",
      );
      return re.test(path.basename(file));
    }
    return path.basename(file) === specName;
  }),
);

const bannedPatterns = [
  {
    label: "console or pageerror listeners",
    regex: /page\.on\(\s*["'`](console|pageerror)["'`]/g,
  },
  {
    label: "response-based synchronization",
    regex: /waitForResponse\(/g,
  },
  {
    label: "backend GET/PUT/DELETE assertions",
    regex: /page\.request\.(get|put|delete)\(/g,
  },
];

for (const file of demoSpecFiles) {
  if (!fs.existsSync(file)) {
    continue;
  }

  const content = fs.readFileSync(file, "utf8");
  const lines = content.split("\n");

  lines.forEach((line, idx) => {
    for (const rule of bannedPatterns) {
      if (rule.regex.test(line)) {
        violations.push(
          `${path.relative(process.cwd(), file)}:${idx + 1} ${rule.label}`,
        );
      }
      rule.regex.lastIndex = 0;
    }
  });
}

if (violations.length > 0) {
  console.error("UI-only demo guard found banned patterns:");
  violations.forEach((violation) => console.error(`  - ${violation}`));
  process.exit(1);
}

console.log("UI-only demo guard passed.");
