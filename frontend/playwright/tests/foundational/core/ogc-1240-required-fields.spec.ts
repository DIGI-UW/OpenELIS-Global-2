import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1240 — required fields on Enter Order:
 *  - a required input tells assistive technology it is required
 *    (aria-required), and its asterisk is hidden from it;
 *  - a patient missing the National ID the deployment requires holds both
 *    saves and is named in the checklist, instead of a save the server
 *    refuses without a word.
 */

const API = "/api/OpenELIS-Global";

test.describe("OGC-1240 required fields on Enter Order", () => {
  test("required inputs are announced as required and their asterisks are hidden", async ({
    page,
  }) => {
    await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
    const labNumber = page.locator("#labNumber");
    await expect(labNumber).not.toHaveValue("", { timeout: NAV_TIMEOUT });

    await expect(labNumber).toHaveAttribute("aria-required", "true");
    await expect(page.locator("#sampleType-0")).toHaveAttribute(
      "aria-required",
      "true",
    );
    await expect(
      page.locator('label[for="labNumber"] .requiredlabel'),
    ).toHaveAttribute("aria-hidden", "true");

    const section = page.getByTestId("patient-search-section");
    await section.getByRole("button", { name: "New Patient" }).click();
    const nationalId = section.locator("#nationalId");
    await expect(nationalId).toBeVisible({ timeout: UI_TIMEOUT });
    const configuration = await (
      await page.request.get(`${API}/rest/configuration-properties`)
    ).json();
    const required = configuration.PATIENT_NATIONAL_ID_REQUIRED !== "false";
    if (required) {
      await expect(nationalId).toHaveAttribute("aria-required", "true");
    } else {
      await expect(nationalId).not.toHaveAttribute("aria-required", /.*/);
    }
  });

  test("a patient without the required National ID holds both saves and names the field", async ({
    page,
  }) => {
    const configuration = await (
      await page.request.get(`${API}/rest/configuration-properties`)
    ).json();
    test.skip(
      configuration.PATIENT_NATIONAL_ID_REQUIRED === "false",
      "the deployment does not require a National ID",
    );

    await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    const section = page.getByTestId("patient-search-section");
    await section.getByRole("button", { name: "New Patient" }).click();
    const lastName = section.locator("#lastName");
    await expect(lastName).toBeVisible({ timeout: UI_TIMEOUT });
    await lastName.pressSequentially("Requiredwalk", { delay: 20 });
    await section.locator("#firstName").pressSequentially("Ida", { delay: 20 });
    await section
      .getByRole("textbox", { name: /Date of Birth/ })
      .fill("05/03/1988");
    await section
      .locator("#create_patient_gender label")
      .filter({ hasText: "Female" })
      .click();
    const samples = page.getByTestId("order-sample-test-section");
    await samples.locator("#sampleType-0").selectOption({ label: "Serum" });
    await samples.locator('label[for^="test-0-"]').first().click();

    const checklist = page.getByTestId("to-continue-checklist");
    await expect(checklist).toContainText("Enter the patient's National ID", {
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByRole("button", { name: "Save and exit", exact: true }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Save and next", exact: true }),
    ).toBeDisabled();

    await checklist.getByText("Enter the patient's National ID").click();
    await expect(section.locator("#nationalId")).toBeFocused();
    await section
      .locator("#nationalId")
      .pressSequentially(`QA1240${Date.now()}`, { delay: 10 });
    await expect(checklist).toHaveCount(0, { timeout: UI_TIMEOUT });
  });
});
