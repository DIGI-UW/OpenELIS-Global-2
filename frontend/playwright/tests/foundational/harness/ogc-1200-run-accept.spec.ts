import { randomInt, randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
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
 * OGC-1200 slice 3: a run row expands into the Results Entry row of its
 * pending test with the analyzer's value prefilled and editable. Accepting it
 * is a Results Entry save: an edited value is saved as entered, a critical one
 * asks for the acknowledgement first, and the value leaves the run.
 */

const CATALOG = "/api/OpenELIS-Global/rest/test-catalog";
const UI_TIMEOUT = 20_000;

/** Normal 5 to 100, critical below 2 or above 150, on the test's primary part. */
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

test(
  "a run row is accepted through its Results Entry row",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1200C-${run}`;
    const values = [
      { name: `Run accept normal ${run}`, code: "RANORMAL", value: "50" },
      { name: `Run accept panic ${run}`, code: "RAPANIC", value: "200" },
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

    const name = `Run accept ${run}`;
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
          completed_at: "20261111090000",
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

    await page.goto(`/Results?accessionNumber=${order.accession}`);
    const waiting = page.getByText(/^Waiting in RUN-\d+$/).first();
    await expect(waiting).toBeVisible();
    await waiting.click();
    await expect(page).toHaveURL(/\/Results\?run=\d+$/);

    const rows = page.getByTestId("run-rows").locator("> tbody > tr");
    const valueRows = page
      .getByTestId("run-rows")
      .locator('> tbody > tr[data-testid^="run-row-"]');
    await expect(valueRows).toHaveCount(2);

    await test.step("An edited value is accepted as entered", async () => {
      const normal = valueRows.filter({ hasText: values[0].code });
      await normal.getByRole("button", { name: /expand row/i }).click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await expect(entry.getByTestId("run-row-provenance")).toContainText(
        "reported 50",
      );
      const input = entry.locator('input[id^="unifiedResultValue-"]');
      await expect(input).toHaveValue("50", { timeout: UI_TIMEOUT });
      await expect(
        entry.getByTestId("run-entry-analyzer-reported"),
      ).toBeVisible();
      await input.fill("51");
      await expect(entry.getByTestId("run-entry-edited")).toHaveText(
        "Edited, analyzer reported 50",
      );
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      await signIfAsked(page);
      await expect(valueRows).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(page.getByTestId("run-chip-ALL")).toHaveText("All (1)");
    });

    await test.step("A critical value is acknowledged before it is accepted", async () => {
      await valueRows
        .first()
        .getByRole("button", { name: /expand row/i })
        .click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await expect(
        entry.locator('input[id^="unifiedResultValue-"]'),
      ).toHaveValue("200", { timeout: UI_TIMEOUT });
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      const modal = page.locator(".cds--modal.is-visible");
      await expect(page.getByTestId("result-alert-modal")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await modal.getByRole("button", { name: /Acknowledge and save/ }).click();
      await signIfAsked(page);
      await expect(valueRows).toHaveCount(0, { timeout: UI_TIMEOUT });
      await expect(rows).toHaveCount(0);
    });

    await test.step("Both values are results on the worklist and nothing waits", async () => {
      await page.goto(`/Results?accessionNumber=${order.accession}`);
      const inputs = page.locator('input[id^="unifiedResultValue-"]');
      const resultRow = (name: string) =>
        page.locator("tbody > tr").filter({ hasText: name });
      await expect(resultRow(values[0].name)).toContainText(
        "Value used for clinical decisions: 51",
        { timeout: UI_TIMEOUT },
      );
      await expect(resultRow(values[1].name)).toContainText(
        "Value used for clinical decisions: 200",
      );
      await expect(page.getByText(/^Waiting in RUN-\d+$/)).toHaveCount(0);
      await expect(inputs).toHaveCount(0);
    });
  },
);
