import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1423 — Refer Out from Prepare Samples. A tube is referred from its row
 * (or every in-house tube at once) to a reference laboratory with a reason;
 * the referral is staged on the step and saved with the step's Save; an order
 * whose every tube is referred skips Sample check and shows as Referred Out on
 * the dashboard; the referred test leaves the worklist and shows on the
 * Referred Out results screen.
 */

const API = "/api/OpenELIS-Global";
const REFERENCE_LAB = "CEDRES";
const SEED_TEST_ID = "13";

async function openPrepareSamples(page: Page, accessionNumber: string) {
  await page.goto(
    `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
    { timeout: NAV_TIMEOUT },
  );
  await expect(page.getByText("Refer Out / Subcontract")).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
}

async function pickReferenceLab(page: Page, scope = page.locator("main")) {
  const lab = scope.getByRole("combobox", { name: "Reference lab" });
  await lab.click();
  await page.getByRole("option", { name: REFERENCE_LAB }).click();
  await expect(lab).toHaveValue(REFERENCE_LAB);
}

async function fillCollectionDateTime(page: Page, sampleIndex: number) {
  const now = new Date();
  const dd = String(now.getUTCDate()).padStart(2, "0");
  const mm = String(now.getUTCMonth() + 1).padStart(2, "0");
  const yyyy = now.getUTCFullYear();
  const date = page.locator(`#collectionDate-${sampleIndex}`);
  await date.fill(`${dd}/${mm}/${yyyy}`);
  await page.keyboard.press("Escape");
  await page.locator(`#collectionTime-${sampleIndex}`).fill("10:00");
  await page.keyboard.press("Tab");
}

async function referredOutItems(page: Page, accessionNumber: string) {
  const response = await page.request.get(
    `${API}/rest/ReferredOutTests?searchType=LAB_NUMBER&labNumber=${encodeURIComponent(accessionNumber)}`,
  );
  expect(response.status()).toBe(200);
  return (await response.json()).referralDisplayItems || [];
}

async function dashboardLists(
  page: Page,
  accessionNumber: string,
  status: string,
) {
  const response = await page.request.get(
    `${API}/rest/order/dashboard?search=${encodeURIComponent(accessionNumber)}&status=${status}`,
  );
  expect(response.status()).toBe(200);
  const orders = (await response.json()).orders || [];
  return orders.find((order) => order.labNumber === accessionNumber) || null;
}

async function worklistLists(page: Page, accessionNumber: string) {
  const response = await page.request.get(
    `${API}/rest/WorkPlanByTest?test_id=${SEED_TEST_ID}`,
  );
  expect(response.status()).toBe(200);
  const body = await response.text();
  return body.includes(accessionNumber);
}

test.describe("Refer Out from Prepare Samples (OGC-1423)", () => {
  test("a tube referred from its row is saved with the step and the fully referred order skips Sample check", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const accessionNumber = await createSampleOrder(page, {});
    expect(accessionNumber, "seeded order").not.toBe("");
    expect(
      await worklistLists(page, accessionNumber),
      "the test is on the worklist before the referral",
    ).toBe(true);

    await openPrepareSamples(page, accessionNumber);
    await test.step("Refer Out stages the referral on the tube", async () => {
      await page
        .locator("main")
        .getByRole("button", { name: "Refer Out", exact: true })
        .first()
        .click();
      await pickReferenceLab(page);
      const reason = page.getByLabel("Reason", { exact: true });
      const reasonOptions = reason.locator("option");
      await expect
        .poll(() => reasonOptions.count(), { timeout: UI_TIMEOUT })
        .toBeGreaterThan(1);
      await reason.selectOption({ index: 1 });
      await page.getByRole("button", { name: "Save Referral" }).click();
      await expect(page.getByTestId("refer-out-pending")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(
        await referredOutItems(page, accessionNumber),
        "nothing is saved before the step's Save",
      ).toHaveLength(0);
    });

    await test.step("Save and next saves the referral and finishes order entry without Sample check", async () => {
      // A referred tube is still a physical tube: its collection date and time
      // are the step's remaining requirement before Save and next.
      await fillCollectionDateTime(page, 0);
      await expect(
        page
          .locator("main")
          .getByRole("button", { name: "Save and next", exact: true }),
      ).toBeEnabled({ timeout: UI_TIMEOUT });
      const saved = page.waitForResponse(
        (r) =>
          /rest\/SamplePatientEntry$/.test(r.url()) &&
          r.request().method() === "POST",
      );
      await page
        .locator("main")
        .getByRole("button", { name: "Save and next", exact: true })
        .click();
      expect((await saved).status()).toBe(200);
      await expect(page).toHaveURL(/done=/, { timeout: NAV_TIMEOUT });
      await expect(page).not.toHaveURL(/\/qa/);

      const items = await referredOutItems(page, accessionNumber);
      expect(items).toHaveLength(1);
      expect(items[0]).toEqual(
        expect.objectContaining({
          accessionNumber,
          referenceLabDisplay: REFERENCE_LAB,
        }),
      );
      const order = await (
        await page.request.get(
          `${API}/rest/order/search?labNumber=${encodeURIComponent(accessionNumber)}`,
        )
      ).json();
      expect(order.fullyReferred).toBe(true);
      expect(order.complete).toBe(true);
      expect(order.samples[0].referralItems[0]).toEqual(
        expect.objectContaining({
          referredInstituteName: REFERENCE_LAB,
          referralStatus: "DRAFT",
        }),
      );
      expect(
        await worklistLists(page, accessionNumber),
        "the referred test left the worklist",
      ).toBe(false);
    });

    await test.step("the dashboard files it under Referred Out and Has referred tests", async () => {
      const referredOut = await dashboardLists(
        page,
        accessionNumber,
        "referred_out",
      );
      expect(referredOut, "listed under Referred Out").not.toBeNull();
      expect(referredOut.referralSummary).toEqual(
        expect.objectContaining({
          referredTests: 1,
          totalTests: 1,
          referredTo: REFERENCE_LAB,
        }),
      );
      expect(
        await dashboardLists(page, accessionNumber, "has_referred"),
        "listed under Has referred tests",
      ).not.toBeNull();
      expect(
        await dashboardLists(page, accessionNumber, "in_progress"),
        "no longer in progress",
      ).toBeNull();
    });

    await test.step("reopening the step shows the saved referral", async () => {
      await openPrepareSamples(page, accessionNumber);
      const row = page.locator("main tr", { hasText: REFERENCE_LAB }).first();
      await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row).toContainText("Draft");
      await expect(page.getByTestId("refer-out-pending")).toHaveCount(0);
    });
  });

  test("Refer out all in-house samples stages every tube at once", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accessionNumber = await createSampleOrder(page, {});
    expect(accessionNumber, "seeded order").not.toBe("");
    await openPrepareSamples(page, accessionNumber);

    await page.getByTestId("refer-out-all").click();
    const bulkForm = page.getByTestId("refer-out-bulk-form");
    await expect(bulkForm).toBeVisible();
    await pickReferenceLab(page, bulkForm);
    await bulkForm.getByRole("button", { name: "Save Referral" }).click();

    await expect(page.getByTestId("refer-out-pending")).toHaveCount(1, {
      timeout: UI_TIMEOUT,
    });
    await expect(page.getByTestId("refer-out-all")).toBeDisabled();

    const saved = page.waitForResponse(
      (r) =>
        /rest\/SamplePatientEntry$/.test(r.url()) &&
        r.request().method() === "POST",
    );
    await page
      .locator("main")
      .getByRole("button", { name: "Save and exit", exact: true })
      .click();
    expect((await saved).status()).toBe(200);
    await expect
      .poll(
        async () => (await referredOutItems(page, accessionNumber)).length,
        {
          timeout: UI_TIMEOUT,
        },
      )
      .toBe(1);
  });
});
