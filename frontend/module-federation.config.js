/**
 * The shell's Module Federation configuration (docs/modules.md). The core
 * frontend is the host of every module remote and is itself a remote named
 * `openelis_core`: the surface under src/module-exports is what a module may
 * import (`openelis_core/components`, `/api`, `/hooks`, `/contexts`, `/sdk`).
 *
 * The shared singletons are the frontend contract: a module is built with the
 * same list (see the module template) and runs on the shell's copies, so a
 * second React, Router, Intl, Carbon or Query client never loads.
 */
export const SHARED_SINGLETONS = {
  react: { singleton: true, requiredVersion: "^17.0.0" },
  "react-dom": { singleton: true, requiredVersion: "^17.0.0" },
  "react-router-dom": { singleton: true, requiredVersion: "^5.2.0" },
  "react-intl": { singleton: true, requiredVersion: "^5.20.0" },
  "@carbon/react": { singleton: true, requiredVersion: "^1.15.0" },
  "@carbon/icons-react": { singleton: true, requiredVersion: "^11.17.0" },
  "@tanstack/react-query": { singleton: true, requiredVersion: "^4.44.0" },
};

export const CORE_REMOTE_NAME = "openelis_core";

export const CORE_EXPOSES = {
  "./components": "./src/module-exports/components.js",
  "./api": "./src/module-exports/api.js",
  "./hooks": "./src/module-exports/hooks.js",
  "./contexts": "./src/module-exports/contexts.js",
  "./sdk": "./src/module-exports/sdk.js",
};

export default {
  name: CORE_REMOTE_NAME,
  filename: "remoteEntry.js",
  exposes: CORE_EXPOSES,
  shared: SHARED_SINGLETONS,
  // The exposed index is JavaScript; the type declarations modules compile
  // against are published with the module SDK, not generated here.
  dts: false,
};
