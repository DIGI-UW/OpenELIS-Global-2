# Tasks: Migrate @PreAuthorize from Controllers to Service Layer

**Input**: `specs/012-privilege-based-rbac/` (existing privilege model)
**Prerequisites**: spec 012 Phase 1–3 complete ✓

**Motivation**: Currently all 196 `@PreAuthorize` annotations sit on
controllers. This means any service method called from a scheduled job, an
internal service call, or a future non-HTTP entry point bypasses authorization
entirely. Moving annotations to the service layer makes authorization enforce at
the business logic boundary regardless of how the service is invoked.

**Architecture rule**: `@PreAuthorize` belongs on the **service interface
method**, not the implementation. The controller keeps no `@PreAuthorize`. The
controller's role is routing and serialization only.

**Sequencing rule**: Each domain group (S001–S010) is independent and can be
done in parallel. The teardown phase (S011) runs last, after all domains are
migrated and tests are green.

---

## Guiding principles

- Annotate the **service interface method**, not `ServiceImpl` — Spring's proxy
  picks up annotations on the interface
- Use the same `hasAuthority('PRIV_*')` expressions already established in spec
  012 — no new privilege names introduced here
- Remove the `@PreAuthorize` from the controller **in the same commit** as
  adding it to the service — never leave both in place simultaneously
- `EndpointPrivilegeCoverageTest` (T018) must be updated to scan service
  interfaces instead of `@RestController` classes — done in S000 before any
  domain migration
- Where a service method is called with multiple privilege levels (e.g. view vs.
  edit), use `hasAnyAuthority(...)` on the service method or split into separate
  methods

---

## S000 — Test infrastructure update (do first)

- [ ] S000 Update `EndpointPrivilegeCoverageTest` to scan `@Service`-annotated
      interfaces for `@PreAuthorize("hasAuthority(...)")` instead of scanning
      `@RestController` classes. Keep the controller scan running in parallel
      during the migration window so both layers are verified green until S011.
      `src/test/java/org/openelisglobal/security/EndpointPrivilegeCoverageTest.java`

---

## S001 — Orders domain

**Privilege in use**: `PRIV_ORDER_VIEW`, `PRIV_ORDER_CREATE`

Controllers currently annotated:

- `AddressHierarchyRestController` — `PRIV_ORDER_VIEW`
- `EntityNamesProviderRestController` — `PRIV_ORDER_VIEW`
- `TestNamesProviderRestController` — `PRIV_ORDER_VIEW`
- `AllTestsForSampleTypeProviderRestController` — `PRIV_ORDER_VIEW`
- `OrderProgramsDashboardController` — `PRIV_ORDER_VIEW`
- `ElectronicOrdersRestController` — `PRIV_ORDER_VIEW`
- `DBImageController` — `PRIV_ORDER_VIEW`
- `GenericSampleOrderRestController` — `PRIV_ORDER_CREATE`
- `SampleRestController` — `PRIV_ORDER_CREATE`
- `QaChecklistRestController` — `PRIV_ORDER_CREATE`
- `SampleBatchEntryRestController` — `PRIV_ORDER_CREATE`
- `SampleItemRestController` — `PRIV_ORDER_CREATE`
- `SampleTypeRequestRestController` — `PRIV_ORDER_CREATE`
- `ReferralRestController` — `PRIV_ORDER_CREATE`
- `TestRestController` — `PRIV_ORDER_CREATE`

- [ ] S001a Add `@PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")` to read
      methods on `SampleService`, `GenericSampleOrderService`,
      `ElectronicOrderService` interfaces. Remove from corresponding
      controllers.

- [ ] S001b Add `@PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")` to write
      methods on `SamplePatientEntryService`, `SampleEditService`,
      `GenericSampleOrderService` interfaces. Remove from corresponding
      controllers.

---

## S002 — Results domain

**Privilege in use**: `PRIV_RESULT_VIEW`, `PRIV_RESULT_ENTER`,
`PRIV_RESULT_MODIFY`

Controllers currently annotated:

- `AccessionResultsRestController` — `PRIV_RESULT_VIEW`
- `PendingAnalysisForTestProviderRestController` — `PRIV_RESULT_VIEW`
- `WorkplanByPriorityRestController` — `PRIV_RESULT_VIEW`
- `WorkplanByTestRestController` — `PRIV_RESULT_VIEW`
- `WorkplanByTestSectionRestController` — `PRIV_RESULT_VIEW`
- `WorkplanByPanelRestController` — `PRIV_RESULT_VIEW`
- `ResultRestController` — `PRIV_RESULT_ENTER`, `PRIV_RESULT_MODIFY`

- [ ] S002a Add `@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")` to read
      methods on `ResultService`, `AnalysisService` interfaces. Remove from
      corresponding controllers.

- [ ] S002b Add `@PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")` and
      `@PreAuthorize("hasAuthority('PRIV_RESULT_MODIFY')")` to write methods on
      `LogbookResultsPersistService`, `ResultService` interfaces. Remove from
      corresponding controllers.

---

## S003 — Validation domain

**Privilege in use**: `PRIV_RESULT_VALIDATE`

Controllers currently annotated:

- `AccessionValidationRestController` — `PRIV_RESULT_VALIDATE`
- `ValidationRestController` — `PRIV_RESULT_VALIDATE`

- [ ] S003 Add `@PreAuthorize("hasAuthority('PRIV_RESULT_VALIDATE')")` to
      validate methods on `ResultValidationService` interface. Remove from
      corresponding controllers.

---

## S004 — Patient domain

**Privilege in use**: `PRIV_PATIENT_VIEW`, `PRIV_PATIENT_EDIT`

Controllers currently annotated:

- `PatientSearchRestController` — `PRIV_PATIENT_VIEW`
- `PatientSearchPopulateRestController` — `PRIV_PATIENT_VIEW`
- `SampleRestController` (patient read path) — `PRIV_PATIENT_VIEW`
- `PatientRestController` — `PRIV_PATIENT_EDIT`
- `SampleRestController` (patient write path) — `PRIV_PATIENT_EDIT`

- [ ] S004a Add `@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")` to read
      methods on `PatientService`, `PatientIdentityService` interfaces. Remove
      from corresponding controllers.

- [ ] S004b Add `@PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")` to write
      methods on `PatientService` interface. Remove from corresponding
      controllers.

---

## S005 — NCE (Non-Conformance Events) domain

**Privilege in use**: `PRIV_NCE_VIEW`, `PRIV_NCE_CREATE`, `PRIV_NCE_EDIT`

Controllers currently annotated:

- `NceEnhancementRestController` — `PRIV_NCE_VIEW`
- `ViewNonConformEventsRestController` — `PRIV_NCE_VIEW`
- `ReportNonConformEventsRestController` — `PRIV_NCE_VIEW`
- `NonConformingEventsCorrectionActionRestController` — `PRIV_NCE_VIEW`
- `NceEnhancementRestController` (create endpoints) — `PRIV_NCE_CREATE`
- `NceEnhancementRestController` (edit endpoints) — `PRIV_NCE_EDIT`

- [ ] S005 Add `@PreAuthorize` to methods on `NCEventService`,
      `NceNumberGeneratorService`, `NceCategoryService`, `NceTypeService`
      interfaces using the appropriate `PRIV_NCE_*` privilege. Remove from
      corresponding controllers.

---

## S006 — Analyzer domain

**Privilege in use**: `PRIV_ANALYZER_IMPORT`, `PRIV_ANALYZER_CONFIGURE`

Controllers currently annotated:

- `AnalyzerFhirImportController` — `PRIV_ANALYZER_IMPORT`
- `AnalyzerImportController` — `PRIV_ANALYZER_IMPORT`
- `ResultRestController` (analyzer path) — `PRIV_ANALYZER_IMPORT`
- `AnalyzerRestController` — `PRIV_ANALYZER_CONFIGURE`
- `AnalyzerPluginConfigRestController` — `PRIV_ANALYZER_CONFIGURE`
- `AnalyzerTestNameMenuRestController` — `PRIV_ANALYZER_CONFIGURE`
- `AnalyzerTestNameRestController` — `PRIV_ANALYZER_CONFIGURE`

- [ ] S006a Add `@PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")` to
      import methods on `AnalyzerService`, `AnalyzerResultsService` interfaces.
      Remove from corresponding controllers.

- [ ] S006b Add `@PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")` to
      configuration methods on `AnalyzerService`, `AnalyzerTestMappingService`
      interfaces. Remove from corresponding controllers.

---

## S007 — Reports domain

**Privilege in use**: `PRIV_REPORT_RUN`

Controllers currently annotated:

- `ReportRestController` — `PRIV_REPORT_RUN`
- `ReportDefinitionRestController` — `PRIV_REPORT_RUN`
- `PrintWorkplanReportRestController` — `PRIV_REPORT_RUN`
- Various reports controllers — `PRIV_REPORT_RUN`

- [ ] S007 Add `@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")` to report
      execution methods on `PatientReportService`, `ReportService` interfaces.
      Remove from corresponding controllers.

---

## S008 — Shipment domain

**Privilege in use**: `PRIV_SHIPMENT_VIEW`, `PRIV_SHIPMENT_CREATE`

Controllers currently annotated:

- `ShipmentRestController` — `PRIV_SHIPMENT_VIEW`
- `StorageRestController` — `PRIV_ADMIN_SYSTEM` (storage is admin-scoped)

- [ ] S008 Add `@PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")` to read
      methods and `@PreAuthorize("hasAuthority('PRIV_SHIPMENT_CREATE')")` to
      write methods on `ShipmentService` interface. Remove from corresponding
      controllers.

---

## S009 — EQA domain

**Privilege in use**: `PRIV_EQA_VIEW`, `PRIV_EQA_MANAGE`

Controllers currently annotated:

- `EQAAlertRestController` — `PRIV_EQA_VIEW`
- `EQAMyProgramsRestController` — `PRIV_EQA_VIEW`
- `EQAOrdersRestController` — `PRIV_EQA_VIEW`
- `EQAResultRestController` — `PRIV_EQA_VIEW`
- `EQASubmissionRestController` — `PRIV_EQA_MANAGE`

Additionally, several EQA controllers have **commented-out** legacy
`hasRole('EQA Coordinator')` / `hasRole('Global Administrator')` annotations
that were never migrated:

- `EQAProgramRestController` — 3 methods
- `EQAEnrollmentRestController` — 2 methods
- `EQADistributionRestController` — 3 methods

- [ ] S009a Add `@PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")` to read methods
      and `@PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")` to write methods on
      `EQAResultService`, `SampleEQAService`, `EQAProgramEnrollmentService`,
      `EQADistributionService` interfaces. Remove from corresponding
      controllers.

- [ ] S009b Remove the commented-out `hasRole(...)` annotations from
      `EQAProgramRestController`, `EQAEnrollmentRestController`,
      `EQADistributionRestController` — they are dead code and misleading.

---

## S010 — Admin / System domain

**Privilege in use**: `PRIV_ADMIN_SYSTEM`, `PRIV_AUDIT_VIEW`

This is the largest group — 115 controllers annotated with `PRIV_ADMIN_SYSTEM`
across: `testconfiguration`, `common`, `coldstorage`, `calendar`, `analyzer`,
`inventory`, `sitebranding`, `storage`, `systemuser`, `siteinformation`,
`provider`, `organization`, `notification`, `localization`, `fhir`,
`externalconnections`, `esig`, `dictionary`, `dataexchange`, `barcode`,
`audittrail`, `alert`, `admin`, and more.

- [ ] S010a Identify the ~15 core admin service interfaces that back these
      controllers. For each, add
      `@PreAuthorize("hasAuthority('PRIV_ADMIN_SYSTEM')")` to their mutating
      methods (create, update, delete, configure). Read-only admin methods can
      use `PRIV_ADMIN_SYSTEM` as well since all admin reads are admin-only.
      Remove from corresponding controllers.

- [ ] S010b Add `@PreAuthorize("hasAuthority('PRIV_AUDIT_VIEW')")` to read
      methods on `AuditTrailService` (or equivalent). Remove from
      `AuditTrailReportRestController`.

- [ ] S010c Add `@PreAuthorize("hasAuthority('PRIV_ESIG_USE')")` to electronic
      signature methods (currently on 6 esig controllers). Remove from
      corresponding controllers.

---

## S011 — Teardown: remove controller-layer @PreAuthorize (LAST)

**⚠️ Do not start until S000–S010 are all complete and CI is green.**

- [ ] S011a Update `EndpointPrivilegeCoverageTest` to scan service interfaces
      ONLY — remove the controller scan added in S000.

- [ ] S011b Verify no `@PreAuthorize` remains on any `@RestController` class or
      method. A `grep` CI check can enforce this going forward.

- [ ] S011c Add a CI grep rule that fails the build if any new `@PreAuthorize`
      is added to a controller — all new authorization must go on service
      interfaces.

---

## Test strategy per domain task

For each S00X domain task, write/update tests before moving the annotation:

1. **Red**: confirm the service method has no `@PreAuthorize` yet (test that
   calling it without the right authority does NOT throw `AccessDeniedException`
   — it should fail for other reasons)
2. **Move annotation**: add to service interface, remove from controller
3. **Green**: confirm calling the service method without the right authority now
   throws `AccessDeniedException`; calling with the right authority does not

Use `@WithMockUser(authorities = "PRIV_*")` or
`SecurityMockMvcRequestPostProcessors.user(...)` for service-layer security
tests with `@EnableMethodSecurity`.

---

## Risk notes

- **S010 is the highest risk** — 115 controllers. Batch by package, not all at
  once. Each package should be its own commit.
- **Service methods shared across privilege levels** (e.g. `ResultService` is
  used by both result entry and validation paths) — use `hasAnyAuthority(...)`
  on the service interface or split the method if the operations are
  meaningfully different.
- **Internal service-to-service calls** — if `ServiceA` calls `ServiceB` and
  `ServiceB` has `@PreAuthorize`, Spring's proxy is bypassed for direct Java
  calls. Use `@Autowired ServiceB` (not direct instantiation) so the proxy is in
  the call path.

---

## Dependencies & Execution Order

```
S000 (Test infrastructure)
    ├── S001 (Orders)
    ├── S002 (Results)
    ├── S003 (Validation)       ← all parallel once S000 is done
    ├── S004 (Patient)
    ├── S005 (NCE)
    ├── S006 (Analyzer)
    ├── S007 (Reports)
    ├── S008 (Shipment)
    ├── S009 (EQA)
    └── S010 (Admin — largest, do last among domains)
            └── S011 (Teardown — after all domains done)
```
