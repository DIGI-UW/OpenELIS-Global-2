import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let dashboard = null;

// Helper function to log in and navigate to the homepage
const loginAndNavigateToHome = () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
};

// Helper function to add a new order
const addNewOrder = (dashboardType, testType, sampleType, panelType) => {
  homePage.goToOrderPage();
  dashboard.searchPatientByFName();
  dashboard.searchPatient();
  cy.wait(200);
  dashboard.checkPatientRadio();
  dashboard.clickNext();
  cy.wait(200);
  dashboard[`select${testType}`](); // Dynamically call the appropriate select function
  dashboard.clickNext();
  cy.wait(200);
  dashboard[`select${sampleType}`](); // Dynamically call the appropriate sample function
  dashboard[`check${panelType}`](); // Dynamically call the appropriate panel function
  dashboard.clickNext();
  cy.wait(200);
  dashboard.generateLabNo();
  dashboard.selectSite();
  dashboard.selectRequesting();
  cy.wait(1000);
  dashboard.submitButton();
  cy.wait(8000);
};

// Helper function to validate success and print barcode
const validateSuccessAndPrintBarcode = () => {
  dashboard.clickPrintBarCode();
  cy.wait(200);
};

// Helper function to change order status and save
const changeOrderStatusAndSave = (dashboardType) => {
  dashboard.selectFirstOrder();
  cy.wait(500);
  dashboard.selectStatus();
  dashboard.selectTechnician();
  dashboard.selectPathologist();
  dashboard.checkReadyForRelease();
  dashboard.saveOrder();
  cy.wait(200);
};

// Helper function to validate order status
const validateOrderStatus = (dashboardType) => {
  dashboard = homePage[`goTo${dashboardType}Dashboard`]();
  dashboard.statusFilter();
};

describe("Dashboard Tests", function () {
  before("Navigate to homepage", () => {
    loginAndNavigateToHome();
  });

  describe("Pathology Dashboard", function () {
    before("Navigate to Pathology Dashboard", function () {
      dashboard = homePage.goToPathologyDashboard();
      cy.wait(500);
    });

    it("User adds a new Pathology order", function () {
      addNewOrder(
        "Pathology",
        "Histopathology",
        "PathologySample",
        "PathologyPanel",
      );
    });

    //it("Validate Success by Confirming Print Barcode button", function () {
    // validateSuccessAndPrintBarcode();
    //});

    it("User navigates back to Pathology Dashboard to confirm added order", function () {
      dashboard = homePage.goToPathologyDashboard();
    });

    it("Change The Status of Order and saves it", function () {
      changeOrderStatusAndSave("Pathology");
    });

    it("Validate the Status of Order", function () {
      validateOrderStatus("Pathology");
    });
  });

  // ImmunoChemistry Dashboard Tests
  describe("ImmunoChemistry Dashboard", function () {
    before("Navigate to ImmunoChemistry Dashboard", function () {
      dashboard = homePage.goToImmunoChemistryDashboard();
      cy.wait(500);
    });

    it("User adds a new ImmunoChemistry order", function () {
      addNewOrder(
        "ImmunoChemistry",
        "ImmunoChem",
        "ImmunoChemSample",
        "ImmunoChemTest",
      );
    });

    it("Validate Success by Confirming Print Barcode button", function () {
      validateSuccessAndPrintBarcode();
    });

    it("User navigates back to ImmunoChemistry Dashboard to confirm added order", function () {
      homePage.goToImmunoChemistryDashboard();
    });

    it("Change The Status of Order and saves it", function () {
      changeOrderStatusAndSave("ImmunoChemistry");
    });

    it("Validate the Status of Order", function () {
      validateOrderStatus("ImmunoChemistry");
    });
  });

  // Cytology Dashboard Tests
  describe("Cytology Dashboard", function () {
    before("Navigate to Cytology Dashboard", function () {
      dashboard = homePage.goToCytologyDashboard();
      cy.wait(500);
    });

    it("User adds a new Cytology order", function () {
      addNewOrder("Cytology", "Cytology", "FluidSample", "CovidPanel");
    });

    it("Validate Success by Confirming Print Barcode button", function () {
      validateSuccessAndPrintBarcode();
    });

    it("User navigates back to Cytology Dashboard to confirm added order", function () {
      homePage.goToCytologyDashboard();
    });

    it("Change The Status of Order and saves it", function () {
      changeOrderStatusAndSave("Cytology");
    });

    it("Validate the Status of Order", function () {
      validateOrderStatus("Cytology");
    });
  });
});
