import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";

/**
 * Storage CRUD — Rooms.
 *
 * Rooms are the top of the storage hierarchy, so these flows are
 * self-seeding: each test creates its own room via the UI, then
 * operates on that row. No fixture preconditions required.
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
  const roomName = `PW Room ${suffix}`;
  const roomCode = makeShortCode("PR");

  await test.step(`create room "${roomName}"`, async () => {
    await page.goto("/Storage/rooms/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/rooms\/new/);

    await page.getByLabel("Name", { exact: true }).fill(roomName);
    await page.getByLabel("Code", { exact: true }).fill(roomCode);
    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/rooms\?t=\d+/);
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
    await expect(page.locator("tbody tr", { hasText: roomName })).toBeVisible();
  });

  return { roomName, roomCode };
}

async function openRowActions(page: Page, rowText: string) {
  const row = page.locator("tbody tr", { hasText: rowText });
  await expect(row).toBeVisible();
  const overflowMenu = row.locator(".cds--overflow-menu");
  await expect(overflowMenu).toBeVisible();
  await overflowMenu.click();
}

test.describe("Storage CRUD — Rooms", () => {
  test("add room flow", async ({ page }) => {
    const suffix = Date.now().toString(36);
    const { roomName } = await createRoom(page, suffix);
    await expect(page.locator("tbody tr", { hasText: roomName })).toBeVisible();
  });

  test("edit room flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { roomName } = await createRoom(page, suffix);

    await test.step("open edit page from overflow menu", async () => {
      await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
      await openRowActions(page, roomName);
      await page.getByRole("menuitem", { name: "Edit" }).click();
    });

    await test.step("verify edit page rendered", async () => {
      await expect(page).toHaveURL(/\/Storage\/rooms\/\d+\/edit/);
      await expect(
        page.getByRole("heading", { name: /edit\s+room/i }),
      ).toBeVisible();
    });
  });

  test("delete room flow with cascade summary", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { roomName } = await createRoom(page, suffix);

    await test.step("open delete confirm modal", async () => {
      await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
      await openRowActions(page, roomName);
      await page.getByRole("menuitem", { name: "Delete" }).click();
    });

    const dialog = page.getByRole("dialog");

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
      await expect(dialog).toBeHidden();
      await expect(page.locator("tbody tr", { hasText: roomName })).toHaveCount(
        0,
      );
    });
  });
});
