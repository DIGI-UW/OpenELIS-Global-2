import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Clinical result and validation parity migration", () => {
  test("loads accession-based result search shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-031",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 accession results shell parity smoke",
    });

    await gotoAndWait("/AccessionResults");
    await ensureAuthenticatedShell();

    await expect(page.locator("#searchResults")).toBeVisible();
  });

  test("loads accession validation search shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-047",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 accession validation shell parity smoke",
    });

    await gotoAndWait("/AccessionValidation");
    await ensureAuthenticatedShell();

    await expect(page.locator("#accessionNumber")).toBeVisible();
    await expect(page.locator('[data-testid="Search-btn"]')).toBeVisible();
  });
});
