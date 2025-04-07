import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";

describe("Dashboard Tests", function () {
  let loginPage, homePage, dashboard, adminPage, providerManagementPage;

  // Helper function to add a new order
  const completeOrderWorkflow = (
    dashboardType,
    testType,
    sampleType,
    panelType,
  ) => {
    // Navigate to order page
    homePage.goToOrderPage();
    dashboard.searchPatientByFName();
    dashboard.searchPatient();
    dashboard.checkPatientRadio();
    dashboard.clickNext();

    // Select test
    dashboard[`select${testType}`]();
    dashboard.clickNext();

    // Select sample and panel
    dashboard[`select${sampleType}`]();
    dashboard[`check${panelType}`]();
    dashboard.clickNext();

    // Generate lab number and complete form
    dashboard.generateLabNo();
    dashboard.selectSite();
    dashboard.selectRequesting();
    dashboard.submitButton();

    // Print barcode
    dashboard.clickPrintBarCode();

    // Go back to specific dashboard
    dashboard = homePage[`goTo${dashboardType}Dashboard`]();

    // Change order status
    dashboard.selectFirstOrder();
    dashboard.selectStatus();
    dashboard.selectTechnician();
    dashboard.selectPathologist();
    dashboard.checkReadyForRelease();
    dashboard.saveOrder();

    // Validate order status
    dashboard.statusFilter();
  };

  // Setup before each test
  beforeEach(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = new AdminPage();
    providerManagementPage = new ProviderManagementPage();
  });

  it("Should add a new provider/requester", function () {
    // Navigate to admin page
    dashboard = homePage.goToAdminPage();
    dashboard = adminPage.goToProviderManagementPage();

    // Add provider
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
  });

  it("Should complete Pathology workflow", function () {
    dashboard = homePage.goToPathologyDashboard();
    completeOrderWorkflow(
      "Pathology",
      "Histopathology",
      "PathologySample",
      "PathologyPanel",
    );
  });

  it("Should complete ImmunoChemistry workflow", function () {
    dashboard = homePage.goToImmunoChemistryDashboard();
    completeOrderWorkflow(
      "ImmunoChemistry",
      "ImmunoChem",
      "ImmunoChemSample",
      "ImmunoChemTest",
    );
  });

  it("Should complete Cytology workflow", function () {
    dashboard = homePage.goToCytologyDashboard();
    completeOrderWorkflow("Cytology", "Cytology", "FluidSample", "CovidPanel");
  });
});
