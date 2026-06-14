import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

/**
 * OGC-949 — Sample Storage configuration (US8). M9 video proof.
 *
 * The video walks: open a test → Storage section → set the storage condition,
 * special-handling flags, and disposal method → save. Persistence is a singleton
 * upsert (one row per test), so the save is idempotent and safe to record
 * repeatedly against the shared build stack.
 *
 * Run the recording with: npm run pw:test:core-demo-video
 * (the core-demo project runs the same spec without video as a CI-safe check.)
 *
 * Assumes the build stack's seeded catalog has at least one test.
 */
test.describe("OGC-949: Sample Storage configuration (US8)", () => {
  test("US8 — configure a test's storage, handling, and disposal", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title", async () => {
      await demo.title(
        "Sample Storage configuration",
        "Storage conditions, special handling, and disposal — per test",
      );
    });

    await test.step("Open a test in the editor", async () => {
      await page.goto("/MasterListsPage/TestCatalogList");
      await expect(page.locator('[data-cy^="test-row-"]').first()).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-cy^="test-row-"]').first().click();
      await expect(page.locator('[data-cy="section-storage"]')).toBeVisible({
        timeout: 15_000,
      });
    });

    await test.step("US8 — Open the Storage section", async () => {
      await page.locator('[data-cy="section-storage"]').click();
      await demo.scene("SAMPLE STORAGE");
      await expect(page.getByTestId("storage-section")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US8-storage-section");
      await demo.pause(2000);
    });

    await test.step("US8 — Set storage condition, handling, and disposal", async () => {
      await page.locator("#storage-condition").selectOption("REFRIGERATED");
      await page.locator("#storage-duration").fill("7");
      await page.locator("#storage-duration-unit").selectOption("days");
      // Special-handling flag (click the Carbon checkbox label).
      await page.getByText("Protect from light", { exact: true }).click();
      await page
        .locator("#storage-disposal-method")
        .selectOption("INCINERATION");
      await demo.evidence("US8-storage-configured");
      await demo.pause(2000);
    });

    await test.step("US8 — Save the storage configuration", async () => {
      await page.getByRole("button", { name: "Save" }).click();
      await demo.scene("STORAGE SAVED");
      // The success notification confirms the singleton upsert persisted.
      await expect(page.getByText("Sample storage saved.")).toBeVisible({
        timeout: 15_000,
      });
      await demo.evidence("US8-storage-saved");
      await demo.pause(2500);
    });
  });
});
