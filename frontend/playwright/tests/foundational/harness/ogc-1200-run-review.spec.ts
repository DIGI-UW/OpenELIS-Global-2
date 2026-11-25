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
import { acceptAll } from "../../../helpers/analyzer-review";

/**
 * OGC-1200 slice 2: a run's values are judged against the patient's ranges,
 * the worklist chips filter the run with counts over the whole run, and a
 * selection follows the run and is summarised from the run, the chip and its
 * exclusions.
 */

const CATALOG = "/api/OpenELIS-Global/rest/test-catalog";

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
  "a run's chips count the whole run and a selection follows the run",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1200B-${run}`;
    const values = [
      { name: `Run review normal ${run}`, code: "RVNORMAL", value: "50" },
      { name: `Run review high ${run}`, code: "RVHIGH", value: "120" },
      { name: `Run review panic ${run}`, code: "RVPANIC", value: "200" },
      { name: `Run review second ${run}`, code: "RVSECOND", value: "60" },
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

    const name = `Run review ${run}`;
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
          completed_at: "20261110090000",
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
      .toBe(4);

    await page.goto(`/Results?accessionNumber=${order.accession}`);
    const waiting = page.getByText(/^Waiting in RUN-\d+$/).first();
    await expect(waiting).toBeVisible();
    await waiting.click();
    await expect(page).toHaveURL(/\/Results\?run=\d+$/);

    await test.step("Each chip counts the whole run", async () => {
      await expect(page.getByTestId("run-chip-ALL")).toHaveText("All (4)");
      await expect(page.getByTestId("run-chip-NEEDS_REVIEW")).toHaveText(
        "Needs review (2)",
      );
      await expect(page.getByTestId("run-chip-CRITICAL")).toHaveText(
        "Critical (1)",
      );
      await expect(page.getByTestId("run-chip-ABNORMAL")).toHaveText(
        "Abnormal (1)",
      );
      await expect(page.getByTestId("run-chip-NORMAL")).toHaveText(
        "Normal (2)",
      );
      await expect(page.getByTestId("run-chip-EXCEPTIONS")).toHaveText(
        "Exceptions (0)",
      );
    });

    const rows = page.getByTestId("run-rows").locator("tbody tr");
    await test.step("The run lists critical, then abnormal, then normal values", async () => {
      await expect(rows).toHaveCount(4);
      await expect(rows.nth(0)).toContainText("200");
      await expect(rows.nth(0).getByTestId("flag-CRITICAL")).toBeVisible();
      await expect(rows.nth(1)).toContainText("120");
      await expect(rows.nth(1).getByTestId("flag-ABNORMAL")).toBeVisible();
    });

    await test.step("A chip shows only its rows", async () => {
      await page.getByTestId("run-chip-CRITICAL").click();
      await expect(rows).toHaveCount(1);
      await expect(rows.first()).toContainText("200");
      await page.getByTestId("run-chip-ALL").click();
      await expect(rows).toHaveCount(4);
    });

    await test.step("A selection is summarised by flag and survives a chip change", async () => {
      await rows.nth(0).locator("label").click();
      await rows.nth(2).locator("label").click();
      const bar = page.getByTestId("run-selection");
      await expect(bar).toContainText(
        "Selected values: 2 (1 critical, 0 abnormal, 1 normal).",
      );
      await page.getByTestId("run-chip-NORMAL").click();
      await expect(rows).toHaveCount(2);
      await expect(bar).toContainText("Selected values: 2");

      await page.getByTestId("run-select-all").click();
      await expect(bar).toContainText(
        "Selected values: 2 (0 critical, 0 abnormal, 2 normal).",
      );
      await page.getByTestId("run-clear-selection").click();
      await expect(bar).toHaveCount(0);
    });

    await acceptAll(page, analyzer, [order.accession], {
      note: "OGC-1200 runs slice 2 walk.",
    });
  },
);
