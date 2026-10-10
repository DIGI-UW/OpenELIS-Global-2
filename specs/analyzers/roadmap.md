# Analyzer roadmap

Analyzer work after the
[baseline remediation](../roadmaps/analyzer-baseline-roadmap.md) (stack
#4588), in order. The [spec](spec.md) is the design authority; a decision
changes it in the PR that makes it.

How this list works:

- Each item is one piece of work. A piece that needs more than one PR or more
  than one repository has its own plan under `specs/roadmaps/`, with its scope
  fixed when the plan is written.
- Anything found while a piece is under way becomes a new item here, never a
  step added to the running plan.
- An item is removed when its work lands.

## In order

1. **The testing deployment's delivery check reaches an active GeneXpert.**
   One PR. The seed (`projects/analyzer-harness/seed-analyzers.sh`) checks
   only the assays that are on, as setup does; the smoke analyzer activates on
   the bundled catalog; a kept connection whose profile is gone is recreated.
   Rehearse against a local stack built like the deployment. From the
   baseline review (#4632, #4644).
2. **Upgrade rehearsal on the testing site.** An analyzer left by the baseline
   migration is set up again on a baseline type, its connection re-pinned, and
   it activates, recorded as the upgrade user story with the deployed build's
   SHA. The deployment needs the maintainer's go at the time.
3. **The Linux installer receives analyzer results.** One PR, a template and a
   doc. The installer mounts
   `install/installerTemplate/linux/templates/oe_server.xml` over Tomcat's
   `server.xml`, and its 8443 connector requests no client certificate, so
   every `/analyzer/fhir` delivery gets 401. Replace that connector with the
   `SSLHostConfig` form in `tomcat/oe_server.xml`
   (`certificateVerification="optional"`, `AnyClientCertificateTrustManager`),
   keeping the installer's keystore secret and `[% keystore_password %]` and
   dropping the truststore attributes; the trust manager already ships in the
   image. Delete the installer caveat at the end of
   [bridge-pairing.md](../../docs/analyzers/bridge-pairing.md). Verify on a
   rendered, booted install: a request with no certificate still succeeds, and
   a paired Bridge's delivery gets 2xx. The Bridge must reach 8443 directly;
   TLS ending at nginx would strip its certificate.
4. **Analyzer cleanup.** One PR.
   - The profile editor stops carrying hints: profiles carry no result value
     hints (rule 1), yet `ProfileTestDefinitions.jsx` (lines 86 to 95) still
     reads `result_value_hints` and writes them back when a test's values
     change. Set the values only.
   - `TestQcTarget.expectedDictResultId` holds a dictionary entry ID, as
     `TestResult.value` does for a type D answer, not a test result ID. Rename
     it `expectedDictionaryEntryId`.
   - Answer codes load in one query:
     `AnalyzerMappingCatalogServiceImpl.getActiveResultOptions` runs one
     terminology query per answer (`codingsOf`).
   - Affected analyzers load in one query:
     `AnalyzerTypeCatalogServiceImpl.affectedAnalyzer` loads each analyzer's
     latest mapping separately, for every revision in the catalog.
5. **Review rows are built in a service, in bulk.** One PR.
   `AnalyzerResultsController.analyzerResultsToAnalyzerResultItem` builds one
   review row at a time, each looking up its delivery receipt, placement and
   component label, and the paging helper builds rows for the whole queue.
   Move row building into a service that takes the list, resolves each lookup
   in one query, and builds only the page shown.
6. **Low findings from the baseline review** that the stack did not fix: each
   filed as an issue, linking its thread.
7. **Pairing proves the code without sending it.** A Bridge release, then the
   OpenELIS pin. Plan: [analyzer-pairing-proof](../roadmaps/analyzer-pairing-proof.md).
8. **The Bridge contract drops result value hints.** The profile schema and
   its README still define `result_value_hints`, the validator checks them and
   the catalog strips them on load. Reject them instead. Rides with the Bridge
   release of item 7.
9. **Codes settled at setup.** Bridge, mock and OpenELIS. Plan:
   [analyzer-codes-at-setup](../roadmaps/analyzer-codes-at-setup.md).
10. **Outbound orders.** OpenELIS sends no orders to analyzers. The user, 7
    Oct: "obviously we would use the translation for both ways in the
    future". After item 9, so an order carries the code the instrument was set
    up with.
11. **FILE and HL7 qualification.** Qualify, with native traffic, the FILE and
    HL7 instruments core ships beyond GeneXpert and FluoroCycler: exports,
    assays, units, status and control semantics, archive and error retention.
    HL7 result parts (OBX-4 sub-identity, OBX-5 components, OBX-8, NTE) land
    with the first HL7 baseline profile. Decide here which domains the
    harness's filesystem catalogs may replace: today they suppress the bundled
    Horiba CBC and vector CSVs. Acceptance: UI directory configuration reaches
    Bridge watching; native files and HL7 messages save correct clinical values
    with independent readback.
12. **Durable delivery.** Prove OpenELIS queue outage, restart and replay on
    the current traffic helper, and operator retry after another transient
    failure (with #4421). Restore mock attachment after a Bridge replacement.
    Acceptance: replay returns the original counts; a retried delivery adds no
    clinical result.
13. **Cepheid coverage in the Bridge.** Parser-level tests for 301-2002 Rev E
    §6.3.4.1.9 to 6.3.4.1.11 (assays outside the profile: multi-result,
    single-result, quantitative with LOG and C notes). MTB/RIF values and
    MTB/RIF Ultra verified from Cepheid LIS guidance, then added to the profile
    and the mock's fixtures.
14. **Core qualification.** Run every supported workflow and present
    recordings from the same registered tests. Acceptance: every supported
    ASTM, FILE and HL7 workflow and required recovery scenario has passing
    independent readback, exact image identities and accessible reviewed
    video.
15. **Madagascar distro.** Each distro profile written from its vendor
    host-interface document (openelis-work vendor manuals and integration
    specs first) with `docs/profiles/<id>.md` and its unverifiable rows marked;
    then the distro removes profiles core carries, unsets the shipped-pattern
    override, rebuilds the rest as baseline profiles, runs the migration and
    updates pins. Acceptance: the distro consumes working core profiles and
    defaults without site-specific mapping repair.
16. **Answer codes UI.** Dictionary Management edits an answer's codes in any
    system through `/rest/test-catalog/answers/{id}/terminology`, and the test
    catalog's option table shows each answer's codes read-only. Acceptance:
    saving keeps `dictionary.loinc_code` on the SAME_AS LOINC code.
17. **Analyzer Types under Admin.** The types page (`/analyzers/types`) moves
    to the Admin menu; analyzers stay in the Analyzers menu (decided 6 Oct).
18. **The Bridge and mock bump OpenELIS's pins.** When either default branch
    moves, a workflow opens the OpenELIS submodule bump as a PR (decided 7 Oct).

## Owned elsewhere

- **QC read permission.** Whoever owns QC roles decides whether QC reads are
  gated on `QaPermissions.VIEW_QC`, as the QC export is, instead of ADMIN, so
  Lab Supervisors keep the QC dashboard.

## Open findings

| ID     | Finding                                                                                                                                            | Next step                                                                                                                                   |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| F-REV2 | Transactional TRUNCATE can retain locks that block a second connection or `REQUIRES_NEW` work. Credible risk; no failing case demonstrated.        | Reproduce the cross-connection case; fix fixture isolation or transaction ownership so it finishes or fails diagnostically without hanging. |
| F-REV4 | Repeated `SpringContext.getBean` access and reference-table lookups add indirection and query work. No functional failure or measured problem.     | Prefer injected or context-scoped dependencies; verify lifecycle correctness and query reduction.                                           |
| F-REV5 | The 2.3.x seed assigns the COVID LOINC to HIV viral-load variants. The harness CSVs are corrected; the main dictionary and deployed sites are not. | Audit affected records on deployed sites; apply a narrowly scoped correction preserving IDs, history and report labels.                     |

## Open pull requests

| PR                                       | State                        | Next step                                                                                                  |
| ---------------------------------------- | ---------------------------- | ---------------------------------------------------------------------------------------------------------- |
| OpenELIS #4421                           | Open, behind develop (9 Oct) | Retry and Dismiss of undelivered results in the audit trail; item 12 builds on it.                         |
| OpenELIS #3974                           | Open, conflicting (9 Oct)    | TypeScript migration of analyzer forms whose targets were mostly removed in September: close or narrow it. |
| OpenELIS issue #4428                     | Open (9 Oct)                 | Profile editor acceptance and advanced settings.                                                           |
| Madagascar test harness #4, #9, #10, #11 | As of 1 Oct                  | With item 15: reconcile #4 with merged #15; narrow #10; review the outbound proof in #9 and #11.           |
| Review tooling #16, #31                  | As of 1 Oct                  | With item 14: compare #16 with merged #17; update #31's evidence manifests to the tested pins.             |

## Acceptance and test rules for this work

- Start the ordinary core application, catalog and Bridge-shipped profiles
  through `scripts/dev-stack`; use CI's supported runner for CI parity.
- Prepare patients, orders and specimens through validated REST APIs, and read
  persisted results back independently of the mapping the application chose.
- Exercise the visible UI for any setup, confirmation, correction, activation
  or acceptance a story claims.
- Native mock traffic crosses the real Bridge transport. No SQL fixture,
  mapping-repair script or replacement service manufactures acceptance.
- Retry and repeat prove no additional clinical result.
- CI and recordings execute the same registered scenarios. A passing old
  recording is not final-commit qualification.
- Track code, CI, merge, deployed build and human acceptance as separate
  states.
