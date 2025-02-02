import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";
import OrderEntityPage from "../pages/OrderEntityPage";

let homePage = null;
let loginPage = null;
let modifyOrderPage = null;
let patientPage = new PatientEntryPage();
let orderEntityPage = new OrderEntityPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Modify Order search by accession Number", function () {
  it("User Visits Home Page and goes to Modify Order Page ", function () {
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("User searches with accession number", () => {
    cy.wait(1000);
    cy.fixture("Order").then((order) => {
      modifyOrderPage.enterAccessionNo(order.labNo);
      modifyOrderPage.clickSubmitButton();
    });
  });

  it("should check for program selection button and go to next page ", function () {
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();
  });

  it("should be able to record", function () {
    modifyOrderPage.assignValues();
  });

  it("User should click next to go add order page and submit the order", function () {
    modifyOrderPage.clickNextButton();
    cy.wait(1000);
    modifyOrderPage.clickNextButton();
  });

  it("should be able to print barcode", function () {
    cy.window().then((win) => {
      cy.spy(win, "open").as("windowOpen");
    });
    modifyOrderPage.clickPrintBarcodeButton();
    cy.get("@windowOpen").should(
      "be.calledWithMatch",
      "/api/OpenELIS-Global/LabelMakerServlet?labNo=",
    );
  });
});

describe("Modify Order search by patient ", function () {
  it("User Visits Home Page and goes to Modify Order Page ", function () {
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("Should search Patient By First and LastName", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientLastName(patient.lastName);
    });
      patientPage.clickSearchBtn();
      patientPage.selectPatient();
      cy.wait(800);
      modifyOrderPage.clickNextButton();
  });

  it("User adds sample", function(){
    cy.fixture("Order").then((order) => {
      orderEntityPage.selectSampleTypeOption(order.sampleType);      
    });
    orderEntityPage.checkPanelCheckBoxField();
    modifyOrderPage.clickRejectSample();
    modifyOrderPage.rejectReason();
    //modifyOrderPage.clickNextButton();
  });
  
  it("Add Order", function () {
    orderEntityPage.generateLabOrderNumber();
    cy.fixture("Order").then((order) => {
      orderEntityPage.searchRequester(order.requester);
      cy.wait(500);
      orderEntityPage.enterSiteName(siteName);
    });
  });

  it("Result Reporting", function(){
    modifyOrderPage.checkPatientEmail();
    modifyOrderPage.checkRequesterSms();
    modifyOrderPage.clickSubmitButton();
  });

  //TO DO needs fixing
  it("Should be able to search by respective patient ", function () {
    cy.wait(1000);
    modifyOrderPage.clickRespectivePatient();
  });
  it("should check for program selection button and go to next page ", function () {
    cy.wait(1000);
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();
  });

  it("should be able to record", function () {
    cy.wait(1000);
    modifyOrderPage.assignValues();
  });

  it("User should click next to go add order page and submit the order", function () {
    modifyOrderPage.clickNextButton();
    cy.wait(1000);
    modifyOrderPage.clickNextButton();
  });

  it("should be able to print barcode", function () {
    cy.window().then((win) => {
      cy.spy(win, "open").as("windowOpen");
    });
    modifyOrderPage.clickPrintBarcodeButton();
    cy.get("@windowOpen").should(
      "be.calledWithMatch",
      "/api/OpenELIS-Global/LabelMakerServlet?labNo=",
    );
  });
});
