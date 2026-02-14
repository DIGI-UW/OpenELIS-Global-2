import { test, expect } from "@playwright/test";
import { NotebookWorkflow } from "../fixtures/notebook-workflow";

test.describe("Notebook workflow (smoke)", () => {
  async function openWorkflowOrSkip(workflow: NotebookWorkflow) {
    await workflow.gotoDashboard();
    const hasEntry = await workflow.openFirstNotebookEntry();
    test.skip(!hasEntry, "No notebook entries are available for this run");
    await workflow.openWorkflowTab();
  }

  test("loads workflow shell with navigation and page header", async ({
    page,
  }) => {
    const workflow = new NotebookWorkflow(page);
    await openWorkflowOrSkip(workflow);

    await expect(workflow.pageItems.first()).toBeVisible();
    await expect(workflow.activePageItem).toBeVisible();
    await expect(workflow.pageHeader).toBeVisible();
    await expect(workflow.navigationSummary).toBeVisible();
  });

  test("can navigate to next workflow page", async ({ page }) => {
    const workflow = new NotebookWorkflow(page);
    await openWorkflowOrSkip(workflow);

    const totalPages = await workflow.pageItems.count();
    test.skip(totalPages < 2, "Workflow has fewer than two pages");

    const firstTitle = (await workflow.pageHeader.textContent())?.trim() || "";
    await workflow.clickPage(1);
    await expect(workflow.activePageItem).toContainText(/Page 2|2/);
    await expect(workflow.pageHeader).toBeVisible();

    const secondTitle = (await workflow.pageHeader.textContent())?.trim() || "";
    expect(secondTitle.length).toBeGreaterThan(0);
    expect(secondTitle === firstTitle).toBeFalsy();
  });

  test("shows page progress on active workflow page", async ({ page }) => {
    const workflow = new NotebookWorkflow(page);
    await openWorkflowOrSkip(workflow);

    const progress = page.locator(".page-progress");
    await expect(progress).toBeVisible();
    await expect(progress).toContainText(/\/\d+/);
  });
});
