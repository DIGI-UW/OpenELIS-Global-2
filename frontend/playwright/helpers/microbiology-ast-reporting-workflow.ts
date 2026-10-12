import { expect } from "@playwright/test";
import type { Locator, Page, TestInfo } from "@playwright/test";
import { seedMicrobiologyAstReporting } from "./seed-microbiology-data";
import { readMicrobiologyPatientReport } from "./microbiology-patient-report";
import { LONG_TIMEOUT } from "./timeouts";

export async function runMicrobiologyAstReportingWorkflow(
  page: Page,
  testInfo: TestInfo,
  checkpoint: (
    step: number,
    description: string,
  ) => Promise<void> = async () => {},
) {
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
  await checkpoint(
    1,
    "Record and validate the initial susceptibility readings",
  );
  await enter(firstRun, "4");
  await section.selectOption("REPORT");
  await page
    .getByRole("button", { name: "Release partial report", exact: true })
    .click();
  await expect(
    page.getByLabel("Report version", { exact: true }).locator("option"),
  ).toHaveCount(2, { timeout: LONG_TIMEOUT });
  await section.selectOption("AST");
  await checkpoint(
    2,
    "Issue the first partial report, then repeat susceptibility testing",
  );
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
  await checkpoint(
    3,
    "Select the repeat for one drug while retaining the other drug result",
  );
  const versions = page.getByLabel("Report version", { exact: true });
  await expect(versions.locator("option")).toHaveCount(3, {
    timeout: LONG_TIMEOUT,
  });
  await versions.selectOption({ index: 1 });
  await expect(
    reportTable.getByRole("cell", { name: "Susceptible", exact: true }),
  ).toHaveCount(2);
  await checkpoint(
    4,
    "Verify that the earlier issued report remains unchanged",
  );
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
  await checkpoint(5, "Release the final report and verify the case is locked");
  const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
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
  await testInfo.attach("issued-patient-report.pdf", {
    body: pdf!,
    contentType: "application/pdf",
  });
  await checkpoint(
    6,
    "Print the issued report with selected readings and external notes",
  );
}
