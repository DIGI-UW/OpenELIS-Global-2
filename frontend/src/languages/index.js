import en from "./en.json";
import enGB from "./en_GB.json";
import enLK from "./en_LK.json";
import enUS from "./en_US.json";
import es from "./es.json";
import fr from "./fr.json";
import id from "./id.json";
import idID from "./id_ID.json";
import mg from "./mg.json";
import ro from "./ro.json";
import si from "./si.json";
import siLK from "./si_LK.json";
import ta from "./ta.json";
import taLK from "./ta_LK.json";

export const languages = {
  en: { label: "English", messages: en },
  "en-GB": { label: "English (UK)", messages: enGB },
  "en-LK": { label: "English (Sri Lanka)", messages: enLK },
  "en-US": { label: "English (US)", messages: enUS },
  es: { label: "Español", messages: es },
  fr: { label: "Français", messages: fr },
  id: { label: "Indonesia", messages: id },
  "id-ID": { label: "Indonesia (ID)", messages: idID },
  mg: { label: "Malagasy", messages: mg },
  ro: { label: "Română", messages: ro },
  si: { label: "සිංහල", messages: si }, // Sinhala
  "si-LK": { label: "සිංහල (Sri Lanka)", messages: siLK },
  ta: { label: "தமிழ்", messages: ta }, // Tamil
  "ta-LK": { label: "தமிழ் (Sri Lanka)", messages: taLK },
};
