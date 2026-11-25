import fs from "fs";
import { fileURLToPath } from "url";
import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

// The locale files the frontend ships, read from the source tree so a new
// Transifex pull is covered without touching this spec. English is part of
// the main bundle and never downloaded on its own.
const LOCALE_CODES = fs
  .readdirSync(
    fileURLToPath(new URL("../../../../src/languages", import.meta.url)),
  )
  .filter((file) => file.endsWith(".json") && file !== "en.json")
  .map((file) => file.replace(/\.json$/, ""));

// A production build serves a locale as `/assets/messages-<code>-<hash>.js`
// (vite.config.ts names the locale chunks apart from the code chunks); the
// Vite dev server serves the JSON module itself.
function bundleCode(url: string): string | undefined {
  return LOCALE_CODES.find((code) =>
    new RegExp(
      `/(?:assets/messages-${code}-[\\w-]+\\.js|src/languages/${code}\\.json)(?:\\?|$)`,
    ).test(url),
  );
}

function recordBundles(page: import("@playwright/test").Page): string[] {
  const codes: string[] = [];
  page.on("request", (request) => {
    const code = bundleCode(request.url());
    if (code) {
      codes.push(code);
    }
  });
  return codes;
}

const ENGLISH_LABEL = "Select Locale";
const FRENCH_LABEL = "Sélectionner la langue";

async function openUserPanel(page: import("@playwright/test").Page) {
  const icon = page.locator("#user-Icon");
  await expect(icon).toBeVisible({ timeout: UI_TIMEOUT });
  if (!(await page.locator("#selector").isVisible())) {
    await icon.click();
  }
  await expect(page.locator("#selector")).toBeVisible({ timeout: UI_TIMEOUT });
}

test.describe("Locale bundles load on demand", () => {
  test("the shell starts with English only and downloads no other locale", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      localStorage.setItem("locale", "en");
    });
    const downloaded = recordBundles(page);

    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    await openUserPanel(page);
    await expect(page.getByText(ENGLISH_LABEL, { exact: true })).toBeVisible();

    expect(downloaded, "no locale bundle for an English shell").toEqual([]);
  });

  test("switching the language downloads that locale once and translates in place", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      localStorage.setItem("locale", "en");
    });
    const downloaded = recordBundles(page);
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    await openUserPanel(page);

    try {
      await page.locator("#selector").selectOption("fr");
      await expect(page.getByText(FRENCH_LABEL, { exact: true })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(downloaded).toEqual(["fr"]);
      expect(await page.evaluate(() => localStorage.getItem("locale"))).toBe(
        "fr",
      );

      await page.locator("#selector").selectOption("en");
      await expect(page.getByText(ENGLISH_LABEL, { exact: true })).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      await page.locator("#selector").selectOption("fr");
      await expect(page.getByText(FRENCH_LABEL, { exact: true })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(downloaded, "a bundle is downloaded once per page").toEqual([
        "fr",
      ]);
    } finally {
      await page.locator("#selector").selectOption("en");
      await expect(page.getByText(ENGLISH_LABEL, { exact: true })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    }
  });

  test("a stored locale is downloaded before the shell renders, and a regional code without a bundle uses its base language", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      localStorage.setItem("locale", "fr-HT");
    });
    const downloaded = recordBundles(page);

    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    await openUserPanel(page);
    await expect(page.getByText(FRENCH_LABEL, { exact: true })).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    expect(downloaded, "the base language only").toEqual(["fr"]);
  });
});
