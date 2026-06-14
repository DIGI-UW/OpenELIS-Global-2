import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

/**
 * OGC-949 — Reference ranges, coverage validation, and the activation safety
 * gate (US7). M9 video proof — the highest-value story: a test cannot be
 * activated while its reference ranges leave an age window uncovered without an
 * explicit acknowledgment (the H-03 neonatal-bilirubin patient-safety gate).
 *
 * The video walks: open a test → Ranges section → add a range that starts above
 * age 0 (leaving a gap from birth) → save → the coverage panel flags the gap →
 * Basic Info → toggle Active → the activation is BLOCKED by the acknowledgment
 * modal → acknowledge → the test activates.
 *
 * Run the recording with: npm run pw:test:core-demo-video
 * (the core-demo project runs the same spec without video as a CI-safe check.)
 *
 * Mutating by design — proving the gate requires a real activate round-trip.
 * Assumes the build stack's seeded catalog has at least one test. The gap is
 * created by the demo's own range (gender Male, ages 1–18y → birth–1y uncovered),
 * so it does not depend on the test's pre-existing ranges.
 */
test.describe("OGC-949: Activation coverage gate (US7)", () => {
  test("US7 — a coverage gap blocks activation until acknowledged", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title", async () => {
      await demo.title(
        "Reference ranges & the activation gate",
        "A coverage gap blocks activation until a clinician acknowledges it",
      );
    });

    await test.step("Open a test in the editor", async () => {
      await page.goto("/MasterListsPage/TestCatalogList");
      await expect(page.locator('[data-cy^="test-row-"]').first()).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-cy^="test-row-"]').first().click();
      await expect(page.locator('[data-cy="section-ranges"]')).toBeVisible({
        timeout: 15_000,
      });
    });

    await test.step("US7 — Add a reference range that leaves a gap", async () => {
      await page.locator('[data-cy="section-ranges"]').click();
      await demo.scene("REFERENCE RANGES");
      await expect(page.getByTestId("add-range")).toBeVisible({
        timeout: 10_000,
      });
      await page.getByTestId("add-range").click();

      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      // Male, 1–18 years → birth to 1 year is left uncovered (the gap).
      await dialog.locator("#range-sex").selectOption("M");
      await dialog.locator("#range-age-unit").selectOption("years");
      await dialog.locator("#range-minAgeValue").fill("1");
      await dialog.locator("#range-maxAgeValue").fill("18");
      await demo.evidence("US7-range-modal");
      await demo.pause(1500);
      await dialog.getByText("Save").click();
    });

    await test.step("US7 — The coverage panel flags the gap", async () => {
      // Persist the ranges; the per-sex coverage report recomputes on reload.
      await page.getByRole("button", { name: "Save" }).click();
      await demo.scene("COVERAGE VALIDATION");
      await expect(page.getByText("Has gaps")).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US7-coverage-gap");
      await demo.pause(2500);
    });

    await test.step("US7 — Activation is blocked by the acknowledgment modal", async () => {
      await page.locator('[data-cy="section-basic-info"]').click();
      const activeToggle = page.getByRole("switch", { name: /Active/ });
      await expect(activeToggle).toBeVisible({ timeout: 10_000 });
      // Guarantee an off→on transition (the gate only fires when turning ON).
      if ((await activeToggle.getAttribute("aria-checked")) === "true") {
        await activeToggle.click();
      }
      await activeToggle.click();

      await demo.scene("ACTIVATION BLOCKED");
      await expect(
        page.getByText("This test has reference-range coverage gaps."),
      ).toBeVisible({ timeout: 10_000 });
      await demo.evidence("US7-activation-blocked");
      await demo.pause(3000);
    });

    await test.step("US7 — Acknowledge and activate", async () => {
      await page.getByText("Acknowledge and activate").click();
      await demo.scene("ACTIVATED WITH ACKNOWLEDGMENT");
      // The modal closes once the acknowledged activation succeeds.
      await expect(
        page.getByText("This test has reference-range coverage gaps."),
      ).toBeHidden({ timeout: 10_000 });
      await demo.evidence("US7-activated");
      await demo.pause(2500);
    });
  });
});
