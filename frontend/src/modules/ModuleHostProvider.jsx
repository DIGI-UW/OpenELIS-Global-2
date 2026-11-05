import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { registerModuleRemotes } from "./LazyRemote";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../components/utils/Utils";
import config from "../config.json";
import { planModules } from "./moduleRegistry";

const EMPTY = { modules: [], skipped: [], remotes: [], routes: [], slots: {} };

export const ModuleHostContext = createContext({
  ...EMPTY,
  status: "idle",
  registry: null,
  error: null,
  reload: () => {},
});

/**
 * Reads `/rest/module-registry` once per authenticated session, keeps the
 * started modules whose frontend contract this shell satisfies, registers
 * their remotes with the Module Federation runtime and offers the plan
 * (modules, routes, slot contributions) to the rest of the shell. A failed
 * read leaves the shell exactly as it is without modules. Plain state, no
 * query cache, so the shell renders the same with or without one.
 *
 * In development, `localStorage["oe.module-override.<id>"]` may point a
 * module's remote entry at the module's own Vite dev server.
 */
export default function ModuleHostProvider({ children }) {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const authenticated = Boolean(
    userSessionDetails && userSessionDetails.authenticated,
  );
  const [registry, setRegistry] = useState(null);
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState(null);
  const [generation, setGeneration] = useState(0);

  useEffect(() => {
    if (!authenticated) {
      setStatus("idle");
      setRegistry(null);
      return undefined;
    }
    let cancelled = false;
    setStatus("loading");
    readRegistry((data) => {
      if (cancelled) {
        return;
      }
      if (data && Array.isArray(data.modules)) {
        setRegistry(data);
        setError(null);
        setStatus("ready");
      } else {
        setRegistry(null);
        setError(new Error("module registry unavailable"));
        setStatus("error");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [authenticated, generation]);

  const plan = useMemo(
    () => (registry ? planModules(registry, config.serverBaseUrl) : EMPTY),
    [registry],
  );

  const registered = useRef(new Set());
  useEffect(() => {
    const fresh = plan.remotes
      .map((remote) => applyDevOverride(remote))
      .filter((remote) => !registered.current.has(remote.name));
    if (fresh.length === 0) {
      return;
    }
    try {
      registerModuleRemotes(fresh);
      fresh.forEach((remote) => registered.current.add(remote.name));
    } catch (reason) {
      console.error("module remotes could not be registered", reason);
    }
  }, [plan.remotes]);

  const value = useMemo(
    () => ({
      ...plan,
      status,
      registry,
      error,
      reload: () => setGeneration((n) => n + 1),
    }),
    [plan, status, registry, error],
  );

  return (
    <ModuleHostContext.Provider value={value}>
      {children}
    </ModuleHostContext.Provider>
  );
}

export function useModuleHost() {
  return useContext(ModuleHostContext);
}

/**
 * Reads the registry through the core's callback client (undefined on any
 * failure). Guarded so that a shell rendered with a partial client, as unit
 * tests of core pages do, simply runs without modules.
 */
function readRegistry(callback) {
  try {
    getFromOpenElisServer("/rest/module-registry", callback);
  } catch (reason) {
    console.error("module registry could not be requested", reason);
    callback(undefined);
  }
}

function applyDevOverride(remote) {
  if (!import.meta.env.DEV) {
    return remote;
  }
  try {
    const moduleId = remote.name.replace(/_/g, "-");
    const override =
      window.localStorage.getItem(`oe.module-override.${moduleId}`) ||
      window.localStorage.getItem(`oe.module-override.${remote.name}`);
    return override ? { ...remote, entry: override } : remote;
  } catch {
    return remote;
  }
}
