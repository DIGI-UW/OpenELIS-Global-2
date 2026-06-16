import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

/**
 * OGC-949 — the four remaining Test Catalog editor sections (US9–US12).
 * M9–M12 video proof.
 *
 * One walkthrough of the sections that complete the v1 editor:
 *   US11 Analyzers (read-only), US10 Terminology (add a mapping + save +
 *   round-trip), US9 Panels (membership management), US12 Display Order
 *   (per-sample-type ordering). Each is URL-routed under the one Test Catalog
 *   Management SideNav entry (#3504), so the video also re-proves the
 *   consolidated nav for the new sections.
 *
 * Run the recording with: npm run pw:test:core-demo-video
 * Assumes the build stack's seeded catalog has at least one test.
 */
test.describe("OGC-949: Test Catalog editor sections M9–M12 (US9–US12)", () => {
  test("US9–US12 — analyzers, terminology, panels, display order", async ({
    page,
  }, testInfo) => {
    test.setTimeout(240_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title", async () => {
      await demo.title(
        "Test Catalog — completing the editor",
        "Analyzers · Terminology · Panels · Display Order (US9–US12)",
      );
    });

    await test.step("Open a test in the editor", async () => {
      await page.goto("/MasterListsPage/TestCatalogList");
      await expect(page.locator('[data-cy^="test-row-"]').first()).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-cy^="test-row-"]').first().click();
      await expect(page.locator('[data-cy="section-analyzers"]')).toBeVisible({
        timeout: 15_000,
      });
    });

    await test.step("US11 — Analyzers (read-only)", async () => {
      await page.locator('[data-cy="section-analyzers"]').click();
      await demo.scene("ANALYZERS (read-only)");
      await expect(page.getByTestId("analyzers-section")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US11-analyzers-section");
      await demo.pause(2000);
    });

    await test.step("US10 — Terminology: add a LOINC mapping and save", async () => {
      await page.locator('[data-cy="section-terminology"]').click();
      await demo.scene("TERMINOLOGY MAPPINGS");
      await expect(page.getByTestId("terminology-section")).toBeVisible({
        timeout: 10_000,
      });
      await page.locator("#terminology-source").selectOption("LOINC");
      await page.locator("#terminology-code").fill("1558-6");
      await page.locator("#terminology-relationship").selectOption("SAME_AS");
      await page.getByRole("button", { name: "Add mapping" }).click();
      await demo.evidence("US10-mapping-added");
      const savePut = page.waitForResponse(
        (r) =>
          r.url().includes("/test-catalog/tests/") &&
          r.url().includes("/terminology") &&
          r.request().method() === "PUT",
      );
      await page
        .getByTestId("terminology-section")
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await savePut;
      await demo.scene("TERMINOLOGY SAVED");
      // Prove persistence on visible UI: reload, reopen, the code survives.
      await page.reload();
      await page.locator('[data-cy="section-terminology"]').click();
      await expect(page.getByText("1558-6")).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US10-terminology-persisted");
      await demo.pause(2500);
    });

    await test.step("US9 — Panels: membership management", async () => {
      await page.locator('[data-cy="section-panels"]').click();
      await demo.scene("PANELS");
      await expect(page.getByTestId("panels-section")).toBeVisible({
        timeout: 10_000,
      });
      // The add-to-panel typeahead + the "create new panel" pointer to Master Lists.
      await page.getByRole("button", { name: "Create new panel" }).click();
      await demo.evidence("US9-panels-section");
      await demo.pause(2000);
    });

    await test.step("US12 — Display Order (per sample type)", async () => {
      await page.locator('[data-cy="section-display-order"]').click();
      await demo.scene("DISPLAY ORDER");
      await expect(page.getByTestId("display-order-section")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US12-display-order-section");
      await demo.pause(2500);
    });
  });
});
