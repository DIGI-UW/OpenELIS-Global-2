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
      nonConformingCorrectiveActionsNav: "#menu_non_conforming_corrective_actions_nav",
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

  visit() {
    cy.visit("/");
  }

  goToSign() {
    return new LoginPage();
  }

  openNavigationMenu() {
    cy.get(this.selectors.menuButton, { timeout: 30000 }).click();
  }

  clickDropdownItem(dropdownSelector, itemSelector) {
    this.openNavigationMenu();
    cy.get(dropdownSelector).click();
    cy.get(itemSelector).click();
  }

  goToOrderPage() {
    this.clickDropdownItem(this.selectors.sampleDropdown, this.selectors.sampleAddNav);
    return new OrderEntityPage();
  }

  goToBatchOrderEntry() {
    this.clickDropdownItem(this.selectors.sampleDropdown, this.selectors.sampleBatchEntryNav);
    return new BatchOrderEntry();
  }

  goToPatientEntry() {
    this.clickDropdownItem(this.selectors.patientDropdown, this.selectors.patientAddOrEditNav);
    return new PatientEntryPage();
  }

  goToModifyOrderPage() {
    this.clickDropdownItem(this.selectors.sampleDropdown, this.selectors.sampleEditNav);
    return new ModifyOrderPage();
  }

  goToWorkPlanPlanByTest() {
    this.clickDropdownItem(this.selectors.workplanDropdown, this.selectors.workplanTestNav);
    return new WorkPlan();
  }

  goToWorkPlanPlanByPanel() {
    this.clickDropdownItem(this.selectors.workplanDropdown, this.selectors.workplanPanelNav);
    return new WorkPlan();
  }

  goToWorkPlanPlanByUnit() {
    this.clickDropdownItem(this.selectors.workplanDropdown, this.selectors.workplanBenchNav);
    return new WorkPlan();
  }

  goToWorkPlanPlanByPriority() {
    this.clickDropdownItem(this.selectors.workplanDropdown, this.selectors.workplanPriorityNav);
    return new WorkPlan();
  }

  goToReportNCE() {
    this.clickDropdownItem(this.selectors.nonconformityDropdown, this.selectors.nonConformingReportNav);
    return new NonConform();
  }

  goToViewNCE() {
    this.clickDropdownItem(this.selectors.nonconformityDropdown, this.selectors.nonConformingViewNav);
    return new NonConform();
  }

  goToCorrectiveActions() {
    this.clickDropdownItem(this.selectors.nonconformityDropdown, this.selectors.nonConformingCorrectiveActionsNav);
    return new NonConform();
  }

  goToResultsByUnit() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsLogbook).click({ force: true });
    return new Result();
  }

  goToResultsByOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsAccession).click({ force: true });
    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsPatient).click({ force: true });
    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsReferred).click({ force: true });
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsRange).click({ force: true });
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get(this.selectors.resultsMenu).click({ force: true });
    cy.get(this.selectors.resultsStatus).click({ force: true });
    return new Result();
  }

  goToValidationByRoutine() {
    this.clickDropdownItem(this.selectors.resultValidationMenu, this.selectors.resultValidationRoutine);
    return new Validation();
  }

  goToValidationByOrder() {
    this.clickDropdownItem(this.selectors.resultValidationMenu, this.selectors.accessionValidation);
    return new Validation();
  }

  goToValidationByRangeOrder() {
    this.clickDropdownItem(this.selectors.resultValidationMenu, this.selectors.accessionValidationRange);
    return new Validation();
  }

  goToRoutineReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click();
    cy.get(this.selectors.reportsRoutineNav).click();
    return new RoutineReportPage();
  }

  goToStudyReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click({ force: true });
    cy.get(this.selectors.reportsStudyNav).click({ force: true });
    return new StudyReportPage();
  }

  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get(this.selectors.pathologyMenu).click();
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

  goToAdminPage() {
    this.openNavigationMenu();
    cy.get(this.selectors.administrationMenu).click();
    return new AdminPage();
  }
}

export default HomePage;
