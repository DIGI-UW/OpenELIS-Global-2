class ProgramEntryPage {
  constructor() {}

  // Centralized selectors
  selectors = {
    program: "#additionalQuestionsSelect",
    testSection: "#test_section",
    questionare: "#additionalOrderEntryQuestions",
    //submitButton: "button[type='submit']",
    submitButton: "#submitProgram",
  };

  verifyPageLoads() {
    cy.contains("h2", "Add/Edit Program").should("be.visible");
  }

  selectProgram(value) {
    cy.get(this.selectors.program).select(value);
  }

  selectTest(value) {
    cy.get(this.selectors.testSection).select(value);
  }

  typeQuestionare(value) {
    cy.get(this.selectors.questionare).type(value);
  }

  submitProgram() {
    cy.get(this.selectors.submitButton).click();
  }
}

export default ProgramEntryPage;
