# Research and Scope Decisions

**Inspected OE2 baseline**: `e57a53399c2134fe3ff58009119cc05906c61e5e`  
**Date**: 2026-09-13

## Product Baseline and Clarifications

The pinned
[product specification](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.md)
and
[mock](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.html)
contain Sample & Testing, Referrals and Non-Conformance. The complete fictional
worked example is Sample & Testing. Its seven columns and the broader design's
50 attribute entries are not universal limits on an instance's reporting
catalog. The inspected HTML hash was
`5e0d4386ee29d55e269759dea3c5c326daacdff65117876043e696bc0a7c44e9`.

User clarification establishes functional usefulness and little friction,
instance-aware fields, both layouts with the spreadsheet default, every repeated
result preserved in extra rows, reusable definitions without a personal-library
requirement, and a common configurable reporting capability. These directions
take precedence over conflicting assumptions in the earlier product draft.

## Evidence and Engineering Decisions

| Finding                                                 | Evidence                                                                                                                                                                                                                                                                                                                                                                                                              | Implication                                                                                                                                           |
| ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| Tests are instance data                                 | [TestServiceImpl.java](../../src/main/java/org/openelisglobal/test/service/TestServiceImpl.java) lists configured tests and filters by section; [Test.java](../../src/main/java/org/openelisglobal/test/valueholder/Test.java) carries localized names, active state and units                                                                                                                                        | Resolve local tests and labels through metadata, not a frontend list                                                                                  |
| Tests have configured result fields                     | [TestResultComponent.java](../../src/main/java/org/openelisglobal/testresultcomponent/valueholder/TestResultComponent.java) and [its service](../../src/main/java/org/openelisglobal/testresultcomponent/service/TestResultComponentServiceImpl.java) store labels, types, units, order and repeated-reading configuration                                                                                            | Support component-aware catalog/values after proving actual result linkage                                                                            |
| Additional patient/sample information is configurable   | [ObservationHistoryTypeConfigurationHandler.java](../../src/main/java/org/openelisglobal/observationhistorytype/service/ObservationHistoryTypeConfigurationHandler.java), [ObservationHistory.java](../../src/main/java/org/openelisglobal/observationhistory/valueholder/ObservationHistory.java), [QuestionnaireItem.java](../../src/main/java/org/openelisglobal/questionnaire/valueholder/QuestionnaireItem.java) | Existing metadata can supply configured fields; typed source mappings still need to resolve actual values and repetitions                             |
| Existing configuration loading supports domain handlers | [DomainConfigurationHandler.java](../../src/main/java/org/openelisglobal/configuration/service/DomainConfigurationHandler.java) and [ConfigurationInitializationService.java](../../src/main/java/org/openelisglobal/configuration/service/ConfigurationInitializationService.java)                                                                                                                                   | Reuse the configuration-loading pattern rather than introduce a second administration system                                                          |
| The old Routine CSV already varies test columns         | [CSVRoutineColumnBuilder.java](../../src/main/java/org/openelisglobal/reports/action/implementation/reportBeans/CSVRoutineColumnBuilder.java) builds test and observation-history crosstabs                                                                                                                                                                                                                           | Instance-specific columns are grounded in existing behavior; its native SQL, entry-date filtering and pivot assumptions are not the new engine design |
| The old CSV writer differs from the desired output      | [CSVRoutineSampleExportReport.java](../../src/main/java/org/openelisglobal/reports/action/implementation/CSVRoutineSampleExportReport.java) buffers output and uses Windows-1252                                                                                                                                                                                                                                      | Build a bounded UTF-8 writer; do not extend that legacy path                                                                                          |
| Collection date belongs to the specimen workflow        | [SampleEditRestController.java](../../src/main/java/org/openelisglobal/sample/controller/rest/SampleEditRestController.java), [SamplePatientEntryServiceImpl.java](../../src/main/java/org/openelisglobal/sample/service/SamplePatientEntryServiceImpl.java), [SampleEditServiceIntegrationTest.java](../../src/test/java/org/openelisglobal/sample/service/SampleEditServiceIntegrationTest.java)                    | Use specimen collection date for Sample & Testing, not entry date                                                                                     |
| Result identity and display formatting are distinct     | [Result.java](../../src/main/java/org/openelisglobal/result/valueholder/Result.java), [ResultServiceImpl.java](../../src/main/java/org/openelisglobal/result/service/ResultServiceImpl.java), [StatusService.java](../../src/main/java/org/openelisglobal/common/services/StatusService.java)                                                                                                                         | Preserve identities; prove correction, dictionary/multiselect, component and nullable-precision behavior with fixtures                                |
| Referral and non-conformance records already exist      | [Referral.java](../../src/main/java/org/openelisglobal/referral/valueholder/Referral.java), [NcEvent.java](../../src/main/java/org/openelisglobal/qaevent/valueholder/NcEvent.java), [SampleQaEvent.java](../../src/main/java/org/openelisglobal/sampleqaevent/valueholder/SampleQaEvent.java)                                                                                                                        | Source-specific dates/relationships need mappings, not separate builders or queues; distinguish event occurrence from catalog definition              |
| Existing UI/data access can be reused                   | [Routine.jsx](../../frontend/src/components/reports/Routine.jsx), [useServerData.ts](../../frontend/src/components/utils/useServerData.ts), [UserRoleServiceImpl.java](../../src/main/java/org/openelisglobal/userrole/service/UserRoleServiceImpl.java)                                                                                                                                                              | Native entry points, shared queries and current role/lab access support a low-friction workflow                                                       |

These are source findings, not proof that new exports already work. Catalog
existence does not establish every value mapping. Component-result associations,
questionnaire repetitions and historical non-conformance/rejection relationships
must be established in the planned fixture tests.

## Source Coverage and Deliberate Changes

Source IDs below belong to the pinned product document, not this feature's FR
IDs.

| Product behavior                                           | Application-owned MVP decision                                                                                          |
| ---------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| FR-1 catalog, groups and ordered selection; BR-009         | Keep interaction intent; resolve common/configured fields from the instance without a fixed count                       |
| FR-1 report families                                       | One configured capability for the three mock types; no separate report applications                                     |
| FR-2 dates and filters                                     | Keep named filters and definition-specific dates; useful defaults, including finalized results for Sample & Testing     |
| FR-3 review and retained choices                           | Keep; offer completed output within the current flow and retain queue access                                            |
| FR-4/FR-5 estimates and separate synchronous generation    | Defer the extra execution mode; use one job path with inline ready-file delivery                                        |
| FR-6 and job lifecycle rules                               | Keep bounded queue, download/re-download, retry, queued cancellation, recovery and expiry                               |
| FR-7 saved configurations                                  | Include shared instance-wide definitions; the user removed personal-library scope as an MVP prerequisite                |
| FR-8 preferences, estimated wait, cross-page notifications | Defer preference persistence and advanced notifications; current-page job updates remain                                |
| BR-003/004 access and identifying fields                   | Reuse existing reporting/patient/lab access; no blanket exclusion of patient fields or new permission-setup journey     |
| BR-012 row meaning                                         | Support the user's spreadsheet and detailed layouts; keep all repeated results and explicit source-specific row meaning |
| BR-015 compatible field families                           | A definition selects a known source and valid fields; no accidental mixed-source join                                   |
| BR-017 CSV format                                          | Keep BOM, escaping, nulls and ISO dates; common headers are canonical, configured headers use captured instance labels  |
| BR-013 cancelled-history removal                           | Retain history in MVP; automated record deletion and generic queue deletion are deferred                                |
| Custom file names, dedicated settings controls             | Generated file names and existing configuration initially                                                               |
| QC/Catalyst/new external integrations                      | Outside this reporting implementation                                                                                   |

## What Configuration Can and Cannot Supply

Configuration can name a report, select a supported source, expose fields and
filters, set defaults, declare its date/row meaning and choose layouts. Instance
metadata supplies configured items and labels. A source mapping resolves the
actual records, relationships and calculations. A new definition over an
existing mapping should need configuration only; a new source kind can require
mapping code. There is no promise to infer arbitrary schema joins from a
configuration file.

Prove this boundary with three shipped definitions and an additional definition
over an existing source, using the same frontend and job implementation.

## Non-Conformance source audit — 2026-09-14

The rejection entry path creates an `NcEvent` and an explicit `NceSpecimen`
link. It sets `reportDate` but leaves `dateOfEvent` empty. The general event
creation worker also sets the reporting date; its update path can later set the
actual event date. The user explicitly approved the fallback on September 14,
2026: use the event date when present, otherwise the recorded date, with a
visible date-basis label. Ordinary recorded rejections therefore remain eligible
for the selected period. This resolves the date-choice blocker; implementation
and focused validation remain required. No activation or acceptance is claimed
by recording this decision.

Reason identifiers need their source context. `RejectionController.setupForm`
uses `DisplayListService.QA_EVENTS`, whose `createSortedQAEvents` reads `QaEvent`.
Saving that form writes its chosen ID to both `SampleItem.rejectReasonId` and
`NcEvent.nceTypeId`. By contrast, `NonConformingEventWorkerImpl` populates its
form from `NceTypeService` and saves that type ID to `NcEvent.nceTypeId`.
Consequently a blanket lookup of every event's type ID in `nce_type` is not
justified. A linked specimen's recorded rejection reason has its own `QaEvent`
authority; the configured event type is a separate concept. Fixture coverage must
include equal numeric IDs in these different catalogs to prevent a plausible but
wrong reason label.

`NceSpecimen` stores explicit event, specimen and optional analysis IDs. Use
these links rather than guessing associations from accession text. The older
`SampleQaEvent` has its own occurrence identity, entered date and sample/specimen
relationships; its inspected mapping has no `NcEvent` foreign key.
`NonConformityUpdateWorker` still creates those older records. Any combined
export must preserve distinct occurrence identities and prove a shared occurrence
before deduplicating; equality of catalog reason, sample or calendar date alone
is insufficient. These are source-audit findings, not a claim that the
Non-Conformance export is implemented or validated.

Inspected source: `qaevent/controller/RejectionController.java`,
`common/services/DisplayListService.java`,
`qaevent/worker/NonConformingEventWorkerImpl.java`,
`qaevent/worker/NonConformityUpdateWorker.java`, the `NcEvent` and `NceSpecimen`
valueholders, and `src/main/resources/hibernate/hbm/SampleQaEvent.hbm.xml`.
