/**
 * The frontend contract between the OpenELIS shell and its modules, in the
 * form both Vite configurations consume (docs/modules.md).
 *
 * The shell is the host of every module remote and is itself the remote
 * `openelis_core`, whose entry points are declared in ./types. The shared
 * singletons are the libraries a module runs on the shell's copies of: the
 * shell lists them with its own copies, a module lists the same keys with
 * `import: false` so it never bundles a fallback. The federation plugin
 * shares exact keys only; a library missing from this list is bundled by the
 * module, and a context from that copy reads null in the shell.
 */
export const CORE_REMOTE_NAME = "openelis_core";

export const CORE_ENTRY_POINTS = Object.freeze([
  "components",
  "api",
  "hooks",
  "contexts",
  "sdk",
]);

export const sharedSingletons = Object.freeze({
  react: { singleton: true, requiredVersion: "^17.0.0" },
  // The plugin shares exact keys: without this one every module bundles its
  // own copy of the JSX runtime (bound to the shared React, so it works, but
  // it is one more chunk per module).
  "react/jsx-runtime": { singleton: true, requiredVersion: "^17.0.0" },
  "react-dom": { singleton: true, requiredVersion: "^17.0.0" },
  "react-router-dom": { singleton: true, requiredVersion: "^5.2.0" },
  "react-intl": { singleton: true, requiredVersion: "^5.20.0" },
  "@carbon/react": { singleton: true, requiredVersion: "^1.15.0" },
  "@carbon/icons-react": { singleton: true, requiredVersion: "^11.17.0" },
  "@tanstack/react-query": { singleton: true, requiredVersion: "^4.44.0" },
});

/**
 * The shared list for a module's Vite configuration: the shell's singletons,
 * none of them bundled.
 */
export const sharedForModule = () =>
  Object.fromEntries(
    Object.entries(sharedSingletons).map(([name, options]) => [
      name,
      { ...options, import: false },
    ]),
  );

/**
 * The shell as a remote of a module. The entry is served next to the shell's
 * index.html on the same origin, so the default is right for a deployment and
 * for `scripts/dev-stack up --module`.
 */
export const coreRemote = (entry = "/remoteEntry.js") => ({
  type: "module",
  name: CORE_REMOTE_NAME,
  entry,
});
