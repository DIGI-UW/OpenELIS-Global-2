import LoginPage from "./LoginPage";
import AdminPage from "./AdminPage";

class HomePage {
  constructor() {
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      sampleAddNav: "#menu_sample_add_nav",
      sampleMenu: "#menu_sample",
      batchEntry: "#menu_sample_batch_entry",
      patientMenu: "#menu_patient",
      patientAddEdit: "#menu_patient_add_or_edit_nav",
      patientMerge: "#menu_patient_merge",
      sampleEditNav: "#menu_sample_edit_nav",
      workplanMenu: "#menu_workplan",
      workplanTestNav: "#menu_workplan_test_nav",
      workplanPanelNav: "#menu_workplan_panel_nav",
      workplanBenchNav: "#menu_workplan_bench_nav",
      workplanPriorityNav: "#menu_workplan_priority_nav",
      nonConformityDropdown: "#menu_nonconformity_dropdown",
      nonConformingReport: "#menu_non_conforming_report",
      nonConformingView: "#menu_non_conforming_view",
      nonConformingActions: "#menu_non_conforming_corrective_actions",
      resultsMenu: "#menu_results",
      resultsLogbook: "#menu_results_logbook_nav",
      resultsAccession: "#menu_results_accession_nav",
      resultsPatient: "#menu_results_patient",
      resultsReferred: "#menu_results_referred_nav",
      resultsRange: "#menu_results_range_nav",
      resultsStatus: "#menu_results_status_nav",
      validationMenu: "#menu_resultvalidation",
      reportsMenu: "#menu_reports",
      reportsRoutine: "#menu_reports_routine",
      reportsStudy: "[data-cy='sidenav-button-menu_reports_study']",
      pathologyNav: "#menu_pathology_nav",
      immunochemMenu: "#menu_immunochem",
      cytologyMenu: "#menu_cytology",
      administrationMenu: "#menu_administration",
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
    // Desktop viewports render the nav permanently with no menu button;
    // only click the hamburger when the nav is actually collapsed.
    cy.get("body").then(($body) => {
      if ($body.find(".cds--side-nav--expanded").length === 0) {
        cy.get(this.selectors.menuButton).click();
      }
    });
  }

  closeNavigationMenu() {
    cy.get("body").then(($body) => {
      const $btn = $body.find(this.selectors.menuButton);
      const ariaLabel = $btn.attr("aria-label");

      // Only click if the button exists (small viewports) and the menu is open.
      if (
        $btn.length &&
        ariaLabel &&
        ariaLabel.toLowerCase().includes("close")
      ) {
        cy.wrap($btn).click();
      }
    });
  }

  // Order Entry related functions
  // Patient Entry related functions
  // Patient Merge (Admin function)
  // Modify Order related functions
  // Work Plan related functions
  // Non-Conforming related functions
  // Results related functions
  // Validation related functions
  // Reports related functions
  goToReports() {
    this.openNavigationMenu();
    cy.get(this.selectors.reportsMenu).click();
  }

  // Dashboard related functions
  // Admin related functions
  goToAdminPageProgram() {
    return this.goToAdminPage();
  }

  goToAdminPage() {
    // Admin has no submenu: the menu entry itself opens the dashboard.
    this.openNavigationMenu();
    cy.get(this.selectors.administrationMenu).should("be.visible").click();
    cy.location("pathname").should("eq", "/MasterListsPage");
    this.closeNavigationMenu();
    return new AdminPage();
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

  selectReadyforValidation() {
    cy.contains(this.selectors.link, "Ready For Validation").click();
  }
}

export default HomePage;
