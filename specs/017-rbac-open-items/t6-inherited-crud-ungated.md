# T6 — Inherited CRUD on method-gated services is ungated

**Severity**: HIGH — authenticated users can mutate entities through inherited `insert/update/delete`
**Effort**: medium; mechanical per service, but 109 services and one design choice
**Found**: 2026-09-21, by a fixture change that made a slice test's Reception user real

## What was found

`DELETE /rest/alerts/{id}` returned **204** for a Reception user. `AlertService` is
gated per method (`alert:view` on reads, `alert:manage` on writes it declares), but
it `extends BaseObjectService<Alert, Long>`, and the `delete(id, sysUserId)` the
controller calls is **inherited** — it carries no gate. The controller-level
`hasRole('ADMIN')` that used to cover it was removed by S011c on the premise that
the service gate would take over. It did not, because only a **type-level**
`@PreAuthorize` on an interface covers the methods that interface inherits.
`ClassLevelPreAuthorizeSemanticsTest#typeLevelInterfaceAnnotation_alsoCoversMethodsInheritedFromAnUngatedSuperInterface`
now pins that.

Measured across `src/main/java` (regex that handles a parenthesis inside the
annotation value — an earlier count of "202 of 202" was wrong for exactly that
reason):

| gated interfaces extending `BaseObjectService` | count |
|---|---|
| type-level gate (inherited CRUD covered) | 91 |
| type-level + method-level (covered) | 1 |
| **method-level only (inherited CRUD ungated)** | **110** |

`AlertService` is fixed by redeclaring the inherited CRUD with gates (reads
`alert:view`, all eight write methods `alert:manage`), leaving **109**. Full
list: `InheritedCrudGateCoverageTest.BASELINE`.

This is the third structural blind spot in the coverage scans, after
interfaces-only (`@Service` classes, T1) and Mockito annotation copying. The
common thread: each scan checked that an annotation is *present*, never what it
*covers*. T1's "DONE" stands for what it measured; it did not measure this.

## Exposure

- **Writes** (`insert`, `insertAll`, `save`, `saveAll`, `update`, `updateAll`,
  `delete`×2, `deleteAll`×2): open to any authenticated user wherever a controller
  or another service calls them on one of the 109 services. Includes
  `PatientService`, `SampleService`, `SampleItemService`, `AnalysisService`,
  `ResultLimitService`, `SystemUserService`, `RoleService`, `UserRoleService`,
  `OrganizationService`, `ProviderService`, `TestService`, `TestSectionService`.
- **Reads** (`get`, `getAll`, `getPage`, `getMatch*`, `getCount*`, …): same
  services, open to any authenticated user. Lower severity but in scope.

Whether a given inherited method is actually reachable from a controller varies;
the controller scan for that has not been done. Treat the list as the upper bound
and the Alert case as proof the bound is reached.

## Fix shapes

**(a) Redeclare per service** — what `AlertService` does. Explicit, no framework
subtlety, ~30 lines per service; reads get the domain's `*:view`, writes its
`*:manage`. 109 × ~30 lines, mechanical. Ratchet already in place.

**(b) Type-level floor + method overrides** — add a type-level
`@PreAuthorize("hasAuthority('PRIV_X_VIEW')")` to each of the 109, keep the
method-level gates (a method-level annotation takes precedence over the type-level
one; there is no duplicate error between the two levels). Inherited reads land on
`*:view`; inherited writes must still be redeclared with `*:manage` or they land on
`*:view` too — weaker than (a) if a write is missed, but never *open*. One line
plus the write redeclarations per service.

**(c) Gate `BaseObjectService` itself** — annotate its CRUD once with SpEL that asks
the target for its privileges, e.g.
`@PreAuthorize("hasAuthority(#root.this.viewPrivilege())")` on reads and
`managePrivilege()` on writes, with `BaseObjectServiceImpl` defaulting both to a
privilege nobody holds (fail closed) and each impl overriding with its pair. Every
descendant — including the 91 already covered and every future one — is gated by
construction, and `ServicePrivilegeCoverageTest` stops needing to reason about
inheritance. Two one-line overrides per impl (~200 impls), one framework-level
change to test carefully (`#root.this` is the target object in method security
expressions).

**Recommendation**: (c) for the structural fix, (a) as the interim for any
service a live test proves reachable. (b) is the cheapest but leaves the
"missed write lands on *:view*" edge. Whichever is chosen, the ratchet's rule for
"covered" (`InheritedCrudGateCoverageTest`) should be updated to match.

## Definition of done

- [x] Pin the semantic the finding rests on (type-level covers inherited;
      method-level does not).
- [x] Fix the instance a test caught (`AlertService`), fully — all inherited
      writes and the reads the alerts bell uses.
- [x] Ratchet: `InheritedCrudGateCoverageTest` — no new offenders; baseline only
      shrinks.
- [ ] Decide (a)/(b)/(c).
- [ ] Controller-reachability scan of the 109 to order the work by exposure.
- [ ] Work the baseline to zero; delete the ratchet or turn it into a hard rule.
