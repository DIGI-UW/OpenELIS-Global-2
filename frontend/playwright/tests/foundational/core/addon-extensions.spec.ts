import { test, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import { probeAddon } from "../../../helpers/addon-package";

/**
 * Add-ons (docs/addons/design.md): an administrator installs a package, sees
 * who published it and what it may read, turns it on; its page then runs in
 * a sandboxed frame. The frame's own script must run under the headers the
 * backend serves add-on files with (Content-Security-Policy: sandbox), get its
 * context by postMessage, and still not be able to read OpenELIS cookies.
 */

// Unique per run: the test only reads the add-on it installed.
const ID = `e2e-${Date.now().toString(36)}`;
const UI_FILE = new RegExp(`/rest/addons/${ID}/ui/probe\\.html$`);

async function openExtensions(page: Page) {
  await page.goto("/MasterListsPage/extensions", {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("addon-admin")).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
}

test.describe.serial("Add-on extensions", () => {
  test("an unsigned package installs turned off and says so", async ({
    page,
  }) => {
    await openExtensions(page);
    await page
      .getByTestId("addon-admin")
      .locator('input[type="file"]')
      .setInputFiles({
        name: `${ID}.zip`,
        mimeType: "application/zip",
        buffer: probeAddon(ID),
      });

    const installed = page.getByTestId("addon-installed");
    await expect(installed).toContainText(
      `E2E probe ${ID} 1.0.0 is installed and turned off`,
      { timeout: LONG_TIMEOUT },
    );
    const card = page.getByTestId(`addon-${ID}`);
    await expect(card.getByTestId(`addon-provenance-${ID}`)).toContainText(
      "Unverified publisher",
    );
    await expect(card).not.toContainText("Verified publisher:");
    await expect(card.getByRole("switch")).not.toBeChecked();
  });

  test("turning it on asks for consent to its permissions", async ({
    page,
  }) => {
    await openExtensions(page);
    const card = page.getByTestId(`addon-${ID}`);
    await card.locator(`label[for="addon-toggle-${ID}"]`).click();

    const consent = page.getByRole("dialog", {
      name: `Turn on E2E probe ${ID}?`,
    });
    await expect(consent).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(consent).toContainText(
      "Lab numbers and received dates of the orders it is shown",
    );
    await consent.getByRole("button", { name: "Turn on" }).click();
    await expect(card.getByRole("switch")).toBeChecked({ timeout: UI_TIMEOUT });
  });

  test("its page runs its own script in the sandbox and gets its context", async ({
    page,
  }) => {
    const served = page.waitForResponse(UI_FILE, { timeout: NAV_TIMEOUT });
    await page.goto(`/extensions/${ID}/probe-page`, {
      waitUntil: "domcontentloaded",
    });
    const response = await served;
    expect(response.status()).toBe(200);
    expect(response.headers()["content-security-policy"]).toContain(
      "sandbox allow-scripts allow-forms",
    );
    expect(response.headers()["x-content-type-options"]).toBe("nosniff");

    const frame = page.frameLocator(`iframe[title="Probe ${ID}"]`);
    await expect(frame.locator("#script")).toHaveText("script ran", {
      timeout: LONG_TIMEOUT,
    });
    await expect(frame.locator("#status")).toHaveText("context received", {
      timeout: LONG_TIMEOUT,
    });
    await expect(frame.locator("#page")).toHaveText("page extension-page");
    await expect(frame.locator("#token")).toHaveText("token oeat.");
    await expect(frame.locator("#origin")).toHaveText("origin null");
    await expect(frame.locator("#cookie")).toHaveText("cookie blocked");
  });

  test("the menu has an entry for it and validation shows its side panel", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const nav = page.getByRole("navigation", { name: "Side navigation" });
    await expect(
      nav.getByRole("link", { name: `Probe ${ID}` }),
    ).toHaveAttribute("href", `/extensions/${ID}/probe-page`, {
      timeout: NAV_TIMEOUT,
    });

    await page.goto("/ResultValidation", { waitUntil: "domcontentloaded" });
    const slot = page.locator(
      '[data-extension-slot-name="result-validation-side-panel-slot"]',
    );
    const panel = slot.frameLocator(`iframe[title="Probe panel ${ID}"]`);
    await expect(panel.locator("#status")).toHaveText("context received", {
      timeout: LONG_TIMEOUT,
    });
    await expect(panel.locator("#page")).toHaveText("page result-validation");
  });

  test("turned off, it disappears and its files are no longer served", async ({
    page,
  }) => {
    await openExtensions(page);
    const card = page.getByTestId(`addon-${ID}`);
    await card.locator(`label[for="addon-toggle-${ID}"]`).click();
    await expect(card.getByRole("switch")).not.toBeChecked({
      timeout: UI_TIMEOUT,
    });

    const file = await page.request.get(
      `/api/OpenELIS-Global/rest/addons/${ID}/ui/probe.html`,
    );
    expect(file.status()).toBe(404);
    await page.goto(`/extensions/${ID}/probe-page`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("addon-page-missing")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    await openExtensions(page);
    await page
      .getByTestId(`addon-${ID}`)
      .getByRole("button", { name: "Remove" })
      .click();
    const confirm = page.getByRole("dialog", {
      name: `Remove E2E probe ${ID}?`,
    });
    await confirm.getByRole("button", { name: "Remove" }).click();
    await expect(page.getByTestId(`addon-${ID}`)).toHaveCount(0, {
      timeout: UI_TIMEOUT,
    });
  });
});
