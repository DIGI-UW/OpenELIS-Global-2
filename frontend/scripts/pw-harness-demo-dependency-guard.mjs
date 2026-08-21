import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Linter } from "eslint";
import tsParser from "@typescript-eslint/parser";
import rule from "../eslint-local-rules/pw-demo-no-backend-access.js";

const DEMO_ROOT = "playwright/tests/demo/harness";
const AUTH_SETUP = "playwright/tests/auth.setup.ts";
const SOURCE_EXTENSIONS = [".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs"];
const RUNNER_DIAGNOSTIC_MESSAGE_IDS = new Set([
  "consoleListener",
  "networkListener",
]);
const RULE_ID = "local/pw-demo-no-backend-access";

function collectFiles(directory, predicate, files = []) {
  if (!fs.existsSync(directory)) return files;
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      collectFiles(absolutePath, predicate, files);
    } else if (entry.isFile() && predicate(absolutePath)) {
      files.push(absolutePath);
    }
  }
  return files;
}

function runtimeImportSources(source, filePath) {
  const { ast } = tsParser.parseForESLint(source, {
    comment: false,
    ecmaVersion: "latest",
    filePath,
    jsx: true,
    loc: true,
    range: true,
    sourceType: "module",
  });
  const imports = [];

  function recordRuntimeSource(sourceValue, node) {
    if (!sourceValue) return;
    imports.push({
      column: (node?.loc?.start.column ?? 0) + 1,
      line: node?.loc?.start.line ?? 1,
      source: sourceValue,
    });
  }

  function addRuntimeSource(node) {
    if (!node?.source || typeof node.source.value !== "string") return;
    if (node.importKind === "type" || node.exportKind === "type") return;
    if (
      node.type === "ImportDeclaration" &&
      node.specifiers.length > 0 &&
      node.specifiers.every((specifier) => specifier.importKind === "type")
    ) {
      return;
    }
    recordRuntimeSource(node.source.value, node.source);
  }

  function visit(node) {
    if (!node || typeof node !== "object") return;
    if (node.type === "ImportExpression") {
      const sourceValue = getStaticString(node.source);
      recordRuntimeSource(sourceValue, node.source);
    }
    if (
      node.type === "CallExpression" &&
      node.callee.type === "Identifier" &&
      node.callee.name === "require"
    ) {
      const sourceValue = getStaticString(node.arguments[0]);
      recordRuntimeSource(sourceValue, node.arguments[0]);
    }
    for (const [key, value] of Object.entries(node)) {
      if (["loc", "parent", "range", "tokens"].includes(key)) continue;
      if (Array.isArray(value)) value.forEach(visit);
      else visit(value);
    }
  }

  for (const node of ast.body) {
    if (
      node.type === "ImportDeclaration" ||
      node.type === "ExportAllDeclaration" ||
      node.type === "ExportNamedDeclaration"
    ) {
      addRuntimeSource(node);
    }
    visit(node);
  }
  return imports;
}

function getStaticString(node) {
  if (node?.type === "Literal" && typeof node.value === "string") {
    return node.value;
  }
  if (
    node?.type === "TemplateLiteral" &&
    node.expressions.length === 0 &&
    node.quasis.length === 1
  ) {
    return node.quasis[0].value.cooked;
  }
  return null;
}

function resolveLocalImport(importerPath, source, frontendRoot) {
  if (!source.startsWith(".")) return null;
  const unresolved = path.resolve(path.dirname(importerPath), source);
  const rootPrefix = `${path.resolve(frontendRoot)}${path.sep}`;
  if (!unresolved.startsWith(rootPrefix)) return null;

  const candidates = path.extname(unresolved)
    ? [unresolved]
    : [
        ...SOURCE_EXTENSIONS.map((extension) => `${unresolved}${extension}`),
        ...SOURCE_EXTENSIONS.map((extension) =>
          path.join(unresolved, `index${extension}`),
        ),
      ];
  return candidates.find((candidate) => fs.existsSync(candidate)) || null;
}

function lintBackendAccess(source, filename) {
  const linter = new Linter({ configType: "flat" });
  return linter.verify(
    source,
    [
      {
        files: ["**/*.{js,jsx,ts,tsx,mjs,cjs}"],
        languageOptions: {
          parser: tsParser,
          parserOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
          },
        },
        plugins: {
          local: {
            rules: { "pw-demo-no-backend-access": rule },
          },
        },
        rules: { [RULE_ID]: "error" },
      },
    ],
    { filename },
  );
}

function relativeToFrontend(frontendRoot, absolutePath) {
  return path.relative(frontendRoot, absolutePath).replaceAll(path.sep, "/");
}

export function findHarnessDemoDependencyViolations({
  frontendRoot = process.cwd(),
} = {}) {
  const absoluteFrontendRoot = path.resolve(frontendRoot);
  const specs = collectFiles(
    path.join(absoluteFrontendRoot, DEMO_ROOT),
    (filePath) => filePath.endsWith(".spec.ts"),
  ).sort();
  const violations = [];

  for (const specPath of specs) {
    const specRelativePath = relativeToFrontend(absoluteFrontendRoot, specPath);
    const visited = new Set();
    const pending = [specPath];
    const authSetupPath = path.join(absoluteFrontendRoot, AUTH_SETUP);
    if (fs.existsSync(authSetupPath)) pending.push(authSetupPath);

    while (pending.length > 0) {
      const dependencyPath = pending.shift();
      if (!dependencyPath || visited.has(dependencyPath)) continue;
      visited.add(dependencyPath);

      const dependencyRelativePath = relativeToFrontend(
        absoluteFrontendRoot,
        dependencyPath,
      );
      const source = fs.readFileSync(dependencyPath, "utf8");
      const isRunnerInfrastructure =
        dependencyRelativePath === "playwright/helpers/test-base.ts";
      for (const message of lintBackendAccess(source, dependencyRelativePath)) {
        if (message.ruleId !== RULE_ID) continue;
        if (
          isRunnerInfrastructure &&
          RUNNER_DIAGNOSTIC_MESSAGE_IDS.has(message.messageId)
        ) {
          continue;
        }
        violations.push({
          column: message.column,
          dependencyPath: dependencyRelativePath,
          line: message.line,
          message: message.message,
          messageId: message.messageId,
          specPath: specRelativePath,
        });
      }

      for (const runtimeImport of runtimeImportSources(
        source,
        dependencyPath,
      )) {
        const importedPath = resolveLocalImport(
          dependencyPath,
          runtimeImport.source,
          absoluteFrontendRoot,
        );
        if (importedPath && !visited.has(importedPath)) {
          pending.push(importedPath);
        } else if (runtimeImport.source.startsWith(".")) {
          violations.push({
            column: runtimeImport.column,
            dependencyPath: dependencyRelativePath,
            line: runtimeImport.line,
            message:
              "Demo specs must use resolvable local runtime imports so the UI-only dependency guard can inspect them.",
            messageId: "unresolvedLocalImport",
            specPath: specRelativePath,
          });
        }
      }
    }
  }

  return violations.sort((left, right) =>
    [left.specPath, left.dependencyPath, left.line, left.column]
      .join(":")
      .localeCompare(
        [right.specPath, right.dependencyPath, right.line, right.column].join(
          ":",
        ),
      ),
  );
}

function run() {
  const violations = findHarnessDemoDependencyViolations();
  if (violations.length === 0) {
    console.log("Harness demo dependency guard passed.");
    return;
  }

  console.error("Harness demo dependency guard failed:");
  for (const violation of violations) {
    console.error(
      `  - ${violation.specPath} -> ${violation.dependencyPath}:${violation.line}:${violation.column} ${violation.message}`,
    );
  }
  process.exitCode = 1;
}

const isMain =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) run();
