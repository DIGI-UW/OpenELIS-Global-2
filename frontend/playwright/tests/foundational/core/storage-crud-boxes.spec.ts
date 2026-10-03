import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { StorageManagement } from "../../../fixtures/storage-management";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Storage CRUD — Boxes.
 *
 * Boxes sit at the bottom of the hierarchy and require a parent Rack to
 * exist before they can be created. The "add" flows therefore have a hard
 * precondition: the current environment must already contain at least one
 * rack. If none are present the rack Dropdown renders empty and the test
 * fails loudly on the picker assertion with an actionable message — it does
 * NOT skip.
 *
 * Creating and editing a box are both modals on the Storage Management
 * dashboard now; the /Storage/boxes/new and /Storage/boxes/{id}/edit pages
 * are gone. The grid presets and the custom rows/columns escape hatch moved
 * into the Add modal unchanged; Edit offers the dimensions without presets,
 * exactly as the old edit page did.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 *   - getByRole / getByLabel first
 *   - Carbon Dropdowns use the #id + listbox trigger CSS pattern
 *     (explicitly allowed in the guide for Carbon structural elements)
 *   - dialog-scoped lookups for modals
 */

/**
 * Backend enforces MAX_CODE_LENGTH = 10 in CodeValidationServiceImpl and
 * storage_box.code is VARCHAR(10) (same constraint as rooms/devices/etc).
 * Pattern is ^[A-Z0-9][A-Z0-9_-]*$ after server-side uppercase normalization.
 * 2-char prefix + 6-char base36 slice = 8 chars, well under the cap.
 */
function makeShortCode(prefix: string): string {
  const slice = Date.now().toString(36).slice(-6).toUpperCase();
  return `${prefix}${slice}`;
}

const NO_RACK_MESSAGE =
  "At least one rack must exist for the Box CRUD specs to run — " +
  "seed a rack (room→device→shelf→rack) before exercising this flow.";

async function openAddBoxModal(
  storage: StorageManagement,
  boxLabel: string,
  boxCode: string,
): Promise<Locator> {
  const dialog = await storage.openAddModal("Add Box");
  await dialog.getByLabel("Label", { exact: true }).fill(boxLabel);
  await dialog.getByLabel("Code", { exact: true }).fill(boxCode);
  await storage.selectFirstDropdownOption(
    dialog,
    "storage-add-modal-parent",
    NO_RACK_MESSAGE,
  );
  return dialog;
}

async function expectBoxCreated(
  storage: StorageManagement,
  dialog: Locator,
  boxLabel: string,
) {
  await dialog.getByRole("button", { name: "Create", exact: true }).click();
  await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
  // The container refreshes the table in place rather than navigating.
  await expect(storage.page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
  await storage.expectLevelSelected("boxes");
  await expect(storage.row(boxLabel)).toBeVisible({ timeout: LONG_TIMEOUT });
}

async function createBox(
  page: Page,
  suffix: string,
  preset = "8x12 (96-well plate)",
) {
  const storage = new StorageManagement(page);
  const boxLabel = `PW Box ${suffix}`;
  const boxCode = makeShortCode("PB");

  await test.step(`create box "${boxLabel}" from the Add Box modal`, async () => {
    await storage.gotoLevel("boxes");
    const dialog = await openAddBoxModal(storage, boxLabel, boxCode);
    await storage.selectDropdownOption(
      dialog,
      "storage-add-modal-grid",
      preset,
    );
    await expectBoxCreated(storage, dialog, boxLabel);
  });

  return { storage, boxLabel, boxCode };
}

test.describe("Storage CRUD — Boxes", () => {
  test("add box flow with preset dimensions", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-preset`;
    const { storage, boxLabel } = await createBox(
      page,
      suffix,
      "8x12 (96-well plate)",
    );
    // Capacity is rows x columns, so the row proves the preset was applied.
    await expect(
      storage.row(boxLabel).getByRole("cell", { name: "96", exact: true }),
    ).toBeVisible();
  });

  test("add box flow with custom dimensions", async ({ page }) => {
    const storage = new StorageManagement(page);
    const suffix = `${Date.now().toString(36)}-custom`;
    const boxLabel = `PW Box ${suffix}`;
    const boxCode = makeShortCode("PC");

    await storage.gotoLevel("boxes");
    const dialog = await openAddBoxModal(storage, boxLabel, boxCode);

    await test.step("Custom unlocks the rows/columns inputs", async () => {
      const rows = dialog.getByLabel("Rows", { exact: true });
      const columns = dialog.getByLabel("Columns", { exact: true });
      // Presets own the dimensions; only "Custom" hands them to the user.
      await expect(rows).toBeDisabled();
      await expect(columns).toBeDisabled();

      await storage.selectDropdownOption(
        dialog,
        "storage-add-modal-grid",
        "Custom",
      );

      await expect(rows).toBeEnabled();
      await rows.fill("5");
      await columns.fill("7");
    });

    await test.step("box is created with the custom grid", async () => {
      await expectBoxCreated(storage, dialog, boxLabel);
      // Capacity is rows x columns, so the row proves the grid was applied.
      await expect(
        storage.row(boxLabel).getByRole("cell", { name: "35", exact: true }),
      ).toBeVisible();
    });
  });

  test("edit box flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { storage, boxLabel } = await createBox(page, suffix);

    await test.step("edit modal opens preloaded with the box's grid", async () => {
      await storage.gotoLevel("boxes");
      const dialog = await storage.openEditModal(boxLabel, "Edit Box");
      const rows = dialog.getByLabel("Rows", { exact: true });
      await expect(rows).toHaveValue("8");
      await expect(dialog.getByLabel("Columns", { exact: true })).toHaveValue(
        "12",
      );
      // Presets are an Add-time convenience; Edit hands over the dimensions.
      await expect(dialog.locator("#storage-edit-modal-grid")).toHaveCount(0);

      await rows.fill("4");
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
    });

    await test.step("the resized grid shows in the table as a new capacity", async () => {
      // The container refreshes the table in place rather than navigating.
      await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
      // Capacity is rows x columns, so the row proves the resize was applied.
      await expect(
        storage.row(boxLabel).getByRole("cell", { name: "48", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("the resize survives a reload, so it really persisted", async () => {
      await storage.gotoLevel("boxes");
      await expect(
        storage.row(boxLabel).getByRole("cell", { name: "48", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });
  });

  test("delete box flow with validation handling", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { storage, boxLabel } = await createBox(page, suffix);

    await test.step("open delete modal", async () => {
      await storage.gotoLevel("boxes");
      await storage.openRowActions(boxLabel);
      await page.getByRole("menuitem", { name: "Delete" }).click();
    });

    const dialog = page.getByRole("dialog", { name: "Delete Location" });

    await test.step("confirm and delete", async () => {
      await expect(dialog).toBeVisible();
      // Boxes are leaves: no cascade summary, no confirmation checkbox.
      await dialog.getByRole("button", { name: "Delete" }).click();
      await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
    });

    await test.step("row removed from listing", async () => {
      await expect(storage.row(boxLabel)).toHaveCount(0);
    });
  });
});
