import { Page, expect, Locator } from "@playwright/test";
import { UI_TIMEOUT } from "../helpers/timeouts";

/**
 * AnalyzerForm Page Object
 *
 * Encapsulates interactions with the analyzer create/edit modal (AnalyzerForm component).
 * Uses data-testid selectors that match the component's DOM structure.
 */
export class AnalyzerFormPage {
  readonly page: Page;
  readonly modal: Locator;
  readonly header: Locator;
  readonly nameInput: Locator;
  readonly typeDropdown: Locator;
  readonly pluginTypeDropdown: Locator;
  readonly defaultConfigDropdown: Locator;
  readonly identifierPatternInput: Locator;
  readonly protocolVersionDropdown: Locator;
  readonly ipAddressInput: Locator;
  readonly portInput: Locator;
  readonly statusDropdown: Locator;
  readonly connectionFields: Locator;
  readonly fileProtocolInfo: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly notification: Locator;

  constructor(page: Page) {
    this.page = page;
    this.modal = page.locator('[data-testid="analyzer-form"]');
    this.header = page.locator('[data-testid="analyzer-form-header"]');
    this.nameInput = page.locator('[data-testid="analyzer-form-name-input"]');
    this.typeDropdown = page.locator(
      '[data-testid="analyzer-form-type-dropdown"]',
    );
    this.pluginTypeDropdown = page.locator(
      '[data-testid="analyzer-form-plugin-type-dropdown"]',
    );
    this.defaultConfigDropdown = page.locator(
      '[data-testid="analyzer-form-default-config-dropdown"]',
    );
    this.identifierPatternInput = page.locator(
      '[data-testid="analyzer-form-identifier-pattern-input"]',
    );
    this.protocolVersionDropdown = page.locator(
      '[data-testid="analyzer-form-protocol-version-dropdown"]',
    );
    this.ipAddressInput = page.locator(
      '[data-testid="analyzer-form-ip-input"]',
    );
    this.portInput = page.locator('[data-testid="analyzer-form-port-input"]');
    this.statusDropdown = page.locator(
      '[data-testid="analyzer-form-status-dropdown"]',
    );
    this.connectionFields = page.locator(
      '[data-testid="analyzer-form-connection-fields"]',
    );
    this.fileProtocolInfo = page.locator(
      '[data-testid="analyzer-form-file-protocol-info"]',
    );
    this.saveButton = page.locator('[data-testid="analyzer-form-save-button"]');
    this.cancelButton = page.locator(
      '[data-testid="analyzer-form-cancel-button"]',
    );
    this.notification = page.locator(
      '[data-testid="analyzer-form-notification"]',
    );
  }

  /** Assert the form modal is open and visible */
  async expectOpen() {
    await expect(this.modal).toBeVisible();
    await expect(this.header).toBeVisible();
  }

  /** Fill the analyzer name field */
  async fillName(name: string) {
    await this.nameInput.fill(name);
  }

  /**
   * Select an item from a Carbon Dropdown using keyboard navigation.
   *
   * Carbon Dropdown (non-filterable) supports: open → ArrowDown/Up → Enter.
   * We press ArrowDown until the target option gets aria-selected, then Enter.
   * This avoids clicking inside the listbox overlay, which causes flaky
   * pointer-interception on adjacent dropdowns during close animation.
   */
  private async selectDropdownItem(dropdown: Locator, text: string) {
    const trigger = dropdown.locator(
      'button[role="combobox"], .cds--list-box__field',
    );
    await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
    await trigger.click();

    // Scope listbox to this dropdown's container (Carbon renders it as a child)
    const listbox = dropdown.getByRole("listbox");
    await expect(listbox).toBeVisible({ timeout: UI_TIMEOUT });

    try {
      // Reset to top, then navigate down to the target option
      const option = listbox.getByRole("option", { name: text }).first();
      await expect(option).toBeVisible({ timeout: UI_TIMEOUT });

      await this.page.keyboard.press("Home");
      const maxPresses = 20;
      for (let i = 0; i < maxPresses; i++) {
        const selected = await option.getAttribute("aria-selected");
        if (selected === "true") break;
        await this.page.keyboard.press("ArrowDown");
      }

      // Fail explicitly if we didn't reach the target
      const finalSelected = await option.getAttribute("aria-selected");
      if (finalSelected !== "true") {
        throw new Error(
          `Could not navigate to option "${text}" after ${maxPresses} ArrowDown presses`,
        );
      }

      await this.page.keyboard.press("Enter");
    } catch (e) {
      // Close the listbox so it doesn't interfere with subsequent interactions
      await this.page.keyboard.press("Escape");
      await expect(listbox).not.toBeVisible({ timeout: UI_TIMEOUT });
      throw e;
    }

    // Ensure the listbox is fully closed before returning
    await expect(listbox).not.toBeVisible({ timeout: UI_TIMEOUT });
  }

  /** Select an analyzer type (category) from the dropdown */
  async selectType(typeText: string) {
    await this.selectDropdownItem(this.typeDropdown, typeText);
  }

  /** Select a plugin type from the dropdown */
  async selectPluginType(typeText: string) {
    await this.selectDropdownItem(this.pluginTypeDropdown, typeText);
  }

  /** Select a default config template from the dropdown */
  async selectDefaultConfig(configText: string) {
    await this.selectDropdownItem(this.defaultConfigDropdown, configText);
  }

  /** Fill the IP address field */
  async fillIpAddress(ip: string) {
    await this.ipAddressInput.fill(ip);
  }

  /** Fill the port field */
  async fillPort(port: string) {
    await this.portInput.fill(port);
  }

  /** Click the save button */
  async save() {
    await this.saveButton.click();
  }

  /** Assert a success notification appeared */
  async expectSuccessNotification() {
    await expect(this.notification).toBeVisible({ timeout: UI_TIMEOUT });
    const cls = await this.notification.getAttribute("class");
    if (cls && /error/i.test(cls)) {
      const text = await this.notification.textContent();
      throw new Error(`Expected success notification but got error: ${text}`);
    }
    await expect(this.notification).toHaveAttribute("class", /success|info/i);
  }

  /** Assert a notification of any kind appeared */
  async expectNotification() {
    await expect(this.notification).toBeVisible({ timeout: UI_TIMEOUT });
  }

  /** Get the current value of the identifier pattern input */
  async getIdentifierPattern(): Promise<string> {
    return (await this.identifierPatternInput.inputValue()) || "";
  }

  /** Get the current value of the name input */
  async getName(): Promise<string> {
    return (await this.nameInput.inputValue()) || "";
  }

  /** Get the current value of the port input */
  async getPort(): Promise<string> {
    return (await this.portInput.inputValue()) || "";
  }
}
