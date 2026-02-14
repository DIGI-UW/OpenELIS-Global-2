# Feature Specification: Carbon Design System Sidenav

**Feature Branch**: `009-carbon-sidenav`  
**Created**: December 4, 2025  
**Status**: Draft  
**Input**: Updated layout and sidenav following Carbon design best practices,
with a simple two-mode side nav that can be toggled to be expanded or collapsed.
When expanded, it serves as hierarchical tab navigation for subpages. Each page
can be easily configured to use either mode, with collapsed as the default.

## Background & Analysis

This feature was refactored from the `analyzer-layout-poc` branch, which
implemented a proof-of-concept for a Carbon-compliant layout specifically for
analyzer pages. The POC demonstrated key improvements over the existing
`Header.js` implementation:

### Current Implementation Issues (Header.js)

1. **Uses `HeaderContainer` render prop pattern** - This creates an inverted
   control flow where Carbon manages sidenav state
2. **SideNav uses `isPersistent={false}`** - This makes the sidenav overlay
   content rather than push it
3. **No state persistence** - Sidenav state is lost on navigation
4. **Content positioned as child of Header** - Breaks Carbon's expected layout
   structure

### POC Improvements (AnalyzerLayout.js)

1. **Direct sidenav state control** - Bypasses `HeaderContainer` for explicit
   state management
2. **SideNav uses `isFixedNav={true}` + `isChildOfHeader={true}`** - Proper
   Carbon UI Shell structure
3. **State persisted to localStorage** - User preference survives navigation
4. **Content as sibling to Header/SideNav** - Proper DOM structure for content
   pushing
5. **Auto-expansion of active menu branch** - Highlights current location in
   navigation tree

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

1. **Given** the sidenav is in collapsed mode, **When** I click the menu toggle
   button in the header, **Then** the sidenav smoothly expands to full width
   (256px/16rem) showing full menu labels, and the main content area shifts
   right to accommodate it.
2. **Given** the sidenav is in expanded mode, **When** I click the menu toggle
   button, **Then** the sidenav collapses to rail mode (48px/3rem), and the main
   content area expands to fill the available space.
3. **Given** the sidenav is expanded, **When** the CSS transition completes,
   **Then** there should be no visual jank or layout shift in the content area.

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

1. **Given** a page is configured with `defaultExpanded={true}`, **When** a user
   with no stored preference visits that page, **Then** the sidenav appears in
   expanded mode.
2. **Given** a page is configured with default collapsed mode (or no
   configuration), **When** a user with no stored preference visits, **Then**
   the sidenav appears in collapsed mode.
3. **Given** a user has a stored preference, **When** they visit a page with
   different default configuration, **Then** the user's stored preference takes
   precedence.

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

- **FR-001**: System MUST provide a toggle button in the header that switches
  the sidenav between expanded (256px) and collapsed (48px) modes.
- **FR-002**: System MUST persist the user's sidenav mode preference in
  localStorage and restore it on subsequent visits.
- **FR-003**: System MUST use CSS transitions for smooth expand/collapse
  animations (110ms cubic-bezier timing as per Carbon standards).
- **FR-004**: System MUST support hierarchical menu structures with
  expandable/collapsible parent items.
- **FR-005**: System MUST auto-expand menu items in the path to the currently
  active page on initial load.
- **FR-006**: System MUST allow pages to configure their default sidenav mode
  via props (defaultExpanded: boolean).
- **FR-007**: System MUST properly push content when sidenav expands (not
  overlay) when using `isFixedNav` mode.
- **FR-008**: System MUST render sidenav as a sibling to Content component (not
  nested within Header) for proper Carbon layout structure.
- **FR-009**: System MUST support navigation items with both action URLs and
  child menus (dual-action items).
- **FR-010**: System MUST highlight the currently active navigation item.

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
- **CR-005**: Tests MUST be included (unit tests for toggle logic, E2E tests for
  navigation flows, >70% coverage goal).

### Key Entities

- **Layout**: Wrapper component that provides sidenav mode and renders Header +
  SideNav + Content structure.
- **SideNav State**: Boolean indicating expanded (true) or collapsed (false)
  mode.
- **Menu Structure**: Hierarchical tree of menu items with `menu` (metadata) and
  `childMenus` (nested items).
- **User Preference**: localStorage key-value pair storing user's mode
  preference.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Users can toggle sidenav mode in under 150ms perceived response
  time (animation completes smoothly).
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

## Assumptions

- The existing menu API (`/rest/menu`) will continue to provide the hierarchical
  menu structure without modification.
- Carbon Design System v1.15+ is already installed and configured in the
  frontend.
- The refactored layout will initially be applied to analyzer routes
  (`/analyzers/*`) as proven in the POC, with gradual rollout to other sections.
- The existing `Header.js` using `HeaderContainer` pattern will remain for
  backward compatibility during transition.
- User's localStorage is available and writable in typical deployment scenarios
  (graceful fallback for edge cases).

## References

- **POC Implementation**: `analyzer-layout-poc` branch -
  `frontend/src/components/layout/AnalyzerLayout.js`
- **Carbon Design System**:
  [UI Shell Left Panel Usage](https://carbondesignsystem.com/components/UI-shell-left-panel/usage/)
- **Carbon Design System**:
  [UI Shell Left Panel Accessibility](https://carbondesignsystem.com/components/UI-shell-left-panel/accessibility/)
- **Existing Header**: `frontend/src/components/layout/Header.js` - Current
  implementation for comparison
