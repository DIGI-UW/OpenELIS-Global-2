# OGC-782 R1 Live Deployment Evidence

## Scope

This record closes automated deployment and review-harness evidence for the R1
authoritative-alignment remediation. It does not record human UAT decisions and
does not make sidenav implementation an R1 gate. Existing M1 navigation remains
available in its own UAT story.

## Provenance

- Target: `https://amr.openelis-global.org`
- Full runtime deployment: `20260807T022035Z-ee1238ce1c1d`
- Runtime app SHA: `ee1238ce1c1df75315b0f7791a2fd94b695149c0`
- Runtime app branch:
  `feat/782-ogc-782-microbiology-r1-authoritative-alignment`
- Review-tooling SHA: `f3deb02e6e45cacbe9a7ad77159c2aaf3fea8e2c`
- Grist checklist revision:
  `57379544553a890c9644183a09bd8f32fa17c11587cd649bbd659265bbdc62b8`
- Deployment scope: `app`; schema-affecting: `true`
- Target verification: health `passed`; smoke `passed`
- Public routes: `/` returned 200; `/Microbiology/worklist` returned 200
- Final evidence/test-only branch head and deployment ID are recorded in PR
  #4004 and the immutable live `__review/target.json` response. No application
  behavior changed after the runtime pass above.

## Service-Created Fixtures

Review tooling provisioned both scenarios through the authenticated,
property-gated OpenELIS service endpoint. The seed used no SQL, fixed primary
keys, DAO bypass, or production-exposed fixture route.

| Scenario | Stable key                         | Accession            | Primary case                           | Sibling case                           |
| -------- | ---------------------------------- | -------------------- | -------------------------------------- | -------------------------------------- |
| WORKLIST | `review-amr-ee1238ce1c1d-worklist` | `UATMICRO6B4BDCE2B2` | `bbc612f7-423f-4f44-8de6-b883bad163ef` | `6aa3792d-1cea-4ee8-afa2-8c935b58ddc2` |
| R1       | `review-amr-ee1238ce1c1d-r1`       | `UATMICROD690D0405C` | `fbe9f526-7a17-46ca-acf7-f0e5ed88cacc` | `73163f45-da05-4969-abff-e9604022f7db` |

## Grist And Overlay

- Grist source contains 22 story rows and 67 stable AMR steps.
- Nine story rows belong to the R1 workflow partition.
- The schema-v2 story index exposes 20 stories to
  `amr.openelis-global.org`.
- OGC-788 Macro stories `AMR-S15` and `AMR-S16` are assigned to
  `phrases.openelis-global.org` and do not render in the AMR picker.
- The shared aggregate transport still contains all 22 stories by design; the
  picker uses the host-filtered schema-v2 index and renders one story at a
  time.
- The deployed picker showed exactly 20 options with independent completion
  counts. Manual AST retained three steps through Refresh checklist and a full
  browser reload. The panel stayed open and did not enter a refresh loop.
- Signed-in review identity rendered as `Reviewing as Open ELIS`. Anonymous
  reviewer-name requirements remain covered in review-tooling tests.

## Worklist Browser Evidence

- The configured `/Microbiology/worklist` route rendered Carbon Culture and AST
  queue surfaces with a linkable Home-to-worklist breadcrumb.
- `workflow=BACTERIOLOGY&sort=newest` survived a full reload and returned the
  SHA-scoped R1 and WORKLIST accessions.
- The R1 accession link preserved workflow/sort state into the case; the
  Isolates section survived reload; the breadcrumb restored the worklist query.
- AST selection produced canonical
  `grain=ast&workflow=BACTERIOLOGY&sort=newest` state and rendered active and
  pending-setup run rows.
- At `390x844`, body, document, and viewport widths were all 390 pixels. The AST
  switcher, real summary counts, refresh status, and review launcher remained
  readable with no document-level horizontal overflow.
- Desktop Culture, desktop AST, and compact AST screenshots were inspected
  during the deployed browser run. No overlapping or raw pre-remediation
  worklist styling was observed.

## Focused Playwright

The live-only test now requires exact app branch/SHA, harness SHA, checklist
revision, and fixture accession. It validates the host-filtered story index,
R1 story/step partition, one-story overlay behavior, refresh/reload stability,
canonical worklist state, case section state, and breadcrumb return. It uses
semantic locators and response/readiness assertions only; no fixed waits or
forced interactions are present.

Command result against the runtime deployment: authentication setup plus the
single `core-live-uat` test passed (`2 passed`, 7.4 seconds) with zero retries.
Filter changes wait on their successful worklist responses, so the final run
contains no navigation-induced aborted worklist request.

## Issues And Boundaries

1. The global application still emits unrelated 404/subscription console noise
   during navigation. The same issue predates R1 and does not prevent the
   worklist or overlay from completing, but it should remain visible as shared
   shell debt.
2. The aggregate `uat-amr.json` is intentionally not host-filtered. Consumers
   must use the schema-v2 index; treating the aggregate as the picker inventory
   recreates the prior 51-step story-collapse bug.
3. Human Pass/Fail/N/A decisions remain open in T230/T273. Automated browser
   evidence is not acceptance.
4. Reviewed-run queue semantics, manual resistance confirmation, shared reagent
   policy, and least-privilege Bridge identity remain T283, T277, T288, and
   T290 respectively. This deployment does not invent rulings for them.
