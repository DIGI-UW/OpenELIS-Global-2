import en from "./en.json";

/**
 * Every locale bundle in this directory, as a loader. Vite reads the glob at
 * build time, so dropping a new Transifex pull (e.g. fr_MG.json) here is all
 * it takes, but each bundle becomes its own chunk that only downloads when a
 * user runs that locale. Loading them all eagerly put the whole directory
 * (about 1 MB per locale, 30 locales) into the first page load, which on a
 * slow link was most of what a user waited for before the login form showed.
 * English stays in the main chunk: it is the fallback for every other locale
 * and the one bundle the app can never wait for, so the glob leaves it out
 * rather than offering a loader for a file the static import already holds.
 */
const bundleLoaders = import.meta.glob(["./*.json", "!./en.json"]);

/**
 * Canonical form for a locale code: BCP 47 casing with hyphens. Accepts the
 * Java/Transifex underscore form and any casing, so the code configured in
 * volume/configuration/backend/locales and the filename Transifex pulls both
 * land on the same key: "fr_MG", "fr-mg" and "FR_mg" all become "fr-MG".
 */
export const normalizeLocaleCode = (code) => {
  if (typeof code !== "string" || !code.trim()) {
    return "";
  }
  const [language, ...rest] = code.trim().replace(/_/g, "-").split("-");
  return [
    language.toLowerCase(),
    ...rest.map((part) =>
      part.length === 2 ? part.toUpperCase() : part.toLowerCase(),
    ),
  ].join("-");
};

/** Loader per canonical code, straight from the file names. */
const loaders = {};
for (const [path, load] of Object.entries(bundleLoaders)) {
  const fileCode = path.replace(/^\.\//, "").replace(/\.json$/, "");
  loaders[normalizeLocaleCode(fileCode)] = load;
}

/** Every locale a bundle ships for, canonical and sorted. */
export const availableLocales = Object.freeze(
  ["en", ...Object.keys(loaders)].sort(),
);

/** Raw bundles in memory, keyed by canonical code. English from the start. */
const rawBundles = { en };
const inFlight = {};

/**
 * The raw bundle for one canonical code: the copy in memory, the download in
 * flight, or a new download. Resolves undefined when no file exists for the
 * code or the download fails (logged once), so a caller always ends up with
 * the best bundle that could be had and never with a rejected promise.
 */
const loadRawBundle = (code) => {
  if (rawBundles[code]) {
    return Promise.resolve(rawBundles[code]);
  }
  const load = loaders[code];
  if (!load) {
    return Promise.resolve(undefined);
  }
  if (!inFlight[code]) {
    inFlight[code] = load().then(
      (mod) => {
        rawBundles[code] = mod.default ?? mod;
        delete inFlight[code];
        return rawBundles[code];
      },
      (error) => {
        delete inFlight[code];
        console.warn(
          `[languages] the ${code} translations could not be loaded; English is shown instead`,
          error,
        );
        return undefined;
      },
    );
  }
  return inFlight[code];
};

/** The codes whose bundles make up a locale's messages: its base, then itself. */
const bundleCodesFor = (code) => {
  const base = code.split("-")[0];
  const codes = base !== code && base !== "en" ? [base, code] : [code];
  return codes.filter((candidate) => candidate !== "en" && loaders[candidate]);
};

/**
 * A bundle layered over its base language and English, so a key a translation
 * has not caught up with yet renders the most specific text available rather
 * than the raw key id: a regional variant (fr-MG) falls back to its base
 * language (fr) first, then to English. New keys are added to en.json only —
 * Transifex is the source of truth for every other bundle — so between an
 * English release and its translation round every locale would otherwise show
 * identifiers like `label.patientHistory.filterByCategory` to the user.
 */
const withFallbacks = (code) => {
  const base = code.split("-")[0];
  return {
    ...en,
    ...(base !== code && base !== "en" ? rawBundles[base] : {}),
    ...rawBundles[code],
  };
};

/**
 * True for a code the Intl APIs accept as a language tag. react-intl throws a
 * RangeError on anything else, so such a code must never reach IntlProvider.
 */
const isLanguageTag = (code) => {
  try {
    return Intl.getCanonicalLocales(code).length === 1;
  } catch {
    return false;
  }
};

/**
 * The messages for a locale code in any spelling, from what is in memory now:
 * the exact bundle layered over its base language and English, or English
 * alone until the bundles arrive. The returned code is the canonical form of
 * what was asked for (valid BCP 47, safe for react-intl and the Intl APIs),
 * so a selector showing the configured locale stays in sync with intl.locale
 * even when the messages had to fall back; a code that is not a language tag
 * at all resolves to English. `ready` says whether every bundle that exists
 * for the code is in memory; `loadMessagesForLocale` makes it so, and a test
 * that renders a locale other than English preloads it the same way. Never
 * returns undefined messages, so a stale localStorage value or an
 * unconfigured navigator.language cannot break startup or a switch.
 */
export const resolveMessagesForLocale = (code) => {
  const canonical = normalizeLocaleCode(code);
  if (!canonical || canonical === "en" || !isLanguageTag(canonical)) {
    return { code: "en", messages: en, ready: true };
  }
  const needed = bundleCodesFor(canonical);
  const ready = needed.every((candidate) => rawBundles[candidate]);
  const messages = needed.length === 0 ? en : withFallbacks(canonical);
  return { code: canonical, messages, ready };
};

/**
 * Downloads the bundles a locale needs (its base language and itself, when
 * they exist) and resolves with the same shape as `resolveMessagesForLocale`,
 * `ready` included. A failed download leaves English in place, so the app
 * renders either way.
 */
export const loadMessagesForLocale = async (code) => {
  const canonical = normalizeLocaleCode(code) || "en";
  await Promise.all(bundleCodesFor(canonical).map(loadRawBundle));
  const resolved = resolveMessagesForLocale(canonical);
  return { ...resolved, ready: true };
};

/**
 * Best label we can produce without the backend: the language's own name for
 * itself. The backend's displayName (from the locales config) wins wherever it
 * is available; this only covers the static fallback list below.
 */
const labelFor = (code) => {
  try {
    const label = new Intl.DisplayNames([code], { type: "language" }).of(code);
    return label ? label.charAt(0).toUpperCase() + label.slice(1) : code;
  } catch {
    return code;
  }
};

/**
 * Default language configuration used when backend is unavailable.
 * The actual enabled languages are fetched from /rest/supportedlocales/active.
 */
export const defaultLanguages = {
  en: { label: "English" },
  fr: { label: "Français" },
};

/**
 * Every shipped locale with a label, for a selector that renders before the
 * backend's list arrives. Derived from the files on disk, never hand-kept.
 * @deprecated Use ConfigurationContext.enabledLanguages for the configured list
 */
export const languages = Object.fromEntries(
  availableLocales.map((code) => [code, { label: labelFor(code) }]),
);

/**
 * Builds the languages object from backend-provided locales, keyed by the
 * canonical code so the header selector, react-intl and the message lookup all
 * agree — the config file may spell a code either way (fr_MG or fr-MG).
 * Falls back to the code itself if displayName is not provided. The messages
 * themselves are not part of the entry: they load when the locale is chosen.
 * @param {Array} supportedLocales - Array of {localeCode, displayName, fallback} from backend
 * @returns {Object} Languages object with {[canonicalCode]: {label, fallback}}
 */
export function buildLanguagesFromConfig(supportedLocales) {
  if (!supportedLocales || supportedLocales.length === 0) {
    return defaultLanguages;
  }

  const result = {};
  for (const locale of supportedLocales) {
    const code = normalizeLocaleCode(locale && locale.localeCode);
    if (!code) {
      continue;
    }
    result[code] = {
      label: locale.displayName || code,
      fallback: locale.fallback || false,
    };
  }

  // Ensure we always have at least one language (English as ultimate fallback)
  if (Object.keys(result).length === 0) {
    return defaultLanguages;
  }

  return result;
}
