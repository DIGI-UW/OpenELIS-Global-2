import en from "./en.json";

/**
 * Every locale bundle in this directory, loaded at build time. Dropping a new
 * Transifex pull (e.g. fr_MG.json) here is all it takes — no manual import or
 * registry edit. Vite statically analyzes the glob, so the bundles are part of
 * the build exactly as the hand-written imports were.
 */
const bundleModules = import.meta.glob("./*.json", { eager: true });

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

/** Raw bundles keyed by canonical code, straight from the files. */
const rawBundles = {};
for (const [path, mod] of Object.entries(bundleModules)) {
  const fileCode = path.replace(/^\.\//, "").replace(/\.json$/, "");
  rawBundles[normalizeLocaleCode(fileCode)] = mod.default ?? mod;
}

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
 * All available language message bundles, keyed by canonical locale code.
 * These are bundled at build time and contain UI translations.
 */
export const languageMessages = Object.fromEntries(
  Object.keys(rawBundles).map((code) => [
    code,
    code === "en" ? en : withFallbacks(code),
  ]),
);

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
  en: { label: "English", messages: languageMessages.en },
  fr: { label: "Français", messages: languageMessages.fr },
};

/**
 * Legacy export for backwards compatibility — now derived from whatever
 * bundles exist instead of a hand-maintained list.
 * Components should migrate to using ConfigurationContext for dynamic locale list.
 * @deprecated Use ConfigurationContext.supportedLocales instead
 */
export const languages = Object.fromEntries(
  Object.keys(languageMessages)
    .sort()
    .map((code) => [
      code,
      { label: labelFor(code), messages: languageMessages[code] },
    ]),
);

/**
 * The message bundle for a locale code in any spelling: the exact bundle
 * first, then the base language's, then English. The returned code is the
 * canonical form of what was asked for (valid BCP 47, safe for react-intl and
 * the Intl APIs), so a selector showing the configured locale stays in sync
 * with intl.locale even when the messages had to fall back. Never returns
 * undefined messages, so a stale localStorage value or an unconfigured
 * navigator.language cannot break startup or a switch.
 */
export const resolveMessagesForLocale = (code) => {
  const canonical = normalizeLocaleCode(code);
  if (!canonical) {
    return { code: "en", messages: languageMessages.en };
  }
  const messages =
    languageMessages[canonical] ||
    languageMessages[canonical.split("-")[0]] ||
    languageMessages.en;
  return { code: canonical, messages };
};

/**
 * Builds the languages object from backend-provided locales, keyed by the
 * canonical code so the header selector, react-intl and the message lookup all
 * agree — the config file may spell a code either way (fr_MG or fr-MG).
 * Falls back to the code itself if displayName is not provided, and to the
 * base language's (then English) messages if no bundle exists for a
 * configured locale.
 * @param {Array} supportedLocales - Array of {localeCode, displayName, fallback} from backend
 * @returns {Object} Languages object with {[canonicalCode]: {label, messages, fallback}}
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
      messages: resolveMessagesForLocale(code).messages,
      fallback: locale.fallback || false,
    };
  }

  // Ensure we always have at least one language (English as ultimate fallback)
  if (Object.keys(result).length === 0) {
    return defaultLanguages;
  }

  return result;
}
