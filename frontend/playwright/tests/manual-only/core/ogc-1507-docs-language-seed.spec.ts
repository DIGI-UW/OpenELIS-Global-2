import { test, expect } from "../../../helpers/test-base";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import * as fs from "fs";
import * as path from "path";

/**
 * OGC-1507 slice G: prepares a stack for user-manual screenshots in one
 * language. It switches the browser to DOCS_LANG (default fr), reports one NCE
 * per shipped NCE category through the Report NCE form, and leaves them for the
 * NCE register and dashboard captures. It then says which shipped NCE labels
 * still show in English, so the capture is not taken before Transifex has them.
 *
 *   DOCS_LANG=fr BASE_URL=https://... npm run pw:test -- --project=core-live-uat \
 *     ogc-1507-docs-language-seed
 *
 * Operator-run only (manual-only bucket): it writes NCE records on the target.
 */

const LANG = process.env.DOCS_LANG || "fr";

/** The screen's label for a key in the documentation language. */
function label(key: string): string {
  const read = (language: string): Record<string, string> => {
    const file = path.join(process.cwd(), "src/languages", `${language}.json`);
    return fs.existsSync(file)
      ? JSON.parse(fs.readFileSync(file, "utf-8"))
      : {};
  };
  return read(LANG)[key] || read("en")[key];
}

interface NceCategory {
  id: string;
  name: string;
  displayKey?: string;
  types: { id: string; name: string; displayKey?: string }[];
}

async function categories(
  request: import("@playwright/test").APIRequestContext,
  language: string,
): Promise<NceCategory[]> {
  const response = await request.get(`${API_PREFIX}/rest/nce/categories`, {
    headers: { "Accept-Language": language },
  });
  expect(response.status(), "NCE categories").toBe(200);
  return (await response.json()) as NceCategory[];
}

test("seed one NCE per shipped category in the documentation language", async ({
  page,
}) => {
  test.setTimeout(300_000);
  const translated = await categories(page.request, LANG);
  const english = await categories(page.request, "en");
  const shipped = translated.filter((category) => category.displayKey);
  expect(shipped.length, "shipped NCE categories").toBeGreaterThan(0);

  const untranslated: string[] = [];
  translated.forEach((category, i) => {
    if (LANG !== "en" && category.name === english[i]?.name) {
      untranslated.push(category.name);
    }
    category.types.forEach((type, j) => {
      if (LANG !== "en" && type.name === english[i]?.types[j]?.name) {
        untranslated.push(type.name);
      }
    });
  });

  await page.addInitScript((language) => {
    localStorage.setItem("locale", language);
  }, LANG);

  for (const category of shipped.filter((c) => c.types.length > 0)) {
    await page.goto("/ReportNonConformingEvent", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const categorySelect = page.locator("#nce-category");
    await expect(
      categorySelect.locator("option", { hasText: category.name }),
    ).toHaveCount(1, { timeout: UI_TIMEOUT });
    await page.locator("#reporting-unit").selectOption({ index: 1 });
    await categorySelect.selectOption(category.id);
    await page.locator("#nce-type").selectOption(category.types[0].id);
    await page.getByTestId("nce-severity-MINOR").click();
    await page
      .locator("#nce-title")
      .fill(`${category.name}: ${category.types[0].name}`);
    await page.locator("#nce-description").fill(category.types[0].name);
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/reportnonconformingevent") &&
        response.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: label("nce.button.submit") })
      .click();
    expect((await saved).status(), `NCE for ${category.name}`).toBe(200);
  }

  test.info().annotations.push({
    type: "untranslated",
    description: untranslated.length
      ? `Still English in ${LANG}: ${untranslated.join(", ")}`
      : `Every shipped NCE label shows in ${LANG}`,
  });
});
