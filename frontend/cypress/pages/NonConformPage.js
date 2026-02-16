class NonConform {
  constructor() {
    this.selectors = {
      //centralized selectors
      title: "h2",
      searchType: "#type",
      searchField: "[data-cy='fieldName']",
      searchButton: "[data-testid='nce-search-button']",
      searchResult: "[data-testid='nce-search-result']",
      nceNumberResult: "[data-testid='nce-number-result']",
      sampleCheckbox: "[data-testid='nce-sample-checkbox']",
      goToFormButton: "[data-testid='nce-goto-form-button']",
      startDate: "input#startDate",
      reportingUnits: "#reportingUnits",
      description: "#text-area-1",
      suspectedCause: "#text-area-2",
      correctiveActionText: "#text-area-3",
      descriptionAndComments: "#text-area-10",
      nceCategory: "#nceCategory",
      nceType: "#nceType",
      consequences: "#consequences",
      recurrence: "#recurrence",
      labComponent: "#labComponent",
      discussionDate: "#tdiscussionDate",
      proposedCorrectiveAction: "#text-area-corrective",
      dateCompleted: "#dateCompleted",
      actionTypeCheckbox: "#correctiveAction",
      resolutionYes: "span:contains('Yes')",
      dateCompleted0: ".cds--date-picker-input__wrapper > #dateCompleted-0",
      submitButton: "[data-testid='nce-submit-button']",
      radioTable: "table",
      radioButton: 'input[type="radio"][name="radio-group"]',
    };
  }

  getReportNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  getViewNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  selectSearchType(type) {
    cy.get(this.selectors.searchType, { timeout: 15000 })
      .should("be.visible")
      .select(type);
  }

  enterSearchField(value) {
    cy.get(this.selectors.searchField, { timeout: 15000 })
      .should("be.visible")
      .clear({ force: true })
      .type(value, { force: true, delay: 0 });
  }

  clickSearchButton() {
    cy.get(this.selectors.searchButton, { timeout: 15000 })
      .should("be.visible")
      .and("not.be.disabled")
      .click({ force: true });
  }

  validateSearchResult(expectedValue) {
    cy.get(this.selectors.searchResult, { timeout: 15000 })
      .first()
      .should("be.visible")
      .invoke("text")
      .then((text) => text.trim())
      .should("eq", expectedValue);
  }

  validateLabNoSearchResult(labNo) {
    this.ensureNceDetailsLoaded();
    cy.get(this.selectors.searchResult, { timeout: 15000 })
      .first()
      .should("be.visible")
      .invoke("text")
      .then((text) => text.trim())
      .should("eq", labNo);
  }

  validateNCESearchResult(NCENo) {
    this.ensureNceDetailsLoaded();
    cy.get(this.selectors.nceNumberResult, { timeout: 15000 })
      .first()
      .should("be.visible")
      .invoke("text")
      .then((text) => text.trim())
      .should("eq", NCENo);
  }

  ensureNceDetailsLoaded() {
    cy.get("body", { timeout: 15000 }).then(($body) => {
      const hasDetails = $body.find(this.selectors.searchResult).length > 0;
      const hasSelectableRows =
        $body.find(this.selectors.radioButton).length > 0;

      if (!hasDetails && hasSelectableRows) {
        this.checkRadioButton();
      }
    });
  }

  clickCheckbox() {
    cy.get(this.selectors.sampleCheckbox)
      .should("be.visible")
      .check({ force: true });
  }

  clickGoToNceFormButton() {
    cy.get(this.selectors.goToFormButton).should("be.visible").click();
  }

  enterStartDate(date) {
    cy.get(this.selectors.startDate).type(date);
  }

  selectReportingUnit(unit) {
    cy.get(this.selectors.reportingUnits).select(unit);
  }

  enterDescription(description) {
    cy.get(this.selectors.description).type(description);
  }

  enterSuspectedCause(suspectedCause) {
    cy.get(this.selectors.suspectedCause).type(suspectedCause);
  }

  enterCorrectiveAction(correctiveAction) {
    cy.get(this.selectors.correctiveActionText).type(correctiveAction);
  }

  enterNceCategory(nceCategory) {
    cy.get(this.selectors.nceCategory).select(nceCategory);
  }

  enterNceType(nceType) {
    cy.get(this.selectors.nceType).select(nceType);
  }

  enterConsequences(consequences) {
    cy.get(this.selectors.consequences).select(consequences);
  }

  enterRecurrence(recurrence) {
    cy.get(this.selectors.recurrence).select(recurrence);
  }

  enterLabComponent(labComponent) {
    cy.get(this.selectors.labComponent).select(labComponent);
  }

  enterDescriptionAndComments(testText) {
    cy.get(this.selectors.descriptionAndComments).type(testText);
    cy.get(this.selectors.correctiveActionText).type(testText);
    cy.get(this.selectors.suspectedCause).type(testText);
  }

  enterDiscussionDate(date) {
    cy.get(this.selectors.discussionDate).type(date);
  }

  enterProposedCorrectiveAction(action) {
    cy.get(this.selectors.proposedCorrectiveAction)
      .should("not.be.disabled")
      .type(action, { force: true });
  }

  enterDateCompleted(date) {
    cy.get(this.selectors.dateCompleted).type(date);
  }

  selectActionType() {
    cy.get(this.selectors.actionTypeCheckbox).check({ force: true });
  }

  checkResolution() {
    cy.get(this.selectors.resolutionYes).click();
  }

  enterDateCompleted0(date) {
    cy.get(this.selectors.dateCompleted0).type(date);
  }

  submitForm() {
    cy.get(this.selectors.submitButton).click();
  }

  clickSubmitButton() {
    cy.get(this.selectors.submitButton).should("be.visible").click();
  }

  checkRadioButton() {
    cy.get(this.selectors.radioTable, { timeout: 15000 }).should("be.visible");
    cy.get(this.selectors.radioButton, { timeout: 15000 }).should("exist");
    return cy
      .get("tbody tr", { timeout: 15000 })
      .first()
      .within(() => {
        cy.get(this.selectors.radioButton)
          .should("exist")
          .click({ force: true });
      });
  }

  getAndSaveNceNumber() {
    cy.get(this.selectors.nceNumberResult)
      .invoke("text")
      .then((text) => {
        cy.readFile("cypress/fixtures/NonConform.json").then((existingData) => {
          const newData = { ...existingData, NceNumber: text.trim() };
          cy.writeFile("cypress/fixtures/NonConform.json", newData);
        });
      });
  }
}

export default NonConform;
