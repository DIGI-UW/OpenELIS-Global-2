import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";

const SRC = path.dirname(fileURLToPath(import.meta.url));
const ENTRY = path.join(SRC, "index.jsx");

// Already unreachable on develop; this PR neither caused nor fixes them.
const KNOWN_UNREACHABLE = new Set([
  "patient-header2",
  "patient-name",
  "patient-dob",
  "patient-id",
  "analyzer-type-affected",
  "analyzer-type-affected__item",
]);

const EXTENSIONS = ["", ".js", ".jsx", ".ts", ".tsx", ".css", ".scss", ".json"];
const CLASS_ATTRIBUTE =
  /\b(?:className|class)\s*=\s*(?:"([^"]*)"|'([^']*)'|\{`([^`]*)`\}|\{"([^"]*)"\}|\{'([^']*)'\})/g;

function walk(dir, found = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, found);
    } else {
      found.push(full);
    }
  }
  return found;
}

function resolveImport(importer, specifier) {
  if (!specifier.startsWith(".")) return null;
  const base = path.resolve(path.dirname(importer), specifier);
  for (const extension of EXTENSIONS) {
    const candidate = base + extension;
    if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
      return candidate;
    }
  }
  for (const extension of [".js", ".jsx", ".ts", ".tsx"]) {
    const candidate = path.join(base, "index" + extension);
    if (fs.existsSync(candidate)) return candidate;
  }
  return null;
}

function topLevelSelectors(stylesheet) {
  const css = stylesheet.replace(/\/\*[\s\S]*?\*\//g, "");
  const selectors = [];
  let depth = 0;
  let buffer = "";
  for (const character of css) {
    if (character === "{") {
      if (depth === 0) {
        selectors.push(buffer.replace(/\s+/g, " ").trim());
        buffer = "";
      }
      depth++;
    } else if (character === "}") {
      depth--;
      if (depth === 0) buffer = "";
    } else if (depth === 0) {
      // A top-level ";" ends an @use/@import; without this the next selector
      // reads as "@use ... .microbiology-admin" and looks like an at-rule.
      buffer = character === ";" ? "" : buffer + character;
    }
  }
  return selectors.filter((selector) => selector && !selector.startsWith("@"));
}

// Split at Carbon: appWideStylesAfterCarbon.scss is imported from App.jsx
// after ./index.scss, appWideStyles.scss from index.jsx before it.
const AGGREGATES = [
  path.join(SRC, "appWideStyles.scss"),
  path.join(SRC, "appWideStylesAfterCarbon.scss"),
];

const allFiles = walk(SRC);
const isTest = (file) =>
  /\.(test|spec)\./.test(file) || /__tests__|__mocks__/.test(file);
const modules = allFiles.filter(
  (file) => /\.(jsx?|tsx?)$/.test(file) && !isTest(file),
);
const stylesheets = allFiles.filter((file) => /\.(css|scss)$/.test(file));

const staticImports = new Map();
const dynamicImports = new Map();
for (const module of modules) {
  const source = fs.readFileSync(module, "utf8");
  const statics = [
    ...source.matchAll(/(?:from\s*|^\s*import\s+)["']([^"']+)["']/gm),
  ];
  const dynamics = [...source.matchAll(/import\s*\(\s*["']([^"']+)["']/g)];
  const resolve = (matches) => [
    ...new Set(
      matches.map((match) => resolveImport(module, match[1])).filter(Boolean),
    ),
  ];
  staticImports.set(module, resolve(statics));
  dynamicImports.set(module, resolve(dynamics));
}

// Sass resolves an extensionless @import against stylesheets only, so reusing
// resolveImport here would point "./components/home/Dashboard" at Dashboard.tsx.
function resolveStylesheetImport(importer, specifier) {
  if (!specifier.startsWith(".")) return null;
  const base = path.resolve(path.dirname(importer), specifier);
  for (const extension of ["", ".scss", ".css"]) {
    const candidate = base + extension;
    if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
      return candidate;
    }
  }
  return null;
}

for (const stylesheet of stylesheets) {
  const references = [
    ...fs
      .readFileSync(stylesheet, "utf8")
      .matchAll(/^\s*@(?:import|use)\s+["']([^"']+)["']/gm),
  ];
  staticImports.set(stylesheet, [
    ...new Set(
      references
        .map((match) => resolveStylesheetImport(stylesheet, match[1]))
        .filter(Boolean),
    ),
  ]);
}

const staticClosures = new Map();
function staticClosure(module) {
  if (staticClosures.has(module)) return staticClosures.get(module);
  const seen = new Set([module]);
  staticClosures.set(module, seen);
  const stack = [module];
  while (stack.length) {
    for (const next of staticImports.get(stack.pop()) || []) {
      if (seen.has(next)) continue;
      seen.add(next);
      if (staticImports.has(next)) stack.push(next);
    }
  }
  return seen;
}

// What the browser has loaded on each route: the entry, plus that route's chunk.
const entryClosure = staticClosure(ENTRY);
const routeClosures = new Map();
const routeRoots = new Set([ENTRY]);
for (const module of modules) {
  for (const target of dynamicImports.get(module) || []) routeRoots.add(target);
}
for (const root of routeRoots) {
  routeClosures.set(root, new Set([...entryClosure, ...staticClosure(root)]));
}

const selectorsByStylesheet = new Map(
  stylesheets.map((stylesheet) => [
    stylesheet,
    topLevelSelectors(fs.readFileSync(stylesheet, "utf8")),
  ]),
);

const usersByClass = new Map();
for (const module of modules) {
  for (const match of fs
    .readFileSync(module, "utf8")
    .matchAll(CLASS_ATTRIBUTE)) {
    const value =
      match[1] ?? match[2] ?? match[3] ?? match[4] ?? match[5] ?? "";
    for (const token of value.split(/[\s${}()?:|&,'"+]+/)) {
      if (!token || token.startsWith("cds--")) continue;
      if (!usersByClass.has(token)) usersByClass.set(token, new Set());
      usersByClass.get(token).add(module);
    }
  }
}

function stylesheetsStyling(className) {
  const pattern = new RegExp(
    "(^|[\\s,>+~])\\." +
      className.replace(/[-.]/g, "\\$&") +
      "($|[\\s,>:.\\[])",
  );
  return stylesheets.filter((stylesheet) =>
    selectorsByStylesheet.get(stylesheet).some((s) => pattern.test(s)),
  );
}

describe("app-wide stylesheet reach", () => {
  it("styles every class on every route that can render it", () => {
    const unreachable = [];
    for (const [className, users] of usersByClass) {
      if (KNOWN_UNREACHABLE.has(className)) continue;
      const owners = stylesheetsStyling(className);
      if (!owners.length) continue;
      if (owners.some((owner) => entryClosure.has(owner))) continue;
      for (const [root, loaded] of routeClosures) {
        if (owners.some((owner) => loaded.has(owner))) continue;
        const stranded = [...users].filter((user) => loaded.has(user));
        if (!stranded.length) continue;
        unreachable.push(
          `.${className} (${owners.map((o) => path.relative(SRC, o)).join(", ")})` +
            ` is not loaded on the route rooted at ${path.relative(SRC, root)},` +
            ` which renders ${path.relative(SRC, stranded[0])}`,
        );
        break;
      }
    }
    expect(unreachable).toEqual([]);
  });

  it("keeps element-wide rules out of the app-wide stylesheets", () => {
    const imported = AGGREGATES.flatMap((aggregate) =>
      [
        ...fs
          .readFileSync(aggregate, "utf8")
          .matchAll(/^\s*@import\s+"([^"]+)"/gm),
      ].map((match) => resolveStylesheetImport(aggregate, match[1])),
    );
    const leaks = [];
    for (const stylesheet of imported) {
      for (const selector of selectorsByStylesheet.get(stylesheet) || []) {
        for (const one of selector.split(",")) {
          if (/^\s*(body|html|\*)\b/.test(one)) {
            leaks.push(`${path.relative(SRC, stylesheet)}: ${selector}`);
          }
        }
      }
    }
    expect(leaks).toEqual([]);
  });

  // The reach assertion asks only whether SOME owner sheet is in the closure,
  // so a sheet imported by both an aggregate and its own component passes it
  // while Vite emits that sheet into the entry AND into that route's lazy
  // chunk. The preload helper appends the lazy copy after the entry and never
  // removes it, so from the first visit to the route its rules win every
  // equal-specificity tie for the rest of the session, app-wide.
  it("gives each aggregated sheet exactly one importer", () => {
    const importers = new Map();
    for (const graph of [staticImports, dynamicImports]) {
      for (const [importer, targets] of graph) {
        for (const target of targets) {
          if (!importers.has(target)) importers.set(target, new Set());
          importers.get(target).add(importer);
        }
      }
    }
    const shared = [];
    for (const aggregate of AGGREGATES) {
      for (const sheet of staticImports.get(aggregate) || []) {
        const others = [...(importers.get(sheet) || [])].filter(
          (importer) => importer !== aggregate,
        );
        if (!others.length) continue;
        shared.push(
          `${path.relative(SRC, sheet)} is aggregated by ` +
            `${path.relative(SRC, aggregate)} and also imported by ` +
            others.map((other) => path.relative(SRC, other)).join(", "),
        );
      }
    }
    expect(shared).toEqual([]);
  });
});
