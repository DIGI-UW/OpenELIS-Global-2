import { expect, type Page } from "@playwright/test";
import { NAV_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/** Generate the patient report through its public form and retain the actual PDF. */
export async function printedReport(
  page: Page,
  accession: string,
): Promise<Buffer> {
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
