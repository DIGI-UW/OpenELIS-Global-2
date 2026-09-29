import AdminPage from "./AdminPage";

class HomePage {
  constructor() {
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      administrationMenu: "#menu_administration",
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
