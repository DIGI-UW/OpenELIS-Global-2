import LoginPage from "./LoginPage";
import PatientEntryPage from "./PatientEntryPage";
import PatientMergePage from "./PatientMergePage";
import OrderEntityPage from "./OrderEntityPage";
import ModifyOrderPage from "./ModifyOrderPage";
import WorkPlan from "./WorkPlan";
import NonConform from "./NonConformPage";
import Result from "./ResultsPage";
import Validation from "./Validation";
import BarcodeConfigPage from "./BarcodeConfigPage";
import BatchOrderEntry from "./BatchOrderEntryPage";
import RoutineReportPage from "./RoutineReportPage";
import StudyReportPage from "./StudyReportPage";
import DashBoardPage from "./DashBoard";
import AdminPage from "./AdminPage";
import HelpPage from "./HelpPage";

class HomePage {
  constructor() {
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      sampleAddNav: "#menu_sample_add_nav",
      sampleMenu: "span#menu_sample",
      batchEntry: "#menu_sample_batch_entry",
      patientMenu: "span#menu_patient",
      patientAddEdit: "#menu_patient_add_or_edit_nav",
      patientMerge: "#menu_patient_merge",
      sampleEditNav: "#menu_sample_edit_nav",
      workplanMenu: "span#menu_workplan",
      workplanTestNav: "#menu_workplan_test_nav",
      workplanPanelNav: "#menu_workplan_panel_nav",
      workplanBenchNav: "#menu_workplan_bench_nav",
      workplanPriorityNav: "#menu_workplan_priority_nav",
      nonConformityDropdown: "span#menu_nonconformity",
      nonConformingReport: "span#menu_non_conforming_report",
      nonConformingView: "span#menu_non_conforming_view",
      nonConformingActions: "span#menu_non_conforming_corrective_actions",
      resultsMenu: "span#menu_results",
      resultsMenuButton: "#menu_results > .cds--side-nav__submenu",
      resultsLogbook: "#menu_results_logbook_nav",
      resultsAccession: "#menu_results_accession_nav",
      resultsPatient: "#menu_results_patient_nav",
      resultsReferred: "#menu_results_referred_nav",
      resultsRange: "#menu_results_range_nav",
      resultsStatus: "#menu_results_status_nav",
      validationMenu: "#menu_resultvalidation",
      routineValidation: "#menu_resultvalidation_routine",
      rangeOrderValidation: "#menu_accession_validation_range",
      accessionValidation: "#menu_accession_validation",
      reportsMenu: "#menu_reports",
      reportsMenuButton: "#menu_reports > .cds--side-nav__submenu",
      reportsRoutine: "#menu_reports_routine",
      reportsStudy: "#menu_reports_study",
      reportsStudyButton: "#menu_reports_study > .cds--side-nav__submenu",
      pathologyNav: "#menu_pathology_nav",
      immunochemMenu: "#menu_immunochem",
      cytologyMenu: "#menu_cytology",
      administrationMenu: "span#menu_administration",
      administrationNav: "#menu_administration_nav",
      helpMenu: "#menu_help",
      minimizeIcon: "#minimizeIcon",
      searchIcon: "#search-Icon",
      searchItem: "#searchItem",
      patientSearch: "#patientSearch",
      notificationIcon: "#notification-Icon",
      userIcon: "#user-Icon",
      userHelp: "#user-Help",
      maximizeIcon: "#maximizeIcon",
      link: "a.cds--link",
    };
  }

  visit() {
    cy.visit("/");
  }

  goToSign() {
    return new LoginPage();
  }

  openNavigationMenu() {
    cy.get(this.selectors.menuButton)
      .should("be.visible")
      .then(($btn) => {
        const menuLabel = ($btn.attr("aria-label") || "").toLowerCase();
        if (menuLabel.includes("open")) {
          cy.wrap($btn).click({ force: true });
        }
      });

    // SHOW mode keeps submenu children hidden unless the menu is pinned.
    cy.get(this.selectors.menuButton)
      .should("be.visible")
      .then(($btn) => {
        const menuLabel = ($btn.attr("aria-label") || "").toLowerCase();
        if (menuLabel.includes("pin")) {
          cy.wrap($btn).click({ force: true });
        }
      })
      .invoke("attr", "aria-label")
      .then((menuLabel) => {
        if (menuLabel) {
          expect(menuLabel.toLowerCase()).to.include("close");
        }
      });

    cy.get(".cds--side-nav", { timeout: 10000 }).should(
      "have.class",
      "cds--side-nav--expanded",
    );
  }

  clickNavigationItem(selector) {
    cy.get(selector)
      .should("be.visible")
      .scrollIntoView()
      .click({ force: true });
  }

  expandResultsMenu() {
    cy.get(this.selectors.resultsMenuButton)
      .should("be.visible")
      .then(($submenuButton) => {
        if ($submenuButton.attr("aria-expanded") !== "true") {
          cy.wrap($submenuButton).click({ force: true });
        }
      });

    cy.get(this.selectors.resultsMenuButton).should(
      "have.attr",
      "aria-expanded",
      "true",
    );
  }

  expandReportsMenu() {
    cy.get(this.selectors.reportsMenuButton)
      .should("be.visible")
      .then(($submenuButton) => {
        if ($submenuButton.attr("aria-expanded") !== "true") {
          cy.wrap($submenuButton).click({ force: true });
        }
      });

    cy.get(this.selectors.reportsMenuButton).should(
      "have.attr",
      "aria-expanded",
      "true",
    );
  }

  closeNavigationMenu() {
    cy.get(this.selectors.menuButton)
      .then(($btn) => {
        const ariaLabel = $btn.attr("aria-label");

        // Only click if the current state indicates the menu is open.
        if (ariaLabel && ariaLabel.toLowerCase().includes("close")) {
          cy.wrap($btn).click();
        }
      })
      .should(($btn) => {
        const ariaLabel = $btn.attr("aria-label");
        if (ariaLabel) {
          expect(ariaLabel.toLowerCase()).not.to.include("close");
        }
      });
  }

  // Order Entry related functions
  goToOrderPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).should("be.visible").click();
    cy.get(this.selectors.sampleAddNav).should("be.visible").click();
    return new OrderEntityPage();
  }

  goToBatchOrderEntry() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).click();
    cy.get(this.selectors.batchEntry).click();
    return new BatchOrderEntry();
  }

  goToBarcode() {
    this.openNavigationMenu();
    cy.get("#menu_sample").click();
    cy.get("[data-cy='menu_sample_print_barcode']").click();
    return new BarcodeConfigPage();
  }

  // Patient Entry related functions
  goToPatientEntry() {
    this.openNavigationMenu();
    cy.get(this.selectors.patientMenu).click();
    cy.get(this.selectors.patientAddEdit).click();
    return new PatientEntryPage();
  }

  // Patient Merge (Admin function)
  goToPatientMerge() {
    this.openNavigationMenu();
    cy.get(this.selectors.patientMenu).click();
    cy.get(this.selectors.patientMerge).should("be.visible").click();
    return new PatientMergePage();
  }

  // Modify Order related functions
  goToModifyOrderPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).should("be.visible").click();
    cy.get(this.selectors.sampleEditNav).should("be.visible").click();
    return new ModifyOrderPage();
  }

  // Work Plan related functions
  goToWorkPlanPlanByTest() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).should("be.visible").click();
    cy.get(this.selectors.workplanTestNav).should("be.visible").click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPanel() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanPanelNav).click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByUnit() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanBenchNav).should("be.visible").click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPriority() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanPriorityNav).should("be.visible").click();
    return new WorkPlan();
  }

  // Non-Conforming related functions
  goToReportNCE() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown).click();
    cy.get(this.selectors.nonConformingReport).should("be.visible").click();
    return new NonConform();
  }

  goToViewNCE() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown).click();
    cy.get(this.selectors.nonConformingView).should("be.visible").click();
    return new NonConform();
  }

  goToCorrectiveActions() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown).click();
    cy.get(this.selectors.nonConformingActions).should("be.visible").click();
    return new NonConform();
  }

  // Results related functions
  goToResultsByUnit() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsLogbook);
    return new Result();
  }

  goToResultsByOrder() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsAccession);
    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsPatient);
    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsReferred);
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsRange);
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    this.expandResultsMenu();
    this.clickNavigationItem(this.selectors.resultsStatus);
    return new Result();
  }

  // Validation related functions
  goToValidationByRoutine() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.routineValidation).click();
    return new Validation();
  }

  goToValidationByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.accessionValidation).click();
    return new Validation();
  }

  goToValidationByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.rangeOrderValidation).click();
    return new Validation();
  }

  // Reports related functions
  goToRoutineReports() {
    this.openNavigationMenu();
    this.expandReportsMenu();
    cy.get(this.selectors.reportsRoutine).should("be.visible").click();
    return new RoutineReportPage();
  }

  goToStudyReports() {
    this.openNavigationMenu();
    this.expandReportsMenu();
    cy.get(this.selectors.reportsStudyButton)
      .should("be.visible")
      .then(($studySubmenu) => {
        if ($studySubmenu.attr("aria-expanded") !== "true") {
          cy.wrap($studySubmenu).click({ force: true });
        }
      });
    return new StudyReportPage();
  }

  goToReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click();
  }

  // Dashboard related functions
  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.pathologyNav).should("be.visible").click();
    return new DashBoardPage();
  }

  goToImmunoChemistryDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.immunochemMenu).click();
    return new DashBoardPage();
  }

  goToCytologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.cytologyMenu).click();
    return new DashBoardPage();
  }

  // Admin related functions
  goToAdminPageProgram() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationMenu).click();
    this.closeNavigationMenu();
    return new AdminPage();
  }

  goToAdminPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationNav).click();
    this.closeNavigationMenu();
    return new AdminPage();
  }

  goToHelp() {
    this.openNavigationMenu();
    cy.get(this.selectors.helpMenu).click();
    return new HelpPage();
  }

  // UI interaction functions
  afterAll() {
    cy.get(this.selectors.minimizeIcon).should("be.visible").click();
  }

  searchBar() {
    cy.get(this.selectors.searchIcon).click();
    cy.get(this.selectors.searchItem).type("Smith");
    cy.get(this.selectors.patientSearch).click();
    cy.get(this.selectors.searchIcon).click();
  }

  clickNotifications() {
    cy.get(this.selectors.notificationIcon).click();
    cy.get(this.selectors.notificationIcon).click();
  }

  clickUserIcon() {
    cy.get(this.selectors.userIcon).click();
    cy.get(this.selectors.userIcon).click();
  }

  clickHelpIcon() {
    cy.get(this.selectors.userHelp).click();
    cy.get(this.selectors.userHelp).click();
  }

  selectInProgress() {
    cy.get(this.selectors.maximizeIcon).click();
  }

  selectReadyforValidation() {
    cy.contains(this.selectors.link, "Ready For Validation").click();
  }

  selectOrdersCompletedToday() {
    cy.contains(this.selectors.link, "Orders Completed Today").click();
  }

  selectPartiallyCompletedToday() {
    cy.contains(this.selectors.link, "Partially Completed Today").click();
  }

  selectOrdersEnteredByUsers() {
    cy.contains(this.selectors.link, "Orders Entered By Users").click();
  }

  selectOrdersRejected() {
    cy.contains(this.selectors.link, "Orders Rejected").click();
  }

  selectUnPrintedResults() {
    cy.contains(this.selectors.link, "UnPrinted Results").click();
  }

  selectElectronicOrders() {
    cy.contains(this.selectors.link, "Electronic Orders").click();
  }

  selectAverageTurnAroundTime() {
    cy.contains(this.selectors.link, "Average Turn Around time").click();
  }

  selectDelayedTurnAround() {
    cy.contains(this.selectors.link, "Delayed Turn Around").click();
  }
}

export default HomePage;
