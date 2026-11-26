import { randomInt, randomUUID } from "node:crypto";
import type { Locator, Page } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { geneXpertMockSource } from "../../../helpers/analyzer-native-traffic";
import { activateShippedAnalyzer } from "../../../helpers/analyzer-setup-flow";
import {
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";
import { signIfAsked } from "../../../helpers/results-nce-ui";

/**
 * OGC-1200 slice 6 (FR-G3): the validator reads the run's QC verdict on the
 * Validation row. A test accepted despite its run's failed control reads QC
 * failed, with the decision and the control in the row's QC section; a test
 * whose control passed reads QC passed.
 */

const CATALOG = "/api/OpenELIS-Global/rest/test-catalog";
const UI_TIMEOUT = 20_000;

async function setRanges(page: Page, testId: string) {
  const outcome = await page.evaluate(
    async ({ catalog, testId }) => {
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      };
      const results = await (
        await fetch(`${catalog}/tests/${testId}/sample-results`, {
          credentials: "include",
          headers,
        })
      ).json();
      const ranges = await fetch(`${catalog}/tests/${testId}/ranges`, {
        method: "PUT",
        credentials: "include",
        headers,
        body: JSON.stringify({
          testId,
          ranges: [
            {
              componentId: results.components[0].id,
              gender: null,
              minAge: 0,
              maxAge: null,
              lowNormal: 5,
              highNormal: 100,
              lowCritical: 2,
              highCritical: 150,
              lowValid: 0,
              highValid: 1000,
            },
          ],
        }),
      });
      return ranges.ok ? "" : `ranges ${ranges.status}: ${await ranges.text()}`;
    },
    { catalog: CATALOG, testId },
  );
  expect(outcome, "the test's ranges must save").toBe("");
}

async function openSection(section: Locator) {
  const header = section.locator(".unifiedRefSectionHeader");
  if ((await header.getAttribute("aria-expanded")) !== "true") {
    await header.click();
  }
  await expect(header).toHaveAttribute("aria-expanded", "true");
}

async function recordControlLine(
  page: Page,
  testId: string,
  kit: string,
  outcome: "Valid" | "Invalid",
) {
  const qc = page.getByTestId("run-qc");
  await qc.getByTestId("run-qc-record").click();
  const form = qc.getByTestId("run-qc-form");
  await form.locator("select").first().selectOption(testId);
  await form.locator("label", { hasText: /^Control line$/ }).click();
  await form.getByLabel("Kit lot").fill(kit);
  await form.locator("label", { hasText: new RegExp(`^${outcome}$`) }).click();
  await form.getByTestId("run-qc-save").click();
  await expect(qc.getByText("Control recorded on the run.")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

test(
  "the run's QC verdict reaches the Validation row, an accepted failure included",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(300_000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1200V-${run}`;
    const values = [
      { name: `Run QC failed ${run}`, code: "RVFAIL", value: "50" },
      { name: `Run QC passed ${run}`, code: "RVPASS", value: "60" },
    ].map((entry) => ({
      ...entry,
      loinc: `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`,
    }));
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const seeded = await seedNumericTests(page, run, values, {
      testSection: "Molecular Biology",
      sampleType: "Plasma",
    });
    for (const target of seeded) {
      await setRanges(page, target.id);
    }
    const [failed, passed] = seeded;

    const name = `Run validation ${run}`;
    const draft: Draft = await send(
      page,
      "post",
      "/analyzer-types/cepheid-genexpert-astm/duplicate",
      { sourceRevision: 1, displayName: name },
    );
    const defaults = draft.profile.configDefaults as {
      extractionOverrides: { resultParts: Record<string, string> };
    };
    defaults.extractionOverrides.resultParts.number = "R.4.1";
    delete defaults.extractionOverrides.resultParts.call;
    await publish(
      page,
      draft,
      values.map(({ code, loinc }) => numeric(code, loinc)),
    );
    const analyzer = await activateShippedAnalyzer(
      page,
      {
        profileId: draft.profile.profileMeta.id,
        revision: 1,
        displayName: name,
        labUnit: "Molecular Biology",
      },
      name,
      { senderId: sender },
    );
    const source = await geneXpertMockSource(page.request);
    const order = await createClinicalOrder(page, {
      testIds: seeded.map((target) => target.id),
      specimenName: "Plasma",
    });
    const received = await page.request.post(
      `${process.env.MOCK_SIMULATOR_URL || "http://localhost:8085"}/simulate/astm/mindray_ba88a`,
      {
        data: {
          destination: source.destination,
          sender_id: sender,
          sample_id: order.accession,
          completed_at: "20261124110000",
          results: values.map(({ code, value }) => ({
            test_code: code,
            type: "TEXT",
            value,
            unit: "mmol/L",
          })),
        },
      },
    );
    expect(received.ok(), await received.text()).toBeTruthy();
    await expect
      .poll(
        async () =>
          (await worklistFor(page, analyzer.id, order.accession)).length,
      )
      .toBe(2);

    let runLabel = "";
    await test.step("One test's control fails and is accepted despite it; the other's passes", async () => {
      await page.goto(`/Results?accessionNumber=${order.accession}`);
      const waiting = page.getByText(/^Waiting in RUN-\d+$/).first();
      await expect(waiting).toBeVisible();
      runLabel = ((await waiting.textContent()) || "").replace(
        "Waiting in ",
        "",
      );
      await waiting.click();
      await expect(page).toHaveURL(/\/Results\?run=\d+$/);

      await recordControlLine(page, failed.id, `KIT-F-${run}`, "Invalid");
      await recordControlLine(page, passed.id, `KIT-P-${run}`, "Valid");
      const line = page
        .getByTestId("run-qc")
        .getByTestId(`run-qc-test-${failed.id}`);
      await line
        .getByRole("button", { name: "Accept despite QC failure" })
        .click();
      const decide = line.getByTestId(`run-qc-decide-${failed.id}`);
      await decide.getByLabel("Reason").fill("Checked against a second run");
      await decide.getByTestId(`run-qc-decide-confirm-${failed.id}`).click();
      await expect(
        line.getByTestId(`run-qc-decision-${failed.id}`),
      ).toContainText("Accept despite QC failure");
    });

    await test.step("Both values are accepted from the run", async () => {
      const valueRows = page
        .getByTestId("run-rows")
        .locator('> tbody > tr[data-testid^="run-row-"]');
      await expect(valueRows).toHaveCount(2);
      for (const remaining of [2, 1]) {
        await valueRows
          .first()
          .getByRole("button", { name: /expand row/i })
          .click();
        const entry = page.locator('[data-testid^="run-row-entry-"]');
        await expect(
          entry.locator('input[id^="unifiedResultValue-"]'),
        ).toHaveValue(/\d/, { timeout: UI_TIMEOUT });
        await entry
          .getByRole("button", { name: /^accept$/i })
          .first()
          .click();
        await signIfAsked(page);
        await expect(valueRows).toHaveCount(remaining - 1, {
          timeout: UI_TIMEOUT,
        });
      }
    });

    const openQueueRow = async (testName: string) => {
      await page.goto("/validation?type=order", {
        waitUntil: "domcontentloaded",
      });
      const main = page.getByRole("main");
      await main.locator("#validationSearch").fill(order.accession);
      const loaded = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/AccessionValidation") &&
          response.request().method() === "GET",
      );
      await main.getByTestId("validation-load").click();
      await loaded;
      const row = main.getByRole("row").filter({ hasText: testName });
      await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
      return { main, row };
    };

    await test.step("Validation reads QC failed on the accepted failure, with its run's record", async () => {
      const { main, row: failedRow } = await openQueueRow(values[0].name);
      await expect(
        failedRow.locator('[data-testid^="check-before-release-"]'),
      ).toContainText("QC fail");
      await failedRow.getByRole("button", { name: "Expand Row" }).click();
      await expect(main.getByTestId("review-qc")).toHaveText(/QC failed/);
      const qcSection = main.getByTestId("ref-section-qc");
      await expect(qcSection).toContainText(runLabel);
      await openSection(qcSection);
      await expect(qcSection.getByTestId("run-qc-provenance-run")).toHaveText(
        runLabel,
      );
      await expect(
        qcSection.getByTestId("run-qc-provenance-decision"),
      ).toContainText(
        "Accept despite QC failure · Reason: Checked against a second run",
      );
      await expect(
        qcSection.locator('[data-testid^="run-qc-provenance-control-"]'),
      ).toContainText(`KIT-F-${run}`);
      await expect(
        qcSection.locator('[data-testid^="run-qc-provenance-control-"]'),
      ).toContainText("Declared by the technician");
      await expect(
        qcSection.locator('[data-testid^="run-qc-provenance-control-"]'),
        "the operator reads as a name and the time on the lab's 24-hour clock",
      ).toHaveText(/ · [^,·]+ · \d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/);
    });

    await test.step("The test whose control passed reads QC passed", async () => {
      const { main, row: passedRow } = await openQueueRow(values[1].name);
      await expect(
        passedRow.locator('[data-testid^="check-before-release-"]'),
      ).not.toContainText("QC fail");
      await passedRow.getByRole("button", { name: "Expand Row" }).click();
      await expect(main.getByTestId("review-qc")).toHaveText(/QC passed/);
      const qcSection = main.getByTestId("ref-section-qc");
      await openSection(qcSection);
      await expect(qcSection.getByTestId("run-qc-provenance-run")).toHaveText(
        runLabel,
      );
      await expect(
        qcSection.getByTestId("run-qc-provenance-verdict"),
      ).toHaveText("Pass");
      await expect(
        qcSection.getByTestId("run-qc-provenance-decision"),
      ).toHaveCount(0);
    });
  },
);
