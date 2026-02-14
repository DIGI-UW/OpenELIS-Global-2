import { expect, Locator, Page } from "@playwright/test";

/**
 * Notebook workflow smoke fixture.
 *
 * This fixture intentionally focuses on a stable user path:
 * Dashboard -> first notebook entry -> Workflow tab.
 */
export class NotebookWorkflow {
  readonly page: Page;
  readonly workflowContainer: Locator;
  readonly pageItems: Locator;
  readonly activePageItem: Locator;
  readonly pageHeader: Locator;
  readonly navigationSummary: Locator;

  constructor(page: Page) {
    this.page = page;
    this.workflowContainer = page.locator(".notebook-workflow-container");
    this.pageItems = page.locator(".page-item");
    this.activePageItem = page.locator(".page-item.active");
    this.pageHeader = page.locator(".page-header h3");
    this.navigationSummary = page.locator(".navigation-summary");
  }

  async gotoDashboard() {
    await this.page.goto("/NoteBookDashboard");
    await expect(this.page).toHaveURL(/\/NoteBookDashboard/);
    await this.page.waitForLoadState("networkidle");
  }

  /**
   * Opens the first visible notebook entry.
   *
   * Returns false if no entries are available for the environment.
   */
  async openFirstNotebookEntry(): Promise<boolean> {
    const tiles = this.page.locator(".notebook-dashboard-tile");
    await this.page.waitForLoadState("networkidle");
    if ((await tiles.count()) === 0) {
      return false;
    }

    const firstTile = tiles.first();
    const viewButton = firstTile.getByRole("button", { name: /view/i });
    if ((await viewButton.count()) > 0) {
      await viewButton.first().click();
    } else {
      // Fallback for environments where role text differs.
      await firstTile.locator(".notebook-tile-buttons button").first().click();
    }

    await expect(this.page).toHaveURL(/\/NoteBookInstanceEditForm\/\d+/);
    return true;
  }

  async openWorkflowTab() {
    const workflowTab = this.page.getByRole("button", { name: /workflow/i });
    if ((await workflowTab.count()) > 0) {
      await workflowTab.first().click();
    } else {
      // Content switcher order: Content, Attachments, Workflow, ...
      await this.page.locator(".cds--content-switcher-btn").nth(2).click();
    }
    await expect(this.workflowContainer).toBeVisible();
  }

  async clickPage(index: number) {
    await this.pageItems.nth(index).click();
    await expect(this.pageItems.nth(index)).toHaveClass(/active/);
  }
}
