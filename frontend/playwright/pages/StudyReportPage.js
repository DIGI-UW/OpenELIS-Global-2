import { expect } from "playwright/test";
class StudyReportPage {
  constructor(page) {
    this.page = page;
  }

  async visitHomePage() {
    homePage = await loginPage.goToHomePage();
  }

  async visitStudyReports() {
    report = await homePage.goToStudyReports();
  }

  async clickSideNavMenuItem(nthChild, submenuChild) {
    await this.page.click(
      `.cds--side-nav__items > :nth-child(${nthChild}) > .cds--side-nav__item > .cds--side-nav__submenu`,
    );
    await this.page.click(
      `:nth-child(${nthChild}) > .cds--side-nav__item > .cds--side-nav__menu > :nth-child(${submenuChild}) > .cds--side-nav__menu-item > .cds--side-nav__link`,
    );
  }

  async verifyButtonDisabled() {
    await expect(this.page.locator("section > .cds--btn")).toBeDisabled();
  }

  async typeInField(fieldIds, value) {
    if (typeof fieldIds == "string") {
      await this.page
        .getByRole("textbox", { name: fieldIds })
        .click({ force: true });
      await this.page.getByRole("textbox", { name: fieldIds }).fill(value);
      return;
    }
    for (const fieldId of fieldIds) {
      await this.page
        .getByRole("textbox", { name: fieldId })
        .click({ force: true });
      await this.page.getByRole("textbox", { name: fieldId }).fill(value);
      return;
    }
  }

  async verifyButtonVisible() {
    await expect(this.page.locator("section > .cds--btn")).toBeVisible();
  }

  async verifyHeaderText(selector, expectedText) {
    await expect(this.page.locator(selector)).toHaveText(expectedText);
  }

  async typeInDate(fieldId, value) {
    await this.page.click(`#${fieldId}`); // Focus the input
    await this.page.type(`#${fieldId}`, value); // Type the date
  }

  async clickAccordionItem(nthChild) {
    await this.page.click(
      `:nth-child(${nthChild}) > .cds--accordion__item > .cds--accordion__heading`,
    );
  }

  async clickAccordionPatient(nthChild) {
    await this.page.click(
      `:nth-child(${nthChild}) > .cds--accordion > .cds--accordion__item > .cds--accordion__heading`,
    );
  }

  async verifyElementVisible(selectors) {
    if (Array.isArray(selectors)) {
      for (const selector of selectors) {
        const locator = this.page.locator(selector);
        if (await locator.isVisible()) {
          return true;
        }
      }
    } else {
      await expect(this.page.locator(selectors)).toBeVisible();
    }
  }

  async selectDropdown(optionId, value) {
    await this.page.selectOption(`#${optionId}`, value);
  }

  async visitStudyReports() {
    await this.page.click(":nth-child(2) > .cds--link");
  }

  async visitARVInitialVersion1() {
    await this.clickSideNavMenuItem(1, 1);
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitARVInitialVersion2() {
    // await this.clickSideNavMenuItem(1, 2);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(1, 2);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV-initial",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitARVFollowUpVersion1() {
    // await this.clickSideNavMenuItem(1, 3);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(1, 3);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV-Follow-up",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitARVFollowUpVersion2() {
    // await this.clickSideNavMenuItem(1, 4);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(1, 4);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV-Follow-up",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitARVFollowUpVersion3() {
    // await this.clickSideNavMenuItem(1, 5);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(1, 5);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV -->Initial-FollowUp-VL",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitAuditTrailReport() {
    await this.page.click(
      ":nth-child(11) > .cds--side-nav__item > .cds--side-nav__submenu",
    );
    await this.page.click(
      ":nth-child(11) > .cds--side-nav__item > .cds--side-nav__menu > div > .cds--side-nav__menu-item > .cds--side-nav__link",
    );
  }

  async visitEIDVersion1() {
    // await this.clickSideNavMenuItem(2, 1);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(2, 1);
    await this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Diagnostic for children with DBS-PCR",
    );
    await this.clickAccordionPatient(2);
    await this.verifyElementVisible("#patientId");
    await this.verifyElementVisible("#local_search");
    await this.clickAccordionPatient(2);
    await this.clickAccordionItem(3);
    await this.verifyElementVisible(["#from", "#display_from"]);
    await this.verifyElementVisible(["#to", "#display_to"]);
    await this.clickAccordionItem(3);
    await this.clickAccordionItem(6);
    await this.verifyElementVisible("#siteName");
    await this.clickAccordionItem(6);
    await expect(
      this.page.locator(":nth-child(7) > :nth-child(2) > .cds--btn"),
    ).toBeVisible();
  }

  async visitEIDVersion2() {
    // await this.clickSideNavMenuItem(2, 2);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(2, 2);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Diagnostic for children with DBS-PCR",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitVLVersion() {
    // await this.clickSideNavMenuItem(3, 1);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(3, 1);
    await this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Viral Load",
    );
    await this.clickAccordionPatient(2);
    await this.verifyElementVisible("#patientId");
    await this.verifyElementVisible("#local_search");
    await this.clickAccordionPatient(2);
    await this.clickAccordionItem(3);
    await this.verifyElementVisible(["#from", "#display_from"]);
    await this.verifyElementVisible(["#to", "#display_to"]);
    await this.clickAccordionItem(3);
    await this.clickAccordionItem(6);
    await this.verifyElementVisible("#siteName");
    await this.clickAccordionItem(6);
    await expect(
      this.page.locator(":nth-child(7) > :nth-child(2) > .cds--btn"),
    ).toBeVisible();
  }

  async visitIntermediateVersion1() {
    // await this.clickSideNavMenuItem(4, 1);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(4, 1);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Indeterminate",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitIntermediateVersion2() {
    // await this.clickSideNavMenuItem(4, 2);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(4, 2);

    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Indeterminate",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitIntermediateByService() {
    // await this.clickSideNavMenuItem(4, 3);
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(4, 3);
    await this.verifyHeaderText(
      ".cds--lg\\:col-span-16 > section > h3",
      "Indeterminate",
    );
    await this.typeInDate("startDate", "01/02/2023");
    await this.typeInField("Referral Center or Laboratory", "CAME");
    await this.verifyElementVisible("#siteName");
  }

  async visitSpecialRequest() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(5, 1);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Special Request",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitCollectedARVPatientReport() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(6, 1);
    await this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Collected ARV Patient Report",
    );
    await this.verifyButtonDisabled();
    await this.typeInField("National ID", "UG-23SLHD7DBD");
    await this.verifyButtonVisible();
  }

  async visitAssociatedPatientReport() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(7, 1);
    await this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Associated Patient Report",
    );
    await this.verifyButtonDisabled();
    await this.typeInField("National ID", "UG-23SLHD7DBD");
    await this.verifyButtonVisible();
  }

  async visitNonConformityReportByDate() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(9, 1);
    await this.verifyHeaderText("h1", "Non-conformity Report By Date");
    await this.verifyButtonDisabled();
    await this.typeInDate("startDate", "01/02/2023");
    await this.verifyButtonVisible();
  }

  async visitNonConformityReportByUnitAndReason() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(9, 2);
    await this.verifyHeaderText(
      "h1",
      "Non Conformity Report by Unit and Reason",
    );
    await this.verifyButtonDisabled();
    await this.typeInDate("startDate", "01/02/2023");
    await this.verifyButtonVisible();
  }

  async visitNonConformityReportByLabNo() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(9, 3);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV -->Initial-FollowUp-VL",
    );
    await this.verifyButtonDisabled();
    await this.typeInField(["from", "display_from"], "DEV0124000000000000");

    await this.verifyButtonVisible();
  }

  async visitNonConformityReportByNotification() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(9, 4);
    await this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Non-conformity notification",
    );
    await this.verifyButtonVisible();
  }

  async visitNonConformityReportFollowUpRequired() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(9, 5);
    await this.verifyHeaderText("h1", "Follow-up Required");
    await this.verifyButtonDisabled();
    await this.typeInDate("startDate", "01/02/2023");
    await this.verifyButtonVisible();
  }

  async visitGeneralReportInExportByDate() {
    await this.visitStudyReports();
    await this.clickSideNavMenuItem(10, 1);
    await this.verifyHeaderText("h1", "Export a CSV File by Date");
    await this.selectDropdown("studyType", "Testing");
    await this.selectDropdown("dateType", "Order Date");
  }
}

export default StudyReportPage;
