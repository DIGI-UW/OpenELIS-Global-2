import { readFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";
import { describe, expect, it } from "vitest";
import federation from "../../../module-federation.config.js";
import {
  CORE_ENTRY_POINTS,
  sharedForModule,
  sharedSingletons,
} from "../../../packages/module-sdk/shared.js";

/**
 * The SDK's ambient declarations are hand-written, so this is what keeps them
 * honest: every value the shell exports at runtime from an entry point is
 * declared under the matching `declare module "openelis_core/<entry>"`, and
 * nothing is declared that the runtime does not export. The shared singleton
 * list is the one the SDK hands to modules.
 */
const declarationFile = path.resolve(
  __dirname,
  "../../../packages/module-sdk/types/openelis_core.d.ts",
);

const declaredValues = () => {
  const source = ts.createSourceFile(
    declarationFile,
    readFileSync(declarationFile, "utf8"),
    ts.ScriptTarget.Latest,
    true,
  );
  const perModule = {};
  for (const statement of source.statements) {
    if (!ts.isModuleDeclaration(statement) || !statement.body) continue;
    const names = new Set();
    for (const node of statement.body.statements || []) {
      const exported = node.modifiers?.some(
        (m) => m.kind === ts.SyntaxKind.ExportKeyword,
      );
      if (!exported) continue;
      if (ts.isVariableStatement(node)) {
        for (const declaration of node.declarationList.declarations) {
          names.add(declaration.name.getText());
        }
      } else if (ts.isFunctionDeclaration(node) && node.name) {
        names.add(node.name.text);
      }
    }
    perModule[statement.name.text] = names;
  }
  return perModule;
};

const runtimeExports = {
  components: () => import("../components"),
  api: () => import("../api"),
  hooks: () => import("../hooks"),
  contexts: () => import("../contexts"),
  sdk: () => import("../sdk"),
};

describe("@openelis/module-sdk declarations", () => {
  const declared = declaredValues();

  it.each(CORE_ENTRY_POINTS)(
    "declare every runtime value of openelis_core/%s and nothing more",
    async (entry) => {
      const runtime = Object.keys(await runtimeExports[entry]()).sort();
      const types = [...(declared[`openelis_core/${entry}`] || [])].sort();
      expect(types).toEqual(runtime);
    },
  );

  it.each(CORE_ENTRY_POINTS)(
    "ship a test double for every runtime value of openelis_core/%s",
    async (entry) => {
      const runtime = Object.keys(await runtimeExports[entry]()).sort();
      const doubles = Object.keys(
        await import(
          `../../../packages/module-sdk/testing/${entry}.${entry === "components" || entry === "sdk" ? "jsx" : "js"}`
        ),
      );
      expect(runtime.filter((name) => !doubles.includes(name))).toEqual([]);
    },
  );

  it("hand the shell's federation plugin and a module the same shared list", () => {
    expect(federation.shared).toBe(sharedSingletons);
    const forModule = sharedForModule();
    expect(Object.keys(forModule)).toEqual(Object.keys(sharedSingletons));
    for (const [name, options] of Object.entries(forModule)) {
      expect(options).toEqual({ ...sharedSingletons[name], import: false });
    }
  });
});
