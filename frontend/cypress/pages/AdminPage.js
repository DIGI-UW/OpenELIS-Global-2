//This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";

class AdminPage {
  constructor() {}

  visit() {
    cy.visit("/administration"); //need to confirm this
  }
  //this page is also included in the homepage
  goToAdminPage() {
    this.openNavigationMenu();
    cy.get("#menu_administration").click();
    cy.get("#menu_administration_nav").click();
    return new AdminPage();
  }

  //lab number management
  goToLabNumberManagementPage() {
    // Click on the element using the provided selector
    cy.get("a.cds--side-nav__link[href='#labNumber']")
      .should("be.visible") // Ensure the element is visible
      .click(); // Click to navigate to the page

    // Verify the URL or some unique identifier of the target page
    cy.url().should("include", "#labNumber"); // Validate URL fragment
    cy.contains("Lab Number Management").should("be.visible"); // Confirm presence of the page content

    return new LabNumberManagementPage(); // Return the page object
  }
}

export default AdminPage;
