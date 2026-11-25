import { test, expect, type Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-439: the Result Delivery foundation. An administrator sets the Email
 * channel up on the Channels page and sends a test message; the outcome is
 * reported in plain language and recorded in Sent Messages, which every
 * product's messages share. Delivery Settings round-trip, External
 * Connections no longer offers the channel types, and a bench user is kept
 * out of Sent Messages.
 *
 * The SMTP host points at a port nothing listens on, so the send fails at
 * once with "Could not reach"; no mail server is needed.
 */

const CHANNELS_URL = "/MasterListsPage/notificationTriggerConfig/channels";
const UNREACHABLE_HOST = "127.0.0.1";
const UNREACHABLE_PORT = "2525";

async function openChannels(page: Page) {
  await page.goto(CHANNELS_URL, {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(page.getByTestId("channel-row-EMAIL")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

test("the Email channel is set up, tested and its failure lands in Sent Messages", async ({
  page,
}) => {
  await test.step("set the Email channel to an unreachable SMTP host", async () => {
    await openChannels(page);
    // An earlier run may have left the unreachable host on file with its
    // failure; start from another host so the save below changes the connection.
    const reset = await page.request.put(
      `${API_PREFIX}/rest/delivery/channels/email`,
      {
        headers: { "X-CSRF-Token": await csrfToken(page) },
        data: {
          active: true,
          host: "smtp.example.org",
          port: 587,
          security: "STARTTLS",
          senderAddress: "lab@example.org",
        },
      },
    );
    expect(reset.status()).toBe(200);
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("channel-row-EMAIL")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await page.getByTestId("channel-edit-EMAIL").click();
    await page.getByLabel("SMTP host").fill(UNREACHABLE_HOST);
    await page.getByLabel("Port", { exact: true }).fill(UNREACHABLE_PORT);
    await page.getByLabel("Sender address").fill("lab@example.org");
    await page.getByLabel("Sender name").fill("CPHL");
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/delivery/channels/email") &&
        response.request().method() === "PUT",
    );
    await page.getByTestId("channel-save-EMAIL").click();
    await saved;
    await expect(page.getByTestId("channel-status-EMAIL")).toHaveText("Ready", {
      timeout: UI_TIMEOUT,
    });
  });

  await test.step("the stored channel never carries a secret", async () => {
    const listed = await page.request.get(
      `${API_PREFIX}/rest/delivery/channels`,
    );
    expect(listed.status()).toBe(200);
    const channels = (await listed.json()) as {
      channel: string;
      secret?: string;
    }[];
    expect(channels.map((channel) => channel.channel).sort()).toEqual([
      "EMAIL",
      "SMS",
      "WHATSAPP",
    ]);
    expect(channels.every((channel) => channel.secret === undefined)).toBe(
      true,
    );
  });

  await test.step("a test send reports the unreachable host and marks the channel Failing", async () => {
    await page.getByTestId("channel-test-EMAIL").click();
    await page
      .getByLabel("Address", { exact: true })
      .fill("someone@example.org");
    const tested = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/delivery/channels/email/test") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Send", exact: true }).click();
    await tested;
    await expect(page.getByTestId("channel-test-outcome")).toContainText(
      `Could not reach ${UNREACHABLE_HOST} on port ${UNREACHABLE_PORT}`,
      { timeout: UI_TIMEOUT },
    );
    await page
      .getByRole("button", { name: "Close", exact: true })
      .last()
      .click();
    await expect(page.getByTestId("channel-status-EMAIL")).toHaveText(
      "Failing",
      {
        timeout: UI_TIMEOUT,
      },
    );
  });

  await test.step("Sent Messages lists the test under product Test as Failed, with its attempt", async () => {
    await page.goto("/SentMessages?product=test&status=failed", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { name: "Sent Messages" }),
    ).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    const row = page.locator('[data-testid^="sent-row-"]').first();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row).toContainText("Test");
    await expect(row).toContainText("Failed");
    await expect(row).toContainText(`Could not reach ${UNREACHABLE_HOST}`);
    await expect(row).toContainText("so•••••@example.org");
    await row.getByRole("button", { name: /expand/i }).click();
    await expect(page.getByText("OpenELIS test message")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { name: "Attempts", exact: true }),
    ).toBeVisible();
  });

  await test.step("Resend queues the same message again", async () => {
    const row = page.locator('[data-testid^="sent-row-"]').first();
    const id = (await row.getAttribute("data-testid"))!.replace(
      "sent-row-",
      "",
    );
    const queued = page.waitForResponse(
      (response) =>
        response.url().includes(`/rest/delivery/messages/${id}/resend`) &&
        response.request().method() === "POST",
    );
    await page.getByTestId(`resend-${id}`).click();
    const response = await queued;
    expect(response.status()).toBe(200);
    await expect(page.getByText("Message queued again.")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  await test.step("switch the channel off again", async () => {
    const put = await page.request.put(
      `${API_PREFIX}/rest/delivery/channels/email`,
      {
        headers: { "X-CSRF-Token": await csrfToken(page) },
        data: {
          active: false,
          host: UNREACHABLE_HOST,
          port: Number(UNREACHABLE_PORT),
          security: "STARTTLS",
        },
      },
    );
    expect(put.status()).toBe(200);
  });
});

test("delivery settings round-trip and refuse bad retry delays", async ({
  page,
}) => {
  await page.goto("/MasterListsPage/notificationTriggerConfig/delivery", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  const delays = page.getByLabel("Retry after (minutes, in order)");
  await expect(delays).toBeVisible({ timeout: UI_TIMEOUT });
  await delays.fill("0,x");
  await page.getByTestId("delivery-settings-save").click();
  await expect(delays).toHaveAttribute("data-invalid", "true");

  await delays.fill("1,5,15");
  await page
    .getByLabel("Lab contact line")
    .fill("CPHL, 7000 0000, 08:00 to 17:00");
  const saved = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/delivery/settings") &&
      response.request().method() === "PUT",
  );
  await page.getByTestId("delivery-settings-save").click();
  expect((await saved).status()).toBe(200);
  await expect(page.getByText("Settings saved.")).toBeVisible({
    timeout: UI_TIMEOUT,
  });

  const settings = await page.request.get(
    `${API_PREFIX}/rest/delivery/settings`,
  );
  const body = (await settings.json()) as {
    retryDelays: number[];
    labContact: string;
  };
  expect(body.retryDelays).toEqual([1, 5, 15]);
  expect(body.labContact).toBe("CPHL, 7000 0000, 08:00 to 17:00");
});

test("External Connections no longer offers the channel types for a new row", async ({
  page,
}) => {
  const form = await page.request.get(`${API_PREFIX}/rest/ExternalConnection`);
  expect(form.status()).toBe(200);
  const offered = ((await form.json()) as { programmedConnections: string[] })
    .programmedConnections;
  expect(offered).not.toContain("SMTP_SERVER");
  expect(offered).not.toContain("WHATSAPP_SERVER");
  expect(offered).toContain("SMPP_SERVER");
  expect(offered).toContain("BMP_SMS_SERVER");
  const refused = await page.request.post(
    `${API_PREFIX}/rest/ExternalConnection`,
    {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: {
        externalConnection: {
          programmedConnection: "SMTP_SERVER",
          activeAuthenticationType: "BASIC",
          uri: "smtp://mail.example.org:587",
        },
      },
    },
  );
  expect(refused.status()).toBe(400);
});

test("the Notification Triggers sent tab redirects to Sent Messages", async ({
  page,
}) => {
  await page.goto("/MasterListsPage/notificationTriggerConfig/sent", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(page).toHaveURL(/\/SentMessages\?product=trigger/, {
    timeout: UI_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Sent Messages" }),
  ).toBeVisible({
    timeout: UI_TIMEOUT,
  });
});

test.describe("as a user without the Reports role", () => {
  // The EQA participant is a bench user with Results and Reception only.
  test.use({ storageState: "playwright/.auth/participant.json" });

  test("Sent Messages and the delivery APIs are refused", async ({ page }) => {
    expect(
      (await page.request.get(`${API_PREFIX}/rest/delivery/messages`)).status(),
    ).toBe(403);
    expect(
      (await page.request.get(`${API_PREFIX}/rest/delivery/channels`)).status(),
    ).toBe(403);
    expect(
      (await page.request.get(`${API_PREFIX}/rest/delivery/settings`)).status(),
    ).toBe(403);
    await page.goto("/SentMessages", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByText("Access Denied")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
