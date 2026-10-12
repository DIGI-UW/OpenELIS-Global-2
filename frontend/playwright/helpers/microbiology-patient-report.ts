import { expect } from "@playwright/test";
import type { Page } from "@playwright/test";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";
import { LONG_TIMEOUT } from "./timeouts";
import type { SeededMicrobiologyCase } from "./seed-microbiology-data";

export async function readMicrobiologyPatientReport(
  page: Page,
  seeded: SeededMicrobiologyCase,
) {
  await page.goto("/RoutineReport?type=patient&report=patientCILNSP_vreduit", {
    waitUntil: "domcontentloaded",
  });
  await page
    .getByRole("button", { name: "Report By Patient", exact: true })
    .click();
  await page
    .getByLabel("Patient Id", { exact: true })
    .fill(seeded.patientExternalId!);
  await page.locator('[data-cy="searchPatientButton"]').click();
  await page
    .locator(`[data-cy="patient-result-row-${seeded.patientId}"]`)
    .locator("label")
    .click();
  await expect(page.getByTestId("selected-patient")).toContainText(
    "UAT Microbiology",
  );
  let pdf: Buffer | undefined;
  await page.context().route("**/ReportPrint?**", async (route) => {
    // Capture the real server response before Chrome hands it to its PDF viewer.
    const response = await route.fetch();
    expect(response.headers()["content-type"]).toContain("application/pdf");
    pdf = await response.body();
    await route.fulfill({ response });
  });
  try {
    await page
      .getByRole("button", { name: "Generate Printable Version", exact: true })
      .click();
    await expect
      .poll(() => pdf?.subarray(0, 5).toString(), { timeout: LONG_TIMEOUT })
      .toBe("%PDF-");
    const document = await getDocument({
      data: new Uint8Array(pdf!),
      useSystemFonts: true,
    }).promise;
    try {
      const pages: string[] = [];
      for (let index = 1; index <= document.numPages; index++) {
        const content = await (await document.getPage(index)).getTextContent();
        pages.push(
          content.items
            .map((item) => ("str" in item ? item.str : ""))
            .join(" "),
        );
      }
      const text = pages.join(" ").replace(/\s+/g, " ");
      return { text, pdf: pdf! };
    } finally {
      await document.destroy();
    }
  } finally {
    await page.context().unroute("**/ReportPrint?**");
  }
}
