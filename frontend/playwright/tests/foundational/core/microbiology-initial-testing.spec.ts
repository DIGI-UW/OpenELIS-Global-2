import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyInitialTestingCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test.describe("Microbiology initial testing", () => {
  test(
    "saves component results, external provenance, an additional test and case notes",
    { tag: ["@evidence", "@microbiology", "@case"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Initial testing on a microbiology case",
        "Component results are entered and saved for validation, a result performed elsewhere records where, a test is added and a note is kept.",
      );
      await demo.caption(
        "Off screen: a case is ordered with a two-component initial assay.",
      );
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
      await demo.caption(
        "The technologist opens the initial assay for result entry.",
      );
      await table
        .getByRole("button", { name: "Enter or edit", exact: true })
        .click();
      const fields = table.getByRole("textbox", { name: /^Result for/ });
      await expect(fields).toHaveCount(2);
      await fields.nth(0).fill("12.5");
      await demo.caption(
        "A value with more decimals than the test allows keeps Save disabled.",
      );
      await expect(
        table.getByRole("button", { name: "Save results", exact: true }),
      ).toBeDisabled();
      await demo.caption(
        "With valid values for both components, the results are saved.",
      );
      await fields.nth(0).fill("12");
      await fields.nth(1).fill("34");
      await table
        .getByRole("button", { name: "Save results", exact: true })
        .click();
      await expect(
        table.getByText("Awaiting validation", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.highlight(
        table.getByText("Awaiting validation", { exact: true }),
      );
      await demo.caption(
        "After a reload, both component values are still there.",
      );
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
      await demo.caption(
        "The test was performed elsewhere: the technologist records the performing laboratory and date.",
      );
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
      await demo.highlight(table.getByText("External", { exact: true }));
      await expect(fields.nth(0)).toHaveValue("12");
      await table.getByRole("button", { name: "Cancel", exact: true }).click();
      await demo.caption(
        "An additional test is added to the case from the test catalog.",
      );
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
      await demo.caption("A case note records a bench observation.");
      await page
        .getByLabel("New note", { exact: true })
        .fill("M3 bench observation");
      await page
        .getByRole("button", { name: "Save note", exact: true })
        .click();
      await expect(
        page
          .getByRole("table", { name: "Case notes", exact: true })
          .getByText("M3 bench observation", { exact: true }),
      ).toBeVisible();
      await demo.caption(
        "After a reload, the note and the added test are kept.",
      );
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
      const frame = page.viewportSize();
      await demo.caption(
        "On a phone-width screen the same page fits without sideways scrolling.",
      );
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
      if (frame) await page.setViewportSize(frame);
      await demo.verified(
        "Initial testing is saved with its provenance",
        "Results await validation, the external laboratory is recorded, and the added test and the note survive a reload.",
      );
    },
  );
});
