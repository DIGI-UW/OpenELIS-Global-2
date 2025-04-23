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
    // Removed centralized selectors
  }

  visit() {
    cy.visit("/");
  }

  goToSign() {
    return new LoginPage();
  }

  openNavigationMenu() {
    cy.get("[data-cy='menuButton']", { timeout: 30000 }).click();
  }

  // Order Entry related functions
  goToOrderPageExt() {
    this.openNavigationMenu();
    cy.get("span#menu_sample_dropdown").click({ force: true });
    cy.get("#menu_sample_add_nav", { timeout: 20000 }).click({ force: true });
    return new OrderEntityPage();
  }

  goToOrderPage() {
    this.openNavigationMenu();
    cy.get("span#menu_sample", { timeout: 20000 }).should("be.visible").click();
    cy.get("#menu_sample_add_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new OrderEntityPage();
  }

  goToBatchOrderEntry() {
    this.openNavigationMenu();
    //cy.get("span#menu_sample").click();
    cy.contains("span", "Order").click();
    cy.get("#menu_sample_batch_entry").click();
    return new BatchOrderEntry();
  }

  // Patient Entry related functions
  goToPatientEntry() {
    this.openNavigationMenu();
    cy.get("span#menu_patient", { timeout: 20000 }).click();
    cy.get("#menu_patient_add_or_edit_nav", { timeout: 20000 }).click();
    return new PatientEntryPage();
  }

  // Modify Order related functions
  goToModifyOrderPage() {
    this.openNavigationMenu();
    cy.get("span#menu_sample", { timeout: 20000 }).should("be.visible").click();
    cy.get("#menu_sample_edit_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new ModifyOrderPage();
  }

  // Work Plan related functions
  goToWorkPlanPlanByTest() {
    this.openNavigationMenu();
    cy.get("span#menu_workplan", { timeout: 20000 })
      .should("be.visible")
      .click();
    cy.get("#menu_workplan_test_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPanel() {
    this.openNavigationMenu();
    cy.get("span#menu_workplan").click({ force: true });
    cy.get("#menu_workplan_panel_nav", { timeout: 20000 }).click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByUnit() {
    this.openNavigationMenu();
    cy.get("span#menu_workplan", { timeout: 20000 }).click();
    cy.get("#menu_workplan_bench_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPriority() {
    this.openNavigationMenu();
    cy.get("span#menu_workplan", { timeout: 20000 }).click();
    cy.get("#menu_workplan_priority_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new WorkPlan();
  }

  // Non-Conforming related functions
  goToReportNCE() {
    this.openNavigationMenu();
    cy.get("span#menu_nonconformity_dropdown", { timeout: 20000 }).click();
    cy.get("span#menu_non_conforming_report", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new NonConform();
  }

  goToViewNCE() {
    this.openNavigationMenu();
    cy.get("span#menu_nonconformity_dropdown", { timeout: 20000 }).click();
    cy.get("span#menu_non_conforming_view", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new NonConform();
  }

  goToCorrectiveActions() {
    this.openNavigationMenu();
    cy.get("span#menu_nonconformity", { timeout: 20000 }).click();
    cy.get("span#menu_non_conforming_corrective_actions")
      .should("be.visible")
      .click();
    return new NonConform();
  }

  // Results related functions
  goToResultsByUnit() {
    this.openNavigationMenu();
    cy.get("span#menu_results", { timeout: 20000 }).click();
    cy.get("#menu_results_logbook_nav", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new Result();
  }

  goToResultsByOrder() {
    cy.get("[data-cy='menuButton']").click();
    cy.get("span#menu_results_dropdown").click();
    cy.get("#menu_results_accession_nav").click();

    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.contains("span", "Results").click();
    cy.contains("span", "By Patient").click();

    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get("span#menu_results", { timeout: 20000 }).click();
    cy.get("#menu_results_referred_nav", { timeout: 20000 }).click();
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get("span#menu_results", { timeout: 20000 }).click();
    cy.get("#menu_results_range_nav", { timeout: 20000 }).click();
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get("span#menu_results", { timeout: 20000 }).click();
    cy.get("#menu_results_status_nav", { timeout: 20000 }).click();
    return new Result();
  }

  // Validation related functions
  goToValidationByRoutine() {
    this.openNavigationMenu();
    cy.contains("span", "Validation").click();
    cy.contains("span", "Routine").click();
    return new Validation();
  }

  goToValidationByOrder() {
    this.openNavigationMenu();
    cy.contains("span", "Validation").click();
    cy.get("#menu_accession_validation").click();
    return new Validation();
  }

  goToValidationByRangeOrder() {
    this.openNavigationMenu();
    cy.contains("span", "Validation").click();
    cy.contains("span", "By Range of Order Numbers").click();
    return new Validation();
  }

  // Reports related functions
  goToRoutineReports() {
    this.openNavigationMenu();
    cy.get("#menu_reports", { timeout: 20000 }).click();
    cy.get("#menu_reports_routine", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new RoutineReportPage();
  }

  goToStudyReports() {
    this.openNavigationMenu();
    cy.get("#menu_reports", { timeout: 20000 }).click();
    cy.get("[data-cy='sidenav-button-menu_reports_study']", { timeout: 20000 })
      .should("be.visible")
      .click();
    return new StudyReportPage();
  }

  goToReports() {
    this.openNavigationMenu();
    cy.get("#menu_reports", { timeout: 15000 }).click();
  }

  // Dashboard related functions
  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get("#menu_pathology_nav", { timeout: 15000 })
      .should("be.visible")
      .click();
    return new DashBoardPage();
  }

  goToImmunoChemistryDashboard() {
    this.openNavigationMenu();
    cy.get("#menu_immunochem").click();
    return new DashBoardPage();
  }

  goToCytologyDashboard() {
    this.openNavigationMenu();
    cy.get("#menu_cytology").click();
    return new DashBoardPage();
  }

  // Admin related functions
  goToAdminPageProgram() {
    this.openNavigationMenu();
    cy.get("span#menu_administration", { timeout: 30000 }).click();
    return new AdminPage();
  }

  goToAdminPage() {
    this.openNavigationMenu();
    cy.get("#menu_administration_nav", { timeout: 30000 }).click();
    return new AdminPage();
  }

  // UI interaction functions
  afterAll() {
    cy.get("#minimizeIcon").should("be.visible").click();
  }

  searchBar() {
    cy.get("#search-Icon").click();
    cy.get("#searchItem").type("Smith");
    cy.get("#patientSearch").click();
    cy.get("#search-Icon").click();
  }

  clickNotifications() {
    cy.get("#notification-Icon").click();
    cy.get("#notification-Icon").click();
  }

  clickUserIcon() {
    cy.get("#user-Icon").click();
    cy.get("#user-Icon").click();
  }

  clickHelpIcon() {
    cy.get("#user-Help").click();
    cy.get("#user-Help").click();
  }

  selectInProgress() {
    cy.get("#maximizeIcon").click();
  }

  selectReadyforValidation() {
    cy.contains("a.cds--link", "Ready For Validation").click();
  }

  selectOrdersCompletedToday() {
    cy.contains("a.cds--link", "Orders Completed Today").click();
  }

  selectPartiallyCompletedToday() {
    cy.contains("a.cds--link", "Partially Completed Today").click();
  }

  selectOrdersEnteredByUsers() {
    cy.contains("a.cds--link", "Orders Entered By Users").click();
  }

  selectOrdersRejected() {
    cy.contains("a.cds--link", "Orders Rejected").click();
  }

  selectUnPrintedResults() {
    cy.contains("a.cds--link", "UnPrinted Results").click();
  }

  selectElectronicOrders() {
    cy.contains("a.cds--link", "Electronic Orders").click();
  }

  selectAverageTurnAroundTime() {
    cy.contains("a.cds--link", "Average Turn Around time").click();
  }

  selectDelayedTurnAround() {
    cy.contains("a.cds--link", "Delayed Turn Around").click();
  }
}

export default HomePage;
