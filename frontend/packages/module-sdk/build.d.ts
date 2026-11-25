/**
 * The module build preset (build.js): a module's Vite configuration derived
 * from its manifest. `frontend/vite.config.ts`:
 *
 *   import { openelisModule } from "@openelis/module-sdk/build";
 *   export default openelisModule();
 */
import type { CoreRemote, SharedSingleton } from "./shared";

/** The parts of the manifest the preset reads. */
export interface ModuleManifestForBuild {
  id: string;
  frontend?: {
    routes?: Array<{ path: string; expose: string }>;
    slots?: Array<{ slot: string; expose: string }>;
  };
  [key: string]: unknown;
}

export interface ExposeOptions {
  /** The frontend directory the paths are relative to; the working directory by default. */
  root?: string;
  /** Where route components live, `src/pages` by default. */
  pages?: string;
  /** Where slot components live, `src/slots` by default. */
  slots?: string;
  /** A file per expose name (`"./Home": "./src/Home.tsx"`), over the convention. */
  overrides?: Record<string, string>;
  /** The extensions tried in order, `SOURCE_EXTENSIONS` by default. */
  extensions?: readonly string[];
}

export interface OpenelisModuleOptions {
  /** The manifest's path relative to `root`, `DEFAULT_MANIFEST` by default. */
  manifest?: string;
  root?: string;
  pages?: string;
  slots?: string;
  /** A file per expose name, over the `pages`/`slots` convention. */
  exposes?: Record<string, string>;
  /** The shell's remote entry URL, `/remoteEntry.js` by default. */
  coreEntry?: string;
  /** Where the bundle goes, inside the Maven build by default. */
  outDir?: string;
  /** The dev server's port, 5174 by default. */
  port?: number;
  /** Merged over the preset's Vite `server` settings. */
  server?: Record<string, unknown>;
  /** Merged over the preset's Vite `build` settings. */
  build?: Record<string, unknown>;
  /** Merged over the preset's Vite `resolve` settings. */
  resolve?: Record<string, unknown>;
  /** Merged over the preset's Vitest settings (jsdom, globals, the doubles' aliases). */
  test?: Record<string, unknown>;
  /** Vite plugins appended after the federation plugin. */
  plugins?: unknown[];
}

/** The federation plugin's options for a module. */
export interface FederationOptions {
  name: string;
  filename: "remoteEntry.js";
  exposes: Record<string, string>;
  remotes: { openelis_core: CoreRemote };
  shared: Record<string, SharedSingleton & { import: false }>;
  experiments: { externalRuntime: true };
  dts: false;
}

/** The manifest path the preset reads, relative to `frontend/`. */
export const DEFAULT_MANIFEST: string;

/** The extensions a page or slot file may have, in the order they are tried. */
export const SOURCE_EXTENSIONS: readonly string[];

/** A module's remote name: its id as a JavaScript identifier. */
export function remoteNameFor(id: string): string;

/** Where the Maven build packages the bundle, relative to `frontend/`. */
export function outDirFor(id: string): string;

/** Reads and parses the manifest; a missing or malformed file names itself. */
export function readManifest(path: string): ModuleManifestForBuild;

/** The exposes, one per manifest route and slot contribution; an expose without a file throws. */
export function exposesFor(
  manifest: ModuleManifestForBuild,
  options?: ExposeOptions,
): Record<string, string>;

export function federationOptionsFor(
  manifest: ModuleManifestForBuild,
  options?: {
    root?: string;
    pages?: string;
    slots?: string;
    exposes?: Record<string, string>;
    coreEntry?: string;
  },
): FederationOptions;

/** Loads a build dependency from the module's own node_modules, else from the SDK's. */
export function loadBuildDependency(root: string, name: string): Promise<any>;

/** The module's Vite configuration as a function of the build mode; Vite and Vitest both accept it. */
export function openelisModule(
  options?: OpenelisModuleOptions,
): (env: { mode: string }) => Promise<Record<string, unknown>>;
