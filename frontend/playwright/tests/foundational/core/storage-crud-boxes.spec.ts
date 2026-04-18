import { test, expect } from "../../../helpers/test-base";

async function selectFirstRack(page) {
  await page
    .locator("#box-add-rack button.cds--list-box__field")
    .click({ timeout: 10000 });
  await page.getByRole("option").first().click({ timeout: 10000 });
}

async function createBox(page, suffix, preset = "8x12 (96-well plate)") {
  const boxLabel = `PW Box ${suffix}`;
  const boxCode = `PB-${suffix}`;

  await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
  await expect(page).toHaveURL(/\/Storage\/boxes\/new/);
  await expect(page.locator("#box-add-label")).toBeVisible();

  await page.locator("#box-add-label").fill(boxLabel);
  await page.locator("#box-add-code").fill(boxCode);
  await selectFirstRack(page);
  await page.locator("#box-add-preset button.cds--list-box__field").click();
  await page.getByRole("option", { name: preset }).click();
  await page.getByRole("button", { name: "Add" }).click();

  await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
  await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
  await expect(page.locator("tbody tr", { hasText: boxLabel })).toBeVisible();

  return { boxLabel, boxCode };
}

async function openBoxRowActions(page, rowText) {
  const row = page.locator("tbody tr", { hasText: rowText });
  await expect(row).toBeVisible();
  const overflowMenu = row.locator(".cds--overflow-menu");
  await expect(overflowMenu).toBeVisible();
  await overflowMenu.click();
}

test.describe("Storage CRUD - Boxes", () => {
  test("add box flow with preset dimensions", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-preset`;
    const { boxLabel } = await createBox(page, suffix, "8x12 (96-well plate)");
    await expect(page.locator("tbody tr", { hasText: boxLabel })).toBeVisible();
  });

  test("add box flow with custom dimensions", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-custom`;
    const boxLabel = `PW Box ${suffix}`;
    const boxCode = `PC-${suffix}`;

    await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/boxes\/new/);
    await expect(page.locator("#box-add-label")).toBeVisible();

    await page.locator("#box-add-label").fill(boxLabel);
    await page.locator("#box-add-code").fill(boxCode);
    await selectFirstRack(page);
    await page.locator("#box-add-preset button.cds--list-box__field").click();
    await page.getByRole("option", { name: "Custom" }).click();
    await page.locator("#box-add-rows").fill("5");
    await page.locator("#box-add-columns").fill("7");
    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
    await expect(page.locator("tbody tr", { hasText: boxLabel })).toBeVisible();
  });

  test("edit box flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { boxLabel } = await createBox(page, suffix);

    await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
    await openBoxRowActions(page, boxLabel);
    await page.getByRole("menuitem", { name: "Edit" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\/\d+\/edit/);
    await expect(page.getByRole("heading", { name: /Edit/i })).toBeVisible();
  });

  test("delete box flow with validation handling", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { boxLabel } = await createBox(page, suffix);

    await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
    await openBoxRowActions(page, boxLabel);
    await page.getByRole("menuitem", { name: "Delete" }).click();

    await expect(page.getByText("Delete Location")).toBeVisible();
    await page.getByRole("button", { name: "Delete" }).click();
    await expect(page.getByText("Delete Location")).toBeHidden();
    await expect(page.locator("tbody tr", { hasText: boxLabel })).toHaveCount(
      0,
    );
  });
});
