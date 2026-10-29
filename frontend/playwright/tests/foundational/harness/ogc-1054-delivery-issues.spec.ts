import type { Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { geneXpertMockSource } from "../../../helpers/analyzer-native-traffic";

const API = "/api/OpenELIS-Global/rest";

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 undelivered analyzer results", () => {
  test(
    "shows a result the Bridge could not deliver and dismisses it through the visible UI",
    { tag: ["@evidence", "@astm", "@exceptions"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "A result from an instrument nobody set up is shown, not lost",
        "The Bridge cannot say which analyzer sent it, so it is listed as undelivered with the reason.",
      );
      await demo.caption(
        "Off screen: a mock GeneXpert that no analyzer connection names sends an HIV-1 viral load to the Bridge.",
      );
      const source = await geneXpertMockSource(page.request);
      const mockUrl =
        process.env.MOCK_SIMULATOR_URL ||
        process.env.MOCK_URL ||
        "http://localhost:8085";
      const senderId = `UNREGISTERED-${Date.now()}`;
      const accession = `DEV01${String(Date.now()).padStart(15, "0")}`;
      const sent = await page.request.post(
        `${mockUrl}/simulate/fixture/${source.name}/hivvl/quantified`,
        {
          data: {
            destination: source.destination,
            source_ip: source.ip,
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
                sender?: string;
                failureReason: string;
              }>;
            };
          };
          // Pending receipts may not have a sender yet. Match only this run once attributed.
          const issue = data.data.rows.find(
            (row) =>
              row.sourceId === source.ip && row.sender?.startsWith(senderId),
          );
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
      await demo.caption(
        "The Analyzers page flags results that were not delivered.",
      );
      await demo.highlight(banner);

      await banner
        .getByRole("button", { name: "Review undelivered results" })
        .click();
      await expect(page).toHaveURL(/\/AnalyzerResults\?view=import-issues/);

      const section = page.getByTestId("analyzer-delivery-issues");
      await expect(
        section.getByRole("heading", { name: "Undelivered analyzer results" }),
      ).toBeVisible();
      const unrecognizedRow = section
        .getByRole("row")
        .filter({ hasText: senderId });
      await expect(unrecognizedRow).toBeVisible();
      await expect(unrecognizedRow).toContainText("Unrecognized sender");
      await expect(unrecognizedRow).toContainText(
        "The sender matches no saved analyzer connection. Set up the analyzer, then retry.",
      );
      await expect(unrecognizedRow.getByText("Not delivered")).toBeVisible();
      await capture(page, testInfo, "02-undelivered-result-explained");
      await demo.caption(
        "The row names the sender and says why: it matches no saved analyzer connection.",
      );
      await demo.highlight(unrecognizedRow, 3500);
      await demo.caption("The reviewer dismisses it.");

      await unrecognizedRow.getByRole("button", { name: "Dismiss" }).click();

      await expect(unrecognizedRow).not.toBeVisible();
      await capture(page, testInfo, "03-undelivered-result-dismissed");
      await demo.verified(
        "The undelivered result was shown with its reason, then dismissed",
        "Unrecognized sender: set up the analyzer, then retry.",
      );
    },
  );
});
