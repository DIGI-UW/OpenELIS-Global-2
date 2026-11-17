/**
 * The shell's Module Federation configuration (docs/modules.md). The core
 * frontend is the host of every module remote and is itself a remote named
 * `openelis_core`: the surface under src/module-exports is what a module may
 * import (`openelis_core/components`, `/api`, `/hooks`, `/contexts`, `/sdk`).
 *
 * The shared singletons are the frontend contract: a module is built with the
 * same list and runs on the shell's copies, so a second React, Router, Intl,
 * Carbon or Query client never loads. The list is written once, in the module
 * SDK (packages/module-sdk/shared.js), which a module installs from this
 * checkout.
 */
import {
  CORE_REMOTE_NAME,
  sharedSingletons,
} from "./packages/module-sdk/shared.js";

export const SHARED_SINGLETONS = sharedSingletons;

export { CORE_REMOTE_NAME };

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
