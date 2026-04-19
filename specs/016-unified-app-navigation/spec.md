# Feature Specification: Unified Application Navigation

**Feature Branch**: `spec/016-unified-app-navigation`  
**Created**: 2026-04-06  
**Status**: Ready for Planning  
**Input**: Feature request from navigation unification epic

## Overview

This specification outlines the implementation of a unified,
configuration-driven navigation system for OpenELIS Global 2, replacing the
current fragmented sidebar implementations across Admin, Reports, and Validation
sections with a single global sidebar component.

## User Scenarios & Testing

### Primary User Stories

1. **As a Global Administrator**, I want to see all navigation options in a
   single sidebar, so I can efficiently navigate between different functional
   areas without confusion.

2. **As a Regular User**, I want to see only the navigation options relevant to
   my role, so the interface remains clean and focused on my tasks.

3. **As a Country Configurator**, I want to customize navigation menus via
   configuration files, so I can adapt the system to country-specific workflows
   without code changes.

4. **As a Developer**, I want a single navigation component to maintain, so I
   can implement navigation features consistently across the application.

### Acceptance Tests

- **Test 1**: Global Admin sees all menu items (Admin, Reports, Validation)
- **Test 2**: Regular User sees only permitted sections
- **Test 3**: Navigation menus load from JSON configuration
- **Test 4**: Role-based filtering works correctly
- **Test 5**: Icons display properly for all menu items
- **Test 6**: Navigation state persists across page refreshes

## Requirements

### Functional Requirements

#### FR0: Frontend Stack Modernization (Prerequisite)

- Migrate application from React 17 to React 18
- Upgrade `@carbon/react` and `@carbon/icons-react` to latest v11 releases
- Remove all legacy `carbon-components` (v10) dependencies
- Ensure all existing components are compatible with the new stack before
  implementing the unified navigation

#### FR1: Global Sidebar Component

- Create a unified `GlobalSidebar` React component
- Replace all local SideNav implementations (Admin, Reports, Validation)
- Maintain current Carbon Design System styling
- Support collapsible/expandable menu sections
- Persist expanded/collapsed state using `localStorage` to survive page reloads

#### FR2: Configuration-Driven Menus

- Define menu structure in JSON files under `configuration/menus/`
- Support hierarchical menu structure with parent/child relationships
- Enable country-specific menu customizations
- Include icon mapping for menu items
- **Note**: This is separate from the existing
  `/var/lib/openelis-global/menu/menu_config.json` which handles menu filtering.
  The new configuration will define the complete menu structure and will be
  loaded via a new `MenuConfigurationHandler` following the existing
  `DomainConfigurationHandler` pattern.
- **Config Resolution Strategy**: Follow existing
  `ConfigurationInitializationService` pattern where filesystem configs (e.g.,
  Madagascar distribution) completely override classpath defaults. No merging
  occurs.
- **Icon Loading Strategy:** Implement an explicit `MenuIconRegistry.js` that
  maps icon strings to their respective imported `@carbon/icons-react`
  components. This guarantees zero bundle bloat and type safety while avoiding
  React 18 async chunking complexity for basic navigation icons.

## Clarifications

### Session 2026-04-06

- Q: How should the new Menu configuration handle the relationship between
  bundled base menus and distribution-specific overrides? → A: Standard
  Overwrite: If a distribution provides menus.json, it must define the entire
  menu structure, completely ignoring the base classpath config. Matches
  existing DomainConfigurationHandler behavior.
- Q: How should the string iconName from the JSON configuration be mapped to the
  actual Carbon React component at runtime? → A: Explicit Icon Registry: Create
  a central `MenuIconRegistry.js` that explicitly imports only the icons needed
  by OpenELIS and its distributions.
- Q: How should the existing POST endpoints for menu mutation be handled in the
  configuration-driven paradigm? → A: Layered DB with Provenance: Each menu row
  tracks its origin via a `config_source` column (`distribution` vs `admin`).
  Config reloads only overwrite `distribution`-sourced rows; `admin` overrides
  persist across reloads. Admins can reset individual items to distribution
  defaults by deleting their override.
- Q: How should the migration from hardcoded JSX SideNavs to config-driven menus
  be sequenced? → A: Big-bang cutover: Build the new GlobalSidebar + config
  loading, then replace all three SideNavs (Admin, Reports, Validation) at once.

#### FR3: Role-Based Access Control

- Extend backend menu model with `requiredRoles` attribute as an array of Spring
  Security role keys
- Support multiple roles per menu item - show item if user has ANY of the listed
  roles (OR semantics)
- Filter menu items based on user roles in `/rest/menu` API using Spring
  Security role keys (e.g., `GLOBAL_ADMIN`)
- Maintain security by enforcing server-side filtering
- Treat omitted or empty `requiredRoles` as visible to all authenticated users

#### FR4: Backend Enhancements

- Add `iconName` field to menu entity
- Implement `DomainConfigurationHandler` for JSON menu loading
- Update `MenuController` to perform role filtering
- Add FHIR UUID support for menu entities

### Constitution Compliance

#### I. Configuration-Driven Variation

- All menu customizations implemented via JSON configuration
- No country-specific code branches for navigation
- Database-driven configuration via `SystemConfiguration`

#### II. Carbon Design System First

- Exclusively use Carbon SideNav components
- Styling via Carbon tokens and themes
- Icons from `@carbon/icons-react`

#### III. FHIR/IHE Standards Compliance

- Menu entities include `fhir_uuid` for external integration
- Note: This feature only adds the database schema foundation (`fhir_uuid`
  column). Actual FHIR resource mapping and synchronization logic will be
  handled in a separate interoperability epic.

#### IV. Layered Architecture Pattern

- Valueholder: `Menu` entity with new fields (note: the entity is named `Menu`,
  not `MenuValueholder`)
- DAO: `MenuDAO` with HQL queries only
- Service: `MenuService` with transaction management
- Controller: `MenuController` with role filtering

#### V. Test-Driven Development

- Unit tests for all new components
- Integration tests for menu loading
- E2E tests for navigation flows

## Key Entities

### Frontend Components

```
GlobalSidebar
├── MenuSection (collapsible)
├── MenuItem (with icon)
└── RoleFilter (HOC)
```

### Backend Model

```java
@Entity
public class Menu extends BaseObject<String> {
    private String id;
    private String parentId;
    private String displayKey;
    private String actionURL;  // Matches existing entity field name
    private String iconName;
    private List<String> requiredRoles;  // Array of role keys
    private UUID fhirUuid;
    private Integer presentationOrder;  // Matches existing entity field name
}
```

### Configuration Structure

```json
{
  "menus": [
    {
      "id": "admin",
      "displayKey": "sidenav.label.admin",
      "iconName": "Settings",
      "requiredRoles": ["GLOBAL_ADMIN"],
      "children": [...]
    }
  ]
}
```

## Success Criteria

1. **Single Source of Truth**: All navigation uses the global sidebar component
2. **Configuration Driven**: Menus defined in JSON, not hardcoded
3. **Role Security**: Proper filtering based on user permissions
4. **Performance**: Menu loading completes within 200ms
5. **Accessibility**: Full WCAG 2.1 AA compliance
6. **Maintainability**: Navigation changes require no code deployment

## Implementation Phases

### Phase 1: Backend Foundation

1. Extend MenuValueholder with new fields
2. Create Liquibase migration for schema changes
3. Implement JSON configuration loading
4. Update menu API with role filtering

### Phase 2: Frontend Component

1. Create GlobalSidebar component
2. Implement configuration fetching
3. Add role-based filtering
4. Integrate with existing routing

### Phase 3: Migration & Cleanup

1. Replace Admin.js SideNav
2. Replace Reports SideNav
3. Replace Validation SideNav
4. Remove deprecated components

## Assumptions & Constraints

### Assumptions

- Existing `/rest/menu` endpoint can be extended
- User role information available in frontend context
- Country configurations follow existing patterns

### Constraints

- Must maintain backward compatibility during migration
- Cannot break existing bookmarks or deep links
- Must support all existing Carbon Design System features

## Dependencies

- Carbon Design System v1.15+
- React Intl for internationalization
- Existing user authentication system
- DomainConfigurationHandler framework
