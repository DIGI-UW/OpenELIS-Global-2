import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1551: a message sent to a channel that is not set up fails with one
 * recorded attempt and its reason, and a resend is a row of its own, tagged
 * Resend, that names the message it repeats. SMS is never set up on the test
 * stack, so its test send fails at once.
 */

const CHANNELS_URL = "/MasterListsPage/notificationTriggerConfig/channels";

test("a send to a channel that is not set up counts its attempt, and its resend says what it repeats", async ({
  page,
}) => {
  const address = `+6757${String(Date.now()).slice(-7)}`;
  let original = "";
  let resent = "";

  await test.step("a test SMS fails because SMS is not set up", async () => {
    await page.goto(CHANNELS_URL, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByTestId("channel-row-SMS")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await page.getByTestId("channel-test-SMS").click();
    await page.getByLabel("Address", { exact: true }).fill(address);
    const tested = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/delivery/channels/sms/test") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Send", exact: true }).click();
    const outcome = await (await tested).json();
    expect(outcome.success).toBe(false);
    original = String(outcome.sentMessageId);
    await expect(page.getByTestId("channel-test-outcome")).toContainText(
      "SMS is not set up",
      { timeout: UI_TIMEOUT },
    );
    await page
      .getByRole("button", { name: "Close", exact: true })
      .last()
      .click();
  });

  await test.step("Sent Messages shows the failure with its one attempt and reason", async () => {
    await page.goto("/SentMessages?product=test&status=failed", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    await expect(breadcrumb.getByRole("link", { name: "Reports" })).toBeVisible(
      { timeout: UI_TIMEOUT },
    );
    const row = page.getByTestId(`sent-row-${original}`);
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row).toContainText("Failed");
    await expect(row.getByRole("cell").filter({ hasText: /^1$/ })).toHaveCount(
      1,
    );
    await expect(page.getByTestId(`sent-resend-${original}`)).toHaveCount(0);
    await row.getByRole("button", { name: /expand/i }).click();
    await expect(
      page.getByRole("heading", { name: "Attempts", exact: true }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.getByText("SMS is not set up").first()).toBeVisible();
    await expect(page.getByText("No attempt yet")).toHaveCount(0);
  });

  await test.step("Resend adds a row tagged Resend that names the message it repeats", async () => {
    const queued = page.waitForResponse(
      (response) =>
        response.url().includes(`/rest/delivery/messages/${original}/resend`) &&
        response.request().method() === "POST",
    );
    await page.getByTestId(`resend-${original}`).click();
    const response = await queued;
    expect(response.status()).toBe(200);
    resent = String((await response.json()).id);
    expect(resent).not.toBe(original);
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    const row = page.getByTestId(`sent-row-${resent}`);
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.getByTestId(`sent-resend-${resent}`)).toHaveText(
      "Resend",
    );
    await expect(row.getByRole("cell").filter({ hasText: /^1$/ })).toHaveCount(
      1,
    );
    await row.getByRole("button", { name: /expand/i }).click();
    await expect(page.getByTestId(`sent-resent-from-${resent}`)).toContainText(
      "Resend of the message sent",
      { timeout: UI_TIMEOUT },
    );
    await expect(page.getByTestId(`sent-row-${original}`)).toBeVisible();
  });

  await test.step("Show the original lists only the message the resend repeats", async () => {
    await page.getByTestId(`sent-show-original-${resent}`).click();
    await expect(page).toHaveURL(new RegExp(`[?&]message=${original}`));
    await expect(page.getByTestId("sent-one-message")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(page.getByTestId(`sent-row-${original}`)).toBeVisible();
    await expect(page.locator('[data-testid^="sent-row-"]')).toHaveCount(1);
    await page.getByRole("button", { name: "Show all messages" }).click();
    await expect(page.getByTestId(`sent-row-${resent}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
