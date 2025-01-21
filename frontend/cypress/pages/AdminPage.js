//This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import GlobalMenuConfigPage from "./GlobalMenuConfigPage";

class AdminPage {
  constructor() {}

  visit() {
    cy.visit("/administration"); //need to confirm this
  }

  //lab number management
  goToLabNumberManagementPage() {
    // Click on the element using the provided selector
    cy.get("a.cds--side-nav__link[href='#labNumber']")
      .should("be.visible")
      .click();

    cy.url().should("include", "#labNumber");
    cy.contains("Lab Number Management").should("be.visible");

    return new LabNumberManagementPage();
  }

  //global menu configuration
  goToGlobalMenuConfigPage() {
    // Expand the dropdown by clicking the button with the expanded state
    cy.contains("span", "Menu Configuration").click();
    cy.get("ul.cds--side-nav__menu").should("be.visible"); // Ensure the dropdown menu is visible
    // Click the link for "Global Menu Configuration"
    cy.get('a.cds--side-nav__link[href="#globalMenuManagement"]').click(); // Click the "Global Menu Configuration" link

    // Verify the URL and the visibility of the content
    cy.url().should("include", "#globalMenuManagement");
    cy.contains("Global Menu Management").should("be.visible");

    return new GlobalMenuConfigPage();
  }
}

export default AdminPage;
