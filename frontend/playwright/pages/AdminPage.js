import { expect } from "@playwright/test";
import LabNumberManagementPage from "./LabNumberManagementPage";
import GlobalMenuConfigPage from "./GlobalMenuConfigPage";

class AdminPage {
  constructor(page) {
    this.page = page;
  }

  // Visit the Admin page
  async visit() {
    await this.page.goto("/administration");
  }

  // Lab number management
  async goToLabNumberManagementPage() {
    // Click on the element using the provided selector
    await this.page.locator("a.cds--side-nav__link[href='#labNumber']").click();
    console.log("working");
    // Verify the URL and content
    await expect(this.page).toHaveURL(/#labNumber/i);
    console.log("working.................");
    const heading = await this.page.getByRole("heading", {
      name: "Lab Number Management",
    });
    await expect(heading).toBeVisible();
    return new LabNumberManagementPage(this.page);
  }

  // Global menu configuration
  async goToGlobalMenuConfigPage() {
    // Click the "Menu Configuration" span to expand the dropdown
    await this.page.getByRole("button", { name: "Menu Configuration" }).click();

    // Click the "Global Menu Configuration" link
    await this.page
      .getByRole("link", { name: "Global Menu Configuration" })
      .click();

    // Verify the URL and content
    await expect(this.page).toHaveURL(/#globalMenuManagement/);

    await expect(
      this.page.locator("text=Global Menu Management").nth(0),
    ).toBeVisible();

    // Return a new instance of GlobalMenuConfigPage
    return new GlobalMenuConfigPage(this.page);
  }
}

export default AdminPage;
