import fs from "fs";
import path from "path";
import {
  availableLocales,
  languages,
  normalizeLocaleCode,
  resolveMessagesForLocale,
  loadMessagesForLocale,
  buildLanguagesFromConfig,
  defaultLanguages,
} from "../index";

/**
 * The languages registry is derived from the directory, not hand-maintained,
 * and a bundle only downloads when a user runs its locale.
 *
 * Before this, every Transifex pull had to be wired into index.js by hand and
 * a dozen bundles (fr_MG, en_US, ar, de, …) sat on disk unreachable: a locale
 * configured in volume/configuration/backend/locales silently rendered
 * English. Then every bundle shipped in the first page load (about 1 MB per
 * locale), which on a slow link was most of the wait before the login form.
 */

const languagesDir = path.join(__dirname, "..");
const bundleFilesOnDisk = fs
  .readdirSync(languagesDir)
  .filter((f) => f.endsWith(".json"));
const readBundle = (file) =>
  JSON.parse(fs.readFileSync(path.join(languagesDir, file), "utf8"));
const en = readBundle("en.json");
const fr = readBundle("fr.json");
const frMG = readBundle("fr_MG.json");

describe("dynamic bundle registry", () => {
  it("lists every locale file in the directory", () => {
    for (const file of bundleFilesOnDisk) {
      const code = normalizeLocaleCode(file.replace(/\.json$/, ""));
      expect(
        availableLocales.includes(code),
        `${file} is on disk but missing from availableLocales — the registry must not need manual wiring`,
      ).toBe(true);
    }
  });

  it("lists nothing that is not a file (no phantom locales)", () => {
    const codesFromDisk = new Set(
      bundleFilesOnDisk.map((f) =>
        normalizeLocaleCode(f.replace(/\.json$/, "")),
      ),
    );
    for (const code of availableLocales) {
      expect(codesFromDisk.has(code), `${code} has no backing file`).toBe(true);
    }
  });

  it("keeps the legacy languages export usable for every bundle", () => {
    expect(Object.keys(languages)).toEqual([...availableLocales]);
    for (const entry of Object.values(languages)) {
      expect(entry.label && typeof entry.label === "string").toBe(true);
    }
  });
});

describe("normalizeLocaleCode", () => {
  it("maps the Java/Transifex underscore form onto BCP 47", () => {
    expect(normalizeLocaleCode("fr_MG")).toBe("fr-MG");
    expect(normalizeLocaleCode("FR_mg")).toBe("fr-MG");
    expect(normalizeLocaleCode("fr-mg")).toBe("fr-MG");
    expect(normalizeLocaleCode("en")).toBe("en");
    expect(normalizeLocaleCode(" en_US ")).toBe("en-US");
  });

  it("is safe on garbage", () => {
    expect(normalizeLocaleCode("")).toBe("");
    expect(normalizeLocaleCode(null)).toBe("");
    expect(normalizeLocaleCode(undefined)).toBe("");
  });
});

describe("resolveMessagesForLocale before a bundle is loaded", () => {
  it("answers English at once for English and for garbage", () => {
    expect(resolveMessagesForLocale("en")).toEqual({
      code: "en",
      messages: en,
      ready: true,
    });
    expect(resolveMessagesForLocale("").code).toBe("en");
    expect(resolveMessagesForLocale(undefined).ready).toBe(true);
  });

  it("answers English for an unknown language, and is ready", () => {
    const { code, messages, ready } = resolveMessagesForLocale("xx");
    expect(code).toBe("xx");
    expect(messages).toEqual(en);
    expect(ready).toBe(true);
  });

  it("resolves a stored value that is not a language tag to English itself", () => {
    // IntlProvider throws on such a tag, so it must never be handed over.
    for (const garbage of ["zz-??", "fr-", "en_US_", "a-b-c-d-e-f-g-h-i"]) {
      expect(resolveMessagesForLocale(garbage), garbage).toEqual({
        code: "en",
        messages: en,
        ready: true,
      });
    }
    expect(resolveMessagesForLocale("fr-HT").code).toBe("fr-HT");
  });

  it("answers English for a shipped locale not yet loaded, and is not ready", () => {
    // de ships but nothing in this file loads it, so this stays the cold case.
    const { code, messages, ready } = resolveMessagesForLocale("de");
    expect(code).toBe("de");
    expect(messages).toEqual(en);
    expect(ready).toBe(false);
  });
});

describe("loadMessagesForLocale", () => {
  it("matches a config-file code to its Transifex bundle whatever the spelling", async () => {
    const translatedKey = Object.keys(frMG).find((k) => frMG[k]);
    expect(translatedKey, "fr_MG.json should not be empty").toBeTruthy();
    for (const spelling of ["fr_MG", "fr-MG", "FR_mg"]) {
      const { code, messages, ready } = await loadMessagesForLocale(spelling);
      expect(code).toBe("fr-MG");
      expect(ready).toBe(true);
      expect(messages[translatedKey]).toBe(frMG[translatedKey]);
    }
  });

  it("layers a regional variant over its base language before English", async () => {
    // Precedence contract for every key: the variant's own value wins, a key
    // it lacks takes the base language's, and only then English. Asserted
    // across all fr keys so it holds whether Transifex pulls fr_MG sparse or
    // complete.
    const { messages } = await loadMessagesForLocale("fr_MG");
    for (const key of Object.keys(fr)) {
      const expected = key in frMG ? frMG[key] : fr[key];
      expect(messages[key], key).toBe(expected);
    }
    const enOnlyKey = Object.keys(en).find(
      (key) => !(key in fr) && !(key in frMG),
    );
    if (enOnlyKey) {
      expect(messages[enOnlyKey]).toBe(en[enOnlyKey]);
    }
  });

  it("covers every English key so no locale renders raw message ids", async () => {
    const { messages } = await loadMessagesForLocale("fr_MG");
    for (const key of Object.keys(en)) {
      expect(key in messages, `missing ${key}`).toBe(true);
    }
  });

  it("falls back to the base language for an unbundled regional code", async () => {
    const { code, messages, ready } = await loadMessagesForLocale("fr_XX");
    expect(code).toBe("fr-XX");
    expect(ready).toBe(true);
    const frKey = Object.keys(fr).find((k) => fr[k] && fr[k] !== en[k]);
    expect(messages[frKey]).toBe(fr[frKey]);
  });

  it("falls back to English for an unknown language and for garbage", async () => {
    expect((await loadMessagesForLocale("xx")).messages).toEqual(en);
    expect((await loadMessagesForLocale("")).code).toBe("en");
    expect((await loadMessagesForLocale(undefined)).messages).toEqual(en);
  });

  it("makes the synchronous lookup ready once the bundles are in memory", async () => {
    await loadMessagesForLocale("fr-MG");
    const { messages, ready } = resolveMessagesForLocale("fr_MG");
    expect(ready).toBe(true);
    const translatedKey = Object.keys(frMG).find((k) => frMG[k]);
    expect(messages[translatedKey]).toBe(frMG[translatedKey]);
    expect(resolveMessagesForLocale("fr").ready).toBe(true);
  });
});

describe("buildLanguagesFromConfig", () => {
  it("keys the result by canonical code so selector, intl and lookup agree", () => {
    const built = buildLanguagesFromConfig([
      { localeCode: "fr_MG", displayName: "French Madagascar" },
      { localeCode: "en", displayName: "English", fallback: true },
    ]);
    expect(Object.keys(built)).toEqual(["fr-MG", "en"]);
    expect(built["fr-MG"].label).toBe("French Madagascar");
    expect(built["en"].fallback).toBe(true);
  });

  it("keeps a configured locale selectable even without a bundle", () => {
    // "xx" is not a real language, so Transifex will never add a bundle for it.
    expect(availableLocales.includes("xx")).toBe(false);
    const built = buildLanguagesFromConfig([
      { localeCode: "xx", displayName: "Unbundled" },
    ]);
    expect(built["xx"].label).toBe("Unbundled");
  });

  it("skips rows without a code and falls back to defaults when empty", () => {
    expect(buildLanguagesFromConfig([])).toBe(defaultLanguages);
    expect(buildLanguagesFromConfig(null)).toBe(defaultLanguages);
    const built = buildLanguagesFromConfig([
      { localeCode: "", displayName: "nameless" },
      { localeCode: "fr", displayName: "Français" },
    ]);
    expect(Object.keys(built)).toEqual(["fr"]);
  });
});
