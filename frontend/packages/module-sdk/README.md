# @openelis/module-sdk

What a module's frontend compiles and tests against: the type declarations of
the shell's surface (`openelis_core/components`, `/api`, `/hooks`, `/contexts`,
`/sdk`), the federation settings both sides share, a base `tsconfig`, and one
test double per entry point. The package version is the frontend contract
version the shell publishes in `/rest/module-registry`.

Nothing here runs in the browser: a module keeps importing `openelis_core/*`
and the shell provides those at runtime. The full guide is
[docs/modules.md](../../../docs/modules.md) in the core repository.

## Install

The package is built from the core checkout a module targets and installed
from there (ADR 0001, decision 1). With the core checked out at `./core`
inside the module repository, as the module CI does:

```json
"devDependencies": {
  "@openelis/module-sdk": "file:../core/frontend/packages/module-sdk"
}
```

`scripts/dev-stack up --module <checkout>` links `<checkout>/core` to the core
checkout it runs from; for a hand-run build, `ln -s <core checkout> core`.

## Use

`vite.config.js`, the whole of it:

```js
import { openelisModule } from "@openelis/module-sdk/build";

export default openelisModule();
```

The preset reads the manifest and derives the remote name from the id, the
exposes from `frontend.routes[].expose` and `frontend.slots[].expose`
(`./ListPage` is `src/pages/ListPage.(jsx|tsx|js|ts)`, a slot's `./Card` is
`src/slots/Card.*`; a name without a file fails the build naming it), the
`openelis_core` remote, the shared singletons (`import: false`), the output
directory inside the Maven build, the dev server port, dedupe and the test
aliases. Options: `exposes` (file overrides), `pages`, `slots`, `manifest`,
`coreEntry`, `outDir`, `port`, `build`, `server`, `resolve`, `test`,
`plugins`. The pieces are exported too (`shared.js`: `coreRemote()`,
`sharedForModule()`; `build.js`: `exposesFor()`, `federationOptionsFor()`)
for a configuration written by hand.

`tsconfig.json`:

```json
{
  "extends": "@openelis/module-sdk/tsconfig.base.json",
  "include": ["src"]
}
```

`npx tsc --noEmit` then checks every `openelis_core/*` import by name and props.

Tests (`vite.config.js`, `test` block):

```js
import {
  openelisCoreAliases,
  openelisCoreDedupe,
} from "@openelis/module-sdk/testing";

export default defineConfig({
  // One React for the module and the doubles, which live in the core checkout
  resolve: { dedupe: openelisCoreDedupe },
  test: { environment: "jsdom", globals: true, alias: openelisCoreAliases },
});
```

The doubles answer from memory: `apiResponses` and `apiCalls` from
`@openelis/module-sdk/testing/sdk.jsx` script and record the API a page calls,
`testPatients` in `testing/components.jsx` feeds the patient search, and the
contexts in `testing/contexts.js` default to an administrator session.
