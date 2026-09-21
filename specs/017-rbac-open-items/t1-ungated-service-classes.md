# T1 — Gate the @Service classes the coverage scan cannot see

**Severity**: HIGH — unauthenticated-adjacent exposure on live endpoints
**Effort**: large (41 services, each needs a privilege judgement)

## Problem

`ServicePrivilegeCoverageTest` scans **interfaces** in a `*.service.*` package.
A `@Service` **class with no interface** is never visited, so it passes the gate
by being invisible rather than by being gated.

41 such classes are reachable from a controller. Because PR #3443 removed the
controllers' `hasRole(...)` guards and the interceptor fails open on unmapped
`/rest` paths, several of these now have **no authorization at all**.

This is not theoretical. `MicroAstAnalyzerEventService` and
`MicroCultureAnalyzerEventService` were exactly this shape: their controllers'
`hasRole('ANALYSER_IMPORT')` was the only guard, and removing it left analyzer
result ingestion open to any authenticated user. Both were fixed in `f0dbe54fe`
— the 41 below were deliberately left alone rather than gated in bulk during a
CI-debug cycle.

## STATUS: DONE (2026-09-21)

All 41 are resolved — 16 gated with an explicit privilege, 25 marked
`@CrossDomainService` with a justification (form-binding helpers, accessioner
steps, status/reflex predicates, shared reference-list cache). The
controller-reachable ungated count is now **0**.

`ServiceClassPrivilegeCoverageTest` was added so the category cannot regress: it
asserts every controller-reachable `@Service` class carries `@PreAuthorize` or
`@CrossDomainService`. Inversion-verified — removing one gate fails it by name.

The list below is kept as the record of what was triaged.

## DECIDED — blocks PR #3443

Chosen 2026-09-21. All 41 must be gated (or exempted with justification) before
#3443 merges. Nothing ships with a known-open endpoint.

Note the cost that was accepted: this adds 41 privilege judgements to a PR
already at ~699 files. Two mistakes were made in the 339 gates done so far — one
write gated on a view privilege, one service left entirely ungated — so each of
these needs the same care, and the coverage-scan extension below is what stops
the category regressing.

## Approach

For each service, decide from its callers whether it is:

1. **user-reachable** → add `@PreAuthorize("hasAuthority('PRIV_*')")` per method,
   reusing an existing privilege from `Privileges.java` wherever one fits;
2. **internal/pipeline infrastructure** → extract an interface and mark it
   `@CrossDomainService` with an accurate `callers =` justification;
3. **not actually reachable** → confirm and record why.

Reads take `*_VIEW`, writes take `*_MANAGE`/`*_CONFIGURE` — do not gate a write
on a view privilege (see `resolveAlert`, fixed in `0984b7472`, for that mistake).

## Definition of done

- Every service below is gated, exempted with justification, or documented as
  unreachable.
- `ServicePrivilegeCoverageTest` is extended to scan `@Service` **classes** too,
  so this category cannot regress. Verify by inversion: remove one gate and
  confirm the scan fails.
- No role holds fewer privileges than it did before; add grants to `012-004` if a
  new privilege is introduced, and assert reachability in
  `SelfIdentityMethodsUngatedTest`.

## Services

- [ ] `AddressService` — `common/services/` (reached from `OrganizationController.java`)
- [ ] `AnalyzerConnectionProbeService` — `analyzer/service/` (reached from `AnalyzerConnectionProbeRestController.java`)
- [ ] `AuditEntitySnapshotService` — `audittrail/service/` (reached from `SystemAuditEventRestController.java`)
- [ ] `CatalogHealthService` — `testcatalog/service/` (reached from `TestCatalogEditorRestController.java`)
- [ ] `DataSubmitter` — `datasubmission/` (reached from `DataSubmissionController.java`)
- [ ] `DisplayListService` — `common/services/` (reached from `OrganizationController.java`)
- [ ] `ExchangeConfigurationService` — `common/services/` (reached from `ResultReportingConfigurationController.java`)
- [ ] `LoincIntegrityService` — `testcatalog/service/` (reached from `TestCatalogEditorRestController.java`)
- [ ] `MassIndexerService` — `hibernate/search/massindexer/` (reached from `MassIndexerRestController.java`)
- [ ] `MicrobiologyUatScenarioService` — `microbiology/service/` (reached from `MicrobiologyUatScenarioRestController.java`)
- [ ] `ModbusPollingService` — `coldstorage/service/impl/` (reached from `FreezerDeviceController.java`)
- [ ] `NonConformityUpdateWorker` — `qaevent/worker/` (reached from `NonConformityController.java`)
- [ ] `OrderResponseWorker` — `dataexchange/orderresult/` (reached from `ResultValidationController.java`)
- [ ] `PanelTestConfigurationUtil` — `testconfiguration/action/` (reached from `PanelOrderController.java`)
- [ ] `PatientEditUpdate` — `patient/saving/` (reached from `PatientEntryByProjectController.java`)
- [ ] `PatientEntry` — `patient/saving/` (reached from `OrderLabelReprintController.java`)
- [ ] `PatientEntryAfterAnalyzer` — `patient/saving/` (reached from `PatientEntryByProjectController.java`)
- [ ] `PatientEntryAfterSampleEntry` — `patient/saving/` (reached from `PatientEntryByProjectController.java`)
- [ ] `PatientManagementUpdate` — `sample/service/` (reached from `PatientManagementController.java`)
- [ ] `PatientResultTreeService` — `common/services/` (reached from `ResultsTreeProviderRestController.java`)
- [ ] `PatientSecondEntry` — `patient/saving/` (reached from `PatientEntryByProjectController.java`)
- [ ] `PhoneNumberService` — `common/services/` (reached from `CommonValidationsRestController.java`)
- [ ] `QAService` — `common/services/` (reached from `WorkPlanByTestController.java`)
- [ ] `RangeCoverageValidationService` — `testcatalog/service/` (reached from `TestCatalogEditorRestController.java`)
- [ ] `ReportTrackingService` — `common/services/` (reached from `ReportController.java`)
- [ ] `RequesterService` — `common/services/` (reached from `NonConformityController.java`)
- [ ] `ResultSaveService` — `common/services/` (reached from `LogbookResultsController.java`)
- [ ] `ResultsLoadUtility` — `result/action/util/` (reached from `WorkPlanByTestController.java`)
- [ ] `ResultsValidation` — `result/action/util/` (reached from `ResultValidationController.java`)
- [ ] `ResultsValidationRetroCIUtility` — `resultvalidation/util/` (reached from `ResultValidationRetroCController.java`)
- [ ] `ResultsValidationUtility` — `resultvalidation/util/` (reached from `ResultValidationController.java`)
- [ ] `RuleResultScope` — `common/services/` (reached from `CalculatedValueRestController.java`)
- [ ] `SampleEntry` — `patient/saving/` (reached from `BaseWorkplanController.java`)
- [ ] `SampleEntryAfterPatientEntry` — `patient/saving/` (reached from `SampleEntryByProjectController.java`)
- [ ] `SampleOrderService` — `common/services/` (reached from `GenericSampleOrderRestController.java`)
- [ ] `SampleSecondEntry` — `patient/saving/` (reached from `SampleEntryByProjectController.java`)
- [ ] `ShipmentFhirImportService` — `shipment/fhir/` (reached from `ShippingBoxRestController.java`)
- [ ] `StatusService` — `common/services/` (reached from `BaseWorkplanController.java`)
- [ ] `TableIdService` — `common/services/` (reached from `NonConformityController.java`)
- [ ] `TestAddControllerUtills` — `testconfiguration/action/` (reached from `TestAddRestController.java`)
- [ ] `TestReflexUtil` — `testreflex/action/util/` (reached from `AnalyzerResultsController.java`)
## Reproduce this list

```bash
# @Service classes (no interface), reachable from a controller, with no @PreAuthorize
grep -rl '^@Service' src/main/java --include='*.java' \
  | xargs grep -Ll '@PreAuthorize' \
  | xargs grep -l '^public class'
```
