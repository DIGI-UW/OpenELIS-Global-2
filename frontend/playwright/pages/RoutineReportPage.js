import { expect } from "@playwright/test";

class RoutineReportPage {
  constructor(page) {
    this.page = page;
  }

  async navigateToSection(sectionNumber, subsectionNumber) {
    await this.page.click(
      `.cds--white > :nth-child(1) > .cds--side-nav__navigation > .cds--side-nav__items > :nth-child(${sectionNumber}) > .cds--side-nav__item > .cds--side-nav__submenu`,
    );
    await this.page.click(
      `:nth-child(${sectionNumber}) > .cds--side-nav__item > .cds--side-nav__menu > :nth-child(${subsectionNumber}) > .cds--side-nav__menu-item > .cds--side-nav__link`,
    );
  }

  async validatePageHeader(expectedText) {
    await this.page.waitForSelector("section > h3, h1", { state: "visible" });
    const headerText = await this.page.textContent("section > h3, h1");
    expect(headerText).toBe(expectedText);
  }

  async validateFieldVisibility(selector) {
    await expect(this.page.locator(selector)).toBeVisible();
  }

  async validateButtonDisabled(selector) {
    await expect(this.page.locator(selector)).toBeDisabled();
  }

  async validateButtonVisible(selector) {
    await expect(this.page.locator(selector)).toBeVisible();
  }

  async visitRoutineReports() {
    await this.page.waitForSelector(":nth-child(2) > .cds--link", {
      state: "visible",
    });

    await this.page.click(":nth-child(2) > .cds--link");
  }

  async toggleAccordion(accordionNumber) {
    await this.page.click(
      `:nth-child(${accordionNumber})> .cds--accordion__item > .cds--accordion__heading`,
    );
  }

  async toggleAccordionPatient(accordionNumber) {
    await this.page.click(
      `:nth-child(${accordionNumber}) >.cds--accordion > .cds--accordion__item > .cds--accordion__heading`,
    );
  }

  async toggleCheckbox(checkboxNumber, containerSelector) {
    await this.page.click(
      `${containerSelector} > :nth-child(${checkboxNumber}) input[type="checkbox"]`,
      { force: true },
    );
  }

  async checkAllCheckboxes(start, end, containerSelector) {
    for (let i = start; i <= end; i++) {
      await this.toggleCheckbox(i, containerSelector);
    }
  }

  async validateAllCheckBox(check) {
    const checkbox = this.page.locator(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) > .cds--checkbox-label",
    );
    if (check === "be.checked") {
      await expect(checkbox).toBeChecked();
    } else {
      await expect(checkbox).not.toBeChecked();
    }
  }

  async uncheckCheckbox(checkboxNumber, containerSelector) {
    await this.toggleCheckbox(checkboxNumber, containerSelector);
  }

  async selectDropdown(selector, value) {
    await this.page.selectOption(selector, value);
  }

  async typeInDatePicker(selector, date) {
    await this.page.fill(selector, date);
  }
}

export default RoutineReportPage;
