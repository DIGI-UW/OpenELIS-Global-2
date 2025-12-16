// This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import MenuConfigPage from "./MenuConfigPage";
import BarcodeConfigPage from "./BarcodeConfigPage";
import ProgramEntryPage from "./ProgramEntryPage";
import ProviderManagementPage from "./ProviderManagementPage";
import OrganizationManagementPage from "./OrganizationManagementPage";
import UserManagementPage from "./UserManagementPage";
import ReflexTestsConfigPage from "./ReflexTestsConfigPage";
import DictionaryMenuPage from "./DictionaryMenu";
import GeneralConfigurationsPage from "./GeneralConfigurationsPage";
import NotifyUserPage from "./NotifyUserPage";
import ResultReportingConfigurationPage from "./ResultReportingConfiguration";
import BatchTestReassignmentandCancelationPage from "./BatchTestReassignmentandCancelation";
import TestManagementPage from "./TestManagementPage";

class AdminPage {
  constructor() {
    this.selectors = {
      providerManagement: "[data-cy='providerMgmnt']",
      organizationManagement: "[data-cy='orgMgmnt']",
      labNumberManagement: "[data-cy='labNumberMgmnt']",
      globalMenuManagement: "[data-cy='globalMenuMgmnt']",
      barcodeConfig: "[data-cy='barcodeConfig']",
      programEntry: "[data-cy='programEntry']",
      userManagement: "[data-cy='userMgmnt']",
      notifyUser: "[data-cy='notifyUser']",
      resultReportingConfig: "[data-cy='resultReportingConfiguration']",
      batchTest: "[data-cy='batchTestReassignment']",
      span: "span",
      testManagement: "[data-cy='testManagementConfigMenu']",
    };
  }

  visit() {
    cy.visit("/administration");
  }

  goToProviderManagementPage() {
    cy.get(this.selectors.providerManagement).should("be.visible").click();
    cy.url().should("include", "#providerMenu");
    cy.contains("Provider Management").should("be.visible");
    return new ProviderManagementPage();
  }

  goToOrganizationManagement() {
    cy.get(this.selectors.organizationManagement).should("be.visible").click();
    cy.url().should("include", "#organizationManagement");
    cy.contains("Organization Management").should("be.visible");
    return new OrganizationManagementPage();
  }

  goToLabNumberManagementPage() {
    cy.get(this.selectors.labNumberManagement).should("be.visible").click();
    cy.url().should("include", "#labNumber");
    cy.contains("Lab Number Management").should("be.visible");
    return new LabNumberManagementPage();
  }

  goToGlobalMenuConfigPage() {
    cy.contains(this.selectors.span, "Menu Configuration").click();
    cy.get(this.selectors.globalMenuManagement).should("be.visible").click();
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
    cy.get(this.selectors.barcodeConfig).should("be.visible").click();
    return new BarcodeConfigPage();
  }

  goToProgramEntry() {
    cy.get(this.selectors.programEntry).should("be.visible").click();
    return new ProgramEntryPage();
  }

  goToDictionaryMenuPage() {
    cy.get("[data-cy='dictMenu']").should("be.visible").click();
    return new DictionaryMenuPage();
  }

  goToUserManagementPage() {
    cy.get(this.selectors.userManagement).click();
    return new UserManagementPage();
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

  goToNonConformityConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='nonConformConfig']").click();

    return new GeneralConfigurationsPage();
  }

  goToMenuStatementConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='menuStatementConfig']").click();

    return new GeneralConfigurationsPage();
  }

  goToWorkPlanConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='workPlanConfig']").click();

    return new GeneralConfigurationsPage();
  }

  goToSiteInformationConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='siteInfoMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToResultEntityConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='resultConfigMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToPatientEntityConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='patientConfigMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToPrintedReportConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='printedReportsConfigMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToOrderEntityConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='sampleEntryConfigMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToValidationConfig() {
    cy.contains("span", "General Configurations").click();
    cy.get("[data-cy='validationConfigMenu']").click();

    return new GeneralConfigurationsPage();
  }

  goToNotifyUserPage() {
    cy.get(this.selectors.notifyUser).should("be.visible").click();
    return new NotifyUserPage();
  }

  goToResultReportingConfigurationPage() {
    cy.get(this.selectors.resultReportingConfig).should("be.visible").click();
    return new ResultReportingConfigurationPage();
  }

  goToBatchTestReassignmentandCanelationPage() {
    cy.get(this.selectors.batchTest).should("be.visible").click();
    return new BatchTestReassignmentandCancelationPage();
  }

  goToTestManagementPage() {
    cy.get(this.selectors.testManagement).should("be.visible").click();
    return new TestManagementPage();
  }
}

export default AdminPage;
