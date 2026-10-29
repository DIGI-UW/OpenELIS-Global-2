import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { worklistFor } from "../../../helpers/analyzer-api";
import { deactivateAnalyzerByName } from "../../../helpers/deactivate-analyzer";

test("an open analyzer list detects stopped reception, recovers, and receives an instrument result", async ({
  page,
}) => {
  test.setTimeout(120_000);
  const run = randomUUID().slice(0, 8);
  const name = `Live connection ${run}`;
  const sender = `GX-LIVE-${run}`;
  const analyzer = await activateShippedGeneXpert(page, name, sender);
  const row = page.getByTestId(`analyzer-row-${analyzer.id}`);
  const status = row.getByTestId(`status-badge-${analyzer.id}`);
  const endpoint = `/api/OpenELIS-Global/rest/analyzer/analyzers/${analyzer.id}`;

  try {
    await expect(row).toContainText("Bridge reception active");
    await expect(status).toHaveText("Active");

    // Another operator stops reception while this operator leaves the list open.
    const stopped = await page.request.post(`${endpoint}/deactivate`, {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: {},
    });
    expect(stopped.ok()).toBeTruthy();
    expect((await stopped.json()).deactivated).toBe(true);
    await expect(status).toHaveText("Reception stopped", { timeout: 45_000 });

    await row.getByTestId(`analyzer-row-overflow-${analyzer.id}`).click();
    await page.getByTestId(`analyzer-action-reactivate-${analyzer.id}`).click();
    await page
      .getByRole("button", { name: "Reactivate analyzer", exact: true })
      .click();
    await expect(status).toHaveText("Active");
    await expect(row).toContainText("Bridge reception active");

    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const order = await createClinicalOrder(page, {
      testIds: [testId],
      specimenName: "Plasma",
    });
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      order.accession,
      { assay: "hivvl", outcome: "quantified" },
      sender,
    );
    await expect
      .poll(async () =>
        (await worklistFor(page, analyzer.id, order.accession))
          .filter((result) => result.rawTestCode === "HIVVL")
          .map((result) => result.rawResultValue),
      )
      .toContain("1009.64");
  } finally {
    await deactivateAnalyzerByName(page, name);
  }
});
