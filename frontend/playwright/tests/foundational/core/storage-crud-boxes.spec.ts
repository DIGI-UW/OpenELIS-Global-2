import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";

/**
 * Storage CRUD — Boxes.
 *
 * Boxes sit at the bottom of the hierarchy and require a parent Rack
 * to exist before they can be created. The "add" flows therefore have
 * a hard precondition: the current environment must already contain
 * at least one rack. If none are present the rack Dropdown renders
 * empty and the test fails loudly on the picker assertion with an
 * actionable message — it does NOT skip.
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

async function selectFirstRack(page: Page) {
  const rackField = page.locator("#box-add-rack button.cds--list-box__field");
  await expect(rackField).toBeVisible();
  await rackField.click();

  const rackListbox = page.locator("#box-add-rack").getByRole("listbox");
  const firstOption = rackListbox.getByRole("option").first();
  await expect(
    firstOption,
    "At least one rack must exist for the Box CRUD specs to run — " +
      "seed a rack (room→device→shelf→rack) before exercising this flow.",
  ).toBeVisible();
  await firstOption.click();
}

async function createBox(
  page: Page,
  suffix: string,
  preset = "8x12 (96-well plate)",
) {
  const boxLabel = `PW Box ${suffix}`;
  const boxCode = makeShortCode("PB");

  await test.step(`create box "${boxLabel}"`, async () => {
    await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/boxes\/new/);

    await page.getByLabel("Label", { exact: true }).fill(boxLabel);
    await page.getByLabel("Code", { exact: true }).fill(boxCode);
    await selectFirstRack(page);

    await page.locator("#box-add-preset button.cds--list-box__field").click();
    await page
      .locator("#box-add-preset")
      .getByRole("option", { name: preset })
      .click();

    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
    await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
    await expect(page.locator("tbody tr", { hasText: boxLabel })).toBeVisible();
  });

  return { boxLabel, boxCode };
}

async function openBoxRowActions(page: Page, rowText: string) {
  const row = page.locator("tbody tr", { hasText: rowText });
  await expect(row).toBeVisible();
  const overflowMenu = row.locator(".cds--overflow-menu");
  await expect(overflowMenu).toBeVisible();
  await overflowMenu.click();
}

test.describe("Storage CRUD — Boxes", () => {
  test("add box flow with preset dimensions", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-preset`;
    const { boxLabel } = await createBox(page, suffix, "8x12 (96-well plate)");
    await expect(page.locator("tbody tr", { hasText: boxLabel })).toBeVisible();
  });

  test("add box flow with custom dimensions", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-custom`;
    const boxLabel = `PW Box ${suffix}`;
    const boxCode = makeShortCode("PC");

    await test.step("fill out form with custom rows/columns", async () => {
      await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
      await expect(page).toHaveURL(/\/Storage\/boxes\/new/);

      await page.getByLabel("Label", { exact: true }).fill(boxLabel);
      await page.getByLabel("Code", { exact: true }).fill(boxCode);
      await selectFirstRack(page);

      await page.locator("#box-add-preset button.cds--list-box__field").click();
      await page
        .locator("#box-add-preset")
        .getByRole("option", { name: "Custom" })
        .click();

      await page.getByLabel("Rows", { exact: true }).fill("5");
      await page.getByLabel("Columns", { exact: true }).fill("7");
      await page.getByRole("button", { name: "Add" }).click();
    });

    await test.step("verify box appears in listing", async () => {
      await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
      await expect(
        page.locator("tbody tr", { hasText: boxLabel }),
      ).toBeVisible();
    });
  });

  test("edit box flow via overflow menu", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-edit`;
    const { boxLabel } = await createBox(page, suffix);

    await test.step("open edit page from overflow menu", async () => {
      await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
      await openBoxRowActions(page, boxLabel);
      await page.getByRole("menuitem", { name: "Edit" }).click();
    });

    await test.step("verify edit page rendered", async () => {
      await expect(page).toHaveURL(/\/Storage\/boxes\/\d+\/edit/);
      await expect(
        page.getByRole("heading", { name: /edit\s+box/i }),
      ).toBeVisible();
    });
  });

  test("delete box flow with validation handling", async ({ page }) => {
    const suffix = `${Date.now().toString(36)}-delete`;
    const { boxLabel } = await createBox(page, suffix);

    await test.step("open delete modal", async () => {
      await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();
      await openBoxRowActions(page, boxLabel);
      await page.getByRole("menuitem", { name: "Delete" }).click();
    });

    const dialog = page.getByRole("dialog");

    await test.step("confirm and delete", async () => {
      await expect(dialog).toBeVisible();
      // Boxes are leaves: no cascade summary, no confirmation checkbox.
      await dialog.getByRole("button", { name: "Delete" }).click();
      await expect(dialog).toBeHidden();
    });

    await test.step("row removed from listing", async () => {
      await expect(page.locator("tbody tr", { hasText: boxLabel })).toHaveCount(
        0,
      );
    });
  });
});
