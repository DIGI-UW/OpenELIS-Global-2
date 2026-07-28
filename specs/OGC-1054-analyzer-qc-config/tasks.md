# OGC-1054 Tasks

## C0 - Baseline and specifications

- [x] Rebase the single branch onto current `develop`.
- [x] Run focused analyzer/QC baseline tests.
- [x] Create the canonical OGC-1054 spec, plan, route contract, and checklist.
- [x] Reconcile Feature 004, 012, 014, OGC-41, and the roadmap.
- [x] Replace PR title/body with current acceptance state.
- [x] Commit the C0 checkpoint.

## C1 - URL state and page shell

- [x] Add failing route-helper tests for stable query ordering and safe
      `returnTo`.
- [x] Add failing list/profile tests for URL-restored search and filters.
- [x] Add failing semantic header/breadcrumb tests.
- [x] Implement shared route helpers and `PageHeader`.
- [x] Migrate analyzer/QC setup surfaces and remove obsolete `PageTitle`.
- [x] Run focused tests.
- [x] Commit C1.

## C2 - Guided setup

- [x] Add failing router-backed tests for Instrument → Verify → Connect →
      Review.
- [x] Add failing tests for QC detour save/cancel return paths.
- [x] Implement `AnalyzerSetupProgress` and canonical route transitions.
- [x] Add the Review route and readiness summary.
- [x] Preserve list/catalog return context without reapplying profiles.
- [x] Run focused tests.
- [x] Commit C2.

## C3 - Carbon and responsive remediation

- [x] Add failing profile/mapping table accessibility tests.
- [x] Convert profile/mapping tables to reusable Carbon composition.
- [x] Replace ambiguous actions and add translated accessible labels.
- [x] Verify desktop and mobile layout against the pinned design baseline.
- [x] Run focused tests.
- [x] Commit C3.

## C4 - Acceptance closure

- [x] Run targeted and package-level JUnit 4 analyzer/QC suites.
- [x] Run focused and package-level Vitest/RTL suites.
- [x] Run Spotless, Prettier, supported ESLint, build, and Playwright guard.
- [x] Run `digi-uw/code-qa` pre-deployment gates and retain outputs.
- [x] Complete the `digi-uw/code-qa` evidence bundle after remote recording.
- [x] Push the exact application build and deploy it to analyzer UAT.
- [x] Sync Grist steps `AN-QC-001` through `AN-QC-008`.
- [x] Run and inspect `harness-demo`.
- [x] Run `harness-demo-video` and retain MP4/screenshots/report.
- [x] Update evidence and roadmap with exact SHAs and results.
- [x] Update the PR body after pushing the final provenance commit.
- [ ] Resolve review comments and CI; keep the PR non-draft.
