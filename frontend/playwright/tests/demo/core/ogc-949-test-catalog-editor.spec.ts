import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

/**
 * OGC-949 — Unified Test Catalog editor walkthrough (US3–US6). M9 video proof.
 *
 * A read-mostly tour that doubles as the stakeholder video: browse the test list
 * (US3), open a test in the editor, and walk its Basic Info (US4), Sample &
 * Results (US5) and Methods (US6) sections. The mutating flows (save / diff
 * reconcile) are proven by the backend integration + vitest suites; this demo
 * stays non-mutating so it is safely repeatable against the shared build stack.
 *
 * Run the recording with: npm run pw:test:core-demo-video
 * (the core-demo project runs the same spec without video as a CI-safe check.)
 *
 * Assumes the build stack's seeded catalog has at least one test (standard OE
 * seed data). US7 (Ranges/activation) + US8 (Storage) videos land with M7/M8.
 */
test.describe("OGC-949: Unified Test Catalog editor (US3–US6)", () => {
  test("US3–US6 — browse the catalog and walk the editor sections", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title", async () => {
      await demo.title(
        "Unified Test Catalog",
        "Browse tests, then configure one across the editor's sections",
      );
    });

    await test.step("US3 — Test list view", async () => {
      await page.goto("/MasterListsPage/TestCatalogList");
      await demo.scene("TEST CATALOG LIST");
      // The list renders at least one test (seeded catalog data).
      await expect(page.locator('[data-cy^="test-row-"]').first()).toBeVisible({
        timeout: 20_000,
      });
      await demo.evidence("US3-test-list");
      await demo.pause(2500);
    });

    await test.step("US3 — Open a test in the editor", async () => {
      await page.locator('[data-cy^="test-row-"]').first().click();
      // The editor shell renders its SideNav sections.
      await expect(page.locator('[data-cy="section-basic-info"]')).toBeVisible({
        timeout: 15_000,
      });
      await demo.scene("TEST CATALOG EDITOR");
      await demo.evidence("US3-editor-opened");
      await demo.pause(2000);
    });

    await test.step("US4 — Basic Info section", async () => {
      // Basic Info is the default section; its identity field renders.
      await expect(page.locator("#basic-info-name")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US4-basic-info");
      await demo.pause(2000);
    });

    await test.step("US5 — Sample & Results section", async () => {
      await page.locator('[data-cy="section-sample-results"]').click();
      // The multi-component editor loads (the Add-component control is present).
      await expect(page.getByTestId("add-component")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US5-sample-results");
      await demo.pause(1500);
      // Demonstrate the add-component affordance (in-memory only; not saved).
      await page.getByTestId("add-component").click();
      await demo.evidence("US5-add-component");
      await demo.pause(1500);
    });

    await test.step("US6 — Methods section", async () => {
      await page.locator('[data-cy="section-methods"]').click();
      await expect(page.getByTestId("methods-section")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US6-methods");
      await demo.pause(1500);
    });
  });
});
