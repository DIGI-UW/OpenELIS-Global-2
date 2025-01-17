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
  constructor() {}

  visit() {
    cy.visit("/");
  }

  goToSign() {
    return new LoginPage();
  }

  goToOrderPage() {
    this.openNavigationMenu();
    cy.get("#menu_sample_dropdown").click();
    cy.get("#menu_sample_add_nav").click();
    return new OrderEntityPage();
  }

  openNavigationMenu() {
    cy.get("header#mainHeader > button[title='Open menu']", {
      timeout: 30000,
    }).click();
  }

  goToBatchOrderEntry() {
    this.openNavigationMenu();
    cy.get("#menu_sample_dropdown").click();
    cy.get("#menu_sample_batch_entry_nav").click();
    return new BatchOrderEntry();
  }

  goToPatientEntry() {
    this.openNavigationMenu();
    cy.get("#menu_patient_dropdown").click();
    cy.get("#menu_patient_add_or_edit_nav").click();
    return new PatientEntryPage();
  }

  goToModifyOrderPage() {
    this.openNavigationMenu();
    cy.get("#menu_sample_dropdown").click();
    cy.get("#menu_sample_edit_nav").click();
    return new ModifyOrderPage();
  }
  goToWorkPlanPlanByTest() {
    this.openNavigationMenu();
    cy.get("#menu_workplan_dropdown").click();
    cy.get("#menu_workplan_test_nav").click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPanel() {
    this.openNavigationMenu();
    cy.get("#menu_workplan_dropdown").click();
    cy.get("#menu_workplan_panel_nav").click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByUnit() {
    this.openNavigationMenu();
    cy.get("#menu_workplan_dropdown").click();
    cy.get("#menu_workplan_bench_nav").click();
    return new WorkPlan();
  }

  goToWorkPlanPlanByPriority() {
    this.openNavigationMenu();
    cy.get("#menu_workplan_dropdown").click();
    cy.get("#menu_workplan_priority_nav").click();
    return new WorkPlan();
  }

  goToReportNCE() {
    this.openNavigationMenu();
    cy.get("#menu_nonconformity_dropdown").click();
    cy.get("#menu_non_conforming_report_nav").click();
    return new NonConform();
  }

  goToViewNCE() {
    this.openNavigationMenu();
    cy.get("#menu_nonconformity_dropdown").click();
    cy.get("#menu_non_conforming_view_nav").click();
    return new NonConform();
  }
  goToCorrectiveActions() {
    this.openNavigationMenu();
    cy.get("#menu_nonconformity_dropdown").click();
    cy.get("#menu_non_conforming_corrective_actions_nav").click();
    return new NonConform();
  }

  goToResultsByUnit() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_logbook").click();
    return new Result();
  }

  goToResultsByOrder() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_accession").click();
    return new Result();
  }

  goToResultsByPatient() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_patient").click();
    return new Result();
  }

  goToResultsForRefferedOut() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_referred ").click();
    return new Result();
  }

  goToResultsByRangeOrder() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_range").click();
    return new Result();
  }

  goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    cy.get("#menu_results").click();
    cy.get("#menu_results_status").click();
    return new Result();
  }

  goToValidationByRoutine() {
    this.openNavigationMenu();
    cy.get("#menu_resultvalidation").click();
    cy.get("#menu_resultvalidation_routine ").click();
    return new Validation();
  }
  goToValidationByOrder() {
    this.openNavigationMenu();
    cy.get("#menu_resultvalidation").click();
    cy.get("#menu_accession_validation ").click();
    return new Validation();
  }
  goToValidationByRangeOrder() {
    this.openNavigationMenu();
    cy.get("#menu_resultvalidation").click();
    cy.get("#menu_accession_validation_range ").click();
    return new Validation();
  }

  goToRoutineReports() {
    this.openNavigationMenu();
    cy.get("#menu_reports").click();
    cy.get("#menu_reports_routine_nav").click();
    return new RoutineReportPage();
  }
  goToStudyReports() {
    this.openNavigationMenu();
    cy.get("#menu_reports").click();
    cy.get("#menu_reports_study_nav").click();
    return new StudyReportPage();
  }

  goToPathologyDashboard() {
    this.openNavigationMenu();
    cy.get("#menu_pathology_dropdown").click();
    cy.get("#menu_pathologydashboard_nav").click();
    return new DashBoardPage();
  }

  goToImmunoChemistryDashboard() {
    this.openNavigationMenu();
    cy.get("#menu_immunochem_dropdown").click();
    cy.get("#menu_immunochemdashboard_nav").click();
    return new DashBoardPage();
  }

  goToAdminPage() {
    this.openNavigationMenu();
    cy.get("#menu_administration").click();
    return new AdminPage();
  }
}

export default HomePage;
