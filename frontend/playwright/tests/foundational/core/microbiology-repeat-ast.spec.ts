import { test, expect } from "../../../helpers/test-base";
import { seedReviewedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { readMicrobiologyPatientReport } from "../../../helpers/microbiology-patient-report";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test(
  "preserves the original AST reading and reports the explicitly selected retest",
  { tag: ["@evidence", "@microbiology", "@ast"] },
  async ({ page }, info) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, info);
    await demo.intro(
      "A retest that replaces one reading on the report",
      "The original reading is kept, and the report uses the retest only because someone chose it, with a reason.",
    );
    await demo.caption(
      "Off screen: a case with a reviewed AST run is prepared. Ciprofloxacin read 4, susceptible.",
    );
    const seeded = await seedReviewedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}?section=ast`);
    const original = page.getByTestId(`ast-run-${seeded.astRunId}`);
    await expect(original).toContainText("4", { timeout: LONG_TIMEOUT });
    await demo.highlight(original);
    await demo.caption("A retest is started, with the reason for it.");
    await original.getByRole("button", { name: "Retest", exact: true }).click();
    const dialog = page.getByRole("dialog");
    await dialog
      .getByLabel("Reason", { exact: true })
      .fill("Fresh inoculum after control failure");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const runs = page.locator('[data-testid^="ast-run-"]');
    await expect(runs).toHaveCount(2);
    const repeated = runs.filter({
      hasNot: page.getByRole("button", { name: "Retest", exact: true }),
    });
    const drug = repeated.getByRole("row", { name: /Ciprofloxacin \(UAT\)/ });
    await drug
      .getByRole("button", { name: "Enter reading", exact: true })
      .click();
    await demo.caption("The retest reads 32: resistant.");
    await dialog.getByLabel("MIC / zone", { exact: true }).fill("32");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(drug).toContainText("Resistant");
    await demo.highlight(drug);
    await demo.caption(
      "After validation, the retest reading is chosen for the report, with a reason.",
    );
    await repeated
      .getByRole("button", { name: "Validate run", exact: true })
      .click();
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    // Select by the different observed reading after validation changes the run's actions.
    const selectedDrug = runs.getByRole("row", {
      name: /Ciprofloxacin \(UAT\).*32/,
    });
    await selectedDrug
      .getByRole("button", { name: "Use for report", exact: true })
      .click();
    await dialog
      .getByLabel("Reason", { exact: true })
      .fill("Retest confirmed resistance");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      original.getByRole("row", { name: /Ciprofloxacin \(UAT\)/ }),
    ).toContainText("4");
    await demo.caption(
      "The original run still shows its reading of 4. A partial report is released.",
    );
    await demo.highlight(
      original.getByRole("row", { name: /Ciprofloxacin \(UAT\)/ }),
    );
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("REPORT");
    await page
      .getByRole("button", { name: "Release partial report", exact: true })
      .click();
    await expect(
      page.getByLabel("Report version", { exact: true }).locator("option"),
    ).toHaveCount(2);
    const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
    expect(text).toContain("Ciprofloxacin (UAT) 32 ug/mL Resistant");
    expect(text).not.toContain("Ciprofloxacin (UAT) 4 ug/mL Susceptible");
    await info.attach("selected-retest-patient-report.pdf", {
      body: pdf,
      contentType: "application/pdf",
    });
    await demo.verified(
      "The report uses the chosen retest, and the original reading is kept",
      "The patient report reads Ciprofloxacin 32 ug/mL Resistant, and the first run still shows 4.",
    );
  },
);
