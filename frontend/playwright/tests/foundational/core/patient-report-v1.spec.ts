import { test, expect, type Page } from "../../../helpers/test-base";
import {
  createSampleOrder,
  enterResults,
  validateResults,
} from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-932: Reports › Patient Status Report prints the redesigned patient
 * report for an order, both while a result awaits validation and once it is
 * released.
 *
 * The order is created and resulted through the app's API. The report is
 * requested through the form exactly as a user requests it, and the URL the
 * form opens is then fetched with the user's session so the PDF it serves can
 * be checked: a headless browser downloads a PDF rather than showing it. The
 * report's content is covered by the backend tests that render it.
 */

async function printedReport(page: Page, accession: string): Promise<Buffer> {
  await page.goto("/RoutineReport?type=patient&report=patientCILNSP_vreduit", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Patient Status Report" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  const byLabNumber = page.getByRole("button", {
    name: "Report By Lab Number",
    exact: true,
  });
  await byLabNumber.click();
  await expect(byLabNumber).toHaveAttribute("aria-expanded", "true");
  await page
    .getByRole("textbox", { name: "From", exact: true })
    .fill(accession);

  await page
    .context()
    .route("**/api/OpenELIS-Global/ReportPrint?**", (route) =>
      route.fulfill({ status: 200, contentType: "text/html", body: "" }),
    );
  const popupPromise = page.waitForEvent("popup");
  await page
    .getByRole("button", { name: "Generate Printable Version" })
    .click();
  const popup = await popupPromise;
  await popup.waitForURL(/\/ReportPrint\?/);
  const url = popup.url();
  await popup.close();
  await page.context().unroute("**/api/OpenELIS-Global/ReportPrint?**");

  expect(new URL(url).searchParams.get("accessionDirect")).toBe(accession);
  const response = await page.request.get(url);
  expect(response.headers()["content-type"]).toContain("application/pdf");
  return response.body();
}

test("Patient Status Report prints an order awaiting validation and once released", async ({
  page,
}) => {
  const accession = await test.step("create an order", () =>
    createSampleOrder(page, { labNo: "", receivedDate: "", receivedTime: "" }));
  expect(accession).not.toBe("");

  await test.step("enter its result", () => enterResults(page, accession));
  const pending = await test.step("print it awaiting validation", () =>
    printedReport(page, accession));
  expect(pending.subarray(0, 5).toString()).toBe("%PDF-");

  await test.step("release it", () => validateResults(page, accession));
  const released = await test.step("print it released", () =>
    printedReport(page, accession));
  expect(released.subarray(0, 5).toString()).toBe("%PDF-");
  expect(released.toString("latin1")).toContain("DejaVuSans");
});
