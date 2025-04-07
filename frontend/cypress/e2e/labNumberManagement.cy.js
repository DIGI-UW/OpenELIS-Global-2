import LoginPage from "../pages/LoginPage";

describe("Lab Number Management", function () {
  let loginPage;
  let homePage;
  let adminPage;
  let labNumMgtPage;

  beforeEach(() => {
    // Initialize LoginPage object and navigate to Admin Page for each test
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();

    // Load fixture data for each test
    cy.fixture("LabNumberManagement").as("labNMData");
  });

  it("User navigates to the Lab Number Management page", function () {
    labNumMgtPage = adminPage.goToLabNumberManagementPage();
    labNumMgtPage.verifyPageLoaded();
  });

  it("Validate Page Visibility", function () {
    labNumMgtPage = adminPage.goToLabNumberManagementPage();
    labNumMgtPage.verifyPageLoaded();
  });

  it("User selects legacy lab number type and submits", function () {
    labNumMgtPage = adminPage.goToLabNumberManagementPage();

    cy.get("@labNMData").then((labNumberManagementData) => {
      labNumMgtPage.selectLabNumber(
        labNumberManagementData.legacyLabNumberType,
      );
      labNumMgtPage.clickSubmitButton();
    });
  });

  it("User selects alpha numeric lab number type and submits", function () {
    labNumMgtPage = adminPage.goToLabNumberManagementPage();

    cy.get("@labNMData").then((labNumberManagementData) => {
      labNumMgtPage.selectLabNumber(labNumberManagementData.alphaLabNumberType);
      labNumMgtPage.checkPrefixCheckBox();
      labNumMgtPage.typePrefix(labNumberManagementData.userPrefix);
      labNumMgtPage.clickSubmitButton();
    });
  });
});
