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
    cy.visit("/MasterListsPage");
  }

  goToProviderManagementPage() {
    cy.get(this.selectors.providerManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.url().should("include", "/providerMenu");
    cy.contains("Provider Management").should("be.visible");
    return new ProviderManagementPage();
  }

  goToOrganizationManagement() {
    // Ensure we're on Admin tile view (not a nested route); app uses /MasterListsPage or /admin
    cy.location("pathname").then((pathname) => {
      if (!/^\/(MasterListsPage|admin)(\/|$|#)/.test(pathname)) {
        cy.visit("/MasterListsPage");
      }
    });
    cy.get(this.selectors.organizationManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.url().should("include", "/organizationManagement");
    cy.contains("Organization Management").should("be.visible");
    return new OrganizationManagementPage();
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

  goToBarcodeConfigPage() {
    cy.get(this.selectors.barcodeConfig)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new BarcodeConfigPage();
  }

  goToProgramEntry() {
    cy.get(this.selectors.programEntry)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new ProgramEntryPage();
  }

  goToDictionaryMenuPage() {
    cy.get("[data-cy='dictMenu']")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new DictionaryMenuPage();
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

  goToTestManagementPage() {
    cy.get(this.selectors.testManagement)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    return new TestManagementPage();
  }
}

export default AdminPage;
