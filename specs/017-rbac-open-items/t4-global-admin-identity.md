# T4 — Stop identifying Global Admin by a mutable role name

**Severity**: MEDIUM
**Effort**: small (one changeset + one comparison)

## Problem

`PrivilegeServiceImpl` grants the all-privileges sentinel by comparing the role's
**display name** to the literal `"Global Administrator"`:

```java
if (role.getName() != null && Constants.ROLE_GLOBAL_ADMIN.equalsIgnoreCase(role.getName().trim())) {
    return Set.of(Privileges.GLOBAL_ADMIN_SENTINEL);
}
```

`system_role.name` is ordinary editable data (`character(30)`, surfaced in the
role admin UI), not a stable security identifier. So the highest-privilege
bypass in the system is reachable by *naming*:

- **Privilege loss** — rename or re-localise the role and the match fails.
  Resolution then returns only directly-mapped rows, which for a grouping role is
  typically empty, so every global admin silently drops to near-zero privileges.
  Sites that seeded it as `"Admin"` are already in this position; the codebase
  anticipates that spelling in `UnifiedSystemUserController.initialize()`.
- **Privilege escalation** — anyone able to create or rename a role to exactly
  `"Global Administrator"` grants its holders the entire catalog, with no row
  ever added to `system_role_privilege`.

## Definition of done

- Global Admin is identified by something immutable: the seeded role id/UUID, or
  (better) a non-editable `is_global_admin` boolean column set by migration.
- The role admin UI cannot rename or delete that row.
- Tests cover: renamed role, a case variant, a second role created with the same
  name, and a fresh-install vs upgraded database.

## Note on the current state

A related inconsistency was fixed earlier on this branch: the comparison was
case-**sensitive** here while `CustomUserDetailsService` used `equalsIgnoreCase`
for the same concept, so `"GLOBAL ADMINISTRATOR"` got `ROLE_ADMIN` but *not* the
privilege short-circuit. Both now use `equalsIgnoreCase`. That removes the
disagreement but does not address the underlying "name as identity" problem.
