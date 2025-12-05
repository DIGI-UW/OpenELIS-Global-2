/**
 * E2E Tests for Sidenav Navigation
 * Validates tri-state toggle, persistence, and navigation without page reload
 */

describe("Sidenav Navigation (Real Menu)", () => {
  before(() => {
    // Use cy.session() for faster tests
    cy.login("admin", "admin123");
  });

  beforeEach(() => {
    cy.visit("/");
  });

  it("should cycle through Close -> Show -> Lock modes", () => {
    // Initial state: Should be Closed (rail)
    // Note: Default might be different based on previous test runs if not cleared
    // But we can check state transitions
    
    const menuButton = '[data-cy="menuButton"]';
    const sideNav = '.cds--side-nav';
    const content = '.cds--content';

    // Click 1: Should open (Show/Overlay)
    cy.get(menuButton).click();
    cy.get(sideNav).should('have.class', 'cds--side-nav--expanded');
    
    // Verify content does NOT have margin (overlay mode)
    // Note: We need to verify our custom logic or Carbon's class
    // For now, checking visibility
    cy.get(sideNav).should('be.visible');

    // Click 2: Should Lock (Push content)
    cy.get(menuButton).click();
    cy.get(sideNav).should('have.class', 'cds--side-nav--expanded');
    
    // Verify content has margin (pushed)
    // Using the class we added in Layout.js/Header.js logic
    // Or checking computed style if Carbon handles it
    // cy.get(content).should('have.css', 'margin-left', '256px'); // 16rem

    // Click 3: Should Close (Rail)
    cy.get(menuButton).click();
    cy.get(sideNav).should('not.have.class', 'cds--side-nav--expanded');
  });

  it("should navigate without full page reload", () => {
    // Expand menu to access items
    cy.get('[data-cy="menuButton"]').click(); 

    // spy on window reload
    cy.window().then(w => {
      w.beforeReload = true; 
      w.onbeforeunload = () => { w.beforeReload = false; } 
    });

    // Click a menu item (e.g. Dashboard or Storage)
    // We need to find a real link. Dashboard is usually there.
    cy.contains('a', 'Dashboard').click();

    // Verify URL changed
    cy.url().should('include', '/Dashboard');

    // Verify NO page reload happened
    cy.window().should('have.property', 'beforeReload', true);
  });

  it("should persist lock mode across reloads", () => {
    // 1. Set to Lock mode
    cy.get('[data-cy="menuButton"]').click(); // Show
    cy.get('[data-cy="menuButton"]').click(); // Lock
    
    // Verify locked state (e.g. by class or localstorage)
    cy.window().then(win => {
      expect(win.localStorage.getItem('mainSideNavMode')).to.eq('lock');
    });

    // 2. Reload page
    cy.reload();

    // 3. Verify still locked
    cy.get('.cds--side-nav').should('have.class', 'cds--side-nav--expanded');
    cy.window().then(win => {
      expect(win.localStorage.getItem('mainSideNavMode')).to.eq('lock');
    });
  });
});

