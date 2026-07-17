import { test, expect, Page } from "../../../helpers/test-base";
import { seedCapa } from "../../../helpers/seed-capa-data";

/**
 * CAPA Register (OGC-707) — cross-NCE corrective/preventive action view at
 * /qa/qms/capa-register. Folds the manual UAT into E2E: seed CAPAs across every
 * derived state via the real REST write path, then assert the endpoint join,
 * client-derived status tags, the four summary tiles, and the status/assignee
 * filters + empty state.
 *
 * Completion status is asserted (green tag + Completed filter); the parent-NCE
 * date_completed does not round-trip through the legacy resolve endpoint, so
 * the Completed(90d) tile is asserted only as rendering a number, not a count.
 */

const REGISTER_API = "/api/OpenELIS-Global/rest/nce/capa-register";
const REGISTER_URL = "/qa/qms/capa-register";

// Unique per run so seeded rows are isolable from any pre-existing data.
const RUN = Date.now().toString(36);
const TAG = `E2E-${RUN}`;

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}`;
}
function shift(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return iso(d);
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

/** Filter the register to a substring of person_responsible. */
async function filterByAssignee(page: Page, text: string): Promise<void> {
  await page.fill("#capa-assignee-filter", text);
}

async function selectStatus(page: Page, label: string): Promise<void> {
  await page.locator("#capa-status-filter").click();
  await page.getByRole("option", { name: label, exact: true }).click();
}

test.describe.serial("CAPA Register (OGC-707)", () => {
  test.beforeAll(async ({ browser }) => {
    const ctx = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await ctx.newPage();
    // A page context is needed so seedCapa can read the CSRF token from auth state.
    await page.goto("/", { waitUntil: "domcontentloaded", timeout: 15_000 });
    for (const seed of Object.values(SEEDS)) {
      await seedCapa(page, seed);
    }
    await ctx.close();
  });

  test("endpoint returns seeded CAPAs joined to their NCEs", async ({
    page,
  }) => {
    const res = await page.request.get(REGISTER_API);
    expect(res.status(), "register is gated on qa.view.qms; admin passes").toBe(
      200,
    );
    const rows = await res.json();
    expect(Array.isArray(rows)).toBe(true);

    for (const seed of Object.values(SEEDS)) {
      const row = rows.find(
        (r: { nceNumber: string }) => r.nceNumber === seed.nceNumber,
      );
      expect(row, `${seed.nceNumber} must appear in the register`).toBeTruthy();
      expect(row.correctiveAction).toBe(seed.correctiveAction);
      expect(row.personResponsible).toBe(seed.personResponsible);
      expect(row.dueDate).toBe(seed.dueDate); // additive due_date column round-trips
      expect(row.nceEventId).toBeGreaterThan(0); // cross-NCE join populated
    }
  });

  test("rows render with client-derived status tags", async ({ page }) => {
    await page.goto(REGISTER_URL, { waitUntil: "domcontentloaded" });
    await filterByAssignee(page, TAG);

    const overdue = page.locator("tr", { hasText: SEEDS.overdue.nceNumber });
    const week = page.locator("tr", { hasText: SEEDS.week.nceNumber });
    const future = page.locator("tr", { hasText: SEEDS.future.nceNumber });
    const done = page.locator("tr", { hasText: SEEDS.done.nceNumber });

    await expect(overdue).toContainText("Overdue");
    await expect(week).toContainText("Open");
    await expect(future).toContainText("Open");
    await expect(done).toContainText("Completed");
  });

  test("summary tiles reflect the seeded states", async ({ page }) => {
    await page.goto(REGISTER_URL, { waitUntil: "domcontentloaded" });
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
    await page.goto(REGISTER_URL, { waitUntil: "domcontentloaded" });
    await filterByAssignee(page, TAG);

    await selectStatus(page, "Overdue");
    await expect(page.locator("table tbody tr")).toHaveCount(1);
    await expect(page.locator("table tbody tr")).toContainText(
      SEEDS.overdue.nceNumber,
    );

    await selectStatus(page, "Completed");
    await expect(page.locator("table tbody tr")).toHaveCount(1);
    await expect(page.locator("table tbody tr")).toContainText(
      SEEDS.done.nceNumber,
    );
  });

  test("assignee filter isolates a row and empties on no match", async ({
    page,
  }) => {
    await page.goto(REGISTER_URL, { waitUntil: "domcontentloaded" });

    await filterByAssignee(page, SEEDS.week.personResponsible);
    await expect(page.locator("table tbody tr")).toHaveCount(1);
    await expect(page.locator("table tbody tr")).toContainText(
      SEEDS.week.nceNumber,
    );

    await filterByAssignee(page, `no-such-owner-${RUN}`);
    await expect(page.locator(".qa-empty")).toBeVisible();
  });
});
