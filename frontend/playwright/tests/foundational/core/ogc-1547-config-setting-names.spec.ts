import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

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

// Result settings are all named, so a camelCase key is caught there too.
const RESULT_RAW_KEY = /^\S+\.\S+$|^[a-z]+[A-Z][A-Za-z]*$/;

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
      let names: string[];
      if (menu === "ResultConfigurationMenu") {
        // OGC-1364 v3: switches and fields grouped by purpose, no table.
        const settings = page.getByTestId("result-validation-settings");
        await expect(settings).toBeVisible({ timeout: LONG_TIMEOUT });
        names = await settings
          .locator(".cds--toggle__label-text, .cds--label")
          .allInnerTexts();
      } else {
        const rows = page.locator("tbody tr");
        await expect(rows.first()).toBeVisible({ timeout: LONG_TIMEOUT });
        await page
          .getByRole("combobox", { name: /Items per page/i })
          .selectOption("50");
        names = await rows.locator("td:nth-child(2)").allInnerTexts();
      }
      expect(names.length).toBeGreaterThan(0);
      const rawKey =
        menu === "ResultConfigurationMenu" ? RESULT_RAW_KEY : RAW_KEY;
      const raw = names
        .map((name) => name.trim())
        .filter((name) => rawKey.test(name));
      expect(raw, `settings on ${menu} listed by their raw key`).toEqual([]);
    });
  }

  test("a pathology identifier setting shows its name and description", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/ResultConfigurationMenu", {
      waitUntil: "domcontentloaded",
    });
    const partScheme = page
      .getByTestId("settings-group-pathology")
      .locator('[data-testid^="setting-"]', {
        hasText: "Pathology: part designation",
      });
    await expect(partScheme).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(partScheme).toContainText("How specimen parts are designated");
  });
});
