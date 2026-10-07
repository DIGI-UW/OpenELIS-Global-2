import { Page, Locator, expect } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../helpers/timeouts";

/**
 * Storage Management Page Object — /Storage.
 *
 * A single container at /Storage with three tabs (Dashboard, Sample Items,
 * Inventory Lots). The five hierarchy levels are tiles on the Dashboard tab,
 * each swapping the table below it; /Storage/{level} deep-links to that
 * level. The container owns the breadcrumb and the heading, so the embedded
 * level/sample/lot pages do not render their own.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md.
 * The tiles are Carbon ClickableTiles rendered as <a> without href, so they
 * expose no role — they are located by the app's own class hooks and their
 * selected state is read from aria-pressed, never from a CSS class.
 */

export const STORAGE_LEVELS = [
  "rooms",
  "devices",
  "shelves",
  "racks",
  "boxes",
] as const;

export type StorageLevel = (typeof STORAGE_LEVELS)[number];

export type StorageTab = "Dashboard" | "Sample Items" | "Inventory Lots";

const LEVEL_TILE_LABEL: Record<StorageLevel, string> = {
  rooms: "Rooms",
  devices: "Devices",
  shelves: "Shelves",
  racks: "Racks",
  boxes: "Boxes",
};

export class StorageManagement {
  readonly page: Page;
  readonly heading: Locator;
  readonly breadcrumb: Locator;
  readonly addButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole("heading", {
      level: 2,
      name: "Storage Management",
      exact: true,
    });
    this.breadcrumb = page.getByRole("navigation", { name: /breadcrumb/i });
    this.addButton = page.getByRole("button", { name: "Add", exact: true });
  }

  /** Breadcrumb + heading the container supplies for every tab and level. */
  async expectContainer() {
    await expect(this.heading).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      this.breadcrumb.getByRole("link", { name: "Home", exact: true }),
    ).toBeVisible();
    await expect(
      this.breadcrumb.getByText("Storage Management", { exact: true }),
    ).toBeVisible();
  }

  tab(name: StorageTab): Locator {
    return this.page.getByRole("tab", { name, exact: true });
  }

  async expectTabSelected(name: StorageTab) {
    await expect(this.tab(name)).toHaveAttribute("aria-selected", "true", {
      timeout: UI_TIMEOUT,
    });
  }

  tile(level: StorageLevel): Locator {
    return this.page.locator(
      `.storage-metric-tile:has(.metric-label:text-is("${LEVEL_TILE_LABEL[level]}"))`,
    );
  }

  /** Exactly one level tile is pressed at a time. */
  async expectLevelSelected(level: StorageLevel) {
    for (const candidate of STORAGE_LEVELS) {
      await expect(this.tile(candidate)).toHaveAttribute(
        "aria-pressed",
        candidate === level ? "true" : "false",
        { timeout: UI_TIMEOUT },
      );
    }
  }

  /** Deep-link to a level and confirm the container resolved onto it. */
  async gotoLevel(level: StorageLevel) {
    await this.page.goto(`/Storage/${level}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(this.page).toHaveURL(new RegExp(`/Storage/${level}`), {
      timeout: LONG_TIMEOUT,
    });
    await this.expectContainer();
    await this.expectTabSelected("Dashboard");
    await this.expectLevelSelected(level);
  }

  /** Switch levels the way a user does: by clicking the tile. */
  async selectLevel(level: StorageLevel) {
    await this.tile(level).click();
    await expect(this.page).toHaveURL(new RegExp(`/Storage/${level}`), {
      timeout: UI_TIMEOUT,
    });
    await this.expectLevelSelected(level);
  }

  /**
   * Named column headers, in order — identifies which level is shown.
   *
   * Reads the Carbon header label span rather than the <th>: a sortable
   * <th> also contains a visually-hidden "Click to sort rows by ..."
   * description, and the row-actions column is unnamed.
   */
  get columnHeaders(): Locator {
    return this.page
      .locator("table th .cds--table-header-label")
      .filter({ hasText: /\S/ });
  }

  row(text: string): Locator {
    return this.page.locator("tbody tr", { hasText: text });
  }

  async openRowActions(text: string) {
    const row = this.row(text);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    const actions = row.getByRole("button", { name: "Options", exact: true });
    await expect(actions).toBeVisible();
    await actions.click();
  }

  /**
   * Creating a location is a modal now, not a /Storage/{level}/new page.
   * `heading` is the modal heading, which Carbon also uses as its aria-label.
   */
  async openAddModal(heading: string): Promise<Locator> {
    await expect(this.addButton).toBeVisible({ timeout: LONG_TIMEOUT });
    await this.addButton.click();
    const dialog = this.page.getByRole("dialog", { name: heading });
    await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
    return dialog;
  }

  /**
   * Editing is a modal now, not a /Storage/{level}/{id}/edit page.
   * `heading` is the modal heading, which Carbon also uses as its aria-label.
   */
  async openEditModal(rowText: string, heading: string): Promise<Locator> {
    await this.openRowActions(rowText);
    await this.page.getByRole("menuitem", { name: "Edit" }).click();
    const dialog = this.page.getByRole("dialog", { name: heading });
    await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
    return dialog;
  }

  /** Carbon Dropdown: the `id` prop lands on the list-box wrapper. */
  async selectDropdownOption(
    scope: Locator,
    dropdownId: string,
    optionName: string | RegExp,
  ) {
    const dropdown = scope.locator(`#${dropdownId}`);
    await dropdown.locator("button.cds--list-box__field").click();
    const option = dropdown.getByRole("option", { name: optionName });
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();
  }

  /**
   * Picks whichever parent the environment happens to have. Fails loudly
   * rather than skipping when the hierarchy above this level is unseeded.
   */
  async selectFirstDropdownOption(
    scope: Locator,
    dropdownId: string,
    emptyMessage: string,
  ) {
    const dropdown = scope.locator(`#${dropdownId}`);
    await dropdown.locator("button.cds--list-box__field").click();
    const firstOption = dropdown.getByRole("option").first();
    await expect(firstOption, emptyMessage).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await firstOption.click();
  }
}
