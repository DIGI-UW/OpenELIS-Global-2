import type { Page, TestInfo } from "@playwright/test";
import { expect, test as baseTest } from "../../../helpers/test-base";

const API = "/api/OpenELIS-Global/rest";

const test = baseTest.extend<{
  unregisteredSource: {
    mockUrl: string;
    mockName: string;
    network: { subnet: string; ip: string };
  };
}>({
  unregisteredSource: async ({ playwright }, use) => {
    const mockUrl =
      process.env.MOCK_SIMULATOR_URL ||
      process.env.MOCK_URL ||
      "http://localhost:8085";
    const mockName = `unregistered-${Date.now()}`;
    const mockRequest = await playwright.request.newContext();
    let sourceCreated = false;
    try {
      const created = await mockRequest.post(`${mockUrl}/analyzers`, {
        data: { name: mockName, template: "genexpert_astm", port: 9600 },
      });
      expect(
        created.ok(),
        `Create mock source: ${created.status()}`,
      ).toBeTruthy();
      sourceCreated = true;
      const network = (await created.json()) as { subnet: string; ip: string };
      await use({ mockUrl, mockName, network });
    } finally {
      try {
        if (sourceCreated) {
          const removed = await mockRequest.delete(
            `${mockUrl}/analyzers/${mockName}`,
          );
          expect(
            removed.ok(),
            `Remove mock source: ${removed.status()}`,
          ).toBeTruthy();
        }
      } finally {
        await mockRequest.dispose();
      }
    }
  },
});

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 undelivered analyzer results", () => {
  test("shows a result the Bridge could not deliver and dismisses it through the visible UI", async ({
    page,
    unregisteredSource,
  }, testInfo) => {
    const { mockUrl, mockName, network } = unregisteredSource;
    const senderId = `UNREGISTERED-${Date.now()}`;
    const accession = `DEV01${String(Date.now()).padStart(15, "0")}`;
    expect(network.subnet).toMatch(/^10\.\d+\.\d+\.0\/24$/);
    const bridgeIp = network.subnet.replace(/\.0\/24$/, ".2");
    const sent = await page.request.post(
      `${mockUrl}/simulate/fixture/${mockName}/hivvl/quantified`,
      {
        data: {
          destination: `tcp://${bridgeIp}:12001`,
          sample_id: accession,
          sender_id: senderId,
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
      .poll(async () => {
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
        const issue = data.data.rows.find((row) => row.sourceId === network.ip);
        issueId = issue?.id || "";
        failureReason = issue?.failureReason || "";
        return issue?.id || null;
      })
      .not.toBeNull();
    expect(failureReason, `Delivery issue ${issueId}`).toBe(
      "UNREGISTERED_SOURCE",
    );

    await page.goto("/analyzers", {
      waitUntil: "domcontentloaded",
    });
    const banner = page.getByTestId("delivery-issues-attention");
    await expect(banner).toContainText("not delivered");
    await capture(page, testInfo, "01-analyzers-undelivered-banner");

    await banner
      .getByRole("button", { name: "Review undelivered results" })
      .click();
    await expect(page).toHaveURL(/\/AnalyzerResults\?view=import-issues/);

    const section = page.getByTestId("analyzer-delivery-issues");
    await expect(
      section.getByRole("heading", { name: "Undelivered analyzer results" }),
    ).toBeVisible();
    const unrecognizedRow = section.getByRole("row", {
      name: new RegExp(network.ip.replace(/\./g, "\\.")),
    });
    await expect(unrecognizedRow).toBeVisible();
    await expect(unrecognizedRow).toContainText("Unrecognized sender");
    await expect(unrecognizedRow).toContainText(
      "The sender matches no saved analyzer connection. Set up the analyzer, then retry.",
    );
    await expect(unrecognizedRow.getByText("Not delivered")).toBeVisible();
    await capture(page, testInfo, "02-undelivered-result-explained");

    await unrecognizedRow.getByRole("button", { name: "Dismiss" }).click();

    await expect(unrecognizedRow).not.toBeVisible();
    await capture(page, testInfo, "03-undelivered-result-dismissed");
  });
});
