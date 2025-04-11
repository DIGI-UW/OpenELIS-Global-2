//This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import GlobalMenuConfigPage from "./GlobalMenuConfigPage";
import UserManagementPage from "./UserManagementPage";
import ProgramEntryPage from "./ProgramEntryPage";
import ProviderManagementPage from "./ProviderManagementPage";

class AdminPage {
  constructor() {}

  visit() {
    cy.visit("/administration"); //need to confirm this
  }
  //Provider Management
  goToProviderManagementPage() {
    cy.get("[data-cy='providerMgmnt']").should("be.visible");
    cy.get("[data-cy='providerMgmnt']").click();
    cy.url().should("include", "#providerMenu");
    cy.contains("Provider Management").should("be.visible");
    return new ProviderManagementPage();
  }
  //lab number management
  goToLabNumberManagementPage() {
    cy.get("[data-cy='labNumberMgmnt']").should("be.visible");
    cy.get("[data-cy='labNumberMgmnt']").click();
    cy.url().should("include", "#labNumber");
    cy.contains("Lab Number Management").should("be.visible");
    return new LabNumberManagementPage();
  }
  //global menu configuration
  goToGlobalMenuConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    //cy.get("[data-cy='menuConfig']").click();
    cy.get("[data-cy='globalMenuMgmnt']").should("be.visible");
    cy.get("[data-cy='globalMenuMgmnt']").click();
    // Verify the URL and the visibility of the content
    cy.url().should("include", "#globalMenuManagement");
    cy.contains("Global Menu Management").should("be.visible");

    return new GlobalMenuConfigPage();
  }
  //User Management
  goToUserManagementPage() {
    // Click on the "User Management" menu option
    cy.contains("span", "User Management").click();
    // Wait for and click the actual menu button if needed
    cy.get("[data-cy='userManagement']").should("be.visible").click();
    // Verify we are on the correct page
    cy.url().should("include", "#userManagement");
    cy.contains("User Management").should("be.visible");

    return new UserManagementPage();
  }

  goToProgramEntry() {
    cy.get("[data-cy='programEntry']").should("be.visible");
    cy.get("[data-cy='programEntry']").click();

    return new ProgramEntryPage();
  }
}

export default AdminPage;
