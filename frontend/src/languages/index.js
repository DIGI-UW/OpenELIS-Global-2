import en from "./en.json";
import enGB from "./en_GB.json";
import enLK from "./en_LK.json";
import enUS from "./en_US.json";
import es from "./es.json";
import fr from "./fr.json";
import id from "./id.json";
import mg from "./mg.json";
import ro from "./ro.json";
import si from "./si.json";
import siLK from "./si_LK.json";
import ta from "./ta.json";
import taLK from "./ta_LK.json";
import amET from "./am_ET.json";
import sw from "./sw.json";

/**
 * A bundle layered over English, so a key a translation has not caught up with
 * yet renders its English text rather than the raw key id. New keys are added
 * to en.json only — Transifex is the source of truth for every other bundle —
 * so between an English release and its translation round every locale would
 * otherwise show identifiers like `label.patientHistory.filterByCategory` to
 * the user.
 */
const withEnglishFallback = (messages) => ({ ...en, ...messages });

/**
 * All available language message bundles.
 * These are bundled at build time and contain UI translations.
 */
export const languageMessages = {
  en: en,
  "en-GB": withEnglishFallback(enGB),
  "en-LK": withEnglishFallback(enLK),
  "en-US": withEnglishFallback(enUS),
  es: withEnglishFallback(es),
  fr: withEnglishFallback(fr),
  id: withEnglishFallback(id),
  mg: withEnglishFallback(mg),
  ro: withEnglishFallback(ro),
  si: withEnglishFallback(si),
  "si-LK": withEnglishFallback(siLK),
  ta: withEnglishFallback(ta),
  "ta-LK": withEnglishFallback(taLK),
  sw: withEnglishFallback(sw),
  "am-ET": withEnglishFallback(amET),
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
 * Legacy export for backwards compatibility.
 * Components should migrate to using ConfigurationContext for dynamic locale list.
 * @deprecated Use ConfigurationContext.supportedLocales instead
 */
export const languages = {
  en: { label: "English", messages: languageMessages["en"] },
  "en-GB": { label: "English (UK)", messages: languageMessages["en-GB"] },
  "en-LK": {
    label: "English (Sri Lanka)",
    messages: languageMessages["en-LK"],
  },
  "en-US": { label: "English (US)", messages: languageMessages["en-US"] },
  es: { label: "Español", messages: languageMessages["es"] },
  fr: { label: "Français", messages: languageMessages["fr"] },
  id: { label: "Indonesia", messages: languageMessages["id"] },
  mg: { label: "Malagasy", messages: languageMessages["mg"] },
  ro: { label: "Română", messages: languageMessages["ro"] },
  si: { label: "සිංහල", messages: languageMessages["si"] },
  "si-LK": { label: "සිංහල (Sri Lanka)", messages: languageMessages["si-LK"] },
  ta: { label: "தமிழ்", messages: languageMessages["ta"] },
  "ta-LK": { label: "தமிழ் (Sri Lanka)", messages: languageMessages["ta-LK"] },
  sw: { label: "Swahili", messages: languageMessages["sw"] },
  "am-ET": { label: "Amharic", messages: languageMessages["am-ET"] },
};

/**
 * Builds the languages object from backend-provided locales.
 * Falls back to default label if displayName not provided.
 * Falls back to English messages if no message bundle exists for a configured locale.
 * @param {Array} supportedLocales - Array of {localeCode, displayName, fallback} from backend
 * @returns {Object} Languages object with {[localeCode]: {label, messages, fallback}}
 */
export function buildLanguagesFromConfig(supportedLocales) {
  if (!supportedLocales || supportedLocales.length === 0) {
    return defaultLanguages;
  }

  const result = {};
  for (const locale of supportedLocales) {
    const code = locale.localeCode;
    const messages = languageMessages[code] || languageMessages["en"];

    result[code] = {
      label: locale.displayName || code,
      messages: messages,
      fallback: locale.fallback || false,
    };
  }

  // Ensure we always have at least one language (English as ultimate fallback)
  if (Object.keys(result).length === 0) {
    return defaultLanguages;
  }

  return result;
}
