import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyEnvironmental } from "../../../helpers/seed-microbiology-data";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { readFile } from "node:fs/promises";

test("routes a site subject, records environmental culture context and releases a Laporan Hasil certificate", async ({
  page,
}, testInfo) => {
  const seeded = await seedMicrobiologyEnvironmental(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByText(/M8 water sampling site/)).toBeVisible();
  await expect(page.getByText(/Outlet tap/)).toBeVisible();
  await expect(page.getByText(/Requester: M8 requester/)).toBeVisible();
  await expect(page.getByText("Date of birth", { exact: true })).toHaveCount(0);
  const purpose = page.getByLabel("Culture purpose", { exact: true });
  await expect(purpose).toHaveValue("ROUTINE_MONITORING");
  await purpose.selectOption("POST_CLEANING_CHECK");
  await page.getByLabel("Replicates", { exact: true }).fill("3");
  const savedContext = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/cases/${seeded.caseId}/context`) &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await savedContext;
  await expect(purpose).toBeEnabled();
  await page.reload();
  await expect(purpose).toHaveValue("POST_CLEANING_CHECK");
  await expect(page.getByLabel("Replicates", { exact: true })).toHaveValue("3");
  const section = page.getByLabel("Testing section", { exact: true });
  await section.selectOption("CULTURE");
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Bottle or plate identifier").fill("M8-WATER-PLATE");
  await dialog
    .getByLabel("Medium", { exact: true })
    .selectOption(String(seeded.mediumItemId));
  for (const label of ["Medium lot", "Atmosphere"]) {
    const field = dialog.getByLabel(label, { exact: true });
    const value = await field
      .locator("option")
      .evaluateAll((options) =>
        options
          .map((option) => (option as HTMLOptionElement).value)
          .find(Boolean),
      );
    expect(value).toBeTruthy();
    await field.selectOption(value!);
  }
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const row = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /M8-WATER-PLATE/ }) });
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  await section.selectOption("REPORT");
  await expect(
    page.getByRole("button", { name: "Release final report", exact: true }),
  ).toBeDisabled();
  await section.selectOption("CULTURE");
  await row
    .getByRole("button", { name: "Record outcome", exact: true })
    .click();
  await dialog.getByLabel("Outcome", { exact: true }).selectOption("NO_GROWTH");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(row.getByText("No growth", { exact: true })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await section.selectOption("REPORT");
  await expect(page.getByText(/M8-WATER-PLATE: No growth/)).toBeVisible();
  const versions = page.getByLabel("Report version", { exact: true });
  await expect(versions.locator("option")).toHaveCount(1);
  const release = page.getByRole("button", {
    name: "Release final report",
    exact: true,
  });
  await expect(release).toBeEnabled({ timeout: LONG_TIMEOUT });
  const refreshedCase = page.waitForResponse((response) =>
    response.url().endsWith(`/cases/${seeded.caseId}/shell`),
  );
  await release.click();
  await expect(versions.locator("option")).toHaveCount(2, {
    timeout: LONG_TIMEOUT,
  });
  await (await refreshedCase).finished();
  await page.reload();
  await section.selectOption("CULTURE");
  await expect(
    page.getByRole("button", { name: "Inoculate culture", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Record outcome", exact: true }),
  ).toHaveCount(0);
  await section.selectOption("REPORT");
  const downloaded = page.waitForEvent("download");
  await page
    .getByRole("button", {
      name: "Download Laporan Hasil certificate",
      exact: true,
    })
    .click();
  const certificate = await downloaded;
  const certificatePath = testInfo.outputPath("environmental-certificate.pdf");
  await certificate.saveAs(certificatePath);
  const pdf = await readFile(certificatePath);
  const document = await getDocument({
    data: new Uint8Array(pdf),
    useSystemFonts: true,
  }).promise;
  try {
    let text = "";
    for (let i = 1; i <= document.numPages; i++) {
      const content = await (await document.getPage(i)).getTextContent();
      text += content.items
        .map((item) => ("str" in item ? item.str : ""))
        .join(" ");
    }
    expect(text).toContain("LAPORAN HASIL");
    expect(text).toContain(seeded.accessionNumber);
    expect(text).toContain("M8 water sampling site");
    expect(text).toContain("M8-WATER-PLATE");
    expect(text).toContain("No growth");
    expect(text).not.toContain("UAT Microbiology Patient");
    await testInfo.attach("environmental-certificate.pdf", {
      body: pdf,
      contentType: "application/pdf",
    });
  } finally {
    await document.destroy();
  }
});
