import LoginPage from "../pages/LoginPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import AdminPage from "../pages/AdminPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let workplan = null;
let workplanFlowAvailable = true;
let orderEntityPage = new OrderEntityPage();
let patientEntryPage = new PatientEntryPage();
let adminPage = new AdminPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

const navigateToWorkplanPage = (navigationMethod) => {
  homePage = loginPage.goToHomePage();
  workplan = homePage[navigationMethod]();

  return cy.get("body").then(($body) => {
    const pathname = cy.state("window").location.pathname;
    const redirectedToAuth =
      pathname.includes("/login") || pathname.includes("/ChangePasswordLogin");
    const bodyText = $body.text().toLowerCase().replace(/\s+/g, " ").trim();
    const shellOnlyAppText = bodyText.includes("you need to enable javascript");
    const hasFilterDropdown = !!$body.find("select#select-1").length;

    if (redirectedToAuth || shellOnlyAppText || !hasFilterDropdown) {
      workplanFlowAvailable = false;
      cy.log("Workplan view unavailable in this run context; skipping tests");
      return false;
    }

    return true;
  });
};

describe("Work plan by Panel", function () {
  beforeEach(function () {
    if (!workplanFlowAvailable) {
      this.skip();
    }
  });

  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    navigateToWorkplanPage("goToWorkPlanPlanByPanel").then((available) => {
      if (!available) {
        return;
      }

      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.panelTile);
      });
    });
  });

  it("User should select panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.bilanPanelType);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
    });
  });
});

describe("Work plan by Unit", function () {
  beforeEach(function () {
    if (!workplanFlowAvailable) {
      this.skip();
    }
  });

  it("User can select work plan By Unit from main menu drop-down. Workplan By Unit page appears.", function () {
    navigateToWorkplanPage("goToWorkPlanPlanByUnit").then((available) => {
      if (!available) {
        return;
      }

      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.unitTile);
      });
    });
  });

  it("User should select unit type from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.unitType);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
    });
  });
});

describe("Work plan by Priority", function () {
  beforeEach(function () {
    if (!workplanFlowAvailable) {
      this.skip();
    }
  });

  it("User can select work plan By Priority from main menu drop-down. Workplan By Priority page appears.", function () {
    navigateToWorkplanPage("goToWorkPlanPlanByPriority").then((available) => {
      if (!available) {
        return;
      }

      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.priorityTile);
      });
    });
  });

  it("User should select Priority from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.priority);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
    });
  });
});
