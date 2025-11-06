import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let loginPage = null;
let batchOrder = null;
let adminPage = new AdminPage();

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
    const data = this.batchOrderData;
    batchOrder.selectForm(data.formTypeRoutine);
    batchOrder.selectSampleType(data.serumSample);
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
    const data = this.batchOrderData;
    batchOrder.selectMethod(data.methodOnDemand);
    batchOrder.checkFacilityCheckbox();
    batchOrder.checkPatientCheckbox();
    batchOrder.enterSiteName(data.siteName);
    batchOrder.checkNextButtonEnabled();
  });

  it("User adds New Patient", function () {
    batchOrder.clickNewPatientButton();
    const data = this.batchOrderData;
    batchOrder.uniqueHealthIDNum(data.healthID);
    batchOrder.nationalID(data.nationalID);
    batchOrder.firstName(data.firstName);
    batchOrder.lastName(data.lastName);
    batchOrder.typePatientYears(data.years);
    batchOrder.typePatientMonths(data.months);
    batchOrder.typePatientDays(data.days);
    batchOrder.selectGender(); //female in this case
  });
  //Save button is lacking and needs to be added for this test to work
  //it("User should click save new patient information button", function () {
  // batchOrder.clickSavePatientButton();
  //});

  it("Generate BarCode", function () {
    const data = this.batchOrderData;
    batchOrder.typeLabNumber(data.labNumber);
    batchOrder.clickGenerateAndSaveBarcode();
    batchOrder.checkNextLabel().should("be.visible");
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
    const data = this.batchOrderData;
    batchOrder.selectForm(data.formTypeEID);
    batchOrder.selectDNAPCRTest();
    batchOrder.selectTubeSample();
    batchOrder.selectBloodSample();
  });

  it("User Selects Methods, Site Name and Move to Next Page", function () {
    const data = this.batchOrderData;
    batchOrder.selectMethod(data.methodPrePrinted);
    batchOrder.checkFacilityCheckbox();
    batchOrder.checkPatientCheckbox();
    batchOrder.enterSiteName(data.siteName);
    batchOrder.checkNextButtonEnabled();
  });

  it("User Searches for Existing Patient", function () {
    batchOrder.clickSearchPatientButton();
    const data = this.batchOrderData;
    batchOrder.lastName(data.lastName);
    batchOrder.firstName(data.firstName);
    batchOrder.localSearchButton();
    batchOrder.checkPatientRadio(); //the first on the list
  });

  it("Should Visit Batch Order Entry Page", function () {
    batchOrder.visitBatchOrderEntryPage();
  });

  it(" User enters Lab Number and Generates Barcode", function () {
    const data = this.batchOrderData;
    batchOrder.typeLabNumber(data.labNumber);
    batchOrder.visitBatchOrderEntryPage();
    batchOrder.clickGenerateButton();
    batchOrder.saveOrder();
  });
  it("User clicks the finish button", function () {
    batchOrder.clickFinishButton();
  });
});
