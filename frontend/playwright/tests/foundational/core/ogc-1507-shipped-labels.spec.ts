import { test, expect, type Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1507: an NCE type OpenELIS ships ("Documentation error", linked to the
 * Transifex key nce.type.documentation_error at start) shows in the user's
 * language on the Report NCE form; an administrator's French override in
 * Translation Management shows for French users only and returns to the
 * shipped translation on Reset; and an event that concerns no sample is
 * reported from the form.
 */

const KEY = "nce.type.documentation_error";
const FRENCH = "Erreur de documentation";
const ENGLISH = "Documentation error";

interface NceCategory {
  id: string;
  name: string;
  types: { id: string; name: string; displayKey?: string }[];
}

async function shippedType(page: Page, language: string) {
  const response = await page.request.get(`${API_PREFIX}/rest/nce/categories`, {
    headers: { "Accept-Language": language },
  });
  expect(response.status(), "NCE categories").toBe(200);
  for (const category of (await response.json()) as NceCategory[]) {
    const type = category.types.find((t) => t.displayKey === KEY);
    if (type) {
      return { category, type };
    }
  }
  throw new Error(`no NCE type carries ${KEY}`);
}

async function typeOptions(page: Page, language: string, categoryId: string) {
  await page.addInitScript((locale) => {
    localStorage.setItem("locale", locale);
  }, language);
  await page.goto("/ReportNonConformingEvent", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  const category = page.locator("#nce-category");
  await expect(category.locator(`option[value="${categoryId}"]`)).toHaveCount(
    1,
    { timeout: UI_TIMEOUT },
  );
  await category.selectOption(categoryId);
  return page.locator("#nce-type option");
}

async function localizationId(page: Page): Promise<string> {
  const response = await page.request.get(
    `${API_PREFIX}/rest/localizations?description=${encodeURIComponent(
      `NCE Type: ${ENGLISH}`,
    )}`,
  );
  expect(response.status()).toBe(200);
  const rows = (await response.json()) as { id: string; displayKey?: string }[];
  const row = rows.find((r) => r.displayKey === KEY);
  expect(row, "the shipped type's localization").toBeTruthy();
  return row!.id;
}

test("a shipped NCE type follows the user's language and the lab's override", async ({
  page,
}) => {
  test.setTimeout(120_000);
  const id = await localizationId(page);
  const resetFrench = async () => {
    const reset = await page.request.post(
      `${API_PREFIX}/rest/localizations/${id}/translations/fr/reset`,
      { headers: { "X-CSRF-Token": await csrfToken(page) }, data: {} },
    );
    expect(reset.status(), "reset to the shipped translation").toBe(200);
  };
  await resetFrench();
  const french = await shippedType(page, "fr-FR");
  const english = await shippedType(page, "en-US");
  expect(french.type.name).toBe(FRENCH);
  expect(english.type.name).toBe(ENGLISH);

  try {
    await test.step("the form lists it in French", async () => {
      const options = await typeOptions(page, "fr", french.category.id);
      await expect(options.filter({ hasText: FRENCH })).toHaveCount(1);
    });

    await test.step("a French override shows for French users only", async () => {
      const saved = await page.request.put(
        `${API_PREFIX}/rest/localizations/${id}/translations`,
        {
          headers: { "X-CSRF-Token": await csrfToken(page) },
          data: { fr: "Documentation incomplète" },
        },
      );
      expect(saved.status()).toBe(200);
      expect((await saved.json()).origins.fr).toBe("SITE_OVERRIDE");
      expect((await shippedType(page, "fr-FR")).type.name).toBe(
        "Documentation incomplète",
      );
      expect((await shippedType(page, "en-US")).type.name).toBe(ENGLISH);

      const blank = await page.request.put(
        `${API_PREFIX}/rest/localizations/${id}/translations`,
        {
          headers: { "X-CSRF-Token": await csrfToken(page) },
          data: { fr: " " },
        },
      );
      expect(blank.status(), "a blank override is refused").toBe(400);
    });

    await test.step("Translation Management resets it to the shipped translation", async () => {
      await page.addInitScript(() => {
        localStorage.setItem("locale", "en");
      });
      await page.goto("/MasterListsPage/translationManagement", {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      await page.locator("#locale-select").selectOption("fr");
      const search = page.getByRole("searchbox").first();
      await search.fill(`NCE Type: ${ENGLISH}`);
      const row = page
        .locator("tbody tr")
        .filter({ hasText: `NCE Type: ${ENGLISH}` });
      await expect(row.first()).toContainText("Site override", {
        timeout: UI_TIMEOUT,
      });
      await row.first().getByRole("button", { name: "Edit" }).click();
      const frenchRow = page.getByTestId("translation-row-fr");
      await frenchRow
        .getByRole("button", { name: "Reset to shipped translation" })
        .click();
      const reset = page.waitForResponse(
        (response) =>
          response
            .url()
            .includes(`/rest/localizations/${id}/translations/fr/reset`) &&
          response.request().method() === "POST",
      );
      await frenchRow
        .getByRole("button", { name: "Reset", exact: true })
        .click();
      expect((await reset).status()).toBe(200);
      await expect(frenchRow.getByText("Shipped", { exact: true })).toBeVisible(
        { timeout: UI_TIMEOUT },
      );
      await expect(
        frenchRow.getByText("Site override", { exact: true }),
      ).toHaveCount(0);
      await expect(page.locator("#translation-fr")).toHaveValue(FRENCH);
      expect((await shippedType(page, "fr-FR")).type.name).toBe(FRENCH);
    });
  } finally {
    await resetFrench();
  }
});

test("an event that concerns no sample is reported from the form", async ({
  page,
}) => {
  const { category, type } = await shippedType(page, "en-US");
  await page.goto("/ReportNonConformingEvent", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.locator(`#nce-category option[value="${category.id}"]`),
  ).toHaveCount(1, { timeout: UI_TIMEOUT });
  await page.locator("#reporting-unit").selectOption({ index: 1 });
  await page.locator("#nce-category").selectOption(category.id);
  await page.locator("#nce-type").selectOption(type.id);
  const minor = page.getByTestId("nce-severity-MINOR");
  await minor.focus();
  await page.keyboard.press("Space");
  await expect(minor).toHaveAttribute("aria-checked", "true");
  await page.locator("#nce-title").fill("OGC-1507 event with no sample");
  await page.locator("#nce-description").fill("Logbook page missing.");
  const saved = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/reportnonconformingevent") &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Submit NCE" }).click();
  expect((await saved).status()).toBe(200);
  await expect(page.getByText("Save was successful")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
});
