import LoginPage from "./LoginPage";
import PatientEntryPage from "./PatientEntryPage";
import OrderEntityPage from "./OrderEntityPage";
import ModifyOrderPage from "./ModifyOrderPage";
import WorkPlan from "./WorkPlan";
import NonConform from "./NonConformPage";
import Result from "./ResultsPage";
import Validation from "./Validation";
import BatchOrderEntry from "./BatchOrderEntryPage";
import RoutineReportPage from "./RoutineReportPage";
import StudyReportPage from "./StudyReportPage";
import DashBoardPage from "./DashBoard";
import AdminPage from "./AdminPage";

class HomePage {
  constructor() {
    // Centralized selectors
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      sampleDropdown: "#menu_sample_dropdown",
      patientDropdown: "#menu_patient_dropdown",
      workplanDropdown: "#menu_workplan_dropdown",
      nonconformityDropdown: "#menu_nonconformity_dropdown",
      resultsMenu: "#menu_results_dropdown",
      resultValidationMenu: "#menu_resultvalidation",
      reportsMenu: "#menu_reports_dropdown",
      pathologyMenu: "#menu_pathology",
      immunochemMenu: "#menu_immunochem",
      cytologyMenu: "#menu_cytology",
      administrationMenu: "#menu_administration",
      minimizeIcon: "#minimizeIcon",
      searchIcon: "#search-Icon",
      searchItem: "#searchItem",
      patientSearch: "#patientSearch",
      notificationIcon: "#notification-Icon",
      userIcon: "#user-Icon",
      helpIcon: "#user-Help",
      maximizeIcon: "#maximizeIcon",
      sampleAddNav: "#menu_sample_add_nav",
      sampleBatchEntryNav: "#menu_sample_batch_entry_nav",
      sampleEditNav: "#menu_sample_edit_nav",
      patientAddOrEditNav: "#menu_patient_add_or_edit_nav",
      workplanTestNav: "#menu_workplan_test_nav",
      workplanPanelNav: "#menu_workplan_panel_nav",
      workplanBenchNav: "#menu_workplan_bench_nav",
      workplanPriorityNav: "#menu_workplan_priority_nav",
      nonConformingReportNav: "#menu_non_conforming_report_nav",
      nonConformingViewNav: "#menu_non_conforming_view_nav",
      nonConformingCorrectiveActionsNav:
        "#menu_non_conforming_corrective_actions_nav",
      resultsLogbook: "#menu_results_logbook_nav",
      resultsAccession: "#menu_results_accession_nav",
      resultsPatient: "#menu_results_patient_nav",
      resultsReferred: "#menu_results_referred_nav",
      resultsRange: "#menu_results_range_nav",
      resultsStatus: "#menu_results_status_nav",
      resultValidationRoutine: "#menu_resultvalidation_routine",
      accessionValidation: "#menu_accession_validation",
      accessionValidationRange: "#menu_accession_validation_range",
      reportsRoutineNav: "[data-cy='sidenav-button-menu_reports_routine']",
      reportsStudyNav: "[data-cy='sidenav-button-menu_reports_study']",
    };
  }

  // Visit the home page
  visit() {
    cy.visit("/");
  }

  // Navigate to the login page
  goToSign() {
    return new LoginPage();
  }

  // Open the navigation menu
  openNavigationMenu() {
    cy.get(this.selectors.menuButton, { timeout: 30000 }).click();
  }

  // Click a dropdown item
  clickDropdownItem(dropdownSelector, itemSelector) {
    this.openNavigationMenu();
    cy.get(dropdownSelector).click();
    cy.get(itemSelector).click();
  }

  // Navigate to the Order Entry page
  goToOrderPage() {
    this.clickDropdownItem(
      this.selectors.sampleDropdown,
      this.selectors.sampleAddNav,
    );
    return new OrderEntityPage();
  }

  // Navigate to the Batch Order Entry page
  goToBatchOrderEntry() {
    this.clickDropdownItem(
      this.selectors.sampleDropdown,
      this.selectors.sampleBatchEntryNav,
    );
    return new BatchOrderEntry();
  }

  // Navigate to the Patient Entry page
  goToPatientEntry() {
    this.clickDropdownItem(
      this.selectors.patientDropdown,
      this.selectors.patientAddOrEditNav,
    );
    return new PatientEntryPage();
  }

  // Navigate to the Modify Order page
  goToModifyOrderPage() {
    this.clickDropdownItem(
      this.selectors.sampleDropdown,
      this.selectors.sampleEditNav,
    );
    return new ModifyOrderPage();
  }

  // Navigate to the Work Plan by Test page
  goToWorkPlanPlanByTest() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      this.selectors.workplanTestNav,
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Panel page
  goToWorkPlanPlanByPanel() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      this.selectors.workplanPanelNav,
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Unit page
  goToWorkPlanPlanByUnit() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      this.selectors.workplanBenchNav,
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Priority page
  goToWorkPlanPlanByPriority() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      this.selectors.workplanPriorityNav,
    );
    return new WorkPlan();
  }

  // Navigate to the Non-Conforming Report page
  goToReportNCE() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      this.selectors.nonConformingReportNav,
    );
    return new NonConform();
  }

  // Navigate to the View Non-Conforming Events page
  goToViewNCE() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      this.selectors.nonConformingViewNav,
    );
    return new NonConform();
  }

  // Navigate to the Corrective Actions page
  goToCorrectiveActions() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      this.selectors.nonConformingCorrectiveActionsNav,
    );
    return new NonConform();
  }

  // Navigate to the Results by Unit page
  goToResultsByUnit() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsLogbook).click({ force: true });
    return new Result();
  }

  // Navigate to the Results by Order page
  goToResultsByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsAccession).click({ force: true });
    return new Result();
  }

  // Navigate to the Results by Patient page
  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsPatient).click({ force: true });
    return new Result();
  }

  // Navigate to the Results for Referred Out page
  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsReferred).click({ force: true });
    return new Result();
  }

  // Navigate to the Results by Range Order page
  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsRange).click({ force: true });
    return new Result();
  }

  // Navigate to the Results by Test and Status page
  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsStatus).click({ force: true });

    return new Result();
  }

  // Navigate to the Validation by Routine page
  goToValidationByRoutine() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      this.selectors.resultValidationRoutine,
    );
    return new Validation();
  }

  // Navigate to the Validation by Order page
  goToValidationByOrder() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      this.selectors.accessionValidation,
    );
    return new Validation();
  }

  // Navigate to the Validation by Range Order page
  goToValidationByRangeOrder() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      this.selectors.accessionValidationRange,
    );
    return new Validation();
  }

  // Navigate to the Routine Reports page
  goToRoutineReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click();
    cy.get(this.selectors.reportsRoutineNav).click();
    return new RoutineReportPage();
  }

  // Navigate to the Study Reports page
  goToStudyReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click({ force: true });
    cy.get(this.selectors.reportsStudyNav).click({ force: true });
    return new StudyReportPage();
  }

  goToReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click({ force: true });
  }

  // Navigate to the Pathology Dashboard
  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.pathologyMenu).click();
    return new DashBoardPage();
  }

  // Navigate to the ImmunoChemistry Dashboard
  goToImmunoChemistryDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.immunochemMenu).click();
    return new DashBoardPage();
  }

  // Navigate to the Cytology Dashboard
  goToCytologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.cytologyMenu).click();
    return new DashBoardPage();
  }

  // Navigate to the Admin page
  goToAdminPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationMenu).click();
    return new AdminPage();
  }

  // Home page navigation

  // Minimize the page
  afterAll() {
    cy.get(this.selectors.minimizeIcon).should("be.visible").click();
  }

  // Search for a patient
  searchBar() {
    cy.get(this.selectors.searchIcon).click();
    cy.get(this.selectors.searchItem).type("Smith");
    cy.get(this.selectors.patientSearch).click();
    cy.get(this.selectors.searchIcon).click();
  }

  // Click the notifications icon
  clickNotifications() {
    cy.get(this.selectors.notificationIcon).click();
    cy.get(this.selectors.notificationIcon).click();
  }

  // Click the user icon
  clickUserIcon() {
    cy.get(this.selectors.userIcon).click();
    cy.get(this.selectors.userIcon).click();
  }

  // Click the help icon
  clickHelpIcon() {
    cy.get(this.selectors.helpIcon).click();
    cy.get(this.selectors.helpIcon).click();
  }

  // Select In Progress
  selectInProgress() {
    cy.get(this.selectors.maximizeIcon).click();
  }

  // Select Ready for Validation
  selectReadyforValidation() {
    cy.contains("a.cds--link", "Ready For Validation").click();
  }

  // Select Orders Completed Today
  selectOrdersCompletedToday() {
    cy.contains("a.cds--link", "Orders Completed Today").click();
  }

  // Select Partially Completed Today
  selectPartiallyCompletedToday() {
    cy.contains("a.cds--link", "Partially Completed Today").click();
  }

  // Select Orders Entered By Users
  selectOrdersEnteredByUsers() {
    cy.contains("a.cds--link", "Orders Entered By Users").click();
  }

  // Select Orders Rejected
  selectOrdersRejected() {
    cy.contains("a.cds--link", "Orders Rejected").click();
  }

  // Select UnPrinted Results
  selectUnPrintedResults() {
    cy.contains("a.cds--link", "UnPrinted Results").click();
  }

  // Select Electronic Orders
  selectElectronicOrders() {
    cy.contains("a.cds--link", "Electronic Orders").click();
  }

  // Select Average Turn Around Time
  selectAverageTurnAroundTime() {
    cy.contains("a.cds--link", "Average Turn Around time").click();
  }

  // Select Delayed Turn Around
  selectDelayedTurnAround() {
    cy.contains("a.cds--link", "Delayed Turn Around").click();
  }
}

export default HomePage;
