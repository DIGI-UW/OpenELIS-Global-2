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
      resultsMenu: "#menu_results",
      resultValidationMenu: "#menu_resultvalidation",
      reportsMenu: "#menu_reports",
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
      "#menu_sample_add_nav",
    );
    return new OrderEntityPage();
  }

  // Navigate to the Batch Order Entry page
  goToBatchOrderEntry() {
    this.clickDropdownItem(
      this.selectors.sampleDropdown,
      "#menu_sample_batch_entry_nav",
    );
    return new BatchOrderEntry();
  }

  // Navigate to the Patient Entry page
  goToPatientEntry() {
    this.clickDropdownItem(
      this.selectors.patientDropdown,
      "#menu_patient_add_or_edit_nav",
    );
    return new PatientEntryPage();
  }

  // Navigate to the Modify Order page
  goToModifyOrderPage() {
    this.clickDropdownItem(
      this.selectors.sampleDropdown,
      "#menu_sample_edit_nav",
    );
    return new ModifyOrderPage();
  }

  // Navigate to the Work Plan by Test page
  goToWorkPlanPlanByTest() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      "#menu_workplan_test_nav",
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Panel page
  goToWorkPlanPlanByPanel() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      "#menu_workplan_panel_nav",
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Unit page
  goToWorkPlanPlanByUnit() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      "#menu_workplan_bench_nav",
    );
    return new WorkPlan();
  }

  // Navigate to the Work Plan by Priority page
  goToWorkPlanPlanByPriority() {
    this.clickDropdownItem(
      this.selectors.workplanDropdown,
      "#menu_workplan_priority_nav",
    );
    return new WorkPlan();
  }

  // Navigate to the Non-Conforming Report page
  goToReportNCE() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      "#menu_non_conforming_report_nav",
    );
    return new NonConform();
  }

  // Navigate to the View Non-Conforming Events page
  goToViewNCE() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      "#menu_non_conforming_view_nav",
    );
    return new NonConform();
  }

  // Navigate to the Corrective Actions page
  goToCorrectiveActions() {
    this.clickDropdownItem(
      this.selectors.nonconformityDropdown,
      "#menu_non_conforming_corrective_actions_nav",
    );
    return new NonConform();
  }

  // Navigate to the Results by Unit page
  goToResultsByUnit() {
    this.clickDropdownItem(this.selectors.resultsMenu, "#menu_results_logbook");
    return new Result();
  }

  // Navigate to the Results by Order page
  goToResultsByOrder() {
    this.clickDropdownItem(
      this.selectors.resultsMenu,
      "#menu_results_accession",
    );
    return new Result();
  }

  // Navigate to the Results by Patient page
  goToResultsByPatient() {
    this.clickDropdownItem(this.selectors.resultsMenu, "#menu_results_patient");
    return new Result();
  }

  // Navigate to the Results for Referred Out page
  goToResultsForRefferedOut() {
    this.clickDropdownItem(
      this.selectors.resultsMenu,
      "#menu_results_referred",
    );
    return new Result();
  }

  // Navigate to the Results by Range Order page
  goToResultsByRangeOrder() {
    this.clickDropdownItem(this.selectors.resultsMenu, "#menu_results_range");
    return new Result();
  }

  // Navigate to the Results by Test and Status page
  goToResultsByTestAndStatus() {
    this.clickDropdownItem(this.selectors.resultsMenu, "#menu_results_status");
    return new Result();
  }

  // Navigate to the Validation by Routine page
  goToValidationByRoutine() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      "#menu_resultvalidation_routine",
    );
    return new Validation();
  }

  // Navigate to the Validation by Order page
  goToValidationByOrder() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      "#menu_accession_validation",
    );
    return new Validation();
  }

  // Navigate to the Validation by Range Order page
  goToValidationByRangeOrder() {
    this.clickDropdownItem(
      this.selectors.resultValidationMenu,
      "#menu_accession_validation_range",
    );
    return new Validation();
  }

  // Navigate to the Routine Reports page
  goToRoutineReports() {
    this.clickDropdownItem(
      this.selectors.reportsMenu,
      "#menu_reports_routine_nav",
    );
    return new RoutineReportPage();
  }

  // Navigate to the Study Reports page
  goToStudyReports() {
    this.clickDropdownItem(
      this.selectors.reportsMenu,
      "#menu_reports_study_nav",
    );
    return new StudyReportPage();
  }

  // Navigate to the Pathology Dashboard
  goToPathologyDashboard() {
    this.clickDropdownItem(this.selectors.pathologyMenu, "");
    return new DashBoardPage();
  }

  // Navigate to the ImmunoChemistry Dashboard
  goToImmunoChemistryDashboard() {
    this.clickDropdownItem(this.selectors.immunochemMenu, "");
    return new DashBoardPage();
  }

  // Navigate to the Cytology Dashboard
  goToCytologyDashboard() {
    this.clickDropdownItem(this.selectors.cytologyMenu, "");
    return new DashBoardPage();
  }

  // Navigate to the Admin page
  goToAdminPage() {
    this.clickDropdownItem(this.selectors.administrationMenu, "");
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
