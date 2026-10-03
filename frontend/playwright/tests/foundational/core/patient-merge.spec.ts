import { test, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedOrderForPatient,
  seedPatient,
  type SeededPatient,
} from "../../../helpers/seed-patient-order";

/**
 * The Merge Patient Records wizard end to end. Consolidation rules themselves
 * are pinned by PatientMergeServiceIntegrationTest and
 * PatientMergeConsolidationServiceIntegrationTest; this proves the screens
 * drive a merge and that its outcome shows where users look for patients.
 */

async function choosePatient(page: Page, panel: 1 | 2, patient: SeededPatient) {
  await page.locator(`#patient${panel}-patientId`).fill(patient.nationalId);
  const answered = page.waitForResponse((r) =>
    r.url().includes("/rest/patient-search-results"),
  );
  await page.locator(`#patient${panel}-local_search`).click();
  await answered;
  await page
    .locator(`label[for="patient${panel}-${patient.patientPK}"]`)
    .click();
  await expect(
    page.getByRole("heading", { name: `Patient Id: ${patient.nationalId}` }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

/** Searches Add Or Modify Patient; returns the patient's result row. */
async function searchPatients(
  page: Page,
  criteria: { lastName?: string; labNumber?: string },
) {
  await page.goto("/PatientManagement", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  await expect(
    main.getByRole("button", { name: "Search", exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  if (criteria.lastName) {
    await main
      .getByRole("textbox", { name: "Last Name" })
      .fill(criteria.lastName);
  }
  if (criteria.labNumber) {
    await main
      .getByRole("textbox", { name: "Previous Lab Number" })
      .fill(criteria.labNumber);
  }
  const answered = page.waitForResponse((r) =>
    r.url().includes("/rest/patient-search-results"),
  );
  await main.getByRole("button", { name: "Search", exact: true }).click();
  await answered;
}

function resultRowFor(page: Page, patient: SeededPatient) {
  return page
    .locator('[data-cy="patientResultsTable"]')
    .getByRole("row")
    .filter({ hasText: patient.nationalId });
}

test.describe("Merge patient records", () => {
  test("merging two patients keeps the primary and marks the other merged", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const primary = await seedPatient(page);
    const duplicate = await seedPatient(page);
    const duplicateOrder = await seedOrderForPatient(page, duplicate);

    await test.step("select both patients", async () => {
      await page.goto("/PatientMerge", { waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("heading", { name: "Merge Patient Records" }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await choosePatient(page, 1, primary);
      await choosePatient(page, 2, duplicate);
      await page.getByRole("button", { name: "Next Step" }).click();
    });

    await test.step("make the first patient primary", async () => {
      const next = page.getByRole("button", { name: "Next Step" });
      await expect(next).toBeDisabled({ timeout: UI_TIMEOUT });
      const choice = `Patient 1: ${primary.patientPK} - ${primary.firstName} ${primary.lastName}`;
      await page.getByText(choice, { exact: true }).click();
      await expect(page.getByRole("radio", { name: choice })).toBeChecked();
      await next.click();
    });

    await test.step("confirm with a reason", async () => {
      await expect(page.getByText("Merge Summary")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      const summaryValue = (label: string) =>
        page
          .getByText(label, { exact: true })
          .locator("xpath=following-sibling::span[1]");
      await expect(summaryValue("Primary Patient:")).toHaveText(
        `${primary.patientPK} - ${primary.firstName} ${primary.lastName}`,
      );
      await expect(summaryValue("Merging From:")).toHaveText(
        `${duplicate.patientPK} - ${duplicate.firstName} ${duplicate.lastName}`,
      );
      await expect(
        page.getByRole("listitem").filter({ hasText: /^Samples:/ }),
      ).toHaveText("Samples: 1");
      const confirm = page.getByRole("button", { name: /Confirm Merge$/ });
      await expect(confirm).toBeDisabled();
      await page
        .getByRole("textbox", { name: "Reason for Merge (required)" })
        .fill("Duplicate registration found in E2E");
      await page
        .getByText("I understand this action cannot be undone", {
          exact: false,
        })
        .click();
      await expect(page.getByRole("checkbox")).toBeChecked();
      await confirm.click();
      await expect(
        page.getByText("Patient merge completed successfully"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page).toHaveURL(
        new RegExp(`/PatientManagement/${primary.patientPK}$`),
        { timeout: UI_TIMEOUT },
      );
    });

    await test.step("the merged patient is still found by name, tagged merged", async () => {
      await searchPatients(page, { lastName: duplicate.lastName });
      const row = resultRowFor(page, duplicate);
      await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(row).toContainText("Merged");
    });

    await test.step("the primary patient is not tagged merged", async () => {
      await searchPatients(page, { lastName: primary.lastName });
      const row = resultRowFor(page, primary);
      await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(row).not.toContainText("Merged");
    });

    await test.step("the merged patient's order now belongs to the primary", async () => {
      await searchPatients(page, { labNumber: duplicateOrder });
      await expect(resultRowFor(page, primary)).toHaveCount(1, {
        timeout: UI_TIMEOUT,
      });
      await expect(resultRowFor(page, duplicate)).toHaveCount(0);
    });
  });
});
