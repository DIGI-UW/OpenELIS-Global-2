//This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import GlobalMenuConfigPage from "./GlobalMenuConfigPage";
import ProgramEntryPage from "./ProgramEntryPage";
import ProviderManagementPage from "./ProviderManagementPage";
import NonComformMenuPage from "./NonComformMenuPage";
import PatientMenuConfigPage from "./PatitentMenuConfigPage";

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

  goToNonComformMenuConfigPage() {
    cy.contains("button", "Menu Configuration").click({ force: true });
    cy.get(".cds--side-nav__menu-item")
      .should("be.visible")
      .contains("Non-Conform Menu Configuration")
      .click({ force: true });

    // Verify the URL and the visibility of the content
    cy.url().should("include", "#nonConformityMenuManagement");
    cy.contains("Non-Conformity Menu Management").should("be.visible");

    return new NonComformMenuPage();
  }

  goToPatientMenuConfigPage() {
    cy.contains("button", "Menu Configuration").click({ force: true });
    cy.get(".cds--side-nav__menu-item")
      .should("be.visible")
      .contains("Patient Menu Configuration")
      .click({ force: true });

    // Verify the URL and the visibility of the content
    cy.url().should("include", "#patientMenuManagement");
    cy.contains("Patient Menu Management").should("be.visible");

    return new PatientMenuConfigPage();
  }

  goToProgramEntry() {
    cy.get("[data-cy='programEntry']").should("be.visible");
    cy.get("[data-cy='programEntry']").click();

    return new ProgramEntryPage();
  }
}

export default AdminPage;
