import { selectCultureMedium } from "../../../helpers/microbiology-ui";
import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyIsolateWorkspace } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import type { Locator } from "@playwright/test";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

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

test(
  "picks a growth isolate, retains identification history and refers its demand-created specimen",
  { tag: ["@evidence", "@microbiology", "@isolates"] },
  async ({ page }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "From growth to a referred isolate",
      "An isolate is picked from a positive culture, identified, re-identified with a reason, and referred to a reference laboratory.",
    );
    await demo.caption(
      "Off screen: a culture case is ordered, with a reference laboratory and an isolate sample type configured.",
    );
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
    await selectCultureMedium(page, seeded.mediumItemId!);
    await firstOption(dialog.getByLabel("Medium lot", { exact: true }));
    await firstOption(dialog.getByLabel("Atmosphere", { exact: true }));
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const culture = page
      .locator('[data-testid^="culture-row-"]')
      .filter({ has: page.getByRole("heading", { name: /M5-GROWTH/ }) });
    await expect(culture).toBeVisible({ timeout: LONG_TIMEOUT });
    await demo.caption("The bottle grows: its outcome is recorded as growth.");
    await culture
      .getByRole("button", { name: "Record outcome", exact: true })
      .click();
    await dialog.getByLabel("Outcome", { exact: true }).selectOption("GROWTH");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await demo.caption(
      "An isolate is picked from the growth with its Gram stain and colony morphology.",
    );
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
    await demo.highlight(isolate);
    await demo.caption(
      "ISO-1 is identified as Escherichia coli, preliminary, at 95% confidence.",
    );
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
    await demo.caption(
      "Re-identification makes it final, with the reason recorded.",
    );
    await isolate
      .getByRole("button", { name: "Reidentify", exact: true })
      .click();
    await dialog
      .getByLabel("Local identification status")
      .selectOption("FINAL");
    await dialog
      .getByLabel("Re-identification reason")
      .fill("Confirmed using biochemical panel");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await demo.caption(
      "The identification history keeps both identifications and the reason.",
    );
    await isolate
      .getByRole("button", { name: "Identification history", exact: true })
      .click();
    await expect(isolate.getByRole("table").getByRole("row")).toHaveCount(3);
    await expect(
      isolate.getByText("Confirmed using biochemical panel", { exact: true }),
    ).toBeVisible();
    await demo.highlight(isolate.getByRole("table"));
    await demo.caption(
      "The isolate is referred out: its sample type, requested test, reference laboratory, reason and handoff.",
    );
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
      await page.request.get(
        "/api/OpenELIS-Global/rest/configuration-properties",
      )
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
    await demo.caption(
      "After a reload, the isolate shows as referred, with its own isolate sample number.",
    );
    await page.reload();
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ISOLATES");
    await expect(
      page.getByText("Referred", { exact: true }).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      isolate.getByText(`${seeded.accessionNumber}-ISO-1`, { exact: true }),
    ).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath("isolate-referral-desktop.png"),
      fullPage: true,
    });
    const frame = page.viewportSize();
    await demo.caption(
      "On a phone-width screen the isolate fits without sideways scrolling.",
    );
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(isolate.getByRole("heading", { name: /ISO-1/ })).toBeVisible();
    await expect
      .poll(() =>
        isolate.evaluate(
          (element) => element.scrollWidth <= element.clientWidth,
        ),
      )
      .toBe(true);
    await expect
      .poll(() =>
        page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      )
      .toBe(true);
    await page.screenshot({
      path: testInfo.outputPath("isolate-referral-mobile.png"),
      fullPage: true,
    });
    if (frame) await page.setViewportSize(frame);
    await demo.verified(
      "The isolate is identified, with its history, and referred",
      `${seeded.accessionNumber}-ISO-1 kept both identifications and was referred with its own sample.`,
    );
  },
);

test(
  "received isolate preserves sender identification and requires validator acknowledgment of mismatch",
  { tag: ["@evidence", "@microbiology", "@isolates"] },
  async ({ page }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "An isolate received from another laboratory",
      "The sender's identification is kept beside the local one, and a difference between them needs a validator's acknowledgment.",
    );
    await demo.caption(
      "Off screen: a case is opened for an isolate received from another laboratory.",
    );
    const seeded = await seedMicrobiologyIsolateWorkspace(page, true);
    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("CULTURE");
    const culture = page.locator('[data-testid^="culture-row-"]');
    await expect(culture).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await demo.caption(
      "A received isolate is not picked from a local culture, so the culture offers no Pick isolate.",
    );
    await expect(
      culture.getByRole("button", { name: "Pick isolate", exact: true }),
    ).toHaveCount(0);
    await expect(
      culture.getByRole("button", { name: "Edit positive time", exact: true }),
    ).toHaveCount(0);
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ISOLATES");
    const isolate = page.locator('[data-testid^="isolate-"]');
    await expect(isolate).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await expect(
      page.getByRole("button", { name: "Pick isolate", exact: true }),
    ).toBeDisabled();
    const dialog = page.getByRole("dialog");
    await expect(
      isolate.getByText("Growth (received isolate)", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await demo.highlight(
      isolate.getByText("Growth (received isolate)", { exact: true }),
    );
    await demo.caption(
      "The sender's organization, original specimen and reported identification are recorded.",
    );
    await isolate
      .getByRole("button", {
        name: "Record sender identification",
        exact: true,
      })
      .click();
    await choose(
      dialog.getByRole("combobox", {
        name: "Sending organization",
        exact: true,
      }),
      `UAT reference ${seeded.accessionNumber.slice("UATMICRO".length)}`,
    );
    const original = dialog.getByRole("combobox", {
      name: "Original specimen type",
      exact: true,
    });
    await choose(
      original,
      `UAT culture specimen ${seeded.accessionNumber.slice("UATMICRO".length)}`,
    );
    await dialog
      .getByLabel("Sender-reported identification text", { exact: true })
      .fill("Sender preliminary organism");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await demo.caption(
      "The laboratory identifies the isolate itself, as Escherichia coli.",
    );
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
      .selectOption("FINAL");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      isolate.getByText("Sender and local organism identifications differ", {
        exact: true,
      }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      isolate.getByText("Sender preliminary organism", { exact: true }),
    ).toBeVisible();
    await demo.highlight(
      isolate.getByText("Sender and local organism identifications differ", {
        exact: true,
      }),
    );
    await demo.caption(
      "The two identifications differ; the validator acknowledges the mismatch with a reason.",
    );
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
    await demo.highlight(isolate.getByText(/Mismatch acknowledged by/));
    await demo.verified(
      "The mismatch is acknowledged, and both identifications stay",
      "The sender's report and the local Escherichia coli identification are kept side by side.",
    );
  },
);

test(
  "refers the original specimen using only the shared form's required receiving laboratory",
  { tag: ["@evidence", "@microbiology", "@isolates"] },
  async ({ page }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Referring the original specimen",
      "The specimen itself is sent to a reference laboratory with only the fields the shared referral form requires.",
    );
    await demo.caption(
      "Off screen: a culture case is ordered, with a reference laboratory configured.",
    );
    const seeded = await seedMicrobiologyIsolateWorkspace(page);
    const receivingLab = `UAT reference ${seeded.accessionNumber.slice("UATMICRO".length)}`;
    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ISOLATES");
    await demo.caption(
      "The specimen, the requested test and the receiving laboratory are all the form needs.",
    );
    await page
      .getByRole("button", { name: "Refer specimen out", exact: true })
      .click();
    const dialog = page.getByRole("dialog");
    await dialog
      .getByLabel("Isolate or specimen item", { exact: true })
      .selectOption(seeded.sampleItemId);
    await dialog
      .getByLabel("Requested referral or confirmatory test", { exact: true })
      .selectOption(seeded.cultureTestId!);
    await choose(
      dialog.getByRole("combobox", { name: "Reference lab", exact: true }),
      receivingLab,
    );
    await dialog
      .getByRole("button", { name: "Save Referral", exact: true })
      .click();
    await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
    const table = page.getByRole("table", {
      name: "Isolate and specimen referrals",
      exact: true,
    });
    await expect(
      table.getByRole("cell", { name: receivingLab, exact: true }),
    ).toBeVisible();
    await demo.highlight(table);
    await demo.caption(
      "After a reload the referral is still listed, and no isolate was created for it.",
    );
    await page.reload({ waitUntil: "domcontentloaded" });
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ISOLATES");
    await expect(
      table.getByRole("cell", { name: receivingLab, exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.locator('[data-testid^="isolate-"]')).toHaveCount(0);
    await demo.verified(
      "The specimen is referred without creating an isolate",
      `The referral to ${receivingLab} is listed for the case.`,
    );
  },
);
