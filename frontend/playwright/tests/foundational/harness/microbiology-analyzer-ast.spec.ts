import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  seedAnalyzerReviewMicrobiologyCase,
  submitQcFailedAstAnalyzerResults,
  submitUnmatchedAstAnalyzerResults,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

// Exercise normalized AST review with a real configured harness connection.
// Instrument parsing and protocol compatibility have their own harness journeys.
const SOURCE_ANALYZER = "Cepheid GeneXpert (ASTM Mode)";

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

test.describe("Microbiology analyzer AST review", () => {
  test("Analyzer AST results expose QC evidence, resolve explicitly, and become reviewable", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const seeded = await seedAnalyzerReviewMicrobiologyCase(
      page,
      SOURCE_ANALYZER,
    );
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

    const browserIngressAttempt = await page.request.post(
      "/api/OpenELIS-Global/rest/analyzer/events/ast",
      { data: {} },
    );
    expect(browserIngressAttempt.status()).toBe(401);

    await submitQcFailedAstAnalyzerResults(seeded);
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
    await expect(astCard.getByText(SOURCE_ANALYZER)).toBeVisible();
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
    const seeded = await seedAnalyzerReviewMicrobiologyCase(
      page,
      SOURCE_ANALYZER,
    );
    const unmatched = await submitUnmatchedAstAnalyzerResults(seeded);

    await page.goto("/Dashboard", {
      waitUntil: "domcontentloaded",
    });
    const adminMenu = page.getByRole("button", { name: "Admin", exact: true });
    await expect(adminMenu).toBeVisible({ timeout: LONG_TIMEOUT });
    await adminMenu.click();
    await page
      .getByRole("link", { name: "Stuck analyzer events", exact: true })
      .click();
    await page.waitForURL((url) => {
      return (
        url.pathname === "/AnalyzerResults" &&
        url.searchParams.get("view") === "import-issues"
      );
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
      row.getByRole("link", { name: "Open analyzer results" }),
    ).toHaveAttribute(
      "href",
      `/AnalyzerResults?id=${seeded.analyzerInstrumentId}`,
    );
    await expect(
      page.getByRole("button", { name: /retry|reprocess/i }),
    ).toHaveCount(0);
  });
});
