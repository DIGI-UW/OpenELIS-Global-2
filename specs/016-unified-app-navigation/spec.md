# Feature Specification: Unified Application Navigation

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

#### FR1: Global Sidebar Component

- Create a unified `GlobalSidebar` React component
- Replace all local SideNav implementations (Admin, Reports, Validation)
- Maintain current Carbon Design System styling
- Support collapsible/expandable menu sections

#### FR2: Configuration-Driven Menus

- Define menu structure in JSON files under `configuration/menus/`
- Support hierarchical menu structure with parent/child relationships
- Enable country-specific menu customizations
- Include icon mapping for menu items

#### FR3: Role-Based Access Control

- Extend backend menu model with `requiredRole` attribute
- Filter menu items based on user roles in `/rest/menu` API
- Support multiple roles per menu item
- Maintain security by enforcing server-side filtering

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
- Sync menu configurations via FHIR if required

#### IV. Layered Architecture Pattern

- Valueholder: `MenuValueholder` with new fields
- DAO: `MenuDAO` with HQL queries only
- Service: `MenuService` with transaction management
- Controller: `MenuRestController` with role filtering

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
public class MenuValueholder extends BaseObject<String> {
    private String id;
    private String parentId;
    private String displayKey;
    private String url;
    private String iconName;
    private String requiredRole;
    private UUID fhirUuid;
    private Integer sortOrder;
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
      "requiredRole": "GLOBAL_ADMIN",
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
