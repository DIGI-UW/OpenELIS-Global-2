import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1547 — Result Entry Configuration listed the pathology identifier
 * settings by their raw keys ("pathology.identifier.partScheme"), and the
 * sibling configuration screens did the same for the sample acceptance
 * checklist and freezer monitoring settings. They now read as names an
 * administrator understands.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1547-config-setting-names
 */

const RAW_KEY = /^\S+\.\S+$/;

const MENUS = [
  "ResultConfigurationMenu",
  "SampleEntryConfigurationMenu",
  "SiteInformationMenu",
];

test.describe("OGC-1547 configuration setting names", () => {
  for (const menu of MENUS) {
    test(`${menu} lists no setting by its raw key`, async ({ page }) => {
      await page.goto(`/MasterListsPage/${menu}`, {
        waitUntil: "domcontentloaded",
      });
      const rows = page.locator("tbody tr");
      await expect(rows.first()).toBeVisible({ timeout: LONG_TIMEOUT });
      await page
        .getByRole("combobox", { name: /Items per page/i })
        .selectOption("50");

      const names = await rows.locator("td:nth-child(2)").allInnerTexts();
      expect(names.length).toBeGreaterThan(0);
      const raw = names
        .map((name) => name.trim())
        .filter((name) => RAW_KEY.test(name));
      expect(raw, `settings on ${menu} listed by their raw key`).toEqual([]);
    });
  }

  test("a pathology identifier setting shows its name and description", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/ResultConfigurationMenu", {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("tbody tr").first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await page
      .getByRole("combobox", { name: /Items per page/i })
      .selectOption("50");

    const partScheme = page.getByRole("row", {
      name: /Pathology: part designation/,
    });
    await expect(partScheme).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(partScheme).toContainText("How specimen parts are designated");
  });
});
