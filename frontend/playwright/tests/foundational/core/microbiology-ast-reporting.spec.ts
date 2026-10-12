import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyAstReporting } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import type { Locator } from "@playwright/test";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";

test("selects one repeated agent, issues immutable partial versions, and locks the final case", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  const seeded = await seedMicrobiologyAstReporting(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  const section = page.getByLabel("Testing section", { exact: true });
  await page
    .getByLabel("New note", { exact: true })
    .fill("M6 internal bench observation");
  await page.getByRole("button", { name: "Save note", exact: true }).click();
  await expect(page.getByLabel("New note", { exact: true })).toHaveValue("");
  await page
    .getByLabel("New note", { exact: true })
    .fill("M6 external clinical note");
  await page.getByText("Send with Result", { exact: true }).click();
  await page.getByRole("button", { name: "Save note", exact: true }).click();
  await expect(page.getByLabel("New note", { exact: true })).toHaveValue("");
  await section.selectOption("AST");
  const isolate = page.getByTestId(`ast-isolate-${seeded.isolateId}`);
  await expect(isolate).toBeVisible({ timeout: LONG_TIMEOUT });
  await isolate
    .getByRole("button", { name: "Start AST run", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel("Breakpoint standard", { exact: true })
    .selectOption(seeded.activeBreakpointStandardId!);
  await dialog
    .getByLabel("Reason", { exact: true })
    .fill("Initial susceptibility testing");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const runs = isolate.locator('[data-testid^="ast-run-"]');
  await expect(runs).toHaveCount(1, { timeout: LONG_TIMEOUT });
  const firstRun = runs.nth(0);
  const enter = async (run: Locator, value: string) => {
    const rows = run.getByRole("table").getByRole("row");
    await expect(rows).toHaveCount(3);
    for (let index = 1; index <= 2; index++) {
      await rows
        .nth(index)
        .getByRole("button", { name: "Enter reading", exact: true })
        .click();
      await dialog.getByLabel("MIC / zone", { exact: true }).fill(value);
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
    }
    await run
      .getByRole("button", { name: "Validate run", exact: true })
      .click();
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      run.getByRole("button", { name: "Repeat", exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  };
  await enter(firstRun, "4");
  await section.selectOption("REPORT");
  await page
    .getByRole("button", { name: "Release partial report", exact: true })
    .click();
  await expect(
    page.getByLabel("Report version", { exact: true }).locator("option"),
  ).toHaveCount(2, { timeout: LONG_TIMEOUT });
  await section.selectOption("AST");
  await firstRun.getByRole("button", { name: "Repeat", exact: true }).click();
  await dialog
    .getByLabel("Reason", { exact: true })
    .fill("Confirm increased resistance");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(runs).toHaveCount(2, { timeout: LONG_TIMEOUT });
  const repeated = runs.nth(1);
  await enter(repeated, "64");
  await repeated
    .getByRole("table")
    .getByRole("row")
    .nth(1)
    .getByRole("button", { name: "Use for report", exact: true })
    .click();
  await dialog
    .getByLabel("Reason", { exact: true })
    .fill("Repeat confirmed for this agent only");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await section.selectOption("REPORT");
  const reportTable = page.getByRole("table", { name: /^ISO-1/ });
  await expect(
    reportTable.getByRole("cell", { name: "Resistant", exact: true }),
  ).toHaveCount(1);
  await expect(
    reportTable.getByRole("cell", { name: "Susceptible", exact: true }),
  ).toHaveCount(1);
  await page
    .getByRole("button", { name: "Release partial report", exact: true })
    .click();
  const versions = page.getByLabel("Report version", { exact: true });
  await expect(versions.locator("option")).toHaveCount(3, {
    timeout: LONG_TIMEOUT,
  });
  await versions.selectOption({ index: 1 });
  await expect(
    reportTable.getByRole("cell", { name: "Susceptible", exact: true }),
  ).toHaveCount(2);
  await versions.selectOption("");
  await page
    .getByRole("button", { name: "Release final report", exact: true })
    .click();
  await expect(versions.locator("option")).toHaveCount(4, {
    timeout: LONG_TIMEOUT,
  });
  await page.reload({ waitUntil: "domcontentloaded" });
  await section.selectOption("AST");
  await expect(isolate).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    isolate.getByRole("button", { name: "Start AST run", exact: true }),
  ).toHaveCount(0);
  await expect(
    isolate.getByRole("button", { name: "Repeat", exact: true }),
  ).toHaveCount(0);
  await section.selectOption("REPORT");
  await expect(
    reportTable.getByRole("cell", { name: "Resistant", exact: true }),
  ).toHaveCount(1);
  await expect(
    reportTable.getByRole("cell", { name: "Susceptible", exact: true }),
  ).toHaveCount(1);
  await page.screenshot({
    path: testInfo.outputPath("ast-final-report.png"),
    fullPage: true,
  });
  await page.goto("/RoutineReport?type=patient&report=patientCILNSP_vreduit", {
    waitUntil: "domcontentloaded",
  });
  await page
    .getByRole("button", { name: "Report By Patient", exact: true })
    .click();
  await page
    .getByLabel("Patient Id", { exact: true })
    .fill(seeded.patientExternalId!);
  await page.locator('[data-cy="searchPatientButton"]').click();
  await page
    .locator(`[data-cy="patient-result-row-${seeded.patientId}"]`)
    .locator("label")
    .click();
  await expect(page.getByTestId("selected-patient")).not.toContainText(
    "None selected",
  );
  let pdf: Buffer | undefined;
  await page.context().route("**/ReportPrint?**", async (route) => {
    // Capture the real server response before Chrome hands it to its PDF viewer.
    const response = await route.fetch();
    expect(response.headers()["content-type"]).toContain("application/pdf");
    pdf = await response.body();
    await route.fulfill({ response });
  });
  await page
    .getByRole("button", { name: "Generate Printable Version", exact: true })
    .click();
  await expect
    .poll(() => pdf?.subarray(0, 5).toString(), { timeout: LONG_TIMEOUT })
    .toBe("%PDF-");
  const document = await getDocument({
    data: new Uint8Array(pdf!),
    useSystemFonts: true,
  }).promise;
  const pages: string[] = [];
  for (let index = 1; index <= document.numPages; index++) {
    const content = await (await document.getPage(index)).getTextContent();
    pages.push(
      content.items.map((item) => ("str" in item ? item.str : "")).join(" "),
    );
  }
  const text = pages.join(" ").replace(/\s+/g, " ");
  expect(text).toContain(seeded.accessionNumber);
  expect(text).toContain("ISO-1");
  expect(text).toContain("64 ug/mL Resistant");
  expect(text).toContain("4 ug/mL Susceptible");
  expect(text).toContain("M6 external clinical note");
  expect(text).not.toContain("M6 internal bench observation");
  const sections = [
    "Initial testing",
    "Culture findings",
    "AST / DST",
    "Additional testing",
  ];
  for (const section of sections) expect(text).toContain(section);
  expect(sections.map((section) => text.indexOf(section))).toEqual(
    sections.map((section) => text.indexOf(section)).sort((a, b) => a - b),
  );
  await document.destroy();
  await testInfo.attach("issued-patient-report.pdf", {
    body: pdf!,
    contentType: "application/pdf",
  });
});
