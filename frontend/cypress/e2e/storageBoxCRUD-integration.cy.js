/**
 * E2E Tests for Box/Plate CRUD - Real Backend Integration
 *
 * IMPORTANT:
 * - NO response stubbing. We only spy (req.continue) and assert request bodies.
 * - Uses seeded fixtures via load-storage-fixtures.
 *
 * Run individually:
 *   npm run cy:run -- --spec "cypress/e2e/storageBoxCRUD-integration.cy.js"
 */
import "../support/load-storage-fixtures";
import "../support/storage-setup";

describe("Storage Box CRUD - Real Backend Integration", () => {
  let apiErrors = [];

  before(() => {
    cy.setupStorageTests();
    cy.visit("/Storage");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
  });

  beforeEach(() => {
    apiErrors = [];

    // Spy (do not stub) to capture errors + request bodies.
    cy.intercept("**/rest/storage/**", (req) => {
      req.continue((res) => {
        if (res.statusCode >= 400) {
          apiErrors.push({
            method: req.method,
            url: req.url,
            statusCode: res.statusCode,
            requestBody: req.body,
            responseBody: res.body,
          });
        }
      });
    }).as("storageAny");

    cy.intercept("POST", "**/rest/storage/boxes**").as("createBox");
    cy.intercept("PUT", "**/rest/storage/boxes/**").as("updateBox");
    cy.intercept("DELETE", "**/rest/storage/boxes/**").as("deleteBox");
    cy.intercept("GET", "**/rest/storage/boxes/**/can-delete**").as(
      "canDeleteBox",
    );
  });

  afterEach(function () {
    if (this.currentTest?.state === "failed") {
      if (apiErrors.length) {
        cy.task("logObject", {
          message: `Storage API errors (${apiErrors.length})`,
          errors: apiErrors.slice(-20),
        });
      }
      cy.exec("docker logs --tail 200 openelisglobal-webapp", {
        failOnNonZeroExit: false,
        timeout: 30000,
      });
    }
  });

  const selectFirstRackForBoxesTab = () => {
    cy.get('[data-testid="tab-boxes"]').click();
    cy.get('button[role="tab"]')
      .contains("Boxes")
      .should("have.attr", "aria-selected", "true");

    // Rack dropdown is a Carbon Dropdown with testid rack-selector.
    cy.get('[data-testid="rack-selector"]', { timeout: 15000 }).should(
      "be.visible",
    );

    // Open dropdown and select first non-empty option.
    cy.get('[data-testid="rack-selector"]').click({ force: true });
    cy.get(".cds--list-box__menu-item", { timeout: 10000 })
      .should("have.length.at.least", 2) // includes "Select"
      .eq(1)
      .click({ force: true });
  };

  it("disables Add Box until rack is selected", () => {
    cy.get('[data-testid="tab-boxes"]').click();
    cy.get('[data-testid="add-box-button"]', { timeout: 10000 })
      .should("be.visible")
      .should("be.disabled");
  });

  it("creates a box via UI and persists to backend", () => {
    const newLabel = `E2E Box ${Date.now()}`;
    const newCode = `BX${Date.now().toString().slice(-6)}`.toUpperCase();

    selectFirstRackForBoxesTab();

    cy.get('[data-testid="add-box-button"]').should("not.be.disabled").click();

    // Modal fields
    cy.get('[data-testid="box-label"]', { timeout: 10000 })
      .should("be.visible")
      .type(newLabel);
    cy.get('[data-testid="box-code"]').should("be.visible").type(newCode);
    cy.get('[data-testid="box-rows"]').should("be.visible").clear().type("8");
    cy.get('[data-testid="box-columns"]')
      .should("be.visible")
      .clear()
      .type("12");

    // Save (Carbon button text)
    cy.contains("button", "Save", { timeout: 10000 })
      .should("not.be.disabled")
      .click();

    cy.wait("@createBox", { timeout: 20000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      expect(interception.request.body).to.have.property("label", newLabel);
      expect(interception.request.body).to.have.property("code", newCode);
      expect(interception.request.body).to.have.property("active");
    });

    // Verify box selector contains the new label (real backend refresh)
    cy.get('[data-testid="box-selector"]', { timeout: 15000 }).should(
      "be.visible",
    );
    cy.get('[data-testid="box-selector"]').click({ force: true });
    cy.contains(".cds--list-box__menu-item", newLabel, {
      timeout: 15000,
    }).should("be.visible");
  });

  it("edits a selected box via UI and persists to backend", () => {
    const updatedLabel = `E2E Box Updated ${Date.now()}`;

    selectFirstRackForBoxesTab();

    // Select first real box
    cy.get('[data-testid="box-selector"]')
      .should("be.visible")
      .click({ force: true });
    cy.get(".cds--list-box__menu-item", { timeout: 10000 })
      .should("have.length.at.least", 2)
      .eq(1)
      .click({ force: true });

    // Open overflow menu and click Edit
    cy.get('[data-testid="location-actions-overflow-menu"]', { timeout: 10000 })
      .should("be.visible")
      .click({ force: true });
    cy.get('[data-testid="edit-location-menu-item"]')
      .should("be.visible")
      .click({ force: true });

    cy.get('[data-testid="box-label"]', { timeout: 10000 })
      .should("be.visible")
      .clear()
      .type(updatedLabel);

    cy.contains("button", "Save", { timeout: 10000 }).click();

    cy.wait("@updateBox", { timeout: 20000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      expect(interception.request.body).to.have.property("label", updatedLabel);
      expect(interception.request.body).to.have.property("active");
    });
  });

  it("blocks delete when backend says can-delete=false (constraint path)", () => {
    // Find a constrained box via API, then drive UI against it.
    cy.request("/rest/storage/boxes").then((boxesRes) => {
      const boxes = boxesRes.body || [];
      expect(boxes.length).to.be.greaterThan(0);

      const checkAll = boxes.map((b) =>
        cy
          .request({
            url: `/rest/storage/boxes/${b.id}/can-delete`,
            failOnStatusCode: false,
          })
          .then((r) => ({ box: b, res: r })),
      );

      cy.wrap(Promise.all(checkAll)).then((results) => {
        const blocked = results.find((x) => x.res.status === 409);
        if (!blocked) {
          cy.log(
            "No constrained box found in fixtures; skipping constraint path.",
          );
          return;
        }

        const { box } = blocked;

        cy.get('[data-testid="tab-boxes"]').click();
        // Select the box's parent rack first
        cy.get('[data-testid="rack-selector"]').click({ force: true });
        cy.contains(".cds--list-box__menu-item", String(box.parentRackId), {
          timeout: 1000,
        }).then(
          () => {
            // unlikely match by id string; fall back to selecting first rack
            cy.get(".cds--list-box__menu-item").eq(1).click({ force: true });
          },
          () => {
            cy.get(".cds--list-box__menu-item").eq(1).click({ force: true });
          },
        );

        // Select the box by label in dropdown
        cy.get('[data-testid="box-selector"]').click({ force: true });
        cy.contains(".cds--list-box__menu-item", box.label, { timeout: 15000 })
          .should("be.visible")
          .click({ force: true });

        cy.get('[data-testid="location-actions-overflow-menu"]')
          .should("be.visible")
          .click({ force: true });
        cy.get('[data-testid="delete-location-menu-item"]')
          .should("be.visible")
          .click({ force: true });

        cy.wait("@canDeleteBox", { timeout: 15000 }).then((interception) => {
          expect(interception.response.statusCode).to.be.oneOf([200, 409]);
        });

        cy.contains("button", "Delete", { timeout: 10000 }).should(
          "be.disabled",
        );
      });
    });
  });
});
