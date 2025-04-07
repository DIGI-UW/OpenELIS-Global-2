import LoginPage from "../pages/LoginPage";

describe("Work Plan Tests", () => {
  let homePage = null;
  let loginPage = null;
  let workplan = null;

  beforeEach(() => {
    // Setup before each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
  });

  describe("Work plan by Test", () => {
    beforeEach(() => {
      workplan = homePage.goToWorkPlanPlanByTest();
    });

    it("should display work plan by test page with correct title", () => {
      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.testTile);
      });
    });

    it("should select test from dropdown selector option", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.testName);
        workplan.getPrintWorkPlanButton();
      });
    });

    it("should display all known orders", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.testName);
      });

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

  describe("Work plan by Panel", () => {
    beforeEach(() => {
      workplan = homePage.goToWorkPlanPlanByPanel();
    });

    it("should display work plan by panel page with correct title", () => {
      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.panelTile);
      });
    });

    it("should select panel from dropdown selector option", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.panelType);
        workplan.getPrintWorkPlanButton();
      });
    });

    it("should display all known orders", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.panelType);
      });

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

  describe("Work plan by Unit", () => {
    beforeEach(() => {
      workplan = homePage.goToWorkPlanPlanByUnit();
    });

    it("should display work plan by unit page with correct title", () => {
      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.unitTile);
      });
    });

    it("should select unit type from dropdown selector option", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.unitType);
        workplan.getPrintWorkPlanButton();
      });
    });

    it("should display all known orders", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.unitType);
      });

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

  describe("Work plan by Priority", () => {
    beforeEach(() => {
      workplan = homePage.goToWorkPlanPlanByPriority();
    });

    it("should display work plan by priority page with correct title", () => {
      cy.fixture("workplan").then((options) => {
        workplan.getWorkPlanFilterTitle(options.priorityTile);
      });
    });

    it("should select priority from dropdown selector option", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.priority);
        workplan.getPrintWorkPlanButton();
      });
    });

    it("should display all known orders", () => {
      cy.fixture("workplan").then((options) => {
        workplan.selectDropdownOption(options.priority);
      });

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
});
