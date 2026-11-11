import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { test, expect } from "../../../helpers/test-base";
import { captureDebugContext } from "../../../helpers/debug-context";
import { videoPause } from "../../../helpers/video-pause";

// The local workload runner creates real jobs and checks every exported value
// and identity independently. This browser check verifies delivery of those
// qualified files through the same queue and controls used for routine reports.
const receiptPath = process.env.REPORTING_WORKLOAD_RECEIPT;

for (const layout of ["SPREADSHEET", "RESULT_LIST"]) {
  test(`${layout} workload download remains usable at desktop and phone widths`, async ({
    page,
  }, testInfo) => {
    test.skip(
      !receiptPath,
      "Run the disposable reporting workload and provide its verified receipt.",
    );
    testInfo.setTimeout(60_000);
    const receipt = JSON.parse(readFileSync(receiptPath!, "utf8"));
    expect(receipt.status).toBe("passed");
    const file = receipt.csvs.find(
      (item: { layout: string; rowCount: number }) =>
        item.layout === layout && item.rowCount === 50_000,
    );
    expect(file).toBeDefined();
    expect(file.jobId).toMatch(/^[0-9a-f-]{36}$/);
    const browserErrors: string[] = [];
    page.on("pageerror", (error) => browserErrors.push(error.message));
    try {
      for (const [name, width, height] of [
        ["desktop", 1280, 900],
        ["narrow", 390, 844],
      ] as const) {
        await test.step(`${name}: return to the report and download all rows`, async () => {
          await page.setViewportSize({ width, height });
          await page.goto(`/CustomDataExport?view=queue&job=${file.jobId}`);
          await expect(
            page.getByRole("heading", { name: "My Report Queue", exact: true }),
          ).toBeVisible();
          const report = page.getByTestId(`reporting-job-${file.jobId}`);
          await expect(report).toContainText("Ready to download");
          await expect(report).toContainText("50,000 rows");
          const downloadLink = report.getByRole("link", {
            name: "Download CSV",
            exact: true,
          });
          await expect(downloadLink).toBeVisible();
          await report.scrollIntoViewIfNeeded();
          await page.screenshot({
            path: testInfo.outputPath(`workload-${layout}-${name}.png`),
          });
          expect(
            await page.evaluate(
              () => document.documentElement.scrollWidth <= innerWidth,
            ),
          ).toBe(true);
          const completed = page.waitForEvent("download");
          await downloadLink.click();
          const download = await completed;
          expect(await download.failure()).toBeNull();
          const stream = await download.createReadStream();
          const chunks: Buffer[] = [];
          for await (const chunk of stream) chunks.push(Buffer.from(chunk));
          const bytes = Buffer.concat(chunks);
          expect(bytes.length).toBe(file.bytes);
          expect(createHash("sha256").update(bytes).digest("hex")).toBe(
            file.sha256,
          );
          expect(bytes.subarray(0, 3)).toEqual(Buffer.from([0xef, 0xbb, 0xbf]));
          expect(bytes.toString("utf8").split("\r\n")).toHaveLength(50_002);
          await expect(report).toContainText("Ready to download");
          await page.reload();
          await expect(downloadLink).toBeVisible();
        });
      }
      expect(browserErrors).toEqual([]);
      await videoPause(page, 750, testInfo);
    } catch (error) {
      console.log(await captureDebugContext(page, browserErrors));
      throw error;
    }
  });
}
