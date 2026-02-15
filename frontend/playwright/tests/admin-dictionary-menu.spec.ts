import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin dictionary menu parity migration", () => {
  test("loads dictionary page and opens add dictionary modal", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-004",
      riskTier: "P1",
      domain: "admin",
      notes: "Dictionary management shell and add modal parity smoke",
    });

    await gotoAndWait("/MasterListsPage/DictionaryMenu");
    await ensureAuthenticatedShell();

    await expect(page.locator('[data-cy="addButton"]')).toBeVisible();
    await expect(page.locator('[data-cy="modifyButton"]')).toBeVisible();
    await expect(page.locator('[data-cy="deactivateButton"]')).toBeVisible();
    await expect(page.locator("#dictionary-entry-search")).toBeVisible();

    await page.locator('[data-cy="addButton"]').click();
    await expect(page.locator("#dictNumber")).toBeVisible();
    await expect(page.locator("#description")).toBeVisible();
    await expect(page.locator("#dictEntry")).toBeVisible();
    await expect(page.locator("#localAbbrev")).toBeVisible();
    await expect(page.locator("#loincCode")).toBeVisible();
    await page.getByRole("button", { name: "Cancel" }).click();
  });
});
