import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyInitialTestingCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology initial testing", () => {
  test("saves component results, external provenance, an additional test and case notes", async ({
    page,
  }, testInfo) => {
    const seeded = await seedMicrobiologyInitialTestingCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    const table = page.getByRole("table", {
      name: "Testing and results",
      exact: true,
    });
    await expect(
      table.getByText("UAT Microbiology initial assay", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await table
      .getByRole("button", { name: "Enter or edit", exact: true })
      .click();
    const fields = table.getByRole("textbox", { name: /^Result for/ });
    await expect(fields).toHaveCount(2);
    await fields.nth(0).fill("12.5");
    await expect(
      table.getByRole("button", { name: "Save results", exact: true }),
    ).toBeDisabled();
    await fields.nth(0).fill("12");
    await fields.nth(1).fill("34");
    await table
      .getByRole("button", { name: "Save results", exact: true })
      .click();
    await expect(
      table.getByText("Awaiting validation", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await page.reload();
    await table
      .getByRole("button", { name: "Enter or edit", exact: true })
      .click();
    await expect(fields.nth(0)).toHaveValue("12");
    await expect(fields.nth(1)).toHaveValue("34");
    const external = table.getByRole("checkbox", {
      name: "Tested elsewhere",
      exact: true,
    });
    await external.focus();
    await external.press("Space");
    await expect(external).toBeChecked();
    const performer = table.getByRole("combobox", {
      name: "Performed by",
      exact: true,
    });
    await performer.click();
    await performer.press("ArrowDown");
    await performer.press("Enter");
    await table
      .getByLabel("Date performed", { exact: true })
      .fill(seeded.collectionLocalDate!);
    await table
      .getByRole("button", { name: "Save provenance", exact: true })
      .click();
    await expect(table.getByText("External", { exact: true })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(fields.nth(0)).toHaveValue("12");
    await table.getByRole("button", { name: "Cancel", exact: true }).click();
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ADDITIONAL");
    await page.getByRole("button", { name: /— 0/ }).click();
    await page
      .getByRole("searchbox")
      .last()
      .fill("UAT Microbiology follow-up assay");
    const choice = page.getByRole("button", {
      name: "UAT Microbiology follow-up assay",
      exact: true,
    });
    await expect(choice).toBeVisible({ timeout: LONG_TIMEOUT });
    await choice.press("Enter");
    await page
      .getByRole("button", { name: "Add selected tests", exact: true })
      .click();
    await expect(
      table.getByText("UAT Microbiology follow-up assay", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await page
      .getByLabel("New note", { exact: true })
      .fill("M3 bench observation");
    await page.getByRole("button", { name: "Save note", exact: true }).click();
    await expect(
      page
        .getByRole("table", { name: "Case notes", exact: true })
        .getByText("M3 bench observation", { exact: true }),
    ).toBeVisible();
    await page.reload();
    await expect(
      page
        .getByRole("table", { name: "Case notes", exact: true })
        .getByText("M3 bench observation", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ADDITIONAL");
    await expect(
      table.getByText("UAT Microbiology follow-up assay", { exact: true }),
    ).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath("case-testing-desktop.png"),
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(
        () =>
          page.evaluate(
            () => document.documentElement.scrollWidth <= window.innerWidth,
          ),
        { timeout: LONG_TIMEOUT },
      )
      .toBeTruthy();
    await page.screenshot({
      path: testInfo.outputPath("case-testing-mobile.png"),
      fullPage: true,
    });
  });
});
