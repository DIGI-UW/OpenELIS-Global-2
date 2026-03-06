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
 * All available language message bundles.
 * These are bundled at build time and contain UI translations.
 */
export const languageMessages = {
  en: en,
  "en-GB": enGB,
  "en-LK": enLK,
  "en-US": enUS,
  es: es,
  fr: fr,
  id: id,
  mg: mg,
  ro: ro,
  si: si,
  "si-LK": siLK,
  ta: ta,
  "ta-LK": taLK,
  sw: sw,
  "am-ET": amET,
};

/**
 * Default language configuration used when backend is unavailable.
 * The actual enabled languages are fetched from /rest/supportedlocales/active.
 */
export const defaultLanguages = {
  en: { label: "English", messages: en },
  fr: { label: "Français", messages: fr },
};

/**
 * Legacy export for backwards compatibility.
 * Components should migrate to using ConfigurationContext for dynamic locale list.
 * @deprecated Use ConfigurationContext.supportedLocales instead
 */
export const languages = {
  en: { label: "English", messages: en },
  "en-GB": { label: "English (UK)", messages: enGB },
  "en-LK": { label: "English (Sri Lanka)", messages: enLK },
  "en-US": { label: "English (US)", messages: enUS },
  es: { label: "Español", messages: es },
  fr: { label: "Français", messages: fr },
  id: { label: "Indonesia", messages: id },
  mg: { label: "Malagasy", messages: mg },
  ro: { label: "Română", messages: ro },
  si: { label: "සිංහල", messages: si },
  "si-LK": { label: "සිංහල (Sri Lanka)", messages: siLK },
  ta: { label: "தமிழ்", messages: ta },
  "ta-LK": { label: "தமிழ் (Sri Lanka)", messages: taLK },
  sw: { label: "Swahili", messages: sw },
  "am-ET": { label: "Amharic", messages: amET },
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
