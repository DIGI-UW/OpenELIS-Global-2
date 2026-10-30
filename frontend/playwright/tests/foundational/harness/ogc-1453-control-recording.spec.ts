import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import {
  activateShippedAnalyzer,
  activateShippedGeneXpert,
} from "../../../helpers/analyzer-setup-flow";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { worklistFor } from "../../../helpers/analyzer-api";
import { csrfToken } from "../../../helpers/api-session";
import { GENEXPERT } from "../../../helpers/shipped-profiles";
import { API, send } from "../../../helpers/analyzer-profile-api";
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

test("shared lot numbers remain distinguishable and level tabs load the matching control", async ({
  page,
}, testInfo) => {
  const run = randomUUID().slice(0, 8);
  const name = `QC chart ${run}`,
    sender = `QCCHART-${run}`,
    specimen = `QCLOT-${run}`;
  const draft = await send(
    page,
    "post",
    "/analyzer-types/cepheid-genexpert-astm/duplicate",
    { sourceRevision: 1, displayName: name },
  );
  // Synthetic configuration declares HIGH; the vendor fixture carries no level.
  draft.profile.controlResultRecognition.rules[
    "astm-order-action-control"
  ].controlLevel = "HIGH";
  await send(page, "put", `/analyzer-types/drafts/${draft.draftId}`, {
    profile: draft.profile,
  });
  await send(
    page,
    "post",
    `/analyzer-types/drafts/${draft.draftId}/publish`,
    {},
  );
  const analyzer = await activateShippedAnalyzer(
    page,
    {
      ...GENEXPERT,
      profileId: draft.profile.profileMeta.id,
      displayName: name,
    },
    name,
    { senderId: sender },
  );
  try {
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    let highLotId: string;
    for (const controlLevel of ["NORMAL", "HIGH"]) {
      const lot = await send(page, "post", "/qc/controlLot", {
        productName: name,
        lotNumber: specimen,
        controlLevel,
        testId,
        instrumentId: analyzer.id,
        calculationMethod: "MANUFACTURER_FIXED",
        manufacturerMean: 1009.64,
        manufacturerStdDev: 100,
        activationDate: "2026-10-28T00:00:00Z",
        expirationDate: "2099-12-31T00:00:00Z",
        status: "ACTIVE",
        unitOfMeasure: "copies/mL",
      });
      if (controlLevel === "HIGH") highLotId = lot.id;
    }
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      specimen,
      { assay: "hivvl", outcome: "control-quantified" },
      sender,
      {},
      undefined,
      "20261030120000",
    );
    // The transport ACK precedes asynchronous recording; open the chart only
    // after this run exists, rather than assuming a live chart subscription.
    await expect
      .poll(async () => send(page, "get", `/qc/charts/${highLotId}`), {
        timeout: 30_000,
      })
      .toMatchObject({
        dataPoints: [expect.objectContaining({ value: 1009.64 })],
      });
    await page.goto(`/analyzers/qc/charts/${analyzer.id}`);
    await page
      .getByRole("combobox", { name: "Control lot", exact: true })
      .click();
    const normal = `${specimen} — Normal — ${name}`;
    const high = `${specimen} — High — ${name}`;
    await expect(
      page.getByRole("option", { name: normal, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("option", { name: high, exact: true }),
    ).toBeVisible();
    await page.getByRole("option", { name: normal, exact: true }).click();
    await page.getByRole("tab", { name: "High", exact: true }).click();
    await expect(
      page.getByRole("combobox", { name: "Control lot", exact: true }),
    ).toContainText(high);
    const chart = page.getByTestId("chart-panel-HIGH");
    await expect(chart.getByTestId("lj-chart")).toBeVisible();
    // Carbon's point is evidence of a rendered measurement, not merely an empty chart frame.
    await expect(chart.locator("circle.dot")).toHaveCount(1);
    await chart.locator("circle.dot").hover({ timeout: 10_000 });
    await expect(page.locator(".lj-tooltip")).toContainText("1009.64");
    await page.screenshot({
      path: testInfo.outputPath("shared-lot-high-control.png"),
      fullPage: true,
    });
    await page.getByRole("tab", { name: "Normal", exact: true }).click();
    await expect(
      page.getByRole("combobox", { name: "Control lot", exact: true }),
    ).toContainText(normal);
    await expect(
      page.getByTestId("chart-panel-NORMAL").getByText("No QC data available"),
    ).toBeVisible();
  } finally {
    await send(
      page,
      "post",
      `/analyzer/analyzers/${analyzer.id}/deactivate`,
      {},
    );
  }
});
