/**
 * The federation settings the shell and its modules share (shared.js).
 */

export interface SharedSingleton {
  singleton: true;
  requiredVersion: string;
}

export interface CoreRemote {
  type: "module";
  name: string;
  entry: string;
}

export const CORE_REMOTE_NAME: "openelis_core";

/** The shell's entry points, each an `openelis_core/<entry>` import. */
export const CORE_ENTRY_POINTS: readonly [
  "components",
  "api",
  "hooks",
  "contexts",
  "sdk",
];

/** The libraries a module runs on the shell's copies of, by package name. */
export const sharedSingletons: Readonly<Record<string, SharedSingleton>>;

/** The shared list for a module: the shell's singletons with `import: false`, none bundled. */
export function sharedForModule(): Record<
  string,
  SharedSingleton & { import: false }
>;

/** The shell as a module's `openelis_core` remote; the entry defaults to `/remoteEntry.js`. */
export function coreRemote(entry?: string): CoreRemote;
