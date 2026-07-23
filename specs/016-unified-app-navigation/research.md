# Research: Unified Application Navigation

**Feature**: 016-unified-app-navigation **Date**: 2026-04-06

## Configuration Resolution Strategy

**Decision**: Standard Overwrite — distribution configs completely replace base
classpath configs (no merging).

**Rationale**: Matches the existing `ConfigurationInitializationService`
pattern. The service checks filesystem first
(`/var/lib/openelis-global/configuration/backend/{domain}`), then classpath
(`classpath*:configuration/{domain}`). The two sources are never merged — if
filesystem files exist, only those are used. This is how all 12 existing
`DomainConfigurationHandler` implementations work (SiteBranding, Roles,
Dictionary, etc.).

**Alternatives Considered**:

- **Deep merge**: Distribution config merged with base config. Rejected because
  no existing handler uses merging, and it adds complexity around conflict
  resolution for nested menu structures.
- **Additive overlay**: Distribution adds/removes items from base. Rejected
  because it requires a diff format and makes the final menu structure
  non-obvious.

## Icon Mapping Strategy

**Decision**: Explicit Icon Registry — a central `MenuIconRegistry.js` that maps
icon name strings to imported `@carbon/icons-react` components.

**Rationale**: Guarantees zero bundle bloat (only imported icons are bundled),
provides type safety, and avoids React 18 async chunking complexity. The
registry pattern is simple to maintain and extend.

**Alternatives Considered**:

- **Dynamic async loading**: Use `React.lazy()` to load icons on demand.
  Rejected because navigation icons are small and always needed; the async
  overhead adds latency without meaningful bundle savings for ~20 icons.
- **String-to-component mapping via barrel export**: Import all icons from
  `@carbon/icons-react` and index by name. Rejected because it imports the
  entire icon library into the bundle (~2MB+).

## Configuration Lifecycle & Persistence

**Decision**: Layered DB with Provenance — each menu row tracks its origin via a
`config_source` column (`distribution` vs `admin`). Config reloads only
overwrite `distribution`-sourced rows. Admin overrides persist across reloads.

**Rationale**: Based on research across multiple configuration management
patterns:

| Pattern                        | Source                                            | Fit for OpenELIS                                                                   |
| ------------------------------ | ------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Layered Precedence Chain       | Spring Boot, Kubernetes, Azure App Config         | No runtime mutation; requires restart                                              |
| Seed-then-Own                  | OpenMRS Initializer, Django fixtures, Rails seeds | Drift risk; can't distinguish distribution vs admin changes                        |
| GitOps Write-Through           | Azure App Config, Backstage                       | Poor fit for on-premise labs with limited connectivity                             |
| **Layered DB with Provenance** | WordPress, Drupal, Salesforce                     | **Best fit** — preserves admin edits, supports distribution reloads, tracks origin |

Key advantages:

- Clean separation of distribution defaults vs instance customizations
- Admin can "reset to default" by deleting their override
- Config reloads are safe — only touch distribution-sourced rows
- Matches existing OpenELIS patterns (ConfigurationInitializationService already
  has checksum tracking)

**Alternatives Considered**:

- **Seed-then-Own (OpenMRS pattern)**: Config loads into DB, DB is runtime
  truth. Simpler but loses the ability to distinguish distribution defaults from
  admin edits. A config reload would overwrite everything including intentional
  admin customizations.
- **Config-only, no DB**: Config files are sole source of truth. No admin UI.
  Rejected because existing admin menu management UI is actively used.

## Frontend Stack Modernization

**Decision**: Upgrade React 17 → 18, @carbon/react to latest v11, remove legacy
carbon-components v10.

**Rationale**: Current versions are significantly outdated. React 17.0.2 is 3+
years old. @carbon/react 1.15.0 is far behind latest (1.104.1). The legacy
`carbon-components` 10.58.12 is a v10 package that should have been removed when
@carbon/react (v11) was adopted. Building new features on outdated dependencies
creates technical debt.

**Alternatives Considered**:

- **Keep current versions**: Build unified navigation on React 17 + Carbon
  1.15.0. Rejected by project owner — "no, I'm not building on top of outdated
  tech stack."
- **Partial upgrade (React 18 only)**: Upgrade React but keep Carbon versions.
  Rejected because Carbon v11 latest includes fixes and improvements needed for
  the navigation components.

## Migration Strategy

**Decision**: Big-bang cutover — build GlobalSidebar + config loading, then
replace all three SideNavs (Admin, Reports, Validation) at once.

**Rationale**: Avoids maintaining two navigation systems in parallel. The
unified sidebar is the whole point of the feature — an incremental approach
would temporarily increase complexity.

**Alternatives Considered**:

- **Incremental per-section**: Migrate one section at a time (Admin first, then
  Reports, then Validation). Lower risk per release but requires maintaining
  both old and new navigation systems simultaneously, which contradicts the
  "single source of truth" goal.
