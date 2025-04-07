import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let patientPage = null;

describe("Patient Entry Tests", () => {
  beforeEach(() => {
    // Setup before each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    patientPage = homePage.goToPatientEntry();
  });

  describe("Add New Patient", () => {
    it("should display Add|Modify Patient page with search field", () => {
      patientPage
        .getPatientEntryPageTitle()
        .should("contain.text", "Add Or Modify Patient");
    });

    it("should have external search button deactivated", () => {
      patientPage.getExternalSearchButton();
    });

    it("should navigate to create Patient tab and show submit button", () => {
      patientPage.clickNewPatientTab();
      patientPage.getSubmitButton().should("be.visible");
    });

    it("should allow entering and clearing patient information", () => {
      patientPage.clickNewPatientTab();
      cy.fixture("Patient").then((patient) => {
        patientPage.enterPatientInfo(
          patient.firstName,
          patient.lastName,
          patient.subjectNumber,
          patient.nationalId,
          patient.DOB,
        );
      });
      patientPage.clearPatientInfo();
    });

    it("should save new patient information", () => {
      patientPage.clickNewPatientTab();
      cy.fixture("Patient").then((patient) => {
        patientPage.enterPatientInfo(
          patient.firstName,
          patient.lastName,
          patient.subjectNumber,
          patient.nationalId,
          patient.DOB,
        );
      });
      patientPage.clickSavePatientButton();
      cy.get("div[role='status']").should("be.visible");
    });
  });

  describe("Search Patient", () => {
    it("should search patients by gender", () => {
      patientPage.getMaleGenderRadioButton();
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientByGender("M");
    });

    it("should search patient by first name only", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByFirstNameOnly(patient.firstName);
        patientPage.getFirstName().should("have.value", patient.firstName);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTablebyRespectiveField(
          patient.firstName,
          "firstName",
        );
      });
    });

    it("should search patient by last name only", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByLastNameOnly(patient.lastName);
        patientPage.getLastName().should("have.value", patient.lastName);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTablebyRespectiveField(
          patient.lastName,
          "lastName",
        );
      });
    });

    it("should search patient by both names", () => {
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
    });

    it("should search patient by date of birth", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByDateOfBirth(patient.DOB);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTablebyRespectiveField(
          patient.DOB,
          "DOB",
        );
      });
    });

    it("should search patient by lab number and get empty results", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientBylabNo(patient.labNo);
        cy.intercept(
          "GET",
          `**/rest/patient-search-results?*labNumber=${patient.labNo}*`,
        ).as("getPatientSearch");
        patientPage.clickSearchPatientButton();
        cy.wait("@getPatientSearch").then((interception) => {
          const responseBody = interception.response.body;
          expect(responseBody.patientSearchResults).to.be.an("array").that.is
            .empty;
        });
      });
    });

    it("should search patient by patient ID", () => {
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
});
