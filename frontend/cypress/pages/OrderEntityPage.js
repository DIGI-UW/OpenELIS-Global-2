//import { cy } from "date-fns/locale";
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
    cy.get(".cds--btn.cds--btn--primary.forwardButton").click();
  }

  selectProgram(program) {
    cy.get("#additionalQuestionsSelect").select(program);
  }

  selectSampleTypeOption(sampleType) {
    cy.get("#sampleId_0").select(sampleType);
  }

  checkPanelCheckBoxField() {
    cy.get("#panel_0_1").check({ force: true });
  }


  validateAcessionNumber(order) {
    cy.intercept("GET", `**/rest/SampleEntryAccessionNumberValidation**`).as(
      "accessionNoValidation",
    );
    cy.get("#display_labNo").type(order, { timeout: 10000 });

    cy.wait("@accessionNoValidation").then((interception) => {
      const responseBody = interception.response.body;
      console.log(responseBody);
      expect(responseBody.status).to.be.false;
    });
  }
  enterSiteName(siteName) {
    cy.get("input#siteName").type(siteName);
    cy.get(".suggestions") 
    .contains(siteName)
    .click();
  }
  searchRequester(requester) {
    cy.get("input#requesterId").type(requester);
    cy.get(".suggestions")
    .contains(requester)
    .click();
  }

  requesterFName(requesterFName) {
    cy.get("input#requesterFirstName").type(requesterFName);
  }

  generateLabOrderNumber() {
    cy.get("#generate", { timeout: 10000 }).should("be.visible").click();
  }

  requesterLName(requesterLName) {
    cy.get("input#requesterLastName").type(requesterLName);
  }
  clickSubmitOrderButton() {
    cy.get("button.forwardButton.cds--btn--primary")
      .should("be.visible")
      .click();
  }

  printBarCode() {
    cy.get("[data-cy='print-barcode-button']", { timeout: 15000 })
  .should("exist")
  .should("be.visible")
  .click();
    }
}

export default OrderEntityPage;
