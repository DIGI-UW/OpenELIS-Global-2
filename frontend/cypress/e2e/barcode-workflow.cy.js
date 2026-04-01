import LoginPage from "../pages/LoginPage";

describe("OGC-284 barcode workflow rollout", () => {
  before(() => {
    const loginPage = new LoginPage();
    loginPage.visit();
    loginPage.goToHomePage();
  });

  it("shows shared labels UI on Generic Sample Order", () => {
    cy.visit("/GenericSample/Order");
    cy.contains(/label quantities/i, { timeout: 15000 }).should("be.visible");
    cy.contains(/order labels/i, { timeout: 15000 }).should("be.visible");
    cy.contains(/running total/i, { timeout: 15000 }).should("be.visible");
  });

  it("loads Print Barcode reprint entry point", () => {
    cy.visit("/PrintBarcode");
    cy.contains(/print bar code labels/i).should("be.visible");
  });
});
