import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let barcodePage = null;

before(() => {
  // Set viewport for consistent rendering
  cy.viewport(1280, 720);

  // Login flow
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Barcode configuration", function () {
  it("User navigates to Barcode Config", function () {
    barcodePage = adminPage.goToBarcodeConfigPage();

    // Assert the URL or presence of a key element
    cy.url().should("include", "#barcodeConfiguration");
    cy.get("h2").contains("Bar Code Configuration").should("be.visible"); // Change header text if needed
  });

  it("User adjusts the Default Bar Code Labels", function () {
    barcodePage.captureDefaultOrder();
    barcodePage.captureDefaultSpecimen();
  });

  it("User sets Maximum Bar Code Labels", function () {
    barcodePage.captureMaxOrder();
    barcodePage.captureMaxSpecimen();
  });

  it("User unchecks Optional Elements and Preprinted Bar Code Accession number", function () {
    barcodePage.uncheckCheckBoxes();
  });

  it("User adjusts Dimensions Bar Code Label", function () {
    barcodePage.dimensionsBarCodeLabel();
  });

  it("User rechecks the boxes", function () {
    barcodePage.checkCheckBoxes();
  });

  it("User saves changes", function () {
    barcodePage.saveChanges();
    barcodePage.verifySaveSuccess(); // This checks if the toast/alert message appears
  });
});
