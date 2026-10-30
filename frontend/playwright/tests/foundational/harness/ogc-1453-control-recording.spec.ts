import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { worklistFor } from "../../../helpers/analyzer-api";
import { csrfToken } from "../../../helpers/api-session";
import { API } from "../../../helpers/analyzer-profile-api";
import { applyMappings } from "../../../helpers/analyzer-mapping-page";

test("a held control recovers onto its lot once, while a new run records again", async ({
  page,
}, testInfo) => {
  const run = randomUUID().slice(0, 8);
  const sender = `QC1453-${run}`;
  const specimen = `QC1453-${run}`;
  const analyzer = await activateShippedGeneXpert(
    page,
    `OGC-1453 ${run}`,
    sender,
  );
  const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
  const send = (messageTime: string) =>
    sendGeneXpertFixture(
      page,
      analyzer.id,
      specimen,
      { assay: "hivvl", outcome: "control-quantified" },
      sender,
      {},
      undefined,
      messageTime,
    );

  await send("20261028120000");
  await expect
    .poll(
      async () =>
        (await worklistFor(page, analyzer.id, specimen)).filter(
          (row) => row.importIssueReason === "qc_lot_missing",
        ).length,
      { timeout: 30_000 },
    )
    .toBe(1);
  await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
  await page.getByRole("button", { name: /QC Controls/ }).click();
  await expect(
    page.getByText(/No matching control lot was found/),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("control-held-before-lot.png"),
    fullPage: true,
  });

  // Use the application's lot service; this test owns its analyzer, lot and run IDs.
  const response = await page.request.post(`${API}/qc/controlLot`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: {
      productName: `OGC-1453 control ${run}`,
      lotNumber: specimen,
      controlLevel: "NORMAL",
      testId,
      instrumentId: analyzer.id,
      calculationMethod: "MANUFACTURER_FIXED",
      manufacturerMean: 1009.64,
      manufacturerStdDev: 100,
      activationDate: "2026-10-28T00:00:00Z",
      expirationDate: "2099-12-31T00:00:00Z",
      status: "ACTIVE",
      unitOfMeasure: "copies/mL",
    },
  });
  const lot = (await response.json()) as { id: string };
  const points = async () => {
    const chart = await page.request.get(`${API}/qc/charts/${lot.id}`);
    return (await chart.json()).dataPoints as Array<{
      resultId: string;
      value: number;
    }>;
  };
  expect(await points()).toHaveLength(0);
  await page.goto(`/analyzers/${analyzer.id}/mapping`);
  await applyMappings(page);
  await expect
    .poll(points, { timeout: 30_000 })
    .toEqual([
      expect.objectContaining({ resultId: expect.any(String), value: 1009.64 }),
    ]);
  const firstId = (await points())[0].resultId;

  await send("20261028120000");
  expect((await points()).map((point) => point.resultId)).toEqual([firstId]);
  await send("20261028120100");
  await expect
    .poll(async () => (await points()).length, { timeout: 30_000 })
    .toBe(2);
  expect((await points()).map((point) => point.value)).toEqual([
    1009.64, 1009.64,
  ]);
  await page.goto(`/analyzers/qc/charts/${analyzer.id}`);
  await expect(
    page.getByText(specimen, { exact: false }).first(),
  ).toBeVisible();
  await page.getByRole("tab", { name: "Normal", exact: true }).click();
  await expect(
    page.getByTestId("chart-panel-NORMAL").getByTestId("lj-chart"),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("control-runs-recorded.png"),
    fullPage: true,
  });
  await testInfo.attach("control-recording-evidence", {
    contentType: "application/json",
    body: JSON.stringify(
      {
        analyzerId: analyzer.id,
        lotId: lot.id,
        specimen,
        points: await points(),
      },
      null,
      2,
    ),
  });
});
