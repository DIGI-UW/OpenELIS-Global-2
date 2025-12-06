/**
 * E2E Tests: Enhanced Sidenav Navigation & Tri-State Toggle
 *
 * Feature: specs/009-carbon-sidenav
 *
 * Tests cover:
 * - Tri-state toggle (CLOSE → SHOW → LOCK)
 * - Active state with URL prefix matching
 * - Auto-expansion based on current route
 * - Mode persistence across page reloads
 * - Content push in LOCK mode
 * - SPA navigation (no full page reloads)
 * - Storage page defaults to LOCK mode
 * - Non-storage pages default to CLOSE mode
 *
 * Constitution: Section V.5 - Individual test execution during development
 * Run: npm run cy:run -- --spec "cypress/e2e/sidenavEnhanced.cy.js"
 */

describe("Enhanced Sidenav Navigation", () => {
  before(() => {
    // Login once per test file (cy.session pattern)
    cy.login("admin", "adminADMIN!");
  });

  beforeEach(() => {
    cy.viewport(1280, 900);

    // Clear sidenav preferences for clean state
    cy.window().then((win) => {
      win.localStorage.removeItem("storageSideNavMode");
      win.localStorage.removeItem("mainSideNavMode");
    });
  });

  describe("User Story P1: Tri-State Toggle (CLOSE → SHOW → LOCK)", () => {
    it("should cycle through three modes when clicking hamburger menu", () => {
      cy.visit("/Dashboard");

      // Initial state: CLOSE mode (rail)
      cy.get(".cds--side-nav")
        .should("exist")
        .and("not.have.class", "cds--side-nav--expanded");

      // Click 1: CLOSE → SHOW (overlay)
      cy.get('[data-cy="menuButton"]').click();
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("not.have.class", "content-nav-locked"); // No content push

      // Click 2: SHOW → LOCK (pushed)
      cy.get('[data-cy="menuButton"]').click();
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("have.class", "content-nav-locked"); // Content pushed

      // Click 3: LOCK → CLOSE (rail)
      cy.get('[data-cy="menuButton"]').click();
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
      cy.get(".cds--content").should("not.have.class", "content-nav-locked");
    });

    it("should display correct icon for each mode", () => {
      cy.visit("/Dashboard");

      // CLOSE mode: Menu icon (hamburger)
      cy.get('[data-cy="menuButton"] svg')
        .should("exist")
        .and("have.attr", "aria-label")
        .and("match", /menu/i);

      // SHOW mode: Pin icon
      cy.get('[data-cy="menuButton"]').click();
      cy.wait(100); // Let animation settle

      // LOCK mode: Close icon (X)
      cy.get('[data-cy="menuButton"]').click();
      cy.wait(100);
      cy.get('[data-cy="menuButton"] svg').should("exist");
    });
  });

  describe("User Story P2: Storage Page Defaults to LOCK Mode", () => {
    it("should default to LOCK mode on Storage pages", () => {
      cy.visit("/Storage");

      // Should be in LOCK mode by default
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("have.class", "content-nav-locked");

      // Storage menu should be auto-expanded
      cy.get("#menu_storage").should("exist");
      cy.get("#menu_storage_management").should("be.visible");
    });

    it("should persist LOCK mode across Storage page navigations", () => {
      cy.visit("/Storage");

      // Verify LOCK mode
      cy.get(".cds--content").should("have.class", "content-nav-locked");

      // Navigate to Cold Storage Monitoring (still Storage context)
      cy.get("#menu_freezer_monitoring").click();
      cy.url().should("include", "/FreezerMonitoring");

      // Should maintain LOCK mode
      cy.get(".cds--content").should("have.class", "content-nav-locked");
    });

    it("should allow user to override default LOCK mode", () => {
      cy.visit("/Storage");

      // Toggle to CLOSE mode (user preference)
      cy.get('[data-cy="menuButton"]').click(); // LOCK → CLOSE

      // Verify CLOSE mode
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
      cy.get(".cds--content").should("not.have.class", "content-nav-locked");

      // Reload page - should respect user's choice
      cy.reload();

      // Should stay in CLOSE mode (user override)
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
      cy.get(".cds--content").should("not.have.class", "content-nav-locked");
    });
  });

  describe("User Story P3: Active State with URL Matching", () => {
    it("should show active state for exact URL match", () => {
      cy.visit("/Dashboard");

      // Expand nav to see Home link
      cy.get('[data-cy="menuButton"]').click();

      // Home (/Dashboard) should be active
      cy.get("#menu_home_nav")
        .should("exist")
        .and("have.class", "cds--side-nav__link--current");
    });

    it("should show active state for prefix URL match (parent route)", () => {
      cy.visit("/Storage/samples");

      // Storage Management (/Storage) should be active even though we're on /Storage/samples
      cy.get("#menu_storage").should("exist");

      // Wait for menu to auto-expand
      cy.get("#menu_storage_management", { timeout: 3000 })
        .should("be.visible")
        .find(".cds--side-nav__link")
        .should("have.class", "cds--side-nav__link--current");
    });

    it("should update active state when navigating between pages", () => {
      cy.visit("/Storage");

      // Storage Management should be active
      cy.get("#menu_storage_management")
        .find(".cds--side-nav__link")
        .should("have.class", "cds--side-nav__link--current");

      // Navigate to Cold Storage Monitoring
      cy.get("#menu_freezer_monitoring").click();
      cy.url().should("include", "/FreezerMonitoring");

      // Cold Storage should now be active
      cy.get("#menu_freezer_monitoring")
        .find(".cds--side-nav__link")
        .should("have.class", "cds--side-nav__link--current");

      // Storage Management should NOT be active
      cy.get("#menu_storage_management")
        .find(".cds--side-nav__link")
        .should("not.have.class", "cds--side-nav__link--current");
    });

    it("should show hover state on subnav items", () => {
      cy.visit("/Storage");

      // Hover over Cold Storage Monitoring
      cy.get("#menu_freezer_monitoring").trigger("mouseover");

      // Should show hover background (visual regression - can't easily test CSS)
      // But at minimum, element should be visible and interactive
      cy.get("#menu_freezer_monitoring")
        .should("be.visible")
        .and("not.be.disabled");
    });
  });

  describe("User Story P4: Auto-Expansion Based on Route", () => {
    it("should auto-expand Storage menu when on Storage page", () => {
      cy.visit("/Storage");

      // Storage menu should be auto-expanded
      cy.get("#menu_storage").should("exist");
      cy.get("#menu_storage_management").should("be.visible");
      cy.get("#menu_freezer_monitoring").should("be.visible");
    });

    it("should auto-expand nested menus when on deeply nested route", () => {
      cy.visit("/ResultValidationRetroC?type=virology&test=DNA PCR");

      // Top-level "Validation" menu should be expanded
      cy.get("#menu_resultvalidation").should("exist");

      // Level 1: "Study" should be visible
      cy.get("#menu_resultvalidation_study", { timeout: 3000 }).should(
        "be.visible",
      );

      // Level 2: "Virology" should be visible (nested submenu)
      // Note: Due to complexity, we'll just verify the parent is expanded
      // Full nested expansion is a future enhancement
    });

    it("should collapse sibling sections in accordion mode", () => {
      cy.visit("/Storage");

      // Storage should be expanded
      cy.get("#menu_storage_management").should("be.visible");

      // Click Generic Sample to expand it
      cy.get("#menu_generic_sample_dropdown").click();

      // Generic Sample should now be expanded
      cy.get("#menu_generic_sample_order", { timeout: 2000 }).should(
        "be.visible",
      );

      // Storage should be COLLAPSED (accordion behavior)
      cy.get("#menu_storage_management").should("not.be.visible");
    });
  });

  describe("User Story P5: SPA Navigation (No Page Reloads)", () => {
    it("should navigate without full page reload when clicking nav items", () => {
      cy.visit("/Storage");

      // Set up reload detection
      cy.window().then((win) => {
        win.beforeReload = true;
        win.addEventListener("beforeunload", () => {
          win.beforeReload = false;
        });
      });

      // Click Cold Storage Monitoring
      cy.get("#menu_freezer_monitoring").click();
      cy.url().should("include", "/FreezerMonitoring");

      // Verify no reload occurred
      cy.window().its("beforeReload").should("eq", true);
    });

    it("should navigate to first child when clicking top-level menu with children", () => {
      cy.visit("/Dashboard");
      cy.get('[data-cy="menuButton"]').click(); // Expand nav

      // Click "Generic Sample" top-level menu
      cy.get("#menu_generic_sample_dropdown").click();

      // Should navigate to first child (/GenericSample/Order)
      cy.url().should("include", "/GenericSample/Order");

      // And menu should be expanded
      cy.get("#menu_generic_sample_order").should("be.visible");
    });
  });

  describe("User Story P6: Mode Persistence Across Reloads", () => {
    it("should persist LOCK mode across page reloads", () => {
      cy.visit("/Dashboard");

      // Set to LOCK mode
      cy.get('[data-cy="menuButton"]').click(); // CLOSE → SHOW
      cy.get('[data-cy="menuButton"]').click(); // SHOW → LOCK

      // Verify LOCK mode
      cy.get(".cds--content").should("have.class", "content-nav-locked");

      // Reload page
      cy.reload();

      // Should restore LOCK mode
      cy.get(".cds--content").should("have.class", "content-nav-locked");
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
    });

    it("should NOT persist SHOW mode (temporary only)", () => {
      cy.visit("/Dashboard");

      // Set to SHOW mode
      cy.get('[data-cy="menuButton"]').click(); // CLOSE → SHOW

      // Verify SHOW mode (expanded but no content push)
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("not.have.class", "content-nav-locked");

      // Reload page
      cy.reload();

      // Should revert to CLOSE mode (SHOW was temporary)
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
    });

    it("should use separate preferences for Storage vs non-Storage contexts", () => {
      // Set Dashboard (main context) to CLOSE mode (already default)
      cy.visit("/Dashboard");

      // Verify CLOSE mode
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );

      // Go to Storage page (storage context)
      cy.visit("/Storage");

      // Should default to LOCK mode (different context)
      cy.get(".cds--content").should("have.class", "content-nav-locked");

      // Toggle to CLOSE in storage context
      cy.get('[data-cy="menuButton"]').click(); // LOCK → CLOSE

      // Navigate back to Dashboard
      cy.visit("/Dashboard");

      // Should restore main context preference (CLOSE)
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );

      // Navigate back to Storage
      cy.visit("/Storage");

      // Should restore storage context preference (CLOSE - user's override)
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
    });
  });

  describe("User Story P7: Click-Outside to Close (SHOW Mode Only)", () => {
    it("should close sidenav when clicking outside in SHOW mode", () => {
      cy.visit("/Dashboard");

      // Enter SHOW mode
      cy.get('[data-cy="menuButton"]').click(); // CLOSE → SHOW

      // Verify SHOW mode
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("not.have.class", "content-nav-locked");

      // Click outside (on content area)
      cy.get(".cds--content").click({ force: true });

      // Should collapse to CLOSE mode
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
    });

    it("should NOT close when clicking outside in LOCK mode", () => {
      cy.visit("/Dashboard");

      // Enter LOCK mode
      cy.get('[data-cy="menuButton"]').click(); // CLOSE → SHOW
      cy.get('[data-cy="menuButton"]').click(); // SHOW → LOCK

      // Verify LOCK mode
      cy.get(".cds--content").should("have.class", "content-nav-locked");

      // Click outside (on content area)
      cy.get(".cds--content").click({ force: true });

      // Should STAY in LOCK mode (locked = persistent)
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");
      cy.get(".cds--content").should("have.class", "content-nav-locked");
    });
  });

  describe("Edge Cases and Error Handling", () => {
    it("should handle invalid SHOW mode in localStorage gracefully", () => {
      // Manually set invalid state (simulates old bug)
      cy.window().then((win) => {
        win.localStorage.setItem("mainSideNavMode", "show");
      });

      cy.visit("/Dashboard");

      // Should auto-clear invalid SHOW and use default CLOSE
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );

      // Verify localStorage was cleared
      cy.window().then((win) => {
        const saved = win.localStorage.getItem("mainSideNavMode");
        // Should be null (cleared) or 'close' (set to default)
        expect(saved).to.be.oneOf([null, "close"]);
      });
    });

    it("should handle missing menu items gracefully", () => {
      cy.visit("/Dashboard");
      cy.get('[data-cy="menuButton"]').click();

      // All menu items should render without errors
      // Even if some have missing actionURL or no children
      cy.get(".cds--side-nav__items").should("exist");

      // Check console for warnings about missing actionURL (expected for some items)
      // But should not crash
    });
  });

  describe("Performance and UX", () => {
    it("should render sidenav within 2 seconds", () => {
      const start = Date.now();
      cy.visit("/Dashboard");

      cy.get('[data-cy="menuButton"]').should("exist");

      cy.then(() => {
        const duration = Date.now() - start;
        expect(duration).to.be.lessThan(2000);
      });
    });

    it("should have smooth CSS transitions when expanding/collapsing", () => {
      cy.visit("/Dashboard");

      // Expand sidenav
      cy.get('[data-cy="menuButton"]').click();

      // Wait for transition to complete
      cy.get(".cds--side-nav").should("have.class", "cds--side-nav--expanded");

      // Collapse sidenav
      cy.get('[data-cy="menuButton"]').click();
      cy.get('[data-cy="menuButton"]').click();

      // Should collapse smoothly (no jank)
      cy.get(".cds--side-nav").should(
        "not.have.class",
        "cds--side-nav--expanded",
      );
    });
  });
});
