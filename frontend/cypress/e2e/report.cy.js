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
    reportPage.navigateToSection(1, 1);
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

  it("Should Visit Statistics Reports", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(2, 1);
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
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(2, 2);
    reportPage.validatePageHeader("Test Report Summary");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits HIV Test Summary and validates", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(2, 3);
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Rejection Report and validates", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(3, 1);
    reportPage.validatePageHeader("Rejection Report");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Activity Report By Test Type", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(4, 1);
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
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(4, 2);
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
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(4, 3);
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
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(5, 1);
    reportPage.validatePageHeader("External Referrals Report");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #endDate",
      "02/02/2023",
    );
    reportPage.selectDropdown("#locationcode", "CEDRES");
    reportPage.validateButtonVisible(":nth-child(4) > .cds--btn");
  });

  it("Visits Non Conformity Report By Date", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(6, 1);
    reportPage.validatePageHeader("Non ConformityReport by Date");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Non Conformity Report By Unit", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(6, 2);
    reportPage.validatePageHeader("Non Conformity Report by Unit and Reason");
    reportPage.validateButtonDisabled(".cds--form > :nth-child(3) > .cds--btn");
    reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    reportPage.validateButtonVisible(".cds--form > :nth-child(3) > .cds--btn");
  });

  it("Visits Export Routine CSV", () => {
    reportPage.visitRoutineReports();
    reportPage.navigateToSection(9, 1);
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
    reportPage.visitARVInitialVersion2();
  });

  it("should visit ARV Follow-Up Version 1 and verify the header and button state", () => {
    reportPage.visitARVFollowUpVersion1();
  });

  it("should visit ARV Follow-Up Version 2 and verify the header and button state", () => {
    reportPage.visitARVFollowUpVersion2();
  });

  it("should visit ARV Follow-Up Version 3 and verify the header and button state", () => {
    reportPage.visitARVFollowUpVersion3();
  });

  it("should visit EID Version 1 and verify the accordion items and button state", () => {
    reportPage.visitEIDVersion1();
  });

  it("should visit EID Version 2 and verify the header and button state", () => {
    reportPage.visitEIDVersion2();
  });

  it("should visit VL Version and verify the accordion items and button state", () => {
    reportPage.visitVLVersion();
  });

  it("should visit Intermediate Version 1 and verify the header and button state", () => {
    reportPage.visitIntermediateVersion1();
  });

  it("should visit Intermediate Version 2 and verify the header and button state", () => {
    reportPage.visitIntermediateVersion2();
  });

  it("should visit Intermediate By Service and verify the input fields and button state", () => {
    reportPage.visitIntermediateByService();
  });

  it("should visit Special Request and verify the header and button state", () => {
    reportPage.visitSpecialRequest();
  });

  it("should visit Collected ARV Patient Report and verify the header and button state", () => {
    reportPage.visitCollectedARVPatientReport();
  });

  it("should visit Associated Patient Report and verify the header and button state", () => {
    reportPage.visitAssociatedPatientReport();
  });

  it("should visit Non-Conformity Report By Date and verify the header and button state", () => {
    reportPage.visitNonConformityReportByDate();
  });

  it("should visit Non-Conformity Report By Unit and Reason and verify the header and button state", () => {
    reportPage.visitNonConformityReportByUnitAndReason();
  });

  it("should visit Non-Conformity Report By Lab No and verify the header and button state", () => {
    reportPage.visitNonConformityReportByLabNo();
  });

  it("should visit Non-Conformity Report By Notification and verify the button state", () => {
    reportPage.visitNonConformityReportByNotification();
  });

  it("should visit Non-Conformity Report Follow-Up Required and verify the header and button state", () => {
    reportPage.visitNonConformityReportFollowUpRequired();
  });

  it("should visit General Report In Export By Date and select options", () => {
    reportPage.visitGeneralReportInExportByDate();
  });

  it("User Visits Audit Trail Report And Validates", function () {
    reportPage.visitStudyReports();
    reportPage.visitAuditTrailReport();
    reportPage.verifyHeaderText("section > h3", "Audit Trail");
    cy.fixture("Order").then((order) => {
      reportPage.typeInField("labNo", order.labNo);
    });
    //reportPage.validateAudit();
  });
});
