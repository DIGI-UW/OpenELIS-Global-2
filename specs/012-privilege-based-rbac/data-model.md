# Data Model: Privilege-Based RBAC

## New Tables

### `system_privilege`

Stores atomic capability definitions. These are defined by implementors and
seeded via Liquibase — not editable at runtime by lab administrators.

```sql
CREATE TABLE clinlims.system_privilege (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,  -- e.g. 'result:enter'
    description     VARCHAR(255),
    category        VARCHAR(50),                   -- e.g. 'orders', 'results', 'admin'
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    lastupdated     TIMESTAMP NOT NULL DEFAULT NOW(),
    sys_user_id     VARCHAR(255)
);
```

**Privilege naming convention**: `{domain}:{action}`

| Category   | Privileges                                                             |
| ---------- | ---------------------------------------------------------------------- |
| `orders`   | `order:create`, `order:view`, `order:edit`, `order:cancel`             |
| `results`  | `result:view`, `result:enter`, `result:modify`, `result:validate`      |
| `patients` | `patient:view`, `patient:create`, `patient:edit`                       |
| `reports`  | `report:run`, `report:export`                                          |
| `nce`      | `nce:view`, `nce:create`, `nce:edit`, `nce:assign`                     |
| `analyzer` | `analyzer:import`, `analyzer:configure`                                |
| `admin`    | `admin:users`, `admin:system`, `admin:tests`, `admin:panels`           |
| `audit`    | `audit:view`                                                           |
| `shipment` | `shipment:create`, `shipment:view`, `shipment:edit`, `shipment:delete` |

---

### `system_role_privilege`

Maps roles to their **directly-owned** privileges. Does NOT store inherited
privileges — those are resolved at runtime by walking the parent chain.

```sql
CREATE TABLE clinlims.system_role_privilege (
    id              SERIAL PRIMARY KEY,
    system_role_id  INTEGER NOT NULL REFERENCES clinlims.system_role(id),
    privilege_id    INTEGER NOT NULL REFERENCES clinlims.system_privilege(id),
    lastupdated     TIMESTAMP NOT NULL DEFAULT NOW(),
    sys_user_id     VARCHAR(255),
    UNIQUE (system_role_id, privilege_id)
);
```

---

## Modified Tables

### `system_role` (existing, extended)

The `grouping_parent` column already exists and is repurposed:

| Current meaning                  | New meaning                                                                                         |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| Display grouping only (cosmetic) | **Functional inheritance pointer** — child role inherits all privileges of parent role transitively |

No schema change required. Liquibase migration updates the semantic use and
ensures non-grouping roles have the correct parent set.

**Constraint**: Only non-grouping roles should be assigned as parents of other
non-grouping roles. Grouping container roles (`Global Roles`, `Lab Unit Roles`)
are never parents of functional roles.

---

## Unchanged Tables

| Table                 | Role in new system                                          |
| --------------------- | ----------------------------------------------------------- |
| `system_user_role`    | Global role assignments — unchanged                         |
| `user_lab_unit_roles` | Department-scoped role assignments — unchanged              |
| `lab_unit_role_map`   | Lab unit → role list — unchanged                            |
| `lab_roles`           | Role entries per lab unit map — unchanged                   |
| `system_role_module`  | **Retained during migration period**, deprecated in Phase 5 |
| `system_module_url`   | **Retained during migration period**, deprecated in Phase 5 |

---

## Role Inheritance & Privilege Resolution

### Inheritance Tree (Seed Data)

```
Global Administrator
│   (no parent — implicitly holds ALL privileges)
│
├── User Account Administrator        privileges: admin:users
├── Audit Trail                        privileges: audit:view
└── Analyser Import                    privileges: analyzer:import, analyzer:configure

Lab Unit Roles (grouping container — not a parent for inheritance)
│
Reception                              privileges: order:create, order:view,
│                                                  order:edit, patient:view,
│                                                  patient:create, patient:edit,
│                                                  nce:view, nce:create,
│                                                  shipment:view, shipment:create
│
Results  ──────── parent: Reception    privileges: result:view, result:enter,
│                                                  result:modify
│
Validation ─────── parent: Results     privileges: result:validate, nce:edit,
│                                                  nce:assign
│
Pathologist ────── parent: Validation  privileges: result:pathology-sign-off
│
Cytopathologist ── parent: Validation  privileges: result:cytopathology-sign-off

Reports                                privileges: report:run, report:export,
                                                   order:view, result:view

EQA Coordinator                        privileges: eqa:manage, eqa:view,
                                                   report:run, result:view
```

### Resolution Algorithm

At login, `CustomUserDetailsService` resolves privileges by walking the parent
chain:

```
resolvePrivileges(role):
  if role is Global Administrator → return ALL_PRIVILEGES

  privileges = directPrivilegesOf(role)        -- query system_role_privilege
  if role.groupingParent is not null and parent is not a grouping container:
      privileges += resolvePrivileges(parent)  -- recursive
  return deduplicated set
```

The resolved set is loaded as Spring `GrantedAuthority` objects with prefix
`PRIV_` (e.g., `PRIV_RESULT_ENTER`) to distinguish them from role authorities.

### Example: Validation user in Hematology

```
Assigned role:  Validation (in Hematology department)

Direct privileges:     result:validate, nce:edit, nce:assign
+ from Results:        result:view, result:enter, result:modify
+ from Reception:      order:create, order:view, order:edit,
                       patient:view, patient:create, patient:edit,
                       nce:view, nce:create, shipment:view, shipment:create

Effective privileges (all, scoped to Hematology):
  order:create, order:view, order:edit,
  patient:view, patient:create, patient:edit,
  result:view, result:enter, result:modify, result:validate,
  nce:view, nce:create, nce:edit, nce:assign,
  shipment:view, shipment:create
```

---

## Session Payload Change

`/rest/session` response currently:

```json
{
  "authenticated": true,
  "roles": ["Validation"],
  "userLabRolesMap": { "Hematology": ["Validation"] },
  "loginLabUnit": "Hematology",
  "CSRF": "..."
}
```

After this migration:

```json
{
  "authenticated": true,
  "roles": ["Validation"],
  "userLabRolesMap": { "Hematology": ["Validation"] },
  "loginLabUnit": "Hematology",
  "privileges": [
    "order:create",
    "order:view",
    "order:edit",
    "patient:view",
    "patient:create",
    "patient:edit",
    "result:view",
    "result:enter",
    "result:modify",
    "result:validate",
    "nce:view",
    "nce:create",
    "nce:edit",
    "nce:assign",
    "shipment:view",
    "shipment:create"
  ],
  "CSRF": "..."
}
```

The `roles` array is retained for backwards compatibility. `privileges` is the
new authoritative field for frontend access checks.

---

## Migration Liquibase Sequence

| Changeset                                 | Content                                                                              |
| ----------------------------------------- | ------------------------------------------------------------------------------------ |
| `012-001-create-system-privilege`         | Create `system_privilege` table                                                      |
| `012-002-create-system-role-privilege`    | Create `system_role_privilege` table                                                 |
| `012-003-seed-privileges`                 | Insert all privilege definitions                                                     |
| `012-004-seed-role-privilege-mappings`    | Map each role to its direct privileges                                               |
| `012-005-set-role-parent-chains`          | Update `grouping_parent` on functional roles for inheritance                         |
| `012-006-migrate-legacy-role-assignments` | Migrate `Results entry` → `Results`, `Validator` → `Validation` user assignments     |
| `012-007-deactivate-legacy-roles`         | Set `active=false` on `Inventory mgr`, `Results Admin`, `Results entry`, `Validator` |
| `012-008-fix-auto-allow-fallback`         | (No schema change — tracked here for deployment ordering reference)                  |
