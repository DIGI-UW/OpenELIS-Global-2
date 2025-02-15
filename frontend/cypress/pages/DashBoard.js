class DashBoardPage {
  constructor() {}

  searchPatientByFName() {
    cy.get("#firstName").type("John");
  }
  searchPatient() {
    cy.get("#local_search").click();
  }

  checkPatientRadio() {
    cy.get("table tbody tr").first().find("td label span").click();
  }

  clickNext() {
    cy.get(".forwardButton").click();
  }

  selectHistopathology() {
    cy.get("#additionalQuestionsSelect").select("Histopathology");
  }

  selectImmunoChem() {
    cy.get("#additionalQuestionsSelect").select("Immunohistochemistry");
  }

  selectCytology() {
    cy.get("#additionalQuestionsSelect").select("Cytology");
  }

  selectFluidSample() {
    cy.get("#sampleId_0").should("be.visible").select("Fluid");
  }

  checkCovidPanel() {
    cy.contains("label.cds--checkbox-label", "COVID-19 PCR").click();
  }

  selectPathologySample() {
    cy.get("#sampleId_0")
      .should("be.visible")
      .select("Histopathology specimen");
  }

  selectImmunoChemSample() {
    cy.get("#sampleId_0")
      .should("be.visible")
      .select("Immunohistochemistry specimen");
  }

  checkImmunoChemTest() {
    cy.contains(".cds--checkbox-label-text", "Anti-CD7").click();
  }
  checkPathologyPanel() {
    cy.contains(
      "label.cds--checkbox-label",
      "Histopathology examination",
    ).click();
  }

  generateLabNo() {
    cy.contains("a.cds--link", "Generate").click();
  }

  selectSite() {
    cy.get("#siteName").type("279 - CAMES ");
    cy.contains(".suggestion-active", "279 - CAMES ").click();
    cy.wait(200);
  }

  selectRequesting() {
    cy.get("#requesterId").type("Optimus");
    cy.contains(".suggestion-active", "Optimus").click();
    cy.wait(200);
  }

  submitButton() {
    cy.contains(".forwardButton", "Submit").should("be.visible").click();
  }

  clickPrintBarCode() {
    cy.contains(".cds--btn--primary", "Print Barcode").should("be.visible");
  }

  selectFirstOrder() {
    cy.get('tbody[aria-live="polite"] > tr').first().click();
  }

  saveOrder() {
    cy.get("#pathology_save2").click();
  }

  selectPathologyStatus() {
    cy.get("#status").select("Processing");
  }

  pathologyStatusFilter() {
    cy.get("#statusFilter").should("be.visible").select("Processing");
  }
  selectStatus() {
    cy.get("#status").should("be.visible").select("Completed");
  }
  selectPathologist() {
    cy.get("#assignedPathologist").select("ELIS,Open");
  }

  selectTechnician() {
    cy.get("#assignedTechnician").select("External,Service");
  }

  checkImmunoChem() {
    cy.get("#referToImmunoHistoChemistry").check();
  }

  chckImmunoChemOption() {
    cy.get("#ihctest-input").select("Anti-CD 5(Immunohistochemistry specimen)");
  }

  addImmunoChemReport() {
    cy.get("#report").select("Immunohistochemistry Report");
  }

  checkReadyForRelease() {
    cy.get("#release").should("be.visible").check({ force: true });
  }

  statusFilter() {
    cy.get("#statusFilter").select("Completed");
  }
}

export default DashBoardPage;
