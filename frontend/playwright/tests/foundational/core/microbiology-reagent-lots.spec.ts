import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import {
  createAndIdentifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
  selectCultureMedium,
} from "../../../helpers/microbiology-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test("offers only eligible culture lots and retains exact culture and AST provenance", async ({
  page,
}, info) => {
  test.setTimeout(120_000);
  const seeded = await seedMicrobiologyMvpCase(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}?section=setup`);
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  const options = await (
    await page.request.get(
      `/api/OpenELIS-Global/rest/microbiology/cases/${seeded.caseId}/cultures/options`,
    )
  ).json();
  const medium = options.media.find((m: { lots: { lotNumber: string }[] }) =>
    m.lots.some((l) => l.lotNumber === "UAT-MICRO-MEDIA-FEFO"),
  );
  expect(medium).toBeTruthy();
  await selectCultureMedium(page, medium.id);
  await dialog
    .getByLabel("Bottle or plate identifier")
    .fill("UAT-MICRO-BOTTLE-R1");
  const lot = dialog.getByLabel("Medium lot", { exact: true });
  expect(await lot.locator("option").allTextContents()).not.toContain(
    "UAT-MICRO-MEDIA-EXPIRED",
  );
  await lot.selectOption({ label: "UAT-MICRO-MEDIA-FEFO" });
  const atmosphere = dialog.getByLabel("Atmosphere", { exact: true });
  const value = await atmosphere
    .locator("option")
    .evaluateAll((rows) =>
      rows.map((r) => (r as HTMLOptionElement).value).find(Boolean),
    );
  await atmosphere.selectOption(value!);
  await dialog.getByLabel("Temperature (°C)", { exact: true }).fill("35");
  await dialog.getByLabel("Incubation duration", { exact: true }).fill("48");
  await dialog
    .getByLabel("Duration unit", { exact: true })
    .selectOption("HOURS");
  await expect(
    dialog.getByRole("button", { name: "Save", exact: true }),
  ).toBeEnabled();
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const row = page.locator('[data-testid^="culture-row-"]').filter({
    has: page.getByRole("heading", { name: /UAT-MICRO-BOTTLE-R1/ }),
  });
  await expect(row).toContainText("UAT-MICRO-MEDIA-FEFO", {
    timeout: LONG_TIMEOUT,
  });
  await page.reload();
  await expect(row).toContainText("UAT-MICRO-MEDIA-FEFO");
  await createAndIdentifyMicrobiologyIsolate(page, seeded);
  await openMicrobiologyCaseSection(page, "Manual AST");
  await page
    .getByRole("button", { name: "Start AST run", exact: true })
    .click();
  const panel = dialog.getByLabel("Panel", { exact: true });
  const panelValue = await panel
    .locator("option")
    .evaluateAll((rows) =>
      rows.map((r) => (r as HTMLOptionElement).value).find(Boolean),
    );
  await panel.selectOption(panelValue!);
  const card = dialog.getByRole("radio", { name: /UAT-MICRO-CARD-FEFO/ });
  await expect(card).toBeEnabled();
  await expect(
    dialog.getByRole("radio", { name: /UAT-MICRO-MEDIA-EXPIRED/ }),
  ).toBeDisabled();
  const scanner = dialog.getByRole("searchbox", {
    name: "Scan or enter lot number",
  });
  await scanner.fill("UAT-MICRO-CARD-FEFO");
  await scanner.press("Enter");
  await expect(card).toBeChecked();
  await expect(dialog.getByLabel(/Reason/)).toHaveCount(0);
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
  const usage = page.getByRole("table", { name: "Recorded lot usage" });
  await expect(usage).toContainText("UAT-MICRO-CARD-FEFO");
  await expect(usage).toContainText("AST setup");
  await page.reload();
  await expect(usage).toContainText("UAT-MICRO-CARD-FEFO");
  await page.screenshot({
    path: info.outputPath("m9-ast-lot-provenance.png"),
    fullPage: true,
  });
});
