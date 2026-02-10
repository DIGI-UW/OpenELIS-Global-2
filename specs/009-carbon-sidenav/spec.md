# Feature Specification: Carbon Design System Layout & Sidenav Refactor

**Feature Branch**: `009-carbon-sidenav`  
**Created**: December 4, 2025  
**Last Updated**: January 27, 2026  
**Status**: Implemented  
**Input**: Refactor the existing Layout/Header components to follow Carbon
design best practices with improved sidenav functionality. This is a **refactor,
not a rewrite** — ALL existing header functionality (notifications, user menu,
language selector, search bar, logout, panels) MUST be preserved. The only
behavioral change is the sidenav toggle (tri-state: show/lock/close) and active
styling.

## Critical Scope Constraint

**NO LOSS OF FUNCTIONALITY**: This refactor MUST preserve 100% of existing
header/layout functionality:

- ✅ User menu (profile, logout)
- ✅ Notifications panel (SlideOverNotifications)
- ✅ Language selector (onChangeLanguage)
- ✅ Search bar (SearchBar component)
- ✅ Help links
- ✅ Version display
- ✅ All existing menu items and navigation
- ✅ Authentication/session handling
- ✅ Configuration context (ConfigurationContext)
- ✅ Notification context (NotificationContext)

**ONLY CHANGES**:

- Sidenav toggle behavior: tri-state (SHOW overlay, LOCK push, CLOSE rail)
- Sidenav active styling: left-border indicator for current page
- Content push/overlay based on mode
- User preference persistence to localStorage

## Background & Analysis

This feature improves the sidenav behavior while **preserving all existing
functionality**. The current sidenav only supports overlay mode and loses state
on navigation. Users need a persistent "locked" mode for complex workflows.

### Current Limitations

1. **Single mode only** - Sidenav can only overlay content, not push it
2. **No state persistence** - Sidenav state is lost on navigation/refresh
3. **No auto-expansion** - Users must manually expand menus to find current page
4. **No lock mode** - Users cannot keep sidenav persistently open while working

### What This Feature Adds

1. **Tri-state sidenav** - Three modes: SHOW (overlay), LOCK (push content),
   CLOSE (collapsed rail)
2. **Preference persistence** - User's preferred mode saved to localStorage
3. **Auto-expansion** - Menu automatically expands to show current page location
4. **Content push** - In LOCK mode, content shifts to accommodate sidenav

### What This Feature Preserves (Non-Negotiable)

All existing header functionality MUST continue to work identically:

- User menu, notifications, search, language selector, logout
- All panel interactions and state management
- Configuration and notification contexts
- Menu items and navigation behavior

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Toggle Sidenav Between Modes (Priority: P1)

As a laboratory user, I want to toggle the sidenav between expanded and
collapsed modes so that I can balance navigation visibility with screen real
estate for my main work.

**Why this priority**: Core functionality that all other features depend on.
Without toggle capability, the two-mode design cannot function.

**Independent Test**: User clicks the hamburger menu button and observes the
sidenav expand/collapse with smooth animation, and content area adjusts
accordingly.

**Acceptance Scenarios**:

1. **Given** the sidenav is in **CLOSE (rail)** mode, **When** I click the menu
   toggle button, **Then** it enters **SHOW (overlay)** mode: the sidenav opens
   over the content with no content push.
2. **Given** the sidenav is in **SHOW (overlay)** mode, **When** I click the
   menu toggle again, **Then** it enters **LOCK (push)** mode: the sidenav stays
   open and the content shifts right by 16rem.
3. **Given** the sidenav is in **LOCK (push)** mode, **When** I click the menu
   toggle again, **Then** it returns to **CLOSE (rail)**: the sidenav collapses
   to 48px/3rem and the content fills the space.
4. **Given** the sidenav is in **SHOW (overlay)** mode, **When** I click outside
   the sidenav, **Then** it closes to **CLOSE (rail)** mode.
5. **Given** the sidenav is expanded in any mode, **When** the CSS transition
   completes, **Then** there should be no visual jank or layout shift in the
   content area.

---

## Clarifications

### Session 2025-12-05

- Q: Should the sidenav behavior apply globally to all authenticated routes? →
  A: Yes. All authenticated pages use the enhanced sidenav. Storage and
  multi-tab pages default to LOCK mode; user preference overrides page defaults.
- Q: Should this refactor preserve ALL existing header functionality? → A: Yes.
  This is a refactor, NOT a rewrite. Zero loss of functionality. All header
  actions (notifications, user menu, language, search, logout) must remain
  intact.

### Session 2026-01-27 (Constitution Amendment)

- Q: Can we use Playwright for E2E tests instead of Cypress? → A: Yes.
  Constitution amended to v1.9.0 to allow Playwright as recommended framework
  for new tests (Section V.5). Testing Roadmap already provided guidance for
  framework selection. Playwright offers faster execution, better debugging, and
  modern async/await patterns.

---

### User Story 2 - Persist User Preference Across Sessions (Priority: P1)

As a laboratory user who prefers a specific sidenav mode, I want my preference
to be remembered so that I don't have to re-expand or collapse the sidenav every
time I navigate or refresh the page.

**Why this priority**: Essential for user experience - without persistence,
users face repeated friction adjusting the interface.

**Independent Test**: User expands sidenav, navigates to another page, and
verifies sidenav remains expanded.

**Acceptance Scenarios**:

1. **Given** I have toggled the sidenav to expanded mode, **When** I navigate to
   a different page within the application, **Then** the sidenav remains in
   expanded mode.
2. **Given** I have toggled the sidenav to collapsed mode, **When** I refresh
   the browser, **Then** the sidenav opens in collapsed mode.
3. **Given** I am a new user with no stored preference, **When** I first visit a
   page configured for collapsed mode, **Then** the sidenav appears collapsed by
   default.

---

### User Story 3 - Hierarchical Navigation in Expanded Mode (Priority: P2)

As a laboratory user navigating a complex menu structure, I want to see
expandable/collapsible submenus in the expanded sidenav so that I can quickly
find and access nested pages.

**Why this priority**: Adds significant value for complex applications but
depends on P1 functionality being in place.

**Independent Test**: User can expand a parent menu item to reveal child items,
then collapse it, without affecting other menu sections.

**Acceptance Scenarios**:

1. **Given** the sidenav is expanded and I am viewing a parent menu item with
   children, **When** I click on the parent item's expand chevron, **Then** the
   child menu items appear below with appropriate indentation.
2. **Given** a submenu is expanded, **When** I click the parent item's collapse
   chevron, **Then** the child items hide smoothly.
3. **Given** I navigate to a nested page (e.g., `/analyzers/qc/alerts`),
   **When** the page loads, **Then** the parent menu items in the path
   automatically expand to show my current location highlighted.

---

### User Story 4 - Page-Level Mode Configuration (Priority: P2)

As a developer configuring a page layout, I want to easily specify whether a
page should use expanded or collapsed sidenav by default so that different
sections of the application can have appropriate navigation experiences.

**Why this priority**: Enables tailored UX for different application sections;
analyzer pages may benefit from expanded nav while simpler pages work well
collapsed.

**Independent Test**: Developer can configure a route to use expanded mode by
default and verify it renders correctly.

**Acceptance Scenarios**:

1. **Given** a page is configured with `defaultMode="lock"`, **When** a user
   with no stored preference visits that page, **Then** the sidenav appears in
   LOCK (push) mode with content shifted right.
2. **Given** a page is configured with `defaultMode="close"` (or no
   configuration), **When** a user with no stored preference visits, **Then**
   the sidenav appears in CLOSE (rail) mode.
3. **Given** a user has a stored preference, **When** they visit a page with
   different default configuration, **Then** the user's stored preference takes
   precedence over the page default.

---

### User Story 5 - Collapsed Mode Rail with Icons (Priority: P3)

As a laboratory user using the collapsed sidenav, I want to see icon-based
navigation with tooltips so that I can still identify navigation items without
expanding the full menu.

**Why this priority**: Enhancement for collapsed mode usability; basic
functionality works without this but this improves the experience.

**Independent Test**: User hovers over a collapsed sidenav item and sees a
tooltip with the full label.

**Acceptance Scenarios**:

1. **Given** the sidenav is in collapsed mode, **When** I hover over a
   navigation item, **Then** a tooltip appears showing the full menu label.
2. **Given** the sidenav is in collapsed mode, **When** I view the navigation
   rail, **Then** I see recognizable icons representing each top-level menu
   section.

---

### User Story 6 - Responsive Behavior on Mobile (Priority: P3)

As a laboratory user on a tablet or mobile device, I want the sidenav to behave
appropriately for smaller screens so that I can still navigate the application
effectively.

**Why this priority**: Mobile responsiveness is important but the primary user
base uses desktop/laptop computers in laboratory settings.

**Independent Test**: On a viewport width below 1056px, the sidenav overlays
content rather than pushing it.

**Acceptance Scenarios**:

1. **Given** I am viewing the application on a screen narrower than 1056px,
   **When** I expand the sidenav, **Then** it overlays the content rather than
   pushing it.
2. **Given** the sidenav is overlaying content on mobile, **When** I click
   outside the sidenav, **Then** it collapses automatically.

---

### Edge Cases

- What happens when localStorage is unavailable (private browsing mode)? →
  Gracefully fall back to page default, log warning.
- How does the system handle rapid toggle clicks? → CSS transition should handle
  gracefully; debounce if needed.
- What happens when menu data fails to load? → Display empty sidenav with
  appropriate loading/error state.
- What if a user navigates to a page with very deep menu nesting (>4 levels)? →
  Support up to 4 levels of nesting with progressively smaller font sizes and
  indentation.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST provide a toggle button in the header that cycles the
  sidenav through three modes: SHOW (overlay), LOCK (push), CLOSE (rail).
- **FR-002**: System MUST persist the user's sidenav mode preference in
  localStorage and restore it on subsequent visits.
- **FR-003**: System MUST use CSS transitions for smooth expand/collapse
  animations (110ms cubic-bezier timing as per Carbon standards).
- **FR-004**: System MUST support hierarchical menu structures with
  expandable/collapsible parent items.
- **FR-005**: System MUST auto-expand menu items in the path to the currently
  active page on initial load.
- **FR-006**: System MUST allow pages to configure their default sidenav mode
  via props (defaultMode: 'show' | 'lock' | 'close').
- **FR-007**: System MUST properly push content when sidenav is in LOCK mode
  (content shifts right, not overlaid).
- **FR-008**: System MUST use proper Carbon layout structure for sidenav and
  content positioning.
- **FR-009**: System MUST support navigation items with both action URLs and
  child menus (dual-action items).
- **FR-010**: System MUST highlight the currently active navigation item with a
  left-border indicator consistent with existing visual language.
- **FR-011**: System MUST preserve ALL existing header functionality during
  refactor: user menu, notifications panel, language selector, search bar, help
  links, logout, version display.
- **FR-012**: System MUST preserve ConfigurationContext and NotificationContext
  providers from the existing Layout.js.
- **FR-013**: System MUST apply the refactored layout globally to all
  authenticated routes (not incremental rollout).
- **FR-014**: System MUST support nested subnav structure for Storage section
  with 3-level hierarchy:
  - Level 1: Storage (parent)
  - Level 2: Storage Management, Cold Storage Monitoring (expandable parents)
  - Level 3: Dashboard tabs as leaf items (Sample Items, Rooms, Devices,
    Shelves, Racks, Boxes for Storage Management; Dashboard, Corrective Actions,
    Historical Trends, Reports, Settings for Cold Storage Monitoring)

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) -
  specifically `Header`, `SideNav`, `SideNavItems`, `SideNavMenu`,
  `SideNavMenuItem`, `Content`, `Theme` components.
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text) - menu labels use
  `intl.formatMessage({ id: menuItem.menu.displayKey })`.
- **CR-003**: Configuration-driven variation for sidenav default mode (NO code
  branching per page type).
- **CR-004**: Security: Sidenav MUST only display menu items the user has
  permission to access (existing menu API provides this filtering).
- **CR-005**: Tests MUST be included (unit tests for toggle logic, E2E tests via
  Playwright or Cypress for navigation flows, >70% coverage goal). This feature
  uses Playwright per Testing Roadmap guidance.

### Key Entities

- **SideNav Mode**: Tri-state preference representing user's navigation display
  choice:
  - `show`: Sidenav expanded, overlays content (temporary view)
  - `lock`: Sidenav expanded, pushes content aside (persistent workspace)
  - `close`: Sidenav collapsed to icon rail (maximized content area)
- **Menu Structure**: Hierarchical tree of navigation items supporting up to 4
  levels of nesting, with automatic expansion to show current location.
- **User Preference**: Persistent storage of user's preferred sidenav mode,
  retained across page navigations and browser sessions.
- **Content Area**: Main workspace that adapts based on sidenav mode (full width
  when closed/show, reduced width when locked).

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Users can toggle sidenav mode with smooth animation that completes
  without user-perceived delay.
- **SC-002**: User's sidenav preference persists with 100% reliability across
  page navigations and browser refreshes (when localStorage is available).
- **SC-003**: Navigation hierarchy displays correctly up to 4 levels deep with
  clear visual distinction between levels.
- **SC-004**: All existing navigation functionality continues to work (no
  regression in menu item clicks, external links, active highlighting).
- **SC-005**: Mobile users (viewport < 1056px) experience appropriate overlay
  behavior without content accessibility issues.
- **SC-006**: The layout passes Carbon Design System accessibility audit (WCAG
  2.1 AA compliance for navigation components).
- **SC-007**: Developers can configure page-level default mode with a single
  prop change.
- **SC-008**: ALL existing header functionality works identically after
  refactor: user menu opens/closes, notifications panel works, language selector
  changes language, search bar functions, logout works, help links navigate
  correctly.
- **SC-009**: Zero regression in login/logout flow after refactor.
- **SC-010**: ConfigurationContext and NotificationContext continue to provide
  data to all child components.

## Assumptions

- The existing menu API (`/rest/menu`) will continue to provide the hierarchical
  menu structure without modification.
- Carbon Design System v1.15+ is already installed and configured in the
  frontend.
- All existing header functionality will remain intact after this enhancement.
- User's localStorage is available and writable in typical deployment scenarios
  (graceful fallback for edge cases).
- Storage pages and multi-tab pages will default to LOCK (expanded) mode while
  other pages default to CLOSE (collapsed) mode.

## References

- **POC Implementation**: `analyzer-layout-poc` branch -
  `frontend/src/components/layout/AnalyzerLayout.js`
- **Carbon Design System**:
  [UI Shell Left Panel Usage](https://carbondesignsystem.com/components/UI-shell-left-panel/usage/)
- **Carbon Design System**:
  [UI Shell Left Panel Accessibility](https://carbondesignsystem.com/components/UI-shell-left-panel/accessibility/)
- **Existing Header**: `frontend/src/components/layout/Header.js` - Current
  implementation for comparison
- **Constitution Amendment**: v1.9.0 (2026-01-27) - Section V.5 amended to allow
  Playwright for E2E testing alongside Cypress. See
  `.specify/memory/constitution.md` for details.
