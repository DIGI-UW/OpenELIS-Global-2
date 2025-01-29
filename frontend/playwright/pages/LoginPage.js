import HomePage from "./HomePage";
import TestProperties from "../common/TestProperties";
class LoginPage {
  constructor(page) {
    this.page = page; // Store the page object
    this.testProperties = new TestProperties();
  }

  // Navigate to the login page
  async visit() {
    await this.page.goto("/login");
  }

  // Locate the username field
  getUsernameElement() {
    return this.page.getByRole("textbox", { name: "Username" });
  }

  // Locate the password field
  getPasswordElement() {
    return this.page.getByRole("textbox", { name: "Password" });
  }

  // Enter username into the username field
  async enterUsername(value = this.testProperties.getUsername()) {
    const field = this.getUsernameElement();
    await field.fill(value);
  }

  // Enter password into the password field
  async enterPassword(value = this.testProperties.getPassword()) {
    const field = this.getPasswordElement();
    await field.fill(value);
  }

  // Click the sign-in button
  async signIn() {
    const button = this.page.locator("[type='submit']");
    await button.click();
  }
  // Navigate to the home page
  async goToHomePage() {
    await this.page.waitForTimeout(1000);
    await this.visit();
    await this.enterUsername();
    await this.enterPassword();
    await this.signIn();
    return new HomePage(this.page);
  }
}

export default LoginPage;
