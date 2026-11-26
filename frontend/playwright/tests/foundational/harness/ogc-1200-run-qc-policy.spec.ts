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
import { signIfAsked } from "../../../helpers/results-nce-ui";

/**
 * OGC-1200 slice 8 (FR-H1, FR-C7): a quality officer requires a control on
 * every run of a test in the Test Catalog QC section, with Hold when it is
 * missing. A run whose message brings no control for the test holds its rows
 * as "QC missing", on the worklist and in the run, until a control is recorded
 * on the run; then the value is accepted like any other.
 */

const UI_TIMEOUT = 20_000;

test(
  "a test that requires a control holds a run without one until it is recorded",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(300_000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1200P-${run}`;
    const code = "RXPOLICY";
    const loinc = `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`;
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const [target] = await seedNumericTests(
      page,
      run,
      [{ name: `Run QC policy ${run}`, code, loinc }],
      { testSection: "Molecular Biology", sampleType: "Plasma" },
    );
    const name = `Run QC policy ${run}`;
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
    await publish(page, draft, [numeric(code, loinc)]);
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

    await test.step("The QC policy requires a control on every run and holds without one", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${target.id}/qc-targets`,
        { waitUntil: "domcontentloaded" },
      );
      const required = page.getByTestId("qc-policy-control-required");
      await expect(required).toHaveValue("no", { timeout: UI_TIMEOUT });
      await expect(
        page.getByTestId("qc-policy-control-when-missing"),
      ).toBeDisabled();
      await required.selectOption("yes");
      await page
        .getByTestId("qc-policy-control-when-missing")
        .selectOption("HOLD");
      await expect(
        page.getByTestId("qc-policy-control-explained"),
      ).toContainText("Every run of this test must carry a control");
      await page.getByTestId("qc-targets-save").click();
      await expect(page.getByText("QC targets saved.")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("qc-policy-control-required")).toHaveValue(
        "yes",
        { timeout: UI_TIMEOUT },
      );
      await expect(
        page.getByTestId("qc-policy-control-when-missing"),
      ).toHaveValue("HOLD");
    });

    await test.step("A run without the control is held as QC missing, on the worklist and in the run", async () => {
      const received = await page.request.post(
        `${process.env.MOCK_SIMULATOR_URL || "http://localhost:8085"}/simulate/astm/mindray_ba88a`,
        {
          data: {
            destination: source.destination,
            sender_id: sender,
            sample_id: order.accession,
            completed_at: "20261125090000",
            results: [
              { test_code: code, type: "TEXT", value: "42", unit: "mmol/L" },
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

      await page.goto(`/Results?accessionNumber=${order.accession}`);
      const tag = page.getByText(/^Held: QC missing in RUN-\d+$/).first();
      await expect(tag).toBeVisible({ timeout: UI_TIMEOUT });
      await tag.click();
      await expect(page).toHaveURL(/\/Results\?run=\d+$/);
      await expect(page.getByTestId("run-qc-overall")).toHaveText("Held");
      await expect(
        page.getByTestId(`run-qc-missing-${target.id}`),
      ).toContainText(
        "requires a control on every run, and none is recorded on this one",
      );
      await expect(
        page.getByTestId("run-rows").getByText("Held: QC missing"),
      ).toBeVisible();
    });

    await test.step("A control recorded on the run releases the test, and the value is accepted", async () => {
      const qc = page.getByTestId("run-qc");
      await qc.getByTestId("run-qc-record").click();
      const form = qc.getByTestId("run-qc-form");
      await form.locator("label", { hasText: /^Control line$/ }).click();
      await form.getByLabel("Kit lot").fill(`KIT-${run}`);
      await form.locator("label", { hasText: /^Valid$/ }).click();
      await form.getByTestId("run-qc-save").click();
      await expect(page.getByTestId("run-qc-overall")).toHaveText("Pass", {
        timeout: UI_TIMEOUT,
      });
      await expect(
        page.getByTestId(`run-qc-missing-${target.id}`),
      ).toBeHidden();

      const valueRows = page
        .getByTestId("run-rows")
        .locator('> tbody > tr[data-testid^="run-row-"]');
      await expect(valueRows.first()).toContainText("Ready");
      await valueRows
        .first()
        .getByRole("button", { name: /expand row/i })
        .click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await expect(
        entry.locator('input[id^="unifiedResultValue-"]'),
      ).toHaveValue("42", { timeout: UI_TIMEOUT });
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      await signIfAsked(page);
      await expect(valueRows).toHaveCount(0, { timeout: UI_TIMEOUT });
    });
  },
);
