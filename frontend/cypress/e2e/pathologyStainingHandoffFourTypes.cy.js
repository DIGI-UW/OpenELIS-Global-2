import LoginPage from "../pages/LoginPage";

/**
 * Stage 3 → 4 (Slide Preparation → Slide Staining): browser proof for each
 * pathology workflow type that the staining step shows at least one data row
 * in the Slide Staining table (after notebook pages have loaded). Also asserts
 * `samples-ready?currentStep=staining` returns HTTP 200 with a JSON array.
 *
 * Uses notebook 23 (Pathology Laboratory; URL param is the notebook id) and
 * toggles `notebook.workflow_type` via docker exec (restored after each test).
 *
 * Requires: app at baseUrl, Postgres container `openelisglobal-database`, and
 * existing slide-prep data on notebook 23 (dev seed).
 */
const login = new LoginPage();

const NOTEBOOK_ID = 23;

const WORKFLOW_TYPES = [
  "fnac",
  "peripheral_smear_bone_marrow_morphology",
  "histopathology_biopsy_tissue",
  "cytology_liquid_based_pap_smear",
];

function sqlUpdateWorkflowType(workflowType) {
  // workflow_type is a controlled literal from WORKFLOW_TYPES only
  return `docker exec openelisglobal-database psql -U clinlims -d clinlims -q -c "UPDATE notebook SET workflow_type='${workflowType}' WHERE id=${NOTEBOOK_ID};"`;
}

describe("Pathology — Slide Staining receives samples (four workflow types)", () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
    cy.intercept("GET", "**/rest/notebook/view/23**").as("notebookView");
    // Avoid "?" in minimatch strings (it is a single-char wildcard). Match staining step in query.
    cy.intercept(
      "GET",
      "**/rest/notebook/pathology/workflow/samples-ready**staining**",
    ).as("stainingReady");

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

  afterEach(() => {
    cy.exec(sqlUpdateWorkflowType("fnac"), { failOnNonZeroExit: false });
  });

  WORKFLOW_TYPES.forEach((workflowType) => {
    it(`Slide Staining lists sample rows for workflow_type=${workflowType}`, () => {
      cy.exec(sqlUpdateWorkflowType(workflowType), { failOnNonZeroExit: true });

      cy.visit(`/NoteBookInstanceEditForm/${NOTEBOOK_ID}?mode=edit`, {
        failOnStatusCode: false,
      });
      cy.url({ timeout: 60000 }).should("not.include", "/login");

      cy.wait("@notebookView", { timeout: 60000 });

      cy.contains(/Workflow/i, { timeout: 30000 }).should("be.visible").click();

      cy.get(".pathology-workflow, .notebook-workflow-container", {
        timeout: 30000,
      }).should("exist");

      cy.contains(/Slide Staining/i, { timeout: 30000 })
        .should("be.visible")
        .click();

      cy.wait("@stainingReady", { timeout: 45000 });

      cy.get("@stainingReady").then((interception) => {
        const n = Array.isArray(interception.response?.body)
          ? interception.response.body.length
          : -1;
        cy.task("log", `[pathology 3→4] request=${interception.request.url}`);
        cy.log(
          `[pathology 3→4] samples-ready?currentStep=staining count=${n} (${workflowType})`,
        );
      });

      cy.get("@stainingReady")
        .its("response")
        .then((res) => {
          expect(res.statusCode).to.eq(200);
          expect(res.body, "staining samples-ready body").to.be.an("array");
          expect(
            res.body.length,
            `samples-ready should hand off at least one row (${workflowType})`,
          ).to.be.at.least(1);
        });

      cy.get(".staining-page .cds--data-table tbody tr", { timeout: 20000 }).should(
        ($rows) => {
          expect(
            $rows.length,
            `staining table should list at least one sample row (${workflowType})`,
          ).to.be.at.least(1);
        },
      );
    });
  });
});
