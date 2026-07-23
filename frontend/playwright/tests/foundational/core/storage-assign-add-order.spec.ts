import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Track A / Site 3 — Add Order → Sample Type step
 *
 * The StorageLocationSelector at this site lives inside each sample panel
 * within the multi-step Add Order wizard at /SamplePatientEntry. Reaching
 * Sample Type requires patient + program selection — a heavy setup that
 * depends on environment-specific seed data and is out of scope for a
 * regression smoke.
 *
 * What this spec verifies:
 *   - The /SamplePatientEntry route loads (catches accidental route removal)
 *   - The Add Order shell renders the patient-entry form (catches accidental
 *     unmount of the multi-step wizard)
 *
 * What this spec deliberately does NOT verify:
 *   - The StorageLocationSelector behavior — that's covered in depth by
 *     storage-assign-dashboard.spec.ts. The selector is a shared component
 *     across all four sites; if it works in Site 1, the component itself is
 *     not regressed.
 *
 * Future work: a follow-up spec under demo/core/ can walk the full Patient →
 * Program → Sample Type → assign-storage flow once seed-data infrastructure
 * exists for creating a test patient via REST.
 */
test.describe("Add Order — Sample Type entry point", () => {
  test("/SamplePatientEntry loads with patient-entry shell", async ({
    page,
  }) => {
    await page.goto("/SamplePatientEntry", {
      waitUntil: "domcontentloaded",
      timeout: LONG_TIMEOUT,
    });

    await expect(page).toHaveURL(/\/SamplePatientEntry/, {
      timeout: LONG_TIMEOUT,
    });

    // Add Order opens on the Patient step. The form contains a "Search"
    // action for an existing patient, which is a stable user-visible
    // landmark across the wizard.
    await expect(
      page.getByRole("button", { name: /search/i }).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });
});
