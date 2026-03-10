# Quickstart: Launching the `013` HL7 Lane

## Purpose

Use this guide to start the first implementation branch only after the
coordination artifacts have been reviewed and accepted.

## 1. Reconfirm the planning source of truth

Review these artifacts before creating any implementation branch:

- `specs/013-hjra-hl7-stream-alignment/spec.md`
- `specs/013-hjra-hl7-stream-alignment/plan.md`
- `specs/013-hjra-hl7-stream-alignment/research.md`
- `specs/013-hjra-hl7-stream-alignment/contracts/hl7-branch-contract.md`
- `specs/013-hjra-hl7-stream-alignment/contracts/hl7-readiness-gates.md`
- `specs/roadmaps/parallel_analyzer_lanes_af342372.plan.md`
- `specs/roadmaps/madagascar-atlassian-alignment.md`

## 2. Sync the working tree and submodules

From the repository root:

```bash
git checkout develop
git pull --rebase upstream develop   # substitute origin or your fork remote name if you do not use upstream
git submodule update --init --recursive
```

If the bridge or plugin submodules are intentionally pinned to older commits for
active work, document that exception before creating the implementation branch.

## 3. Start with the listener foundation bundle

Create the first main-repository implementation branch only when the team is
ready to work the bridge and main-repository sides together:

```bash
git checkout -b feat/013-ogc-325-hl7-listener-foundation
```

At the same time, create the corresponding working branch in the bridge
submodule repository using that repository's normal branch conventions, but keep
it explicitly mapped to `OGC-325`.

Do not treat bridge startup alone as progress. M1 is only meaningful if the
paired work can reach the readiness gate defined in
`contracts/hl7-readiness-gates.md`.

## 4. Capture the `OGC-325` proof before opening BC-5380

Before starting BC-5380 work, collect evidence for all of the following:

- MLLP listener accepts representative HL7 traffic
- ACK behavior is demonstrated
- Bridge routes traffic into `/analyzer/hl7`
- One representative bridge-to-OpenELIS ingestion path completes successfully

Use the analyzer mock properly for E2E: start the mock (e.g. via
`projects/analyzer-harness` or `tools/analyzer-mock-server`), configure it to
**load an HL7 profile** and to **mock a specific analyzer type** (e.g. BC-5380
for the first proving path), then run the full path mock → transport →
`/analyzer/hl7` → ingestion. Evidence that relies only on ad-hoc payloads does
not satisfy the mock-based E2E requirement.

If one of these proofs is missing, do not open `feat/013-ogc-327-bc5380-hl7`
yet.

### Harness setup for HL7 + MLLP (Gate 1)

The analyzer harness (`projects/analyzer-harness`) includes the bridge with MLLP
enabled for HL7 E2E proof:

1. **Start the harness** (includes bridge with MLLP on port 2575):

   ```bash
   cd projects/analyzer-harness
   docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml up -d
   ```

2. **Bridge configuration**: MLLP listener on port 2575; forwards to
   `https://oe:8443/api/OpenELIS-Global/analyzer` (router appends `/hl7` for
   HL7).

3. **Load analyzer fixtures** (BC-5380, GenericHL7 plugin) from repo root:

   ```bash
   ./src/test/resources/load-analyzer-test-data.sh --dataset-011
   ```

4. **Send HL7 via MLLP** to `localhost:2575` (e.g. with
   `bc5380-cbc-result.hl7`). The mock must load a profile and mock a specific
   analyzer type (BC-5380) per Gate 1 requirement.

## 5. Open BC-5380 as the first proving branch

After `OGC-325` readiness is accepted:

```bash
git checkout develop
git pull --rebase upstream develop
git checkout -b feat/013-ogc-327-bc5380-hl7
```

Use BC-5380 as the narrow proving slice. Avoid widening the branch into
BS-series work even if some profile handling appears reusable.

## 6. Open BS-series only after BC-5380 is accepted

Once BC-5380 is proven:

```bash
git checkout develop
git pull --rebase upstream develop
git checkout -b feat/013-ogc-326-bs-series-hl7
```

This branch is committed to both BS-200 and BS-300. Early in the branch, verify
whether BS-300 can safely share the BS-200 path. If the answer is "not yet" or
"not cleanly," record that explicitly rather than letting the assumption linger.

## 7. Keep evidence gaps visible

While working the downstream branches:

- Treat missing GenericHL7 docs as a limitation until restored or replaced by
  stronger evidence
- Treat profile JSONs as seed inputs, not proofs of completeness
- Treat stale ASTM examples as contamination risk, not guidance for HL7 scope

## 8. Pre-PR checks

Before any implementation PR:

- Re-read the branch contract and readiness gate documents
- Ensure the branch still matches its allowed scope
- Run the relevant tests for the actual code being changed
- Run formatting before commit: `mvn spotless:apply` and
  `cd frontend && npm run format && cd ..`
- Keep bridge and main-repository PRs linked when working the `OGC-325`
  foundation bundle
