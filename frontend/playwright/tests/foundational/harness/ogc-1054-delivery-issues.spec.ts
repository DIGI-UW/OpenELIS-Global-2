import type { Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

const API = "/api/OpenELIS-Global/rest";

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 undelivered analyzer results", () => {
  test("shows a result the Bridge could not deliver and dismisses it through the visible UI", async ({
    page,
  }, testInfo) => {
    const mockUrl =
      process.env.MOCK_SIMULATOR_URL ||
      process.env.MOCK_URL ||
      "http://localhost:8085";
    const mockName = `unregistered-${Date.now()}`;
    const senderId = `UNREGISTERED-${Date.now()}`;
    const accession = `DEV01${String(Date.now()).padStart(15, "0")}`;
    const created = await page.request.post(`${mockUrl}/analyzers`, {
      data: { name: mockName, template: "genexpert_astm", port: 9600 },
    });
    expect(
      created.ok(),
      `Create unregistered mock source: ${created.status()}`,
    ).toBeTruthy();
    try {
      const network = (await created.json()) as { subnet: string; ip: string };
      expect(network.subnet).toMatch(/^10\.\d+\.\d+\.0\/24$/);
      const bridgeIp = network.subnet.replace(/\.0\/24$/, ".2");
      const sent = await page.request.post(
        `${mockUrl}/simulate/astm/${mockName}`,
        {
          data: {
            destination: `tcp://${bridgeIp}:12001`,
            sample_id: accession,
            sender_id: senderId,
            results: [{ test_code: "MTB-RIF", value: "NOT DETECTED" }],
          },
        },
      );
      expect(
        sent.ok(),
        `Send unregistered native ASTM: ${sent.status()} ${await sent.text()}`,
      ).toBeTruthy();
      expect(((await sent.json()) as { pushed: number }).pushed).toBe(1);
      let issueId = "";
      let failureReason = "";
      await expect
        .poll(
          async () => {
            const response = await page.request.get(
              `${API}/analyzer/delivery-issues`,
            );
            if (!response.ok()) return null;
            const data = (await response.json()) as {
              data: {
                rows: Array<{
                  id: string;
                  sourceId: string;
                  failureReason: string;
                }>;
              };
            };
            // Source attribution fails before Bridge can parse an accession into the outbox summary.
            const issue = data.data.rows.find(
              (row) => row.sourceId === network.ip,
            );
            issueId = issue?.id || "";
            failureReason = issue?.failureReason || "";
            return issue?.id || null;
          },
          { timeout: LONG_TIMEOUT },
        )
        .not.toBeNull();
      expect(failureReason, `Delivery issue ${issueId}`).toBe(
        "UNREGISTERED_SOURCE",
      );

      await page.goto("/analyzers", {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      const banner = page.getByTestId("delivery-issues-attention");
      await expect(banner).toContainText("not delivered", {
        timeout: LONG_TIMEOUT,
      });
      await capture(page, testInfo, "01-analyzers-undelivered-banner");

      await banner
        .getByRole("button", { name: "Review undelivered results" })
        .click();
      await expect(page).toHaveURL(/\/AnalyzerResults\?view=import-issues/, {
        timeout: LONG_TIMEOUT,
      });

      const section = page.getByTestId("analyzer-delivery-issues");
      await expect(
        section.getByRole("heading", { name: "Undelivered analyzer results" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const unrecognizedRow = section.getByRole("row", {
        name: new RegExp(network.ip.replace(/\./g, "\\.")),
      });
      await expect(unrecognizedRow).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(unrecognizedRow).toContainText("Unrecognized sender");
      await expect(unrecognizedRow).toContainText(
        "The sender matches no saved analyzer connection. Set up the analyzer, then retry.",
      );
      await expect(unrecognizedRow.getByText("Not delivered")).toBeVisible();
      await capture(page, testInfo, "02-undelivered-result-explained");

      await unrecognizedRow.getByRole("button", { name: "Dismiss" }).click();

      await expect(unrecognizedRow).not.toBeVisible({ timeout: LONG_TIMEOUT });
      await capture(page, testInfo, "03-undelivered-result-dismissed");
    } finally {
      const removed = await page.request.delete(
        `${mockUrl}/analyzers/${mockName}`,
      );
      expect(
        removed.ok(),
        `Remove temporary mock source: ${removed.status()}`,
      ).toBeTruthy();
    }
  });
});
