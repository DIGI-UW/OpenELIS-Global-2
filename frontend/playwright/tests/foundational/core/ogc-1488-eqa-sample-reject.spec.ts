import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1488: an EQA sample that fails intake is rejected, not resampled, and
 * the rejection needs one of the lab's rejection reasons.
 */

const API = "/api/OpenELIS-Global";

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const CHECKLIST_ITEMS = `${API}/rest/sample-acceptance-checklist/admin/items`;

test("an EQA sample is rejected with a chosen reason", async ({ page }) => {
  test.setTimeout(240_000);
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const headers = { "X-CSRF-Token": await csrfToken(page) };
  const enrolled = await page.request.post(`${API}/rest/eqa/my-programs`, {
    headers,
    data: { programName: `PW1488 scheme ${Date.now()}`, provider: "PW" },
  });
  expect(enrolled.status()).toBe(201);
  const enrollmentId = String((await enrolled.json()).id);
  // Sample Check offers its actions when the domain has a checklist.
  const label = `PW1488 container intact ${Date.now()}`;
  const added = await page.request.post(CHECKLIST_ITEMS, {
    headers,
    data: { domain: "CLINICAL", label },
  });
  expect(added.status()).toBe(200);
  const checklistItemId = String((await added.json()).id);
  try {
    await page.goto("/order/clinical/enter?eqa=true", {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    const labNumber = await page.locator("#labNumber").inputValue();
    await expect(
      page.locator(`#eqaProgramId option[value="${enrollmentId}"]`),
    ).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await page.locator("#eqaProgramId").selectOption(enrollmentId);
    const sampleType = page
      .getByTestId("order-sample-test-section")
      .getByLabel("Sample Type");
    await sampleType.selectOption({ label: "Serum" });
    const firstTest = page
      .getByTestId("order-sample-test-section")
      .locator('input[type="checkbox"][id^="test-0-"]')
      .first();
    await expect(firstTest).toBeAttached({ timeout: LONG_TIMEOUT });
    const firstTestId = await firstTest.getAttribute("id");
    await page.locator(`label[for="${firstTestId}"]`).click();
    await page.getByRole("button", { name: "Save and exit" }).click();
    await expect(page).toHaveURL(/highlight=/, { timeout: LONG_TIMEOUT });

    await page.goto(
      `/order/clinical/collect?order=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(page.getByText(`${labNumber}-1`).first()).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await page.getByRole("button", { name: "Save and exit" }).click();
    await expect(page).toHaveURL(/highlight=/, { timeout: LONG_TIMEOUT });

    await page.goto(
      `/order/clinical/qa?order=${encodeURIComponent(labNumber)}`,
      {
        waitUntil: "domcontentloaded",
      },
    );
    const reject = page.getByRole("button", { name: /Reject$/ });
    await expect(reject).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.getByRole("button", { name: /Resample$/ })).toHaveCount(
      0,
    );
    await expect(page.getByText("Patient Information")).toHaveCount(0);

    await reject.click();
    const commit = page.getByRole("button", { name: /Commit reject$/ });
    await commit.click();
    await expect(page.getByText("Choose a rejection reason.")).toBeVisible();

    const reason = page.locator("#reject-reason-code");
    const firstReason = await reason
      .locator("option")
      .evaluateAll(
        (options) =>
          (options as HTMLOptionElement[]).map((o) => o.value).find(Boolean) ||
          "",
      );
    expect(firstReason).not.toBe("");
    await reason.selectOption(firstReason);
    await commit.click();

    await expect(page.getByText("This sample was rejected")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  } finally {
    await page.request.put(`${CHECKLIST_ITEMS}/${checklistItemId}`, {
      headers,
      data: { label, active: false },
    });
    await page.request.delete(`${API}/rest/eqa/my-programs/${enrollmentId}`, {
      headers,
    });
  }
});
