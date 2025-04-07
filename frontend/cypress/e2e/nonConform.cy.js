import LoginPage from "../pages/LoginPage";

describe("Report Non-Conforming Event", function () {
  let homePage;
  let loginPage;
  let nonConform;

  beforeEach(() => {
    // Set up fresh environment for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    nonConform = homePage.goToReportNCE();

    // Verify page loaded
    nonConform
      .getReportNonConformTitle()
      .should("contain.text", "Report Non-Conforming Event (NCE)");
  });

  it("Should report NCE by Last Name", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Last Name");
      nonConform.enterSearchField(patient.lastName);
      nonConform.clickSearchButton();
      nonConform.clickCheckbox({ force: true });
      nonConform.clickGoToNceFormButton();

      // Enter NCE details
      cy.fixture("NonConform").then((nonConformData) => {
        nonConform.enterStartDate(nonConformData.dateOfEvent);
        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );
        nonConform.submitForm();
      });
    });
  });

  it("Should report NCE by First Name", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("First Name");
      nonConform.enterSearchField(patient.firstName);
      nonConform.clickSearchButton();
      nonConform.clickCheckbox({ force: true });
      nonConform.clickGoToNceFormButton();

      // Enter NCE details
      cy.fixture("NonConform").then((nonConformData) => {
        nonConform.enterStartDate(nonConformData.dateOfEvent);
        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );
        nonConform.submitForm();
      });
    });
  });

  it("Should report NCE by PatientID", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Patient Identification Code");
      nonConform.enterSearchField(patient.nationalId);
      nonConform.clickSearchButton();
      nonConform.clickCheckbox({ force: true });
      nonConform.clickGoToNceFormButton();

      // Save NCE number for later reference
      nonConform.getAndSaveNceNumber();

      // Enter NCE details
      cy.fixture("NonConform").then((nonConformData) => {
        nonConform.enterStartDate(nonConformData.dateOfEvent);
        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );
        nonConform.submitForm();
      });
    });
  });

  it("Should report NCE by Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.enterSearchField(patient.labNo);
      nonConform.clickSearchButton();
    });
  });
});

describe("View New Non-Conforming Event", function () {
  let homePage;
  let loginPage;
  let nonConform;

  beforeEach(() => {
    // Set up fresh environment for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    nonConform = homePage.goToViewNCE();

    // Verify page loaded
    nonConform
      .getViewNonConformTitle()
      .should("contain.text", "View New Non Conform Event");
  });

  it("Should view and update NCE by Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Lab Number");
      nonConform.enterSearchField(patient.labNo);
      nonConform.clickSearchButton();
      nonConform.clickRadioButtonNCE();

      // Enter NCE details
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

  it("Should view and update NCE by NCE Number", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.selectSearchType("NCE Number");
      nonConform.enterSearchField(nce.NceNumber);
      nonConform.clickSearchButton();
      cy.wait(1000);

      // Enter NCE details
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
  let homePage;
  let loginPage;
  let nonConform;

  beforeEach(() => {
    // Set up fresh environment for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    nonConform = homePage.goToCorrectiveActions();

    // Verify page loaded
    nonConform
      .getViewNonConformTitle()
      .should("contain.text", "Nonconforming Events Corrective Action");
  });

  it("Should search by Lab Number and add corrective actions", function () {
    cy.fixture("Patient").then((patient) => {
      nonConform.selectSearchType("Lab Number");
      nonConform.enterSearchField(patient.labNo);
      nonConform.clickSearchButton();
      nonConform.clickRadioButtonNCE();

      // Enter discussion details
      cy.fixture("NonConform").then((nce) => {
        nonConform.enterDiscussionDate(nce.dateOfEvent);
        nonConform.selectActionType();
        nonConform.checkResolution();
        nonConform.enterDateCompleted(nce.dateOfEvent);
        nonConform.enterProposedCorrectiveAction(nce.proposedCorrectiveAction);
        nonConform.enterDateCompleted0(nce.dateOfEvent);
        nonConform.clickSubmitButton();
      });
    });
  });

  it("Should search by NCE Number and add corrective actions", function () {
    cy.fixture("NonConform").then((nce) => {
      nonConform.selectSearchType("NCE Number");
      nonConform.enterSearchField(nce.NceNumber);
      nonConform.clickSearchButton();

      // Enter discussion details
      nonConform.enterDiscussionDate(nce.dateOfEvent);
      nonConform.selectActionType();
      nonConform.checkResolution();
      nonConform.enterDateCompleted(nce.dateOfEvent);
      nonConform.enterProposedCorrectiveAction(nce.proposedCorrectiveAction);
      nonConform.enterDateCompleted0(nce.dateOfEvent);
      nonConform.clickSubmitButton();
    });
  });
});
