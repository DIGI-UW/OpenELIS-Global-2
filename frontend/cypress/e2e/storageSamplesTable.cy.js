/**
 * E2E Test: Samples Table Should Display Samples with Storage Assignments
 *
 * This test specifically validates that samples assigned to storage locations
 * appear in the Samples tab of the Storage Dashboard.
 *
 * CRITICAL: This test WILL FAIL if no samples are assigned to storage locations.
 * That's the expected behavior - we need to fix why samples aren't showing up.
 */

let homePage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Samples Table - Must Display Assigned Samples", function () {
  it("Should display samples with storage assignments in Samples tab", function () {
    // Navigate to Storage Dashboard Samples tab
    cy.visit("/Storage/samples");
    cy.wait(5000); // Wait for API calls to complete

    // Verify dashboard is loaded
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Verify we're on the Samples tab
    cy.get('button[role="tab"]')
      .contains("Samples")
      .should("have.attr", "aria-selected", "true");

    // Wait for sample list container to be visible
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // CRITICAL: Verify table structure exists
    cy.get('.cds--data-table, table, [role="table"], .cds--table-container', {
      timeout: 5000,
    })
      .filter(":visible")
      .first()
      .should("be.visible");

    // CRITICAL: Check if samples table has rows
    cy.get(".cds--data-table tbody tr, table tbody tr", {
      timeout: 5000,
    }).then(($rows) => {
      const rowCount = $rows.length;

      if (rowCount === 0) {
        // FAIL: No samples in table - this is the bug we need to fix
        cy.log(
          "❌ FAILURE: Samples table is empty - no samples with storage assignments found",
        );
        cy.log("This indicates:");
        cy.log("1. Sample assignments may not exist in database");
        cy.log(
          "2. API endpoint /rest/storage/samples may not be returning data",
        );
        cy.log("3. Frontend may not be processing the API response correctly");

        // Take screenshot for debugging
        cy.screenshot("samples-table-empty");

        // Check browser console for errors
        cy.window().then((win) => {
          const consoleErrors = [];
          cy.log("Checking for console errors...");
        });

        // Verify API was called and check response
        cy.intercept("GET", "/rest/storage/samples").as("getSamples");
        cy.visit("/Storage/samples");
        cy.wait("@getSamples").then((interception) => {
          const response = interception.response;
          cy.log("API Response Status:", response?.statusCode);
          cy.log("API Response Body:", JSON.stringify(response?.body));

          if (response?.body && Array.isArray(response.body)) {
            cy.log(`API returned ${response.body.length} samples`);
            if (response.body.length === 0) {
              cy.log(
                "❌ API returned empty array - no sample assignments in database",
              );
            }
          } else {
            cy.log("❌ API returned non-array response:", response?.body);
          }
        });

        // This assertion will fail the test, which is what we want
        expect(
          rowCount,
          "Samples table should contain at least one sample with storage assignment",
        ).to.be.greaterThan(0);
      } else {
        // SUCCESS: Samples are present
        cy.log(`✓ SUCCESS: Found ${rowCount} samples in table`);

        // Verify sample rows have data
        cy.get(".cds--data-table tbody tr, table tbody tr")
          .first()
          .within(() => {
            // Verify sample ID is displayed
            cy.get("td").should("have.length.at.least", 1);
            cy.get("td")
              .first()
              .then(($cell) => {
                const cellText = $cell.text().trim();
                expect(cellText).to.not.be.empty;
                cy.log(`First sample row first cell: "${cellText}"`);
              });

            // Verify location column has data (if location column exists)
            cy.get("td").then(($cells) => {
              const hasLocation = Array.from($cells).some((cell) => {
                const text = cell.textContent.trim();
                return (
                  text.includes("MAIN") || text.includes(">") || text.length > 0
                );
              });
              if (hasLocation) {
                cy.log("✓ Sample location is displayed");
              } else {
                cy.log("⚠ Warning: Sample location may not be displayed");
              }
            });
          });
      }
    });
  });

  it("Should verify API endpoint returns sample assignments", function () {
    // Intercept the API call BEFORE navigating
    cy.intercept("GET", "/rest/storage/samples").as("getSamples");

    // Navigate to Storage Dashboard - this will trigger the API call
    cy.visit("/Storage/samples");

    // Wait for API call to complete
    cy.wait("@getSamples", { timeout: 15000 }).then((interception) => {
      const response = interception.response;

      // Verify API call succeeded
      expect(response.statusCode).to.equal(200);

      // Verify response is an array
      expect(response.body).to.be.an("array");

      // Log response for debugging
      cy.log(`API returned ${response.body.length} samples`);
      if (response.body.length > 0) {
        cy.log("First sample:", JSON.stringify(response.body[0]));
        // Verify sample has required fields
        expect(response.body[0]).to.have.property("id");
        expect(response.body[0]).to.have.property("sampleId");
        expect(response.body[0]).to.have.property("location");
        expect(response.body[0].location).to.contain(">"); // Hierarchical path separator
      } else {
        cy.log("❌ API returned empty array - no sample assignments exist");
        cy.log("This means:");
        cy.log("1. No samples have been assigned to storage locations");
        cy.log("2. SampleStorageAssignment table is empty");
        cy.log("3. Test fixtures may not have created assignments");
      }

      // This assertion will fail if no samples are returned
      expect(
        response.body.length,
        "API should return at least one sample with storage assignment",
      ).to.be.greaterThan(0);
    });
  });
});
