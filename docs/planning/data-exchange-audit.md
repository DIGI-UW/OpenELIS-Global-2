# Data exchange audit: operational ingress and FHIR egress

Audited 8 Oct 2026 against OpenELIS `develop` at `7300b77`, the analyzer stack
`feat/analyzer-baseline-7-specs` (`c52a9f6`) and the security branch
`fix/analyzer-security-pairing` (#4657, `2cfa806`) where a row says so, Bridge
3.3.0 (`1d97585`), the `dataexport` submodule at `bc1c0d4`, and the Claude
Security export of 7 Oct (109 findings, cited as `#n`). Every claim below was
read in source; nothing was exercised at runtime.

Rewritten the same day from a results-only audit, at the owner's direction: "we
are not auditing just results ingress, but FHIR-based and/or data coming in to
OE2 any way, and also outgress too", narrowed to "operational ingress, and
FHIR-based outgress for now".

## Scope

**In: operational ingress.** Every path on which data enters OpenELIS while the
lab runs: analyzer results, orders, patients, referral results, EQA reports,
shipments, and the external FHIR doors (the facade and the co-resident store).
Setup imports done by an admin from a file are not operational and are listed
under "Out of scope".

**In: FHIR egress.** Every path on which OpenELIS data leaves as FHIR: the store
push, the subscriptions and data export to a consolidated server, FHIR Pipes,
referral Tasks, EQA submissions, and the two read doors (the facade and
`/rest/fhir`).

**Out, by decision.** Catalog, locations, holidays, breakpoint and OCL imports
and the Generic Sample Order CSV import (setup or admin actions behind a session
and a role). Notifications (SMS, SMPP, email, WhatsApp:
`notification/service/sender`), the XML result exporter
(`scheduler/independentthreads/ResultExporter.java:71-85`, POST to
`resultReportingURL`), the malaria and aggregate indicator reporting, the
`DataSubmitter`, and the Odoo invoice call. The notification senders and the XML
exporter carry patient data out of the lab and are the largest unaudited egress
after this document; they are out of scope because they are not FHIR, not
because they are safe.

## The rules

Set by the owner on 8 Oct:

1. Microbiology analyzer traffic is not separate from the main analyzer path.
2. No path lets an unstaged result into OpenELIS or into the co-resident FHIR
   store. A result from a machine, a partner lab or a FHIR client is staged, and
   a person accepts it before it is a clinical result.
3. One identity per service, no shared private keys; trust is an explicit pin of
   the peer's certificate (the Bridge pairing model). The co-resident store is
   the accepted exception ("should stay as-is").
4. Data leaves only through a named door: a known peer, OpenELIS's own identity
   on the link, status-marked resources, and a record of what was sent. The
   `/fhir/*` facade only reads.

Rule 4 is this rewrite's wording of the egress side; the owner set the inbound
rules and the facade's read-only decision, and has not yet confirmed this
sentence.

## What good looks like

- Instrument results pass a review step before release to the patient record
  (CLSI AUTO10-A, AUTO15). In OpenELIS that step is the Analyzer Results page
  (`analyzer_results`, then accept) and result validation.
- A FHIR server decides, per interaction, whether this client may create, update
  or delete this resource, including inside a transaction bundle, and
  authenticates clients by mutual TLS, keys or signed tokens (HL7 FHIR R4,
  Security). On HAPI that is an `AuthorizationInterceptor` with default deny.
- One machine, one identity, one door: a machine's credential reaches the one
  endpoint that stages its data and nothing else. The Bridge pairing model is
  this: the Bridge's own certificate, pinned by OpenELIS, is its identity.
- Outbound links carry one named identity per peer, over TLS, and leave an audit
  record (FHIR R4 `AuditEvent`; US Core requires audit of every transaction). A
  rest-hook Subscription endpoint is an outbound link.
- A health check probes the path deliveries take. A green probe on another path
  hides a dead delivery path (the Nosy Be incident, Bridge #44).

## Inventory A: operational ingress

| #   | Path                                                                                                                                                                                                                                       | Peer                                                                                                 | Identity today                                                                                                                                                                                         | Writes                                                                                                                                                                                                                                                                                                                                                  | Staged or reviewed?                                                        |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| A1  | `POST /analyzer/fhir`                                                                                                                                                                                                                      | Analyzer Bridge                                                                                      | Basic chain, `authenticated()` only (`SecurityConfig.java:176`); the module interceptor then admits `is_admin=Y` users (#51). #4657: its own chain for the paired certificate (`BRIDGE_INGRESS_PATHS`) | `analyzer_results`, delivery receipts; QC control rows go straight to QC results                                                                                                                                                                                                                                                                        | Patient rows staged; control rows final                                    |
| A2  | `POST /rest/AnalyzerResults`                                                                                                                                                                                                               | Reviewer on the Analyzer Results page                                                                | `ANALYSER_IMPORT` or `ADMIN` (`AnalyzerResultsController.java:87`)                                                                                                                                     | Promotes staged rows to `Result`, analysis to TechnicalAcceptance                                                                                                                                                                                                                                                                                       | The accept step                                                            |
| A3  | `/fhir/*` HAPI facade (`FhirRestfulServer`, 9 providers), writes                                                                                                                                                                           | External FHIR clients                                                                                | Any authenticated account; no role anywhere under `fhir/`; the MVC module interceptor never runs for this servlet (#19)                                                                                | Observation create/update/delete writes `Result` and `Analysis` through `persistDataSet` and pushes to the store (`ObservationProvider.java:176-184`); DiagnosticReport delete cancels any analysis (`DiagnosticReportProvider.java:129`); Device writes `Analyzer` rows; ServiceRequest, Patient, Specimen (#69), Location, Organization, Practitioner | Final, no staging                                                          |
| A4  | `POST /rest/analyzer/events/ast`, `/culture`                                                                                                                                                                                               | "Bridge-originated" per spec 782; nothing in the Bridge or mock calls them, only a Playwright helper | Own chain, Basic plus `ANALYSER_IMPORT` (`SecurityConfig.java:122,157`)                                                                                                                                | AST: `analyzer_event` row, then `micro_ast_reading` rows marked `ANALYZER_AUTO`, run to RESULTS_IN; a bench user reviews the run. Culture: case stage to POSITIVE_SIGNAL, no review                                                                                                                                                                     | AST staged in its own table; culture not reviewed                          |
| A5  | Scheduled Task pull from every `remote.source.uri` (`FhirApiWorkFlowServiceImpl.java:88-121`, every `remote.poll.frequency`, default 120 s)                                                                                                | Partner OpenELIS or OpenMRS                                                                          | Basic with `fhirstore.username` and `fhirstore.password` (`FhirConfig.java:42-46`); no deployment sets `remote.source.uri` today (roadmap, S8)                                                         | Orders as `ElectronicOrder`; patient rows written directly; returned referral results fetched (`:275-290`) and held for Accept; peer-chosen resource ids kept as local ids (#48); the peer's Task is updated in place (`:511`)                                                                                                                          | Orders and results staged; patients final                                  |
| A6  | `PUT /rest/reference-lab-results/referrals/{id}/accept`                                                                                                                                                                                    | Lab user                                                                                             | RESULTS, VALIDATION or ADMIN                                                                                                                                                                           | `Result` rows, analysis to Finalized (`ReferenceLabResultsServiceImpl.java:79-104`)                                                                                                                                                                                                                                                                     | Reception (OGC-803): a person accepts; the reference lab already validated |
| A7  | EQA reports: participant reports polled from the local store, score reports from each remote store (`EQAFhirExchangeServiceImpl.java:54-98`), every 120 s (`EQAFhirExchangeScheduler.java:22,35`)                                          | Participating labs and the EQA provider, through FHIR stores                                         | Whatever the store trusts (the certgen cert locally, Basic remotely); the report's identifier system is the only selector                                                                              | Scoring and score records applied on arrival                                                                                                                                                                                                                                                                                                            | Not clinical results; applied without a person                             |
| A8  | Client registry search (`PatientSearchRestController.java:171-215`) against `crserver.uri`                                                                                                                                                 | External client registry                                                                             | Basic with `crserver.username` and `crserver.password`; no deployment sets `crserver.uri` today (S8)                                                                                                   | Search results; a registry patient with no national id gets one generated (`generateDynamicID`); the patient is created locally when the user picks the row                                                                                                                                                                                             | A person picks the row; the generated id is the gap                        |
| A9  | Shipment poll of SupplyDelivery from each remote store (`ShipmentFhirImportService.java:154-176`) and the `import-from-fhir` action on `ShippingBoxRestController.java:859`                                                                | Partner store                                                                                        | The pull's Basic                                                                                                                                                                                       | Shipping boxes; with `remote.source.updateStatus` the peer's Task is updated (`:97`)                                                                                                                                                                                                                                                                    | Operational data, not results                                              |
| A10 | Co-resident FHIR store `fhir.openelis.org:8443/fhir`, direct                                                                                                                                                                               | OpenELIS itself, partners that write Tasks or EQA reports, FHIR Pipes                                | mTLS only, no application auth (`hapi_application.yaml` has no interceptor); trusts the one certgen cert                                                                                               | Anything, directly into HAPI's tables                                                                                                                                                                                                                                                                                                                   | None                                                                       |
| A11 | `POST /rest/fhir/replay` (admin re-push to the store); `/OrderRequest`, `/OrderRequest_Raw` (HL7 OML, any authenticated account, #73, #81: removed on #4657); `/pluginServlet/**` (`permitAll`, `SecurityConfig.java:109`, nothing mapped) | Admin; HL7 senders; nobody                                                                           |                                                                                                                                                                                                        |                                                                                                                                                                                                                                                                                                                                                         | n/a; removed; dead but open                                                |

Exposure: the shipped proxy forwards all of `/api/` to OpenELIS
(`volume/nginx/nginx-prod.conf:30`), and Tomcat serves the webapp at
`/api/OpenELIS-Global/` (`tomcat/oe_server.xml:141`), so A1, A3 and A4 are
reachable on the public 443 behind OpenELIS authentication alone. OpenELIS 8443,
the store's 8444 and the database's 15432 are published on every interface
(`docker-compose.yml:50,81,23`). Every distro does the same (S4a).

## Inventory B: FHIR egress

| #   | Path                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Peer                                                                                                                                                              | Identity today                                                                                                                                                                                                         | What leaves                                                                                                                                      |
| --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| B1  | Store push: every persist builds a transaction bundle to `fhirstore.uri` (`FhirPersistanceServiceImpl.java:71-155`); at result entry and validation (`ResultEntryRestController.java:365`), Observation status is `final` only when the analysis is Finalized (`ObservationTransformServiceImpl.java:290-298`)                                                                                                                                                                                                                                                                                | The store named by `fhirstore.uri`: the co-resident store, or a consolidated server when a lab points the setting there (`DIGI-UW/openelis-consolidated-server`)  | mTLS with the certgen key when co-resident; Basic `fhirstore.username` and `fhirstore.password` when remote. Every distro sets `admin` and the default password; OpenELIS only sends them to a store that is not local | Patient, ServiceRequest, Specimen, Observation, DiagnosticReport, Task, Practitioner, Organization, Encounter, Questionnaire; status-marked (F3) |
| B2  | Subscriptions and data export: at boot `RegisterFhirHooksTask` registers one rest-hook Subscription per type in `fhir.subscriber.resources` on the store, channel endpoint `fhir.subscriber` (`:161`), and a `DataExportTask` for that endpoint; `DataExportServiceImpl` (dataexport submodule) reads the local store and sends transaction bundles with the headers stored on the task (`:174-181`), on the backup interval, on `POST /dataexport/fhir` (`FhirExportController`, no role of its own), on `POST /rest/DataExportStatus/{id}/trigger`, and from `FhirTransformationController` | The consolidated server                                                                                                                                           | The task's stored headers (Basic admin in the distros); plain `http://` accepted when `fhir.subscriber.allowHTTP=true`, which the shipped `volume/properties/common.properties:14` sets                                | Task, Patient, ServiceRequest, DiagnosticReport, Observation, Specimen, Practitioner, Encounter (`common.properties:36`): the patient record     |
| B3  | FHIR Pipes pulling the store's 8444                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | The consolidated server                                                                                                                                           | mTLS with the certgen key ("the same cert for both the web app and hapi fhir", 25 Feb 2026)                                                                                                                            | The whole store                                                                                                                                  |
| B4  | Referral out: `referAnalysisesToOrganization` writes Task and ServiceRequest to the local store (`FhirReferralServiceImpl.java:177`); completion, lost and rejected are Task updates (`:247,331,400`); the partner's Task pull reads them. On the pulling side OpenELIS writes status into the peer's store (A5, `:511`)                                                                                                                                                                                                                                                                      | Partner OpenELIS                                                                                                                                                  | The store's trust on the way out; the pull's Basic admin on the write-back                                                                                                                                             | Task, ServiceRequest, Patient, Specimen                                                                                                          |
| B5  | EQA submissions: transaction bundle to each remote store and the local one (`EQAFhirSubmissionServiceImpl.java:205,224`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | The EQA provider's store                                                                                                                                          | Basic to the remote store                                                                                                                                                                                              | DiagnosticReport and Observations (EQA, not patient results)                                                                                     |
| B6  | `/fhir/*` facade reads and searches                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | eSIL (Madagascar) with a Basic account; anyone with an account                                                                                                    | Any authenticated account; no role; no audit; `_count` uncapped (#84)                                                                                                                                                  | Every resource type the nine providers serve                                                                                                     |
| B7  | `/rest/fhir/{type}`, `/{type}/{id}`, `/{type}/_search` (`FhirQueryRestController.java:75-323`), read from the store                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | The UI: Generic Sample Order reads `/rest/fhir/Questionnaire/{id}` (`GenericSampleOrder.jsx:170`, `GenericSampleOrderEdit.jsx:169`); any authenticated user (#64) | Session                                                                                                                                                                                                                | Any resource type in the store                                                                                                                   |
| B8  | UI providers reading the store directly (`PatientDashBoardProvider.java:322`, `LabOrderSearchProvider.java:141`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Internal                                                                                                                                                          | The webapp's own client                                                                                                                                                                                                | Reads only; noted for completeness                                                                                                               |

## Findings

Ordered by distance from the rules. F1 to F8 keep their numbers from the first
version of this audit so the roadmap's references hold.

### F1. The webapp's FHIR facade is a second, unreviewed door for results

A3. `POST /fhir/Observation` from any account creates a `Result` and pushes it
to the store; `PUT` overwrites one; `DELETE` rejects it;
`DELETE /fhir/DiagnosticReport` cancels an analysis in any state. No role, no
staging, no `AuthorizationInterceptor`, no bounded-decimal check (#102, and
#4657's `BoundedDecimal` does not reach this class). Device writes create and
delete analyzers outside analyzer setup. This is the path Bridge #44's reporter
hit by accident; it failed for them only because no transaction handler is
registered. Breaks rule 2. Triage #19, #69, #102. #4657: untouched.

### F2. The co-resident FHIR store trusts one shared key, and the Bridge holds it

A10, B3. certgen makes one self-signed key (`frontend/Dockerfile.certs:31-41`);
it is the server identity of OpenELIS, the store and the Bridge, the only entry
in the store's truststore, and the volume is world-writable. The shipped
analyzer overlay mounts that volume into the Bridge
(`docker-compose.analyzers.yml:19`) and runs it on the `dev` profile (`:26`),
which loads the private key with `kspass`
(`bridge/src/main/resources/application-dev.yml:3-7`). The Bridge never calls
the store, but it holds the one credential the store accepts, on the same
network. The store has no application-level check. Breaks rule 3. Triage #7, #9,
#26, #42, #106. #4657: not in scope.

Two consequences for pairing: a fingerprint pinned to "the Bridge certificate"
also matches OpenELIS and the store while they share a key; and Bridge 3.3.0
only generates its own identity when no keystore is mounted
(`BridgeIdentityEnvironment.java:25`), so the shipped overlay suppresses it.

### F3. The store holds every result with its validation state (no finding)

B1. The pushed Observation's status is `final` only when the analysis is
Finalized and `preliminary` otherwise, and a DiagnosticReport is written only
when Finalized. That is the owner's intended model ("the fhir store should hold
the observation but have different states based on if it's validated or not").
Nothing to change; consumers filter on status.

### F4. The Bridge's credential can accept the rows it staged

A1, A2. `/analyzer/fhir` has no role of its own, so the Bridge runs with an
account that is at least `ANALYSER_IMPORT` and in practice admin (#51, default
`admin`/`adminADMIN!` in the overlay). That same role accepts staged rows,
retries and dismisses delivery issues. A machine that can stage and accept is
unstaged in effect. Rule 2. #4657's pairing chain gives the Bridge its own
identity on `/analyzer/fhir` only, which is the fix, provided the human reviewer
role and the Bridge identity never coincide.

### F5. Microbiology has its own door, its own role chain and no caller

A4. Spec 782 planned "Bridge-originated normalized analyzer events" on a JSON
envelope, but no Bridge profile produces them; the only caller is the Playwright
seed helper. AST events stage in `analyzer_event` and `micro_ast_reading`, with
their own review, their own import-issue surface and their own security chain;
culture events change a case stage with no review. Rule 1 by design. #4657 keeps
it on Basic plus `ANALYSER_IMPORT` because "the Bridge does not invoke it".

### F6. The Bridge's delivery and health are two settings that can disagree

`forward-http-server.uri` and `health-uri` are independent
(`HTTPForwardServerConfigurationProperties.java:19,35`); a URI that does not end
in `/analyzer` sends every result to the HAPI base URL (HAPI-0287) while health
probes `/health` and stays green. Pairing does not carry the delivery URL
(`PairingState.java:43`). The shipped overlay also sets `INSECURE_TLS=true` and
disables hostname verification (`docker-compose.analyzers.yml:40-41`). Bridge
#44; triage #1 for the OpenELIS side (`BridgeHttpClient` trusts every
certificate; #4657 pins the Bridge). The fix is one base URL and a Bridge that
verifies its target (roadmap, S6).

### F7. Secondary inbound paths, as decided after S9a

A5, A6. Referral Accept finalizing the analysis is the OGC-803 reception model
("the reference lab already validated; no local revalidation step"): a person
accepts, rule 2 holds, not a finding. `DELETE /fhir/DiagnosticReport` goes with
F1. The Task pull keeping peer-chosen logical ids (#48) is against FHIR R4's
rule that a logical id belongs to the storing server; the fix is local ids with
the remote id as an identifier, and it waits for the data exchange hardening
because no deployment uses the pull today.

### F8. Dead and stale surface

`/pluginServlet/**` is open with nothing behind it. `SecurityConfig.java:103`
still names `/analyzer/astm` and `/analyzer/hl7` as Bridge endpoints; neither
exists. `/fhir/optimizeStorage` is shadowed by the HAPI servlet mapping.
`/OrderRequest*` is gone on #4657.

### F9. The consolidated-server link is one admin password in every direction

B1, B2, B4, A5. A lab pushes to the shared store with `fhirstore.username=admin`
and the default password; the Task pull writes status into the peer's store with
the same pair; the data export sends whatever headers were stored on the task;
the subscriber endpoint may be plain `http://` because the shipped
`common.properties:14` sets `allowHTTP=true`. The export can be fired by any
authenticated session through `POST /dataexport/fhir` or the DataExportStatus
trigger; neither controller checks a role. There is no identity per link, no TLS
requirement and no audit record on the path that carries the whole patient
record out of the lab. Breaks rules 3 and 4. Triage: none; the export was
outside the security export's scope. #4657: not in scope. Roadmap S8 (the
identity) and S11 (the door).

### F10. Reads of the store are open to every account

B6, B7. Any authenticated account reads every patient through the facade (#19)
and through `/rest/fhir` (#64), with no role, no audit and no `_count` cap. The
UI itself depends on `/rest/fhir/Questionnaire/{id}` for the Generic Sample
Order screens, so a role on the whole controller would break them; the facade
has no Questionnaire provider to move that read to. Rule 4. Roadmap S4 and the
facade review.

### Noted, no finding

- EQA reports (A7) are applied without a person. They are quality-assurance
  data, not patient results, so rule 2 does not apply; the only authorisation is
  the identifier system on the report, which is the EQA epic's to decide.
- The client registry search (A8) generates a national id when the registry has
  none (`PatientSearchRestController.java:200-206`). A person still picks the
  row. The generated id is a data-integrity question for the identity work, not
  this plan.

## Alignment with the security remediation

| Item                                               | Bridge 3.2.7 / 3.3.0                                | #4657                                                         | Still open                                              |
| -------------------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------- |
| Bridge identity and pairing                        | Own key, pairing by code, Basic refused once paired | Pairing entity, pinned client, cert chain on `/analyzer/fhir` | Shared keystore still mounted (F2); compose not updated |
| `/analyzer/fhir` role                              |                                                     | Bridge identity only                                          | Reviewer role must exclude the Bridge identity (F4)     |
| HL7 order servlets                                 |                                                     | Removed                                                       |                                                         |
| Analyzer setup to Global Admin, QC off import role |                                                     | Done                                                          | Import role still accepts staged rows (F4)              |
| Bounded decimals                                   |                                                     | Bundles, AST, calculations                                    | `/fhir/Observation` (F1)                                |
| Webapp `/fhir/*` facade writes                     |                                                     |                                                               | F1                                                      |
| Store reads (`/fhir/*`, `/rest/fhir`)              |                                                     |                                                               | F10                                                     |
| FHIR store exposure and trust                      |                                                     |                                                               | F2, F3                                                  |
| Consolidated-server link, export door              |                                                     |                                                               | F9                                                      |
| Microbiology path                                  |                                                     | kept separate                                                 | F5                                                      |
| Bridge delivery URL and health                     |                                                     |                                                               | F6                                                      |
| Dead paths                                         |                                                     |                                                               | F8                                                      |

## Target: named doors in, named doors out

```
IN                                                           OUT
instrument ─ASTM/HL7/FILE─▶ Bridge ─cert─▶ /analyzer/fhir ─▶ analyzer_results (staged)
                                                             │ reviewer accepts (human role)
partner store ─Task pull, OE's own identity─▶ orders and     │
                                   referral results (staged) │
                                                             ▼
                                        Result ─▶ validation ─▶ store (status-marked)
                                                                   │
                 consolidated server ◀─ export over TLS, one identity per link ─┤
                 FHIR Pipes          ◀─ mTLS, its own certificate ──────────────┤
                 integrations        ◀─ /fhir/* reads, role + scope, audited ───┘
```

- Machines reach `/analyzer/fhir` and nothing else, authenticated by the paired
  certificate. Microbiology instruments are Bridge profiles like any other;
  their results stage in `analyzer_results`.
- The facade does not write. Its reads sit behind a role and resource-type
  scopes and write an `AuditEvent` per call; `/rest/fhir` keeps the one read the
  UI needs and nothing else.
- OpenELIS is the only writer to the co-resident store, with its own client
  certificate; the store is bound to the Docker network, not the host.
- Every outbound link to a consolidated server or partner carries OpenELIS's own
  identity, pinned by the peer, over TLS; plain `http://` is refused.
- The Bridge takes one OpenELIS base URL, verifies that it is OpenELIS, and
  holds results until it is.

## Decisions and plan

Decisions and the ordered remediation live in the
[data exchange roadmap](../../specs/roadmaps/data-exchange-roadmap.md); this
audit is its evidence. The findings here reflect the decisions of 8 Oct (F7 in
particular); the roadmap's "What the investigations found" holds the research
behind them.

## Out of scope

Named above under Scope, with reasons. Also: search `_count` beyond the cap
(#84), XSS findings, and the client-registry identity question.
