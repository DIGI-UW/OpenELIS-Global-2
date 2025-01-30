class DashBoardPage {
  constructor() {}

  homeSearchBar() {
    cy.get('[data-cy="header-global-action-search"]').click();
  }
  searchBarInput(inputName) {
    cy.get("#searchItem").type(inputName);
  }

  searchBarClose() {
    cy.get('[data-cy="header-global-action-search"]').click();
  }

  notificationIcon() {
    cy.get('[data-cy="header-global-action-notifications"]').click();
  }

  notificationIconClose() {
    cy.get("#close-slide-over").click();
  }

  userIcon() {
    cy.get('[data-cy="header-global-action-user"]').click();
  }

  userSelectsEng(engLang) {
    cy.get("#selector").select(engLang);
  }

  userClosesIcon() {
    cy.get('[data-cy="header-global-action-user"]').click();
  }

  checkFilters() {
    cy.get("#filterMyCases").check({ force: true });
  }

  selectCases(myCases) {
    cy.get("#statusFilter").select(myCases);
  }

  selectCompletedCases(myCases2) {
    cy.get("#statusFilter").select(myCases2);
  }

  selectScreeningCases(myCases3) {
    cy.get("#statusFilter").select(myCases3);
  }
  enterLabNumber(labNumb) {
    cy.get("#search-input-41").should("be.visible").type(labNumb);
  }

  pageItems(itemsPerPage) {
    cy.get("#cds-pagination-select-id-59").select(itemsPerPage);
  }
}

export default DashBoardPage;
