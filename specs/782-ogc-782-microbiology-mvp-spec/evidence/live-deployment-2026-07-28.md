# OGC-782 Live Deployment And UAT Verification

## Provenance

- Target: `https://amr.openelis-global.org`
- OpenELIS commit: `9d0c55b6fd3c2363ff8b16d83474f396132180ba`
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
- Policy: all ten steps required
- Checklist revision:
  `e4b7ae19652e786a09aeead8fd39567bb66dc487fef9f9a8b0ac75f7aa0f0a37`
- Live source: `grist-live`
- The rendered overlay reported
  `feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness @
  9d0c55b`.

The live overlay contains configured-navigation, canonical-state, case
navigation, isolate, AST, report-propagation, and shared-specimen review steps.

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

## Validation

- Backend focused logic tests: 51 passed.
- Frontend focused tests: 34 passed across eight files.
- Frontend production build: passed.
- Review-tooling tests: 90 passed, including no-SQL seeding, AMR-only endpoint
  enablement, and the authenticated service-provisioning contract.
- Formatting and whitespace validation passed.
- The ORM/Testcontainers check could not start because Docker Desktop was
  unavailable in the local validation environment; its two errors were
  environment startup errors, not test assertion or compilation failures.

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

