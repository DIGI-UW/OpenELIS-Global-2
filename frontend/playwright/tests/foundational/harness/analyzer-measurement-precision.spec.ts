import { randomInt, randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { geneXpertMockSource } from "../../../helpers/analyzer-native-traffic";
import { activateShippedAnalyzer } from "../../../helpers/analyzer-setup-flow";
import {
  API,
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";
import { acceptAll } from "../../../helpers/analyzer-review";

for (const policy of [
  { places: null, originalDisplay: "1.176", correctedDisplay: "1.18" },
  { places: 0, originalDisplay: "1", correctedDisplay: "1" },
  { places: 1, originalDisplay: "1.2", correctedDisplay: "1.2" },
  { places: 2, originalDisplay: "1.18", correctedDisplay: "1.18" },
]) {
  test(
    `analyzer measurements survive acceptance, editing and release with ${policy.places ?? "unset"} reporting decimal places`,
    {
      tag: ["@astm", "@evidence", "@results"],
    },
    async ({ page }, testInfo) => {
      test.setTimeout(180_000);
      const run = randomUUID().slice(0, 8);
      const name = `Measurement precision ${run}`;
      const sender = `P1574-${run}`;
      const code = "PRECISION";
      const [target] = await seedNumericTests(
        page,
        run,
        [{ name, loinc: `${randomInt(1_000_000, 9_000_000)}-1` }],
        {
          testSection: "Molecular Biology",
          sampleType: "Plasma",
          reportingDecimalPlaces: policy.places,
        },
      );
      // A synthetic single-value ASTM layout, not a vendor certification fixture.
      const draft: Draft = await send(
        page,
        "post",
        "/analyzer-types/cepheid-genexpert-astm/duplicate",
        {
          sourceRevision: 1,
          displayName: name,
        },
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
      type Saved = {
        testId: string;
        testName: string;
        analysisId: string;
        reportingDecimalPlaces: number;
        resultValue: string;
        rawResultValue: string;
        result?: { id?: string; value?: string };
      };
      const evidence: unknown[] = [];
      for (const correction of [null, "1.18"]) {
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
              completed_at: correction ? "20261028190200" : "20261028190100",
              results: [
                {
                  test_code: code,
                  type: "TEXT",
                  value: "1.176",
                  unit: "mmol/L",
                },
              ],
            },
          },
        );
        expect(received.ok(), await received.text()).toBeTruthy();
        expect((await received.json()).pushed).toBe(1);
        await expect
          .poll(
            async () =>
              (await worklistFor(page, analyzer.id, order.accession)).length,
          )
          .toBe(1);
        const [row] = await worklistFor(page, analyzer.id, order.accession);
        expect(row.importIssueReason).toBeFalsy();
        expect(row.rawResultValue).toBe("1.176");
        await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
        const input = page.locator(`[id="resultList${row.id}.result"]`);
        await expect(input).toHaveValue("1.176");
        if (correction) await input.fill(correction);
        await page.screenshot({
          path: testInfo.outputPath(
            correction ? "explicit-correction.png" : "original-measurement.png",
          ),
          fullPage: true,
        });
        await acceptAll(page, analyzer, [order.accession], {
          onScreen: true,
          note: "Synthetic precision regression. Accession verified against this test's order.",
        });
        const read = async () => {
          const response = await page.request.get(
            `${API}/accession-results?accessionNumber=${order.accession}`,
          );
          expect(response.ok()).toBeTruthy();
          const body = (await response.json()) as { testResult: Saved[] };
          const saved = body.testResult.find(
            (item) => item.testId === target.id && item.result?.id,
          );
          expect(saved, "a persisted result for the ordered test").toBeTruthy();
          return saved!;
        };
        const saved = await read();
        expect(saved.reportingDecimalPlaces).toBe(policy.places ?? -1);
        expect(saved.rawResultValue).toBe(correction ?? "1.176");
        expect(saved.result?.value).toBe(correction ?? "1.176");
        expect(saved.resultValue).toBe(
          correction ? policy.correctedDisplay : policy.originalDisplay,
        );
        const history = await page.request.get(
          `${API}/results-entry/analysis/${saved.analysisId}/history`,
        );
        expect(history.ok()).toBeTruthy();
        const audit = (await history.json()) as {
          events: Array<{
            type: string;
            detail: string;
            by: string;
            when: string;
          }>;
        };
        expect(
          audit.events.some(
            (event) =>
              event.type === "RESULT" &&
              event.detail.includes(correction ?? "1.176") &&
              event.by &&
              event.when,
          ),
          "acceptance records the unrounded value, actor and time in history",
        ).toBe(true);
        const receiptId = (row as typeof row & { deliveryReceiptId: string })
          .deliveryReceiptId;
        const receipt = await page.request.get(
          `${API}/analyzer/deliveries/${receiptId}/bundle`,
        );
        expect(receipt.ok()).toBeTruthy();
        expect(await receipt.text()).toContain("1.176");
        await page.goto(`/Results?accessionNumber=${order.accession}`);
        const resultRow = page
          .getByRole("row")
          .filter({ hasText: target.name })
          .first();
        await resultRow
          .getByRole("button", { name: "Edit", exact: true })
          .click();
        await expect(
          page.getByRole("textbox", {
            name: `Result for ${saved.testName}`,
            exact: true,
          }),
        ).toHaveValue(correction ?? "1.176");
        // Opening an unchanged edit must not offer a save or round the stored value.
        await expect(
          resultRow.getByRole("button", { name: "Save", exact: true }),
        ).toHaveCount(0);
        expect((await read()).rawResultValue).toBe(correction ?? "1.176");
        await page.goto(`/validation?labNumber=${order.accession}`);
        const validationRow = page
          .getByRole("row")
          .filter({ hasText: target.name })
          .first();
        await expect(validationRow).toBeVisible();
        await expect(validationRow).toContainText(
          correction ? policy.correctedDisplay : policy.originalDisplay,
        );
        await validationRow
          .getByRole("button", { name: /expand/i })
          .first()
          .click();
        await page.getByTestId("review-modify").click();
        await expect(
          page.getByTestId("review-modify-editor").getByRole("textbox"),
        ).toHaveValue(correction ?? "1.176");
        await page.getByTestId("review-cancel-modification").click();
        const released = page.waitForResponse(
          (response) =>
            response.url().includes(`/analysis/${saved.analysisId}/release`) &&
            response.request().method() === "POST",
        );
        await page.getByRole("button", { name: "Validate & release" }).click();
        await released;
        await expect(
          page.getByText("Nothing is waiting for validation.", { exact: true }),
        ).toBeVisible();
        await expect(validationRow).toHaveCount(0);
        expect((await read()).rawResultValue).toBe(correction ?? "1.176");
        evidence.push({
          accession: order.accession,
          analyzerId: analyzer.id,
          receiptId,
          entered: correction ?? "1.176",
          original: "1.176",
          saved: await read(),
          audit: audit.events,
        });
      }
      await testInfo.attach("measurement-precision", {
        contentType: "application/json",
        body: JSON.stringify(evidence),
      });
    },
  );
}
