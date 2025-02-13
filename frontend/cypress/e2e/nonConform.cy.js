import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let nonConform = null;
let skipBeforeEach = false;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Report Non-Conforming Event", function () {
  beforeEach("navigate to Report Non-Conforming Event Page", function () {
    if (!skipBeforeEach) {
      homePage = loginPage.goToHomePage();
      nonConform = homePage.goToReportNCE();
    }
  });

  it("User visits Report Non-Conforming Event Page", function () {
    nonConform
      .getReportNonConformTitle()
      .should("contain.text", "Report Non-Conforming Event (NCE)");
  });

  it("Should Search by Last Name and Validate the results", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Last Name");
      nonConform.enterSearchField(patient.lastName);
      nonConform.clickSearchButton();
      cy.fixture("EnteredOrder").then((order) => {
        nonConform.validateSearchResult(order.labNo);
      });
    });
  });

  it("Should Search by First Name and Validate the results", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("First Name");
      nonConform.enterSearchField(patient.firstName);
      nonConform.clickSearchButton();
      cy.fixture("EnteredOrder").then((order) => {
        nonConform.validateSearchResult(order.labNo);
      });
    });
  });

  it("Should Search by PatientID and Validate the results", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Patient Identification Code");
      nonConform.enterSearchField(patient.nationalId);
      nonConform.clickSearchButton();
      cy.fixture("EnteredOrder").then((order) => {
        nonConform.validateSearchResult(order.labNo);
      });
    });
  });

  it("Should Search by Lab Number ", function () {
    cy.fixture("EnteredOrder").then((order) => {
      nonConform.selectSearchType("Lab Number");
      nonConform.enterSearchField(order.labNo);
      nonConform.clickSearchButton();
      nonConform.validateSearchResult(order.labNo);
      nonConform.clickCheckbox({ force: true });
      nonConform.clickGoToNceFormButton();
    });

    nonConform.getAndSaveNceNumber();
    skipBeforeEach = true;
  });

  it("Should enter the details and Submit the NCE Reporting Form", function () {
    cy.fixture("NonConform").then((nonConformData) => {
      nonConform.enterStartDate(nonConformData.dateOfEvent);
      nonConform.enterReportingUnit(nonConformData.reportingUnit);
      nonConform.enterDescription(nonConformData.description);
      nonConform.enterSuspectedCause(nonConformData.suspectedCause);
      nonConform.enterCorrectiveAction(nonConformData.proposedCorrectiveAction);
      nonConform.submitForm();
    });
  });
});

describe("View New Non-Conforming Event", function () {
  let skipBeforeEach = false;

  beforeEach("navigate to View New Non-Conforming Event Page", function () {
    if (!skipBeforeEach) {
      homePage = loginPage.goToHomePage();
      nonConform = homePage.goToViewNCE();
    }
  });

  it("User visits View Non-Conforming Event Page", function () {
    nonConform
      .getViewNonConformTitle()
      .should("contain.text", "View New Non Conform Event");
  });
  it("Should Search by Lab Number and Validate the results", function () {
    cy.fixture("EnteredOrder").then((order) => {
      nonConform.selectSearchType("Lab Number");
      nonConform.enterSearchField(order.labNo);
      nonConform.clickSearchButton();
      //nonConform.clickRadioButtonNCE();
      nonConform.validateLabNoSearchResult(order.labNo);
    });
  });

  it("Should Search by Lab Number and Validate the results", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.selectSearchType("NCE Number");
      nonConform.enterSearchField(nce.NceNumber);
      nonConform.clickSearchButton();
      nonConform.validateNCESearchResult(nce.NceNumber);
    });
    skipBeforeEach = true;
  });

  it("Should Enter The details and Submit it", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.enterNceCategory(nce.nceCategory);
      nonConform.enterNceType(nce.nceType);
      nonConform.enterConsequences(nce.consequences);
      nonConform.enterRecurrence(nce.recurrence);
      nonConform.enterLabComponent(nce.labComponent);
      nonConform.enterDescriptionAndComments(nce.test);
      nonConform.submitForm();
    });
  });
});

describe("Corrective Actions", function () {
  let skipBeforeEach = false;

  beforeEach("navigate Corrective Action Page", function () {
    if (!skipBeforeEach) {
      homePage = loginPage.goToHomePage();
      nonConform = homePage.goToCorrectiveActions();
    }
  });

  it("User visits Corrective Actions Page", function () {
    nonConform
      .getViewNonConformTitle()
      .should("contain.text", "Nonconforming Events Corrective Action");
  });
  it("Should Search by Lab Number and Validate the results", function () {
    cy.fixture("EnteredOrder").then((order) => {
      nonConform.selectSearchType("Lab Number");
      nonConform.enterSearchField(order.labNo);
      nonConform.clickSearchButton();
      //nonConform.clickRadioButtonNCE();
      nonConform.validateLabNoSearchResult(order.labNo);
    });
  });

  it("Should Search by NCE Number and Validate the results", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.selectSearchType("NCE Number");
      nonConform.enterSearchField(nce.NceNumber);
      nonConform.clickSearchButton();
      nonConform.validateNCESearchResult(nce.NceNumber);
    });
    skipBeforeEach = true;
  });

  it("Should enter the discussion details and submit", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.enterDiscussionDate(nce.dateOfEvent);
      nonConform.enterProposedCorrectiveAction(nce.proposedCorrectiveAction);
      nonConform.enterDateCompleted(nce.dateOfEvent);
      nonConform.selectActionType();
      nonConform.selectResolution();
      nonConform.enterDateCompleted0(nce.dateOfEvent);
      nonConform.clickSubmitButton();
    });
  });
});
