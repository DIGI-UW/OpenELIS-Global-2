import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyIsolateWorkspace } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import type { Locator } from "@playwright/test";

const firstOption = async (field: Locator) => {
  const value = await field
    .locator("option")
    .evaluateAll((options) =>
      options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
    );
  expect(value).toBeTruthy();
  await field.selectOption(value!);
};
const choose = async (field: Locator, value: string) => {
  await field.fill(value);
  await field.press("ArrowDown");
  await field.press("Enter");
};

test("picks a growth isolate, retains identification history and refers its demand-created specimen", async ({
  page,
}, testInfo) => {
  const seeded = await seedMicrobiologyIsolateWorkspace(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("CULTURE");
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Bottle or plate identifier").fill("M5-GROWTH");
  await firstOption(dialog.getByLabel("Medium lot", { exact: true }));
  await firstOption(dialog.getByLabel("Atmosphere", { exact: true }));
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const culture = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /M5-GROWTH/ }) });
  await expect(culture).toBeVisible({ timeout: LONG_TIMEOUT });
  await culture
    .getByRole("button", { name: "Record outcome", exact: true })
    .click();
  await dialog.getByLabel("Outcome", { exact: true }).selectOption("GROWTH");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await culture
    .getByRole("button", { name: "Pick isolate", exact: true })
    .click();
  await dialog
    .getByLabel("Gram stain", { exact: true })
    .fill("Gram-negative rods");
  await dialog
    .getByLabel("Colony morphology", { exact: true })
    .fill("Smooth colonies");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const isolate = page.locator('[data-testid^="isolate-"]');
  await expect(isolate).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await expect(isolate.getByRole("heading", { name: /ISO-1/ })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await isolate
    .getByRole("button", { name: "Identify organism", exact: true })
    .click();
  await choose(
    dialog.getByRole("combobox", { name: "Organism", exact: true }),
    "Escherichia coli",
  );
  await dialog
    .getByLabel("ID method", { exact: true })
    .selectOption(seeded.methodId!);
  await dialog
    .getByLabel("Local identification status")
    .selectOption("PRELIMINARY");
  await dialog.getByLabel("ID confidence (%)").fill("95");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    isolate.getByRole("button", { name: "Reidentify", exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await isolate
    .getByRole("button", { name: "Reidentify", exact: true })
    .click();
  await dialog.getByLabel("Local identification status").selectOption("FINAL");
  await dialog
    .getByLabel("Re-identification reason")
    .fill("Confirmed using biochemical panel");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await isolate
    .getByRole("button", { name: "Identification history", exact: true })
    .click();
  await expect(isolate.getByRole("table").getByRole("row")).toHaveCount(3);
  await expect(
    isolate.getByText("Confirmed using biochemical panel", { exact: true }),
  ).toBeVisible();
  await isolate
    .getByRole("button", { name: "Refer isolate out", exact: true })
    .click();
  await dialog
    .getByLabel("Isolate sample type", { exact: true })
    .selectOption(seeded.isolateSampleTypeId!);
  await dialog
    .getByLabel("Requested referral or confirmatory test")
    .selectOption(seeded.followUpTestId!);
  await choose(
    dialog.getByRole("combobox", { name: "Reference lab", exact: true }),
    `UAT reference ${seeded.accessionNumber.slice("UATMICRO".length)}`,
  );
  await dialog
    .getByLabel("Reason", { exact: true })
    .selectOption(seeded.referralReasonId!);
  const config = await (
    await page.request.get("/api/OpenELIS-Global/rest/configuration-properties")
  ).json();
  const [year, month, day] = seeded.collectionLocalDate!.split("-");
  const handoffDate =
    config.DEFAULT_DATE_LOCALE === "fr-FR"
      ? `${day}/${month}/${year}`
      : `${month}/${day}/${year}`;
  await dialog.getByLabel("Handoff Date", { exact: true }).fill(handoffDate);
  await dialog.getByLabel("Handoff Time", { exact: true }).fill("10:30");
  await dialog
    .getByLabel("Notes", { exact: true })
    .fill("Transport sealed at ambient temperature");
  await dialog
    .getByRole("button", { name: "Save Referral", exact: true })
    .click();
  await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await expect(
    page
      .getByRole("table", { name: "Isolate and specimen referrals" })
      .getByText("Draft", { exact: true }),
  ).toBeVisible();
  await page.reload();
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("ISOLATES");
  await expect(page.getByText("Referred", { exact: true }).first()).toBeVisible(
    { timeout: LONG_TIMEOUT },
  );
  await expect(
    isolate.getByText(`${seeded.accessionNumber}-ISO-1`, { exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("isolate-referral-desktop.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(isolate.getByRole("heading", { name: /ISO-1/ })).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("isolate-referral-mobile.png"),
    fullPage: true,
  });
});

test("received isolate preserves sender identification and requires validator acknowledgment of mismatch", async ({
  page,
}) => {
  const seeded = await seedMicrobiologyIsolateWorkspace(page, true);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("ISOLATES");
  const isolate = page.locator('[data-testid^="isolate-"]');
  await expect(isolate).toHaveCount(1, { timeout: LONG_TIMEOUT });
  const dialog = page.getByRole("dialog");
  await expect(
    isolate.getByText("Growth (received isolate)", { exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await isolate
    .getByRole("button", { name: "Record sender identification", exact: true })
    .click();
  await choose(
    dialog.getByRole("combobox", { name: "Sending organization", exact: true }),
    `UAT reference ${seeded.accessionNumber.slice("UATMICRO".length)}`,
  );
  await dialog
    .getByLabel("Sender-reported identification text", { exact: true })
    .fill("Sender preliminary organism");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
  await isolate
    .getByRole("button", { name: "Identify organism", exact: true })
    .click();
  await choose(
    dialog.getByRole("combobox", { name: "Organism", exact: true }),
    "Escherichia coli",
  );
  await dialog
    .getByLabel("ID method", { exact: true })
    .selectOption(seeded.methodId!);
  await dialog.getByLabel("Local identification status").selectOption("FINAL");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    isolate.getByText("Sender and local organism identifications differ", {
      exact: true,
    }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    isolate.getByText("Sender preliminary organism", { exact: true }),
  ).toBeVisible();
  await isolate
    .getByRole("button", {
      name: "Acknowledge identification mismatch",
      exact: true,
    })
    .click();
  await dialog
    .getByLabel("Re-identification reason")
    .fill("Reviewed sending organization report");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(isolate.getByText(/Mismatch acknowledged by/)).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
});
