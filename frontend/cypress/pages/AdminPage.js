// This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import MenuConfigPage from "./MenuConfigPage";
import UserManagementPage from "./UserManagementPage";
import ReflexTestsConfigPage from "./ReflexTestsConfigPage";
import GeneralConfigurationsPage from "./GeneralConfigurationsPage";
import NotifyUserPage from "./NotifyUserPage";
import ResultReportingConfigurationPage from "./ResultReportingConfiguration";
import BatchTestReassignmentandCancelationPage from "./BatchTestReassignmentandCancelation";

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
    };
  }

  visit() {
    cy.visit("/MasterListsPage");
  }

  ensureAdminShell() {
    cy.location("pathname").then((pathname) => {
      if (!/^\/(MasterListsPage|admin)(\/|$)/.test(pathname)) {
        cy.visit("/MasterListsPage");
      }
    });
  }

  goToLabNumberManagementPage() {
    cy.get(this.selectors.labNumberManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.url().should("include", "/labNumber");
    cy.contains("Lab Number Management").should("be.visible");
    return new LabNumberManagementPage();
  }

  goToGlobalMenuConfigPage() {
    this.ensureAdminShell();
    cy.contains(this.selectors.span, "Menu Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.globalMenuManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.url().should("include", "/globalMenuManagement");
    cy.contains("Global Menu Management").should("be.visible");

    return new MenuConfigPage();
  }

  goToNonConformConfigPage() {
    this.ensureAdminShell();
    cy.contains("span", "Menu Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='nonConformMenuMgmnt']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new MenuConfigPage();
  }

  goToPatientConfigPage() {
    this.ensureAdminShell();
    cy.contains("span", "Menu Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='patientMenuMgmnt']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new MenuConfigPage();
  }

  goToStudyConfigPage() {
    this.ensureAdminShell();
    cy.contains("span", "Menu Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='studyMenuMgmnt']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new MenuConfigPage();
  }

  goToBillingConfigPage() {
    this.ensureAdminShell();
    cy.contains("span", "Menu Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='billingMenuMgmnt']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new MenuConfigPage();
  }

  goToUserManagementPage() {
    cy.get(this.selectors.userManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new UserManagementPage();
  }

  goToReflexTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='reflex']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new ReflexTestsConfigPage();
  }

  goToCalculatedValueTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='calculatedValue']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new ReflexTestsConfigPage();
  }

  goToNonConformityConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='nonConformConfig']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToMenuStatementConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='menuStatementConfig']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToWorkPlanConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='workPlanConfig']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToSiteInformationConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='siteInfoMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToResultEntityConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='resultConfigMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToPatientEntityConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='patientConfigMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToPrintedReportConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='printedReportsConfigMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToOrderEntityConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='sampleEntryConfigMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToValidationConfig() {
    cy.contains("span", "General Configurations")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get("[data-cy='validationConfigMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });

    return new GeneralConfigurationsPage();
  }

  goToNotifyUserPage() {
    cy.get(this.selectors.notifyUser)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new NotifyUserPage();
  }

  goToResultReportingConfigurationPage() {
    cy.get(this.selectors.resultReportingConfig)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new ResultReportingConfigurationPage();
  }

  goToBatchTestReassignmentandCanelationPage() {
    cy.get(this.selectors.batchTest)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new BatchTestReassignmentandCancelationPage();
  }
}

export default AdminPage;
