import fs from "fs";
import en from "../en.json";
import path from "path";
import {
  availableLocaleCodes,
  languages,
  normalizeLocaleCode,
  resolveMessagesForLocale,
  buildLanguagesFromConfig,
  defaultLanguages,
} from "../index";

/**
 * The languages registry is derived from the directory, not hand-maintained.
 *
 * Before this, every Transifex pull had to be wired into index.js by hand and
 * a dozen bundles (fr_MG, en_US, ar, de, …) sat on disk unreachable: a locale
 * configured in volume/configuration/backend/locales silently rendered
 * English. The registry now loads every *.json in src/languages and any
 * spelling of a code (fr_MG / fr-MG / FR_mg) lands on the same bundle.
 */

const languagesDir = path.join(__dirname, "..");
const bundleFilesOnDisk = fs
  .readdirSync(languagesDir)
  .filter((f) => f.endsWith(".json"));

describe("dynamic bundle loading", () => {
  it("registers every locale file in the directory", () => {
    for (const file of bundleFilesOnDisk) {
      const code = normalizeLocaleCode(file.replace(/\.json$/, ""));
      expect(
        availableLocaleCodes.includes(code),
        `${file} is on disk but missing from the registry — it must not need manual wiring`,
      ).toBe(true);
    }
  });

  it("registers nothing that is not a file (no phantom locales)", () => {
    const codesFromDisk = new Set(
      bundleFilesOnDisk.map((f) =>
        normalizeLocaleCode(f.replace(/\.json$/, "")),
      ),
    );
    for (const code of availableLocaleCodes) {
      expect(codesFromDisk.has(code), `${code} has no backing file`).toBe(true);
    }
  });

  it("keeps the legacy languages export usable for every bundle", () => {
    for (const [code, entry] of Object.entries(languages)) {
      expect(entry.label && typeof entry.label === "string").toBe(true);
      expect(availableLocaleCodes).toContain(code);
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

describe("resolveMessagesForLocale", () => {
  const en = JSON.parse(
    fs.readFileSync(path.join(languagesDir, "en.json"), "utf8"),
  );
  const fr = JSON.parse(
    fs.readFileSync(path.join(languagesDir, "fr.json"), "utf8"),
  );
  const frMG = JSON.parse(
    fs.readFileSync(path.join(languagesDir, "fr_MG.json"), "utf8"),
  );

  it("matches a config-file code to its Transifex bundle whatever the spelling", async () => {
    const translatedKey = Object.keys(frMG).find((k) => frMG[k]);
    expect(translatedKey, "fr_MG.json should not be empty").toBeTruthy();
    for (const spelling of ["fr_MG", "fr-MG", "FR_mg"]) {
      const { code, messages } = await resolveMessagesForLocale(spelling);
      expect(code).toBe("fr-MG");
      expect(messages[translatedKey]).toBe(frMG[translatedKey]);
    }
  });

  it("layers a regional variant over its base language before English", async () => {
    // Precedence contract for every key: the variant's own value wins, a key
    // it lacks takes the base language's, and only then English. Asserted
    // across all fr keys so it holds whether Transifex pulls fr_MG sparse or
    // complete.
    const { messages } = await resolveMessagesForLocale("fr_MG");
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
    const { messages } = await resolveMessagesForLocale("fr_MG");
    for (const key of Object.keys(en)) {
      expect(key in messages, `missing ${key}`).toBe(true);
    }
  });

  it("falls back to the base language for an unbundled regional code", async () => {
    const { code, messages } = await resolveMessagesForLocale("fr_XX");
    expect(code).toBe("fr-XX");
    const frKey = Object.keys(fr).find((k) => fr[k] && fr[k] !== en[k]);
    expect(messages[frKey]).toBe(fr[frKey]);
  });

  it("falls back to English for an unknown language and for garbage", async () => {
    expect((await resolveMessagesForLocale("xx")).messages).toStrictEqual(en);
    expect((await resolveMessagesForLocale("")).code).toBe("en");
    expect((await resolveMessagesForLocale(undefined)).messages).toStrictEqual(
      en,
    );
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

  it("keeps a configured locale selectable even without a bundle", async () => {
    const built = buildLanguagesFromConfig([
      { localeCode: "pt", displayName: "Português" },
    ]);
    expect(built["pt"].label).toBe("Português");
    // No bundle on disk, so selecting it resolves to English rather than
    // leaving the UI on raw message ids.
    expect((await resolveMessagesForLocale("pt")).messages).toStrictEqual(en);
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
