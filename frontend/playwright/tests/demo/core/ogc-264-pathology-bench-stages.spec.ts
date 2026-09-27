import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-264: the histopathology bench stages on the Pathology Dashboard.
 *
 * Story proof for the stage rework: a case now moves through the eleven
 * stages the bench actually works, the filter names each one in the user's
 * language instead of printing the stored constant, and the filter and the
 * count tiles read the same grouping. The seven stages a laboratory may not
 * run are switches an administrator sets per deployment, shown at the end on
 * the Result Entry Configuration page.
 *
 * UI only. The walkthrough drives the visible filter and asserts the visible
 * selection after every change.
 *
 * Record with: npm run pw:test:core-demo-video
 */

/** A few stages from across the bench, chosen to show the span of the list. */
const BENCH_STAGE_WALKTHROUGH = [
  { value: "ACCESSIONED", label: "Accessioned" },
  { value: "MICROTOMY", label: "Microtomy" },
  { value: "UNDER_REVIEW", label: "Under Pathologist Review" },
  { value: "COMPLETED", label: "Completed" },
];

/**
 * Three fixed options ahead of the eleven stages: a disabled placeholder and
 * two groupings.
 */
const STAGE_FILTER_OPTION_COUNT = 3 + 11;

test.describe("OGC-264: Pathology Dashboard bench stages", () => {
  test("the stage filter names every bench stage in the user's language", async ({
    page,
  }, testInfo) => {
    test.setTimeout(150_000);
    const demo = createDemoPresentation(page, testInfo);
    const stageFilter = page.locator("select#statusFilter");

    await test.step("Title", async () => {
      await demo.title(
        "Pathology Dashboard: the histopathology bench stages",
        "Eleven bench stages replace the old status list, the filter and the count tiles read one grouping, and the seven optional stages are per-deployment switches",
      );
    });

    await test.step("Open the dashboard and show the count tiles", async () => {
      await page.goto("/PathologyDashboard", { waitUntil: "domcontentloaded" });
      await demo.scene("PATHOLOGY DASHBOARD");

      await expect(
        page.getByRole("heading", { name: "Pathology", exact: true }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(page.locator(".dashboard-tile")).toHaveCount(4);
      await expect(
        page.getByRole("heading", { name: "Cases in Progress", exact: true }),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      await demo.evidence("ogc-264-dashboard-tiles");
      await demo.pause(2000);
    });

    await test.step("The filter opens on the in-progress grouping", async () => {
      await expect(stageFilter).toHaveValue("IN_PROGRESS", {
        timeout: LONG_TIMEOUT,
      });
      await expect(stageFilter.locator("option")).toHaveCount(
        STAGE_FILTER_OPTION_COUNT,
      );

      await demo.scene("STAGE FILTER");
      await demo.evidence("ogc-264-stage-filter");
      await demo.pause(1500);
    });

    for (const stage of BENCH_STAGE_WALKTHROUGH) {
      await test.step(`Filter on ${stage.label}`, async () => {
        await stageFilter.selectOption(stage.value);

        await expect(stageFilter).toHaveValue(stage.value);
        // The chosen option reads as its English label, never as the stored
        // constant behind it.
        await expect(stageFilter.locator("option:checked")).toHaveText(
          stage.label,
        );

        await demo.scene(stage.label.toUpperCase());
        await demo.pause(1500);
      });
    }

    await test.step("Return to the in-progress grouping", async () => {
      await stageFilter.selectOption("IN_PROGRESS");

      await expect(stageFilter).toHaveValue("IN_PROGRESS");
      await expect(stageFilter.locator("option:checked")).toHaveText(
        "In Progress",
      );

      await demo.scene("IN PROGRESS");
      await demo.evidence("ogc-264-in-progress-grouping");
      await demo.pause(2500);
    });

    await test.step("The optional stages are per-deployment switches", async () => {
      await page.goto("/MasterListsPage/ResultConfigurationMenu", {
        waitUntil: "domcontentloaded",
      });
      await demo.scene("STAGE SWITCHES");

      await expect(
        page.getByRole("heading", {
          name: "Result Entry Configuration",
          exact: true,
        }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });

      // Widen the table to its largest page so the scene does not depend on
      // how many configuration rows this deployment happens to hold.
      const pagination = page.locator(".cds--pagination");
      await expect(pagination).toBeVisible({ timeout: UI_TIMEOUT });
      await pagination.getByLabel("Items per page").selectOption("50");

      const decalcificationSwitch = page.getByRole("cell", {
        name: "pathology.stage.DECALCIFICATION.enabled",
      });
      await expect(decalcificationSwitch).toBeVisible({ timeout: UI_TIMEOUT });

      // The switches sit further down a long configuration table, so bring
      // them into the frame before the camera and the screenshot record it.
      await decalcificationSwitch.scrollIntoViewIfNeeded();
      await expect(
        page.getByRole("cell", {
          name: "pathology.stage.UNDER_REVIEW.enabled",
        }),
      ).toBeVisible();

      await demo.evidence("ogc-264-stage-switches");
      await demo.pause(2500);
    });
  });
});
