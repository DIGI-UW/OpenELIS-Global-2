import { useEffect, useRef, useState } from "react";

const NONE = Object.freeze({});
const DEFAULT_PATTERN = "messages/{locale}.json";

/** The bundle codes `pickModuleMessages` layers for a locale, each read once. */
export function messageCodes(locale) {
  if (!locale) {
    return ["en"];
  }
  const base = locale.split(/[-_]/)[0];
  return [...new Set(["en", base, locale.replace("-", "_"), locale])];
}

/** A bundle's path under the module's asset base, from the manifest's `frontend.messages`. */
export function bundlePath(module, code) {
  const pattern =
    (module.frontend && module.frontend.messages) || DEFAULT_PATTERN;
  return pattern.replace("{locale}", code);
}

/**
 * The remote entry `localStorage["oe.module-override.<id>"]` points a module
 * at in development (by id or by remote name), or null.
 */
export function devOverrideEntry(moduleId, remoteName) {
  try {
    return (
      window.localStorage.getItem(`oe.module-override.${moduleId}`) ||
      (remoteName &&
        window.localStorage.getItem(`oe.module-override.${remoteName}`)) ||
      null
    );
  } catch {
    return null;
  }
}

/**
 * Where a module's dev server answers, or null when the module's files come
 * from its JAR: the directory of the override's entry when one is set, else
 * the origin the module's English bundle redirects to (`scripts/dev-stack up
 * --module` sends the bundles to the dev server as it does the entry).
 */
export async function devServerBase(module, fetchBundle) {
  const override = devOverrideEntry(module.id, module.remoteName);
  if (override) {
    return new URL(".", new URL(override, window.location.href)).href;
  }
  try {
    const response = await fetchBundle(
      `${module.assetBase}${bundlePath(module, "en")}`,
    );
    return response.redirected ? `${new URL(response.url).origin}/` : null;
  } catch {
    return null;
  }
}

/**
 * Message bundles read from the modules' dev servers, by module id and then
 * code, for the codes the active locale uses. A development shell layers them
 * over the registry's (which the backend reads from the installed JAR), so a
 * saved `frontend/public/messages/<code>.json` shows within `intervalMs`
 * without rebuilding the JAR. Each file is re-read while the page is
 * visible, as a conditional request; a module served from its JAR is asked
 * once and then left alone, and so is a code the module does not ship (a 404,
 * or the dev server's index page, which Vite answers for any unknown path);
 * a file read before that goes missing for a while (a checkout, a
 * rename-save) is asked again until it is back.
 * A file that does not parse keeps the last good copy and says so in the
 * console.
 */
export function useDevServerMessages(
  modules,
  locale,
  { enabled = true, intervalMs = 1000, fetchBundle } = {},
) {
  const [bundles, setBundles] = useState(NONE);
  const bases = useRef(new Map());
  const seen = useRef(new Map());
  const missing = useRef(new Set());

  useEffect(() => {
    if (!enabled || !modules || modules.length === 0) {
      return undefined;
    }
    const read =
      fetchBundle || ((url) => window.fetch(url, { cache: "no-cache" }));
    const codes = messageCodes(locale);
    let stopped = false;
    let running = false;

    const readModule = async (module) => {
      if (!bases.current.has(module.id)) {
        bases.current.set(module.id, devServerBase(module, read));
      }
      const base = await bases.current.get(module.id);
      if (!base) {
        return;
      }
      for (const code of codes) {
        const url = `${base}${bundlePath(module, code)}`;
        if (missing.current.has(url)) {
          continue;
        }
        let text;
        try {
          const response = await read(url);
          const type =
            (response.headers && response.headers.get("content-type")) || "";
          if (
            response.status === 404 ||
            (response.ok && !type.includes("json"))
          ) {
            if (!seen.current.has(url)) {
              missing.current.add(url);
            }
            continue;
          }
          if (!response.ok) {
            continue;
          }
          text = await response.text();
        } catch {
          continue;
        }
        if (stopped || seen.current.get(url) === text) {
          continue;
        }
        seen.current.set(url, text);
        let messages;
        try {
          messages = JSON.parse(text);
        } catch {
          console.warn(
            `module ${module.id}: ${bundlePath(module, code)} is not valid JSON; the last good copy stays`,
          );
          continue;
        }
        setBundles((current) => ({
          ...current,
          [module.id]: { ...(current[module.id] || {}), [code]: messages },
        }));
      }
    };

    const poll = async () => {
      if (running || stopped || document.visibilityState === "hidden") {
        return;
      }
      running = true;
      try {
        await Promise.all(modules.map(readModule));
      } finally {
        running = false;
      }
    };

    poll();
    const timer = window.setInterval(poll, intervalMs);
    return () => {
      stopped = true;
      window.clearInterval(timer);
    };
  }, [enabled, modules, locale, intervalMs, fetchBundle]);

  return enabled ? bundles : NONE;
}

const noDevServerMessages = () => NONE;

/**
 * The hook the shell calls: useDevServerMessages in a development build, and
 * a constant otherwise. The condition is the build's own flag, written inline,
 * so a production build keeps the constant and drops everything above; under
 * Vitest the constant too, and a test calls useDevServerMessages itself.
 */
export const useDevModuleMessages =
  import.meta.env.DEV && import.meta.env.MODE !== "test"
    ? useDevServerMessages
    : noDevServerMessages;
