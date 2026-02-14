import LoginPage from "../../pages/LoginPage";
import OrderEntityPage from "../../pages/OrderEntityPage";
import ModifyOrderPage from "../../pages/ModifyOrderPage";
import BarcodeConfigPage from "../../pages/BarcodeConfigPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let barcodePage = new BarcodeConfigPage();
let orderEntityPage = new OrderEntityPage();
let modifyOrderPage = new ModifyOrderPage();

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Barcode configuration", function () {
  it("User Navigates to Barcode Config", function () {
    barcodePage = adminPage.goToBarcodeConfigPage();
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

  it("Check the boxes", function () {
    barcodePage.checkCheckBoxes();
  });

  it("Save Changes", function () {
    barcodePage.saveChanges();
  });

  it("Navigate to Barcode Page", function () {
    barcodePage = homePage.goToBarcode();
    barcodePage.validatePage();
  });

  it("Add Site Name and Sample", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
    });
    barcodePage.selectSampleTypeOption("Serum");
  });

  it("Check Panels and Pre-Print Labels", function () {
    orderEntityPage.checkPanelCheckBoxField();
    barcodePage.clickPrePrintButton();
  });

  it("Add Accession Number and Submit", function () {
    cy.fixture("Patient").then((patient) => {
      modifyOrderPage.enterAccessionNo(patient.labNo);
    });
    barcodePage.clickSubmitButton();
  });
});
