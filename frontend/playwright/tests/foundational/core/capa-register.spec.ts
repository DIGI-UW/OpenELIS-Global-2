import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { withAuthedPage } from "../../../helpers/api-session";
import { seedCapa } from "../../../helpers/seed-capa-data";

/**
 * CAPA Register (OGC-707) — cross-NCE corrective/preventive action view at
 * /qa/qms/capa-register. Folds the manual UAT into E2E: seed CAPAs across every
 * derived state via the real REST write path, then assert the rendered rows,
 * the client-derived status tags, the four summary tiles, and the
 * status/assignee filters + empty state.
 *
 * Completion status is asserted (green tag + Completed filter); the parent-NCE
 * date_completed does not round-trip through the legacy resolve endpoint, so
 * the Completed(90d) tile is asserted only as rendering a number, not a count.
 */

const REGISTER_URL = "/qa/qms/capa-register";

// Unique per run so seeded rows are isolable from any pre-existing data.
const RUN = Date.now().toString(36);
const TAG = `E2E-${RUN}`;

/** yyyy-MM-dd, `days` away from today. */
function shift(days: number): string {
  return new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
}

const SEEDS = {
  overdue: {
    nceNumber: `NCE-${TAG}-OVD`,
    title: `${TAG} overdue`,
    correctiveAction: "Recalibrate analyzer and verify QC",
    actionType: "1",
    personResponsible: `${TAG} Overdue Owner`,
    dueDate: shift(-5),
  },
  week: {
    nceNumber: `NCE-${TAG}-WK`,
    title: `${TAG} due this week`,
    correctiveAction: "Retrain staff on sample labeling",
    actionType: "1",
    personResponsible: `${TAG} Week Owner`,
    dueDate: shift(2),
  },
  future: {
    nceNumber: `NCE-${TAG}-FUT`,
    title: `${TAG} open future`,
    correctiveAction: "Add second-review step to result entry",
    actionType: "1",
    personResponsible: `${TAG} Future Owner`,
    dueDate: shift(30),
  },
  done: {
    nceNumber: `NCE-${TAG}-DONE`,
    title: `${TAG} completed`,
    correctiveAction: "Replace faulty pipette",
    actionType: "1",
    personResponsible: `${TAG} Done Owner`,
    dueDate: shift(-40),
    resolve: true,
  },
};

/** Read a summary tile's numeric value by its visible title. */
async function tileValue(page: Page, title: string): Promise<number> {
  const value = page
    .locator(".qi-tile", { hasText: title })
    .locator(".qi-tile__value");
  await expect(value).toBeVisible();
  return parseInt((await value.textContent())?.trim() || "", 10);
}

async function selectStatus(page: Page, label: string): Promise<void> {
  await page.getByRole("combobox", { name: "Status" }).click();
  await page.getByRole("option", { name: label, exact: true }).click();
}

test.describe("CAPA Register (OGC-707)", () => {
  test.beforeAll(async ({ browser }) => {
    await withAuthedPage(browser, async (page) => {
      for (const seed of Object.values(SEEDS)) {
        await seedCapa(page, seed);
      }
    });
  });

  test.beforeEach(async ({ page }) => {
    await page.goto(REGISTER_URL, { waitUntil: "domcontentloaded" });
  });

  test("rows render with client-derived status tags", async ({ page }) => {
    await page.getByLabel("Assignee", { exact: true }).fill(TAG);

    const overdue = page.getByRole("row", { name: SEEDS.overdue.nceNumber });
    const week = page.getByRole("row", { name: SEEDS.week.nceNumber });
    const future = page.getByRole("row", { name: SEEDS.future.nceNumber });
    const done = page.getByRole("row", { name: SEEDS.done.nceNumber });

    // The cross-NCE join: each seeded action log is rendered against its parent
    // event, with the additive due_date column driving the derived tag.
    await expect(overdue).toContainText(SEEDS.overdue.correctiveAction);
    await expect(overdue).toContainText("Overdue");
    await expect(week).toContainText("Open");
    await expect(future).toContainText("Open");
    await expect(done).toContainText("Completed");
  });

  test("summary tiles reflect the seeded states", async ({ page }) => {
    // Tiles are global (not filtered), so assert >= the seeded contribution to
    // stay robust against any pre-existing register data.
    expect(await tileValue(page, "Open")).toBeGreaterThanOrEqual(3);
    expect(await tileValue(page, "Overdue")).toBeGreaterThanOrEqual(1);
    expect(await tileValue(page, "Due This Week")).toBeGreaterThanOrEqual(1);
    // Completed(90d) needs the parent NCE date_completed, which the legacy
    // resolve endpoint doesn't persist — assert it renders a number only.
    expect(Number.isNaN(await tileValue(page, "Completed"))).toBe(false);
  });

  test("status filter narrows to a single derived state", async ({ page }) => {
    const rows = page.locator("table tbody tr");
    await page.getByLabel("Assignee", { exact: true }).fill(TAG);

    await selectStatus(page, "Overdue");
    await expect(rows).toHaveCount(1);
    await expect(rows).toContainText(SEEDS.overdue.nceNumber);

    await selectStatus(page, "Completed");
    await expect(rows).toHaveCount(1);
    await expect(rows).toContainText(SEEDS.done.nceNumber);
  });

  test("assignee filter isolates a row and empties on no match", async ({
    page,
  }) => {
    const rows = page.locator("table tbody tr");
    const assignee = page.getByLabel("Assignee", { exact: true });

    await assignee.fill(SEEDS.week.personResponsible);
    await expect(rows).toHaveCount(1);
    await expect(rows).toContainText(SEEDS.week.nceNumber);

    await assignee.fill(`no-such-owner-${RUN}`);
    await expect(page.locator(".qa-empty")).toBeVisible();
  });
});
