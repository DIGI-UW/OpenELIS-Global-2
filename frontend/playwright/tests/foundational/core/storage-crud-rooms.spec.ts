import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { StorageManagement } from "../../../fixtures/storage-management";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Storage CRUD — Rooms.
 *
 * Rooms are the top of the storage hierarchy, so these flows are
 * self-seeding: each test creates its own room through the UI, then
 * operates on that row. No fixture preconditions required.
 *
 * Creating and editing a location are both modals on the Storage Management
 * dashboard now; the /Storage/rooms/new and /Storage/rooms/{id}/edit pages
 * are gone.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 *   - getByRole / getByLabel first
 *   - dialog-scoped lookups for modal content
 *   - click the <label> for Carbon Checkboxes (the <input> is
 *     visually-hidden; .check()/getByLabel().check() fails actionability)
 */

/**
 * Backend enforces MAX_CODE_LENGTH = 10 in CodeValidationServiceImpl and
 * storage_room.code is VARCHAR(10) (Liquibase 012-update-code-column-length).
 * Pattern is ^[A-Z0-9][A-Z0-9_-]*$ after server-side uppercase normalization.
 * 2-char prefix + 6-char base36 slice = 8 chars, well under the cap and
 * still unique per-millisecond.
 */
function makeShortCode(prefix: string): string {
  const slice = Date.now().toString(36).slice(-6).toUpperCase();
  return `${prefix}${slice}`;
}

async function createRoom(page: Page, suffix: string) {
  const storage = new StorageManagement(page);
  const roomName = `PW Room ${suffix}`;
  const roomCode = makeShortCode("PR");

  await test.step(`create room "${roomName}" from the Add Room modal`, async () => {
    await storage.gotoLevel("rooms");

    const dialog = await storage.openAddModal("Add Room");
    await dialog.getByLabel("Name", { exact: true }).fill(roomName);
    await dialog.getByLabel("Code", { exact: true }).fill(roomCode);
    await dialog.getByRole("button", { name: "Create", exact: true }).click();

    await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
    // The container refreshes the table in place rather than navigating.
    await expect(page).toHaveURL(/\/Storage\/rooms\?t=\d+/);
    await storage.expectLevelSelected("rooms");
    await expect(storage.row(roomName)).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  return { storage, roomName, roomCode };
}

test.describe("Storage CRUD — Rooms", () => {
  test("add room flow", async ({ page }) => {
    const suffix = Date.now().toString(36);
    const { storage, roomName } = await createRoom(page, suffix);
    await expect(storage.row(roomName)).toBeVisible();
  });

  test("edit room flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { storage, roomName } = await createRoom(page, suffix);
    const renamedRoom = `${roomName} Renamed`;

    await test.step("edit modal opens preloaded with the row's values", async () => {
      await storage.gotoLevel("rooms");
      const dialog = await storage.openEditModal(roomName, "Edit Room");
      const nameField = dialog.getByLabel("Name", { exact: true });
      await expect(nameField).toHaveValue(roomName);

      await nameField.fill(renamedRoom);
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
    });

    await test.step("the new name replaces the old one in the table", async () => {
      // The container refreshes the table in place rather than navigating.
      await expect(page).toHaveURL(/\/Storage\/rooms\?t=\d+/);
      await expect(storage.row(renamedRoom)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("the rename survives a reload, so it really persisted", async () => {
      await storage.gotoLevel("rooms");
      await expect(storage.row(renamedRoom)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });
  });

  test("delete room flow with cascade summary", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { storage, roomName } = await createRoom(page, suffix);

    await test.step("open delete confirm modal", async () => {
      await storage.gotoLevel("rooms");
      await storage.openRowActions(roomName);
      await page.getByRole("menuitem", { name: "Delete" }).click();
    });

    const dialog = page.getByRole("dialog", { name: "Delete Room" });

    await test.step("confirm cascade summary renders and Delete is gated", async () => {
      await expect(dialog).toBeVisible();
      await expect(dialog.getByText(/cascade delete warning/i)).toBeVisible();
      await expect(
        dialog.getByRole("button", { name: "Delete" }),
      ).toBeDisabled();
    });

    await test.step("acknowledge cascade checkbox and delete", async () => {
      // Carbon Checkbox hides the <input>; click the associated <label>.
      await dialog.locator('label[for="storage-delete-confirmation"]').click();
      const deleteButton = dialog.getByRole("button", { name: "Delete" });
      await expect(deleteButton).toBeEnabled();
      await deleteButton.click();
    });

    await test.step("row removed from listing", async () => {
      await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
      await expect(storage.row(roomName)).toHaveCount(0);
    });
  });
});
