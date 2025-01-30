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

  selectSampleTypeOption(sampleType) {
    cy.getElement("select#sampleId_0").select(sampleType);
  }
  checkPanelCheckBoxField() {
    cy.get(
      ".testPanels .cds--checkbox-wrapper:nth-child(5) .cds--checkbox",
    ).check({ force: true });
  }
  generateLabOrderNumber() {
    cy.getElement(
      ":nth-child(2) > :nth-child(1) > :nth-child(2) > .cds--link",
    ).click();
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
  enterRequesterLastAndFirstName(requesterFirstName, requesterLastName) {
    cy.enterText("input#requesterFirstName", requesterFirstName);
    cy.enterText("input#requesterLastName", requesterLastName);
  }
  clickSubmitOrderButton() {
    cy.get('[data-cy="submitOrderButton"]').should("be.visible").click();
  }
}

export default OrderEntityPage;
