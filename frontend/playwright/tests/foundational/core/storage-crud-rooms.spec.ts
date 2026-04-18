import { test, expect } from "../../../helpers/test-base";

async function createRoom(page, suffix) {
  const roomName = `PW Room ${suffix}`;
  const roomCode = `PR-${suffix}`;

  await page.goto("/Storage/rooms/new", { waitUntil: "domcontentloaded" });
  await expect(page).toHaveURL(/\/Storage\/rooms\/new/);
  await expect(page.locator("#storage-add-name")).toBeVisible();

  await page.locator("#storage-add-name").fill(roomName);
  await page.locator("#storage-add-code").fill(roomCode);
  await page.getByRole("button", { name: "Add" }).click();

  await expect(page).toHaveURL(/\/Storage\/rooms\?t=\d+/);
  await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
  await expect(page.locator("tbody tr", { hasText: roomName })).toBeVisible();

  return { roomName, roomCode };
}

async function openRowActions(page, rowText) {
  const row = page.locator("tbody tr", { hasText: rowText });
  await expect(row).toBeVisible();
  const overflowMenu = row.locator(".cds--overflow-menu");
  await expect(overflowMenu).toBeVisible();
  await overflowMenu.click();
}

test.describe("Storage CRUD - Rooms", () => {
  test("add room flow", async ({ page }) => {
    const suffix = Date.now().toString(36);
    const { roomName } = await createRoom(page, suffix);
    await expect(page.locator("tbody tr", { hasText: roomName })).toBeVisible();
  });

  test("edit room flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { roomName } = await createRoom(page, suffix);

    await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
    await openRowActions(page, roomName);
    await page.getByRole("menuitem", { name: "Edit" }).click();

    await expect(page).toHaveURL(/\/Storage\/rooms\/\d+\/edit/);
    await expect(page.getByRole("heading", { name: /Edit/i })).toBeVisible();
  });

  test("delete room flow with cascade summary", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { roomName } = await createRoom(page, suffix);

    await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
    await openRowActions(page, roomName);
    await page.getByRole("menuitem", { name: "Delete" }).click();

    await expect(page.getByText("Cascade Delete Warning")).toBeVisible();

    const deleteButton = page.getByRole("button", { name: "Delete" });
    await expect(deleteButton).toBeDisabled();

    await page
      .getByLabel(
        "I confirm that I want to delete this location and all its child locations. All samples will be unassigned. This action cannot be undone.",
      )
      .check();
    await expect(deleteButton).toBeEnabled();

    await deleteButton.click();
    await expect(page.getByText("Cascade Delete Warning")).toBeHidden();
    await expect(page.locator("tbody tr", { hasText: roomName })).toHaveCount(
      0,
    );
  });
});
