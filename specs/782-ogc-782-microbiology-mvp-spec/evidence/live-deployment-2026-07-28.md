# OGC-782 Live Deployment And UAT Verification

## Current Candidate Status

- PR candidate and deployed app SHA:
  `ee5e4d1fa5324dd0ae742c0cc303b9e1b61b25cf`
- Application deployment ID: `20260729T140857Z-69be0c1c164e`
- Frontend evidence deployment ID: `20260729T142935Z-ee5e4d1fa532`
- Review-tooling SHA:
  `4df8441896e59520ffb9bb247b1b2e3ce3f5248d`
- Deployment scope: backend/app at `69be0c1c164ecb97128b06aaa003df72e58154e6`,
  then frontend-only at the exact PR head.
- Status: health, smoke, public SHA guard, focused tests, canonical deployed
  Playwright, and paced video evidence passed. Human Review-overlay rulings
  remain open as T200.

## Provenance

- Target: `https://amr.openelis-global.org`
- Final public target metadata:
  `https://amr.openelis-global.org/__review/target.json`
- Final live checklist:
  `https://amr.openelis-global.org/__review/uat-amr.json`
- Final health and smoke verification: passed
- Final app SHA guard:
  `ee5e4d1fa5324dd0ae742c0cc303b9e1b61b25cf`
- Earlier verified OpenELIS commit:
  `9d0c55b6fd3c2363ff8b16d83474f396132180ba`
- Review-tooling commit:
  `427aa5feb23a8d7b884211f64df6f752b4878a1e`
- Full application deployment: `20260728T222317Z-9d0c55b6fd3c`
- AMR backend recreation: `20260729T035240Z-9d0c55b6fd3c`
- Full deployment was schema-affecting; the later backend-only recreation was
  not schema-affecting and enabled the property-gated UAT scenario endpoint.
- Health, root route, `/Microbiology/worklist`, and backend smoke checks passed.

## Deterministic Fixture

The AMR review-tooling seed now authenticates to OpenELIS, reads CSRF state from
the authenticated session, and invokes
`POST /rest/microbiology/uat/scenarios`. It does not use SQL, fixed primary
keys, DAO bypass, or a production-enabled fixture endpoint.

Two consecutive runs with stable key
`review-amr-microbiology-mvp` returned the same records:

- accession: `UATMICRO01C82736AB`
- bacteriology case: `a65d620c-c96b-4627-9d69-9c00ba310551`
- sibling TB case: `3491532f-7dba-40f5-863b-7f4e3287d505`

Primary case:
`https://amr.openelis-global.org/Microbiology/cases/a65d620c-c96b-4627-9d69-9c00ba310551?workflow=BACTERIOLOGY&sort=newest&section=isolates`

## Grist And Review Overlay

- Checklist: `Microbiology MVP - review`
- Structure: four stories, ten steps
- Policy: steps 1-9 required; step 10, shared-specimen TB reflection, optional
- Checklist revision:
  `cec8626fb1998b8d1e78ae84616b32f006759d3ad7681fe7d2a4c307ce26dc52`
- Live source: `grist-live`
- The rendered overlay reported
  `feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness @
  9d0c55b`.

The live JSON contains configured-navigation, canonical-state, case navigation,
isolate, AST, report-propagation, and shared-specimen review steps. The final
application-SHA guard and live checklist revision were rechecked after the
candidate deployment.

## UI And Route Verification

- Desktop worklist uses the configured Microbiology sidenav, Carbon toolbar,
  filters, table, status tags, actions, and pagination without overlap.
- At `390x844`, the sidenav defaults closed, the viewport remains 390 pixels
  wide, and horizontal overflow is contained inside the table surface.
- Reloading
  `/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest` preserves the
  selected workflow and sort.
- Opening the deterministic case with `section=isolates` focuses the Isolates
  panel after reload.
- The case breadcrumb points back to
  `/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest`.

## Final Validation

- Complete microbiology backend group: 99 passed with Testcontainers.
- Patient History narrative renderer: 2 focused Vitest tests passed.
- Frontend production build: passed.
- Deployed foundational `core-app` group: 5 passed.
- Canonical deployed `core-demo`: setup plus MVP journey passed.
- Paced deployed `core-demo-video`: setup plus MVP journey passed in 1.9
  minutes.
- The visible Patient History row contains the confirmed organism,
  `Gentamicin (UAT) S`, and `Ciprofloxacin (UAT) R`.
- The final-case mutation request returns named HTTP 409
  `MICROBIOLOGY_CASE_LOCKED`.
- Review-tooling tests: 90 passed, including no-SQL seeding, AMR-only endpoint
  enablement, and the authenticated service-provisioning contract.
- Formatting and whitespace validation passed.
- External evidence:
  `/tmp/ogc-782-mvp-evidence-final-2026-07-29/`
- MP4:
  `/tmp/ogc-782-mvp-evidence-final-2026-07-29/videos/ogc-782-microbiology-mvp-walkthrough.mp4`
- Contact sheet:
  `/tmp/ogc-782-mvp-evidence-final-2026-07-29/contact-sheet.png`

## Issues Flagged

1. **Progress semantics need review.** The new `RECEIVED` case says the next
   action is to record initial setup, while Case info, Inoculation, and Timeline
   are labelled Complete in the progress rail. The labels should be reconciled
   with stage semantics before formal sign-off.
2. **Legacy demo data remains.** The AMR database already contains 15
   duplicate-looking cases from the retired SQL fixture path. The new service
   fixture is idempotent, but deleting old rows is intentionally excluded from
   this non-destructive deployment.
3. **Unrelated notification console errors remain.** The global notification
   subscription check logs failed fetch/JSON parsing errors. The Microbiology
   workflow and Review overlay continue to work; this appears to originate in
   the existing notification surface rather than OGC-782.
4. **Shared edge-router restart interrupted one recording.** OpenELIS
   containers remained healthy, but the review-tooling router restarted and
   briefly refused HTTPS connections. The recovered rerun passed.
5. **Legacy frontend warnings remain.** Deployed navigation emits global 404
   and `react-i18next` initialization warnings. The production build also
   reports existing `:global` CSS, large-chunk, and direct-`eval` warnings.
6. **WHONET mapping remains outside MVP.** Final release succeeds, while
   WHONET export correctly reports `Organism Mapping Required`; complete
   surveillance mapping remains follow-up scope.
