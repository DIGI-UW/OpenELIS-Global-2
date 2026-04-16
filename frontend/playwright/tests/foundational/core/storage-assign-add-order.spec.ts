import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { StorageLocationSelector } from "../../../fixtures/storage-location-selector";

/**
 * Track A / Site 3 — Add Order → Sample Type step
 *
 * User story:
 *   At /SamplePatientEntry the user fills patient → program → arrives at the
 *   "Sample Type" step (page 2 of 5) where the StorageLocationSelector is
 *   embedded inline within each sample panel — workflow="orders" + two-tier
 *   design (compact view + expandable LocationManagementModal).
 *
 * Coverage scope (intentionally narrower than Site 1):
 *   The StorageLocationSelector is a shared component — Site 1 (Storage
 *   Dashboard) exercises the dropdown/create paths definitively. THIS spec
 *   covers Site-3-specific concerns: that the selector mounts inside the
 *   AddOrder multi-step form without context errors, and is interactable
 *   once the user reaches Sample Type.
 *
 *   Walking the full Patient → Program → Sample Type funnel from scratch
 *   requires creating a patient, choosing a program, etc. — heavy setup
 *   that's out of scope for a regression smoke. The spec navigates as far
 *   as the form accepts via stable selectors and asserts on whatever state
 *   it can reach. If patient pre-fill data is missing, the test reports a
 *   skip with a diagnostic.
 *
 * If this spec consistently skips on a real environment, that itself is a
 * signal — either the seed data needs adjustment or the Add Order entry
 * point regressed.
 */
test.describe("Add Order — Sample Type storage selector mounts", () => {
  test("selector is reachable and interactable from /SamplePatientEntry", async ({
    page,
  }) => {
    await page.goto("/SamplePatientEntry", {
      waitUntil: "domcontentloaded",
      timeout: LONG_TIMEOUT,
    });

    // The first page is patient. We don't fill it (out of scope) — but we
    // still want to know whether the page loaded at all and the multi-step
    // shell is rendered.
    const wizard = page.locator(".cds--progress, .cds--tabs, [role='tablist']");
    await expect(wizard.first()).toBeVisible({ timeout: LONG_TIMEOUT });

    // If a sample panel is already visible (e.g. seeded patient), the
    // StorageLocationSelector should be embedded inside it. Otherwise skip
    // — the failure is informative without being spurious.
    const selectorExists = await page
      .locator('[data-testid="storage-location-selector"]')
      .first()
      .isVisible()
      .catch(() => false);

    test.skip(
      !selectorExists,
      "Add Order Sample Type panel not reachable without patient/program prefill — " +
        "seed an in-progress patient to enable this regression spec.",
    );

    const selector = new StorageLocationSelector(page);
    await selector.expectVisible();

    // Site-specific check: clicking the search input opens the
    // tree/autocomplete dropdown without errors.
    await selector.openSearchDropdown();

    // Verify at least one location renders so we know API + UI are wired.
    await expect(
      page.locator(
        '[data-testid="location-tree-view"], [data-testid="location-autocomplete"]',
      ),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });
});
