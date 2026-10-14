import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyNoGrowthReporting } from "../../../helpers/seed-microbiology-data";
import { readMicrobiologyPatientReport } from "../../../helpers/microbiology-patient-report";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test("records no growth without publishing, then releases one final negative report", async ({
  page,
}, testInfo) => {
  const seeded = await seedMicrobiologyNoGrowthReporting(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  const section = page.getByLabel("Testing section", { exact: true });
  await section.selectOption("CULTURE");
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel("Bottle or plate identifier")
    .fill("UAT-NO-GROWTH-01");
  await dialog
    .getByLabel("Medium", { exact: true })
    .selectOption(String(seeded.mediumItemId));
  for (const label of ["Medium lot", "Atmosphere"]) {
    const field = dialog.getByLabel(label, { exact: true });
    const value = await field
      .locator("option")
      .evaluateAll((options) =>
        options
          .map((option) => (option as HTMLOptionElement).value)
          .find(Boolean),
      );
    expect(value).toBeTruthy();
    await field.selectOption(value!);
  }
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const row = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /UAT-NO-GROWTH-01/ }) })
    .first();
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  await section.selectOption("REPORT");
  await expect(
    page.getByRole("button", { name: "Release final report", exact: true }),
  ).toBeDisabled();
  await section.selectOption("CULTURE");
  await row
    .getByRole("button", { name: "Record outcome", exact: true })
    .click();
  await dialog.getByLabel("Outcome", { exact: true }).selectOption("NO_GROWTH");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(row.getByText("No growth", { exact: true })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await section.selectOption("REPORT");
  await expect(page.getByText(/UAT-NO-GROWTH-01: No growth/)).toBeVisible();
  const versions = page.getByLabel("Report version", { exact: true });
  await expect(versions.locator("option")).toHaveCount(1);
  const release = page.getByRole("button", {
    name: "Release final report",
    exact: true,
  });
  await expect(release).toBeEnabled({ timeout: LONG_TIMEOUT });
  await release.click();
  await expect(versions.locator("option")).toHaveCount(2, {
    timeout: LONG_TIMEOUT,
  });
  await page.reload();
  await section.selectOption("CULTURE");
  await expect(
    page.getByRole("button", { name: "Inoculate culture", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Record outcome", exact: true }),
  ).toHaveCount(0);
  const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
  expect(text).toContain(seeded.accessionNumber);
  expect(text).toContain("UAT-NO-GROWTH-01: No growth");
  expect(text).not.toContain("ISO-1");
  await testInfo.attach("negative-patient-report.pdf", {
    body: pdf,
    contentType: "application/pdf",
  });
});
