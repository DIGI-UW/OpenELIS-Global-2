//This handles all pages of the admin
import BillingMenuManagementPage from "./BillingMenuManagementPage";
import GlobalMenuConfigPage from "./GlobalMenuConfigPage";
import LabNumberManagementPage from "./LabNumberManagementPage";
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

  goToProgramEntry() {
    cy.get("[data-cy='programEntry']").should("be.visible");
    cy.get("[data-cy='programEntry']").click();

    return new ProgramEntryPage();
  }

  goToBillingMenuManagementPage() {
    // cy.get("[data-testid='menuConfig']").click();
    cy.contains("span", "Menu Configuration").click();

    cy.get("[data-cy='billingMenuMgmnt']").should("be.visible");
    cy.get("[data-cy='billingMenuMgmnt']").click();
    cy.url().should("include", "#billingMenuManagement");
    cy.contains("Billing Menu Management").should("be.visible");
    return new BillingMenuManagementPage();
  }
}

export default AdminPage;
