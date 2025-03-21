import ProviderMenuPage from "../../pages/ProviderMenu";

describe("Provider Menu Tests", () => {
  const providerMenuPage = new ProviderMenuPage();

  beforeEach(() => {
    providerMenuPage.visit();
  });

  it("should load provider menu and show correct title", () => {
    providerMenuPage.getPageHeading().should("contain.text", "Provider Menu");
  });

  it("should search for a provider", () => {
    providerMenuPage.searchProvider("Smith");
    providerMenuPage.verifyProviderInTable("Smith");
  });

  it("should add a new provider", () => {
    providerMenuPage.clickAddButton();
    cy.get("input#lastName").type("Doe");
    cy.get("input#firstName").type("Jane");
    cy.get("input#telephone").type("1234567890");
    cy.get("input#fax").type("9876543210");
    cy.contains("Submit").click();
    providerMenuPage.verifyProviderInTable("Doe");
  });

  it("should modify a provider", () => {
    providerMenuPage.selectProviderById(1);
    providerMenuPage.clickModifyButton();
    cy.get("input#firstName").clear().type("John");
    cy.contains("Submit").click();
    providerMenuPage.verifyProviderInTable("John");
  });

  it("should deactivate a provider", () => {
    providerMenuPage.selectProviderById(1);
    providerMenuPage.clickDeactivateButton();
    cy.contains("Provider deactivated successfully").should("be.visible");
  });

  it("should handle pagination", () => {
    providerMenuPage.navigateNextPage();
    cy.url().should("include", "page=2");
    providerMenuPage.navigatePreviousPage();
    cy.url().should("include", "page=1");
  });
});
