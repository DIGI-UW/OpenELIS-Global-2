# Analyzer work after the baseline remediation

Analyzer work that is not part of the
[analyzer baseline remediation](../roadmaps/analyzer-baseline-roadmap.md). It
keeps the unfinished items of the roadmap that preceded the remediation
(`specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md`, in git history), each
with its verdict as of 6 October 2026. The remediation's packaging describes
the distro follow-on.

## After the baseline merges

Follow-ups from the baseline stack's reviews, decided on 9 October 2026 to land
after the stack merges. None changes a rule in the [spec](spec.md). The larger
work under "Remaining work" follows; codes settled at setup is its item 6.

OpenELIS only, in order:

1. **The testing deployment's delivery check reaches an active GeneXpert.** The
   seed checks only the assays that are on, as setup does (the #4644 review);
   the smoke analyzer activates on the bundled catalog; a kept connection whose
   profile is gone is recreated. Rehearse against a local stack built like the
   deployment.
2. **Upgrade rehearsal on the testing site**, recorded as the upgrade user
   story, once develop has the baseline. The deployment needs the maintainer's
   go at the time.
3. **The Linux installer receives analyzer results.** The installer mounts
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
4. **The profile editor stops carrying hints.** Profiles carry no result value
   hints (rule 1), yet `ProfileTestDefinitions.jsx` (lines 86 to 95) still
   reads `result_value_hints` and writes them back when a test's values change.
   Set the values only.
5. **QC targets name what they hold.** `TestQcTarget.expectedDictResultId`
   holds a dictionary entry ID, as `TestResult.value` does for a type D answer,
   not a test result ID. Rename it `expectedDictionaryEntryId`.
6. **Review rows are built in a service, in bulk.**
   `AnalyzerResultsController.analyzerResultsToAnalyzerResultItem` builds one
   review row at a time, each looking up its delivery receipt, placement and
   component label, and the paging helper builds rows for the whole queue. Move
   row building into a service that takes the list, resolves each lookup in one
   query, and builds only the page shown.
7. **Answer codes load in one query.**
   `AnalyzerMappingCatalogServiceImpl.getActiveResultOptions` runs one
   terminology query per answer (`codingsOf`). Fetch a test's answer codes with
   one DAO call.
8. **Affected analyzers load in one query.**
   `AnalyzerTypeCatalogServiceImpl.affectedAnalyzer` loads each analyzer's
   latest mapping separately, for every revision in the catalog. Fetch the
   latest mappings in one query.
9. **Low review findings** in files the fix PRs did not touch are filed as
   issues, each linking its thread.

A Bridge release first, then the OpenELIS pin:

10. **Pairing proves the code without sending it.** The first pairing trusts
    whichever certificate answers, so something that intercepts it can pair in
    the Bridge's place. OpenELIS sends
    `HMAC-SHA256(code, "oe-pair-v1" ‖ its client certificate ‖ its server certificate ‖ nonce)`
    with its server certificate's fingerprint and the nonce; the Bridge
    recomputes it from its own view of the handshake, compares in constant
    time, and answers with its own HMAC over the same fields, which OpenELIS
    checks before pinning. A relay gives the two sides different certificates,
    so neither proof verifies, and the code never crosses the wire. There is no
    fallback to the raw code. A configured `BRIDGE_PAIRING_CODE` needs a
    minimum strength (about 16 random characters) or a PAKE. Tests: a proof
    over another client or server certificate is refused, the right proof
    pairs, and a relaying proxy fails on both sides. Existing pairings are
    unaffected.
11. **The Bridge contract drops result value hints.** The profile schema and
    its README still define `result_value_hints`, the validator checks them and
    the catalog strips them on load. Reject them instead.

Owned elsewhere:

12. **QC read permission.** Whoever owns QC roles decides whether QC reads are
    gated on `QaPermissions.VIEW_QC`, as the QC export is, instead of ADMIN, so
    Lab Supervisors keep the QC dashboard.

Vendor coverage still open in the baseline roadmap, long-running and not
blocking: T5.2, the FluoroCycler XT and QuantStudio distro profiles (step 5);
T6.0 and T6.2, the Cepheid 301-2002 parser sections and MTB/RIF Ultra (step 6);
T6.9, HL7 result parts, with the first HL7 baseline profile (step 6).

## Remaining work, in order

| Order | Work                       | Next step                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Acceptance                                                                                                                                                                          |
| ----- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | FILE and HL7 qualification | Qualify, with native traffic, the FILE and HL7 instruments the remediation brings into core beyond GeneXpert and FluoroCycler (steps 5 and 6 ship their profiles; this proves them): exports, assays, units, status and control semantics, archive and error retention.                                                                                                                                                                                                                                                                                                                           | UI directory configuration reaches Bridge watching; native files and HL7 messages save correct clinical values with independent readback.                                           |
| 2     | Durable delivery           | Prove OpenELIS queue outage, restart and replay on the current traffic helper, and operator retry after another transient failure (after #4421). Restore mock attachment after a Bridge replacement.                                                                                                                                                                                                                                                                                                                                                                                              | Replay returns the original counts; a retried delivery adds no clinical result.                                                                                                     |
| 3     | Core qualification         | After the baseline lands, run every supported workflow and present recordings from the same registered tests.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Every supported ASTM, FILE and HL7 workflow and required recovery scenario has passing independent readback, exact image identities and accessible reviewed video.                  |
| 4     | Madagascar distro          | After core qualification, per the remediation's distro follow-on: remove profiles core carries, unset the shipped-pattern override, rebuild the rest as baseline profiles, run the migration, update pins.                                                                                                                                                                                                                                                                                                                                                                                        | The distro consumes working core profiles and defaults without site-specific mapping repair.                                                                                        |
| 5     | Answer codes UI            | Deferred from step 2c (decided 6 Oct): Dictionary Management edits an answer's codes in any system through `/rest/test-catalog/answers/{id}/terminology` (shipped in step 2c), and the test catalog's option table shows each answer's codes read-only.                                                                                                                                                                                                                                                                                                                                           | An answer's LOINC, SNOMED, CIEL and OCL codes are edited in Dictionary Management and shown in the catalog; saving keeps `dictionary.loinc_code` on the SAME_AS LOINC code.         |
| 6     | Codes settled at setup     | Deferred from the #4611 review (9 Oct): a profile entry's `test_code` both names the entry and is the code the instrument is assumed to send. Give entries an `id`; setup writes, on the Bridge connection, the code this instrument sends for each entry it runs; the Bridge reads and orders under that code and puts it in the bundle; OE2 keys each mapping row by that code and links it to the entry for defaults and adoption. A renamed code and an undeclared code take one path. Bridge first, then OE2. Open: answer codes too; the entry `id` format; whether labs rename components. | After setup, no record of the analyzer (connection, bundle, mapping, review row) holds a code the instrument does not send; harness E2E covers a renamed code inbound and outbound. |

## Deferred review items

Open findings from the #4332 review that the remediation does not address.

| ID     | Finding                                                                                                                                                    | Owner                        | Follow-up                                                                                                                                                                     |
| ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| F-REV1 | Harness filesystem catalogs suppress bundled Horiba CBC and vector CSVs for those domains. Confirmed loader behaviour; intended coverage needs a decision. | FILE/HL7 qualification (1)   | Step 7 trims the harness catalog to the analyzer files; they still replace the built-ins of every domain they supply, so the suppression remains; state the supported corpus. |
| F-REV2 | Transactional TRUNCATE can retain locks that block a second connection or `REQUIRES_NEW` work. Credible risk; no failing case demonstrated.                | Backend test infrastructure  | Reproduce the cross-connection case; fix fixture isolation or transaction ownership so it finishes or fails diagnostically without hanging.                                   |
| F-REV4 | Repeated `SpringContext.getBean` access and reference-table lookups add indirection and query work. No functional failure or measured problem.             | OpenELIS service maintenance | Prefer injected or context-scoped dependencies where appropriate; verify lifecycle correctness and query reduction.                                                           |
| F-REV5 | The 2.3.x seed assigns the COVID LOINC to HIV viral-load variants. Step 7 corrects the harness CSVs only; the main dictionary and deployed sites remain.   | General catalog correction   | Audit affected records on deployed sites; apply a narrowly scoped correction preserving IDs, history and report labels.                                                       |

## Moved into the remediation

| Old item                                                         | Now                                                                         |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Repin OpenELIS to Bridge 3.2.6 (#4497)                           | Merged 1 October; the remediation repins again in step 7.                   |
| Reconcile populated catalogs and upgrade from a previous version | Step 2 (fresh-baseline migration) and step 4 (populated-catalog setup E2E). |
| A shipped core HL7 profile                                       | Steps 5 and 6 bring the Madagascar profiles, HL7 ones included, into core.  |
| F-REV3: a single local-code candidate bypasses disambiguation    | Step 1 (exact-match resolver) and step 1b (placement).                      |
| F-REV6: duplicate raw values render one hint editor              | Moot: profiles carry no hints (rule 1).                                     |

## Related open pull requests

| PR                                       | Disposition                                                                                                                                  |
| ---------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| OpenELIS #4421                           | Persists Retry and Dismiss of undelivered results in the audit trail. Open on 6 October.                                                     |
| OpenELIS #3974                           | TypeScript migration of analyzer forms; its targets were mostly removed in September. Close it, or narrow it. Open.                          |
| Madagascar test harness #4, #9, #10, #11 | After core qualification: reconcile #4 with merged #15; narrow #10; review the outbound proof in #9 and #11. Not re-checked since 1 October. |
| Review tooling #16, #31                  | Compare #16 with merged #17; update #31's evidence manifests to the tested pins. Not re-checked since 1 October.                             |

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
