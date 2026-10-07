import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page, TestInfo } from "@playwright/test";
import {
  MICROBIOLOGY_CULTURE_TEST_NAME as cultureTestName,
  MICROBIOLOGY_NON_CULTURE_TEST_NAME as nonCultureTestName,
  seedMicrobiologyOrderCatalog as seedOrderCatalog,
  selectMicrobiologyOrderTest as selectTest,
  startMicrobiologyOrder as startSupportedOrder,
} from "../../../helpers/microbiology-order-entry";
import { videoPause } from "../../../helpers/video-pause";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

async function saveEntryAndOpenCollect(page: Page) {
  const saveAndNext = page.getByRole("button", { name: "Save and next" });
  await expect(saveAndNext).toBeEnabled({ timeout: LONG_TIMEOUT });
  await saveAndNext.click();
  await expect(page).toHaveURL(/\/order\/clinical\/collect$/i, {
    timeout: LONG_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Prepare Samples", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByTestId("sample-collection-card-0").getByLabel("Sample Type"),
  ).not.toHaveValue("", { timeout: LONG_TIMEOUT });
}

async function fillConfiguredDate(
  dateInput: Locator,
  isoDate: string,
): Promise<string> {
  const [year, month, day] = isoDate.split("-");
  const placeholder = (await dateInput.getAttribute("placeholder")) || "";
  const formatted = placeholder.toLowerCase().startsWith("dd")
    ? `${day}/${month}/${year}`
    : `${month}/${day}/${year}`;
  await dateInput.click();
  await dateInput.selectText();
  await dateInput.pressSequentially(formatted);
  await dateInput.press("Enter");
  await expect(dateInput).toHaveValue(formatted);
  return formatted;
}

async function collectAndRoute(page: Page) {
  const collectionCard = page.getByTestId("sample-collection-card-0");
  let displayedCollectionDate = "";
  for (const label of ["Collection Date", "Received Date"]) {
    const dateInput = collectionCard.getByLabel(label, { exact: false });
    const displayedDate = await fillConfiguredDate(dateInput, "2026-08-13");
    if (label === "Collection Date") {
      displayedCollectionDate = displayedDate;
    }
  }
  // Prepare Samples is complete once every sample names who collected it;
  // the laboratory took this one itself.
  await collectionCard.locator('label[for="labPerformedSampling-0"]').click();
  const saveAndNext = page.getByRole("button", { name: "Save and next" });
  await expect(saveAndNext).toBeEnabled({ timeout: LONG_TIMEOUT });
  await saveAndNext.click();
  // Sample check when the laboratory uses it, else order entry is finished
  // and the dashboard says so.
  await expect(page).toHaveURL(/\/order\/clinical(\/qa|\?done=)/i, {
    timeout: LONG_TIMEOUT,
  });
  return displayedCollectionDate;
}

async function expectOrderStepUrl(
  page: Page,
  step: "enter" | "collect",
  labNumber: string,
) {
  await expect(page).toHaveURL(
    (url) =>
      url.pathname === `/order/clinical/${step}` &&
      url.searchParams.get("order") === labNumber,
  );
}

async function reloadThroughBarcode(page: Page, labNumber: string) {
  await page.reload({ waitUntil: "domcontentloaded" });
  const barcode = page.getByRole("searchbox", { name: "Scan barcode" });
  await expect(barcode).toBeVisible({ timeout: LONG_TIMEOUT });
  await barcode.fill(labNumber);
  await barcode.press("Enter");
  await expect(page.getByTestId("order-context-card")).toContainText(
    labNumber,
    { timeout: LONG_TIMEOUT },
  );
  await page.getByTestId("order-step-enter").click();
  await expectOrderStepUrl(page, "enter", labNumber);
}

async function attachResponsiveEvidence(
  page: Page,
  testInfo: TestInfo,
  subject: Locator,
  evidenceName: string,
) {
  const originalViewport = page.viewportSize();
  const viewports = [
    ["desktop", { width: 1440, height: 1000 }],
    ["mobile", { width: 393, height: 851 }],
  ] as const;

  for (const [name, viewport] of viewports) {
    await page.setViewportSize(viewport);
    await subject.scrollIntoViewIfNeeded();
    await expect(subject).toBeVisible({ timeout: LONG_TIMEOUT });
    const subjectWidth = await subject.evaluate((root) => {
      const rootBounds = root.getBoundingClientRect();
      return {
        clientWidth: root.clientWidth,
        scrollWidth: root.scrollWidth,
        overflowingElements: Array.from(root.querySelectorAll("*"))
          .map((element) => {
            const bounds = element.getBoundingClientRect();
            return {
              className: element.className?.toString().slice(0, 120),
              id: element.id,
              right: Math.round(bounds.right),
              tagName: element.tagName,
              width: Math.round(bounds.width),
            };
          })
          .filter(
            ({ right, width }) => right > rootBounds.right + 1 && width > 0,
          )
          .slice(0, 12),
      };
    });
    if (subjectWidth.scrollWidth > subjectWidth.clientWidth) {
      console.info(
        `[responsive-overflow] ${JSON.stringify(subjectWidth.overflowingElements)}`,
      );
    }
    expect(subjectWidth.scrollWidth).toBeLessThanOrEqual(
      subjectWidth.clientWidth,
    );
    const path = testInfo.outputPath(`${evidenceName}-${name}.png`);
    await videoPause(page, 1500, testInfo);
    await subject.screenshot({
      path,
      animations: "disabled",
      style: "#mainHeader { visibility: hidden !important; }",
    });
    await testInfo.attach(`${evidenceName}-${name}`, {
      path,
      contentType: "image/png",
    });
  }

  if (originalViewport) {
    await page.setViewportSize(originalViewport);
    await subject.scrollIntoViewIfNeeded();
  }
}

// V02c1 / FR-02.2: reception treats microbiology like ordinary tests.
// Canonical direct-test grouping and catalog controls are accepted in V02c2.
test.describe("microbiology reception without Program overrides", () => {
  test("saves and reloads a culture order without forcing a Program or reception details", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    const labNumber = await startSupportedOrder(page, seeded);
    const program = page.getByRole("combobox", { name: "Program" });
    await expect(program).toBeEnabled();
    await expect(program).toHaveValue("");
    await selectTest(page, cultureTestName);
    await expect(program).toBeEnabled();
    await expect(program).toHaveValue("");
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toHaveCount(0);
    await expect(
      page.getByLabel("Clinical History", { exact: true }),
    ).toHaveCount(0);
    await expect(
      page.getByRole("combobox", { name: "Culture Protocol" }),
    ).toHaveCount(0);
    await attachResponsiveEvidence(
      page,
      testInfo,
      page.locator(".program-section"),
      "program-remains-independent",
    );
    await saveEntryAndOpenCollect(page);
    await collectAndRoute(page);
    await reloadThroughBarcode(page, labNumber);
    await expect(program).toHaveValue("");
    await expect(program).toBeDisabled();
    await expect(page.getByLabel(cultureTestName)).toBeChecked();
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toHaveCount(0);
    await attachResponsiveEvidence(
      page,
      testInfo,
      page.locator(".program-section"),
      "saved-program-reloaded",
    );
    await page.goto(
      `/Microbiology/worklist?q=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    const rows = page.getByRole("row").filter({
      has: page.getByRole("link", { name: labNumber, exact: true }),
    });
    await expect(rows).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await rows.scrollIntoViewIfNeeded();
    await videoPause(page, 2000, testInfo);
  });

  test("a manually selected Microbiology Program does not turn an ordinary test into a case", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    const labNumber = await startSupportedOrder(page, seeded);
    await selectTest(page, nonCultureTestName);
    const program = page.getByRole("combobox", { name: "Program" });
    await program.fill("Microbiology");
    await expect(
      page.getByRole("option", { name: "Microbiology", exact: true }),
    ).toBeVisible();
    await program.press("ArrowDown");
    await program.press("Enter");
    await expect(program).toHaveValue("Microbiology");
    await program.scrollIntoViewIfNeeded();
    await videoPause(page, 1500, testInfo);
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toHaveCount(0);
    await saveEntryAndOpenCollect(page);
    await collectAndRoute(page);
    await reloadThroughBarcode(page, labNumber);
    await expect(program).toHaveValue("Microbiology");
    await program.scrollIntoViewIfNeeded();
    await videoPause(page, 1500, testInfo);
    await expect(page.getByLabel(nonCultureTestName)).toBeChecked();
    await page.goto(
      `/Microbiology/worklist?q=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    const emptyWorklist = page.getByText(/No cultures match/);
    await expect(emptyWorklist).toBeVisible({ timeout: LONG_TIMEOUT });
    await emptyWorklist.scrollIntoViewIfNeeded();
    await videoPause(page, 2000, testInfo);
  });
});
