import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedReviewedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
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

test.describe("Microbiology repeat AST attempts", () => {
  test("preserves both attempts and reports only the explicitly selected run", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedReviewedMicrobiologyCase(page);

    await test.step("Open the reviewed original attempt", async () => {
      await page.goto(`/Microbiology/cases/${seeded.caseId}?section=ast`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page).toHaveURL(
        new RegExp(`/Microbiology/cases/${seeded.caseId}\\?section=ast$`),
      );
      await expect(
        page.getByRole("heading", { name: "Manual AST" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const original = page
        .getByTestId("microbiology-ast-attempt-row")
        .filter({ hasText: "Original" });
      await expect(original).toContainText("Attempt 1");
      await expect(original).toContainText("Included in report");
      await expect(original).toContainText("Reviewed");
      await expect(page.getByTestId("microbiology-ast-card")).toContainText(
        "Ciprofloxacin (UAT)",
      );
    });

    await test.step("Start a reasoned retest from the original", async () => {
      const retestOption = page.getByRole("radio", { name: "Retest" });
      await retestOption.press("Space");
      await expect(retestOption).toBeChecked();
      await page
        .getByLabel("Reason for repeat or retest")
        .fill("Control failure required a fresh inoculum");
      await expect(
        page.getByRole("button", { name: "Start retest attempt" }),
      ).toBeEnabled();
      await page.getByRole("button", { name: "Start retest attempt" }).click();

      await expect(
        page.getByTestId("microbiology-ast-attempt-row"),
      ).toHaveCount(2, { timeout: LONG_TIMEOUT });
      const retest = page
        .getByTestId("microbiology-ast-attempt-row")
        .filter({ hasText: "Retest" });
      await expect(retest).toContainText("Attempt 2");
      await expect(retest).toContainText("Attempt 1");
      await expect(retest).toContainText(
        "Control failure required a fresh inoculum",
      );
      await expect(retest).toContainText("In Progress");
    });

    await test.step("Record and review a different result without changing attempt 1", async () => {
      await page
        .getByLabel("Antibiotic", { exact: true })
        .selectOption({ label: "Ciprofloxacin (UAT)" });
      await page.getByLabel("MIC (ug/mL)").fill("32");
      await page.getByRole("button", { name: "Record AST reading" }).click();
      await expect(
        page.getByTestId("microbiology-ast-interpretation"),
      ).toContainText("Resistant", { timeout: LONG_TIMEOUT });
      await page.getByRole("button", { name: "Review AST run" }).click();

      await expect(
        page
          .getByTestId("microbiology-ast-card")
          .getByText("Reportable AST Run Required"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const original = page
        .getByTestId("microbiology-ast-attempt-row")
        .filter({ hasText: "Original" });
      const retest = page
        .getByTestId("microbiology-ast-attempt-row")
        .filter({ hasText: "Retest" });
      await expect(original).toContainText("Not included");
      await expect(retest).toContainText("Not included");

      await retest
        .getByRole("button", { name: "Use attempt 2 for reporting" })
        .click();
      await expect(retest).toContainText("Included in report", {
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page
          .getByTestId("microbiology-ast-card")
          .getByText("Final release ready", { exact: true }),
      ).toBeVisible();
      await attachScreenshot(page, testInfo, "repeat-ast-reportable-selection");
    });

    await test.step("Release and verify the selected retest reaches Patient History", async () => {
      await accordionButton(page, "Reports").click();
      await expect(page).toHaveURL(/section=reports$/);
      await page
        .getByRole("button", { name: "Release preliminary report" })
        .click();
      await expect(
        page.getByTestId("microbiology-release-state"),
      ).toContainText("Preliminary Released", { timeout: LONG_TIMEOUT });

      await page.goto(`/PatientResults/${seeded.patientId}`, {
        waitUntil: "domcontentloaded",
      });
      const patientResult = page.getByRole("row", {
        name: /UAT microbiology culture.*ISO-1: Escherichia coli/,
      });
      await expect(patientResult).toContainText("Ciprofloxacin (UAT) R", {
        timeout: LONG_TIMEOUT,
      });
      await expect(patientResult).not.toContainText("Ciprofloxacin (UAT) S");
      await attachScreenshot(page, testInfo, "repeat-ast-patient-result");
    });
  });
});
