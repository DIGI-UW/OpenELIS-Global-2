//This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import MenuConfigPage from "./MenuConfigPage";
import BarcodeConfigPage from "./BarcodeConfigPage";
import ProgramEntryPage from "./ProgramEntryPage";
import ProviderManagementPage from "./ProviderManagementPage";
import OrganizationManagementPage from "./OrganizationManagementPage";
import ReflexTestsConfigPage from "./ReflexTestsConfigPage";
import DictionaryMenuPage from "./DictionaryMenu";

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

  goToOrganizationManagement() {
    cy.get("[data-cy='orgMgmnt']").should("be.visible");
    cy.get("[data-cy='orgMgmnt']").click();
    cy.url().should("include", "#organizationManagement");
    cy.contains("Organization Management").should("be.visible");

    return new OrganizationManagementPage();
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

    return new MenuConfigPage();
  }

  goToNonConformConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='nonConformMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToPatientConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='patientMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToStudyConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='studyMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToBillingConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='billingMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToBarcodeConfigPage() {
    cy.get("[data-cy='barcodeConfig']").should("be.visible").click();

    return new BarcodeConfigPage();
  }

  goToProgramEntry() {
    cy.get("[data-cy='programEntry']").should("be.visible");
    cy.get("[data-cy='programEntry']").click();

    return new ProgramEntryPage();
  }

  goToDictionaryMenuPage() {
    cy.get("[data-cy='dictMenu']").should("be.visible").click();
    return new DictionaryMenuPage();
  }

  goToReflexTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration").click();
    cy.get("[data-cy='reflex']").click();
    return new ReflexTestsConfigPage();
  }

  goToCalculatedValueTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration").click();
    cy.get("[data-cy='calculatedValue']").click();
    return new ReflexTestsConfigPage();
  }
}
export default AdminPage;
