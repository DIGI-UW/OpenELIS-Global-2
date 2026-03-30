/**
 * Study Entry E2E Test Setup Commands
 *
 * Follows the same pattern as patient-merge-setup.js for consistency.
 * Provides Cypress commands for seeding the ARV and EID organizations
 * required by studyInitialEntry.cy.js and studyDoubleEntry.cy.js.
 *
 * The SQL is in cypress/support/study-entry-setup.sql and is executed via
 * the loadStudyOrganizations / checkStudyOrganizationsExist tasks registered
 * in cypress.config.js.
 */

/**
 * Load study entry organization fixtures into the database.
 * Usage: cy.loadStudyOrganizations()
 */
Cypress.Commands.add("loadStudyOrganizations", () => {
  cy.task("loadStudyOrganizations", null, { log: false });
});

/**
 * Check whether the 5 study organizations already exist in the database.
 * Returns true if all 5 are present, false otherwise.
 * Usage: cy.checkStudyOrganizationsExist().then(exists => { ... })
 */
Cypress.Commands.add("checkStudyOrganizationsExist", () => {
  return cy.task("checkStudyOrganizationsExist", null, { log: false });
});

/**
 * Smart setup for study entry E2E tests.
 *
 * Respects the same env-var contract used by other fixture helpers:
 *   SKIP_FIXTURES=true  → skip loading entirely (assumes orgs already exist)
 *   FORCE_FIXTURES=true → always reload even if orgs already exist
 *   (default)           → check existence first, load only if missing
 *
 * Usage: cy.setupStudyEntryOrganizations()
 */
Cypress.Commands.add("setupStudyEntryOrganizations", () => {
  const skipFixtures = Cypress.env("SKIP_FIXTURES") === true;
  const forceFixtures = Cypress.env("FORCE_FIXTURES") === true;

  if (skipFixtures) {
    cy.log(
      "Skipping study org fixture load (SKIP_FIXTURES=true) — assuming orgs exist",
    );
    return;
  }

  if (forceFixtures) {
    cy.log(
      "Force-loading study org fixtures (FORCE_FIXTURES=true) — reloading even if present",
    );
    cy.loadStudyOrganizations();
    return;
  }

  // Default: load only if not already present (fast path on re-runs)
  cy.checkStudyOrganizationsExist().then((exists) => {
    if (exists) {
      cy.log(
        "Study entry organizations already present — skipping seed for faster iteration",
      );
    } else {
      cy.log("Study entry organizations not found — seeding now");
      cy.loadStudyOrganizations();
    }
  });
});
