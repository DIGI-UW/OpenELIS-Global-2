import path from "node:path";
import { fileURLToPath } from "node:url";
import { ESLint } from "eslint";
import { describe, expect, test } from "vitest";

const frontendDirectory = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

/**
 * The src/ lint step in CI does not fail the build, so a route whose
 * component import was removed still merges and throws a ReferenceError
 * only when someone opens that route.
 */
describe("App routes", () => {
  test("render only components that App.jsx declares or imports", async () => {
    const eslint = new ESLint({ cwd: frontendDirectory });
    const [result] = await eslint.lintFiles(["src/App.jsx"]);
    const undefinedComponents = result.messages
      .filter((message) => message.ruleId === "react/jsx-no-undef")
      .map((message) => `${message.line}: ${message.message}`);

    expect(undefinedComponents).toEqual([]);
  }, 60_000);
});
