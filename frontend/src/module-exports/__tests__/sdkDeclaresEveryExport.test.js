import { readFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";
import { describe, expect, it } from "vitest";
import federation from "../../../module-federation.config.js";
import { federationOptionsFor } from "../../../packages/module-sdk/build.js";
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

const sdkFile = (name) =>
  path.resolve(__dirname, "../../../packages/module-sdk", name);

/**
 * The top-level declarations of one of the SDK's module declaration files:
 * the exported value names and the modules it re-exports with `export *`.
 */
const declarationsOf = (file) => {
  const source = ts.createSourceFile(
    file,
    readFileSync(file, "utf8"),
    ts.ScriptTarget.Latest,
    true,
  );
  const values = new Set();
  const reExported = [];
  for (const node of source.statements) {
    if (
      ts.isExportDeclaration(node) &&
      !node.exportClause &&
      node.moduleSpecifier
    ) {
      reExported.push(node.moduleSpecifier.text);
      continue;
    }
    const exported = node.modifiers?.some(
      (m) => m.kind === ts.SyntaxKind.ExportKeyword,
    );
    if (!exported) continue;
    if (ts.isVariableStatement(node)) {
      for (const declaration of node.declarationList.declarations) {
        values.add(declaration.name.getText());
      }
    } else if (ts.isFunctionDeclaration(node) && node.name) {
      values.add(node.name.text);
    }
  }
  return { values: [...values].sort(), reExported };
};

const doubleFile = (entry) =>
  `testing/${entry}.${entry === "components" || entry === "sdk" ? "jsx" : "js"}`;

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

  it.each(CORE_ENTRY_POINTS)(
    "declare the openelis_core/%s double: the shell's surface re-exported, then each test control",
    async (entry) => {
      const runtime = Object.keys(await runtimeExports[entry]());
      const doubles = Object.keys(
        await import(`../../../packages/module-sdk/${doubleFile(entry)}`),
      );
      const { values, reExported } = declarationsOf(
        sdkFile(`testing/${entry}.d.ts`),
      );
      expect(reExported).toEqual([`openelis_core/${entry}`]);
      expect(values).toEqual(
        doubles.filter((name) => !runtime.includes(name)).sort(),
      );
    },
  );

  it.each([
    ["build.js", "build.d.ts"],
    ["shared.js", "shared.d.ts"],
    ["testing/index.js", "testing/index.d.ts"],
  ])("declare every export of %s in %s and nothing more", async (js, dts) => {
    const runtime = Object.keys(
      await import(`../../../packages/module-sdk/${js}`),
    ).sort();
    const { values, reExported } = declarationsOf(sdkFile(dts));
    expect(reExported).toEqual([]);
    expect(values).toEqual(runtime);
  });

  it("hand the shell's federation plugin and a module the same shared list", () => {
    expect(federation.shared).toBe(sharedSingletons);
    const forModule = sharedForModule();
    expect(Object.keys(forModule)).toEqual(Object.keys(sharedSingletons));
    for (const [name, options] of Object.entries(forModule)) {
      expect(options).toEqual({ ...sharedSingletons[name], import: false });
    }
  });

  it("has the shell provide the federation runtime that a preset-built module leaves out (contract 1.1)", () => {
    // A module built with externalRuntime throws at load on a shell without
    // provideExternalRuntime; nothing else in the unit tests or CI's E2E run
    // (no module installed) would notice the shell side going missing.
    expect(federation.experiments).toEqual({ provideExternalRuntime: true });
    const remote = federationOptionsFor({ id: "any-module", frontend: {} });
    expect(remote.experiments).toEqual({ externalRuntime: true });
  });
});
