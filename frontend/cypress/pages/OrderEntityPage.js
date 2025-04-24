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
    cy.contains("button", "Next").click();
  }

  selectCytology() {
    cy.get("#additionalQuestionsSelect").select("Cytology");
  }

  selectSampleTypeOption(sampleType) {
    cy.getElement("select#sampleId_0").select(sampleType);
  }
  checkPanelCheckBoxField() {
    cy.contains("span", "Bilan Biochimique").click();
    cy.contains("span", "Serologie VIH").click();
  }

  generateLabOrderNumber() {
    cy.get("[data-cy='generate-labNumber']").click();
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
    cy.get("input#siteName").clear().type(siteName);
    cy.contains(".suggestion-active", siteName).should("be.visible").click();
  }
  enterRequesterLastAndFirstName(
    fullName,
    requesterFirstName,
    requesterLastName,
  ) {
    cy.get("#requesterId").clear().type(fullName);
    cy.contains(".suggestion-active", fullName).click();
    cy.get("input#requesterFirstName").clear().type(requesterFirstName);
    cy.get("input#requesterLastName").clear().type(requesterLastName);
  }
  rememberSiteAndRequester() {
    cy.contains("span", "Remember site and requester").click();
  }
  clickSubmitOrderButton() {
    cy.contains("button", "Submit").click();
  }
}

export default OrderEntityPage;
