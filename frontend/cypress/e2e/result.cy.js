import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import Result from "../pages/ResultsPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let result = null;
let patientPage = new PatientEntryPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Result By Unit", function () {
  before("navigate to Result By Unit", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByUnit();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.pageTitle);
    });
  });

  it("Should Search by Unit", function () {
    cy.fixture("workplan").then((order) => {
      result.selectUnitType(order.unitType);
    });
  });

  it("should accept the sample, refer the sample, and save the result", function () {
    result.acceptSample();
    result.expandSampleDetails();
    cy.fixture("result").then((res) => {
      result.selectTestMethod(0, res.pcrTestMethod);
      cy.get(":nth-child(3) > .cds--form-item > .cds--checkbox-label").click();
      result.referSample(0, res.testNotPerformed, res.cedres);
      result.setResultValue(0, res.positiveResult);
      result.submitResults();
    });
  });
});

describe("Result By Patient", function () {
  before("navigate to Result By Patient", function () {
    result = homePage.goToResultsByPatient();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.pageTitle);
    });
  });

  it("Should search Patient By First and LastName and validate", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientLastName(patient.lastName);
    });
    patientPage.clickSearchPatientBtn();
    cy.reload();
  });

  it("should search patient By PatientId and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.uniqueID);
    });
    patientPage.clickSearchPatientBtn();
    cy.reload();
  });

  it("should search patient By Lab Number and validate", function () {
    cy.wait(500);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientBylabNo(patient.labNo);
    });
    patientPage.clickSearchPatientBtn();
    cy.reload();
  });

  it("Should be able to search by respective patient and accept the result", function () {
    cy.wait(1000);
    result.selectPatient();
    cy.fixture("result").then((res) => {
      result.acceptResult();
      result.expandSampleDetails();
      result.selectTestMethod(0, res.stainTestMethod);
      result.submitResults();
    });
  });
});

describe("Result By Order", function () {
  before("navigate to Result By Order", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByOrder();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.pageTitle);
    });
  });

  it("Should Search by Accession Number", function () {
    cy.fixture("EnteredOrder").then((order) => {
      cy.get("#accessionNumber").type(order.labNo);
    });
    cy.get(":nth-child(4) > #submit").click();
  });

  it("should accept the sample and save the result", function () {
    cy.fixture("result").then((res) => {
      result.acceptSample();
      result.expandSampleDetails();
      result.selectTestMethod(0, res.stainTestMethod);
      result.submitResults();
    });
  });
});

describe("Result By Referred Out Tests", function () {
  before("navigate to Result By Referred Out Tests", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsForRefferedOut();
  });

  it("User visits Reffered out Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.referralPageTitle);
    });
  });

  it("should search Referrals By Patient and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.uniqueID);
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientLastName(patient.lastName);
    });
    patientPage.clickSearchPatientBtn();
  });

  it("should click respective patient and search for referred out tests", function () {
    result.selectPatient();
    result.search();
  });

  it("should validate the results", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      result.validatePatientResult(patient);
    });
    cy.reload();
  });

  it("should search Referrals By Test Unit and validate", function () {
    cy.fixture("workplan").then((res) => {
      cy.get("#testnames-input").type(res.testName);
      cy.get("#testnames-item-0-item").click();
      cy.get(":nth-child(15) > .cds--btn").click({ force: true });
    });
    //update in UI elements
    // cy.fixture("result").then((res) => {
    //   cy.get("tbody > tr > :nth-child(8)").should(
    //     "contain.text",
    //     res.westernBlotHiv,
    //   );
    // });
    cy.reload();
  });

  it("should search Referrals By LabNumber and validate", function () {
    cy.fixture("EnteredOrder").then((order) => {
      cy.get("#labNumberInput").type(order.labNo);
    });
    cy.get(":nth-child(4) > .cds--lg\\:col-span-4 > .cds--btn")
      .should("be.visible")
      .click();
    //update in UI elements
    // cy.fixture("EnteredOrder").then((patient) => {
    //   cy.get("tbody > tr > :nth-child(3)").should(
    //     "contain.text",
    //     patient.labNo,
    //   );
    // });
  });
  //commented due to UI changes
  // it("should select the respecting referred test and print the selected patient reports", function () {
  //   result.selectRefferedTest();
  //   result.printReport();
  // });
});

describe("Result By Range Of Order", function () {
  before("navigate to Result By Range Of Order", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByRangeOrder();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.pageTitle);
    });
  });

  it("Should Enter Lab Number and perform Search", function () {
    cy.fixture("EnteredOrder").then((order) => {
      cy.get("#startLabNo").type(order.labNo);
    });
    cy.get(":nth-child(5) > #submit").click();
  });

  it("Should Accept And Save the result", function () {
    cy.wait(1000);
    cy.fixture("result").then((res) => {
      result.acceptSample();
      result.expandSampleDetails();
      result.selectTestMethod(0, res.eiaTestMethod);
      result.submitResults();
    });
  });
});

describe("Result By Test And Status", function () {
  before("navigate to Result By Test And Status", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByTestAndStatus();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.pageTitle);
    });
  });

  it("Should select testName, analysis status, and perform Search", function () {
    cy.fixture("workplan").then((order) => {
      result.selectTestName(order.testName);
    });
    cy.fixture("result").then((res) => {
      result.selectAnalysisStatus(res.acceptedStatus);
      result.searchByTest();
    });
  });

  it("Should Validate And accept the result", function () {
    cy.fixture("workplan").then((order) => {
      cy.get("#cell-testName-0 > .sampleInfo").should(
        "contain.text",
        order.testName,
      );
    });
    cy.fixture("result").then((res) => {
      result.acceptSample();
      result.expandSampleDetails();
      result.selectTestMethod(0, res.eiaTestMethod);
      result.submitResults();
    });
  });
});
