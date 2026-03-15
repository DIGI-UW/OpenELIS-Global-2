import LoginPage from "../pages/LoginPage";

let loginPage = null;
let home = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  home = loginPage.goToHomePage();
});

describe("User interacts with the navigation bar", function () {
  it("User searches for patient and closes search bar", function () {
    home.searchBar();
  });

  it("User checks for notifications and closes it", function () {
    home.clickNotifications();
  });

  it("User interacts with the user icon", function () {
    home.clickUserIcon();
  });

  it("User interacts with the help icon", function () {
    home.clickHelpIcon();
  });
});

describe("User navigates to different tiles", function () {
  // This action runs after each test
  afterEach(() => {
    home.afterAll();
  });

  it("User navigates to the In Progress", function () {
    home.selectInProgress();
  });

  it("User navigates to Ready for Validation", function () {
    home.selectReadyforValidation();
  });

  it("User navigates to Orders Completed Today", function () {
    home.selectOrdersCompletedToday();
  });

  it("User navigates to Partially Completed Today", function () {
    home.selectPartiallyCompletedToday();
  });

  it("User navigates to Orders Entered By Users", function () {
    home.selectOrdersEnteredByUsers();
  });

  it("User navigates to Orders Rejected", function () {
    home.selectOrdersRejected();
  });

  it("User navigates to UnPrinted Results", function () {
    home.selectUnPrintedResults();
  });

  it("User navigates to Electronic Orders", function () {
    home.selectElectronicOrders();
  });

  it("User navigates to Average Turn Around time", function () {
    home.selectAverageTurnAroundTime();
  });

  it("User navigates to Delayed Turn Around", function () {
    home.selectDelayedTurnAround();
  });
});

describe("In Progress widget includes retest requests (Issue #2890)", function () {
  it("should fetch In Progress orders from the API endpoint", function () {
    // Intercept the API call for In Progress orders
    cy.intercept("GET", "/rest/home-dashboard/ORDERS_IN_PROGRESS*").as(
      "getInProgressOrders",
    );

    // Navigate to In Progress tile
    home.selectInProgress();

    // Wait for the API call and verify it succeeds
    cy.wait("@getInProgressOrders").then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
    });
  });

  it("should fetch dashboard metrics including In Progress count", function () {
    // Intercept the metrics API call
    cy.intercept("GET", "/rest/home-dashboard/metrics").as("getMetrics");

    // Reload the dashboard to trigger metrics fetch
    cy.visit("/");

    // Wait for the API call and verify it succeeds
    cy.wait("@getMetrics").then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      // Verify the response contains ordersInProgress field
      expect(interception.response.body).to.have.property("ordersInProgress");
    });
  });
});
