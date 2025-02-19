import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let patientPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});
describe("Patient Search", function () {
  it("User Visits Home Page and goes to Add Add|Modify Patient Page", () => {
    homePage = loginPage.goToHomePage();
    patientPage = homePage.goToPatientEntry();
  });

  it("Add/Modify Patient page should appear with search field", function () {
    patientPage
      .getPatientEntryPageTitle()
      .should("contain.text", "Add Or Modify Patient");
  });

  it("External search button should be deactivated", function () {
    patientPage.getExternalSearchButton();
  });

  it("User should be able to navigate to create Patient tab", function () {
    patientPage.clickNewPatientTab();
    patientPage.getSubmitButton().should("be.visible");
  });

  it("User should enter patient Information", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPatientInfo(
        patient.firstName,
        patient.lastName,
        patient.subjectNumber,
        patient.nationalId,
        patient.DOB,
      );
    });
  });
  it("User should click save new patient information button", function () {
    patientPage.clickSavePatientButton();
    cy.wait(1000);
    cy.get("div[role='status']").should("be.visible");
    cy.wait(200).reload();
  });

  it("Validate Search Buttons visibility", function () {
    patientPage.searchForPatientButton().should("be.visible");
    patientPage.clickSearchPatientButton().should("be.visible");
  });

  it("Should be able to search patients By gender", function () {
    patientPage.getkMaleGenderRadioButton();
    patientPage.clickSearchPatientButton();
    cy.wait(500);
    cy.fixture("Patient").then((patient) => {
      patientPage.validatePatientByGender("M");
    });
    cy.wait(200).reload();
  });
  it("Should search Patient By FirstName only", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstNameOnly(patient.firstName);
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.firstName,
        "firstName",
      );
    });
    cy.wait(200).reload();
  });

  it("Should search Patient By LastName only", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByLastNameOnly(patient.lastName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.lastName,
        "lastName",
      );
    });
    cy.wait(200).reload();
  });

  it("Should search Patient By First and LastName", function () {
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
    cy.wait(200).reload();
  });
  it("should search patient By Date Of Birth", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByDateOfBirth(patient.DOB);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.DOB,
        "DOB",
      );
    });
    cy.wait(200).reload();
  });

  it("should search patient By Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientBylabNo(patient.labNo);
      cy.intercept(
        "GET",
        `**/rest/patient-search-results?*labNumber=${patient.labNo}*`,
      ).as("getPatientSearch");
      patientPage.clickSearchPatientButton();
      cy.wait("@getPatientSearch").then((interception) => {
        const responseBody = interception.response.body;
        console.log(responseBody);
        expect(responseBody.patientSearchResults).to.be.an("array").that.is
          .empty;
      });
    });
    cy.wait(200).reload();
  });

  it("should search patient By PatientId", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });
});
