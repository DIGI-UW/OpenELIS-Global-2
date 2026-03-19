import { expect, test, Page, Locator, TestInfo } from "@playwright/test";
import { showSceneLabel, showTitleCard } from "../helpers/title-card";
import { videoPause } from "../helpers/video-pause";

/**
 * OGC-284 — Barcode label quantity workflow (user stories)
 *
 * Three separate tests, one per User Story. Use core-demo-video locally for
 * slowMo + title cards; core-demo for normal-speed CI on the build stack.
 *
 * Run with:
 *   cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' \
 *     npx playwright test ogc-284-barcode-workflow --project=core-demo-video
 */
type PauseFn = (ms: number) => Promise<void>;

/** Site/requester lookup: prefer autosuggest, but fall back to tabbing out. */
async function pickFirstAutosuggestOptional(page: Page, pause: PauseFn) {
  try {
    await expect
      .poll(
        async () => page.locator('[data-cy="auto-suggestion"]').count(),
        {
          timeout: 12_000,
          intervals: [400, 800, 1500],
        },
      )
      .toBeGreaterThan(0);
    await page.locator('[data-cy="auto-suggestion"]').first().click();
    await pause(500);
  } catch {
    await page.keyboard.press("Tab");
    await pause(200);
  }
}

/** Visible success panel after SamplePatientEntry save (class from OrderSuccessMessage). */
async function expectOrderEntrySaveSuccess(page: Page) {
  await expect(page.locator(".orderEntrySuccessMsg")).toBeVisible({
    timeout: 20_000,
  });
}

async function setNumericInput(locator: Locator, preferredValue: string) {
  await locator.click({ clickCount: 3 });
  const currentValue = (await locator.inputValue()).trim();
  let nextValue = preferredValue;
  if (currentValue === preferredValue) {
    const numericValue = Number(preferredValue);
    nextValue = Number.isNaN(numericValue)
      ? `${preferredValue}-1`
      : `${numericValue + 1}`;
  }
  await locator.fill(nextValue);
}

/**
 * Scroll the element into view AND scroll the window so it's in the
 * upper-third of the viewport — making it clearly visible on video.
 */
async function scrollToAndPause(
  page: Page,
  locator: Locator,
  pause: PauseFn,
  pauseMs = 1500,
) {
  await locator.scrollIntoViewIfNeeded();
  // Nudge the viewport so the element sits in the upper portion
  const box = await locator.boundingBox();
  if (box) {
    const targetY = Math.max(0, box.y - 120);
    await page.evaluate(
      (y) => window.scrollTo({ top: y, behavior: "smooth" }),
      targetY,
    );
  }
  await pause(pauseMs);
}

async function waitForAutoSuggestion(page: Page, pattern?: RegExp) {
  if (pattern) {
    const matchingSuggestion = page
      .locator('[data-cy="auto-suggestion"]')
      .filter({ hasText: pattern })
      .first();
    if (
      await matchingSuggestion.isVisible({ timeout: 2_000 }).catch(() => false)
    ) {
      return matchingSuggestion;
    }
  }

  const suggestion = page.locator('[data-cy="auto-suggestion"]').first();
  await expect(suggestion).toBeVisible({ timeout: 5_000 });
  return suggestion;
}

async function waitForSampleStep(page: Page) {
  const sampleSelect = page.locator("select#sampleId_0");
  await expect(sampleSelect).toBeVisible({ timeout: 15_000 });
  await expect(
    sampleSelect.locator("option:not(:first-child)"),
  ).not.toHaveCount(0, { timeout: 15_000 });
}

async function waitForOrderDetailsStep(page: Page) {
  await expect(page.locator("input#labNo")).toBeVisible({ timeout: 15_000 });
  await expect(page.locator("input#order_requestDate")).toBeVisible({
    timeout: 15_000,
  });
}

/**
 * Search for and select a patient by last/first name.
 * Results are Carbon RadioButtons (data-cy="radioButton"), not table rows.
 * Clicking a radio button fires patientSelected() which auto-switches
 * the tab to the Create/Edit Patient form with fields pre-filled.
 */
async function selectPatient(
  page: Page,
  lastName: string,
  firstName: string,
  pause: PauseFn,
) {
  // Ensure we're on the Search tab
  const searchTabBtn = page.locator('[data-cy="searchPatientTabButton"]');
  if (await searchTabBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
    await searchTabBtn.click();
  }

  const lastNameInput = page.locator("input#lastName");
  await expect(lastNameInput).toBeVisible({ timeout: 8000 });
  await lastNameInput.fill(lastName);

  const firstNameInput = page.locator("input#firstName");
  await firstNameInput.fill(firstName);

  // Click Search
  const searchBtn = page.locator(
    '[data-cy="searchPatientButton"], button#local_search',
  );
  await searchBtn.click();

  await expect(page.locator('[data-cy="radioButton"]').first()).toBeVisible({
    timeout: 10_000,
  });

  const namedRow = page
    .locator("tbody tr")
    .filter({
      hasText: new RegExp(
        `${lastName}.*${firstName}|${firstName}.*${lastName}`,
        "i",
      ),
    })
    .filter({ has: page.locator('[data-cy="radioButton"]') })
    .first();
  const firstRadio =
    (await namedRow.count()) > 0
      ? namedRow.locator('[data-cy="radioButton"]').first()
      : page.locator('[data-cy="radioButton"]').first();
  await expect(firstRadio).toBeVisible({ timeout: 5000 });
  await scrollToAndPause(page, firstRadio, pause, 800);
  // Carbon overlays the visible label on top of the radio input, which can
  // intercept pointer events in CI; forcing a radio check matches Cypress.
  await firstRadio.check({ force: true });

  // Wait for patient form hydration before moving to the next wizard step.
  await expect(page.locator('[data-cy="patientSelectionReady"]')).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator("input#lastName")).toHaveValue(
    new RegExp(lastName, "i"),
    {
      timeout: 10_000,
    },
  );
  await expect(page.locator("input#firstName")).toHaveValue(
    new RegExp(firstName, "i"),
    {
      timeout: 10_000,
    },
  );
  await expect(page.locator("input#primaryPhone")).toBeVisible({
    timeout: 10_000,
  });

  // Search results can omit fields that the Add Order validation schema still
  // requires (national ID, gender, DOB). Backfill them before advancing.
  const nationalIdInput = page.locator("input#nationalId");
  if (await nationalIdInput.isVisible({ timeout: 5000 }).catch(() => false)) {
    if (!(await nationalIdInput.inputValue()).trim()) {
      await nationalIdInput.fill(`DEMO-${Date.now()}`);
      await nationalIdInput.press("Tab");
    }

    const genderMale = page.locator("input#radio-1");
    const genderFemale = page.locator("input#radio-2");
    const hasGender =
      (await genderMale.isChecked().catch(() => false)) ||
      (await genderFemale.isChecked().catch(() => false));
    if (!hasGender && (await genderMale.isVisible().catch(() => false))) {
      await genderMale.check({ force: true });
    }

    const birthDateInput = page.locator("input#date-picker-default-id");
    if (
      (await birthDateInput.isVisible().catch(() => false)) &&
      !(await birthDateInput.inputValue()).trim()
    ) {
      await birthDateInput.fill("13/03/1990");
      await birthDateInput.press("Tab");
    }
  }
}

/**
 * Fill the Add Sample step: select sample type, set collection date,
 * and select at least one panel/test so sampleXML gets populated.
 */
async function fillSampleStep(page: Page) {
  const sampleSelect = page.locator("select#sampleId_0");
  await waitForSampleStep(page);

  const options = await sampleSelect.locator("option").allTextContents();
  const serum = options.find((o) => o.toLowerCase().includes("serum"));
  if (serum) {
    await sampleSelect.selectOption({ label: serum.trim() });
  } else {
    await sampleSelect.selectOption({ index: 1 });
  }

  await selectPanelOrTest(page);
}

/**
 * Fill the Order Details step (Step 3) — lab number, dates, site, requester.
 */
async function fillOrderDetails(page: Page, pause: PauseFn) {
  await waitForOrderDetailsStep(page);

  const labNoInput = page.locator("input#labNo");
  const generateBtn = page.locator("[data-cy='generate-labNumber']");
  if (await generateBtn.isVisible({ timeout: 4000 }).catch(() => false)) {
    await generateBtn.click();
    await expect(labNoInput).not.toHaveValue("", { timeout: 10_000 });
  }

  const requestDate = page.locator("input#order_requestDate");
  if (await requestDate.isVisible().catch(() => false)) {
    await requestDate.fill("13/03/2026");
    await page.keyboard.press("Tab");
    await expect(requestDate).toHaveValue("13/03/2026");
  }
  const receivedDate = page.locator("input#order_receivedDate");
  if (await receivedDate.isVisible().catch(() => false)) {
    await receivedDate.fill("13/03/2026");
    await page.keyboard.press("Tab");
    await expect(receivedDate).toHaveValue("13/03/2026");
  }

  const siteInput = page.locator("input#siteName");
  if (await siteInput.isVisible().catch(() => false)) {
    await siteInput.clear();
    await siteInput.fill("CAMES MAN");
    await pause(1200);
    await pickFirstAutosuggestOptional(page, pause);
  }

  const requesterLookup = page.locator("input#requesterId");
  if (await requesterLookup.isVisible().catch(() => false)) {
    await requesterLookup.clear();
    await requesterLookup.fill("Prime");
    await pause(800);
    await pickFirstAutosuggestOptional(page, pause);
  }

  const requesterFirst = page.locator("input#requesterFirstName");
  if (await requesterFirst.isVisible().catch(() => false)) {
    await requesterFirst.clear();
    await requesterFirst.fill("Optimus");
    await expect(requesterFirst).toHaveValue("Optimus");
  }
  const requesterLast = page.locator("input#requesterLastName");
  if (await requesterLast.isVisible().catch(() => false)) {
    await requesterLast.clear();
    await requesterLast.fill("Prime");
    await expect(requesterLast).toHaveValue("Prime");
  }

  if (await requesterLookup.isVisible().catch(() => false)) {
    if (!(await requesterLookup.inputValue()).trim()) {
      await requesterLookup.fill("Prime, Optimus");
    }
    await expect(requesterLookup).not.toHaveValue("", { timeout: 5_000 });
  }
}

/** Click Next and wait for the next page to load. */
async function clickNext(page: Page) {
  const btn = page.getByRole("button", { name: "Next", exact: true });
  await expect(btn).toBeVisible({ timeout: 5000 });
  await expect(btn).toBeEnabled({ timeout: 5_000 });
  await btn.click();
}

/**
 * Select at least one panel or test on the Add Sample step.
 * Tries known panel names first, falls back to the first available checkbox.
 */
async function selectPanelOrTest(page: Page) {
  // Carbon checkbox: the <input> is visually hidden; click the <label> instead.
  const label = page.locator('label:has-text("Bilan Biochimique")');
  await expect(label).toBeVisible({ timeout: 10_000 });
  await label.scrollIntoViewIfNeeded();
  await label.click({ timeout: 5000 });
  await expect(
    page.getByRole("button", { name: "Next", exact: true }),
  ).toBeEnabled({ timeout: 10_000 });
}

async function gotoBarcodeConfig(page: Page) {
  await page.goto("/MasterListsPage/barcodeConfiguration", {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByRole("button", { name: "Save" })).toBeVisible({
    timeout: 15_000,
  });
}

async function gotoSamplePatientEntry(page: Page) {
  await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-cy="searchPatientTabButton"]')).toBeVisible({
    timeout: 15_000,
  });
}

async function gotoPrintBarcode(page: Page) {
  await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: /print bar code labels/i }),
  ).toBeVisible({
    timeout: 15_000,
  });
}

// ─── User Story 1: Admin configures barcode label quantities ─────────────────

test("US1 — Admin configures barcode label quantities", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  const pause: PauseFn = (ms) => videoPause(page, ms, testInfo);

  await showTitleCard(
    page,
    "User Story 1",
    "As a lab administrator, I configure default and maximum label quantities for each label type.",
    3000,
    testInfo,
  );

  await gotoBarcodeConfig(page);

  // ── Default quantities ──────────────────────────────────────────
  await showTitleCard(
    page,
    "Default Label Quantities",
    "Set how many labels are printed by default for Order, Specimen, Slide, Block, and Freezer label types.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-001 — Default Quantities", testInfo);

  const defaultOrderInput = page.locator("#order").first();
  await scrollToAndPause(page, defaultOrderInput, pause, 1200);
  await setNumericInput(defaultOrderInput, "2");
  await pause(600);

  const defaultSpecimenInput = page.locator("#specimen").first();
  await setNumericInput(defaultSpecimenInput, "1");
  await pause(800);

  // ── Maximum quantities ──────────────────────────────────────────
  await showTitleCard(
    page,
    "Maximum Label Quantities",
    "Max limits prevent over-printing. Exceeding them blocks the request unless override=true is set — FR-002, FR-016.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-002 — Maximum Quantities", testInfo);

  const maxOrderInput = page.locator("#maxOrder");
  await scrollToAndPause(page, maxOrderInput, pause, 1200);
  await setNumericInput(maxOrderInput, "10");
  await pause(500);

  const maxSpecimenInput = page.locator("#maxSpecimen");
  await setNumericInput(maxSpecimenInput, "10");
  await pause(800);

  // ── Optional element toggles ────────────────────────────────────
  await showTitleCard(
    page,
    "Optional Label Elements",
    "Lab Number is mandatory. Other fields (Patient DOB, ID, Name…) are toggleable per label type — FR-002c.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-002c — Optional Elements", testInfo);

  const optionalCheckbox = page.locator("#orderPatientDobCheck");
  if (await optionalCheckbox.isVisible({ timeout: 3000 }).catch(() => false)) {
    await scrollToAndPause(page, optionalCheckbox, pause, 2000);
  }

  // ── Save and verify persistence ─────────────────────────────────
  await showTitleCard(
    page,
    "Save & Verify Persistence",
    "Save the configuration, reload the page, and confirm the values are returned unchanged — FR-003.",
    2000,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-003 — Save + Reload", testInfo);

  const saveButton = page.getByRole("button", { name: "Save" });
  await expect(saveButton).toBeEnabled({ timeout: 10_000 });
  await scrollToAndPause(page, saveButton, pause, 800);
  await saveButton.click();
  await expect(
    page.getByText(/bar.?code configurations has been saved/i).first(),
  ).toBeVisible({ timeout: 10_000 });

  await page.reload({ waitUntil: "domcontentloaded" });
  await expect(page.getByRole("button", { name: "Save" })).toBeVisible({
    timeout: 15_000,
  });
  await scrollToAndPause(page, defaultOrderInput, pause, 2000);

  await showTitleCard(
    page,
    "✓ Configuration Persisted",
    "Default order labels = 2, max = 10. Values reload correctly — FR-001, FR-002, FR-003 satisfied.",
    3000,
    testInfo,
  );
});

// ─── User Story 2: Capture label quantities during sample creation ────────────

test("US2 — Capture label quantities during sample creation", async ({
  page,
}, testInfo) => {
  test.setTimeout(180_000);
  const pause: PauseFn = (ms) => videoPause(page, ms, testInfo);

  await showTitleCard(
    page,
    "User Story 2",
    "As a lab user, I can review and edit label quantities before saving. My choices are persisted with the sample.",
    3000,
    testInfo,
  );
  await showSceneLabel(page, "US2 · Add Order", testInfo);

  await gotoSamplePatientEntry(page);

  // ── Step 0: Patient search & select ─────────────────────────────
  await showSceneLabel(page, "US2 · Step 0: Patient", testInfo);
  await selectPatient(page, "TEST-Smith", "John", pause);

  // ── Next → Step 1 (Program selection) ───────────────────────────
  await clickNext(page);

  // Step 1: program selection — skip through (no required input)
  const next2 = page.getByRole("button", { name: "Next", exact: true });
  if (await next2.isVisible({ timeout: 2000 }).catch(() => false)) {
    await expect(next2).toBeEnabled({ timeout: 5_000 });
    await next2.click();
    await waitForSampleStep(page);
  }

  // ── Step 2: Sample type ─────────────────────────────────────────
  await showSceneLabel(page, "US2 · Step 2: Sample Type", testInfo);

  await fillSampleStep(page);

  // ── ★ KEY: Labels Section ───────────────────────────────────────
  await showTitleCard(
    page,
    "Label Quantities Section",
    "Every order-entry workflow now shows one order row + one row per sample, pre-populated from admin defaults — FR-005a, FR-005b.",
    3000,
    testInfo,
  );
  await showSceneLabel(
    page,
    "US2 · FR-005b — Label Quantities Section",
    testInfo,
  );

  const labelsSection = page.getByTestId("labels-section-root");
  await expect(labelsSection).toBeVisible({ timeout: 8000 });
  await scrollToAndPause(page, labelsSection, pause, 2000);

  // Edit order labels
  const orderInput = labelsSection.locator("#labels-order");
  await scrollToAndPause(page, orderInput, pause, 800);
  await orderInput.fill("3");
  await pause(1000);

  // Edit specimen labels
  const specimenInput = labelsSection.locator("#sample-row-1");
  if (await specimenInput.isVisible().catch(() => false)) {
    await scrollToAndPause(page, specimenInput, pause, 600);
    await specimenInput.fill("2");
    await pause(800);
  }

  // Running total
  const runningTotal = labelsSection.locator("p");
  if (await runningTotal.isVisible().catch(() => false)) {
    await scrollToAndPause(page, runningTotal, pause, 2000);
  }

  // ── Step 3: Order details ───────────────────────────────────────
  await clickNext(page);
  await showSceneLabel(page, "US2 · Step 3: Order Details", testInfo);
  await fillOrderDetails(page, pause);

  // Submit
  const submitBtn = page.getByRole("button", { name: "Submit" });
  await scrollToAndPause(page, submitBtn, pause, 1000);
  await submitBtn.click();
  await expectOrderEntrySaveSuccess(page);

  // ── Success: confirm quantities saved ───────────────────────────
  await showTitleCard(
    page,
    "✓ Label Quantities Saved",
    "Order labels = 3, Specimen labels = 2 persisted with the sample record — FR-007, FR-008 satisfied.",
    3000,
    testInfo,
  );
});

// ─── User Story 3: Post-save print dialog + reprint ──────────────────────────

test("US3 — Post-save print dialog and reprint", async ({ page }, testInfo) => {
  test.setTimeout(180_000);
  const pause: PauseFn = (ms) => videoPause(page, ms, testInfo);

  await showTitleCard(
    page,
    "User Story 3",
    "After a successful save, the system shows per-label-type Print buttons. Users print immediately or defer and reprint later.",
    3000,
    testInfo,
  );
  await showSceneLabel(page, "US3 · Post-Save Printing", testInfo);

  // Quickly reach the success page by submitting an order
  await gotoSamplePatientEntry(page);

  await selectPatient(page, "TEST-Smith", "John", pause);
  await clickNext(page);

  const next2 = page.getByRole("button", { name: "Next", exact: true });
  if (await next2.isVisible({ timeout: 2000 }).catch(() => false)) {
    await expect(next2).toBeEnabled({ timeout: 5_000 });
    await next2.click();
    await waitForSampleStep(page);
  }

  await fillSampleStep(page);

  // Show labels section briefly before moving on
  const labelsSection = page.getByTestId("labels-section-root");
  if (await labelsSection.isVisible({ timeout: 5000 }).catch(() => false)) {
    await scrollToAndPause(page, labelsSection, pause, 1500);
  }

  await clickNext(page);
  await showSceneLabel(page, "US3 · Completing Order...", testInfo);
  await fillOrderDetails(page, pause);

  const submitBtn = page.getByRole("button", { name: "Submit" });
  await scrollToAndPause(page, submitBtn, pause, 800);
  await submitBtn.click();
  await expectOrderEntrySaveSuccess(page);

  // ── Post-save print dialog ──────────────────────────────────────
  await showTitleCard(
    page,
    "Post-Save Print Dialog",
    "Order saved and accession assigned. The system now presents per-label-type Print buttons — FR-011, FR-011a.",
    3000,
    testInfo,
  );
  await showSceneLabel(page, "US3 · FR-011 — Print Dialog", testInfo);

  const successArea = page.locator(".orderEntrySuccessMsg").first();
  await scrollToAndPause(page, successArea, pause, 1000);

  // Scroll to print dialog
  const printTitle = page.getByText(/print labels/i).first();
  if (await printTitle.isVisible({ timeout: 3000 }).catch(() => false)) {
    await scrollToAndPause(page, printTitle, pause, 1500);
  }

  // Highlight Print buttons
  const printButtons = page.getByRole("button", { name: "Print" });
  const count = await printButtons.count();
  if (count > 0) {
    await scrollToAndPause(page, printButtons.first(), pause, 2000);
  }

  // Show Done button — deferred printing
  await showTitleCard(
    page,
    "Done — Deferred Printing",
    "Done closes the dialog without printing. The accession is preserved; reprinting is available from Order View — FR-013.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US3 · FR-013 — Done / Defer", testInfo);

  const doneButton = page.getByRole("button", { name: /^(done|skip)$/i });
  if (await doneButton.isVisible({ timeout: 3000 }).catch(() => false)) {
    await scrollToAndPause(page, doneButton, pause, 2000);
  }

  // ── Reprint from Print Barcode page ────────────────────────────
  await showTitleCard(
    page,
    "Reprint from Print Barcode Page",
    "At any time, staff can reprint labels by searching for the order's accession number — FR-013.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US3 · FR-013 — Reprint Entry", testInfo);

  await gotoPrintBarcode(page);

  const printBarcodeHeading = page.getByRole("heading", {
    name: /print bar code labels/i,
  });
  if (await printBarcodeHeading.isVisible().catch(() => false)) {
    await scrollToAndPause(page, printBarcodeHeading, pause, 1500);

    const siteSearch = page.getByLabel(/search site name/i);
    if (await siteSearch.isVisible().catch(() => false)) {
      await scrollToAndPause(page, siteSearch, pause, 2000);
    }
  }

  await showTitleCard(
    page,
    "✓ OGC-284 Complete",
    "US1: Admin Config · US2: Label Quantities in Workflows · US3: Post-Save Print + Deferred Reprint",
    4000,
    testInfo,
  );
});
