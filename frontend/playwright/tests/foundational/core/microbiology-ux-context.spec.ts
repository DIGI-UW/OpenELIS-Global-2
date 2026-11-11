import { writeFile } from "node:fs/promises";
import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyMissingContext } from "../../../helpers/seed-microbiology-data";
import { selectCultureMedium } from "../../../helpers/microbiology-ui";
import { readMicrobiologyPatientReport } from "../../../helpers/microbiology-patient-report";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test("completes missing clinical context through release blockers, preserves row notes and opens an amendment", async ({
  page,
}, info) => {
  test.setTimeout(150_000);
  const seeded = await seedMicrobiologyMissingContext(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}?scope=REPORT`);
  const origin = page.getByLabel("Patient origin", { exact: true });
  await expect(origin).toHaveValue("");
  await page.getByRole("button", { name: "Case info", exact: true }).click();
  await expect(origin).not.toBeVisible();
  const blocker = page.getByRole("link", { name: /Patient origin/ });
  await blocker.click();
  await expect(
    page.getByRole("region", { name: "Case info", exact: true }),
  ).toBeFocused();
  await expect(origin).toBeVisible();
  await origin.selectOption("INPATIENT");
  await page
    .getByRole("button", { name: "Save order detail", exact: true })
    .click();
  await expect(blocker).toHaveCount(0, { timeout: LONG_TIMEOUT });
  const exposure = page.getByLabel(/Recent antibiotic exposure/);
  await exposure.selectOption("true");
  await page.getByLabel(/Antibiotic name/).fill("Ampicillin");
  await page
    .getByRole("button", { name: "Save questionnaire answers", exact: true })
    .click();
  await expect(
    page.getByRole("link", {
      name: /Recent antibiotic exposure|Antibiotic name/,
    }),
  ).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await page.reload();
  await expect(origin).toHaveValue("INPATIENT");
  await expect(exposure).toHaveValue("true");
  await expect(page.getByLabel(/Antibiotic name/)).toHaveValue("Ampicillin");
  await expect(
    page.getByLabel("Date of admission", { exact: true }),
  ).toHaveValue("");
  const section = page.getByLabel("Testing section", { exact: true });
  await section.selectOption("CULTURE");
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Bottle or plate identifier").fill("M9-PLATE");
  await selectCultureMedium(page, seeded.mediumItemId!);
  for (const label of ["Medium lot", "Atmosphere"]) {
    const field = dialog.getByLabel(label, { exact: true });
    const value = await field
      .locator("option")
      .evaluateAll((options) =>
        options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
      );
    if (!value) throw new Error(`Fixture has no ${label}`);
    await field.selectOption(value);
  }
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const culture = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /M9-PLATE/ }) });
  await expect(culture).toBeVisible({ timeout: LONG_TIMEOUT });
  await culture.getByRole("button", { name: "Notes (0)", exact: true }).click();
  await dialog
    .getByLabel("New note", { exact: true })
    .fill("M9 internal culture note");
  await dialog.getByRole("button", { name: "Save note", exact: true }).click();
  await expect(dialog.getByLabel("New note", { exact: true })).toHaveValue("");
  await expect(
    dialog.locator("p").filter({ hasText: /^M9 internal culture note$/ }),
  ).toBeVisible();
  await dialog
    .getByLabel("New note", { exact: true })
    .fill("M9 culture-specific report note");
  await dialog.getByText("Send with Result", { exact: true }).click();
  await dialog.getByRole("button", { name: "Save note", exact: true }).click();
  await expect(dialog.getByLabel("New note", { exact: true })).toHaveValue("");
  await expect(
    dialog
      .locator("p")
      .filter({ hasText: /^M9 culture-specific report note$/ }),
  ).toBeVisible();
  await dialog
    .getByRole("button", { name: "Close", exact: true })
    .last()
    .click();
  await expect(
    culture.getByRole("button", { name: "Notes (2)", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(
    culture.getByRole("button", { name: "Notes (2)", exact: true }),
  ).toBeVisible();
  await culture
    .getByRole("button", { name: "Record outcome", exact: true })
    .click();
  await dialog.getByLabel("Outcome", { exact: true }).selectOption("NO_GROWTH");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(culture.getByText("No growth", { exact: true })).toBeVisible();
  await section.selectOption("REPORT");
  const release = page.getByRole("button", {
    name: "Release final report",
    exact: true,
  });
  await expect(release).toBeEnabled({ timeout: LONG_TIMEOUT });
  await release.click();
  await expect(
    page.getByText("Final case is read-only", { exact: true }),
  ).toBeVisible();
  const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
  expect(text).toContain("M9-PLATE: No growth");
  expect(text).toContain("M9 culture-specific report note");
  expect(text).not.toContain("M9 internal culture note");
  expect(text.indexOf("M9 culture-specific report note")).toBeLessThan(
    text.indexOf("AST / DST"),
  );
  await writeFile(info.outputPath("m9-report-notes.pdf"), pdf);
  await info.attach("m9-report-notes.pdf", {
    path: info.outputPath("m9-report-notes.pdf"),
    contentType: "application/pdf",
  });
  await page.goto(`/Microbiology/cases/${seeded.caseId}?scope=AMENDMENT`);
  const amendment = page.getByTestId("microbiology-amendment-card");
  await amendment
    .getByLabel("Amendment reason")
    .fill("Confirm corrected clinical history");
  await amendment
    .getByRole("button", { name: "Open amendment", exact: true })
    .click();
  await expect(origin).toBeEnabled({ timeout: LONG_TIMEOUT });
  for (const width of [1440, 390]) {
    await page.setViewportSize({ width, height: 1000 });
    await expect
      .poll(() =>
        page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
      )
      .toBe(true);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: info.outputPath(`m9-viewport-${width}.png`),
    });
    await page.screenshot({
      path: info.outputPath(`m9-canonical-${width}.png`),
      fullPage: true,
    });
  }
});
