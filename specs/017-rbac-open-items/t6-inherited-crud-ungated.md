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

## DECIDED 2026-09-21 — shape (c), scoped to controller-reachable services

`BaseObjectService`'s 40 methods now carry one `@PreAuthorize` each, delegating to
`CrudGate` (static, so it works in every method-security context including slice
tests). `CrudGate` resolves, per call: the descendant interface's
`@CrudPrivileges(read=…, write=…)` → else its type-level `@PreAuthorize` (so the 92
already-covered services behave exactly as before; once `BaseObjectService` has a
method-level annotation, Spring stops consulting the descendant's type-level one
for inherited methods, and the fallback re-applies it) → else open.

**A finding that changed the fix.** The "redeclare inherited CRUD with a gate"
approach — what `AlertService` briefly had, and what `LocalizationService`,
`MenuService` and `UnitOfMeasureService` had before this PR — is **not enforced in
production**. Spring Security 6.2 resolves `@PreAuthorize` against the most specific
method, which for an inherited implementation is declared in
`BaseObjectServiceImpl`, whose type hierarchy does not include the descendant
interface. It only appears to work in slice tests, where the bean is a JDK-proxy
stub whose class does implement the interface. `BaseObjectServiceCrudGateTest`
pins this in the production shape (abstract generic impl + descendant interface +
real proxied bean). Those three dead redeclarations are removed and the interfaces
carry `@CrudPrivileges(write=…)` instead; rule 1 of the ratchet forbids the shape.

**Scope applied.** Of the 110 method-gated-only services, 41 have inherited CRUD
reachable from a controller, 21 with an inherited write. Because the gate applies
to *every* caller under the user's authentication, not just controllers, each write
privilege was checked against the internal callers too:

| declared `write=` | services |
|---|---|
| `PRIV_TEST_CONFIGURE` | TestService, TestSectionService, UnitOfMeasureService, ComplianceThresholdService |
| `PRIV_SAMPLE_TYPE_MANAGE` | ComplianceStandardService, ParameterGroupService, VectorSamplingSiteService |
| `PRIV_INVENTORY_MANAGE` | InventoryItemService, InventoryLotService |
| `PRIV_LOCALIZATION_MANAGE` | LocalizationService, SupportedLocaleService |
| `PRIV_DICTIONARY_MANAGE` | DictionaryService |
| `PRIV_EXTCONNECTION_MANAGE` | ExternalConnectionService |
| `PRIV_SYSTEM_CONFIGURE` | MenuService |
| `PRIV_ORDER_EDIT` | ElectronicOrderService |
| `PRIV_ALERT_MANAGE` (+ `read=PRIV_ALERT_VIEW`) | AlertService |

Their callers are admin configuration screens, configuration import (system
context) or the daemon, so the roles that reach them hold the privilege.

**Left open on purpose (in BASELINE, with the reason):**
`SampleItemService` and `AnalysisService` (inserted by `Accessioner` during order
entry and by result-entry persistence: Reception lacks `result:enter`, Results lacks
`order:edit`), `OrganizationService` (`SamplePatientEntryServiceImpl.insert` creates
organisations during order entry), `ReferralService` (`LogbookPersistServiceImpl`
during result entry; Results holds only `referral:view`), `NceSpecimenService`
(sample rejection by Reception; only Validation holds `nce:edit`), `HistoryService`
(`AuditTrailServiceImpl.insert` on every audited write), `SiteInformationService`
(`ShippingBoxRestController` updates a site setting during shipment creation).
Gating these needs either a workflow-scoped privilege or a seed change, not a
one-line declaration. Reads on all 94 baseline services also stay open.

## Definition of done

- [x] Pin the semantic the finding rests on (type-level covers inherited;
      method-level does not).
- [x] Fix the instance a test caught (`AlertService`), fully — all inherited
      writes and the reads the alerts bell uses.
- [x] Ratchet: `InheritedCrudGateCoverageTest` — no new offenders; baseline only
      shrinks.
- [x] Decide (a)/(b)/(c) — (c).
- [x] Controller-reachability scan — 41 reachable, 21 with writes.
- [x] `@CrudPrivileges` on 16 interfaces; 3 dead redeclarations removed.
- [ ] The 7 workflow-blocked services: choose scoped privileges or seed grants, then declare.
- [ ] Inherited reads: declare `read=` per service (all 94 still open).
- [ ] Work BASELINE to zero; then make rule 2 a hard rule and flip `CrudGate`'s undeclared default to deny.
