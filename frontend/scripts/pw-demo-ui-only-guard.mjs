import fs from "fs";
import path from "path";

const root = path.resolve(process.cwd(), "playwright");
const violations = [];

const demoUiOnlyFiles = [
  path.resolve(root, "tests/file-import-ui.spec.ts"),
  path.resolve(root, "tests/file-import-results.spec.ts"),
  path.resolve(root, "tests/astm-genexpert-results.spec.ts"),
  path.resolve(root, "tests/demo-quantstudio-file-config.spec.ts"),
  path.resolve(root, "tests/ogc-284-demo-video.spec.ts"),
  path.resolve(root, "helpers/accept-results.ts"),
  path.resolve(root, "helpers/analyzer-dashboard.ts"),
];

const bannedPatterns = [
  {
    label: "console or pageerror listeners",
    regex: /page\.on\(\s*["'`](console|pageerror)["'`]/g,
  },
  {
    label: "debug context capture",
    regex: /captureDebugContext\(/g,
  },
  {
    label: "response-based synchronization",
    regex: /waitForResponse\(/g,
  },
  {
    label: "backend polling",
    regex: /expect\.poll\(/g,
  },
  {
    label: "backend GET\/PUT\/DELETE assertions",
    regex: /page\.request\.(get|put|delete)\(/g,
  },
];

for (const file of demoUiOnlyFiles) {
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
