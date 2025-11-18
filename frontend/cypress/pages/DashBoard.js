class DashBoardPage {
  constructor() {}

  searchPatientByFName() {
    cy.get("#firstName").should("be.visible").type("John");
  }
  searchPatient() {
    cy.get("#local_search").click();
  }

  checkPatientRadio() {
    cy.get("table tbody tr").first().find("td label span").click();
  }

  clickNext() {
    cy.get(".forwardButton").click();
  }

  selectHistopathology() {
    cy.get("#additionalQuestionsSelect").select("Histopathology");
  }

  selectImmunoChem() {
    cy.get("#additionalQuestionsSelect").select("Immunohistochemistry");
  }

  selectCytology() {
    cy.get("#additionalQuestionsSelect").select("Cytology");
  }

  selectFluidSample() {
    cy.get("#sampleId_0").should("be.visible").select("Fluid");
  }

  checkCovidPanel() {
    cy.contains("label.cds--checkbox-label", "COVID-19 PCR").click();
  }

  selectPathologySample() {
    cy.get("#sampleId_0")
      .should("be.visible")
      .select("Histopathology specimen");
  }

  selectImmunoChemSample() {
    cy.get("#sampleId_0")
      .should("be.visible")
      .select("Immunohistochemistry specimen");
  }

  checkImmunoChemTest() {
    cy.contains(".cds--checkbox-label-text", "Anti-CD7").click();
  }
  checkPathologyPanel() {
    cy.contains(
      "label.cds--checkbox-label",
      "Histopathology examination",
    ).click();
  }

  generateLabNo() {
    cy.get("[data-cy='generate-labNumber']").click();
  }
  selectSite() {
    console.log("=== SELECT SITE DEBUG ===");

    cy.get("#siteName")
      .should("be.visible")
      .then(($input) => {
        console.log(`Input value before typing: "${$input.val()}"`);
        console.log(`Input is enabled: ${!$input.prop("disabled")}`);
      });

    // Type to trigger suggestions
    cy.get("#siteName")
      .type("CAMES MAN")
      .then(() => {
        console.log("Typed 'CAMES MAN'");
      });

    // Check if suggestions appeared - with detailed logging
    cy.get("body").then(($body) => {
      const hasContainer = $body.find(".suggestions-container").length > 0;
      const hasSuggestions = $body.find("ul.suggestions").length > 0;
      const hasItems = $body.find('[data-cy="auto-suggestion"]').length > 0;
      console.log(`Suggestions container exists: ${hasContainer}`);
      console.log(`Suggestions list exists: ${hasSuggestions}`);
      console.log(`Suggestion items exist: ${hasItems}`);

      if (hasItems) {
        const items = $body.find('[data-cy="auto-suggestion"]');
        console.log(`Found ${items.length} suggestion items:`);
        items.each((index, el) => {
          console.log(`  [${index}] "${el.textContent}"`);
        });
      } else {
        console.log("NO SUGGESTION ITEMS FOUND!");
        // Check if input has the typed value
        const inputValue = $body.find("#siteName").val();
        console.log(`Input value is: "${inputValue}"`);
      }
    });

    // Wait for suggestions to appear - check for items directly (container might have height 0)
    cy.get('[data-cy="auto-suggestion"]', { timeout: 10000 })
      .should("have.length.greaterThan", 0)
      .first()
      .should("be.visible")
      .should("contain.text", "CAMES MAN")
      .click();
    console.log("Clicked on CAMES MAN suggestion");

    // Wait for suggestions to disappear (selection complete)
    cy.get(".suggestions-container").should("not.exist");

    // Verify the input value was set (might have " - CAMES MAN" format)
    cy.get("#siteName").should("contain.value", "CAMES MAN");
    console.log("Verified siteName input contains 'CAMES MAN'");
    console.log("=== END SELECT SITE DEBUG ===");
  }

  selectRequesting() {
    // Providers are formatted as "LastName, FirstName" (e.g., "Optimus, Prime")
    cy.get("#requesterId").should("be.visible").type("Optimus");
    console.log("Typed 'Optimus' into requesterId field");

    // Wait for suggestions to appear - check for items directly (container might have height 0)
    // The provider should be "Optimus, Prime" but typing "Optimus" should filter to it
    cy.get('[data-cy="auto-suggestion"]', { timeout: 10000 })
      .should("have.length.greaterThan", 0)
      .first()
      .should("be.visible")
      .should("contain.text", "Optimus") // Match "Optimus, Prime"
      .click();
    console.log("Clicked on Optimus suggestion");

    // Wait for suggestions to disappear (selection complete)
    cy.get(".suggestions-container").should("not.exist");

    // Verify the input value was set (might have " - Optimus" format)
    cy.get("#requesterId").should("contain.value", "Optimus");
    console.log("Verified requesterId input contains 'Optimus'");
  }

  submitButton() {
    cy.contains(".forwardButton", "Submit").should("be.visible").click();
  }

  clickPrintBarCode() {
    cy.get(".orderEntrySuccessMsg").should("be.visible");
    cy.get('[data-cy="printBarCode"]').should("be.visible").click();
  }

  selectFirstOrder() {
    // Wait for table to be visible and have data rows
    cy.get("table", { timeout: 15000 }).should("be.visible");

    // Log current table state for debugging
    cy.get("table").then(($table) => {
      const tbody = $table.find("tbody");
      const rows = tbody.find("tr");
      console.log(`=== SELECT FIRST ORDER DEBUG ===`);
      console.log(`Table visible: ${$table.is(":visible")}`);
      console.log(`Tbody exists: ${tbody.length > 0}`);
      console.log(
        `Tbody visible: ${tbody.length > 0 ? tbody.is(":visible") : false}`,
      );
      console.log(`Tbody height: ${tbody.length > 0 ? tbody.height() : 0}`);
      console.log(`Rows count: ${rows.length}`);
      if (rows.length > 0) {
        console.log(`First row text: "${rows.first().text()}"`);
      }
      console.log(`=== END SELECT FIRST ORDER DEBUG ===`);
    });

    // Wait for tbody to exist and have rows (following testing roadmap - use tbody to exclude header)
    // Use a more specific selector that waits for actual data rows
    cy.get("table tbody tr", { timeout: 15000 })
      .should("have.length.greaterThan", 0)
      .first()
      .should("be.visible")
      .click();
  }

  saveOrder() {
    cy.get("#pathology_save2").click();
  }

  selectPathologyStatus() {
    cy.get("#status").select("Processing");
  }

  pathologyStatusFilter() {
    cy.get("#statusFilter").should("be.visible").select("Processing");
  }
  selectStatus() {
    cy.get("#status").should("be.visible").select("Completed");
  }
  selectPathologist() {
    cy.get("#assignedPathologist").select("ELIS,Open");
  }

  selectTechnician() {
    cy.get("#assignedTechnician").select("External,Service");
  }

  checkImmunoChem() {
    cy.get("#referToImmunoHistoChemistry").check();
  }

  chckImmunoChemOption() {
    cy.get("#ihctest-input").select("Anti-CD 5(Immunohistochemistry specimen)");
  }

  addImmunoChemReport() {
    cy.get("#report").select("Immunohistochemistry Report");
  }

  checkReadyForRelease() {
    cy.get("#release").should("be.visible").check({ force: true });
  }

  statusFilter() {
    cy.get("#statusFilter").should("be.visible").select("Completed");
  }
}

export default DashBoardPage;
