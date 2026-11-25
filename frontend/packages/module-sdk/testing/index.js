/**
 * Test doubles for the shell's surface, one module per entry point, for a
 * module's Vitest run (docs/modules.md). `openelisCoreAliases` is the alias
 * list for `test.alias` in the module's Vite configuration, so a page that
 * imports `openelis_core/components` renders against these doubles instead of
 * the shell, which exists only at runtime.
 */
import { fileURLToPath } from "node:url";
import { sharedSingletons } from "../shared.js";

const here = (file) => fileURLToPath(new URL(file, import.meta.url));

/**
 * The shared libraries, for `resolve.dedupe` in the module's Vite
 * configuration: the doubles live in the core checkout and would otherwise
 * resolve their own React through it, and two Reacts in one test is the
 * "Invalid hook call" error.
 */
export const openelisCoreDedupe = Object.keys(sharedSingletons);

export const openelisCoreAliases = [
  {
    find: /^openelis_core\/components$/,
    replacement: here("./components.jsx"),
  },
  { find: /^openelis_core\/api$/, replacement: here("./api.js") },
  { find: /^openelis_core\/hooks$/, replacement: here("./hooks.js") },
  { find: /^openelis_core\/contexts$/, replacement: here("./contexts.js") },
  { find: /^openelis_core\/sdk$/, replacement: here("./sdk.jsx") },
];
