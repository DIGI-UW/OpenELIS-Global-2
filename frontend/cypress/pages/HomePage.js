import LoginPage from "./LoginPage";
import PatientEntryPage from "./PatientEntryPage";
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
      sampleEditNav: "#menu_sample_edit_nav",
      workplanMenu: "span#menu_workplan",
      workplanTestNav: "#menu_workplan_test_nav",
      workplanPanelNav: "#menu_workplan_panel_nav",
      workplanBenchNav: "#menu_workplan_bench_nav",
      workplanPriorityNav: "#menu_workplan_priority_nav",
      nonConformityDropdown: "span#menu_nonconformity_dropdown",
      nonConformingReport: "span#menu_non_conforming_report",
      nonConformingView: "span#menu_non_conforming_view",
      nonConformingActions: "span#menu_non_conforming_corrective_actions",
      resultsMenu: "span#menu_results",
      resultsLogbook: "#menu_results_logbook_nav",
      resultsAccession: "#menu_results_accession_nav",
      resultsPatient: "#menu_results_patient",
      resultsReferred: "#menu_results_referred_nav",
      resultsRange: "#menu_results_range_nav",
      resultsStatus: "#menu_results_status_nav",
      validationMenu: "#menu_resultvalidation",
      routineValidation: "#menu_resultvalidation_routine",
      rangeOrderValidation: "#menu_accession_validation_range",
      accessionValidation: "#menu_accession_validation",
      reportsMenu: "#menu_reports",
      reportsRoutine: "#menu_reports_routine",
      reportsStudy: "[data-cy='sidenav-button-menu_reports_study']",
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
    cy.get(this.selectors.menuButton).click();
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
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsLogbook).should("be.visible").click();
    return new Result();
  }

  goToResultsByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsAccession).click();
    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsPatient).click();
    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsReferred).click();
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsRange).click();
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click();
    cy.get(this.selectors.resultsStatus).click();
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
    cy.get(this.selectors.reportsMenu).click();
    cy.get(this.selectors.reportsRoutine).should("be.visible").click();
    return new RoutineReportPage();
  }

  goToStudyReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click();
    cy.get(this.selectors.reportsStudy).should("be.visible").click();
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
    return new AdminPage();
  }

  goToAdminPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationNav).click();
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
