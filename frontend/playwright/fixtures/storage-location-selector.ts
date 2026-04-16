import { Page, Locator, expect } from "@playwright/test";
import { SHORT_TIMEOUT, UI_TIMEOUT, LONG_TIMEOUT } from "../helpers/timeouts";

/**
 * Page Object for the StorageLocationSelector component tree, covering both
 * user modes:
 *   - Search: click the "Search for location..." combobox to open a tree of
 *     existing rooms/devices/shelves; click a node to select.
 *   - Create: click the "Location" / "Add Location" button to switch to
 *     cascading Room → Device → Shelf → Rack ComboBoxes; type into each
 *     and either pick an existing option or click "Add new".
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 * semantic role-based selectors first (resilient across Carbon updates),
 * data-testid only when role selectors don't uniquely identify the element.
 *
 * The DOM was verified against testing.openelis-global.org via accessibility
 * snapshots in test-results/. The most stable landmarks are:
 *   - role=combobox, name="Search for location..."   — search input
 *   - role=combobox, name="Room" | "Device" | etc.   — cascading levels
 *   - role=button,   name="Location"                 — toggle to create mode
 *   - role=button,   name="Add new"                  — inline-create per level
 *   - role=button,   name="Add"                      — confirm inline create
 *   - role=button,   name="Assign" | "Confirm Move"  — confirm modal
 *
 * Construct via `inLocationManagementModal(page)` to scope inside the
 * "Manage Location" modal (Storage Dashboard, Result Entry), or pass a
 * custom root locator for inline contexts (Order Label, Add Order Sample).
 */
export class StorageLocationSelector {
  readonly page: Page;
  readonly root: Locator;

  constructor(page: Page, root: Locator) {
    this.page = page;
    this.root = root;
  }

  static inLocationManagementModal(page: Page): StorageLocationSelector {
    return new StorageLocationSelector(
      page,
      page.locator('[data-testid="location-management-modal"]'),
    );
  }

  // ── Search flow ───────────────────────────────────────────────────────────

  /**
   * Open the dropdown by clicking the search input. Carbon's TextInput renders
   * as role=textbox (the underlying <input>), and LocationFilterDropdown sets
   * isOpen=true on its onFocus handler.
   */
  async openSearchDropdown() {
    const search = this.root.getByRole("textbox", {
      name: /search for location/i,
    });
    await search.click();
  }

  /** Click an option in the open tree/autocomplete by visible text. */
  async selectLocationByText(text: string | RegExp) {
    await this.page
      .getByRole("button", { name: text })
      .or(this.page.getByRole("option", { name: text }))
      .first()
      .click({ timeout: UI_TIMEOUT });
  }

  // ── Create flow ───────────────────────────────────────────────────────────

  /** Toggle from search to cascading-create mode. */
  async clickAddLocation() {
    await this.root
      .getByRole("button", { name: /^location$/i })
      .or(this.root.getByRole("button", { name: /^add location$/i }))
      .first()
      .click({ timeout: UI_TIMEOUT });
  }

  /**
   * Fill one cascading level. Carbon ComboBox: clicks the combobox by
   * accessible name, types via pressSequentially (Carbon's onChange needs
   * keystroke events, not single fill). Picks an existing option if the
   * dropdown shows it, else clicks the "Add new" button to inline-create.
   */
  private async fillLevel(
    level: "Room" | "Device" | "Shelf" | "Rack",
    name: string,
  ) {
    const combo = this.root.getByRole("combobox", { name: level, exact: true });
    await expect(combo).toBeEnabled({ timeout: UI_TIMEOUT });
    await combo.click();
    await combo.pressSequentially(name, { delay: 20 });

    const existing = this.page.getByRole("option", { name, exact: true });
    if (await existing.isVisible()) {
      await existing.click();
      return;
    }
    // Inline create — there's one "Add new" button per level row, scoped
    // to the row by walking up to the level's combobox parent.
    const row = combo.locator(
      "xpath=ancestor::*[.//button[normalize-space()='Add new']][1]",
    );
    await row.getByRole("button", { name: /^add new$/i }).click();
  }

  /**
   * Fill the cascading form top-down. Pass `{room, device}` minimum
   * (FR-033a), optional shelf/rack. Skips levels not provided.
   */
  async fillCascadingCreate(levels: {
    room?: string;
    device?: string;
    shelf?: string;
    rack?: string;
  }) {
    if (levels.room) await this.fillLevel("Room", levels.room);
    if (levels.device) await this.fillLevel("Device", levels.device);
    if (levels.shelf) await this.fillLevel("Shelf", levels.shelf);
    if (levels.rack) await this.fillLevel("Rack", levels.rack);
  }

  /** Confirm the inline-create form (the "Add" button at the bottom). */
  async confirmInlineCreate() {
    const btn = this.root.getByRole("button", { name: /^add$/i });
    await expect(btn).toBeEnabled({ timeout: UI_TIMEOUT });
    await btn.click();
  }
}

/**
 * Wraps the LocationManagementModal — opens via "Manage Location" overflow,
 * exposes the inner StorageLocationSelector and the modal-level confirm/cancel.
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

  /** Click the "Assign" or "Confirm Move" primary action. */
  async confirm() {
    const btn = this.root
      .getByRole("button", { name: /^assign$/i })
      .or(this.root.getByRole("button", { name: /^confirm move$/i }));
    await expect(btn).toBeEnabled({ timeout: UI_TIMEOUT });
    await btn.click();
  }

  async cancel() {
    await this.root.getByRole("button", { name: /^cancel$/i }).click();
  }
}

/**
 * Storage Dashboard helper: find an unassigned sample row, click its
 * "Sample actions" overflow menu → "Manage Location", return the open modal.
 */
export async function openManageLocationForUnassignedSample(
  page: Page,
): Promise<LocationManagementModal> {
  const table = page.locator("table").first();
  await expect(table.locator("tbody tr").first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  const rows = table.locator("tbody tr");
  const total = await rows.count();

  for (let i = 0; i < total; i++) {
    const row = rows.nth(i);
    const text = (await row.textContent()) ?? "";
    if (text.includes(">")) continue; // already-assigned rows show "Room > Device > ..."

    await row.getByRole("button", { name: /sample actions/i }).click();
    await page
      .getByRole("menuitem", { name: /manage location/i })
      .click({ timeout: SHORT_TIMEOUT });

    const modal = new LocationManagementModal(page);
    await modal.expectOpen();
    return modal;
  }

  throw new Error(
    `No unassigned sample item found in ${total} rows — seed at least one ` +
      `SampleItem without a storage assignment so this regression spec can run.`,
  );
}

/** Unique name per run so create-flow tests don't collide on retries. */
export function uniqueLocationName(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}`;
}
