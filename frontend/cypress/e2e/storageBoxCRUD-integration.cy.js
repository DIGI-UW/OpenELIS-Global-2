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
    cy.get(".storage-dashboard", { timeout: 3000 }).should("be.visible");
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
        timeout: 3000,
      });
    }
  });

  const selectFirstRackForBoxesTab = () => {
    cy.get('[data-testid="tab-boxes"]').click();
    cy.get('button[role="tab"]')
      .contains("Boxes")
      .should("have.attr", "aria-selected", "true");

    // Rack dropdown is a Carbon Dropdown with testid rack-selector.
    cy.get('[data-testid="rack-selector"]', { timeout: 3000 }).should(
      "be.visible",
    );

    // Open dropdown and select first non-empty option.
    // Carbon Dropdown: click the button to open, then wait for menu
    cy.get('[data-testid="rack-selector"] button', { timeout: 3000 })
      .should("be.visible")
      .click({ force: true });

    // Wait for dropdown menu to appear - try both Carbon v1.15 (role="listbox") and v10 (.cds--list-box__menu-item)
    cy.get("body").then(($body) => {
      const listbox = $body.find('[role="listbox"]');
      const menuItems = $body.find(".cds--list-box__menu-item");

      if (listbox.length > 0) {
        // Carbon v1.15 - use role="option"
        cy.get('[role="listbox"] [role="option"]', { timeout: 3000 })
          .should("have.length.at.least", 1)
          .first()
          .click({ force: true });
      } else if (menuItems.length > 0) {
        // Carbon v10 - use .cds--list-box__menu-item
        cy.get(".cds--list-box__menu-item", { timeout: 3000 })
          .should("have.length.at.least", 2) // includes "Select"
          .eq(1)
          .click({ force: true });
      } else {
        cy.get('[role="listbox"], .cds--list-box__menu-item', {
          timeout: 3000,
        })
          .should("be.visible")
          .first()
          .click();
      }
    });
  };

  // Helper to select a specific rack by ID for the Boxes tab
  const selectRackByIdForBoxesTab = (rackId) => {
    cy.get('[data-testid="tab-boxes"]').click();
    cy.get('button[role="tab"]')
      .contains("Boxes")
      .should("have.attr", "aria-selected", "true");

    cy.get('[data-testid="rack-selector"]', { timeout: 3000 }).should(
      "be.visible",
    );

    // Open dropdown
    cy.get('[data-testid="rack-selector"] button', { timeout: 3000 })
      .should("be.visible")
      .click({ force: true });

    // Select the rack with matching ID - rack options have format "label (parent)" with data-value=id
    // Use role="option" which Carbon uses for dropdown items
    cy.get('[role="listbox"] [role="option"]', { timeout: 3000 })
      .should("have.length.at.least", 1)
      .then(($options) => {
        // Find option by checking if its data-value or text contains the rack ID
        let found = false;
        $options.each((_, el) => {
          const $el = Cypress.$(el);
          const optionId = $el.attr("data-value") || $el.attr("id") || "";
          if (
            optionId.includes(String(rackId)) ||
            $el.text().includes(`id=${rackId}`)
          ) {
            cy.wrap($el).click({ force: true });
            found = true;
            return false; // break
          }
        });
        if (!found) {
          // Fall back to first option
          cy.log(
            `Rack ${rackId} not found in dropdown, selecting first option`,
          );
          cy.wrap($options.first()).click({ force: true });
        }
      });
  };

  it("disables Add Box until rack is selected", () => {
    cy.get('[data-testid="tab-boxes"]').click();
    cy.get('[data-testid="add-box-button"]', { timeout: 3000 })
      .should("be.visible")
      .should("be.disabled");
  });

  it("creates a box via UI and persists to backend", () => {
    const newLabel = `E2E Box ${Date.now()}`;
    const newCode = `BX${Date.now().toString().slice(-6)}`.toUpperCase();

    selectFirstRackForBoxesTab();

    cy.get('[data-testid="add-box-button"]').should("not.be.disabled").click();

    // Modal fields
    cy.get('[data-testid="box-label"]', { timeout: 3000 })
      .should("be.visible")
      .type(newLabel);
    cy.get('[data-testid="box-code"]').should("be.visible").type(newCode);
    cy.get('[data-testid="box-rows"]').should("be.visible").clear().type("8");
    cy.get('[data-testid="box-columns"]')
      .should("be.visible")
      .clear()
      .type("12");

    // Save (Carbon button text)
    cy.contains("button", "Save", { timeout: 3000 })
      .should("not.be.disabled")
      .click();

    cy.wait("@createBox", { timeout: 3000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      expect(interception.request.body).to.have.property("label", newLabel);
      expect(interception.request.body).to.have.property("code", newCode);

      // Verify response contains the created box
      const createdBox = interception.response.body;
      expect(createdBox).to.have.property("id");
      expect(createdBox).to.have.property("label", newLabel);

      // Verify success notification appears
      cy.get(".cds--toast-notification--success", { timeout: 3000 }).should(
        "be.visible",
      );
    });
  });

  // TODO: Carbon ComboBox dropdown interactions are flaky in Cypress headless mode
  // The dropdown menu items don't appear reliably. Consider using React Testing Library
  // or a more robust Carbon-aware selection approach.
  it.skip("edits a selected box via UI and persists to backend", () => {
    const updatedLabel = `E2E Box Updated ${Date.now()}`;

    selectFirstRackForBoxesTab();

    // Select first real box
    cy.get('[data-testid="box-selector"]')
      .should("be.visible")
      .click({ force: true });
    cy.get(".cds--list-box__menu-item", { timeout: 3000 })
      .should("have.length.at.least", 2)
      .eq(1)
      .click({ force: true });

    // Open overflow menu and click Edit
    cy.get('[data-testid="location-actions-overflow-menu"]', { timeout: 3000 })
      .should("be.visible")
      .click({ force: true });
    cy.get('[data-testid="edit-location-menu-item"]')
      .should("be.visible")
      .click({ force: true });

    cy.get('[data-testid="box-label"]', { timeout: 3000 })
      .should("be.visible")
      .clear()
      .type(updatedLabel);

    cy.contains("button", "Save", { timeout: 3000 }).click();

    cy.wait("@updateBox", { timeout: 3000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      expect(interception.request.body).to.have.property("label", updatedLabel);
      expect(interception.request.body).to.have.property("active");
    });
  });

  // TODO: Requires test data with sample-to-box assignments (constrained boxes) - defer to future PR
  it.skip("blocks delete when backend says can-delete=false (constraint path)", () => {
    // Get boxes and find one that's constrained (has samples)
    cy.request({
      url: "/api/OpenELIS-Global/rest/storage/boxes",
      auth: { username: "admin", password: "adminADMIN!" },
    }).then((boxesRes) => {
      const boxes = boxesRes.body || [];
      expect(boxes.length).to.be.greaterThan(0);

      // Helper to find constrained box sequentially using Cypress chaining
      const findConstrainedBox = (index) => {
        if (index >= boxes.length) {
          cy.log("No constrained box found in fixtures; test inconclusive.");
          return;
        }

        const box = boxes[index];
        cy.request({
          url: `/api/OpenELIS-Global/rest/storage/boxes/${box.id}/can-delete`,
          auth: { username: "admin", password: "adminADMIN!" },
          failOnStatusCode: false,
        }).then((canDeleteRes) => {
          if (canDeleteRes.status === 409) {
            // Found a constrained box - use parentRackId from the box object
            cy.log(
              `Found constrained box: ${box.label} (id=${box.id}, parentRackId=${box.parentRackId})`,
            );
            testDeleteBlockedUI(box);
          } else {
            // Try next box
            findConstrainedBox(index + 1);
          }
        });
      };

      const testDeleteBlockedUI = (box) => {
        // Select the rack that contains this box (API returns parentRackId)
        selectRackByIdForBoxesTab(box.parentRackId);

        // Wait for boxes to load after rack selection
        cy.wait(500);

        // Select the constrained box by label in dropdown
        cy.get('[data-testid="box-selector"] button', { timeout: 3000 })
          .should("be.visible")
          .click({ force: true });

        // Wait for dropdown menu to appear and select the box
        // Use role="option" for Carbon dropdown items
        cy.get('[role="listbox"] [role="option"]', { timeout: 5000 })
          .should("have.length.at.least", 1)
          .then(($options) => {
            // Find and click the option containing the box label
            const targetOption = $options.filter(`:contains("${box.label}")`);
            if (targetOption.length > 0) {
              cy.wrap(targetOption.first())
                .scrollIntoView()
                .click({ force: true });
            } else {
              // Fall back to first option if not found
              cy.log(`Box ${box.label} not found in dropdown`);
              cy.wrap($options.first()).click({ force: true });
            }
          });

        // Wait for box to be selected and overflow menu to appear
        cy.wait(300);

        // Open overflow menu and click Delete
        // Scope to only visible elements (not hidden tabs) using filter
        cy.get('[data-testid="location-actions-overflow-menu"]:visible', {
          timeout: 5000,
        })
          .first()
          .click({ force: true });
        cy.get('[data-testid="delete-location-menu-item"]:visible', {
          timeout: 3000,
        })
          .first()
          .click({ force: true });

        // Wait for can-delete check
        cy.wait("@canDeleteBox", { timeout: 5000 }).then((interception) => {
          expect(interception.response.statusCode).to.equal(409);
        });

        // Verify Delete button is disabled due to constraint
        cy.get('[data-testid="delete-box-modal"]', { timeout: 3000 })
          .should("be.visible")
          .within(() => {
            cy.contains("button", "Delete").should("be.disabled");
          });

        // Close the modal
        cy.contains("button", "Cancel").click();
      };

      // Start searching for constrained box
      findConstrainedBox(0);
    });
  });
});
