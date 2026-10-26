import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedFinalizedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { readMicrobiologyPatientReport } from "../../../helpers/microbiology-patient-report";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

const attachScreenshot = async (
  page: Page,
  testInfo: TestInfo,
  name: string,
) => {
  await testInfo.attach(name, {
    body: await page.screenshot({ fullPage: true }),
    contentType: "image/png",
  });
};

test.describe("Microbiology final-report amendments", () => {
  test(
    "preserves the original report, publishes the correction, and relocks the case",
    { tag: ["@evidence", "@microbiology", "@ast"] },
    async ({ page }, testInfo) => {
      test.setTimeout(120_000);
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Amending a final report",
        "A released case is reopened with a reason, the isolate is re-identified, and the amended report becomes version 2 while version 1 is kept.",
      );
      await demo.caption(
        "Off screen: a final report is released naming Escherichia coli.",
      );
      const seeded = await seedFinalizedMicrobiologyCase(page);

      await test.step("Verify the original issued patient report", async () => {
        const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
        expect(text).toContain("ISO-1: Escherichia coli");
        expect(text).toContain("Ciprofloxacin (UAT) 4 ug/mL Susceptible");
        await testInfo.attach("original-patient-report.pdf", {
          body: pdf,
          contentType: "application/pdf",
        });
      });

      await test.step("Open an amendment from its canonical URL", async () => {
        await demo.caption(
          "The final case is read-only. Opening an amendment requires a reason.",
        );
        await page.goto(
          `/Microbiology/cases/${seeded.caseId}?section=amendment`,
          { waitUntil: "domcontentloaded" },
        );
        await expect(page).toHaveURL(
          new RegExp(
            `/Microbiology/cases/${seeded.caseId}\\?section=amendment$`,
          ),
        );
        await expect(
          page.getByRole("heading", { name: "Amendments" }),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await expect(page.getByText("Final case is read-only")).toBeVisible();
        const openButton = page
          .getByTestId("microbiology-amendment-card")
          .getByRole("button", {
            name: "Open amendment",
            exact: true,
          });
        await expect(openButton).toBeDisabled();
        await page
          .getByLabel("Amendment reason")
          .fill("Correct identification after confirmatory testing");
        await expect(openButton).toBeEnabled();
        await demo.highlight(openButton);
        await openButton.click();
        await expect(
          page
            .getByTestId("microbiology-amendment-card")
            .getByText("Open amendment", { exact: true }),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await expect(
          page.getByText(
            "Amendment opened; clinical corrections are now enabled",
          ),
        ).toBeVisible();
      });

      await test.step("Record a reasoned isolate re-identification", async () => {
        await demo.caption(
          "With the amendment open, the isolate is re-identified as Klebsiella pneumoniae, with a reason.",
        );
        await page
          .getByLabel("Testing section", { exact: true })
          .selectOption("ISOLATES");
        await expect(page).toHaveURL(/scope=ISOLATES$/);
        await page.getByRole("button", { name: "Reidentify" }).click();
        const dialog = page.getByRole("dialog");
        const organism = dialog.getByRole("combobox", {
          name: "Organism",
          exact: true,
        });
        await organism.fill("Klebsiella pneumoniae (UAT)");
        await organism.press("ArrowDown");
        await organism.press("Enter");
        await dialog
          .getByLabel("ID method", { exact: true })
          .selectOption({ label: "UAT micro culture" });
        const updateButton = dialog.getByRole("button", {
          name: "Save",
          exact: true,
        });
        await expect(updateButton).toBeDisabled();
        await dialog
          .getByLabel("Re-identification reason")
          .fill("MALDI-TOF repeat corrected the organism");
        await updateButton.click();
        await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
        const isolate = page.locator('[data-testid^="isolate-"]');
        await expect(
          isolate.getByRole("heading", {
            name: /ISO-1 — Klebsiella pneumoniae/,
          }),
        ).toBeVisible();
        await isolate
          .getByRole("button", { name: "Identification history", exact: true })
          .click();
        const history = isolate.getByRole("table", {
          name: "Identification history",
        });
        await expect(history).toContainText("Escherichia coli");
        await expect(history).toContainText("Klebsiella pneumoniae (UAT)");
        await expect(history).toContainText(
          "MALDI-TOF repeat corrected the organism",
        );
        await demo.highlight(history);
      });

      await test.step("Release and inspect immutable report versions", async () => {
        await demo.caption(
          "Releasing the amended report issues version 2, which corrects version 1, and locks the case again.",
        );
        await page
          .getByLabel("Testing section", { exact: true })
          .selectOption("AMENDMENT");
        await expect(page).toHaveURL(/scope=AMENDMENT$/);
        await page
          .getByRole("button", { name: "Release amended report" })
          .click();
        await expect(
          page.getByText("Amended report released; the case is locked again"),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        const history = page.getByTestId("microbiology-amendment-card");
        await expect(history).toContainText("Version 1");
        await expect(history).toContainText("Version 2");
        await expect(history).toContainText("Corrects version 1");
        await expect(history).toContainText("Escherichia coli");
        await expect(history).toContainText("Klebsiella pneumoniae (UAT)");
        await demo.highlight(history);
        await attachScreenshot(page, testInfo, "amendment-version-history");
      });

      await test.step("Verify both patient results and the restored mutation lock", async () => {
        await demo.caption(
          "The printed report names Klebsiella pneumoniae, and the case no longer allows changes.",
        );
        const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
        expect(text).toContain("ISO-1: Klebsiella pneumoniae (UAT)");
        expect(text).toContain("Ciprofloxacin (UAT) 4 ug/mL Susceptible");
        expect(text).not.toContain("Escherichia coli");
        await testInfo.attach("amended-patient-report.pdf", {
          body: pdf,
          contentType: "application/pdf",
        });

        await page.goto(
          `/Microbiology/cases/${seeded.caseId}?section=isolates`,
          {
            waitUntil: "domcontentloaded",
          },
        );
        await expect(page.getByText("Final case is read-only")).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await expect(
          page.getByRole("button", { name: "Reidentify", exact: true }),
        ).toHaveCount(0);
        await expect(
          page.getByRole("button", { name: "Pick isolate", exact: true }),
        ).toHaveCount(0);
      });
      await demo.verified(
        "The amendment is issued as version 2, and version 1 is kept",
        "The corrected identification is on the report, and the case is locked again.",
      );
    },
  );
});
