import LoginPage from "../pages/LoginPage";

describe("Batch Order Entry Tests", function () {
  let loginPage, homePage, batchOrder;

  beforeEach(() => {
    cy.fixture("BatchOrder").as("batchOrderData");
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
  });

  describe("On Demand and Serum form type", function () {
    beforeEach(() => {
      batchOrder = homePage.goToBatchOrderEntry();
      batchOrder.visitSetupPage();
    });

    it("Can complete the full batch order entry workflow for Routine Form and Serum Sample", function () {
      // Step 1: Initial setup
      batchOrder.checkNextButtonDisabled();

      // Step 2: Select form and sample
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.selectForm(batchOrderData.formTypeRoutine);
        batchOrder.selectSampleType(batchOrderData.serumSample);
      });

      // Step 3: Check panels and tests
      batchOrder.checkBilanPanel();
      batchOrder.checkSerologiePanel();
      batchOrder.checkDenguePCR();
      batchOrder.checkHIVViralLoad();
      batchOrder.checkCreatinine();

      // Step 4: Select methods and move to next page
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.selectMethod(batchOrderData.methodOnDemand);
        batchOrder.checkFacilityCheckbox();
        batchOrder.checkPatientCheckbox();
        batchOrder.enterSiteName(batchOrderData.siteName);
        batchOrder.checkNextButtonEnabled();
        batchOrder.clickNextButton(); // Add this method to your page object if it doesn't exist
      });

      // Step 5: Add new patient
      batchOrder.clickNewPatientButton();
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.uniqueHealthIDNum(batchOrderData.healthID);
        batchOrder.nationalID(batchOrderData.nationalID);
        batchOrder.firstName(batchOrderData.firstName);
        batchOrder.lastName(batchOrderData.lastName);
        batchOrder.typePatientYears(batchOrderData.years);
        batchOrder.typePatientMonths(batchOrderData.months);
        batchOrder.typePatientDays(batchOrderData.days);
        batchOrder.selectGender();
      });

      // Step 6: Generate barcode and finish
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.typeLabNumber(batchOrderData.labNumber);
        batchOrder.clickGenerateAndSaveBarcode();
        batchOrder.checkNextLabel().should("be.visible");
        batchOrder.clickFinishButton();
      });
    });
  });

  describe("Pre Printed and EID form type", function () {
    beforeEach(() => {
      batchOrder = homePage.goToBatchOrderEntry();
      batchOrder.visitSetupPage();
    });

    it("Can complete the full batch order entry workflow for EID form and pre-printed method", function () {
      // Step 1: Initial setup
      batchOrder.checkNextButtonDisabled();

      // Step 2: Select form, samples and test
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.selectForm(batchOrderData.formTypeEID);
        batchOrder.selectDNAPCRTest();
        batchOrder.selectTubeSample();
        batchOrder.selectBloodSample();
      });

      // Step 3: Select methods and move to next page
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.selectMethod(batchOrderData.methodPrePrinted);
        batchOrder.checkFacilityCheckbox();
        batchOrder.checkPatientCheckbox();
        batchOrder.enterSiteName(batchOrderData.siteName);
        batchOrder.checkNextButtonEnabled();
        batchOrder.clickNextButton(); // Add this method to your page object if it doesn't exist
      });

      // Step 4: Search for existing patient
      batchOrder.clickSearchPatientButton();
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.lastName(batchOrderData.lastName);
        batchOrder.firstName(batchOrderData.firstName);
        batchOrder.localSearchButton();
        batchOrder.checkPatientRadio();
      });

      // Step 5: Visit batch order entry page and generate barcode
      batchOrder.visitBatchOrderEntryPage();
      cy.get("@batchOrderData").then((batchOrderData) => {
        batchOrder.typeLabNumber(batchOrderData.labNumber);
        batchOrder.visitBatchOrderEntryPage();
        batchOrder.clickGenerateButton();
        batchOrder.saveOrder();
        batchOrder.clickFinishButton();
      });
    });
  });
});
