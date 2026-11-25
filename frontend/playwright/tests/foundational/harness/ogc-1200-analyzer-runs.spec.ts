import { randomInt, randomUUID } from "node:crypto";
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
 * OGC-1200 slice 1: an analyzer message opens a run. The worklist names the run
 * a pending test's value waits in and offers no entry around it; the run opens
 * on /Results with its settings, progress, values and raw message; leaving it
 * restores the worklist; accepting its last value completes it.
 */
test(
  "an analyzer message opens a run that the worklist names and opens",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(180_000);
    const run = randomUUID().slice(0, 8);
    const name = `Runs slice ${run}`;
    const sender = `R1200-${run}`;
    const code = "RUNVALUE";
    const [target] = await seedNumericTests(
      page,
      run,
      [{ name, loinc: `${randomInt(1_000_000, 9_000_000)}-1` }],
      { testSection: "Molecular Biology", sampleType: "Plasma" },
    );
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
    await publish(page, draft, [numeric(code, target.loinc)]);
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
      testIds: [target.id],
      specimenName: "Plasma",
    });
    const received = await page.request.post(
      `${process.env.MOCK_SIMULATOR_URL || "http://localhost:8085"}/simulate/astm/mindray_ba88a`,
      {
        data: {
          destination: source.destination,
          sender_id: sender,
          sample_id: order.accession,
          completed_at: "20261104090000",
          results: [
            { test_code: code, type: "TEXT", value: "4.2", unit: "mmol/L" },
          ],
        },
      },
    );
    expect(received.ok(), await received.text()).toBeTruthy();
    await expect
      .poll(
        async () =>
          (await worklistFor(page, analyzer.id, order.accession)).length,
      )
      .toBe(1);

    // The worklist names the run and offers no entry around it.
    await page.goto(`/Results?accessionNumber=${order.accession}`);
    const row = page.getByRole("row").filter({ hasText: target.name }).first();
    const waiting = row.getByText(/^Waiting in RUN-\d+$/);
    await expect(waiting).toBeVisible();
    const runLabel = (await waiting.textContent())!.replace("Waiting in ", "");
    const runId = runLabel.replace("RUN-", "");
    await expect(row.locator('input[id^="unifiedResultValue-"]')).toHaveCount(
      0,
    );
    await expect(
      row.getByRole("button", { name: "Save", exact: true }),
    ).toHaveCount(0);

    // Opening it replaces the worklist's filters with the run.
    await waiting.click();
    await expect(page).toHaveURL(new RegExp(`/Results\\?run=${runId}$`));
    await expect(page.getByTestId("run-subtitle")).toHaveText(runLabel);
    await expect(page.getByText("The run is the filter")).toBeVisible();
    const settings = page.getByTestId("run-settings");
    await expect(settings).toContainText("Analyzer message");
    await expect(settings).toContainText(name);
    await expect(page.getByTestId("run-progress")).toHaveText(
      "0 of 1 rows dispositioned",
    );
    await expect(
      page.getByTestId("run-rows").getByRole("row").filter({
        hasText: order.accession,
      }),
    ).toContainText("4.2");
    await page.getByRole("button", { name: "View raw", exact: true }).click();
    await expect(page.getByTestId("run-raw")).toContainText("4.2");

    // Going back restores the worklist as it was.
    await page
      .getByRole("button", { name: "Back to all results", exact: true })
      .click();
    await expect(page).toHaveURL(
      new RegExp(`accessionNumber=${order.accession}`),
    );
    await expect(
      page
        .getByRole("row")
        .filter({ hasText: target.name })
        .first()
        .getByText(runLabel, { exact: false }),
    ).toBeVisible();

    // Accepting the run's last value completes it.
    await acceptAll(page, analyzer, [order.accession], {
      note: "OGC-1200 runs slice 1 walk.",
    });
    await page.goto(`/Results?run=${runId}`);
    await expect(page.getByTestId("run-progress")).toHaveText(
      "1 of 1 rows dispositioned",
    );
    await expect(page.getByTestId("run-setting-run.label.state")).toContainText(
      "Complete",
    );
    await page.goto(`/Results?accessionNumber=${order.accession}`);
    await expect(
      page.getByRole("row").filter({ hasText: target.name }).first(),
    ).not.toContainText("Waiting in");
  },
);
