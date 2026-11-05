import React, { Suspense, useMemo, useState } from "react";
import { Loading } from "@carbon/react";
import { loadRemote, registerRemotes } from "@module-federation/runtime";
import ModuleErrorBoundary from "./ModuleErrorBoundary";
import { exposeName } from "./moduleRegistry";

const cache = new Map();
const remotes = new Map();
const ATTEMPTS = 3;
const BACKOFF_MS = 300;

/**
 * Imports one expose of a module remote, retrying a transient failure a few
 * times without ever reloading the page (a missing module bundle must only
 * take out its own slot or page, never the shell). A final failure drops the
 * cached lazy component, so the boundary's Retry and the next navigation
 * import the remote again instead of replaying a remembered rejection.
 */
async function importExpose(remoteName, key) {
  let lastError;
  for (let attempt = 1; attempt <= ATTEMPTS; attempt++) {
    try {
      const loaded = await loadRemote(key);
      if (!loaded) {
        throw new Error(`module remote ${key} returned nothing`);
      }
      return loaded.default ? loaded : { default: loaded };
    } catch (error) {
      lastError = error;
      if (attempt < ATTEMPTS) {
        await new Promise((resolve) =>
          setTimeout(resolve, BACKOFF_MS * attempt),
        );
      }
    }
  }
  cache.delete(key);
  forgetFailedRemote(remoteName);
  throw lastError;
}

/**
 * Registers module remotes with the federation runtime and remembers them, so
 * a remote whose entry failed to load can be registered again with `force`:
 * the runtime otherwise keeps the failed entry and every later import of that
 * remote fails without a request.
 */
export function registerModuleRemotes(list) {
  list.forEach((remote) => remotes.set(remote.name, remote));
  registerRemotes(list);
}

function forgetFailedRemote(remoteName) {
  const remote = remotes.get(remoteName);
  if (!remote) {
    return;
  }
  // The runtime imports an ES module remote with `import()`, and the browser
  // keeps a failed import of that exact URL for the life of the page, so the
  // remote comes back under a fresh entry URL the next load will fetch.
  const entry = `${remote.entry.split("?")[0]}?retry=${Date.now()}`;
  const refreshed = { ...remote, entry };
  remotes.set(remoteName, refreshed);
  try {
    registerRemotes([refreshed], { force: true });
  } catch (reason) {
    console.error(
      `module remote ${remoteName} could not be re-registered`,
      reason,
    );
  }
}

/**
 * The lazy component for one expose of a module remote, created once per
 * `<remote>/<expose>` and loaded on first render.
 */
export function remoteComponent(remoteName, expose) {
  const key = `${remoteName}/${exposeName(expose)}`;
  if (!cache.has(key)) {
    cache.set(
      key,
      React.lazy(() => importExpose(remoteName, key)),
    );
  }
  return cache.get(key);
}

/** Forgets every cached remote component (tests). */
export function resetRemoteComponents() {
  cache.clear();
}

/**
 * Renders an exposed module component inside its own error boundary and
 * suspense fallback, so a module that fails to load or to render shows a
 * placeholder where its page or slot would be and the rest of the shell is
 * unaffected.
 */
export default function LazyRemote({
  moduleId,
  remoteName,
  expose,
  fallback,
  silent,
  ...props
}) {
  // A failed import drops its cached lazy component; a retry must then pick a
  // fresh one, so the attempt is part of what the component is memoised on.
  const [attempt, setAttempt] = useState(0);
  const Component = useMemo(
    () => remoteComponent(remoteName, expose),
    [remoteName, expose, attempt],
  );
  return (
    <ModuleErrorBoundary
      moduleId={moduleId}
      silent={silent}
      onRetry={() => setAttempt((count) => count + 1)}
    >
      <Suspense
        fallback={
          fallback === undefined ? (
            <Loading withOverlay={false} small />
          ) : (
            fallback
          )
        }
      >
        <Component {...props} />
      </Suspense>
    </ModuleErrorBoundary>
  );
}
