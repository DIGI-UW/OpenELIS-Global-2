/**
 * Cypress command to load storage test fixtures
 * Usage: cy.loadStorageFixtures()
 */
Cypress.Commands.add("loadStorageFixtures", () => {
  cy.task("loadStorageTestData", null, { log: false });
});

/**
 * Cypress command to clean storage test fixtures
 * Usage: cy.cleanStorageFixtures()
 */
Cypress.Commands.add("cleanStorageFixtures", () => {
  cy.task("cleanStorageTestData", null, { log: false });
});
