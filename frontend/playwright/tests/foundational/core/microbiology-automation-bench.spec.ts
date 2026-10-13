import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyAutomation,
  seedMicrobiologyCultureWorkspace,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test("places an incoming result and keeps it pending validation", async ({
  page,
}) => {
  test.setTimeout(120000);
  const seeded = await seedMicrobiologyAutomation(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`);
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("INCOMING");
  const row = page
    .getByRole("row")
    .filter({ hasText: "UAT Microbiology follow-up assay" });
  await expect(row).toContainText("Waiting for placement", {
    timeout: LONG_TIMEOUT,
  });
  await row.getByRole("button", { name: "Review placement" }).click();
  await page
    .getByLabel("Destination sample", { exact: true })
    .selectOption(seeded.sampleItemId);
  await page.getByRole("button", { name: "Place result", exact: true }).click();
  await expect(row).toContainText("Placed", { timeout: LONG_TIMEOUT });
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("ADDITIONAL");
  const assay = page
    .getByRole("row")
    .filter({ hasText: "UAT Microbiology follow-up assay" })
    .first();
  await expect(assay).toContainText("12", { timeout: LONG_TIMEOUT });
  await expect(assay).toContainText("Awaiting validation");
  await test.info().attach("placed-result", {
    body: await page.screenshot(),
    contentType: "image/png",
  });
  await page.reload();
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("INCOMING");
  await expect(
    page
      .getByRole("row")
      .filter({ hasText: "UAT Microbiology follow-up assay" }),
  ).toHaveCount(1);
});

test("inoculates from the worklist and scans the saved culture label", async ({
  page,
}) => {
  test.setTimeout(180000);
  const seeded = await seedMicrobiologyCultureWorkspace(page);
  await page.goto("/Microbiology/worklist");
  await page.getByRole("button", { name: "Culture", exact: true }).click();
  await page.locator("#bench-search").fill(seeded.accessionNumber);
  await page.locator("#bench-filter").selectOption("AWAITING_INOCULATION");
  const row = page.getByRole("row").filter({ hasText: seeded.accessionNumber });
  await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await row.locator("label.cds--checkbox-label").click();
  await expect(row.getByRole("checkbox")).toBeChecked();
  await page.getByRole("button", { name: "Inoculate selected cases" }).click();
  await page
    .locator("#bench-cultureTestId")
    .selectOption(seeded.cultureTestId!);
  await page
    .locator("#bench-mediumItemId")
    .selectOption(String(seeded.mediumItemId));
  for (const id of ["bench-lotId", "bench-atmosphereId"]) {
    const select = page.locator(`#${id}`);
    const value = await select
      .locator("option")
      .evaluateAll((options) =>
        options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
      );
    expect(value).toBeTruthy();
    await select.selectOption(value!);
  }
  await page.locator("#bench-duration").fill("0.1");
  const past = new Date(Date.now() - 30 * 60000);
  const local = new Date(past.getTime() - past.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
  await page.locator("#bench-inoculatedAt").fill(local);
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText("Action saved", { exact: false })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await page.locator("#bench-filter").selectOption("FINAL_READ_DUE");
  await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await row.locator("label.cds--checkbox-label").click();
  await expect(row.getByRole("checkbox")).toBeChecked();
  await page
    .getByRole("button", { name: "Print bench sheet", exact: true })
    .click();
  const sheet = page.getByRole("link", { name: /Open saved sheet/ });
  await expect(sheet).toBeVisible();
  const sheetUrl = (await sheet.getAttribute("href"))!;
  const firstPrint = await page.request.get(sheetUrl);
  expect(firstPrint.ok()).toBeTruthy();
  const snapshot = await firstPrint.body();
  expect(snapshot.subarray(0, 4).toString()).toBe("%PDF");
  await page
    .getByRole("button", { name: "Extend incubation by 24 hours" })
    .click();
  const extensionReason = page.locator("#bench-reasonId");
  const reasonId = await extensionReason
    .locator("option")
    .evaluateAll((options) =>
      options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
    );
  await extensionReason.selectOption(reasonId!);
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(row).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await page
    .getByRole("button", { name: "Undo bench action", exact: true })
    .click();
  await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await row.locator("label.cds--checkbox-label").click();
  await expect(row.getByRole("checkbox")).toBeChecked();
  await page
    .getByRole("button", { name: "Record no growth", exact: true })
    .click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(row).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await page
    .getByRole("button", { name: "Undo bench action", exact: true })
    .click();
  await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
  const replay = await page.request.get(sheetUrl);
  expect(await replay.body()).toEqual(snapshot);
  await page.goto(`/Microbiology/cases/${seeded.caseId}?scope=CULTURE`);
  const culture = page.locator('[data-testid^="culture-row-"]').first();
  await expect(culture).toBeVisible({ timeout: LONG_TIMEOUT });
  const id = (await culture.getAttribute("data-testid"))!.replace(
    "culture-row-",
    "",
  );
  await culture
    .getByRole("button", { name: "Container labels", exact: true })
    .click();
  await culture
    .getByLabel("Container label preset", { exact: true })
    .selectOption({ label: "Culture Plate Label" });
  await culture
    .getByLabel("Number of container labels", { exact: true })
    .fill("1");
  const labelResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/containers/labels") &&
      response.request().method() === "POST",
  );
  await culture
    .getByRole("button", { name: "Prepare labels", exact: true })
    .click();
  const printedLabels = await labelResponse;
  expect(printedLabels.ok()).toBeTruthy();
  expect((await printedLabels.body()).subarray(0, 4).toString()).toBe("%PDF");
  await expect(
    culture.getByRole("link", { name: "Open printable labels" }),
  ).toBeVisible();
  await page.goto("/Microbiology/worklist");
  await page.getByRole("heading", { name: "Microbiology worklist" }).waitFor();
  await page.keyboard.type(`PLT-${seeded.caseId}-${id}`);
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(
    new RegExp(`/Microbiology/cases/${seeded.caseId}.*row=${id}`),
    { timeout: LONG_TIMEOUT },
  );
  await expect(page.locator(`[data-testid="culture-row-${id}"]`)).toBeVisible();
  await test.info().attach("scanned-culture", {
    body: await page.screenshot(),
    contentType: "image/png",
  });
});
