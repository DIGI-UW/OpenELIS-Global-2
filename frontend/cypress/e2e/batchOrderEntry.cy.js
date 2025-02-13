import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let batchOrder = null;

const navigateToBatchOrderEntryPage = () => {
  homePage = loginPage.goToHomePage();
  batchOrder = homePage.goToBatchOrderEntry();
};

before(() => {
  cy.fixture("BatchOrder").as("batchOrderData");
});

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Batch Order Entry On Demand and Serum form type", function () {
  before("navigate to Batch Order Entry Page", function () {
    navigateToBatchOrderEntryPage();
  });

  it("User visits Batch Order Entry Setup Page", function () {
    batchOrder.visitSetupPage();
    batchOrder.checkNextButtonDisabled();
  });

  it("User selects Routine Form and Serum Sample", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.selectForm(batchOrderData.formTypeRoutine);
      batchOrder.selectSampleType(batchOrderData.serumSample);
    });
  });

  it("User checks Panels and Tests", function () {
    batchOrder.checkBilanPanel();
    batchOrder.checkSerologiePanel();
    //tests picked at random
    batchOrder.checkDenguePCR();
    batchOrder.checkHIVViralLoad();
    batchOrder.checkCreatinine();
  });

  it("Should Select Methods, Site Name and Move to Next Page", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.selectMethod(batchOrderData.methodOnDemand);
      batchOrder.checkFacilityCheckbox();
      batchOrder.checkPatientCheckbox();
      batchOrder.enterSiteName(batchOrderData.siteName);
      batchOrder.checkNextButtonEnabled();
    });
  });

  it("User adds New Patient", function () {
    batchOrder.clickNewPatientButton();
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.uniqueHealthIDNum(batchOrderData.healthID);
      batchOrder.nationalID(batchOrderData.nationalID);
      batchOrder.firstName(batchOrderData.firstName);
      batchOrder.lastName(batchOrderData.lastName);
      batchOrder.typePatientYears(batchOrderData.years);
      batchOrder.typePatientMonths(batchOrderData.months);
      batchOrder.typePatientDays(batchOrderData.days);
      batchOrder.selectGender(); //female in this case
    });
  });
  //Save button is lacking and needs to be added for this test to work
  //it("User should click save new patient information button", function () {
  // batchOrder.clickSavePatientButton();
  //});

  it("Generate BarCode", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.typeLabNumber(batchOrderData.labNumber);
      batchOrder.clickGenerateAndSaveBarcode();
      batchOrder.checkNextLabel().should("be.visible");
    });
  });

  it("User clicks the finish button", function () {
    batchOrder.clickFinishButton();
  });
});
describe("Batch Order Entry Pre Printed and EID form type", function () {
  before("navigate to Batch Order Entry Page", function () {
    navigateToBatchOrderEntryPage();
  });

  it("User visits Batch Order Entry Setup Page", function () {
    batchOrder.visitSetupPage();
    batchOrder.checkNextButtonDisabled();
  });

  it("User selects EID form, samples and test", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.selectForm(batchOrderData.formTypeEID);
      batchOrder.selectDNAPCRTest();
      batchOrder.selectTubeSample();
      batchOrder.selectBloodSample();
    });
  });

  it("User Selects Methods, Site Name and Move to Next Page", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.selectMethod(batchOrderData.methodPrePrinted);
      batchOrder.checkFacilityCheckbox();
      batchOrder.checkPatientCheckbox();
      batchOrder.enterSiteName(batchOrderData.siteName);
      batchOrder.checkNextButtonEnabled();
    });
  });

  it("User Searches for Existing Patient", function () {
    batchOrder.clickSearchPatientButton();
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.lastName(batchOrderData.lastName);
      batchOrder.firstName(batchOrderData.firstName);
      batchOrder.localSearchButton();
      batchOrder.checkPatientRadio(); //the first on the list
    });
  });

  it("Should Visit Batch Order Entry Page", function () {
    batchOrder.visitBatchOrderEntryPage();
  });

  it(" User enters Lab Number and Generates Barcode", function () {
    cy.fixture("BatchOrder").then((batchOrderData) => {
      batchOrder.typeLabNumber(batchOrderData.labNumber);
      batchOrder.visitBatchOrderEntryPage();
      batchOrder.clickGenerateButton();
      batchOrder.saveOrder();
    });
  });
  it("User clicks the finish button", function () {
    batchOrder.clickFinishButton();
  });
});
