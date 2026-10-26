import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyAutomation,
  seedMicrobiologyCultureWorkspace,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { scanContainerLabel } from "../../../helpers/microbiology-ui";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test(
  "places an incoming result and keeps it pending validation",
  { tag: ["@evidence", "@microbiology", "@worklist"] },
  async ({ page }, testInfo) => {
    test.setTimeout(120000);
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Placing an incoming result",
      "A result that arrives from an instrument or another system waits until a technologist places it on the right sample, and then still needs validation.",
    );
    await demo.caption(
      "Off screen: a case is ordered, and a follow-up assay result arrives for it without a sample.",
    );
    const seeded = await seedMicrobiologyAutomation(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}`);
    await expect(
      page
        .getByRole("status")
        .filter({ hasText: "1 incoming result needs placement" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await demo.caption("The case says one incoming result needs placement.");
    await demo.highlight(
      page
        .getByRole("status")
        .filter({ hasText: "1 incoming result needs placement" }),
    );
    await page
      .getByRole("button", { name: "Place incoming results", exact: true })
      .click();
    const row = page
      .getByRole("row")
      .filter({ hasText: "UAT Microbiology follow-up assay" });
    await expect(row).toContainText("Waiting for placement", {
      timeout: LONG_TIMEOUT,
    });
    await demo.caption(
      "The technologist reviews it and chooses the destination sample.",
    );
    await row.getByRole("button", { name: "Review placement" }).click();
    await page
      .getByLabel("Destination sample", { exact: true })
      .selectOption(seeded.sampleItemId);
    await page
      .getByRole("button", { name: "Place result", exact: true })
      .click();
    await expect(row).toContainText("Placed", { timeout: LONG_TIMEOUT });
    await demo.highlight(row);
    await demo.caption(
      "Placed on the sample, the result appears with its test, still awaiting validation.",
    );
    await expect(
      page
        .getByRole("status")
        .filter({ hasText: "1 incoming result needs placement" }),
    ).toHaveCount(0);
    await page
      .getByLabel("Testing section", { exact: true })
      .selectOption("ADDITIONAL");
    const assay = page
      .getByRole("row")
      .filter({ hasText: "UAT Microbiology follow-up assay" })
      .first();
    await expect(assay).toContainText("12", { timeout: LONG_TIMEOUT });
    await expect(assay).toContainText("Awaiting validation");
    await demo.highlight(assay);
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
    await demo.verified(
      "The incoming result is placed and waits for validation",
      "It sits on the chosen sample as Awaiting validation, and the incoming list keeps its record.",
    );
  },
);

test(
  "inoculates from the worklist and scans the saved culture label",
  { tag: ["@evidence", "@microbiology", "@worklist"] },
  async ({ page }, testInfo) => {
    test.setTimeout(180000);
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Bench work from the worklist",
      "Cases are inoculated in a batch, a bench sheet is printed, bench actions can be undone, and a culture label scan opens the culture.",
    );
    await demo.caption("Off screen: a culture case is ordered.");
    const seeded = await seedMicrobiologyCultureWorkspace(page);
    await page.goto(
      `/Microbiology/worklist?grain=bench&q=${encodeURIComponent(seeded.accessionNumber)}`,
    );
    await expect(page.locator("#bench-search")).toHaveValue(
      seeded.accessionNumber,
    );
    await page.locator("#bench-filter").selectOption("AWAITING_INOCULATION");
    const row = page
      .getByRole("row")
      .filter({ hasText: seeded.accessionNumber });
    await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await demo.caption(
      "The bench worklist shows the case awaiting inoculation. It is selected and inoculated in a batch.",
    );
    await demo.highlight(row);
    await row.locator("label.cds--checkbox-label").click();
    await expect(row.getByRole("checkbox")).toBeChecked();
    await page
      .getByRole("button", { name: "Inoculate selected cases" })
      .click();
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
    await demo.caption(
      "Once saved, the culture moves to final read due. A bench sheet is printed for it.",
    );
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
    await demo.caption(
      "Extending incubation from the bench takes the culture off the list. Undo puts it back.",
    );
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
    await demo.caption(
      "Recording no growth from the bench can be undone the same way. The printed sheet does not change.",
    );
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
    await demo.caption(
      "On the case, a container label is prepared for the culture.",
    );
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
    const labelLink = culture.getByRole("link", {
      name: "Open printable labels",
    });
    await expect(labelLink).toBeVisible();
    const labelPrefix = await labelLink.evaluate(
      async (link: HTMLAnchorElement) => {
        const blob = await (await fetch(link.href)).blob();
        return blob.slice(0, 4).text();
      },
    );
    expect(labelPrefix).toBe("%PDF");
    await demo.caption(
      "Scanning that label on the worklist opens the case at its culture.",
    );
    await page.goto("/Microbiology/worklist");
    await page
      .getByRole("heading", { name: "Microbiology worklist" })
      .waitFor();
    await scanContainerLabel(page, `PLT-${seeded.caseId}-${id}`);
    await expect(page).toHaveURL(
      new RegExp(`/Microbiology/cases/${seeded.caseId}.*row=${id}`),
      { timeout: LONG_TIMEOUT },
    );
    await expect(
      page.locator(`[data-testid="culture-row-${id}"]`),
    ).toBeVisible();
    await demo.highlight(page.locator(`[data-testid="culture-row-${id}"]`));
    await test.info().attach("scanned-culture", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
    await demo.verified(
      "Bench actions are batched, reversible and scannable",
      "The culture was inoculated from the worklist, both bench actions were undone, and its label opens it.",
    );
  },
);

test("bench actions remain reachable on a narrow viewport", async ({
  page,
}) => {
  const seeded = await seedMicrobiologyCultureWorkspace(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(
    `/Microbiology/worklist?grain=bench&q=${encodeURIComponent(seeded.accessionNumber)}`,
  );
  await expect(
    page.getByRole("row").filter({ hasText: seeded.accessionNumber }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    page.getByRole("heading", { name: "Microbiology worklist" }),
  ).toBeVisible();
  const closeMenu = page.getByRole("button", { name: "Close menu" });
  if (await closeMenu.isVisible()) await closeMenu.click();
  for (const name of [
    "Record no growth",
    "Inoculate selected cases",
    "Extend incubation by 24 hours",
    "Print bench sheet",
  ]) {
    const button = page.getByRole("button", { name, exact: true });
    await button.scrollIntoViewIfNeeded();
    await expect(button).toBeInViewport();
  }
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth),
  ).toBeLessThanOrEqual(391);
});
