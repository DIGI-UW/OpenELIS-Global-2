class BillingMenuManagementPage {
  constructor() {}

  visit() {
    cy.visit("/MasterListsPage#billingMenuManagement");
  }

  openMenu() {
    cy.get("[data-testid='menuConfig']").click({ force: true });
    cy.contains("span", "Billing Menu Configuration").click({ force: true });
  }

  enterBillingAddress(address) {
    cy.get("input#billing\\ address").clear().type(address);
  }

  toggleBillingActive(status) {
    if (status) {
      cy.get("input#billing_active").check({ force: true });
    } else {
      cy.get("input#billing_active").uncheck({ force: true });
    }
  }

  submitButton() {
    cy.contains("button", "Submit").click();
  }
}

export default BillingMenuManagementPage;
