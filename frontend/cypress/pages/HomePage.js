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
      batchEntry: "#menu_sample_batch_entry_nav", // leaf item
      patientMenu: "span#menu_patient",
      patientAddEdit: "#menu_patient_add_or_edit_nav",
      patientMerge: "#menu_patient_merge_nav", // leaf item
      sampleEditNav: "#menu_sample_edit_nav",
      workplanMenu: "span#menu_workplan",
      workplanTestNav: "#menu_workplan_test_nav",
      workplanPanelNav: "#menu_workplan_panel_nav",
      workplanBenchNav: "#menu_workplan_bench_nav",
      workplanPriorityNav: "#menu_workplan_priority_nav",
      nonConformityDropdown: "span#menu_nonconformity", // parent menu - no _nav suffix
      nonConformingReport: "#menu_non_conforming_report_nav", // leaf item
      nonConformingView: "#menu_non_conforming_view_nav", // leaf item
      nonConformingActions: "#menu_non_conforming_corrective_actions_nav", // leaf item
      resultsMenu: "span#menu_results",
      resultsLogbook: "#menu_results_logbook_nav",
      resultsAccession: "#menu_results_accession_nav",
      resultsPatient: "#menu_results_patient_nav", // leaf item
      resultsReferred: "#menu_results_referred_nav",
      resultsRange: "#menu_results_range_nav",
      resultsStatus: "#menu_results_status_nav",
      validationMenu: "#menu_resultvalidation",
      routineValidation: "#menu_resultvalidation_routine_nav", // leaf item
      rangeOrderValidation: "#menu_accession_validation_range_nav", // leaf item
      accessionValidation: "#menu_accession_validation_nav", // leaf item
      reportsMenu: "#menu_reports",
      reportsRoutine: "#menu_reports_routine",
      reportsStudy: "[data-cy='sidenav-button-menu_reports_study']",
      pathologyNav: "#menu_pathology_nav",
      immunochemMenu: "#menu_immunochem_nav",
      cytologyMenu: "#menu_cytology_nav",
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

  /**
   * Idempotent expand: only clicks the Carbon SideNavMenu toggle if it is
   * currently collapsed (aria-expanded !== "true"). This avoids the
   * toggle-trap where a second click collapses an already-open menu.
   */
  ensureSidenavMenuExpanded(menuId) {
    cy.get(menuId, { timeout: 15000 })
      .find("button[aria-expanded]")
      .first()
      .then(($btn) => {
        if ($btn.attr("aria-expanded") !== "true") {
          cy.wrap($btn).click();
        }
      });
    cy.get(menuId).find('button[aria-expanded="true"]').first().should("exist");
  }

  openNavigationMenu() {
    // Sidenav uses a 3-step toggle: CLOSE -> SHOW -> LOCK -> CLOSE
    // aria-label tells us the current state:
    //   "Open menu"  = CLOSE (menu hidden)
    //   "Pin menu"   = SHOW  (menu visible, overlay)
    //   "Close menu" = LOCK  (menu visible, pinned)
    cy.get(this.selectors.menuButton).then(($btn) => {
      const label = $btn.attr("aria-label");
      if (label === "Open menu") {
        // CLOSE -> click once -> SHOW (opens menu)
        cy.get(this.selectors.menuButton).click();
      }
      // SHOW or LOCK: menu is already open, no click needed
    });
  }

  closeNavigationMenu() {
    // Pin the sidenav (LOCK state) instead of fully closing it.
    // LOCK mode pushes content aside so both sidenav items AND page
    // content remain accessible — unlike SHOW mode which overlays content.
    cy.get(this.selectors.menuButton).then(($btn) => {
      const label = $btn.attr("aria-label");
      if (label === "Pin menu") {
        // SHOW -> click once -> LOCK (pin; content pushed aside)
        cy.get(this.selectors.menuButton).click();
      }
      // "Close menu" (LOCK): already pinned, no action needed
      // "Open menu" (CLOSE): already closed, no action needed
    });
  }

  // Order Entry related functions
  goToOrderPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).should("be.visible").click();
    cy.get(this.selectors.sampleAddNav).should("be.visible").click();
    this.closeNavigationMenu();
    return new OrderEntityPage();
  }

  goToBatchOrderEntry() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).should("be.visible").click();
    cy.get(this.selectors.batchEntry).should("be.visible").click();
    this.closeNavigationMenu();
    return new BatchOrderEntry();
  }

  goToBarcode() {
    this.openNavigationMenu();
    cy.get("#menu_sample").click();
    cy.get("[data-cy='menu_sample_print_barcode']").click();
    this.closeNavigationMenu();
    return new BarcodeConfigPage();
  }

  // Patient Entry related functions
  goToPatientEntry() {
    this.openNavigationMenu();
    cy.get(this.selectors.patientMenu).click();
    cy.get(this.selectors.patientAddEdit).click();
    this.closeNavigationMenu();
    return new PatientEntryPage();
  }

  // Patient Merge (Admin function)
  goToPatientMerge() {
    this.openNavigationMenu();
    cy.get(this.selectors.patientMenu).click();
    cy.get(this.selectors.patientMerge).should("be.visible").click();
    this.closeNavigationMenu();
    return new PatientMergePage();
  }

  // Modify Order related functions
  goToModifyOrderPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.sampleMenu).should("be.visible").click();
    cy.get(this.selectors.sampleEditNav).should("be.visible").click();
    this.closeNavigationMenu();
    return new ModifyOrderPage();
  }

  // Work Plan related functions
  goToWorkPlanPlanByTest() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).should("be.visible").click();
    cy.get(this.selectors.workplanTestNav).should("be.visible").click();
    this.closeNavigationMenu();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPanel() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanPanelNav).click();
    this.closeNavigationMenu();
    return new WorkPlan();
  }

  goToWorkPlanPlanByUnit() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanBenchNav).should("be.visible").click();
    this.closeNavigationMenu();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPriority() {
    this.openNavigationMenu();
    cy.get(this.selectors.workplanMenu).click();
    cy.get(this.selectors.workplanPriorityNav).should("be.visible").click();
    this.closeNavigationMenu();
    return new WorkPlan();
  }

  // Non-Conforming related functions
  goToReportNCE() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.nonConformingReport)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    // Close sidenav to prevent overlay blocking page content
    this.closeNavigationMenu();
    return new NonConform();
  }

  goToViewNCE() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.nonConformingView)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    // Close sidenav to prevent overlay blocking page content
    this.closeNavigationMenu();
    return new NonConform();
  }

  goToCorrectiveActions() {
    this.openNavigationMenu();
    cy.get(this.selectors.nonConformityDropdown)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.nonConformingActions)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    // Close sidenav to prevent overlay blocking page content
    this.closeNavigationMenu();
    return new NonConform();
  }

  // Results related functions
  goToResultsByUnit() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsLogbook)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  goToResultsByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsAccession)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsPatient)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsReferred)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsRange)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.resultsStatus)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new Result();
  }

  // Validation related functions
  goToValidationByRoutine() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.routineValidation).click();
    this.closeNavigationMenu();
    return new Validation();
  }

  goToValidationByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.accessionValidation).click();
    this.closeNavigationMenu();
    return new Validation();
  }

  goToValidationByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.validationMenu).click();
    cy.get(this.selectors.rangeOrderValidation).click();
    this.closeNavigationMenu();
    return new Validation();
  }

  // Reports related functions
  goToRoutineReports() {
    this.openNavigationMenu();
    this.ensureSidenavMenuExpanded(this.selectors.reportsMenu);
    this.ensureSidenavMenuExpanded(this.selectors.reportsRoutine);
    this.closeNavigationMenu();
    return new RoutineReportPage();
  }

  goToStudyReports() {
    this.openNavigationMenu();
    this.ensureSidenavMenuExpanded(this.selectors.reportsMenu);
    this.ensureSidenavMenuExpanded("#menu_reports_study");
    this.closeNavigationMenu();
    return new StudyReportPage();
  }

  goToReports() {
    this.openNavigationMenu();
    this.ensureSidenavMenuExpanded(this.selectors.reportsMenu);
    this.closeNavigationMenu();
  }

  // Dashboard related functions
  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.pathologyNav)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new DashBoardPage();
  }

  goToImmunoChemistryDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.immunochemMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new DashBoardPage();
  }

  goToCytologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.cytologyMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
    return new DashBoardPage();
  }

  // Admin related functions
  goToAdminPageProgram() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationMenu).click();
    // Ensure we land on Admin tile view (/MasterListsPage or /admin); app has no /administration route
    cy.location("pathname").then((pathname) => {
      if (!/^\/(MasterListsPage|admin)(\/|$|#)/.test(pathname)) {
        cy.visit("/MasterListsPage");
      }
    });
    // Note: Do not call closeNavigationMenu() here - this method has fallback cy.visit()
    // logic that may change the page state, making sidenav toggle behavior unpredictable
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
    cy.get(this.selectors.helpMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    this.closeNavigationMenu();
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
