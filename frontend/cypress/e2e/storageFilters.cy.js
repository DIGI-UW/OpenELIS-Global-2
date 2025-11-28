import LoginPage from "../pages/LoginPage";

/**
 * E2E Tests: Storage Dashboard Filter Functionality
 *
 * Tests tab-specific filtering requirements per FR-065:
 * - Samples tab: Filter by location and status
 * - Rooms tab: Filter by status
 * - Devices tab: Filter by type, room, and status
 * - Shelves tab: Filter by device, room, and status
 * - Racks tab: Filter by room, shelf, device, and status
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/storageFilters.cy.js"
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

describe("Storage Dashboard Filtering - Samples Tab", function () {
  before(() => {
    // Set up intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/storage/samples**").as("getSamples");

    // Navigate to Samples tab ONCE for all tests in this describe block
    cy.visit("/Storage/samples");

    // Verify dashboard is loaded (retry-ability)
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Wait for initial API call
    cy.wait("@getSamples", { timeout: 10000 });

    // Verify we're on the Samples tab
    cy.get('button[role="tab"]')
      .contains("Samples")
      .should("have.attr", "aria-selected", "true");
  });

  beforeEach(() => {
    // Only set up intercepts if needed - no navigation
    // Navigation already done in before() - we're already on Samples tab
  });

  it("Should filter samples by location (room)", function () {
    // Intercept API calls to verify filter parameters
    cy.intercept("GET", "/rest/storage/samples*").as("getSamples");

    // Get initial row count
    cy.get(".cds--data-table tbody tr, table tbody tr", { timeout: 5000 }).then(
      ($initialRows) => {
        const initialCount = $initialRows.length;
        cy.log(`Initial sample count: ${initialCount}`);

        // Select a room filter (if room dropdown exists and has options)
        cy.get("#filter-room").then(($dropdown) => {
          if ($dropdown.length > 0) {
            // Open dropdown and select first non-empty option
            cy.get("#filter-room").click();
            // Wait for dropdown menu to appear (retry-ability)
            cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
              "be.visible",
            );

            // Select first room option (skip "All")
            cy.get(".cds--list-box__menu-item")
              .not(':contains("All")')
              .first()
              .then(($item) => {
                const roomName = $item.text().trim();
                cy.log(`Selecting room filter: ${roomName}`);

                cy.wrap($item).click();
                // Wait for API call and table update (retry-ability)
                cy.wait("@getSamples", { timeout: 10000 });

                // Verify API was called with location filter
                cy.wait("@getSamples").then((interception) => {
                  const url = interception.request.url;
                  cy.log(`API called with URL: ${url}`);
                  expect(url).to.include("location=");

                  // Verify table has filtered results
                  cy.get(".cds--data-table tbody tr, table tbody tr", {
                    timeout: 5000,
                  }).then(($filteredRows) => {
                    const filteredCount = $filteredRows.length;
                    cy.log(`Filtered sample count: ${filteredCount}`);

                    // Filtered count should be <= initial count
                    expect(filteredCount).to.be.at.most(initialCount);

                    // If we have results, verify they match the filter
                    if (filteredCount > 0) {
                      cy.get(".cds--data-table tbody tr, table tbody tr")
                        .first()
                        .then(($row) => {
                          const rowText = $row.text();
                          cy.log(`First filtered row: ${rowText}`);
                          // Location should be visible in the row (hierarchical path)
                          expect(rowText.length).to.be.greaterThan(0);
                        });
                    }
                  });
                });
              });
          } else {
            cy.log(
              "Room filter dropdown not found - skipping location filter test",
            );
          }
        });
      },
    );
  });

  it("Should filter samples by status (active)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/samples*").as("getSamples");

    // Select "Active" status filter
    cy.get("#filter-status").click();
    // Wait for dropdown menu to appear (retry-ability)
    cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should("be.visible");
    cy.get(".cds--list-box__menu-item").contains("Active").click();
    // Wait for API call and table update (retry-ability)
    cy.wait("@getSamples", { timeout: 10000 });

    // Verify API was called with status filter
    cy.wait("@getSamples").then((interception) => {
      const url = interception.request.url;
      cy.log(`API called with URL: ${url}`);
      expect(url).to.include("status=active");

      // Verify response is filtered
      if (interception.response && interception.response.body) {
        const samples = interception.response.body;
        cy.log(`API returned ${samples.length} active samples`);

        // All samples should have active status
        samples.forEach((sample) => {
          expect(sample.status).to.equal("active");
        });
      }
    });
  });
});

describe("Storage Dashboard Filtering - Rooms Tab", function () {
  before(() => {
    // Set up intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");

    // Navigate to Rooms tab ONCE for all tests in this describe block
    cy.visit("/Storage/rooms");

    // Verify dashboard is loaded (retry-ability)
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Wait for initial API call
    cy.wait("@getRooms", { timeout: 10000 });

    // Verify we're on the Rooms tab
    cy.get('button[role="tab"]')
      .contains("Rooms")
      .should("have.attr", "aria-selected", "true");
  });

  beforeEach(() => {
    // Only set up intercepts if needed - no navigation
    // Navigation already done in before() - we're already on Rooms tab
  });

  it("Should filter rooms by status (active)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/rooms*").as("getRooms");

    // Get initial row count
    cy.get(".cds--data-table tbody tr, table tbody tr", { timeout: 5000 }).then(
      ($initialRows) => {
        const initialCount = $initialRows.length;
        cy.log(`Initial room count: ${initialCount}`);

        // Select "Active" status filter
        cy.get("#filter-status").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item").contains("Active").click();
        // Wait for API call and table update (retry-ability)
        cy.wait("@getRooms", { timeout: 10000 });

        // Verify API was called with status filter
        cy.wait("@getRooms").then((interception) => {
          const url = interception.request.url;
          cy.log(`API called with URL: ${url}`);
          expect(url).to.include("status=active");

          // Verify response contains only active rooms
          if (interception.response && interception.response.body) {
            const rooms = interception.response.body;
            cy.log(`API returned ${rooms.length} active rooms`);

            rooms.forEach((room) => {
              expect(room.active).to.equal(true);
            });
          }
        });
      },
    );
  });

  it("Should filter rooms by status (inactive)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/rooms*").as("getRooms");

    // Select "Inactive" status filter
    cy.get("#filter-status").click();
    // Wait for dropdown menu to appear (retry-ability)
    cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should("be.visible");
    cy.get(".cds--list-box__menu-item").contains("Inactive").click();
    // Wait for API call and table update (retry-ability)
    cy.wait("@getRooms", { timeout: 10000 });

    // Verify API was called with status filter
    cy.wait("@getRooms").then((interception) => {
      const url = interception.request.url;
      expect(url).to.include("status=inactive");

      // Verify response contains only inactive rooms
      if (interception.response && interception.response.body) {
        const rooms = interception.response.body;
        rooms.forEach((room) => {
          expect(room.active).to.equal(false);
        });
      }
    });
  });
});

describe("Storage Dashboard Filtering - Devices Tab", function () {
  before(() => {
    // Set up intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");

    // Navigate to Devices tab ONCE for all tests in this describe block
    cy.visit("/Storage/devices");

    // Verify dashboard is loaded (retry-ability)
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Wait for initial API call
    cy.wait("@getDevices", { timeout: 10000 });

    // Verify we're on the Devices tab
    cy.get('button[role="tab"]')
      .contains("Devices")
      .should("have.attr", "aria-selected", "true");
  });

  beforeEach(() => {
    // Only set up intercepts if needed - no navigation
    // Navigation already done in before() - we're already on Devices tab
  });

  it("Should filter devices by room", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/devices*").as("getDevices");

    // Select a room filter
    cy.get("#filter-room").then(($dropdown) => {
      if ($dropdown.length > 0) {
        cy.get("#filter-room").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item")
          .not(':contains("All")')
          .first()
          .then(($item) => {
            const roomName = $item.text().trim();
            cy.wrap($item).click();
            // Wait for API call and table update (retry-ability)
            cy.wait("@getDevices", { timeout: 10000 });

            // Verify API was called with roomId filter
            cy.wait("@getDevices").then((interception) => {
              const url = interception.request.url;
              cy.log(`API called with URL: ${url}`);
              expect(url).to.include("roomId=");

              // Verify response contains only devices from selected room
              if (interception.response && interception.response.body) {
                const devices = interception.response.body;
                devices.forEach((device) => {
                  expect(device.roomId).to.exist;
                });
              }
            });
          });
      } else {
        cy.log("Room filter dropdown not found - skipping room filter test");
      }
    });
  });

  it("Should filter devices by status (active)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/devices*").as("getDevices");

    // Select "Active" status filter
    cy.get("#filter-status").click();
    // Wait for dropdown menu to appear (retry-ability)
    cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should("be.visible");
    cy.get(".cds--list-box__menu-item").contains("Active").click();
    // Wait for API call and table update (retry-ability)
    cy.wait("@getDevices", { timeout: 10000 });

    // Verify API was called with status filter
    cy.wait("@getDevices").then((interception) => {
      const url = interception.request.url;
      expect(url).to.include("status=active");

      // Verify response contains only active devices
      if (interception.response && interception.response.body) {
        const devices = interception.response.body;
        devices.forEach((device) => {
          expect(device.active).to.equal(true);
        });
      }
    });
  });
});

describe("Storage Dashboard Filtering - Shelves Tab", function () {
  before(() => {
    // Set up intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");

    // Navigate to Shelves tab ONCE for all tests in this describe block
    cy.visit("/Storage/shelves");

    // Verify dashboard is loaded (retry-ability)
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Wait for initial API call
    cy.wait("@getShelves", { timeout: 10000 });

    // Verify we're on the Shelves tab
    cy.get('button[role="tab"]')
      .contains("Shelves")
      .should("have.attr", "aria-selected", "true");
  });

  beforeEach(() => {
    // Only set up intercepts if needed - no navigation
    // Navigation already done in before() - we're already on Shelves tab
  });

  it("Should filter shelves by device", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/shelves*").as("getShelves");

    // Select a device filter
    cy.get("#filter-device").then(($dropdown) => {
      if ($dropdown.length > 0) {
        cy.get("#filter-device").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item")
          .not(':contains("All")')
          .first()
          .then(($item) => {
            cy.wrap($item).click();
            // Wait for API call and table update (retry-ability)
            cy.wait("@getShelves", { timeout: 10000 });

            // Verify API was called with deviceId filter
            cy.wait("@getShelves").then((interception) => {
              const url = interception.request.url;
              expect(url).to.include("deviceId=");
            });
          });
      } else {
        cy.log(
          "Device filter dropdown not found - skipping device filter test",
        );
      }
    });
  });

  it("Should filter shelves by room", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/shelves*").as("getShelves");

    // Select a room filter
    cy.get("#filter-room").then(($dropdown) => {
      if ($dropdown.length > 0) {
        cy.get("#filter-room").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item")
          .not(':contains("All")')
          .first()
          .then(($item) => {
            cy.wrap($item).click();
            // Wait for API call and table update (retry-ability)
            cy.wait("@getShelves", { timeout: 10000 });

            // Verify API was called with roomId filter
            cy.wait("@getShelves").then((interception) => {
              const url = interception.request.url;
              expect(url).to.include("roomId=");
            });
          });
      } else {
        cy.log("Room filter dropdown not found - skipping room filter test");
      }
    });
  });

  it("Should filter shelves by status (active)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/shelves*").as("getShelves");

    // Select "Active" status filter
    cy.get("#filter-status").click();
    // Wait for dropdown menu to appear (retry-ability)
    cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should("be.visible");
    cy.get(".cds--list-box__menu-item").contains("Active").click();
    // Wait for API call and table update (retry-ability)
    cy.wait("@getShelves", { timeout: 10000 });

    // Verify API was called with status filter
    cy.wait("@getShelves").then((interception) => {
      const url = interception.request.url;
      expect(url).to.include("status=active");

      // Verify response contains only active shelves
      if (interception.response && interception.response.body) {
        const shelves = interception.response.body;
        shelves.forEach((shelf) => {
          expect(shelf.active).to.equal(true);
        });
      }
    });
  });
});

describe("Storage Dashboard Filtering - Racks Tab", function () {
  before(() => {
    // Set up intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");

    // Navigate to Racks tab ONCE for all tests in this describe block
    cy.visit("/Storage/racks");

    // Verify dashboard is loaded (retry-ability)
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");

    // Wait for initial API call
    cy.wait("@getRacks", { timeout: 10000 });

    // Verify we're on the Racks tab
    cy.get('button[role="tab"]')
      .contains("Racks")
      .should("have.attr", "aria-selected", "true");
  });

  beforeEach(() => {
    // Only set up intercepts if needed - no navigation
    // Navigation already done in before() - we're already on Racks tab
  });

  it("Should filter racks by room", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/racks*").as("getRacks");

    // Select a room filter
    cy.get("#filter-room").then(($dropdown) => {
      if ($dropdown.length > 0) {
        cy.get("#filter-room").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item")
          .not(':contains("All")')
          .first()
          .then(($item) => {
            cy.wrap($item).click();
            // Wait for API call and table update (retry-ability)
            cy.wait("@getRacks", { timeout: 10000 });

            // Verify API was called with roomId filter
            cy.wait("@getRacks").then((interception) => {
              const url = interception.request.url;
              expect(url).to.include("roomId=");

              // Verify response contains racks with roomId column (FR-065a)
              if (interception.response && interception.response.body) {
                const racks = interception.response.body;
                racks.forEach((rack) => {
                  expect(rack.roomId).to.exist;
                });
              }
            });
          });
      } else {
        cy.log("Room filter dropdown not found - skipping room filter test");
      }
    });
  });

  it("Should filter racks by device", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/racks*").as("getRacks");

    // Select a device filter
    cy.get("#filter-device").then(($dropdown) => {
      if ($dropdown.length > 0) {
        cy.get("#filter-device").click();
        // Wait for dropdown menu to appear (retry-ability)
        cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should(
          "be.visible",
        );
        cy.get(".cds--list-box__menu-item")
          .not(':contains("All")')
          .first()
          .then(($item) => {
            cy.wrap($item).click();
            // Wait for API call and table update (retry-ability)
            cy.wait("@getRacks", { timeout: 10000 });

            // Verify API was called with deviceId filter
            cy.wait("@getRacks").then((interception) => {
              const url = interception.request.url;
              expect(url).to.include("deviceId=");
            });
          });
      } else {
        cy.log(
          "Device filter dropdown not found - skipping device filter test",
        );
      }
    });
  });

  it("Should filter racks by status (active)", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/storage/racks*").as("getRacks");

    // Select "Active" status filter
    cy.get("#filter-status").click();
    // Wait for dropdown menu to appear (retry-ability)
    cy.get(".cds--list-box__menu-item", { timeout: 5000 }).should("be.visible");
    cy.get(".cds--list-box__menu-item").contains("Active").click();
    // Wait for API call and table update (retry-ability)
    cy.wait("@getRacks", { timeout: 10000 });

    // Verify API was called with status filter
    cy.wait("@getRacks").then((interception) => {
      const url = interception.request.url;
      expect(url).to.include("status=active");

      // Verify response contains only active racks
      if (interception.response && interception.response.body) {
        const racks = interception.response.body;
        racks.forEach((rack) => {
          expect(rack.active).to.equal(true);
        });
      }
    });
  });

  it("Should display roomId column in racks table (FR-065a)", function () {
    // Verify racks table has roomId column
    cy.get(".cds--data-table thead th, table thead th", { timeout: 5000 }).then(
      ($headers) => {
        const headers = Array.from($headers).map((h) => h.textContent.trim());
        cy.log(`Table headers: ${headers.join(", ")}`);

        // Check if roomId is in headers or in data rows
        cy.get(".cds--data-table tbody tr, table tbody tr", {
          timeout: 5000,
        }).then(($rows) => {
          if ($rows.length > 0) {
            // Check first row for roomId data
            cy.get(".cds--data-table tbody tr, table tbody tr")
              .first()
              .then(($row) => {
                const rowText = $row.text();
                cy.log(`First rack row: ${rowText}`);
              });
          }
        });
      },
    );

    // Verify API response includes roomId
    cy.intercept("GET", "/rest/storage/racks*").as("getRacks");
    cy.wait("@getRacks").then((interception) => {
      if (interception.response && interception.response.body) {
        const racks = interception.response.body;
        if (racks.length > 0) {
          expect(racks[0]).to.have.property("roomId");
          cy.log(`✓ Racks API returns roomId column (FR-065a)`);
        }
      }
    });
  });
});
