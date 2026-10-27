import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1497: saving an empty patient shows each missing field's message once,
 * on its field, and once the fields are corrected the next Save click saves.
 */
test("Add Patient reports each missing field once, then saves on one click", async ({
  page,
}) => {
  test.setTimeout(120_000);
  await page.goto("/PatientManagement/new", { waitUntil: "domcontentloaded" });
  const save = page.getByRole("button", { name: "Save", exact: true });
  await expect(save).toBeVisible({ timeout: NAV_TIMEOUT });

  await save.click();

  await expect(page.getByText("Select the patient's sex.")).toHaveCount(1);
  await expect(
    page.getByText("Enter a date of birth or an age in years, months or days."),
  ).toHaveCount(1);
  await expect(page.getByText("Sex is Required")).toHaveCount(0);

  const s = `${Date.now()}`.slice(-6);
  await page.locator("#nationalId").fill(`PW1497${s}`);
  await page.locator("#lastName").fill("Pwfourteen");
  await page.locator("#firstName").fill("Ninetyseven");
  await page.locator('label[for="radio-2"]').click();
  await page.locator("#years").fill("34");
  await expect(
    page.getByText("Enter a date of birth or an age in years, months or days."),
  ).toHaveCount(0);

  const saved = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      /\/rest\/PatientManagement$/.test(response.url()),
    { timeout: LONG_TIMEOUT },
  );
  await save.click();
  await saved;
  await expect(page).toHaveURL(/\/PatientManagement\/\d+$/, {
    timeout: LONG_TIMEOUT,
  });
});
