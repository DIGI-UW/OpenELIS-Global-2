import { randomUUID } from "node:crypto";
import { expect as baseExpect, test } from "../../../helpers/test-base";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { worklistFor } from "../../../helpers/analyzer-api";
import { acceptAll, savedValue } from "../../../helpers/analyzer-review";
import { csrfToken } from "../../../helpers/api-session";
import { API } from "../../../helpers/analyzer-profile-api";

const expect = baseExpect.configure({ timeout: 20000 });

test(
  "retry is queued honestly and a retained delivery recovers once its sender is configured",
  { tag: ["@astm", "@evidence", "@exceptions"] },
  async ({ page }, testInfo) => {
    test.setTimeout(180000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1493-${run}`;
    const mock = process.env.MOCK_SIMULATOR_URL || "http://localhost:8085";
    const mockName = `retry-${run}`;
    let issueId = "";
    let sourceCreated = false;
    try {
      const created = await page.request.post(`${mock}/analyzers`, {
        data: { name: mockName, template: "genexpert_astm", port: 9600 },
      });
      expect(created.ok(), await created.text()).toBeTruthy();
      sourceCreated = true;
      const network = await created.json();
      const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
      const order = await createClinicalOrder(page, {
        testIds: [testId],
        specimenName: "Plasma",
      });
      const sent = await page.request.post(
        `${mock}/simulate/fixture/${mockName}/hivvl/quantified`,
        {
          data: {
            destination: `tcp://${network.subnet.replace(/\.0\/24$/, ".2")}:12001`,
            sample_id: order.accession,
            sender_id: sender,
            message_time: "20261028150000",
          },
        },
      );
      expect(sent.ok(), await sent.text()).toBeTruthy();
      const issues = async () => {
        const r = await page.request.get(`${API}/analyzer/delivery-issues`);
        expect(r.ok()).toBeTruthy();
        return (await r.json()).data.rows as Array<{
          id: string;
          sourceId: string;
          sender: string;
          state: string;
          failureReason: string;
        }>;
      };
      await expect
        .poll(async () => {
          const found = (await issues()).find(
            (r) => r.sourceId === network.ip && r.sender.startsWith(sender),
          );
          issueId = found?.id || "";
          return found?.failureReason;
        })
        .toBe("UNREGISTERED_SOURCE");
      await page.goto("/AnalyzerResults?view=import-issues", {
        waitUntil: "domcontentloaded",
      });
      const section = page.getByTestId("analyzer-delivery-issues");
      const ownRow = () =>
        section
          .getByRole("row")
          .filter({ hasText: network.ip })
          .filter({ hasText: sender });
      await expect(ownRow()).toContainText("Not delivered");
      const retry = async () => {
        const response = page.waitForResponse(
          (r) =>
            r.request().method() === "POST" &&
            r
              .url()
              .endsWith(
                `/delivery-issues/${encodeURIComponent(issueId)}/retry`,
              ),
        );
        await ownRow()
          .getByRole("button", { name: "Retry", exact: true })
          .click();
        const queued = await response;
        expect(queued.status()).toBe(202);
        expect(await queued.json()).toMatchObject({
          status: "accepted",
          state: "QUEUED",
          id: issueId,
        });
        await expect(
          section.getByText(/Retry requested.*delivery is not yet confirmed/),
        ).toBeVisible();
      };
      await retry();
      await expect
        .poll(async () => (await issues()).find((r) => r.id === issueId)?.state)
        .toBe("DMQ");
      await section
        .getByRole("button", { name: "Refresh delivery status" })
        .click();
      await expect(ownRow()).toContainText("Not delivered");
      await expect(ownRow()).toContainText("no saved analyzer connection");
      await page.screenshot({
        path: testInfo.outputPath("retry-still-undelivered.png"),
        fullPage: true,
        animations: "disabled",
      });
      const analyzer = await activateShippedGeneXpert(
        page,
        `Retry recovery ${run}`,
        sender,
      );
      await page.goto("/AnalyzerResults?view=import-issues", {
        waitUntil: "domcontentloaded",
      });
      await retry();
      await expect
        .poll(
          async () =>
            (await worklistFor(page, analyzer.id, order.accession)).length,
        )
        .toBeGreaterThan(0);
      await expect
        .poll(async () => (await issues()).some((r) => r.id === issueId))
        .toBe(false);
      await section
        .getByRole("button", { name: "Refresh delivery status" })
        .click();
      await expect(ownRow()).toHaveCount(0);
      const recoveredRows = await worklistFor(
        page,
        analyzer.id,
        order.accession,
      );
      expect(
        recoveredRows.some((row) => row.rawResultValue?.includes("1009.64")),
      ).toBe(true);
      // This is the reporting value. Original-measurement preservation during
      // clinical acceptance is a separate tracked defect (OGC-1574).
      await acceptAll(page, analyzer, [order.accession]);
      await expect
        .poll(() => savedValue(page, order, testId, "HIV-1 viral load"))
        .toBe("1010");
      await page.goto(`/Results?accessionNumber=${order.accession}`);
      await expect(
        page.getByRole("row").filter({ hasText: "1010" }).first(),
      ).toBeVisible();
      await page.screenshot({
        path: testInfo.outputPath("retry-recovered.png"),
        fullPage: true,
      });
      await testInfo.attach("delivery-recovery", {
        contentType: "application/json",
        body: JSON.stringify({
          source: "synthetic mock ASTM",
          analyzerId: analyzer.id,
          accession: order.accession,
          issueId,
          unchangedCause: "UNREGISTERED_SOURCE",
          retryHttpStatus: 202,
          reportingValue: "1010",
          receivedMeasurement: "1009.64",
        }),
      });
    } finally {
      testInfo.setTimeout(testInfo.timeout + 30_000);
      if (issueId) {
        const current = await page.request.get(
          `${API}/analyzer/delivery-issues`,
          { timeout: 10_000 },
        );
        if (
          current.ok() &&
          (await current.json()).data.rows.some(
            (r: { id: string }) => r.id === issueId,
          )
        )
          await page.request.post(
            `${API}/analyzer/delivery-issues/${issueId}/dismiss`,
            {
              headers: { "X-CSRF-Token": await csrfToken(page) },
              data: {},
              timeout: 10_000,
            },
          );
      }
      if (sourceCreated) {
        const removed = await page.request.delete(
          `${mock}/analyzers/${mockName}`,
        );
        expect(removed.ok()).toBeTruthy();
      }
    }
  },
);
