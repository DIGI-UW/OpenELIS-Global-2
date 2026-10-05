import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1266 (Clinical Order Entry v4): the three-step clinical workflow with
 * one footer on every step, two levels of required, a To continue checklist,
 * storage saved with Prepare Samples, the Sample check that releases the
 * order for testing, an explicit order status on the dashboard, and cancel.
 *
 * The Sample check step exists only while the laboratory's clinical sample
 * acceptance setting is not Off, so the walk reads the setting first and
 * follows whichever path the laboratory is on.
 */

const API = "/api/OpenELIS-Global";

async function acceptanceMode(page: Page): Promise<string> {
  const response = await page.request.get(
    `${API}/rest/sample-acceptance-checklist/enforcement`,
  );
  expect(response.status()).toBe(200);
  const modes = (await response.json()) as Record<string, string>;
  return (modes.clinical || "OPTIONAL").toUpperCase();
}

async function openNewOrder(page: Page) {
  await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: NAV_TIMEOUT,
  });
  return page.locator("#labNumber").inputValue();
}

async function enterNewPatient(page: Page, suffix: string) {
  const section = page.getByTestId("patient-search-section");
  await section.getByRole("button", { name: "New Patient" }).click();
  await section.locator("#nationalId").fill(`QA1266${suffix}${Date.now()}`);
  await section.locator("#lastName").fill(`Walk${suffix}`);
  await section.locator("#firstName").fill("Ida");
  await section
    .getByRole("textbox", { name: "Date of Birth" })
    .fill("05/03/1988");
  await section
    .locator("#create_patient_gender label")
    .filter({ hasText: "Female" })
    .click();
}

async function chooseSerumWithOneTest(page: Page) {
  const sampleSection = page.getByTestId("order-sample-test-section");
  await sampleSection
    .getByLabel("Sample Type *")
    .selectOption({ label: "Serum" });
  const firstTest = sampleSection.locator('label[for^="test-0-"]').first();
  await expect(firstTest).toBeVisible({ timeout: LONG_TIMEOUT });
  await firstTest.click();
}

async function saveWith(page: Page, name: string) {
  const saved = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/SamplePatientEntry") &&
      response.request().method() === "POST",
    { timeout: LONG_TIMEOUT },
  );
  await page.getByRole("button", { name, exact: true }).click();
  expect((await saved).status()).toBe(200);
}

const highlightedRow = (page: Page) => page.locator("tr.order-highlighted");

test.describe("OGC-1266 clinical order entry", () => {
  test("Enter Order lists what is missing, Save and exit needs the save level, and the dashboard names the order", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const labNumber = await openNewOrder(page);

    // An untouched new order is not an unsaved change.
    await expect(page.locator(".save-status-indicator")).toHaveCount(0);

    const checklist = page.getByTestId("to-continue-checklist");
    await expect(checklist).toContainText("Select or create the patient");
    await expect(checklist).toContainText("Add at least one sample type");
    await expect(
      page.getByRole("button", { name: "Save and exit", exact: true }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Save and next", exact: true }),
    ).toBeDisabled();

    await checklist
      .getByRole("link", { name: "Select or create the patient" })
      .click();
    await expect(page.locator("#order-patient-search-lastName")).toBeFocused();

    await enterNewPatient(page, "A");
    await chooseSerumWithOneTest(page);
    // The checklist disappears once nothing is missing.
    await expect(checklist).toHaveCount(0, { timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("button", { name: "Save and exit", exact: true }),
    ).toBeEnabled({ timeout: UI_TIMEOUT });

    await saveWith(page, "Save and exit");
    await expect(page).toHaveURL(/\/order\/clinical\?highlight=/, {
      timeout: LONG_TIMEOUT,
    });
    await expect(highlightedRow(page)).toContainText(labNumber, {
      timeout: LONG_TIMEOUT,
    });
    await expect(highlightedRow(page)).toContainText("Entered");
    await expect(
      highlightedRow(page).getByRole("button", { name: "Continue" }),
    ).toBeVisible();
    await expect(
      highlightedRow(page).getByRole("button", { name: /Cancel order$/ }),
    ).toBeVisible();
  });

  test("Prepare Samples saves storage with the step and the order is released or finished", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const mode = await acceptanceMode(page);
    test.skip(
      mode === "MANDATORY",
      "a mandatory acceptance checklist needs answers this walk does not give",
    );
    const labNumber = await openNewOrder(page);
    await enterNewPatient(page, "B");
    await chooseSerumWithOneTest(page);
    await saveWith(page, "Save and exit");
    await expect(highlightedRow(page)).toContainText(labNumber, {
      timeout: LONG_TIMEOUT,
    });

    await highlightedRow(page)
      .getByRole("button", { name: "Continue" })
      .click();
    await expect(page).toHaveURL(/\/order\/clinical\/collect\?order=/, {
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 2, name: "Prepare Samples" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    const steps = page.locator(".order-stepper .cds--progress-label");
    await expect(steps).toHaveText(
      mode === "OFF"
        ? ["Enter Order", "Prepare Samples"]
        : ["Enter Order", "Prepare Samples", "Sample check"],
    );

    // OGC-1419: the collector is optional, so a sample with its collection
    // date and time and no collector continues. Storage is skipped, and it
    // travels with the save.
    const card = page.getByTestId("sample-collection-card-0");
    await expect(card.locator("#collector-0")).toHaveValue("", {
      timeout: UI_TIMEOUT,
    });
    await expect(card.locator('label[for="collector-0"]')).toHaveText(
      "Collector",
    );
    await expect(page.getByTestId("to-continue-checklist")).toHaveCount(0);
    const storage = page.getByTestId("prepare-storage-section");
    await expect(storage).toContainText(`${labNumber}-1`);
    await page.locator('label[for="skip-storage-checkbox"]').click();
    await expect(storage).toContainText("Storage skipped for 1 sample");

    const lastStep = mode === "OFF";
    const saveRequest = page.waitForRequest(
      (request) =>
        request.url().includes("/rest/SamplePatientEntry") &&
        request.method() === "POST",
    );
    await saveWith(page, lastStep ? "Save and finish" : "Save and next");
    const sent = (await saveRequest).postDataJSON() as {
      sampleOrderItems: { progressStep: string; storageSkipped: boolean };
    };
    expect(sent.sampleOrderItems.progressStep).toBe("SAMPLES_PREPARED");
    expect(sent.sampleOrderItems.storageSkipped).toBe(true);

    if (lastStep) {
      await expect(page).toHaveURL(/\/order\/clinical\?done=/, {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("order-finished-notice")).toContainText(
        `Order ${labNumber} complete`,
      );
      await expect(highlightedRow(page)).toContainText("Complete");
      return;
    }

    await expect(page).toHaveURL(/\/order\/clinical\/qa\?order=/, {
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 2, name: "Sample check" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("button", { name: "Return to Prepare Samples" }),
    ).toBeVisible();

    // Under Optional acceptance a release with a specimen's checklist still
    // unanswered needs a reason, which the second release carries. A
    // laboratory whose clinical checklist has no items has nothing to answer
    // and releases at once.
    const firstAttempt = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/qa-checklist") &&
        response.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: "Release for testing", exact: true })
      .click();
    const firstStatus = (await firstAttempt).status();
    if (firstStatus === 400) {
      const reason = page.locator("#release-note");
      await expect(reason).toBeVisible({ timeout: UI_TIMEOUT });
      await reason.fill("Checklist kept on paper at this bench");
      const released = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/qa-checklist") &&
          response.request().method() === "POST",
      );
      await page
        .getByRole("button", { name: "Release for testing", exact: true })
        .click();
      expect((await released).status()).toBe(200);
    } else {
      expect(firstStatus).toBe(200);
    }
    await expect(page.locator(".qa-success-tile")).toContainText(
      `Order ${labNumber} complete`,
      { timeout: UI_TIMEOUT },
    );
    await expect(page.locator(".order-stepper")).toContainText("Done");

    await page.getByRole("button", { name: "Back to orders" }).click();
    await expect(page).toHaveURL(/\/order\/clinical\?done=/, {
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("order-finished-notice")).toContainText(
      `Order ${labNumber} complete`,
    );
    await expect(highlightedRow(page)).toContainText("Ready for testing", {
      timeout: LONG_TIMEOUT,
    });
    await expect(
      highlightedRow(page).getByRole("button", { name: "Open" }),
    ).toBeVisible();
    await expect(
      highlightedRow(page).getByRole("button", { name: /Cancel order$/ }),
    ).toHaveCount(0);
  });

  test("Cancel order records a reason and hides the order from the open list", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const labNumber = await openNewOrder(page);
    await enterNewPatient(page, "C");
    await chooseSerumWithOneTest(page);
    await saveWith(page, "Save and exit");
    await expect(highlightedRow(page)).toContainText(labNumber, {
      timeout: LONG_TIMEOUT,
    });

    await highlightedRow(page)
      .getByRole("button", { name: /Cancel order$/ })
      .click();
    const dialog = page.getByRole("dialog", {
      name: `Cancel order ${labNumber}?`,
    });
    await expect(dialog).toBeVisible();
    const confirm = dialog.getByRole("button", { name: /Cancel order$/ });
    await expect(confirm).toBeDisabled();
    await dialog
      .locator("#cancel-order-reason")
      .selectOption({ label: "Duplicate order" });
    await expect(confirm).toBeEnabled();
    const cancelled = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/order/cancel") &&
        response.request().method() === "POST",
    );
    await confirm.click();
    expect((await cancelled).status()).toBe(200);

    await expect(
      page.getByRole("row", { name: new RegExp(labNumber) }),
    ).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await page.locator("#status-filter").click();
    await page.getByRole("option", { name: "Cancelled" }).click();
    const row = page.getByRole("row", { name: new RegExp(labNumber) });
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("Cancelled");
    await expect(row.getByRole("button", { name: "Open" })).toBeVisible();

    const order = await page.request.get(
      `${API}/rest/order/search?labNumber=${encodeURIComponent(labNumber)}`,
    );
    expect(order.status()).toBe(200);
    const stored = (await order.json()) as {
      progressStatus: string;
      progress: { cancelReason: string };
    };
    expect(stored.progressStatus).toBe("CANCELLED");
    expect(stored.progress.cancelReason).toBe("Duplicate order");
  });

  test("Discard on an order that was never saved returns to the dashboard", async ({
    page,
  }) => {
    const labNumber = await openNewOrder(page);
    await page
      .getByTestId("patient-search-section")
      .getByRole("button", { name: "New Patient" })
      .click();
    await page.locator("#lastName").fill("Dropme");

    await page.getByRole("button", { name: /Discard$/ }).click();
    const dialog = page.getByRole("dialog", {
      name: "Discard unsaved changes?",
    });
    await dialog.getByRole("button", { name: /Discard order$/ }).click();

    await expect(page).toHaveURL(/\/order\/clinical$/, {
      timeout: LONG_TIMEOUT,
    });
    const order = await page.request.get(
      `${API}/rest/order/search?labNumber=${encodeURIComponent(labNumber)}`,
    );
    expect(order.status()).not.toBe(200);
  });
});
