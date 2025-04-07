import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let reportPage = null;

describe("Reports Tests", () => {
  beforeEach(() => {
    // Setup before each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
  });

  describe("Routine Reports", () => {
    beforeEach(() => {
      reportPage = homePage.goToRoutineReports();
      reportPage.visitRoutineReports();
    });

    it("should display Patient Status Report with proper forms", () => {
      reportPage.selectPatientStatusReport();
      reportPage.validatePageHeader("Patient Status Report");

      reportPage.toggleAccordionPatient(2);
      reportPage.validateFieldVisibility("#patientId");
      reportPage.validateFieldVisibility("#local_search");
      reportPage.toggleAccordionPatient(2);

      reportPage.toggleAccordion(3);
      reportPage.validateFieldVisibility("#from");
      reportPage.validateFieldVisibility("#to");
      reportPage.toggleAccordion(3);

      reportPage.toggleAccordion(6);
      reportPage.validateFieldVisibility('[data-cy="dateTypeDropdown"]');
      reportPage.validateFieldVisibility(
        ".cds--date-picker-input__wrapper > #startDate",
      );
      reportPage.validateButtonVisible(
        ":nth-child(7) > :nth-child(2) > .cds--btn",
      );
    });

    it("should display Statistics Reports with proper checkboxes", () => {
      reportPage.aggregateReports();
      reportPage.selectStatistics();
      reportPage.validatePageHeader("Statistics Report");

      reportPage.checkAllCheckboxes(
        2,
        11,
        ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
      );
      reportPage.validateFieldVisibility(
        ':nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) input[type="checkbox"]',
      );
    });

    it("should uncheck 'All' checkbox if any individual checkbox is unchecked", () => {
      reportPage.aggregateReports();
      reportPage.selectStatistics();

      reportPage.checkAllCheckboxes(
        2,
        11,
        ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
      );
      reportPage.validateAllCheckBox("not.be.checked");
      reportPage.uncheckCheckbox(
        2,
        ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
      );
      reportPage.validateAllCheckBox("not.be.checked");
    });

    it("should check/uncheck 'All' checkbox for priority", () => {
      reportPage.aggregateReports();
      reportPage.selectStatistics();

      reportPage.checkAllCheckboxes(2, 6, ".inlineDiv");
      reportPage.uncheckCheckbox(2, ".inlineDiv");
      reportPage.validateButtonVisible(
        ":nth-child(3) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) > .cds--checkbox-label",
      );
    });

    it("should display Summary of all tests", () => {
      reportPage.aggregateReports();
      reportPage.allReportsSummary();
      reportPage.validatePageHeader("Test Report Summary");
      reportPage.validateButtonDisabled(
        ".cds--form > :nth-child(3) > .cds--btn",
      );
      reportPage.typeInDatePicker(
        ".cds--date-picker-input__wrapper > #startDate",
        "01/02/2023",
      );
      reportPage.validateButtonVisible(
        ".cds--form > :nth-child(3) > .cds--btn",
      );
    });

    it("should display HIV Test Summary", () => {
      reportPage.aggregateReports();
      reportPage.summaryTestHIV();
      reportPage.validateButtonDisabled(
        ".cds--form > :nth-child(3) > .cds--btn",
      );
      reportPage.typeInDatePicker(
        ".cds--date-picker-input__wrapper > #startDate",
        "01/02/2023",
      );
      reportPage.validateButtonVisible(
        ".cds--form > :nth-child(3) > .cds--btn",
      );
    });
  });

  describe("Study Reports", () => {
    beforeEach(() => {
      reportPage = homePage.goToStudyReports();
    });

    it("should visit ARV Initial Version 1", () => {
      reportPage.visitARVInitialVersion1();
    });

    it("should visit ARV Initial Version 2", () => {
      reportPage.visitARVInitialVersion2();
    });

    it("should visit ARV Follow-Up Version 1", () => {
      reportPage.visitARVFollowUpVersion1();
    });

    it("should visit ARV Follow-Up Version 2", () => {
      reportPage.visitARVFollowUpVersion2();
    });

    it("should visit EID Version 1", () => {
      reportPage.visitEIDVersion1();
    });

    it("should visit VL Version", () => {
      reportPage.visitVLVersion();
    });

    it("should visit Audit Trail Report and validate", () => {
      reportPage.visitAuditTrailReport();
      reportPage.verifyHeaderText("section > h3", "Audit Trail");
      cy.fixture("Patient").then((order) => {
        reportPage.typeInField("labNo", order.labNo);
      });
    });
  });
});
