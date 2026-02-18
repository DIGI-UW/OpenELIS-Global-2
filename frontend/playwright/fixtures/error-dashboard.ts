import { Page, expect, Locator } from "@playwright/test";

/**
 * ErrorDashboard Page Object
 *
 * Encapsulates interactions with the /analyzers/errors page (ErrorDashboard component).
 * Uses data-testid selectors that match the component's DOM structure.
 */
export class ErrorDashboardPage {
  readonly page: Page;
  readonly root: Locator;
  readonly header: Locator;
  readonly acknowledgeAllButton: Locator;
  readonly statsGrid: Locator;
  readonly filtersSection: Locator;
  readonly searchInput: Locator;
  readonly tableContainer: Locator;
  readonly table: Locator;

  constructor(page: Page) {
    this.page = page;
    this.root = page.locator('[data-testid="error-dashboard"]');
    this.header = page.locator('[data-testid="error-dashboard-header"]');
    this.acknowledgeAllButton = page.locator(
      '[data-testid="acknowledge-all-button"]',
    );
    this.statsGrid = page.locator('[data-testid="error-dashboard-stats"]');
    this.filtersSection = page.locator(
      '[data-testid="error-dashboard-filters"]',
    );
    this.searchInput = page.locator('[data-testid="error-search-input"]');
    this.tableContainer = page.locator('[data-testid="error-table-container"]');
    this.table = page.locator('[data-testid="error-table"]');
  }

  /** Navigate to the error dashboard page */
  async goto() {
    await this.page.goto("/analyzers/errors");
  }

  /** Assert the page has loaded (root + header + stats visible) */
  async expectLoaded() {
    await expect(this.root).toBeVisible();
    await expect(this.header).toBeVisible();
    await expect(this.statsGrid).toBeVisible();
  }

  /** Get a stat tile value by testid suffix */
  async getStatValue(
    stat: "total" | "unacknowledged" | "critical" | "last24hours",
  ): Promise<string> {
    const tile = this.page.locator(`[data-testid="stat-${stat}"]`);
    const value = tile.locator(".stat-value");
    return (await value.textContent()) || "0";
  }

  /** Get the number of rows in the error table */
  async getRowCount(): Promise<number> {
    const rows = this.table.locator("tbody tr");
    return rows.count();
  }

  /** Get a specific error row by its ID */
  getRow(id: string): Locator {
    return this.page.locator(`[data-testid="error-row-${id}"]`);
  }

  /** Type into the search input */
  async search(term: string) {
    await this.searchInput.locator("input").fill(term);
  }
}
