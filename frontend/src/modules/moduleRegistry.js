import semverSatisfies from "semver/functions/satisfies";
import semverValidRange from "semver/ranges/valid";
import moduleSdk from "../../packages/module-sdk/package.json";

/**
 * The frontend contract this shell publishes: React 17, React Router 5,
 * react-intl 5, Carbon, TanStack Query 4 and the `openelis_core` surface. Its
 * version is the module SDK's package version, the one source; the backend's
 * ModuleFrontendContract is kept in step by a test. Bumped as a major when a
 * shared singleton's major, an exposed component or a slot's props change.
 */
export const FRONTEND_CONTRACT_VERSION = moduleSdk.version;

/** Where the backend serves a started module's prebuilt assets, through the API proxy. */
export function assetBaseUrl(serverBaseUrl, module) {
  const base = module.assetBase || `/modules/${module.id}/`;
  return `${serverBaseUrl}${base}`;
}

/**
 * Whether the shell may load a module's frontend: the module is started, it
 * declares a frontend with an entry, and its `requires.frontendContract`
 * accepts this shell's contract version.
 */
export function frontendCompatibility(module) {
  if (module.state !== "STARTED") {
    return { loadable: false, reason: `module is ${module.state}` };
  }
  if (!module.frontend || !module.frontend.entry) {
    return { loadable: false, reason: "no frontend" };
  }
  const range = module.requires && module.requires.frontendContract;
  if (!range) {
    return { loadable: false, reason: "no requires.frontendContract" };
  }
  if (!semverValidRange(range)) {
    return {
      loadable: false,
      reason: `invalid frontend contract range '${range}'`,
    };
  }
  if (!semverSatisfies(FRONTEND_CONTRACT_VERSION, range)) {
    return {
      loadable: false,
      reason: `requires frontend contract ${range}, this shell is ${FRONTEND_CONTRACT_VERSION}`,
    };
  }
  return { loadable: true, reason: null };
}

/**
 * Turns the registry payload into what the shell renders: remotes to
 * register, routes, slot contributions and each module's message bundles.
 */
export function planModules(registry, serverBaseUrl) {
  const modules = (registry && registry.modules) || [];
  const loadable = [];
  const skipped = [];
  modules.forEach((module) => {
    const compatibility = frontendCompatibility(module);
    if (!compatibility.loadable) {
      skipped.push({ ...module, frontendReason: compatibility.reason });
      return;
    }
    loadable.push({
      ...module,
      remoteName: module.remoteName || module.id.replace(/-/g, "_"),
      assetBase: assetBaseUrl(serverBaseUrl, module),
      permissionPrefix: `mod.${module.id}.`,
      messages: module.messages || {},
    });
  });
  const remotes = loadable.map((module) => ({
    name: module.remoteName,
    entry: `${module.assetBase}${module.frontend.entry}`,
    type: "module",
  }));
  const routes = [];
  const slots = {};
  loadable.forEach((module) => {
    (module.frontend.routes || []).forEach((route) => {
      routes.push({
        moduleId: module.id,
        remoteName: module.remoteName,
        path: route.path,
        exact: route.exact !== false,
        expose: route.expose,
        permission: route.permission
          ? `${module.permissionPrefix}${route.permission}`
          : null,
        role: route.role || null,
      });
    });
    (module.frontend.slots || []).forEach((slot) => {
      if (!slots[slot.slot]) {
        slots[slot.slot] = [];
      }
      slots[slot.slot].push({
        moduleId: module.id,
        remoteName: module.remoteName,
        expose: slot.expose,
        order: slot.order == null ? 100 : slot.order,
        permission: slot.permission
          ? `${module.permissionPrefix}${slot.permission}`
          : null,
      });
    });
  });
  Object.values(slots).forEach((list) =>
    list.sort((a, b) => a.order - b.order),
  );
  return { modules: loadable, skipped, remotes, routes, slots };
}

/**
 * A module's messages for a locale, key by key: the exact code's text first
 * (`fr-CA`, also spelt `fr_CA`), then the base language's, then English, the
 * same layering the core applies to its own bundles. A translation that has
 * not caught up with every key therefore shows English for the rest instead
 * of raw message ids; empty when the module ships none of the bundles.
 */
export function pickModuleMessages(bundles, locale) {
  if (!bundles || !locale) {
    return {};
  }
  const base = locale.split(/[-_]/)[0];
  const layers = ["en", base, locale.replace("-", "_"), locale]
    .map((code) => bundles[code])
    .filter(Boolean);
  return Object.assign({}, ...layers);
}

/** The expose name without its `./` prefix, as `loadRemote("<remote>/<name>")` wants it. */
export function exposeName(expose) {
  return expose.startsWith("./") ? expose.substring(2) : expose;
}
