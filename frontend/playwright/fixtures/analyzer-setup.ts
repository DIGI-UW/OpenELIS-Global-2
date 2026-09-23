import { expect, Locator, Page } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../helpers/timeouts";

const escapeRegExp = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export class AnalyzerSetupPage {
  readonly page: Page;
  readonly surface: Locator;
  readonly nameInput: Locator;
  readonly typePicker: Locator;
  readonly labUnitPicker: Locator;

  constructor(page: Page) {
    this.page = page;
    this.surface = page.locator('[data-testid="analyzer-setup"]');
    this.nameInput = page.getByRole("textbox", { name: "Analyzer name" });
    this.typePicker = page.getByRole("combobox", { name: "Analyzer type" });
    this.labUnitPicker = page.getByRole("combobox", { name: /^Lab units/ });
  }

  async expectOpen() {
    await expect(this.surface).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      this.surface.getByRole("heading", {
        level: 2,
        name: "Set up a new analyzer",
      }),
    ).toBeVisible();
  }

  async fillName(name: string) {
    await this.nameInput.click();
    await expect(this.nameInput).toBeFocused();
    await this.nameInput.press("ControlOrMeta+A");
    await this.nameInput.pressSequentially(name);
    await expect(this.nameInput).toHaveValue(name);
  }

  async selectProfile(profileName: string) {
    await this.typePicker.click();
    await this.typePicker.fill(profileName);
    await this.page
      .getByRole("option", {
        name: new RegExp(escapeRegExp(profileName), "i"),
      })
      .first()
      .click();
    await expect(this.page).toHaveURL(
      (url) =>
        Boolean(url.searchParams.get("profile")) &&
        Boolean(url.searchParams.get("revision")),
      { timeout: UI_TIMEOUT },
    );
    await expect(this.typePicker).toHaveValue(
      new RegExp(escapeRegExp(profileName), "i"),
    );
    await expect(this.nameInput).toBeEditable();
  }

  async selectFirstLabUnit() {
    await this.labUnitPicker.click();
    const option = this.page.locator('[role="option"]:visible').first();
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();
    await this.nameInput.click();
    await expect(option).not.toBeVisible({ timeout: UI_TIMEOUT });
  }

  async selectLabUnit(name: string) {
    await this.labUnitPicker.click();
    await this.labUnitPicker.fill(name);
    const option = this.page
      .locator('[role="option"]:visible')
      .filter({ hasText: name })
      .first();
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();
    await this.nameInput.click();
    await expect(option).not.toBeVisible({ timeout: UI_TIMEOUT });
  }

  async continueToVerify() {
    await this.page.getByRole("button", { name: "Continue to Verify" }).click();
    await expect(this.page).toHaveURL(
      (url) =>
        url.searchParams.get("setup") === "verify" &&
        Boolean(url.searchParams.get("analyzerId")),
      { timeout: LONG_TIMEOUT },
    );
  }

  async continueToConnect() {
    const button = this.page.getByRole("button", {
      name: "Continue to Connect",
    });
    await expect(button).toBeEnabled({ timeout: LONG_TIMEOUT });
    await button.click();
    await expect(this.page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
      { timeout: UI_TIMEOUT },
    );
  }

  async fillNetworkAddress(address: string) {
    await this.page
      .getByRole("textbox", {
        name: /Analyzer (?:source )?address/,
      })
      .fill(address);
  }

  async fillPort(port: string) {
    const input = this.surface.getByRole("spinbutton", { name: /port/i });
    if (await input.isVisible()) {
      await input.click();
      await input.press("ControlOrMeta+A");
      await input.pressSequentially(port);
      await input.press("Tab");
      await expect(input).toHaveValue(port);
    }
  }

  async fillImportDirectory(path: string) {
    await this.page
      .getByRole("textbox", { name: "Analyzer file directory" })
      .fill(path);
  }

  async testConnection() {
    await this.page
      .getByRole("button", { name: "Test connection", exact: true })
      .click();
    await expect(
      this.page.getByRole("heading", { name: "Connection evidence" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(this.page.getByText("Connection ready")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  }

  async close() {
    await this.page
      .getByRole("button", { name: "Close analyzer setup" })
      .click();
    await expect(this.surface).not.toBeVisible({ timeout: UI_TIMEOUT });
  }
}
