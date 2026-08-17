/**
 * Translation messages a deployment supplies for itself.
 *
 * <p>The bundled bundles under this directory are compiled into the JS chunks by
 * `npm run build`, so replacing a file on disk cannot change a string in a built
 * image — the bytes are already in the bundle. Overrides are therefore fetched
 * over HTTP at runtime from a directory the deployment mounts into the frontend
 * container. That is the one mechanism that behaves identically in the Vite dev
 * server (which serves `public/` at the web root) and in the nginx image (which
 * serves the same files from the built `dist/`, under a `location /translation/`
 * of their own).
 *
 * <p>Nothing here runs unless OVERRIDE_DEFAULT_TRANSLATION is on, and every
 * failure resolves to "no override" — a deployment that mounts nothing, mounts
 * an empty directory, or mounts a broken file sees exactly the shipped UI.
 */

/** Where the mounted directory is served from, in both images. */
export const OVERRIDE_BASE_PATH = "/translation";

/** No override: a frozen singleton so React can compare by identity. */
export const NO_OVERRIDE = Object.freeze({});

/**
 * The override files that apply to a locale, least specific first.
 *
 * <p>File names mirror this directory exactly, so a deployment's Transifex can
 * write into the mounted directory with no renaming step: the underscored form
 * `en_GB.json`, not the BCP-47 tag `en-GB` that react-intl works in.
 *
 * <p>A regional locale also picks up its base language's file, so a deployment
 * that rewords `fr.json` reaches a user running `fr-MG` rather than silently
 * doing nothing for them.
 */
export const overrideFilesFor = (locale) => {
  if (typeof locale !== "string" || !locale.trim()) {
    return [];
  }
  const tag = locale.trim().replace(/-/g, "_");
  const base = tag.split("_")[0];
  const files = base === tag ? [tag] : [base, tag];
  return files.map((name) => `${OVERRIDE_BASE_PATH}/${name}.json`);
};

/**
 * The message pairs in a parsed override file.
 *
 * <p>react-intl formats a message by treating it as a string, so a value that is
 * not one throws where it is rendered rather than where it was loaded. Keeping
 * only string values means a hand-edited file with a stray nested object costs
 * that one key instead of the screen it appears on.
 */
const messagePairsIn = (parsed, url) => {
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    console.warn(
      `[translation] ${url} is not a JSON object of message ids; ignoring it`,
    );
    return {};
  }
  const messages = {};
  let skipped = 0;
  Object.keys(parsed).forEach((key) => {
    if (typeof parsed[key] === "string") {
      messages[key] = parsed[key];
    } else {
      skipped += 1;
    }
  });
  if (skipped) {
    console.warn(
      `[translation] ${url}: ignored ${skipped} entr${
        skipped === 1 ? "y" : "ies"
      } whose value is not a string`,
    );
  }
  return messages;
};

/** One override file, or {} if it is absent, empty, or unreadable. */
const loadOne = async (url, fetchImpl) => {
  let response;
  try {
    response = await fetchImpl(url, { cache: "no-cache" });
  } catch (error) {
    // A deployment that mounts nothing is the common case, not an error.
    console.debug(`[translation] ${url} could not be reached`, error);
    return {};
  }
  if (!response || !response.ok) {
    console.debug(
      `[translation] no override at ${url} (${response && response.status})`,
    );
    return {};
  }
  // Both servers answer an unknown path with the SPA shell rather than a 404 —
  // nginx through try_files, the Vite dev server through its own history
  // fallback. A deployment mounts only the locales it customizes, so most of
  // these requests miss by design; without this check every miss would be a
  // page of HTML parsed as JSON, and "no override" would rest on the parse
  // failing. Debug rather than warn: a miss here is ordinary.
  const contentType = response.headers?.get?.("content-type") || "";
  if (!contentType.includes("json")) {
    console.debug(
      `[translation] no override at ${url} (served ${contentType || "no content type"})`,
    );
    return {};
  }
  try {
    return messagePairsIn(await response.json(), url);
  } catch (error) {
    console.warn(`[translation] ${url} is not valid JSON; ignoring it`, error);
    return {};
  }
};

/**
 * The deployment's overrides for a locale, its base language's file layered
 * under its own. Resolves to NO_OVERRIDE when there is nothing to apply, so a
 * caller can skip re-rendering.
 */
export const loadTranslationOverride = async (
  locale,
  fetchImpl = typeof fetch === "function" ? fetch : null,
) => {
  const files = overrideFilesFor(locale);
  if (!files.length || !fetchImpl) {
    return NO_OVERRIDE;
  }
  const loaded = await Promise.all(files.map((url) => loadOne(url, fetchImpl)));
  const merged = Object.assign({}, ...loaded);
  return Object.keys(merged).length ? merged : NO_OVERRIDE;
};

/**
 * The bundled messages with the deployment's on top, key by key.
 *
 * <p>Only the keys the override names are replaced. A file with three keys
 * rewords three strings and leaves the rest of the bundle — including the
 * English fallback layered under every locale — exactly as shipped.
 */
export const applyOverride = (bundled, override) => {
  if (!override || !Object.keys(override).length) {
    return bundled;
  }
  return { ...bundled, ...override };
};
