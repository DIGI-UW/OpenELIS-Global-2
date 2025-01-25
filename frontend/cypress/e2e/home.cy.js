import LoginPage from "../pages/LoginPage";

let loginPage = null;
let home = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  home = loginPage.goToHomePage();
});

//This action runs after each test
afterEach(() => {
  home.afterAll();
});

describe("User navigates to different tiles", function () {
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
