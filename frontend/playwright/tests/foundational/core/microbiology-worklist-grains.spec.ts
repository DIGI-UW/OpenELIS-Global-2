import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  seedMicrobiologyAstWorklistCase,
  seedMicrobiologyWorklistCase,
  seedReviewedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

const expectColumns = async (page: Page, names: string[]) => {
  for (const name of names) {
    await expect(page.getByRole("columnheader", { name })).toBeVisible();
  }
};

test.describe("M-07 microbiology worklist grains", () => {
  test(
    "Culture worklist presents case work and opens the selected case",
    { tag: ["@evidence", "@microbiology", "@worklist"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "The culture worklist",
        "Open cases are listed with their stage, due action and priority, and a row opens its case.",
      );
      await demo.caption("Off screen: a culture case is ordered.");
      const seeded = await seedMicrobiologyWorklistCase(page);
      const query = new URLSearchParams({
        grain: "cultures",
        q: seeded.caseId,
        sort: "newest",
      });
      await page.goto(`/Microbiology/worklist?${query}`, {
        waitUntil: "domcontentloaded",
      });

      await expect(
        page.getByRole("heading", { name: "Open microbiology cases" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByRole("tab", { name: "Cultures" })).toHaveAttribute(
        "aria-selected",
        "true",
      );
      await expectColumns(page, [
        "Lab #",
        "Patient",
        "Specimen",
        "Stage",
        "Due action",
        "Priority",
        "Last activity by",
      ]);

      await demo.caption(
        "The Cultures tab lists each open case with its stage, due action, priority and last activity.",
      );
      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.caseId}`,
      );
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.highlight(row);
      await expect(row).toContainText(seeded.accessionNumber);
      await row.focus();
      await page.keyboard.press("Enter");
      await page.waitForURL((url) => {
        return (
          url.pathname === `/Microbiology/cases/${seeded.caseId}` &&
          url.searchParams.get("q") === seeded.caseId &&
          url.searchParams.get("sort") === "newest"
        );
      });
      await demo.verified(
        "The culture worklist opens the selected case",
        `${seeded.accessionNumber} opened with the worklist search and sort kept for the way back.`,
      );
    },
  );

  test(
    "AST worklist presents actionable runs and opens the exact isolate attempt",
    { tag: ["@evidence", "@microbiology", "@worklist"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "The AST worklist",
        "AST runs in progress are listed with their isolate, organism and panel, and a row opens exactly that run.",
      );
      await demo.caption(
        "Off screen: an isolate is identified and an AST run is started for it.",
      );
      const seeded = await seedMicrobiologyAstWorklistCase(page);
      const query = new URLSearchParams({
        grain: "ast",
        status: "in-progress",
        q: seeded.caseId,
      });
      await page.goto(`/Microbiology/worklist?${query}`, {
        waitUntil: "domcontentloaded",
      });

      await expect(page.getByRole("heading", { name: "AST runs" })).toBeVisible(
        {
          timeout: LONG_TIMEOUT,
        },
      );
      await expect(page.getByRole("tab", { name: "AST runs" })).toHaveAttribute(
        "aria-selected",
        "true",
      );
      await expect(
        page.getByText("Analyzer results arrive automatically", {
          exact: true,
        }),
      ).toBeVisible();
      await expectColumns(page, [
        "Lab #",
        "Isolate",
        "Patient",
        "Organism",
        "Panel",
        "Status",
        "Started",
        "Priority",
      ]);

      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.astRunId}`,
      );
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(row).toContainText(seeded.accessionNumber);
      await expect(row).toContainText("ISO-1");
      await expect(row).toContainText("Gram negative AST panel (UAT)");
      await expect(row).toContainText("In Progress");
      await demo.caption(
        "The AST runs tab lists the run in progress with its isolate, organism and panel.",
      );
      await demo.highlight(row);
      await row.focus();
      await page.keyboard.press("Enter");
      await page.waitForURL((url) => {
        return (
          url.pathname === `/Microbiology/cases/${seeded.caseId}` &&
          url.searchParams.get("grain") === "ast" &&
          url.searchParams.get("status") === "in-progress" &&
          url.searchParams.get("section") === "ast" &&
          url.searchParams.get("astIsolateId") === seeded.isolateId &&
          url.searchParams.get("astRunId") === seeded.astRunId
        );
      });
      await expect(
        page.getByRole("heading", { name: "AST / DST", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.verified(
        "The AST worklist opens the exact run",
        "The case opened at the AST section on ISO-1's run.",
      );
    },
  );

  test(
    "Reviewed AST runs leave active work and remain available read-only",
    { tag: ["@evidence", "@microbiology", "@worklist"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      const frame = page.viewportSize();
      await demo.intro(
        "Reviewed AST leaves the active list",
        "Once reviewed, a run leaves the AST worklist's active work but stays available, read-only, under Reviewed.",
      );
      await demo.caption(
        "Off screen: a case with a reviewed AST run is prepared.",
      );
      await page.setViewportSize({ width: 1440, height: 900 });
      const seeded = await seedReviewedMicrobiologyCase(page);
      const query = new URLSearchParams({
        grain: "ast",
        q: seeded.caseId,
      });
      await page.goto(`/Microbiology/worklist?${query}`, {
        waitUntil: "domcontentloaded",
      });

      await expect(page.getByRole("heading", { name: "AST runs" })).toBeVisible(
        {
          timeout: LONG_TIMEOUT,
        },
      );
      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.astRunId}`,
      );
      await expect(row).not.toBeVisible();
      await demo.caption(
        "The reviewed run is not among the active AST runs. Filtering to Reviewed shows it.",
      );

      await page.getByLabel("AST status").selectOption("reviewed");
      await page.waitForURL((url) => {
        return (
          url.pathname === "/Microbiology/worklist" &&
          url.searchParams.get("grain") === "ast" &&
          url.searchParams.get("status") === "reviewed" &&
          url.searchParams.get("q") === seeded.caseId
        );
      });

      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(row).toContainText("Reviewed");
      await demo.highlight(row);
      await testInfo.attach("reviewed-ast-worklist", {
        body: await page.getByTestId("microbiology-worklist").screenshot(),
        contentType: "image/png",
      });
      await row.getByRole("button", { name: "Row actions" }).click();
      await demo.caption(
        "Its actions offer only viewing: no editing and no new run.",
      );
      await expect(
        page.getByText("View reviewed AST", { exact: true }),
      ).toBeVisible();
      await expect(page.getByText("Edit AST", { exact: true })).toHaveCount(0);
      await expect(
        page.getByText("Set up new AST run", { exact: true }),
      ).toHaveCount(0);
      await testInfo.attach("reviewed-ast-worklist-read-only", {
        body: await page.getByTestId("microbiology-worklist").screenshot(),
        contentType: "image/png",
      });
      await page.getByText("View reviewed AST", { exact: true }).click();

      await page.waitForURL((url) => {
        return (
          url.pathname === `/Microbiology/cases/${seeded.caseId}` &&
          url.searchParams.get("grain") === "ast" &&
          url.searchParams.get("status") === "reviewed" &&
          url.searchParams.get("section") === "ast" &&
          url.searchParams.get("astIsolateId") === seeded.isolateId &&
          url.searchParams.get("astRunId") === seeded.astRunId &&
          url.searchParams.get("astView") === "reviewed"
        );
      });
      await expect(
        page.getByRole("heading", { name: "AST / DST", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      if (frame) await page.setViewportSize(frame);
      await demo.verified(
        "Reviewed runs stay available, read-only",
        "The reviewed run left the active work and opened in its read-only view.",
      );
    },
  );
});
