import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Test Accreditation: an expiry date typed into the box, rather than picked on
 * the calendar, is kept when the user clicks elsewhere. Carbon's date picker
 * closes its calendar on an outside click without reading the typed text, so
 * the form refused a date it was showing. An impossible date is still refused.
 *
 * The body this creates has no enrolled tests and is deleted at the end.
 */
test("an accrediting body keeps an expiry date typed into the box", async ({
  page,
}) => {
  const code = `T${Date.now().toString(36).toUpperCase()}`.slice(0, 16);
  await page.goto("/qa/qms/accreditation", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await page.getByTestId("add-body-button").click();
  const modal = page.getByRole("dialog", { name: "Add Accrediting Body" });
  await modal.getByRole("textbox", { name: "Code" }).fill(code);
  const name = modal.getByRole("textbox", { name: "Name" });
  await name.fill(`Typed date ${code}`);
  const expires = modal.getByRole("textbox", { name: "Accreditation expires" });

  await test.step("an impossible date is refused", async () => {
    await expires.click();
    await expires.pressSequentially("2031-13-45");
    await name.click();
    await modal.getByRole("button", { name: "Save" }).click();
    await expect(
      modal.getByText("Code, name and expiry date are required."),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });

  await test.step("a real date typed and left is saved", async () => {
    await expires.fill("");
    await expires.pressSequentially("2031-06-30");
    await name.click();
    await modal.getByRole("button", { name: "Save" }).click();
    await expect(modal).toBeHidden({ timeout: UI_TIMEOUT });
    const row = page.getByRole("row").filter({ hasText: code });
    await expect(row).toContainText("2031-06-30");
  });

  await test.step("remove the body again", async () => {
    const row = page.getByRole("row").filter({ hasText: code });
    await row.getByRole("button", { name: "Delete body" }).click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: /Delete/ })
      .click();
    await expect(page.getByRole("row").filter({ hasText: code })).toHaveCount(
      0,
      { timeout: UI_TIMEOUT },
    );
  });
});
