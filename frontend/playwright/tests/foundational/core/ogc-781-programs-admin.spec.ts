import { test, expect } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-781 Programs admin: the reworked Programs screen under Test Management,
 * the deactivate / reactivate lifecycle, the guarded Domain change, and the
 * order-entry picker that only offers active programs of the order's domain.
 */
const PROGRAMS_URL = "/MasterListsPage/program";

const waitForProgramSave = (page) =>
  page.waitForResponse(
    (response) =>
      response.url().includes("/rest/program") &&
      response.request().method() === "POST",
  );

test.describe("OGC-781 Programs admin", () => {
  test("is reached from the Test Management hub and lists programs with domain and status", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/testManagementConfigMenu", {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByRole("link", { name: /^Programs/ })
      .first()
      .click();

    await expect(page).toHaveURL(/\/MasterListsPage\/program$/, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByRole("heading", { name: "Programs" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(
      page.getByRole("columnheader", { name: "Domain" }),
    ).toBeVisible();
    await expect(
      page.getByRole("columnheader", { name: "Status" }),
    ).toBeVisible();
    await expect(
      page.getByRole("row", { name: /Routine Testing/ }).getByText("Clinical"),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("adds a program, guards its domain change, and keeps it out of order entry while deactivated", async ({
    page,
  }) => {
    // Creates a program, edits it, runs the whole lifecycle and loads order
    // entry twice; order entry alone is a slow first paint.
    test.setTimeout(180_000);
    const stamp = Date.now();
    const name = `PW Water Program ${stamp}`;

    await page.goto(PROGRAMS_URL, { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Programs" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    await page.getByRole("button", { name: "Add Program" }).click();
    await page.locator("#pname-new").fill(name);
    await page.locator("#pcode-new").fill(`PW${stamp % 1000000}`);
    await page.locator('label[for="de-new"]').click();
    await page.getByRole("button", { name: "Add First Question" }).click();
    await page.locator('main input[id^="qt-new-"]').fill("Sampling point");

    const created = waitForProgramSave(page);
    await page.getByRole("button", { name: "Submit" }).click();
    await created;

    const row = page.getByRole("row", { name: new RegExp(name) });
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row.getByText("Environmental")).toBeVisible();
    await expect(row.getByText("Active")).toBeVisible();

    // FR-3: changing the Domain of an existing program is confirmed first and
    // the questionnaire is untouched either way.
    await row.getByRole("button", { name: "Edit program" }).click();
    const question = page.locator('main input[id^="qt-"]').first();
    await expect(question).toHaveValue("Sampling point", {
      timeout: UI_TIMEOUT,
    });
    await page.locator('label[for^="dc-"]').first().click();
    const domainDialog = page.getByRole("dialog", {
      name: "Change Program Domain?",
    });
    await expect(domainDialog).toBeVisible({ timeout: UI_TIMEOUT });
    await domainDialog.getByRole("button", { name: "Cancel" }).click();
    await expect(domainDialog).toBeHidden();
    await expect(question).toHaveValue("Sampling point");
    await page.getByRole("button", { name: "Cancel", exact: true }).click();

    // FR-18: deactivate from the row menu with the order-count confirmation.
    await row.getByRole("button", { name: "Options" }).click();
    await page.getByRole("menuitem", { name: /Deactivate/ }).click();
    const deactivateDialog = page.getByRole("dialog", {
      name: "Deactivate this Program?",
    });
    await expect(deactivateDialog).toContainText("has no orders", {
      timeout: UI_TIMEOUT,
    });
    const deactivated = waitForProgramSave(page);
    await deactivateDialog.getByRole("button", { name: /Deactivate/ }).click();
    await deactivated;
    await expect(row).toBeHidden({ timeout: UI_TIMEOUT });

    // FR-19: hidden by default, shown by the toggle with an Inactive tag.
    await page
      .locator("#show-deactivated_label .cds--toggle__appearance")
      .click();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row.getByText("Inactive")).toBeVisible();

    // FR-6: the environmental order picker offers neither the deactivated
    // program nor any Clinical one, so with nothing left to offer it explains
    // itself instead of showing an empty dropdown.
    await page.goto("/order/environmental/enter", {
      waitUntil: "domcontentloaded",
    });
    const picker = page.locator("#program");
    await expect(picker).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(
      page.getByText(/No Environmental programs are currently active/),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await picker.click();
    await expect(page.getByRole("option", { name })).toHaveCount(0);
    await expect(
      page.getByRole("option", { name: "Routine Testing" }),
    ).toHaveCount(0);
    await page.keyboard.press("Escape");

    // FR-18.3: reactivate in one click and the picker offers it again.
    await page.goto(PROGRAMS_URL, { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Programs" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await page
      .locator("#show-deactivated_label .cds--toggle__appearance")
      .click();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await row.getByRole("button", { name: "Options" }).click();
    const reactivated = waitForProgramSave(page);
    await page.getByRole("menuitem", { name: /Reactivate/ }).click();
    await reactivated;
    await expect(row.getByText("Active")).toBeVisible({ timeout: UI_TIMEOUT });

    await page.goto("/order/environmental/enter", {
      waitUntil: "domcontentloaded",
    });
    await expect(picker).toBeVisible({ timeout: NAV_TIMEOUT });
    await picker.click();
    await expect(page.getByRole("option", { name })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  });
});
