import { Page, expect, Locator } from "@playwright/test";

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

  /** Select an item from a Carbon Dropdown by visible text */
  private async selectDropdownItem(dropdown: Locator, text: string) {
    await dropdown.click();
    const item = this.page.getByRole("option", { name: text });
    await item.first().click();
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
    await expect(this.notification).toBeVisible({ timeout: 10000 });
    await expect(this.notification).toHaveAttribute("class", /success|info/i);
  }

  /** Assert a notification of any kind appeared */
  async expectNotification() {
    await expect(this.notification).toBeVisible({ timeout: 10000 });
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
