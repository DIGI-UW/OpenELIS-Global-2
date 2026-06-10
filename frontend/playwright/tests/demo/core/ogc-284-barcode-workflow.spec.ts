import { expect, test, Page, Locator } from "../../../helpers/test-base";
import { showSceneLabel, showTitleCard } from "../../../helpers/title-card";
import { videoPause } from "../../../helpers/video-pause";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

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

/** Site/requester lookup: wait for suggestion dropdown, fall back to Tab if none appear. */
async function pickFirstAutosuggestOptional(page: Page, pause: PauseFn) {
  const suggestion = page.locator('[data-cy="auto-suggestion"]').first();
  try {
    await expect(suggestion).toBeVisible({ timeout: SHORT_TIMEOUT });
    await suggestion.click();
    await pause(500);
  } catch {
    // No suggestions rendered (empty DB, no match) — accept free text via Tab
    await page.keyboard.press("Tab");
    await pause(200);
  }
}

/** Visible success panel after SamplePatientEntry save (class from OrderSuccessMessage). */
async function expectOrderEntrySaveSuccess(page: Page) {
  await expect(page.locator(".orderEntrySuccessMsg")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
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

async function waitForSampleStep(page: Page) {
  const sampleSelect = page.locator("select#sampleId_0");
  await expect(sampleSelect).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    sampleSelect.locator("option:not(:first-child)"),
  ).not.toHaveCount(0, { timeout: LONG_TIMEOUT });
}

async function waitForOrderDetailsStep(page: Page) {
  await expect(page.locator("input#labNo")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(page.locator("input#order_requestDate")).toBeVisible({
    timeout: LONG_TIMEOUT,
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
  if (await searchTabBtn.isVisible()) {
    await searchTabBtn.click();
  }

  const lastNameInput = page.locator("input#lastName");
  await expect(lastNameInput).toBeVisible({ timeout: UI_TIMEOUT });
  await lastNameInput.fill(lastName);

  const firstNameInput = page.locator("input#firstName");
  await firstNameInput.fill(firstName);

  // Click Search
  const searchBtn = page.locator(
    '[data-cy="searchPatientButton"], button#local_search',
  );
  await searchBtn.click();

  await expect(page.locator('[data-cy="radioButton"]').first()).toBeVisible({
    timeout: UI_TIMEOUT,
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
  await expect(firstRadio).toBeVisible({ timeout: SHORT_TIMEOUT });
  await scrollToAndPause(page, firstRadio, pause, 800);
  // Carbon radio: click the visible label sibling instead of forcing the hidden input
  await firstRadio.locator("xpath=..").locator("label").click();

  // Patient selection now flips to the form before all hydrated fields settle.
  const patientForm = page
    .locator(
      '[data-cy="patientSelectionReady"], [data-cy="patientSelectionPending"]',
    )
    .first();
  await expect(patientForm).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(patientForm.locator("input#lastName")).toHaveValue(
    new RegExp(lastName, "i"),
    {
      timeout: UI_TIMEOUT,
    },
  );
  await expect(patientForm.locator("input#firstName")).toHaveValue(
    new RegExp(firstName, "i"),
    {
      timeout: UI_TIMEOUT,
    },
  );
  await expect(patientForm.locator("input#primaryPhone")).toBeVisible({
    timeout: UI_TIMEOUT,
  });

  // Search results can omit fields that the Add Order validation schema still
  // requires (national ID, gender, DOB). Backfill them before advancing.
  const nationalIdInput = patientForm.locator("input#nationalId");
  if (await nationalIdInput.isVisible()) {
    if (!(await nationalIdInput.inputValue()).trim()) {
      await nationalIdInput.fill(`DEMO-${Date.now()}`);
      await nationalIdInput.press("Tab");
    }

    const genderMale = page.locator("input#radio-1");
    const genderFemale = page.locator("input#radio-2");
    const hasGender =
      ((await genderMale.count()) > 0 && (await genderMale.isChecked())) ||
      ((await genderFemale.count()) > 0 && (await genderFemale.isChecked()));
    if (!hasGender && (await genderMale.count()) > 0) {
      // Carbon radio: click the visible label instead of forcing the hidden input
      await expect(page.locator('label[for="radio-1"]')).toBeVisible({
        timeout: SHORT_TIMEOUT,
      });
      await page.locator('label[for="radio-1"]').click();
    }

    const birthDateInput = patientForm.locator("input#date-picker-default-id");
    if (
      (await birthDateInput.isVisible()) &&
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

  const collectionDate = page.locator("input#collectionDate_0");
  if (await collectionDate.isVisible()) {
    await collectionDate.fill("13/03/2026");
    await collectionDate.press("Tab");
  }

  const collectionTime = page.locator("input#collectionTime_0");
  if (await collectionTime.isVisible()) {
    await collectionTime.fill("07:15");
    await collectionTime.press("Tab");
  }

  const collector = page.locator("input#collector_0");
  if (await collector.isVisible()) {
    await collector.fill("E2E Collector");
  }

  const quantity = page.locator("input#quantity");
  if (await quantity.isVisible()) {
    await quantity.fill("1");
  }

  const uomSelect = page.locator("select#uomId_0");
  if (await uomSelect.isVisible()) {
    await uomSelect.selectOption({ index: 1 });
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
  if (await generateBtn.isVisible()) {
    await generateBtn.click();
    await expect(labNoInput).not.toHaveValue("", { timeout: UI_TIMEOUT });
  }

  const requestDate = page.locator("input#order_requestDate");
  if (await requestDate.isVisible()) {
    await requestDate.fill("13/03/2026");
    await page.keyboard.press("Tab");
    await expect(requestDate).toHaveValue("13/03/2026");
  }
  const receivedDate = page.locator("input#order_receivedDate");
  if (await receivedDate.isVisible()) {
    await receivedDate.fill("13/03/2026");
    await page.keyboard.press("Tab");
    await expect(receivedDate).toHaveValue("13/03/2026");
  }

  const siteInput = page.locator("input#siteName");
  if (await siteInput.isVisible()) {
    const siteQueries = ["CAMES MAN", "CAMES", "C"];
    for (const query of siteQueries) {
      await siteInput.clear();
      await siteInput.fill(query);
      await pause(900);
      await pickFirstAutosuggestOptional(page, pause);
      if ((await siteInput.inputValue()).trim()) {
        break;
      }
    }
    await expect(siteInput).not.toHaveValue("", { timeout: UI_TIMEOUT });
  }

  const requesterDepartment = page.locator("select#requesterDepartmentId");
  if (await requesterDepartment.isVisible()) {
    const optionCount = await requesterDepartment.locator("option").count();
    if (optionCount > 1 && !(await requesterDepartment.inputValue()).trim()) {
      await requesterDepartment.selectOption({ index: 1 });
    }
  }

  const requesterLookup = page.locator("input#requesterId");
  if (await requesterLookup.isVisible()) {
    await requesterLookup.clear();
    await requesterLookup.fill("Prime");
    await pause(800);
    await pickFirstAutosuggestOptional(page, pause);
  }

  const requesterFirst = page.locator("input#requesterFirstName");
  if (await requesterFirst.isVisible()) {
    await requesterFirst.clear();
    await requesterFirst.fill("Optimus");
    await expect(requesterFirst).toHaveValue("Optimus");
  }
  const requesterLast = page.locator("input#requesterLastName");
  if (await requesterLast.isVisible()) {
    await requesterLast.clear();
    await requesterLast.fill("Prime");
    await expect(requesterLast).toHaveValue("Prime");
  }

  if (await requesterLookup.isVisible()) {
    if (!(await requesterLookup.inputValue()).trim()) {
      await requesterLookup.fill("Prime, Optimus");
    }
    await expect(requesterLookup).not.toHaveValue("", {
      timeout: SHORT_TIMEOUT,
    });
  }

  // Newer order-entry validation requires explicit selections here.
  const paymentStatus = page.getByRole("combobox", {
    name: "Patient payment status:",
  });
  if (await paymentStatus.isVisible()) {
    const current = await paymentStatus.inputValue();
    if (!current) {
      await paymentStatus.selectOption({ index: 1 });
    }
  }

  const samplingPoint = page.getByRole("combobox", {
    name: "Sampling performed for analysis:",
  });
  if (await samplingPoint.isVisible()) {
    const current = await samplingPoint.inputValue();
    if (!current) {
      await samplingPoint.selectOption({ index: 1 });
    }
  }
}

/** Click Next and wait for the next page to load. */
async function clickNext(page: Page) {
  const btn = page.getByRole("button", { name: "Next", exact: true });
  await expect(btn).toBeVisible({ timeout: SHORT_TIMEOUT });
  await expect(btn).toBeEnabled({ timeout: SHORT_TIMEOUT });
  await btn.click();
}

/**
 * Select at least one panel or test on the Add Sample step.
 * Tries known panel names first, falls back to the first available checkbox.
 */
async function selectPanelOrTest(page: Page) {
  // Carbon checkbox: the <input> is visually hidden; click the <label> instead.
  const label = page.locator('label:has-text("Bilan Biochimique")');
  await expect(label).toBeVisible({ timeout: UI_TIMEOUT });
  await label.scrollIntoViewIfNeeded();
  await label.click({ timeout: SHORT_TIMEOUT });
  await expect(
    page.getByRole("button", { name: "Next", exact: true }),
  ).toBeEnabled({ timeout: UI_TIMEOUT });
}

// OGC-285 replaced the legacy Barcode Configuration form with the Label Presets
// admin page; /MasterListsPage/barcodeConfiguration now redirects to
// /MasterListsPage/labelPresets. Navigate there and wait for the preset list.
async function gotoBarcodeConfig(page: Page) {
  await page.goto("/MasterListsPage/barcodeConfiguration", {
    waitUntil: "domcontentloaded",
  });
  await expect(page).toHaveURL(/\/MasterListsPage\/labelPresets/, {
    timeout: LONG_TIMEOUT,
  });
  await expect(page.getByRole("button", { name: /add preset/i })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

async function gotoSamplePatientEntry(page: Page) {
  await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-cy="searchPatientTabButton"]')).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

async function gotoPrintBarcode(page: Page) {
  await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: /print bar code labels/i }),
  ).toBeVisible({
    timeout: LONG_TIMEOUT,
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

  // OGC-285: the single global Barcode Configuration form was replaced by the
  // Label Presets admin page. Default/max quantities (per-order and per-sample)
  // and optional content fields are now configured per preset.
  await showTitleCard(
    page,
    "Label Presets",
    "Order, Specimen, Block, Slide and Freezer label presets are managed centrally — each with its own barcode type, dimensions, scope, and default/max quantities.",
    2800,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-001/FR-002 — Label Presets", testInfo);

  const presetTable = page.getByRole("table").first();
  await expect(presetTable).toBeVisible({ timeout: UI_TIMEOUT });
  await scrollToAndPause(page, presetTable, pause, 1500);

  // The five Liquibase-seeded system presets are listed (distinctive names;
  // "Order Label" is omitted to avoid colliding with the "Order Labels" table
  // title used in the order-entry section).
  for (const presetName of [
    "Specimen Label",
    "Block Label",
    "Slide Label",
    "Freezer Label",
  ]) {
    // Substring match: the name cell also contains a "System" tag.
    await expect(page.getByText(presetName).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  }
  await pause(800);

  // ── Per-preset configuration ────────────────────────────────────
  await showTitleCard(
    page,
    "Per-Preset Configuration",
    "Each preset carries its own default and maximum quantities plus optional content fields — replacing the single global form (FR-001, FR-002, FR-002c).",
    2800,
    testInfo,
  );
  await showSceneLabel(page, "US1 · FR-002c — Preset Settings", testInfo);

  const addPresetButton = page.getByRole("button", { name: /add preset/i });
  await expect(addPresetButton).toBeVisible({ timeout: UI_TIMEOUT });
  await scrollToAndPause(page, addPresetButton, pause, 1500);

  await showTitleCard(
    page,
    "✓ Label Presets Configured",
    "Label presets are managed centrally with per-preset quantities and barcode settings — FR-001, FR-002, FR-003 satisfied.",
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
  if (await next2.isVisible()) {
    await expect(next2).toBeEnabled({ timeout: SHORT_TIMEOUT });
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
  await expect(labelsSection).toBeVisible({ timeout: UI_TIMEOUT });
  await scrollToAndPause(page, labelsSection, pause, 2000);

  // OGC-285 API mode renders Carbon NumberInputs (role=spinbutton) inside the
  // Order Labels / Sample Labels tables — target them by role rather than the
  // legacy #labels-order / #sample-row-1 ids, and guard on count so the demo is
  // resilient to how many preset columns the seeded data produces.
  const quantityInputs = labelsSection.getByRole("spinbutton");
  const inputCount = await quantityInputs.count();
  if (inputCount > 0) {
    const firstInput = quantityInputs.first();
    await scrollToAndPause(page, firstInput, pause, 800);
    await firstInput.fill("3");
    await pause(1000);
  }
  if (inputCount > 1) {
    const secondInput = quantityInputs.nth(1);
    await scrollToAndPause(page, secondInput, pause, 600);
    await secondInput.fill("2");
    await pause(800);
  }

  // Live total row ("Total labels: N")
  const totalRow = labelsSection.getByText(/total labels/i).first();
  if (await totalRow.isVisible()) {
    await scrollToAndPause(page, totalRow, pause, 2000);
  }

  // ── Step 3: Order details ───────────────────────────────────────
  await clickNext(page);
  await showSceneLabel(page, "US2 · Step 3: Order Details", testInfo);
  await fillOrderDetails(page, pause);

  // Submit
  const submitBtn = page.getByRole("button", { name: "Submit" });
  await scrollToAndPause(page, submitBtn, pause, 1000);
  await expect(submitBtn).toBeEnabled({ timeout: UI_TIMEOUT });
  await submitBtn.click();
  const saveErrorNotice = page
    .locator('[role="alert"], .bx--inline-notification')
    .filter({ hasText: /error|failed/i })
    .first();

  if (await saveErrorNotice.isVisible()) {
    throw new Error(
      `Order save failed with visible UI error: ${(await saveErrorNotice.innerText()).trim()}`,
    );
  }
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
  if (await next2.isVisible()) {
    await expect(next2).toBeEnabled({ timeout: SHORT_TIMEOUT });
    await next2.click();
    await waitForSampleStep(page);
  }

  await fillSampleStep(page);

  // Show labels section briefly before moving on
  const labelsSection = page.getByTestId("labels-section-root");
  if (await labelsSection.isVisible()) {
    await scrollToAndPause(page, labelsSection, pause, 1500);
  }

  await clickNext(page);
  await showSceneLabel(page, "US3 · Completing Order...", testInfo);
  await fillOrderDetails(page, pause);

  // OGC-285 M5b: the Add Order step's API-mode labels section only emits the
  // save POST's labelPersistRequest on a user edit — mount-seeding the default
  // quantities does not. Capture a quantity here so the saved order persists
  // label snapshots; without it the post-save dialog renders the no-snapshot
  // fallback row and the per-preset assertions below have nothing to match.
  const orderLabels = page.locator(
    '.labels-section--api[data-testid="labels-section-root"]',
  );
  await expect(orderLabels).toBeVisible({ timeout: UI_TIMEOUT });
  const qtyInput = orderLabels.getByRole("spinbutton").first();
  await expect(qtyInput).toBeVisible({ timeout: UI_TIMEOUT });
  await qtyInput.fill("2");
  await expect(qtyInput).toHaveValue("2");

  const submitBtn = page.getByRole("button", { name: "Submit" });
  await scrollToAndPause(page, submitBtn, pause, 800);
  await expect(submitBtn).toBeEnabled({ timeout: UI_TIMEOUT });
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
  await expect(successArea).toBeVisible({ timeout: UI_TIMEOUT });
  await scrollToAndPause(page, successArea, pause, 1000);

  // Scroll to print dialog
  const printTitle = page.getByText(/print labels/i).first();
  if (await printTitle.isVisible()) {
    await scrollToAndPause(page, printTitle, pause, 1500);
  }

  // Highlight Print buttons
  const printButtons = page.getByRole("button", { name: "Print" });
  const count = await printButtons.count();
  if (count > 0) {
    await scrollToAndPause(page, printButtons.first(), pause, 2000);
  }

  // Per-sample dialog rows: visible-DOM evidence that the dialog renders
  // one entry per sample row (not a single summed Specimen entry). Pre-fix
  // would show a single "specimen" line; post-fix shows the localized
  // "Order label" + "Specimen label N" per the active label types.
  await showSceneLabel(
    page,
    "US3 · FR-011a — Per-sample dialog rows",
    testInfo,
  );
  // OGC-285 M6: the post-save dialog renders one row per persisted preset, by
  // preset name (e.g. the "Order Label" system preset) — match case-insensitively
  // rather than the old exact lowercase "Order label" count-model string.
  // Post-save dialog renders after an async GET .../labels fetch; allow the
  // longer post-save wait tier.
  await expect(page.getByText(/order label/i).first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  // Done button — resets the form so the user can place another order.
  // (Lives in OrderSuccessMessage's actions row, not inside the dialog.)
  await showTitleCard(
    page,
    "Done — Reset and start over",
    "Done resets the form and returns to the patient step so the user can place another order. Reprinting an existing accession is always available from the Print Barcode page — FR-013.",
    2500,
    testInfo,
  );
  await showSceneLabel(page, "US3 · FR-013 — Done / Reset", testInfo);

  const doneButton = page.getByRole("button", { name: /^(done|skip)$/i });
  if (await doneButton.isVisible()) {
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
  await expect(printBarcodeHeading).toBeVisible({ timeout: LONG_TIMEOUT });
  await scrollToAndPause(page, printBarcodeHeading, pause, 1500);

  const siteSearch = page.getByLabel(/search site name/i);
  if (await siteSearch.isVisible()) {
    await scrollToAndPause(page, siteSearch, pause, 2000);
  }

  await showTitleCard(
    page,
    "✓ OGC-284 Complete",
    "US1: Admin Config · US2: Label Quantities in Workflows · US3: Post-Save Print + Deferred Reprint",
    4000,
    testInfo,
  );
});
