import { Page, Locator, expect } from "@playwright/test";
import {
  QUICK_TIMEOUT,
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../helpers/timeouts";

/**
 * Shared Page Object for the `<StorageLocationSelector />` component tree.
 *
 * This selector appears in four places (each represented by its own spec):
 *   1. Storage Dashboard → Sample Items tab (via `LocationManagementModal`)
 *   2. Order workflow → Step 3 "Label & Store" (inline, legacy autocomplete mode)
 *   3. Add Order → Sample Type step (inline, two-tier design)
 *   4. Result Entry → expandable search-result row
 *
 * The selector has two user modes:
 *   - **Search** (default): a tree-view / autocomplete input (`location-filter-dropdown`)
 *     where the user clicks and picks an existing Room → Device → Shelf path.
 *   - **Create** (toggle via "Add Location" button): cascading Carbon ComboBoxes
 *     (`room-combobox`, `device-combobox`, `shelf-combobox`, `rack-combobox`)
 *     that allow inline creation of new hierarchy levels as the user types.
 *
 * This POM encapsulates both flows so every site's spec can express the user
 * story without re-deriving selectors.
 *
 * Selector source of truth (lines verified in develop):
 *   - `storage-location-selector`           StorageLocationSelector.jsx:165,188
 *   - `location-management-modal`           LocationManagementModal.jsx:629
 *   - `location-search-and-create`          LocationSearchAndCreate.jsx:497
 *   - `add-location-button` (search→create) LocationSearchAndCreate.jsx:522
 *   - `location-create-container`           LocationSearchAndCreate.jsx:534
 *   - `add-location-create-button` (confirm create) LocationSearchAndCreate.jsx:566
 *   - `location-filter-dropdown`            LocationFilterDropdown.jsx:87
 *   - `location-tree-view`                  LocationTreeView.jsx:171
 *   - `location-autocomplete`               LocationAutocomplete.jsx:74
 *   - `room-combobox` / `device-combobox` / `shelf-combobox` / `rack-combobox`
 *                                            EnhancedCascadingMode.jsx:1487,1580,1665,1754
 *   - `assign-button` / `confirm-move-button` LocationManagementModal.jsx:1001
 *   - `sample-actions-overflow-menu`        SampleActionsOverflowMenu.jsx:60
 *   - `manage-location-menu-item`           SampleActionsOverflowMenu.jsx:68
 *   - `dispose-menu-item`                   SampleActionsOverflowMenu.jsx:76
 */
export class StorageLocationSelector {
  readonly page: Page;
  /**
   * Root of the selector. Scope the POM to a specific instance on the page
   * (e.g. a modal, a row, or the Order Label form) by passing a root locator.
   * Defaults to the first `storage-location-selector` on the page.
   */
  readonly root: Locator;

  constructor(page: Page, root?: Locator) {
    this.page = page;
    this.root =
      root ?? page.locator('[data-testid="storage-location-selector"]').first();
  }

  /**
   * Scope a POM to the location management modal (used by Storage Dashboard
   * and Result Entry workflows).
   */
  static inLocationManagementModal(page: Page): StorageLocationSelector {
    const modal = page.locator('[data-testid="location-management-modal"]');
    return new StorageLocationSelector(
      page,
      modal.locator('[data-testid="storage-location-selector"]'),
    );
  }

  // ── Visibility ────────────────────────────────────────────────────────────

  async expectVisible() {
    await expect(this.root).toBeVisible({ timeout: UI_TIMEOUT });
  }

  // ── Mode toggles ──────────────────────────────────────────────────────────

  get searchWrapper(): Locator {
    return this.root.locator('[data-testid="location-search-and-create"]');
  }

  get createWrapper(): Locator {
    return this.root.locator('[data-testid="location-create-container"]');
  }

  /**
   * Click the "Add Location" button that toggles from search-mode to
   * cascading-create mode. In the Move Sample modal this button renders with
   * the text "Location" and a `+` icon.
   */
  async clickAddLocation() {
    const btn = this.root
      .locator('[data-testid="add-location-button"]')
      .first();
    await expect(btn).toBeVisible({ timeout: SHORT_TIMEOUT });
    await btn.click();
    await expect(this.createWrapper).toBeVisible({ timeout: UI_TIMEOUT });
  }

  // ── Search flow ───────────────────────────────────────────────────────────

  /**
   * Open the tree-view/autocomplete dropdown that lists existing locations.
   * Clicks the search input to trigger `isOpen=true` on LocationFilterDropdown.
   */
  async openSearchDropdown() {
    const filter = this.root
      .locator('[data-testid="location-filter-dropdown"]')
      .first();
    await expect(filter).toBeVisible({ timeout: UI_TIMEOUT });
    // The filter input itself — click to open the tree view.
    await filter.locator("input").first().click();
    // Tree view appears when the input gains focus.
    await expect(
      this.page.locator('[data-testid="location-tree-view"]'),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  }

  /**
   * Type into the search input to trigger autocomplete mode (≥2 chars).
   */
  async typeSearch(query: string) {
    const filter = this.root
      .locator('[data-testid="location-filter-dropdown"]')
      .first();
    await filter.locator("input").first().fill(query);
    await expect(
      this.page.locator('[data-testid="location-autocomplete"]'),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  }

  /**
   * Pick a location option by visible text from the tree view or autocomplete.
   * Works whether the user opened the tree or is mid-search.
   */
  async selectLocationByText(text: string | RegExp) {
    const option = this.page
      .locator(
        '[data-testid="location-tree-view"], [data-testid="location-autocomplete"]',
      )
      .getByText(text)
      .first();
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();
  }

  // ── Create flow (cascading ComboBoxes) ────────────────────────────────────

  /**
   * Fill one level of the cascading create form. Each level (room/device/
   * shelf/rack) shares the same DOM shape — a ComboBox with input + an
   * "add-new-<level>-button" sibling. We type via pressSequentially() rather
   * than fill() because Carbon ComboBox listens to keystroke events; fill()
   * triggers a single input event that the ComboBox doesn't process for
   * filtering. After typing, click the matching option if visible, else
   * fall back to the inline-create affordance.
   */
  private async fillLevelCombobox(
    level: "room" | "device" | "shelf" | "rack",
    name: string,
  ) {
    const combo = this.createWrapper
      .locator(`[data-testid="${level}-combobox"]`)
      .getByRole("combobox");
    await expect(combo).toBeVisible({ timeout: UI_TIMEOUT });
    await combo.click();
    await combo.pressSequentially(name, { delay: 20 });
    const existing = this.page.getByRole("option", { name, exact: true });
    if (await existing.isVisible()) {
      await existing.click();
      return;
    }
    await this.createWrapper
      .locator(`[data-testid="add-new-${level}-button"]`)
      .click();
  }

  /**
   * Fill the cascading create form top-down with either existing or new
   * names. Pass `{room, device}` at minimum (FR-033a — 2-level floor).
   * Missing levels are skipped.
   */
  async fillCascadingCreate(levels: {
    room?: string;
    device?: string;
    shelf?: string;
    rack?: string;
  }) {
    if (levels.room) await this.fillLevelCombobox("room", levels.room);
    if (levels.device) await this.fillLevelCombobox("device", levels.device);
    if (levels.shelf) await this.fillLevelCombobox("shelf", levels.shelf);
    if (levels.rack) await this.fillLevelCombobox("rack", levels.rack);
  }

  /**
   * Confirm the cascading inline creation — clicks the "Add" button inside
   * the create container.
   */
  async confirmInlineCreate() {
    const btn = this.createWrapper.locator(
      '[data-testid="add-location-create-button"]',
    );
    await expect(btn).toBeEnabled({ timeout: UI_TIMEOUT });
    await btn.click();
  }
}

/**
 * Page Object for the `LocationManagementModal` (opens when a user clicks
 * "Manage Location" from the Sample Items overflow menu, or when the Order
 * workflow triggers the two-tier expand modal). Encapsulates the outer modal
 * and the final confirm button — delegate to `StorageLocationSelector` for the
 * inner selector flow.
 */
export class LocationManagementModal {
  readonly page: Page;
  readonly root: Locator;
  readonly selector: StorageLocationSelector;

  constructor(page: Page) {
    this.page = page;
    this.root = page.locator('[data-testid="location-management-modal"]');
    this.selector = StorageLocationSelector.inLocationManagementModal(page);
  }

  async expectOpen() {
    await expect(this.root).toBeVisible({ timeout: LONG_TIMEOUT });
  }

  async expectClosed() {
    await expect(this.root).toBeHidden({ timeout: UI_TIMEOUT });
  }

  /**
   * Click the primary confirm button — either "Assign" (new assignment) or
   * "Confirm Move" (existing assignment being moved). Both share the same
   * data-testid at runtime; match either.
   */
  async confirm() {
    const btn = this.root.locator(
      '[data-testid="assign-button"], [data-testid="confirm-move-button"]',
    );
    await expect(btn).toBeEnabled({ timeout: UI_TIMEOUT });
    await btn.click();
  }

  async cancel() {
    const btn = this.root.locator('button:has-text("Cancel")').first();
    await btn.click({ timeout: SHORT_TIMEOUT });
  }
}

/**
 * Helper for Storage Dashboard → Sample Items table. Finds an unassigned row
 * (no location string with ">") and opens its overflow menu → "Manage Location".
 */
export async function openManageLocationForUnassignedSample(
  page: Page,
): Promise<LocationManagementModal> {
  const table = page.locator("table").first();
  await expect(table).toBeVisible({ timeout: LONG_TIMEOUT });

  const rows = table.locator("tbody tr");
  const total = await rows.count();

  for (let i = 0; i < total; i++) {
    const row = rows.nth(i);
    const text = (await row.textContent()) ?? "";
    if (text.includes(">")) continue; // already assigned — skip

    await row
      .locator('[data-testid="sample-actions-overflow-menu"] button')
      .first()
      .click();
    await page
      .locator('[data-testid="manage-location-menu-item"]')
      .first()
      .click({ timeout: SHORT_TIMEOUT });

    const modal = new LocationManagementModal(page);
    await modal.expectOpen();
    return modal;
  }

  throw new Error(
    `No unassigned sample item found in ${total} rows — Track A tests need seed data with at least one sample without a storage location.`,
  );
}

/**
 * Generate a unique location name for create-flow tests so runs don't collide.
 */
export function uniqueLocationName(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}`;
}
