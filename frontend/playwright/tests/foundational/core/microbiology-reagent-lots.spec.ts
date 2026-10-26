import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import {
  createAndIdentifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
  selectCultureMedium,
} from "../../../helpers/microbiology-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test(
  "offers only eligible culture lots and retains exact culture and AST provenance",
  { tag: ["@evidence", "@microbiology", "@culture"] },
  async ({ page }, info) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, info);
    await demo.intro(
      "Only usable lots, and exact provenance",
      "Culture media and AST cards offer only lots that are in date and QC-passed, and the case records exactly which lot was used.",
    );
    await demo.caption(
      "Off screen: a culture case is ordered, with medium and AST card lots including an expired one.",
    );
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
    await demo.caption(
      "The expired medium lot is not offered. The first-expiring usable lot is chosen.",
    );
    await demo.highlight(lot);
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
    await demo.highlight(row);
    await demo.caption(
      "Off screen: an isolate is picked and identified. Starting AST asks for the card lot.",
    );
    await createAndIdentifyMicrobiologyIsolate(page, seeded);
    await openMicrobiologyCaseSection(page, "Manual AST");
    await page
      .getByRole("button", { name: "Start AST run", exact: true })
      .click();
    // The scenario's panel is its organism's default, so starting needs no reason.
    await dialog
      .getByLabel("Panel", { exact: true })
      .selectOption(seeded.astPanelId!);
    await dialog
      .getByLabel("Breakpoint standard", { exact: true })
      .selectOption(seeded.activeBreakpointStandardId!);
    const card = dialog.getByRole("radio", { name: /UAT-MICRO-CARD-FEFO/ });
    await demo.caption(
      "The expired lot is shown but cannot be picked. Scanning the card's lot number selects it.",
    );
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
    await demo.caption(
      "The case records the exact card lot used for AST setup.",
    );
    await expect(usage).toContainText("UAT-MICRO-CARD-FEFO");
    await expect(usage).toContainText("AST setup");
    await demo.highlight(usage);
    await page.reload();
    await expect(usage).toContainText("UAT-MICRO-CARD-FEFO");
    await page.screenshot({
      path: info.outputPath("m9-ast-lot-provenance.png"),
      fullPage: true,
    });
    await demo.verified(
      "Only usable lots offered, and each use recorded",
      "The culture used UAT-MICRO-MEDIA-FEFO and AST used UAT-MICRO-CARD-FEFO. Both survive a reload.",
    );
  },
);
