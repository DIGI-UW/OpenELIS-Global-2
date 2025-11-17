/**
 * Cypress command to load storage test fixtures
 * Usage: cy.loadStorageFixtures() or cy.loadStorageFixtures({ reset: true })
 *
 * Options:
 *   - reset: If true, resets database before loading fixtures
 *   - noVerify: If true, skips verification after loading
 */
Cypress.Commands.add("loadStorageFixtures", (options = {}) => {
  cy.task("loadStorageTestData", options, { log: false });
});

/**
 * Cypress command to clean storage test fixtures
 * Usage: cy.cleanStorageFixtures()
 */
Cypress.Commands.add("cleanStorageFixtures", () => {
  cy.task("cleanStorageTestData", null, { log: false });
});
