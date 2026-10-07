# Analyzer work after the baseline remediation

Analyzer work that is not part of the
[analyzer baseline remediation](../roadmaps/analyzer-baseline-roadmap.md). It
keeps the unfinished items of the roadmap that preceded the remediation
(`specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md`, in git history), each
with its verdict as of 6 October 2026. The remediation's packaging describes
the distro follow-on.

## Remaining work, in order

| Order | Work                       | Next step                                                                                                                                                                                                                                                               | Acceptance                                                                                                                                                                  |
| ----- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | FILE and HL7 qualification | Qualify, with native traffic, the FILE and HL7 instruments the remediation brings into core beyond GeneXpert and FluoroCycler (steps 5 and 6 ship their profiles; this proves them): exports, assays, units, status and control semantics, archive and error retention. | UI directory configuration reaches Bridge watching; native files and HL7 messages save correct clinical values with independent readback.                                   |
| 2     | Durable delivery           | Prove OpenELIS queue outage, restart and replay on the current traffic helper, and operator retry after another transient failure (after #4421). Restore mock attachment after a Bridge replacement.                                                                    | Replay returns the original counts; a retried delivery adds no clinical result.                                                                                             |
| 3     | Core qualification         | After the baseline lands, run every supported workflow and present recordings from the same registered tests.                                                                                                                                                           | Every supported ASTM, FILE and HL7 workflow and required recovery scenario has passing independent readback, exact image identities and accessible reviewed video.          |
| 4     | Madagascar distro          | After core qualification, per the remediation's distro follow-on: remove profiles core carries, unset the shipped-pattern override, rebuild the rest as baseline profiles, run the migration, update pins.                                                              | The distro consumes working core profiles and defaults without site-specific mapping repair.                                                                                |
| 5     | Answer codes UI            | Deferred from step 2c (decided 6 Oct): Dictionary Management edits an answer's codes in any system through `/rest/test-catalog/answers/{id}/terminology` (shipped in step 2c), and the test catalog's option table shows each answer's codes read-only.                 | An answer's LOINC, SNOMED, CIEL and OCL codes are edited in Dictionary Management and shown in the catalog; saving keeps `dictionary.loinc_code` on the SAME_AS LOINC code. |

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
