import { randomInt, randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { csrfToken } from "../../../helpers/api-session";
import { API, signIfAsked } from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1200 slice 10a (FR-C10, FR-C11, FR-C12, FR-H1): a quality officer
 * requires a blank on every run of a test and sets its blank ceiling and RPD
 * limit in the Test Catalog QC section. A batch run without a blank is held for
 * it; a passing blank releases it. A duplicate recorded before its sample's
 * value is not evaluated, and fails, holding the test, once the value is saved.
 */

const pickRow = async (page: Page, accession: string, testName: string) => {
  const box = page.getByLabel(
    new RegExp(`^Select ${accession} ${testName}.* for a batch$`),
  );
  await expect(box).toBeAttached({ timeout: LONG_TIMEOUT });
  await page.locator(`label[for="${await box.getAttribute("id")}"]`).click();
  await expect(box).toBeChecked();
};

test("a run needs its required blank, and a duplicate is judged against its sample's value", async ({
  page,
}) => {
  test.setTimeout(300_000);
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const run = randomUUID().slice(0, 8);
  const name = `Run blank ${run}`;
  const [target] = await seedNumericTests(
    page,
    run,
    [
      {
        name,
        loinc: `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`,
      },
    ],
    { testSection: "Biochemistry", sampleType: "Serum" },
  );
  let batchId: number | undefined;

  try {
    await test.step("The QC section requires a blank and sets the ceiling and RPD limit", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${target.id}/qc-targets`,
        { waitUntil: "domcontentloaded" },
      );
      await page
        .getByTestId("qc-policy-blank-required")
        .selectOption("yes", { timeout: LONG_TIMEOUT });
      await page
        .getByTestId("qc-policy-blank-when-missing")
        .selectOption("HOLD");
      await page.getByTestId("qc-policy-blank-limit").fill("0.5");
      await page.getByTestId("qc-policy-duplicate-limit").fill("10");
      await page.getByTestId("qc-targets-save").click();
      await expect(page.getByText("QC targets saved.")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    const first = await createClinicalOrder(page, {
      testIds: [target.id],
      specimenName: "Serum",
    });
    const second = await createClinicalOrder(page, {
      testIds: [target.id],
      specimenName: "Serum",
    });

    await test.step("A batch run without its blank is held", async () => {
      await page.goto(`/Results?accessionNumber=${first.accession}`);
      await pickRow(page, first.accession, name);
      const search = page.getByRole("searchbox");
      await search.fill(second.accession);
      await search.press("Enter");
      await pickRow(page, second.accession, name);
      const batched = page.waitForResponse((response) =>
        response.url().endsWith("/rest/batch-workplans/batches/from-worklist"),
      );
      await page.getByTestId("batch-these").click();
      batchId = (await (await batched).json()).id;
      await expect(page.getByTestId("run-qc-overall")).toHaveText("Held", {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId(`run-qc-verdict-${target.id}`)).toHaveText(
        "No blank",
      );
      await expect(page.getByTestId(`run-qc-missing-${target.id}`)).toHaveText(
        "This run lacks the blank this test requires on every run, so its rows are held. Record it, or decide the test.",
      );
    });

    await test.step("A passing blank releases the run", async () => {
      await page.getByTestId("run-qc-record-item").click();
      await page.getByLabel("Value read").fill("0.4");
      await page.getByTestId("run-qc-item-save").click();
      await expect(page.getByTestId("run-qc")).toContainText(
        "Pass Blank · 0.4 · Result = 0.4 (threshold 0.5)",
        { timeout: LONG_TIMEOUT },
      );
      await expect(page.getByTestId("run-qc-overall")).toHaveText("Pass");
    });

    await test.step("A duplicate is judged once its sample's value is saved", async () => {
      await page.getByTestId("run-qc-record-item").click();
      await page.locator('label[for$="-qc-item-kind-DUPLICATE"]').click();
      const sample = page.getByLabel("Sample it repeats");
      await expect(
        sample.locator("option", { hasText: first.accession }),
      ).toBeAttached({ timeout: LONG_TIMEOUT });
      await sample.selectOption({ label: first.accession });
      await page.getByLabel("Value read").fill("100");
      await page.getByTestId("run-qc-item-save").click();
      await expect(page.getByTestId("run-qc")).toContainText(
        `Not evaluated Duplicate of ${first.accession} · 100`,
        { timeout: LONG_TIMEOUT },
      );

      const row = page
        .getByTestId("run-rows")
        .locator('> tbody > tr[data-testid^="run-row-"]', {
          hasText: first.accession,
        });
      await row.getByRole("button", { name: /expand row/i }).click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await entry.locator('input[id^="unifiedResultValue-"]').fill("130");
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      await signIfAsked(page);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("run-qc")).toContainText(
        `Fail Duplicate of ${first.accession} · 100 · RPD = 26.1% (threshold 10%)`,
        { timeout: LONG_TIMEOUT },
      );
      await expect(page.getByTestId("run-qc-overall")).toHaveText("Held");
    });
  } finally {
    if (batchId) {
      await page.request.put(
        `${API}/rest/batch-workplans/batches/${batchId}/status`,
        {
          headers: { "X-CSRF-Token": await csrfToken(page) },
          data: { status: "ARCHIVED" },
        },
      );
    }
  }
});
