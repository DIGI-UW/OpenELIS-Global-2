import { cy } from "date-fns/locale";
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
  }

  selectProgram() {
    cy.get("#additionalQuestionsSelect").select("HIV");
  }

  selectSampleTypeOption(sampleType) {
    cy.getElement("select#sampleId_0").select(sampleType);
  }
  checkPanelCheckBoxField() {
    cy.get("#panel_0_1").check();
  }

  generateLabOrderNumber() {
    cy.contains("a.cds--link", "Generate").click();
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
    cy.enterText("input#siteName", siteName);
  }
  searchRequester(requester) {
    cy.enterText("input#requesterId").select(requester);
  }
  clickSubmitOrderButton() {
    cy.get("button.forwardButton.cds--btn--primary").click();
    }
}

export default OrderEntityPage;
