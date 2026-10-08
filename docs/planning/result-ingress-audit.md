# Result ingress audit: one way in for results

Audited 8 Oct 2026 against OpenELIS `feat/analyzer-baseline-7-specs` at
`c52a9f6` (develop plus the analyzer stack), the security branch
`fix/analyzer-security-pairing` (#4657) at `2cfa806`, Bridge 3.3.0 (`1d97585`),
and the Claude Security export of 7 Oct (109 findings, cited as `#n`). Every
claim below was read in source; nothing was exercised at runtime.

## The two rules

Set by the owner on 8 Oct:

1. Microbiology analyzer traffic is not separate from the main analyzer path.
2. No path lets an unstaged result into OpenELIS or into the co-resident FHIR
   store. A result from a machine, a partner lab or a FHIR client is staged, and
   a person accepts it before it is a clinical result.

## What good looks like

- Instrument results pass a review step, human or rule-based, before release to
  the patient record (CLSI AUTO10-A, AUTO15). In OpenELIS that step is the
  Analyzer Results page (`analyzer_results`, then accept) and result validation.
- A FHIR server decides, per interaction, whether this client may create, update
  or delete this resource, including inside a transaction bundle, and
  authenticates clients by mutual TLS, keys or signed tokens (HL7 FHIR R4,
  Security). On HAPI that is an `AuthorizationInterceptor` with default deny.
- One machine, one identity, one door: a machine's credential reaches the one
  endpoint that stages its data and nothing else. The Bridge 3.2.7 pairing model
  is this: the Bridge's own certificate, pinned by OpenELIS, is its identity.
- A health check probes the path deliveries take. A green probe on another path
  hides a dead delivery path (the Nosy Be incident, Bridge #44).

## Inventory: every way a result or a FHIR resource gets in

| #   | Path                                                                                              | Meant for                                                                                                     | Auth today                                                                                                                   | Writes                                                                                                                                                                                                                                                                                                                                                                                   | Staged?                                           |
| --- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| 1   | `POST /analyzer/fhir`                                                                             | Analyzer Bridge                                                                                               | Basic chain, `authenticated()` only (`SecurityConfig.java:176`); the module interceptor then admits `is_admin=Y` users (#51) | `analyzer_results`, delivery receipts; QC control rows go straight to QC results                                                                                                                                                                                                                                                                                                         | Patient rows staged; control rows final           |
| 2   | `POST /rest/AnalyzerResults`                                                                      | Reviewer on the Analyzer Results page                                                                         | `ANALYSER_IMPORT` or `ADMIN` (`AnalyzerResultsController.java:87`)                                                           | Promotes staged rows to `Result`, analysis to TechnicalAcceptance                                                                                                                                                                                                                                                                                                                        | The accept step                                   |
| 3   | `/fhir/*` HAPI facade (`FhirRestfulServer`, 9 providers)                                          | External FHIR clients                                                                                         | Any authenticated account; no role anywhere under `fhir/`; the MVC module interceptor never runs for this servlet (#19)      | Observation create/update/delete writes `Result` and `Analysis` through `persistDataSet` and pushes to the FHIR store (`ObservationProvider.java:176-184`); DiagnosticReport delete cancels any analysis (`DiagnosticReportProvider.java:129`); Device create/update/delete writes `Analyzer` rows; ServiceRequest, Patient, Specimen (#69), Location, Organization, Practitioner writes | Final, no staging                                 |
| 4   | `POST /rest/analyzer/events/ast`, `/culture`                                                      | "Bridge-originated" per spec 782, but nothing in the Bridge or mock calls them; only a Playwright helper does | Own chain, Basic plus `ANALYSER_IMPORT` (`SecurityConfig.java:122,157`)                                                      | AST: `analyzer_event` row, then `micro_ast_reading` rows marked `ANALYZER_AUTO`, run to RESULTS_IN; a bench user must review the run. Culture: case stage to POSITIVE_SIGNAL, no review                                                                                                                                                                                                  | AST staged in its own table; culture not reviewed |
| 5   | Co-resident FHIR store `fhir.openelis.org:8443/fhir`                                              | OpenELIS itself, and the partner that pulls Tasks                                                             | mTLS only, no application auth (`hapi_application.yaml` has no interceptor); trusts the one certgen cert                     | Anything, directly into HAPI's tables                                                                                                                                                                                                                                                                                                                                                    | None                                              |
| 6   | Scheduled Task poll from the remote store                                                         | Partner lab or OpenMRS                                                                                        | n/a                                                                                                                          | Orders as `ElectronicOrder`; returned referral results mark the referral COMPLETED and wait for Accept (OGC-803); patient rows written directly; peer-chosen resource IDs overwrite local ones (#48)                                                                                                                                                                                     | Results staged; patients final                    |
| 7   | `PUT /rest/reference-lab-results/referrals/{id}/accept`                                           | Lab user                                                                                                      | RESULTS, VALIDATION or ADMIN                                                                                                 | `Result` rows, analysis straight to Finalized (`ReferenceLabResultsServiceImpl.java:79-104`)                                                                                                                                                                                                                                                                                             | Skips the validation queue                        |
| 8   | `/OrderRequest`, `/OrderRequest_Raw`                                                              | HL7 OML senders                                                                                               | Any authenticated account (#73, #81)                                                                                         | `ElectronicOrder` and patient rows                                                                                                                                                                                                                                                                                                                                                       | Removed on #4657                                  |
| 9   | `/pluginServlet/**`                                                                               | Plugins                                                                                                       | `permitAll` (`SecurityConfig.java:109`)                                                                                      | Nothing: no servlet is mapped to it                                                                                                                                                                                                                                                                                                                                                      | Dead, but open                                    |
| 10  | `/rest/fhir/replay`, `/dataexport/fhir`, `/rest/fhir/{type}/_search`, shipping `import-from-fhir` | Admin and UI                                                                                                  | ADMIN or session                                                                                                             | Replay pushes to the store; the rest read or write no results                                                                                                                                                                                                                                                                                                                            | n/a                                               |

Exposure: the shipped proxy forwards all of `/api/` to OpenELIS
(`volume/nginx/nginx-prod.conf:30`), and Tomcat serves the webapp at
`/api/OpenELIS-Global/` (`tomcat/oe_server.xml:141`), so rows 1, 3 and 4 are
reachable on the public 443 behind OpenELIS authentication alone. OpenELIS 8443,
the FHIR store's 8444 and the database's 15432 are also published on every
interface (`docker-compose.yml:50,81,23`).

## Findings

Ordered by distance from the two rules.

### F1. The webapp's FHIR facade is a second, unreviewed door for results

Row 3. `POST /fhir/Observation` from any account creates a `Result` and pushes
it to the FHIR store; `PUT` overwrites one; `DELETE` rejects it. No role, no
staging, no `AuthorizationInterceptor`, no bounded-decimal check (#102, and
#4657's `BoundedDecimal` does not reach this class). Device writes create and
delete analyzers outside analyzer setup. This is the path Bridge #44's reporter
hit by accident; it failed for them only because no transaction handler is
registered. Breaks rule 2. Triage #19, #69, #102. #4657: untouched.

### F2. The co-resident FHIR store trusts one shared key, and the Bridge holds it

Row 5. certgen makes one self-signed key (`frontend/Dockerfile.certs:31-41`); it
is the server identity of OpenELIS, the FHIR store and the Bridge, the only
entry in the store's truststore, and the volume is `chmod a+rwx`. The shipped
analyzer overlay mounts that volume into the Bridge
(`docker-compose.analyzers.yml:19`) and runs it on the `dev` profile (`:26`),
which loads the private key with `kspass`
(`bridge/src/main/resources/application-dev.yml:3-7`). The Bridge never calls
the store, but it holds the one credential the store accepts, on the same
network. `docs/directFHIRCommunication.md:65` shows writing to the store with
that key. The store has no application-level check. Breaks rule 2. Triage #7,
#9, #26, #42, #106. #4657: not in scope.

Two consequences for the pairing design: a fingerprint pinned to "the Bridge
certificate" also matches OpenELIS and the FHIR store while they share a key;
and Bridge 3.3.0 only generates its own identity when no keystore is mounted
(`BridgeIdentityEnvironment.java:25`), so the shipped overlay suppresses it.

### F3. The FHIR store holds every result with its validation state (no finding)

`transformPersistResultsEntryFhirObjects` runs at result entry
(`ResultEntryRestController.java:365`) and again at validation. The pushed
Observation's status is `final` only when the analysis is Finalized and
`preliminary` otherwise (`ObservationTransformServiceImpl.java:290-298`), and a
DiagnosticReport is written only when Finalized. That is the owner's intended
model (8 Oct: "the fhir store should hold the observation but have different
states based on if it's validated or not"). Nothing to change; consumers must
filter on status, which is theirs to do.

### F4. The Bridge's credential can accept the rows it staged

Rows 1 and 2. `/analyzer/fhir` has no role of its own, so the Bridge runs with
an account that is at least `ANALYSER_IMPORT` and in practice admin (#51,
default `admin`/`adminADMIN!` in the overlay). That same role accepts staged
rows (`/rest/AnalyzerResults` POST), retries and dismisses delivery issues. A
machine that can stage and accept is unstaged in effect. Rule 2. #4657 keeps
"the import role keeps staged results"; its uncommitted pairing chain gives the
Bridge `ROLE_ANALYZER_BRIDGE` on `/analyzer/fhir` only, which is the fix,
provided the human reviewer role and the Bridge identity never coincide.

### F5. Microbiology has its own door, its own role chain and no caller

Row 4. Spec 782 planned "Bridge-originated normalized analyzer events" on a JSON
envelope, but no Bridge profile produces them; the only caller is the Playwright
seed helper. AST events stage in `analyzer_event` and `micro_ast_reading`, with
their own review (the AST panel's "Accept results"), their own import-issue
surface and their own security chain; culture events change a case stage with no
review. The event carries fields the normalized bundle has no slot for: organism
(id, name, confidence), run-level QC and instrument QC reference, analyzer
message codes, software version, and S/I/R as a call. Rule 1 by design. No
triage finding; #4657 keeps it on Basic plus `ANALYSER_IMPORT` because "the
Bridge does not invoke it".

### F6. The Bridge's delivery and health are two settings that can disagree

`forward-http-server.uri` and `health-uri` are independent
(`HTTPForwardServerConfigurationProperties.java:19,35`); a URI that does not end
in `/analyzer` sends every result to the HAPI base URL (HAPI-0287) while health
probes `/health` and stays green. Pairing does not carry the delivery URL
(`PairingState.java:43`). The shipped overlay also sets `INSECURE_TLS=true` and
disables hostname verification (`docker-compose.analyzers.yml:40-41`). Bridge
#44; triage #1 for the OpenELIS side (`BridgeHttpClient` trusts every
certificate). #4657's uncommitted `BridgeHttpClient` pins the Bridge; the Bridge
side is open.

### F7. Secondary paths that finalize or cancel without the queue

Row 7 finalizes a referral's analysis on Accept, skipping validation.
`DELETE /fhir/DiagnosticReport` cancels an analysis in any state. The remote
Task poll writes patient rows and peer-chosen resource IDs directly (#48).

### F8. Dead and stale surface

`/pluginServlet/**` is open with nothing behind it. `SecurityConfig.java:103`
still names `/analyzer/astm` and `/analyzer/hl7` as Bridge endpoints; neither
exists. `/fhir/optimizeStorage` is shadowed by the HAPI servlet mapping.
`/OrderRequest*` is gone on #4657 (good).

## Alignment with the security remediation

| Item                                               | Bridge 3.2.7 / 3.3.0                                | #4657 committed            | #4657 uncommitted                                             | Still open                                              |
| -------------------------------------------------- | --------------------------------------------------- | -------------------------- | ------------------------------------------------------------- | ------------------------------------------------------- |
| Bridge identity and pairing                        | Own key, pairing by code, Basic refused once paired |                            | Pairing entity, pinned client, cert chain on `/analyzer/fhir` | Shared keystore still mounted (F2); compose not updated |
| `/analyzer/fhir` role                              |                                                     |                            | `ROLE_ANALYZER_BRIDGE`                                        | Reviewer role must exclude the Bridge identity (F4)     |
| HL7 order servlets                                 |                                                     | Removed                    |                                                               |                                                         |
| Analyzer setup to Global Admin, QC off import role |                                                     | Done                       |                                                               | Import role still accepts staged rows (F4)              |
| Bounded decimals                                   |                                                     | Bundles, AST, calculations |                                                               | `/fhir/Observation` (F1)                                |
| Webapp `/fhir/*` facade writes                     |                                                     |                            |                                                               | F1                                                      |
| FHIR store exposure and trust                      |                                                     |                            |                                                               | F2, F3                                                  |
| Microbiology path                                  |                                                     | kept separate              |                                                               | F5                                                      |
| Bridge delivery URL and health                     |                                                     |                            |                                                               | F6                                                      |
| Dead paths                                         |                                                     |                            |                                                               | F8                                                      |

## Target: one door

```
instrument ──ASTM/HL7/FILE──▶ Bridge ──cert──▶ POST /analyzer/fhir ──▶ analyzer_results (staged)
                                                     ▲                          │
                                        the only machine path          reviewer accepts (human role,
                                                                       never the Bridge identity)
                                                                                │
                                                                                ▼
                                                                      Result → validation → FHIR store
```

- Machines reach `/analyzer/fhir` and nothing else, authenticated by the paired
  certificate. Microbiology instruments are Bridge profiles like any other;
  their AST and culture results arrive as normalized bundles and stage in
  `analyzer_results`, from where the existing AST run review accepts them.
- The webapp's FHIR facade does not write results. Its Observation,
  DiagnosticReport, ServiceRequest, Specimen and Device writes go; reads stay
  behind a role.
- OpenELIS is the only writer to the co-resident store, with its own client
  certificate; the store is bound to the Docker network, not the host.
- The Bridge takes one OpenELIS base URL and derives both the delivery path and
  the health probe from it.

## Decisions and plan

Decisions and the ordered remediation live in the
[result ingress roadmap](../../specs/roadmaps/result-ingress-roadmap.md); this
audit is its evidence and is not updated as the plan moves.

## Out of scope

`/rest/fhir` read exposure (#64), search `_count` (#84), XSS findings, and the
remote-source FHIR import (#48) beyond the ID note in F7.
