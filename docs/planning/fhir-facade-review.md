# The native FHIR facade: a read-only, scoped endpoint over OpenELIS data

Companion to [data-exchange-audit.md](data-exchange-audit.md) (audit rows A3, B6
and B7; findings F1 and F10). Reviewed 8 Oct 2026 at
`feat/analyzer-baseline-7-specs` (`0211601`). Every claim was read in source;
nothing was exercised at runtime.

## Purpose, as set

The owner, 8 Oct: the facade exists "to provide a FHIR endpoint for OpenELIS
data for various general integrations...but it needs to be secure and scoped",
and "should not allow external writes for now". Data comes into OpenELIS on one
path per kind of data; the facade is where data goes out.

## What it is today

`FhirRestfulServer` (HAPI `RestfulServer`) is registered at `/fhir/*` beside,
not inside, the Spring MVC dispatcher
(`AnnotationWebAppInitializer.java:37-40`). It registers every
`IResourceProvider` bean and one interceptor,
`StrictSearchParameterInterceptor`, which rejects unknown search parameters and
nothing else (`FhirRestfulServer.java:28-34`).

| Provider         | Read and search parameters                                                                   | Writes today           | What a write touches                                                                                                                            |
| ---------------- | -------------------------------------------------------------------------------------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Patient          | id, identifier, name, given, family, birthdate, gender, address parts, telecom, email, phone | create, update, delete | `Patient`, `Person`                                                                                                                             |
| Practitioner     | id, identifier, name parts, address parts, telecom                                           | create, update, delete | `Provider`, `Person`                                                                                                                            |
| Organization     | id, identifier, name, active, type, partOf, address                                          | create, update, delete | `Organization`                                                                                                                                  |
| Location         | id, identifier, name, status, partOf, \_tag                                                  | create, update, delete | storage entities                                                                                                                                |
| ServiceRequest   | id, identifier, patient, subject, requester, specimen, code, status                          | create, update, delete | `Sample`, `SampleItem`, `Analysis`; delete cancels the analysis                                                                                 |
| Specimen         | id, identifier, accession, patient, subject, type, status, collected                         | create, update, delete | `SampleItem`; update re-links it to any accession named (#69)                                                                                   |
| Observation      | id, identifier, patient, subject, basedOn, specimen, code, status, date                      | create, update, delete | `Result` and `Analysis` through `persistDataSet`, then a push to the FHIR store (`ObservationProvider.java:176-184`); delete rejects the result |
| DiagnosticReport | id, identifier, patient, subject, basedOn, result, specimen, code, status, issued            | delete                 | cancels the analysis in any state (`DiagnosticReportProvider.java:129`)                                                                         |
| Device           | id, identifier, device-name, type, status                                                    | create, update, delete | `Analyzer` rows (delete deactivates)                                                                                                            |

No transaction, batch or `$operation` endpoint is registered.

### Who may call it

- Any authenticated account. Basic requests fall into the general Basic chain
  (`SecurityConfig.java:176`, `authenticated()`), session requests into the
  default chain (`:455`). There is no role check anywhere under
  `src/main/java/org/openelisglobal/fhir/`.
- `ModuleAuthenticationInterceptor` is an MVC interceptor
  (`AppConfig.java:99-103`); the facade is a separate servlet, so no module or
  lab-unit rule applies. Triage #19 rates this 8.7: any account can read every
  patient and write or overwrite results on any analysis.
- The shipped proxy forwards `/api/` to OpenELIS, so the facade is on the public
  443 at `/api/OpenELIS-Global/fhir/*`.
- No audit record is written for facade access; `LogEvent` entries exist only on
  some write paths.
- Search `_count` is uncapped (#84).

### Who uses it

No caller in this repository: not the frontend, not the Bridge, not the mock,
not the harness, not a spec. Partners and the consolidated server read the
co-resident store, not the facade. From the distro repositories and Slack
(roadmap, "What the investigations found"): no deployment configures a facade
client; eSIL (Madagascar) reads ServiceRequest and Practitioner through it with
a Basic-auth account, having been pointed at it on 3 Mar and 3 Apr 2026; no
write through it is evidenced anywhere; Bridge #44's reporter hit it by mistake.
That is the whole known readership.

The facade's history, from GitHub: #2723 (19 Feb 2026) registered the servlet;
#2922 (26 Feb) added the Observation provider for "real-time interoperability
with systems like OpenMRS"; #3555 (7 Sep) added the DiagnosticReport provider
"enabling OpenELIS to process and persist incoming FHIR lab results". It was
built as a two-way door. The owner's decision (no writes) reverses that intent,
and the S4 PR says so to the authors.

## What good looks like

- A FHIR server authenticates every client and decides, per interaction, whether
  this client may read, search, create, update or delete this resource (HL7 FHIR
  R4 Security). On HAPI that is `AuthorizationInterceptor` with a default of
  deny, rules built per request from the caller's identity.
- Scope, not just role: SMART's `patient/*.read` and `user/*.read` model says a
  client's reach is a set of resource types and compartments, named at
  registration. For a lab the natural scopes are resource type (results, orders,
  patients, reference data) and lab unit.
- Every access is audited: FHIR's `AuditEvent`, or at least a log line with
  client, resource, interaction and outcome (FHIR R4 Security; US Core requires
  audit logs of all transactions).
- Data in and data out are different doors. Instrument results enter through
  `/analyzer/fhir` and are staged; orders and referral results enter through the
  Task pull and are staged; nothing enters through the facade.

## The boundary

| Direction                    | Path                                      | Identity                                       | Lands as                                                      |
| ---------------------------- | ----------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------- |
| In: instrument results       | `POST /analyzer/fhir`                     | the paired Bridge's certificate (#4657)        | staged `analyzer_results`, accepted by a reviewer             |
| In: orders, referral results | scheduled Task pull from the remote store | OpenELIS's own client identity to the peer     | `ElectronicOrder`; referral results wait for Accept (OGC-803) |
| In: operator-entered data    | the UI's REST controllers                 | user session, module interceptor               | ordinary entry and validation                                 |
| Out: anything                | `/fhir/*` facade, read and search only    | a named integration identity with a read scope | nothing                                                       |
| Out, internal                | the co-resident store                     | OpenELIS's client certificate                  | OpenELIS's own mirror, status-marked                          |

The facade is the only "out" door a third party gets, so its reach is what the
lab decides to publish, and its clients are known by name.

## Design

1. **No writes.** Delete every `@Create`, `@Update` and `@Delete` on the nine
   providers; keep `@Read` and `@Search`. A `POST` or `PUT` then gets
   HAPI's 405. Observation, DiagnosticReport, ServiceRequest, Specimen and
   Device writes lose their only external path; Patient, Practitioner,
   Organization and Location writes have UI paths already.
2. **A named read scope.** A new role, `FHIR_READ`, held by integration accounts
   only, required by the chain that matches `/fhir/**`. The facade sits outside
   the MVC interceptor, so the rule lives in Spring Security's matcher, not in a
   module row.
3. **Authorization in the server.** Register an `AuthorizationInterceptor` whose
   `buildRuleList` allows read and search for the resource types the caller's
   scope names and denies everything else by default. First scopes: `results`
   (Observation, DiagnosticReport, Specimen, ServiceRequest), `patients`
   (Patient), `reference` (Practitioner, Organization, Location, Device).
   Lab-unit scoping follows once a client needs it; the search providers already
   filter on patient and specimen references.
4. **Audit every call.** A server interceptor on
   `SERVER_INCOMING_REQUEST_POST_PROCESSED` and `SERVER_OUTGOING_RESPONSE`
   writes one `AuditEvent` resource to the co-resident store per call, with
   client, resource type, interaction, query and outcome, and the same line to
   the application log. The owner's decision is "AuditEvent resources now", so
   the resource is part of S4, not a follow-up.
5. **Bounds.** Cap `_count` (#84) and page size in the bundle providers; keep
   `StrictSearchParameterInterceptor`.
6. **Identity.** Integration accounts authenticate with Basic over TLS until the
   own-identity pattern from the ingress audit (per-service key, pinned by
   OpenELIS) covers facade clients too; the role and interceptor do not change
   when the credential does.
7. **Narrow `/rest/fhir`.** `FhirQueryRestController` is a second read door to
   the co-resident store for any authenticated user (#64). The UI depends on one
   read through it: the Generic Sample Order screens fetch
   `/rest/fhir/Questionnaire/{id}` (`GenericSampleOrder.jsx:170`,
   `GenericSampleOrderEdit.jsx:169`), and the facade has no Questionnaire
   provider to move that to. So: the Questionnaire read stays open to any
   session user; every other resource type on `/rest/fhir` requires `FHIR_READ`,
   matched in Spring Security on the path. Moving questionnaires behind a facade
   provider is a follow-up, not a condition of S4.

## Tests that prove it

- An account without `FHIR_READ` gets 403 on `GET /fhir/Patient`; with the role
  it gets 200.
- `POST /fhir/Observation` and `PUT /fhir/Specimen/{id}` get 405 for every
  account, admin included; `DELETE /fhir/DiagnosticReport/{id}` likewise.
- A `results`-scoped client gets 403 on `GET /fhir/Patient`, 200 on
  `GET /fhir/Observation?patient=…`.
- Each call writes one `AuditEvent` to the store naming the client, the resource
  type, the interaction and the outcome; the test reads it back.
- A session user without `FHIR_READ` gets 200 on
  `GET /rest/fhir/Questionnaire/{id}` and 403 on `GET /rest/fhir/Patient`.
- Inversion: removing the interceptor registration makes the scope test fail.

## What the distros answered

Investigated non-interactively from the deployment repositories and Slack
(roadmap, S4a), as the owner asked:

1. No deployment writes to `/OpenELIS-Global/fhir/*`. Nothing has to move.
2. One reader: eSIL (Madagascar), ServiceRequest and Practitioner, with a
   Basic-auth account. That account becomes the first `FHIR_READ` holder, scope
   `results` plus `reference`.
3. Every distro's nginx forwards `/api/` to OpenELIS on 443, so the facade is
   reachable from outside the lab network in every deployment, behind OpenELIS
   authentication alone.

## Plan

Step S4 of the
[data exchange roadmap](../../specs/roadmaps/data-exchange-roadmap.md): one PR
on develop after #4657 lands, with write removal, `FHIR_READ`, the authorization
interceptor, the `AuditEvent` interceptor, the `_count` cap, the narrowed
`/rest/fhir`, and the tests above. Scope by lab unit and a Questionnaire
provider are follow-ups once a client asks for them.
