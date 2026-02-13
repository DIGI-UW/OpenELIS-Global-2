import { Page, expect, Locator } from "@playwright/test";

/**
 * AnalyzersList Page Object
 *
 * Encapsulates interactions with the /analyzers page (AnalyzersList component).
 * Uses data-testid selectors that match the component's DOM structure.
 */
export class AnalyzerListPage {
  readonly page: Page;
  readonly root: Locator;
  readonly header: Locator;
  readonly addButton: Locator;
  readonly statsGrid: Locator;
  readonly searchInput: Locator;
  readonly tableContainer: Locator;
  readonly table: Locator;

  constructor(page: Page) {
    this.page = page;
    this.root = page.locator('[data-testid="analyzers-list"]');
    this.header = page.locator('[data-testid="analyzers-list-header"]');
    this.addButton = page.locator('[data-testid="add-analyzer-button"]');
    this.statsGrid = page.locator('[data-testid="analyzers-list-stats"]');
    this.searchInput = page.locator('[data-testid="analyzer-search-input"]');
    this.tableContainer = page.locator(
      '[data-testid="analyzers-table-container"]',
    );
    this.table = page.locator('[data-testid="analyzers-table"]');
  }

  /** Navigate to the analyzers list page */
  async goto() {
    await this.page.goto("/analyzers");
  }

  /** Assert the page has loaded (root + header + stats visible) */
  async expectLoaded() {
    await expect(this.root).toBeVisible();
    await expect(this.header).toBeVisible();
    await expect(this.statsGrid).toBeVisible();
  }

  /** Get a stat tile value by testid suffix (total, active, inactive) */
  async getStatValue(stat: "total" | "active" | "inactive"): Promise<string> {
    const tile = this.page.locator(`[data-testid="stat-${stat}"]`);
    const value = tile.locator(".stat-value");
    return (await value.textContent()) || "0";
  }

  /** Get the number of rows in the analyzer table */
  async getRowCount(): Promise<number> {
    const rows = this.table.locator("tbody tr");
    return rows.count();
  }

  /** Get a specific analyzer row by its ID */
  getRow(id: string): Locator {
    return this.page.locator(`[data-testid="analyzer-row-${id}"]`);
  }

  /** Get the analyzer name cell for a row */
  getNameCell(id: string): Locator {
    return this.page.locator(`[data-testid="analyzer-name-${id}"]`);
  }

  /** Get the status badge for an analyzer */
  getStatusBadge(id: string): Locator {
    return this.page.locator(`[data-testid="status-badge-${id}"]`);
  }

  /** Open the overflow menu for a specific analyzer row */
  async openOverflowMenu(id: string) {
    const actionsCell = this.page.locator(
      `[data-testid="analyzer-actions-${id}"]`,
    );
    await actionsCell.locator("button").first().click();
  }

  /** Click an action from an open overflow menu */
  async clickAction(
    id: string,
    action:
      | "mappings"
      | "test-connection"
      | "edit"
      | "delete"
      | "copy-mappings",
  ) {
    const actionItem = this.page.locator(
      `[data-testid="analyzer-action-${action}-${id}"]`,
    );
    await actionItem.click();
  }

  /** Type into the search input */
  async search(term: string) {
    await this.searchInput.locator("input").fill(term);
  }

  /** Click the Add Analyzer button */
  async clickAdd() {
    await this.addButton.click();
  }
}
