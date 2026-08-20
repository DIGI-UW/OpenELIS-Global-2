import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  seedMicrobiologyAstWorklistCase,
  seedMicrobiologyWorklistCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const expectColumns = async (page: Page, names: string[]) => {
  for (const name of names) {
    await expect(page.getByRole("columnheader", { name })).toBeVisible();
  }
};

test.describe("M-07 microbiology worklist grains", () => {
  test("Culture worklist presents case work and opens the selected case", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyWorklistCase(page);
    const query = new URLSearchParams({
      workflow: "BACTERIOLOGY",
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

    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText(seeded.accessionNumber);
    await expect(row).toContainText("Linked · 2 workflows");
    await row.focus();
    await page.keyboard.press("Enter");
    await page.waitForURL((url) => {
      return (
        url.pathname === `/Microbiology/cases/${seeded.caseId}` &&
        url.searchParams.get("workflow") === "BACTERIOLOGY" &&
        url.searchParams.get("q") === seeded.caseId &&
        url.searchParams.get("sort") === "newest"
      );
    });
  });

  test("AST worklist presents actionable runs and opens the exact isolate attempt", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyAstWorklistCase(page);
    const query = new URLSearchParams({
      grain: "ast",
      status: "in-progress",
      q: seeded.caseId,
    });
    await page.goto(`/Microbiology/worklist?${query}`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByRole("heading", { name: "AST runs" })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByRole("tab", { name: "AST runs" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    await expect(
      page.getByText("Analyzer results arrive automatically", { exact: true }),
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
    await expect(page.getByRole("heading", { name: "Manual AST" })).toBeVisible(
      { timeout: LONG_TIMEOUT },
    );
  });
});
