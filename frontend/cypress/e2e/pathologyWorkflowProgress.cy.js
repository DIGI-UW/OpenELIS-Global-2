import LoginPage from "../pages/LoginPage";

/**
 * Browser verification: Pathology workflow tab loads, sidebar shows real
 * per-page progress (bulk/page/{id}/progress), and reduced FNAC stages render.
 *
 * Requires stack at baseUrl (default Cypress baseUrl is https://localhost;
 * run with: npx cypress run --config baseUrl=https://localhost:9443 --spec ...).
 *
 * Notebook ID defaults to 23 (FNAC instance in dev DB); override with env:
 *   CYPRESS_pathologyNotebookId=22
 */
const login = new LoginPage();

describe("Pathology workflow — UI progress and stages", () => {
  const notebookId =
    Cypress.env("pathologyNotebookId") ||
    Cypress.env("PATHOLOGY_NOTEBOOK_ID") ||
    23;

  beforeEach(() => {
    cy.intercept("GET", "**/rest/notebook/bulk/page/*/progress").as(
      "pageProgress",
    );
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      expect(user, "fixture user with correctPass").to.exist;
      login.enterUsername(user.username);
      login.enterPassword(user.password);
      login.signIn();
      cy.get("[data-cy='menuButton']", { timeout: 60000 }).should("be.visible");
    });
  });

  it("loads pathology workflow tab, fetches per-page progress, and shows FNAC stages", () => {
    cy.visit(`/NoteBookInstanceEditForm/${notebookId}?mode=edit`, {
      failOnStatusCode: false,
    });

    cy.url({ timeout: 60000 }).should("not.include", "/login");

    cy.contains(/Workflow/i, { timeout: 30000 }).should("be.visible").click();

    cy.get(".pathology-workflow, .notebook-workflow-container", {
      timeout: 30000,
    }).should("exist");

    cy.get(".page-navigation", { timeout: 30000 })
      .should("be.visible")
      .contains("Slide Staining");

    cy.wait("@pageProgress", { timeout: 30000 });
    cy.wait("@pageProgress", { timeout: 30000 });

    cy.get("@pageProgress")
      .its("response")
      .then((res) => {
        expect(res.statusCode).to.eq(200);
        expect(res.body).to.have.property("total");
        expect(res.body).to.have.property("completed");
        expect(res.body).to.have.property("percentage");
      });

    cy.get(".page-navigation .page-progress-cell", { timeout: 15000 })
      .first()
      .should("be.visible")
      .and("contain.text", "%");
  });
});
