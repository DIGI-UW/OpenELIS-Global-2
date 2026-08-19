import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  seedAnalyzerReviewMicrobiologyCase,
  seedMicrobiologyAstWorklistCase,
  seedMicrobiologyWorklistCase,
  seedReviewedMicrobiologyCase,
  submitQcFailedAstAnalyzerResults,
  submitUnmatchedAstAnalyzerResults,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const expectColumns = async (page: Page, names: string[]) => {
  for (const name of names) {
    await expect(page.getByRole("columnheader", { name })).toBeVisible();
  }
};

const frameBelowHeader = async (page: Page, testId: string) => {
  const target = page.getByTestId(testId);
  await target.evaluate((element) => {
    const headerHeight =
      document.querySelector("header")?.getBoundingClientRect().height ?? 0;
    const targetTop = element.getBoundingClientRect().top + window.scrollY;
    window.scrollTo({
      top: targetTop - headerHeight - 16,
      behavior: "instant",
    });
  });
  await expect(target).toBeInViewport();
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

  test("Reviewed AST runs leave active work and remain available read-only", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const seeded = await seedReviewedMicrobiologyCase(page);
    const query = new URLSearchParams({
      grain: "ast",
      q: seeded.caseId,
    });
    await page.goto(`/Microbiology/worklist?${query}`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByRole("heading", { name: "AST runs" })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    const row = page.getByTestId(
      `microbiology-worklist-row-${seeded.astRunId}`,
    );
    await expect(row).not.toBeVisible();

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
    await testInfo.attach("reviewed-ast-worklist", {
      body: await page.getByTestId("microbiology-worklist").screenshot(),
      contentType: "image/png",
    });
    await row.getByRole("button", { name: "Row actions" }).click();
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
    await expect(page.getByRole("heading", { name: "Manual AST" })).toBeVisible(
      { timeout: LONG_TIMEOUT },
    );
  });

  test("Analyzer AST results expose QC evidence, resolve explicitly, and become reviewable", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const seeded = await seedAnalyzerReviewMicrobiologyCase(page);
    const activeQuery = new URLSearchParams({
      grain: "ast",
      q: seeded.caseId,
    });
    await page.goto(`/Microbiology/worklist?${activeQuery}`, {
      waitUntil: "domcontentloaded",
    });

    const row = page.getByTestId(
      `microbiology-worklist-row-${seeded.astRunId}`,
    );
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("Awaiting Results");

    await submitQcFailedAstAnalyzerResults(page, seeded);
    const resultsQuery = new URLSearchParams({
      grain: "ast",
      status: "results-in",
      q: seeded.caseId,
    });
    await page.goto(`/Microbiology/worklist?${resultsQuery}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("QC Failed");
    await row.focus();
    await page.keyboard.press("Enter");
    await page.waitForURL((url) => {
      return (
        url.pathname === `/Microbiology/cases/${seeded.caseId}` &&
        url.searchParams.get("grain") === "ast" &&
        url.searchParams.get("status") === "results-in" &&
        url.searchParams.get("section") === "ast" &&
        url.searchParams.get("astIsolateId") === seeded.isolateId &&
        url.searchParams.get("astRunId") === seeded.astRunId
      );
    });

    await expect(page.getByRole("heading", { name: "Manual AST" })).toBeVisible(
      { timeout: LONG_TIMEOUT },
    );
    const astCard = page.getByTestId("microbiology-ast-card");
    await expect(astCard.getByText("Analyzer QC failed")).toBeVisible();
    await expect(
      astCard.getByText("UAT microbiology AST analyzer"),
    ).toBeVisible();
    await expect(astCard.getByText(/UAT-AST-CARD-/)).toBeVisible();
    await expect(
      page.getByRole("cell", { name: "Ciprofloxacin (UAT)", exact: true }),
    ).toBeVisible();
    await expect(page.getByText("Analyzer: Susceptible")).toBeVisible();
    await frameBelowHeader(page, "microbiology-ast-card");
    await testInfo.attach("analyzer-ast-qc-failed", {
      body: await page.screenshot(),
      contentType: "image/png",
    });

    const reason = page.getByLabel("Reason and corrective action");
    await reason.fill("QC control investigated; supervisor accepts this run");
    const overrideQc = page.getByRole("button", { name: "Override QC flag" });
    await expect(overrideQc).toBeEnabled();
    await overrideQc.click();
    await expect(
      page.getByText("Analyzer results ready for review"),
    ).toBeVisible();
    const accept = page.getByRole("button", { name: "Accept results" });
    await expect(accept).toBeEnabled();
    await accept.click();
    await expect(page.getByTestId("microbiology-ast-run-status")).toContainText(
      "Reviewed",
    );
    await frameBelowHeader(page, "microbiology-ast-card");
    await testInfo.attach("analyzer-ast-reviewed", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });

  test("Unmatched analyzer AST results remain visible for admin reconciliation", async ({
    page,
  }) => {
    const seeded = await seedAnalyzerReviewMicrobiologyCase(page);
    const unmatched = await submitUnmatchedAstAnalyzerResults(page, seeded);

    await page.goto("/AnalyzerResults?view=import-issues", {
      waitUntil: "domcontentloaded",
    });

    await expect(
      page.getByRole("heading", { name: "Analyzer import issues" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const row = page.getByRole("row").filter({ hasText: unmatched.sourceId });
    await expect(row).toContainText("AST result available");
    await expect(row).toContainText(
      "No AST run matched the analyzer and card identifiers.",
    );
    await expect(
      row.getByRole("link", { name: "Open mappings" }),
    ).toHaveAttribute(
      "href",
      `/analyzers/${seeded.analyzerInstrumentId}/mappings`,
    );
    await expect(
      page.getByRole("button", { name: /retry|reprocess/i }),
    ).toHaveCount(0);
  });
});
