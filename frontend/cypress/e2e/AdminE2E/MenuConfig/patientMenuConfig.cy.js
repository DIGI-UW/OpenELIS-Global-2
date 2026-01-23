import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let menuConfigPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Patient Menu Configuration", function () {
  it("User navigates to the Patient Menu Configuration page", function () {
    menuConfigPage = adminPage.goToPatientConfigPage();
  });

  describe("Deactivate Patient Menu", () => {
    it("Deactivate Patient Menu and submit", function () {
      menuConfigPage.validateToggleStatus("Off");
      menuConfigPage.uncheckPatientMenu();
      menuConfigPage.submitButton();
    });

    it("Validate Patient is Deactivated", () => {
      cy.reload();
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validatePatientMenuOff();
    });
  });

  describe("Activate Patient Menu", () => {
    it("Navigate to Patient Menu Page", () => {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage = adminPage.goToPatientConfigPage();
    });

    it("User turns on the toggle switch", function () {
      menuConfigPage.turnOnToggleSwitch();
      menuConfigPage.validateToggleStatus("On");
    });

    it("User checks the menu items and submits", function () {
      menuConfigPage.checkMenuItem("patient");
      menuConfigPage.checkMenuItem("addEditPatient");
      menuConfigPage.checkMenuItem("patientHistory");
      menuConfigPage.checkMenuItem("studyPatient");
      menuConfigPage.submitButton();
      cy.reload();
    });

    it("Verify menu changes", function () {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validatePatientMenuOn();
    });
  });
});
