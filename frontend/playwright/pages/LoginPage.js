import TestProperties from "../common/TestProperties";
import { expect } from "@playwright/test";
class LoginPage {
  constructor(page) {
    this.page = page; // Store the page object
    this.testProperties = new TestProperties();
  }

  // Navigate to the login page
  async visit() {
    await this.page.goto("/login"); // Navigate to the login page (replace with your login page URL);
    return this; // Return this for method chaining
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
  async enterUsername(value) {
    const field = this.getUsernameElement();
    await field.fill(this.testProperties.getUsername());
    return this;
  }

  // Enter password into the password field
  async enterPassword(value) {
    const field = this.getPasswordElement();
    await field.fill(this.testProperties.getPassword());
    return this;
  }

  // Click the sign-in button
  async signIn() {
    const button = this.page.locator("[type='submit']");
    await button.click();
    await this.page.waitForNavigation();
  }
  // Navigate to the home page after login
  async goToHomePage() {
    await this.page.waitForTimeout(1000);
    const homePageUrl = "/";
    await this.page.goto(homePageUrl);
    return this;
  }

  // Perform the full login flow
  async login(username, password) {
    await this.visit(); // Navigate to the login page
    await this.enterUsername(username); // Enter username
    await this.enterPassword(password); // Enter password
    await this.signIn(); // Click the sign-in button
    return this.goToHomePage(); // Navigate to the home page and return the current instance
  }
}

export default LoginPage;
