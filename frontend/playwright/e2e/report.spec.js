import LoginPage from "../pages/LoginPage";
import { expect, test } from "playwright/test";

let browser;
let homePage;
let context;
let page;
let loginPage;
let reportPage;

test.beforeAll(async ({ browser: browserInstance }) => {
  browser = browserInstance;
  context = await browser.newContext();
  page = await context.newPage();

  loginPage = new LoginPage(page);
  homePage = await loginPage.goToHomePage();
});

test.describe("Routine Reports", () => {
  test("User Visits Routine Reports", async () => {
    reportPage = await homePage.goToRoutineReports();
  });

  test("User Visits Patient Status Report and checks for Respective Forms", async ({}) => {
    await reportPage.navigateToSection(1, 1);
    await reportPage.validatePageHeader("Patient Status Report");

    await reportPage.toggleAccordionPatient(2);
    await reportPage.validateFieldVisibility("#patientId");
    await reportPage.validateFieldVisibility("#local_search");
    await reportPage.toggleAccordionPatient(2);

    await reportPage.toggleAccordion(3);
    await expect(page.getByRole("textbox", { name: "From" })).toBeVisible();
    await expect(page.getByRole("textbox", { name: "To" })).toBeVisible();

    await reportPage.toggleAccordion(3);

    await reportPage.toggleAccordion(6);
    await reportPage.validateFieldVisibility('[data-cy="dateTypeDropdown"]');
    await reportPage.validateFieldVisibility(
      ".cds--date-picker-input__wrapper > #startDate",
    );
    await reportPage.validateButtonVisible(
      ":nth-child(7) > :nth-child(2) > .cds--btn",
    );
  });

  test("Should Visit Statistics Reports", async ({}) => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(2, 1);
    await reportPage.validatePageHeader("Statistics Report");

    await reportPage.checkAllCheckboxes(
      2,
      11,
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
    );
    await reportPage.validateFieldVisibility(
      ':nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) input[type="checkbox"]',
    );
  });

  test('Within Statistic Should uncheck the "All" checkbox if any individual checkbox is unchecked', async ({}) => {
    await reportPage.checkAllCheckboxes(
      2,
      11,
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
    );
    await reportPage.validateAllCheckBox("not.be.checked");
    await reportPage.uncheckCheckbox(
      2,
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2)",
    );
    await reportPage.validateAllCheckBox("not.be.checked");
    await reportPage.validateFieldVisibility(
      ':nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) input[type="checkbox"]',
    );
  });

  test('Should check/uncheck "All" checkbox for priority', async () => {
    await reportPage.checkAllCheckboxes(2, 6, ".inlineDiv");
    await reportPage.uncheckCheckbox(2, ".inlineDiv");
    await reportPage.validateButtonVisible(
      ":nth-child(3) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) > .cds--checkbox-label",
    );
  });

  test("should check for print button", async ({}) => {
    await reportPage.validateButtonVisible(":nth-child(6) > .cds--btn");
  });

  test("Visits Summary of all tests", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(2, 2);
    await reportPage.validatePageHeader("Test Report Summary");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits HIV Test Summary and validates", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(2, 3);
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Rejection Report and validates", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(3, 1);
    await reportPage.validatePageHeader("Rejection Report");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Activity Report By Test Type", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(4, 1);
    await reportPage.validatePageHeader("Activity report By test");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.selectDropdown("#type", "Amylase(Serum)");
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Activity Report By Panel Type", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(4, 2);
    await reportPage.validatePageHeader("Activity report By Panel");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.selectDropdown("#type", "NFS");
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Activity Report By Unit", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(4, 3);
    await reportPage.validatePageHeader("Activity report By Test Section");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.selectDropdown("#type", "Biochemistry");
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Referred Out Test Report", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(5, 1);
    await reportPage.validatePageHeader("External Referrals Report");
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #endDate",
      "02/02/2023",
    );
    await reportPage.selectDropdown("#locationcode", "CEDRES");
    await reportPage.validateButtonVisible(":nth-child(4) > .cds--btn");
  });

  test("Visits Non Conformity Report By Date", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(6, 1);
    await reportPage.validatePageHeader("Non ConformityReport by Date");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Non Conformity Report By Unit", async () => {
    reportPage = await homePage.goToRoutineReports();
    // await reportPage.visitRoutineReports();
    // await reportPage.navigateToSection(6, 2);
    await page.getByRole("button", { name: "Non conformity Reports" }).click();
    await page.getByRole("link", { name: "By Unit and Reason" }).click();
    await reportPage.validatePageHeader(
      "Non Conformity Report by Unit and Reason",
    );
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });

  test("Visits Export Routine CSV", async () => {
    await reportPage.visitRoutineReports();
    await reportPage.navigateToSection(9, 1);
    await reportPage.validatePageHeader("Export Routine CSV file");
    await reportPage.validateButtonDisabled(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
    await reportPage.typeInDatePicker(
      ".cds--date-picker-input__wrapper > #startDate",
      "01/02/2023",
    );
    await reportPage.validateButtonVisible(
      ".cds--form > :nth-child(3) > .cds--btn",
    );
  });
});
// Working Perfectly Fine-----------------
test.describe("Study Reports", () => {
  test("User Visits Study Reports", async () => {
    reportPage = await homePage.goToStudyReports();
  });

  test("should visit ARV Initial Version 1 and verify the button state", async () => {
    await reportPage.visitARVInitialVersion1();
  });

  test("should visit ARV Initial Version 2 and verify the header and button state", async () => {
    await reportPage.visitARVInitialVersion2();
  });

  test("should visit ARV Follow-Up Version 1 and verify the header and button state", async () => {
    await reportPage.visitARVFollowUpVersion1();
  });

  test("should visit ARV Follow-Up Version 2 and verify the header and button state", async () => {
    await reportPage.visitARVFollowUpVersion2();
  });

  test("should visit ARV Follow-Up Version 3 and verify the header and button state", async () => {
    await reportPage.visitARVFollowUpVersion3();
  });

  test("should visit EID Version 1 and verify the accordion items and button state", async () => {
    await reportPage.visitEIDVersion1();
  });

  test("should visit EID Version 2 and verify the header and button state", async () => {
    await reportPage.visitEIDVersion2();
  });

  test("should visit VL Version and verify the accordion items and button state", async () => {
    await reportPage.visitVLVersion();
  });

  test("should visit Intermediate Version 1 and verify the header and button state", async () => {
    await reportPage.visitIntermediateVersion1();
  });

  test("should visit Intermediate Version 2 and verify the header and button state", async () => {
    await reportPage.visitIntermediateVersion2();
  });

  test("should visit Intermediate By Service and verify the input fields and button state", async () => {
    await reportPage.visitIntermediateByService();
  });

  test("should visit Special Request and verify the header and button state", async () => {
    await reportPage.visitSpecialRequest();
  });

  test("should visit Collected ARV Patient Report and verify the header and button state", async () => {
    await reportPage.visitCollectedARVPatientReport();
  });

  test("should visit Associated Patient Report and verify the header and button state", async () => {
    await reportPage.visitAssociatedPatientReport();
  });

  test("should visit Non-Conformity Report By Date and verify the header and button state", async () => {
    await reportPage.visitNonConformityReportByDate();
  });

  test("should visit Non-Conformity Report By Unit and Reason and verify the header and button state", async () => {
    await reportPage.visitNonConformityReportByUnitAndReason();
  });

  test("should visit Non-Conformity Report By Lab No and verify the header and button state", async () => {
    await reportPage.visitNonConformityReportByLabNo();
  });

  test("should visit Non-Conformity Report By Notification and verify the button state", async () => {
    await reportPage.visitNonConformityReportByNotification();
  });

  test("should visit Non-Conformity Report Follow-Up Required and verify the header and button state", async () => {
    await reportPage.visitNonConformityReportFollowUpRequired();
  });

  test("should visit General Report In Export By Date and select options", async () => {
    await reportPage.visitGeneralReportInExportByDate();
  });

  test("User Visits Audit Trail Report And Validates", async () => {
    await reportPage.visitStudyReports();
    await reportPage.visitAuditTrailReport();
    await reportPage.verifyHeaderText("section > h3", "Audit Trail");
    
    const order = require("../fixtures/EnteredOrder.json");
    await reportPage.typeInField("Lab No", order.labNo);
  });
});
