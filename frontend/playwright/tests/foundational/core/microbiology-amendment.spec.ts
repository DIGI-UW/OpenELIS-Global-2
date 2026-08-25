import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedFinalizedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const accordionButton = (page: Page, name: string) =>
  page
    .getByTestId("microbiology-case-view")
    .getByRole("button", { name, exact: true });

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
  test("preserves the original report, publishes the correction, and relocks the case", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedFinalizedMicrobiologyCase(page);

    await test.step("Verify the original final result in Patient History", async () => {
      await page.goto(`/PatientResults/${seeded.patientId}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Patient History" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByRole("row", {
          name: /UAT microbiology culture.*ISO-1: Escherichia coli/,
        }),
      ).toContainText("Ciprofloxacin (UAT) S");
    });

    await test.step("Open an amendment from its canonical URL", async () => {
      await page.goto(
        `/Microbiology/cases/${seeded.caseId}?section=amendment`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page).toHaveURL(
        new RegExp(`/Microbiology/cases/${seeded.caseId}\\?section=amendment$`),
      );
      await expect(
        page.getByRole("heading", { name: "Amendments" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByText("Final case is read-only")).toBeVisible();
      const openButton = page.getByRole("button", { name: "Open amendment" });
      await expect(openButton).toBeDisabled();
      await page
        .getByLabel("Amendment reason")
        .fill("Correct identification after confirmatory testing");
      await expect(openButton).toBeEnabled();
      await openButton.click();
      await expect(
        page.getByText("Amendment in progress", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByText(
          "Amendment opened; clinical corrections are now enabled",
        ),
      ).toBeVisible();
    });

    await test.step("Record a reasoned isolate re-identification", async () => {
      await accordionButton(page, "Isolates").click();
      await expect(page).toHaveURL(/section=isolates$/);
      await page.getByRole("button", { name: "Reidentify" }).click();
      await page
        .getByLabel("Organism")
        .selectOption({ label: "Klebsiella pneumoniae (UAT)" });
      await page.getByLabel("Identification method").selectOption("MALDI_TOF");
      const updateButton = page.getByRole("button", {
        name: "Save identification",
      });
      await expect(updateButton).toBeDisabled();
      await page
        .getByLabel("Re-identification reason")
        .fill("MALDI-TOF repeat corrected the organism");
      await expect(updateButton).toBeEnabled();
      await updateButton.click();
      await expect(
        page.getByText(/ISO-1: Klebsiella pneumoniae \(UAT\)/),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByText("Identification history")).toBeVisible();
      await expect(
        page
          .getByTestId("microbiology-isolates-card")
          .getByText(
            "Escherichia coli to Klebsiella pneumoniae (UAT): MALDI-TOF repeat corrected the organism",
          ),
      ).toBeVisible();
    });

    await test.step("Release and inspect immutable report versions", async () => {
      await accordionButton(page, "Amendments").click();
      await expect(page).toHaveURL(/section=amendment$/);
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
      await attachScreenshot(page, testInfo, "amendment-version-history");
    });

    await test.step("Verify both patient results and the restored mutation lock", async () => {
      await page.goto(`/PatientResults/${seeded.patientId}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("row", {
          name: /UAT microbiology culture.*ISO-1: Escherichia coli/,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByRole("row", {
          name: /UAT microbiology culture.*ISO-1: Klebsiella pneumoniae \(UAT\)/,
        }),
      ).toContainText("Ciprofloxacin (UAT) S");
      await attachScreenshot(page, testInfo, "original-and-amended-results");

      await page.goto(`/Microbiology/cases/${seeded.caseId}?section=isolates`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByText("Final case is read-only")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByRole("button", { name: "Reidentify" }),
      ).toBeDisabled();
      await expect(
        page.getByRole("button", { name: "Create isolate" }),
      ).toBeDisabled();
    });
  });
});
