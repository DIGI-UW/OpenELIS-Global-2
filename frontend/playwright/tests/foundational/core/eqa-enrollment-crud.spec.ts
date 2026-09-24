import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import { removeSelfEnrollments } from "../../../helpers/seed-eqa-data";

/**
 * EQA self-enrollment: create, edit and deactivate through the UI (OGC-613).
 *
 * This is the page a lab uses to say which schemes it takes part in, and
 * every other participant surface reads what it writes — the other specs
 * plant enrollment rows with SQL precisely because this path was untested.
 * No seeding here: the spec creates its own enrollment and removes it by
 * driving the page, so what it asserts is the real write path.
 *
 * There is deliberately nothing to assert about deletion. The overflow menu
 * offers Deactivate, not Delete, and the soft-delete endpoint has no caller
 * in the UI at all.
 */

const RUN = Date.now().toString(36);
const PROGRAM = `E2E ${RUN} enrollment`;
const RENAMED = `${PROGRAM} revised`;

// Enrolling is the laboratory's own act, and the write endpoints are gated on
// the participant permission rather than an administrative one — so the spec
// signs in as a bench user. Running it as an administrator would pass even if
// the grant a real laboratory relies on were revoked.
test.use({ storageState: "playwright/.auth/participant.json" });

test.describe("EQA self-enrollment", () => {
  test.afterAll(() => {
    // The page can deactivate a row but never delete one, so the only way to
    // leave the database as we found it is to remove it directly.
    removeSelfEnrollments(`E2E ${RUN}`);
  });

  test("an enrollment is created, edited and deactivated", async ({ page }) => {
    test.setTimeout(180_000);
    const row = () => page.locator("tr", { hasText: PROGRAM });

    await test.step("the enrollment form saves a new programme", async () => {
      await page.goto("/qa/eqa/my-programs", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "My EQA Programs" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Enroll in Program" }).click();
      await expect(
        page.getByRole("heading", { name: "New EQA Program Enrollment" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      // Programme name and provider are the only required fields, and Save
      // stays disabled until both carry text.
      const save = page.getByRole("button", { name: "Save Enrollment" });
      await expect(save).toBeDisabled();
      await page.locator("#enrollment-program-name").fill(PROGRAM);
      await expect(save).toBeDisabled();
      await page.locator("#enrollment-provider").fill("E2E External Provider");
      await expect(save).toBeEnabled();
      await save.click();

      // The success toast is global and auto-dismisses, so the row itself is
      // the assertion — it is also the stronger claim.
      await expect(row()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row().getByText("Active")).toBeVisible();
    });

    await test.step("editing renames it in place", async () => {
      await row().getByRole("button", { name: "Options" }).click();
      await page.getByRole("menuitem", { name: "Edit" }).click();
      await expect(
        page.getByRole("heading", { name: `Editing: ${PROGRAM}` }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.locator("#enrollment-program-name").fill(RENAMED);
      await page.getByRole("button", { name: "Save Changes" }).click();
      await expect(page.locator("tr", { hasText: RENAMED })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      // Editing must replace the row, not add a second one: this run's tag
      // should match exactly one enrollment.
      await expect(page.locator("tbody tr", { hasText: RUN })).toHaveCount(1);
    });

    await test.step("deactivating flips the status tag", async () => {
      const renamedRow = page.locator("tr", { hasText: RENAMED });
      await renamedRow.getByRole("button", { name: "Options" }).click();
      // No confirmation step — the toggle fires on click.
      await page.getByRole("menuitem", { name: "Deactivate" }).click();
      await expect(renamedRow.getByText("Inactive")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });
  });
});
