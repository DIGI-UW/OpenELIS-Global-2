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
    cy.fixture("result").then((res) => {
      result.acceptSample();
      result.expandSampleDetails();
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
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.getLastName().should("not.have.value", patient.inValidName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
    cy.reload();
  });

  it("should search patient By PatientId and validate", function () {
    cy.wait(500);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
    cy.reload();
  });

  it("should search patient By Lab Number and validate", function () {
    cy.wait(500);
    cy.fixture("EnteredOrder").then((patient) => {
      cy.get("#labNumber").type(patient.labNo);
      patientPage.clickSearchPatientButton();
    });
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
      cy.wait(2000); // Use only if absolutely necessary, prefer intercept-based waits

      // Ensure the input field is visible before typing
      cy.get("#accessionNumber")
        .should("be.visible")
        .clear() // Clear any existing text (optional)
        .type(order.labNo);

      // Ensure the submit button is visible and clickable before clicking
      cy.get(":nth-child(4) > #submit")
        .should("be.visible")
        .and("not.be.disabled")
        .click();
    });
  });

  it("should accept the sample and save the result", function () {
    cy.fixture("result").then((res) => {
      // Ensure the page has loaded properly before interacting
      cy.wait(2000); // You can replace this with intercept-based wait

      // Accept the sample
      result.acceptSample();

      // Ensure the sample details can be expanded before interacting
      cy.get("[data-testid='expander-button-0']")
        .should("be.visible")
        .click({ force: true }); // Forces click if the element is hidden

      // Select the test method (Ensure dropdown/options are loaded)
      cy.get("selector-for-test-method-dropdown").should("be.visible"); // Replace with correct selector
      result.selectTestMethod(0, res.stainTestMethod);

      // Submit the results (Ensure button is enabled before clicking)
      cy.get("selector-for-submit-button") // Replace with correct selector
        .should("be.visible")
        .and("not.be.disabled")
        .click();
    });
  });
});

describe("Result By Referred Out Tests", function () {
  before("Navigate to Result By Referred Out Tests", function () {
    cy.session("login", () => {
      loginPage.goToHomePage().then((home) => {
        homePage = home;
        homePage.goToResultsForRefferedOut().then((res) => {
          result = res;
        });
      });
    });
  });

  it("User visits Reffered out Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle().should("contain.text", res.referralPageTitle);
    });
  });

  it("should search Referrals By Patient and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
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
