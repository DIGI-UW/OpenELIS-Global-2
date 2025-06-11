import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let barcodePage = null;
let orderEntityPage = null;
let modifyOrderPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
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

  describe("Navigate to Home Page", ()=>{
    it("Add Site Name and Sample",()=>{
      barcode = homePage.goToBarcode();
      barcode.validatePage();
      cy.fixture("Order").then((order) => {
      barcode = orderEntityPage.enterSiteName(order.siteName);
      order.samples.forEach((sample) => {
      barcode = orderEntityPage.selectSampleTypeOption(sample.sampleType);
      });
    });
    barcodePage = orderEntityPage.checkPanelCheckBoxField();
    barcode.prePrint();
    });

    it("Add Accession Number and Submit",()=>{
      cy.fixture("Patient").then((patient) => {
      barcode = modifyOrderPage.enterAccessionNo(patient.labNo);
    });
    barcode.submitButton();
    });
  });
});
