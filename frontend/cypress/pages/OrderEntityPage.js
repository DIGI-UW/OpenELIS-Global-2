import PatientEntryPage from "./PatientEntryPage";

class OrderEntityPage {
  sampleTypeOptionDropDown = "";

  constructor() {}

  visit() {
    cy.visit("/AddOrder");
  }

  getPatientPage() {
    return new PatientEntryPage();
  }

  clickNextButton() {
    cy.getElement(".cds--btn.cds--btn--primary.forwardButton").click();
    cy.getElement(".cds--btn.cds--btn--primary.forwardButton").click();
  }

  selectSampleTypeOption(sampleType) {
    cy.getElement("select#sampleId_0").select(sampleType);
    cy.getElement("select#sampleId_0").select(sampleType);
  }

  checkPanelCheckBoxField() {
    cy.get(
      ".testPanels .cds--checkbox-wrapper:nth-child(5) .cds--checkbox",
    ).check({ force: true });
    cy.get(
      ".testPanels .cds--checkbox-wrapper:nth-child(5) .cds--checkbox",
    ).check({ force: true });
  }

  validateAcessionNumber(order) {
    cy.intercept("GET", `**/rest/SampleEntryAccessionNumberValidation**`).as(
      "accessionNoValidation",
    );
    cy.get("#labNo").type(order, { delay: 300 });

    cy.wait("@accessionNoValidation").then((interception) => {
      const responseBody = interception.response.body;

      console.log(responseBody);

      expect(responseBody.status).to.be.false;
    });
  }
  enterSiteName(siteName) {
    cy.get("input#siteName").type(siteName);
    cy.wait(500);
    cy.get(".suggestion-active").contains(siteName).click();
  }
  searchRequester(requester) {
    cy.get("input#requesterId").type(requester);
    cy.wait(500);
    cy.get(".suggestion-active").contains(requester).click();
  }

  generateLabOrderNumber() {
    cy.get("#generate", { timeout: 5000 }).should("be.visible").click();
  }

  //for now we dont need FName and LName with the autocomplete option
  requesterLName(requesterLName) {
    cy.get("input#requesterLastName").type(requesterLName);
  }

  requesterFName(requesterFName) {
    cy.get("input#requesterFirstName").type(requesterFName);
  }

  clickSubmitOrderButton() {
    cy.get("#submit-button").should("be.visible").click();
  }

  printBarCode() {
    cy.get("[data-cy='print-barcode-button']", { timeout: 15000 })
      .should("exist")
      .should("be.visible")
      .click();
  }
}

export default OrderEntityPage;
