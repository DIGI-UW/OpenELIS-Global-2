/**
 * The Vite configuration of a module's frontend, derived from its manifest
 * (docs/modules.md): the remote name, the output directory inside the Maven
 * build, the `openelis_core` remote, the shell's shared singletons and the
 * exposes. The manifest is the only list of pages and slot contributions: each
 * `routes[].expose` and `slots[].expose` names a component file under
 * `src/pages` or `src/slots` by convention, and a name without a file fails
 * the build with the paths that were tried, instead of a placeholder at
 * runtime.
 *
 * `frontend/vite.config.js`:
 *
 *   import { openelisModule } from "@openelis/module-sdk/build";
 *   export default openelisModule();
 */
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import react from "@vitejs/plugin-react";
import { federation } from "@module-federation/vite";
import { coreRemote, sharedForModule } from "./shared.js";
import { openelisCoreAliases, openelisCoreDedupe } from "./testing/index.js";

export const DEFAULT_MANIFEST =
  "../src/main/resources/META-INF/openelis-module.json";
export const SOURCE_EXTENSIONS = Object.freeze(["jsx", "tsx", "js", "ts"]);

/** The remote name of a module: its id as a JavaScript identifier. */
export const remoteNameFor = (id) => id.replace(/-/g, "_");

/** Where the Maven build packages the bundle, relative to `frontend/`. */
export const outDirFor = (id) =>
  `../target/classes/META-INF/resources/modules/${id}`;

/** Reads and parses the manifest; a missing or malformed file names itself. */
export function readManifest(path) {
  let text;
  try {
    text = readFileSync(path, "utf8");
  } catch (reason) {
    throw new Error(
      `openelisModule: the manifest ${path} cannot be read (${reason.message})`,
      { cause: reason },
    );
  }
  try {
    return JSON.parse(text);
  } catch (reason) {
    throw new Error(
      `openelisModule: the manifest ${path} is not JSON (${reason.message})`,
      { cause: reason },
    );
  }
}

/**
 * The exposes of a module, one per manifest route and slot contribution:
 * `./Name` is `<pages>/Name.<ext>` for a route and `<slots>/Name.<ext>` for a
 * slot, the first extension in `extensions` that exists under `root`, unless
 * `overrides["./Name"]` names the file. Every expose without a file is
 * reported in one error.
 */
export function exposesFor(
  manifest,
  {
    root = process.cwd(),
    pages = "src/pages",
    slots = "src/slots",
    overrides = {},
    extensions = SOURCE_EXTENSIONS,
  } = {},
) {
  const frontend = manifest.frontend || {};
  const wanted = [
    ...(frontend.routes || []).map((route) => ({
      expose: route.expose,
      directory: pages,
      origin: `route ${route.path}`,
    })),
    ...(frontend.slots || []).map((slot) => ({
      expose: slot.expose,
      directory: slots,
      origin: `slot ${slot.slot}`,
    })),
  ];
  const exposes = {};
  const problems = [];
  for (const { expose, directory, origin } of wanted) {
    if (!expose || exposes[expose]) {
      continue;
    }
    if (overrides[expose]) {
      exposes[expose] = overrides[expose];
      continue;
    }
    const name = expose.replace(/^\.\//, "");
    const candidates = extensions.map((ext) => `./${directory}/${name}.${ext}`);
    const found = candidates.find((candidate) =>
      existsSync(resolve(root, candidate)),
    );
    if (found) {
      exposes[expose] = found;
    } else {
      problems.push(
        `expose "${expose}" (${origin}) has no source file; tried ${candidates.join(", ")}`,
      );
    }
  }
  for (const [expose, file] of Object.entries(overrides)) {
    if (!exposes[expose]) {
      exposes[expose] = file;
    }
  }
  if (problems.length > 0) {
    throw new Error(
      `openelisModule: the manifest names ${problems.length === 1 ? "an expose" : "exposes"} the frontend does not have\n  ${problems.join("\n  ")}`,
    );
  }
  return exposes;
}

/**
 * The federation plugin's options for a module: the remote name from the id,
 * the exposes from the manifest (see `exposesFor`), the shell as the
 * `openelis_core` remote and the shell's singletons, none bundled.
 */
export function federationOptionsFor(manifest, options = {}) {
  if (!manifest || !manifest.id) {
    throw new Error("openelisModule: the manifest has no id");
  }
  return {
    name: remoteNameFor(manifest.id),
    filename: "remoteEntry.js",
    exposes: exposesFor(manifest, {
      root: options.root,
      pages: options.pages,
      slots: options.slots,
      overrides: options.exposes,
    }),
    remotes: { openelis_core: coreRemote(options.coreEntry) },
    shared: sharedForModule(),
    dts: false,
  };
}

/**
 * The module's Vite configuration as a function of the build mode, for
 * `export default openelisModule()` (Vite and Vitest both accept it).
 *
 * Options, all optional: `manifest` (path, relative to `root`), `root` (the
 * frontend directory, default the working directory), `pages`, `slots`
 * (component directories), `exposes` (file overrides by expose name),
 * `coreEntry` (the shell's remote entry URL), `outDir`, `port` (the dev
 * server's, default 5174), `server`, `build`, `test` and `resolve` (merged over
 * the defaults), `plugins` (appended after the federation plugin).
 */
export function openelisModule(options = {}) {
  const root = options.root || process.cwd();
  const manifest = readManifest(
    resolve(root, options.manifest || DEFAULT_MANIFEST),
  );
  const id = manifest.id;
  const port = options.port || 5174;
  const remote = federationOptionsFor(manifest, { ...options, root });
  return ({ mode }) => ({
    plugins: [react(), federation(remote), ...(options.plugins || [])],
    build: {
      outDir: options.outDir || outDirFor(id),
      emptyOutDir: true,
      sourcemap: mode !== "production",
      target: "es2020",
      ...(options.build || {}),
    },
    server: {
      port,
      strictPort: true,
      cors: true,
      origin: `http://localhost:${port}`,
      ...(options.server || {}),
    },
    resolve: {
      dedupe: openelisCoreDedupe,
      ...(options.resolve || {}),
    },
    test: {
      environment: "jsdom",
      globals: true,
      alias: openelisCoreAliases,
      ...(options.test || {}),
    },
  });
}
