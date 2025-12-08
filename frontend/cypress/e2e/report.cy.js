import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let reportPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Routine Reports", function () {
  it("User Visits Routine Reports", function () {
    homePage = loginPage.goToHomePage();
    reportPage = homePage.goToRoutineReports();
  });
  it("User Visits Patient Status Report and checks for Respective Forms", () => {
    //reportPage.navigateToSection(1, 1);
    //reportPage.visitRoutineReports();
    reportPage.selectPatientStatusReport();
    reportPage.validatePageHeader("Patient Status Report");

    reportPage.toggleAccordionPatient(2);
    reportPage.validateFieldVisibility("#patientId");
    reportPage.validateFieldVisibility("#local_search");
    reportPage.toggleAccordionPatient(2);

    reportPage.toggleAccordion(3);
    // reportPage.validateFieldVisibility("#from");
    // reportPage.validateFieldVisibility("#to");
    reportPage.toggleAccordion(3);

    reportPage.toggleAccordion(6);
    reportPage.validateFieldVisibility('[data-cy="dateTypeDropdown"]');
    reportPage.validateFieldVisibility(
      ".cds--date-picker-input__wrapper > #startDate",
    );
    reportPage.validateButtonVisible("[data-cy='printableVersion']");
  });

  it("Should Visit Statistics Reports", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    //reportPage.navigateToSection(2, 1);
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

  it('Within Statistic Should uncheck the "All" checkbox if any individual checkbox is unchecked', () => {
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
    reportPage.validateFieldVisibility(
      ':nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) input[type="checkbox"]',
    );
  });

  it('Should check/uncheck "All" checkbox for priority', () => {
    reportPage.checkAllCheckboxes(2, 6, ".inlineDiv");
    reportPage.uncheckCheckbox(2, ".inlineDiv");
    reportPage.validateButtonVisible(
      ":nth-child(3) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) > .cds--checkbox-label",
    );
  });

  it("should check for print button", () => {
    reportPage.validateButtonVisible(":nth-child(6) > .cds--btn");
  });

  it("Visits Summary of all tests", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.aggregateReports();
    reportPage.allReportsSummary();
    reportPage.validatePageHeader("Test Report Summary");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits HIV Test Summary and validates", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.aggregateReports();
    reportPage.summaryTestHIV();
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Rejection Report and validates", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.selectRejectionReport();
    reportPage.validatePageHeader("Rejection Report");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Activity Report By Test Type", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.navigateToReportsActivity();
    reportPage.selectByTestType();
    reportPage.validatePageHeader("Activity report By test");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.selectDropdown("#type", "Amylase(Serum)");
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Activity Report By Panel Type", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.navigateToReportsActivity();
    reportPage.selectByPanel();
    reportPage.validatePageHeader("Activity report By Panel");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.selectDropdown("#type", "NFS");
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Activity Report By Unit", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.navigateToReportsActivity();
    reportPage.selectByUnit();
    reportPage.validatePageHeader("Activity report By Test Section");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.selectDropdown("#type", "Biochemistry");
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Referred Out Test Report", () => {
    reportPage = homePage.goToRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.selectReferredOutTestReport();
    reportPage.validatePageHeader("External Referrals Report");
    //reportPage.selectDropdownExt();
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );

    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #endDate",
      "02/02/2023",
    );
    reportPage.validateButtonVisible(":nth-child(4) > .cds--btn");
  });

  it("Visits Non Conformity Report By Date", () => {
    reportPage = homePage.goToRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.navigateToNCReports();
    reportPage.selectNCReportByDate();
    reportPage.validatePageHeader("Non ConformityReport by Date");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Non Conformity Report By Unit and Reason", () => {
    reportPage = homePage.goToRoutineReports();
    reportPage.navigateToManagementReports();
    reportPage.navigateToNCReports();
    reportPage.selectNCReportByUnit();
    reportPage.validatePageHeader("Non Conformity Report by Unit and Reason");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Export Routine CSV", () => {
    reportPage = homePage.goToRoutineReports();
    //reportPage.visitRoutineReports();
    reportPage.navigateToRoutineCSVReport();
    reportPage.validatePageHeader("Export Routine CSV file");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });
});

describe("Study Reports", function () {
  it("User Visits Study Reports", function () {
    homePage = loginPage.goToHomePage();
    reportPage = homePage.goToStudyReports();
  });

  it("should visit ARV Initial Version 1 and verify the button state", () => {
    reportPage.visitARVInitialVersion1();
  });

  it("should visit ARV Initial Version 2 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitARVInitialVersion2();
  });

  it("should visit ARV Follow-Up Version 1 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitARVFollowUpVersion1();
  });

  it("should visit ARV Follow-Up Version 2 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitARVFollowUpVersion2();
  });

  it("should visit ARV Follow-Up Version 3 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitARVFollowUpVersion3();
  });

  it("should visit EID Version 1 and verify the accordion items and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitEIDVersion1();
  });

  it("should visit EID Version 2 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitEIDVersion2();
  });

  it("should visit VL Version and verify the accordion items and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitVLVersion();
  });

  it("should visit Intermediate Version 1 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitIntermediateVersion1();
  });

  it("should visit Intermediate Version 2 and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitIntermediateVersion2();
  });

  it("should visit Intermediate By Service and verify the input fields and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitIntermediateByService();
  });

  it("should visit Special Request and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitSpecialRequest();
  });

  it("should visit Collected ARV Patient Report and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitCollectedARVPatientReport();
  });

  it("should visit Associated Patient Report and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitAssociatedPatientReport();
  });

  it("should visit Non-Conformity Report By Date and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitNonConformityReportByDate();
  });

  it("should visit Non-Conformity Report By Unit and Reason and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitNonConformityReportByUnitAndReason();
  });

  it("should visit Non-Conformity Report By Lab No and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitNonConformityReportByLabNo();
  });

  it("should visit Non-Conformity Report By Notification and verify the button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitNonConformityReportByNotification();
  });

  it("should visit Non-Conformity Report Follow-Up Required and verify the header and button state", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitNonConformityReportFollowUpRequired();
  });

  it("should visit General Report In Export By Date and select options", () => {
    reportPage = homePage.goToStudyReports();
    reportPage.visitGeneralReportInExportByDate();
  });

  it("User Visits Audit Trail Report And Validates", function () {
    reportPage = homePage.goToStudyReports();
    reportPage.visitAuditTrailReport();
    reportPage.verifyHeaderText("h3", "Audit Trail");
    //cy.fixture("Patient").then((order) => {
    //  reportPage.typeInField("#labNo", order.labNo);
    //});
    //reportPage.validateAudit();
  });
});
// TO DO
//describe("WHONET Report", function () {
//it("Navigation to WHONET Report and enter data", function () {
//cy.reload();
//reportPage = homePage.goToReports();
//reportPage.visitWhonetReport();
//});
//});
