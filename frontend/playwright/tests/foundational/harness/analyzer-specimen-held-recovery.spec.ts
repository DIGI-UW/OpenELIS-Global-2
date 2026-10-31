import { randomInt, randomUUID } from "node:crypto";
import { expect as baseExpect, test } from "../../../helpers/test-base";
import { worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { activateShippedAnalyzer } from "../../../helpers/analyzer-setup-flow";
import {
  API,
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";

const expect = baseExpect.configure({ timeout: 15_000 });

test(
  "specimen-held results stay intact until compatible choices allow one sample to be saved",
  { tag: ["@astm", "@evidence", "@results"] },
  async ({ page }, testInfo) => {
    test.setTimeout(120_000);
    const run = randomUUID().slice(0, 8);
    const name = `Specimen recovery ${run}`;
    const sender = `P1576-${run}`;
    const targets = await seedNumericTests(
      page,
      run,
      [
        {
          name: `Specimen A ${run}`,
          loinc: `${randomInt(1000000, 9000000)}-1`,
        },
        {
          name: `Specimen B ${run}`,
          loinc: `${randomInt(1000000, 9000000)}-2`,
        },
      ],
      { testSection: "Molecular Biology", sampleType: "Serum|Plasma" },
    );
    const draft: Draft = await send(
      page,
      "post",
      "/analyzer-types/cepheid-genexpert-astm/duplicate",
      { sourceRevision: 1, displayName: name },
    );
    const defaults = draft.profile.configDefaults as {
      numberFormat: string;
      extractionOverrides: { resultParts: Record<string, string> };
    };
    defaults.numberFormat = ".";
    defaults.extractionOverrides.resultParts.number = "R.4.1";
    delete defaults.extractionOverrides.resultParts.call;
    await publish(
      page,
      draft,
      targets.map((target, i) => numeric(`P1576${i}`, target.loinc)),
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
    const generated = await page.request.get(
      `${API}/SampleEntryGenerateScanProvider`,
    );
    const accession = (await generated.json()).body;
    expect(accession).toBeTruthy();
    const probe = await send(
      page,
      "post",
      `/analyzer/analyzers/${analyzer.id}/test-connection`,
      {},
    );
    const listener = probe.checks.find(
      (c: { key: string }) => c.key === "listener",
    );
    expect(listener.status).toBe("PASSED");
    // Generic mock traffic proves the review workflow, not a manufacturer's layout.
    const mock =
      process.env.MOCK_SIMULATOR_URL ||
      process.env.MOCK_URL ||
      "http://localhost:8085";
    await page.request.post(`${mock}/simulate/astm/mindray_ba88a`, {
      data: {
        destination: `tcp://bridge.openelis.org:${listener.details.port}`,
        sender_id: sender,
        sample_id: accession,
        completed_at: "20261028140000",
        results: [
          {
            test_code: "P15760",
            type: "TEXT",
            value: "1.02",
            unit: "mmol/L",
          },
          {
            test_code: "P15761",
            type: "TEXT",
            value: "2.34",
            unit: "mmol/L",
          },
        ],
      },
    });
    const rows = () => worklistFor(page, analyzer.id, accession);
    await expect.poll(async () => (await rows()).length).toBe(2);
    const original = await rows();
    const noOrder = async () =>
      expect(
        (
          await page.request.get(`${API}/order/search?labNumber=${accession}`)
        ).status(),
      ).toBe(404);
    await noOrder();
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
    const choose = async (specimens: string[]) => {
      for (const [i, row] of original.entries()) {
        const select = page.locator(
          `[id="resultList${row.id}.typeOfSampleId"]`,
        );
        await select.selectOption({ label: specimens[i] });
      }
    };
    const save = async (held: boolean) => {
      for (const row of original) {
        const box = page.locator(`[id="resultList${row.id}.isAccepted"]`);
        await expect(box).toBeAttached();
        await page
          .locator(`[id="resultList${row.id}.note"]`)
          .fill("Synthetic mock patient; review workflow test.");
        if (!(await box.isChecked()))
          await page
            .locator(`label[for="resultList${row.id}.isAccepted"]`)
            .click();
      }
      const response = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          r.url().endsWith("/rest/AnalyzerResults"),
      );
      await page.getByRole("button", { name: "Save", exact: true }).click();
      await response;
      if (held) {
        await expect(page.getByText("Specimen decision required")).toHaveCount(
          2,
        );
        await expect(
          page.getByText(
            /Choose the specimen or analysis requested on each row/,
          ),
        ).toBeVisible();
      } else {
        await expect(page.getByTestId("analyzer-results-empty")).toBeVisible();
      }
    };
    // Submit through normal review without a choice to create the real held state.
    await save(true);
    await expect
      .poll(
        async () =>
          (await rows()).filter(
            (r) => r.importIssueReason === "awaiting_specimen",
          ).length,
      )
      .toBe(2);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
    await expect(
      page.getByText(/Choose the specimen or analysis requested on each row/),
    ).toBeVisible();
    await expect(page.getByText("Specimen decision required")).toHaveCount(2);
    await expect(
      page.getByText(/Choose a sample type before saving/),
    ).toHaveCount(2);
    await expect(
      page.getByText("Held results cannot be accepted", { exact: false }),
    ).toHaveCount(0);
    await page.screenshot({
      path: testInfo.outputPath("specimen-held.png"),
      fullPage: true,
    });
    // Different specimen choices cannot put two tests on an unsuitable new tube.
    await choose(["Serum", "Plasma"]);
    await save(true);
    await expect
      .poll(
        async () =>
          (await rows()).filter(
            (r) => r.importIssueReason === "awaiting_specimen",
          ).length,
      )
      .toBe(2);
    await noOrder();
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
    await choose(["Serum", "Serum"]);
    await save(false);
    await expect.poll(async () => (await rows()).length).toBe(0);
    const orderResponse = await page.request.get(
      `${API}/order/search?labNumber=${accession}`,
    );
    const createdOrder = await orderResponse.json();
    expect(createdOrder.samples).toHaveLength(1);
    const saved = await page.request.get(
      `${API}/accession-results?accessionNumber=${accession}`,
    );
    const results = (await saved.json()).testResult.filter(
      (r: { testId: string; result?: { id?: string } }) =>
        targets.some((t) => t.id === r.testId) && r.result?.id,
    );
    expect(results).toHaveLength(2);
    expect(
      results.map((r: { resultValue: string }) => r.resultValue).sort(),
    ).toEqual(["1.02", "2.34"]);
    await page.goto(`/Results?accessionNumber=${accession}`);
    for (const value of ["1.02", "2.34"])
      await expect(
        page.getByRole("row").filter({ hasText: value }).first(),
      ).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath("specimen-resolved.png"),
      fullPage: true,
    });
    await testInfo.attach("specimen-recovery", {
      contentType: "application/json",
      body: JSON.stringify({
        analyzerId: analyzer.id,
        accession,
        stagedIds: original.map((r) => r.id),
        results: results.map(
          (r: {
            testId: string;
            resultValue: string;
            result: { id: string };
          }) => ({
            testId: r.testId,
            value: r.resultValue,
            resultId: r.result.id,
          }),
        ),
        source: "synthetic mock ASTM",
      }),
    });
  },
);
