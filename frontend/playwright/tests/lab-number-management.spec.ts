import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Lab number management parity migration", () => {
  test("loads management page and toggles format controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-023",
      riskTier: "P1",
      domain: "core",
      notes:
        "Lab number management form visibility and format toggle parity smoke",
    });

    await gotoAndWait("/MasterListsPage/labNumber");
    await ensureAuthenticatedShell();

    await expect(
      page.getByRole("heading", { name: /Lab Number Management/i }),
    ).toBeVisible();
    await expect(page.locator("#lab_number_type")).toBeVisible();
    await expect(page.getByTestId("submit-button")).toBeVisible();

    await page.locator("#lab_number_type").selectOption("ALPHANUM");
    await expect(page.locator("#alphanumPrefix")).toBeVisible();
    await expect(page.locator("#usePrefix")).toBeVisible();

    await page.locator("#lab_number_type").selectOption("SITEYEARNUM");
    await expect(page.locator("#alphanumPrefix")).toBeHidden();
  });
});
