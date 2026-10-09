# Data exchange roadmap

Named doors in, named doors out. Data enters OpenELIS on one path per kind
of data and every result is staged before a person accepts it; data leaves
as FHIR only through a door with a known peer, OpenELIS's own identity on the
link, and a record of what was sent.

Evidence: [data-exchange-audit.md](../../docs/planning/data-exchange-audit.md)
(inventories A and B, findings F1 to F10) and
[fhir-facade-review.md](../../docs/planning/fhir-facade-review.md). The
analyzer baseline roadmap's Rules and Repo working agreements apply here too
(`specs/roadmaps/analyzer-baseline-roadmap.md`, which lands on develop with
stack #4588).

Scope, set 8 Oct: "operational ingress, and FHIR-based outgress for now",
without the Generic Sample Order CSV import. Setup imports and the non-FHIR
senders are named out of scope in the audit, with the reason.

This work is its own remediation, separate from the analyzer stack #4588:
each step is a PR against `develop`, after the stack and #4657 have landed
where a step depends on them. The one exception is S1, which is the stack's
#4657.

## Rules

1. Microbiology analyzer traffic is not separate from the main analyzer path.
2. No path lets an unstaged result into OpenELIS or into the co-resident
   FHIR store. A result from a machine, a partner lab or a FHIR client is
   staged, and a person accepts it before it is a clinical result.
3. One identity per service, no shared private keys; trust is an explicit pin
   of the peer's certificate (the Bridge pairing model). The co-resident
   store is the accepted exception (decision 8 Oct).
4. Data leaves only through a named door: a known peer, OpenELIS's own
   identity on the link, status-marked resources, and a record of what was
   sent. The `/fhir/*` facade only reads.

Standards these rules answer to: CLSI AUTO10-A and AUTO15 (instrument
results are reviewed before release); HL7 FHIR R4 Security (authenticate
clients, decide each create, update and delete per interaction, audit); HAPI
`AuthorizationInterceptor` with default deny.

## Decisions (the owner's words)

8 Oct 2026:

- Scope: "we are not auditing just results ingress, but FHIR-based and/or
  data coming in to OE2 any way, and also outgress too"; then "I would rather
  focus on operational ingress, and FHIR-based outgress for now", and "drop
  the CSV order import".
- Co-resident store: "should stay as-is: it's role is basically a synced
  internal fhir representation, even if the boundary is a bit fuzzy today".
  The store holds every Observation "but have different states based on if
  it's validated or not"; it already does (`ObservationTransformServiceImpl`).
- Remote peers: own identity "is the right one", but "I don't want more
  friction/blocking for dev-based setup, so it should be clear how to get
  things set up for dev vs. prod", and "hesitant about retiring the certgen
  service without some really good thought/planning".
- Facade: "should not allow external writes for now"; its goal "is to provide
  a FHIR endpoint for OpenELIS data for various general integrations...but
  it needs to be secure and scoped".
- Microbiology: no separate pathway ("ridiculous"); "once they're verified,
  they should go into the microbio workflows"; micro V2 is "a great place for
  it"; `/rest/analyzer/events`: "Delete the endpoint now". The AST panel's
  "Accept results" reads the staged rows, so analyzer and manual readings
  enter the run at the same point.
- Security branch #4657: "i want the 4657 to land on top of the stack"; it is
  PR 24 of stack #4588, pinned to Bridge 3.3.0.
- The small SecurityConfig and overlay items: "I do them after #4657 merges".
- Liquibase: the specimen-id changeset is renumbered 120 to 132.
- The facade review is written now, as a document; no code before it is read.
- F1, the facade's writes: "this touches on our messy approach to fhir right
  now. We should have one translation interface that goes OE2 datamodel <>
  FHIR, and this should be used universally, but external
  creates/updates/writes etc. should be gated and for now likely removed
  except when going through the proper entrypoints". So: every facade write
  goes; one translation layer, used by the facade's reads, the store push,
  the Task pull and `/analyzer/fhir` alike (S10).
- F2, the Bridge's trust: "Truststore-only volume".
- F4, the reviewer role: "Keep the role, fix the name and docs".
- F6, Bridge 3.3.1: "Now, old value refused"; then, asked whether that
  meant refusing to start: "idk why it's specific to one legacy line - the
  bridge should check it targeting the right endpoint, and start but show
  'Down' otherwise". So: no check for the legacy string; the Bridge probes
  `{uri}/health` and requires OpenELIS's answer, starts either way, holds
  every result and reports forwarding health DOWN naming the probed URL
  until the check passes, then delivers.
- S10, the translators: "I want an audit, along with when and why they were
  implemented. this duplication is a big issues. GSoC this summer was working
  on the fhir facade, so i want an updated view of what's up".
- Facade scope: "Role plus resource-type scopes".
- Facade audit trail: "AuditEvent resources now".
- F7, the secondary paths: "i need reasons for why it's designed this way
  today (from docs, commit history or w/e) and some research on what the
  right appraoch most likely is" before deciding.
- S8 sources: "Distro repositories" and "Slack". The facade review's
  questions for the distros: "i want you to do a non-interactive
  investigation"; answered from the distro repositories and Slack, not by
  asking the team.
- S8, the remote-peer pattern, after the inventory: "OpenELIS's own identity,
  pinned by the peer". OpenELIS presents its own client certificate to a
  shared store; a consolidated puller presents its own to the lab's store,
  which trusts only that one. certgen stays and writes the peer's trusted
  certificate where the operator puts it. Dev: nothing changes. Prod: one
  documented step per peer link. Basic remains the bootstrap for a peer that
  cannot pin.
- F7, after S9a: "Keep reception; IDs with data exchange". Referral Accept
  stays the OGC-803 reception model and is not a finding; DiagnosticReport
  delete leaves with S4; the Task import moves to local ids with the remote
  id as an identifier in the data-exchange hardening.
- S10, after S10a: "Gate the entry points, reuse the translators". The
  orchestrator keeps event sequencing and persistence; the facade's HTTP
  writes go (S4); the FHIR-to-OpenELIS translation stays in the per-resource
  services and the proper inbound doors use it.

## Open decisions

- Rule 4's wording is this rewrite's; the owner set the inbound rules and
  the facade's read-only decision and has not confirmed the egress sentence.
- S11, the export door (F9): whether `fhir.subscriber.allowHTTP` is removed
  or only defaults to `false`, and which role fires an export by hand. Both
  are small; they are asked when S11 starts.

## Steps

Status: `[ ]` open, `[~]` in progress, `[x]` done. Owner in brackets.

```
- [~] S1 Pairing and the Bridge's own door [security thread, #4657 on stack #4588]: /analyzer/fhir on its own chain for the paired certificate only; the Bridge has no user account (closes F4 in effect); overlay without passwords, insecure TLS or the dev profile; HL7 order servlets removed; analyzer setup to Global Admin; bounded decimals; Bridge 3.3.0
- [ ] S2 The micro endpoint goes [this session, after S1]: delete /rest/analyzer/events/*, its chain, controllers, services, the analyzer_event table, the seed helper's posts and the analyzer half of the harness AST story (F5)
- [ ] S3 Dead and stale surface, and the reviewer role's name [this session, with S2]: /pluginServlet/** out of OPEN_PAGES; the stale Bridge-endpoints comment; ANALYSER_IMPORT shown as "Analyzer Results Reviewer" and documented as a human role no machine holds (F4, F8)
- [x] S4a The facade's callers, from the distro repositories and Slack [research; see "What the investigations found"]: who writes to /OpenELIS-Global/fhir/*, who reads it and with which account, and where it is reachable from outside
- [ ] S4 The facade and the store's read doors [this session, after S4a]: every create, update and delete on all nine providers removed; FHIR_READ with per-account resource-type scopes (results, patients, reference) turned into allow rules by an AuthorizationInterceptor with default deny; an AuditEvent written to the co-resident store for every facade call, with its test; _count cap; /rest/fhir behind the same role except the Questionnaire read the Generic Sample Order screens need, which stays open to a session user (F1, F10)
- [ ] S5 No shared key in the Bridge [this session, after S1]: certgen writes the cert-only truststore to a second volume; the Bridge mounts only that; the keystore volume stays with OpenELIS and the store; dev and prod the same; certgen stays (F2)
- [~] S6 Bridge 3.3.1, one base URL [this session, Bridge, on fix/one-openelis-base-url]: forward-http-server.uri is the OpenELIS base, /analyzer/fhir and /health derive from it; health-uri is no longer read; the Bridge probes {uri}/health and requires OpenELIS's answer, starts either way, holds every result and reports forwarding health DOWN naming the probed URL until the check passes; no check for the legacy string; closes Bridge #44 (F6)
- [x] S10a The translators, audited [see "What the investigations found"]
- [ ] S10 One translation each way [this session, after S4, one resource per PR with a round-trip test]: the per-resource services under fhir/service are the layer for both directions; the orchestrator keeps event sequencing and persistence; referral Accept maps Observation to Result through ObservationTransformService instead of its own reads, and the Task pull builds orders through the ServiceRequest, Patient and Specimen services instead of FhirApiWorkFlowServiceImpl's own code; EQA's exchange code follows when its epic allows; the Bridge bundle parser stays as the ingress validator; writes happen only behind gated entry points (F1)
- [ ] S7 Micro V2 landing [micro V2, OGC-1383]: an accepted AST row lands on the AST run; the AST panel's "Accept results" reads analyzer_results (F5)
- [~] S8 Remote peers [inventory done; pattern decided: OpenELIS's own identity, pinned by the peer]: the lab-to-shared-store push and the consolidated-server pull each carry one identity; certgen writes the peer's trusted certificate; dev unchanged, prod one documented step per link; Basic stays the bootstrap for a peer that cannot pin (F2, F9)
- [x] S9a Why the secondary paths are built as they are [see "What the investigations found"]
- [ ] S9 Secondary [after S9a]: the Task import keeps local ids with the remote id as an identifier, in the data-exchange hardening (F7)
- [ ] S11 The export door [this session, after S8]: the subscriber endpoint is TLS only (allowHTTP off by default, or removed); the data export carries the S8 identity instead of stored headers; POST /dataexport/fhir and the DataExportStatus trigger require a named role; one AuditEvent per export attempt (F9)
```

Not changed, by decision: the co-resident store's shared-key mTLS; the
status-marked push of Observations at result entry; referral Accept as the
reception step.

## What the investigations found

S4a and S8, 8 Oct, from the ten DIGI-UW deployment repositories
(openelis-docker, openelis-docker-bundled, the distro template, PNG,
Madagascar, Indonesia, Ethiopia, the two harness repos, deploymentscript)
and Slack:

- **Facade callers.** No deployment configures a facade client. One
  integration was pointed at it: in `#ext-madagascar-e-sil` (3 Mar 2026) a
  DIGI developer told the eSIL integrators the facade was being built "without
  needing to acces the FHIR server directly", to use
  `https://<domain>/api/OpenELIS-Global/fhir/` ("work in progress", check
  `/metadata`), and on 3 Apr to try `/fhir/ServiceRequest` "with basic auth".
  So eSIL (Madagascar) reads ServiceRequest and Practitioner through the
  facade with a Basic-auth account; no write through it is evidenced
  anywhere. Bridge #44's reporter hit it by mistake. That is the whole known
  readership.
- **Exposure.** Every distro's nginx forwards `/api/` to OpenELIS on 443, so
  the facade, `/analyzer/fhir` and `/rest/analyzer/events` are public behind
  OpenELIS authentication in every deployment. Every distro publishes the
  co-resident store's 8444 (and 8081) on all interfaces; openelis-docker,
  openelis-docker-bundled, Indonesia and Ethiopia publish the database's
  15432 too. Madagascar and Indonesia add a public `bridge.*` vhost.
- **Remote peers.** No deployment sets `remote.source.uri` or
  `crserver.uri`; the Task pull and the client registry are unused in the
  field. Every distro sets `fhirstore.username=admin` and the default
  password, which OpenELIS only sends to a store that is not its local one.
  The "consolidated server" (Madagascar eSIL, CDI hub.openelisci.org, Haiti)
  is a separate FHIR store fed one of two ways, per
  `DIGI-UW/openelis-consolidated-server`: labs point `fhirstore.uri` at the
  shared store and push (Basic, admin credentials), or FHIR Pipes pulls from
  each lab's co-resident store on 8444 (mTLS with the one certgen key; "we
  are using the same cert for both the web app and hapi fhir", 25 Feb 2026).
  PNG's README lists 8444 as "Local only, or the consolidated server if
  connected". Indonesia still runs Bridge 3.0.4 with Basic forwarding;
  Madagascar pins 3.2.6 by digest; PNG tracks `:develop`.

So the remote-peer decision (S8) is about two links: a lab pushing to a
shared store, and a consolidated server pulling from a lab's store. Neither
uses the Task pull. The subscription and data export path (audit B2) is a
third link to the same server and was not in the first inventory; whether
any distro sets `fhir.subscriber` is still to be read from the repositories
when S11 starts.

S10a, the translators, from GitHub's history (the local clone is shallow at
8 Oct's develop, so `git log` here answers nothing older):

- **One layer already, not two.** #3595 (Isabirye1515, 8 Sep 2026) split the
  2,000-line `FhirTransformServiceImpl` into an orchestrator that delegates
  to per-resource services under `fhir/service` (Patient, Observation,
  ServiceRequest, Specimen, DiagnosticReport, Practitioner, Organization,
  Task, Common); the store push calls them 49 times. The facade's providers
  use the same services. The first version of the audit said the facade
  translated twice; that was wrong. What sits outside the layer is 22
  transform methods still local to the orchestrator, the EQA exchange code
  (#4140, Aug 2026), the referral FHIR code (2021 onward), and the facade's
  inbound write paths. The Bridge bundle parser
  (`AnalyzerNormalizedResultContract`, Sep 2026) validates a contract; it
  is not a model translator.
- **The facade's history and intent.** #2723 (Isabirye1515, 19 Feb 2026,
  "Internal Fhir implementation") registered the HAPI servlet. #2922
  (nigar-08, 26 Feb 2026) added the Observation provider "enabling real-time
  interoperability with systems like OpenMRS without external sync scripts".
  #3595 (Sep) was "a prerequisite for implementing a FHIR facade search";
  a follow-up (Mutesasira Moses, 8 Sep) made every search answer from the
  database. #3555 (Isabirye1515, 7 Sep) added the DiagnosticReport provider
  "enabling OpenELIS to process and persist incoming FHIR lab results using
  the existing result workflow": the facade was built as a two-way door,
  results in as well as out. #4648 (mozzy11, merged 8 Oct 2026) reworked the
  facade's update path so an unchanged resource sent back saves nothing. The
  Feb-to-Sep authors are the summer's GSoC work; the maintainer's #4648 is
  this week. The owner's decision (no writes through the facade) reverses
  the two-way intent, and S4 has to say so to the authors.

S9a, why the secondary paths are built as they are:

- **Referral Accept finalizes directly.** Came to develop in the
  demo-silnas sync (#3946, 21 Aug 2026). The reason is recorded in OGC-803:
  "Reception (NOT revalidation) … Set Analysis status to validated
  (released) … The reference lab already validated; no local revalidation
  step." The result is staged until a person accepts it (rule 2 holds); the
  skipped step is deliberate. Decision: keep it, named as the reception
  model in the audit, not a finding.
- **DiagnosticReport delete cancels any analysis.** From #3555 (7 Sep 2026),
  part of the facade's two-way design. Goes with S4's write removal; nothing
  separate.
- **The Task import writes peer-chosen IDs.** From the 2021 OE-to-OE
  referral design (CalebSLane, "add organization import and oe to oe
  referral", 13 Jan 2021), which shares one logical id between the two labs
  so each can find the other's Task. FHIR R4 says a logical id is the
  storing server's and "might or might not" survive a copy, and that
  identity across systems belongs in `identifier`
  (https://hl7.org/FHIR/r4/resource.html). The code already builds a
  remote-resource identifier (`createIdentifierToRemoteResource`). Decision:
  local ids, the remote id kept as that identifier; no deployment uses the
  Task pull today (S8), so this waits for the data-exchange hardening (S9).

## Done when

1. Every machine result reaches OpenELIS through `/analyzer/fhir` and is
   staged; `/rest/analyzer/events/*` and the facade's writes are gone.
2. The Bridge holds no OpenELIS private key; its only credential is its own
   certificate, pinned at pairing.
3. The facade is read-only, scoped by role, audited with an AuditEvent per
   call, and documented as the one "out" door for integrations; `/rest/fhir`
   serves the UI's Questionnaire read and nothing else without the role.
4. One OpenELIS base URL configures Bridge delivery and health, and the
   Bridge holds results until it has verified that URL is OpenELIS.
5. Every link to a consolidated server or partner (push, pull, subscription,
   export) carries OpenELIS's own identity over TLS, and the pattern is
   written down for dev and prod.
