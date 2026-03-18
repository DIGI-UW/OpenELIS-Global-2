# Playwright CI Stabilization and Acceleration

## Why this exists

This document defines the Playwright CI execution contract for OpenELIS so CI
remains fast, deterministic, and debuggable while still validating analyzer
harness end-to-end behavior.

## Baseline before remediation

- Analyzer workflow:
  `[.github/workflows/analyzer-e2e.yml](../../.github/workflows/analyzer-e2e.yml)`
  - 2 shards, each performing full Maven + plugin + Docker build.
  - 30 minute job timeout per shard.
  - Docker cache aggressively pruned before every run.
  - Blob reporter enabled, but no merged HTML report fan-in.
- Core workflow:
  `[.github/workflows/playwright-e2e.yml](../../.github/workflows/playwright-e2e.yml)`
  - Single Playwright job with Docker rebuild in the test job.
  - 30 minute job timeout.
  - Docker cache aggressively pruned before every run.
- Harness bind-mount gap:
  - `file-import-results` requires
    `projects/analyzer-harness/volume/analyzer-imports`.
  - CI compose override did not bind this host path to `/data/analyzer-imports`.

## New CI topology

```mermaid
flowchart TD
  analyzerBuild[analyzer_build_once] --> shardA[analyzer_test_shard_1]
  analyzerBuild --> shardB[analyzer_test_shard_2]
  shardA --> analyzerMerge[analyzer_merge_reports]
  shardB --> analyzerMerge
  analyzerMerge --> analyzerGate[analyzer_required_gate]
  coreTest[core_playwright_tests] --> coreMerge[core_merge_reports]
  coreMerge --> coreGate[core_required_gate]
```

## Workflow contracts

### Analyzer harness workflow

- Build once in `build-once`:
  - Maven artifacts and plugin jars are built once.
  - Docker images are built once using Buildx with GHA cache scope
    `analyzer-e2e`.
  - Plugin jars and Docker images are uploaded as short-lived artifacts.
- Test shards in `test-shards`:
  - Download prebuilt artifacts.
  - Restore plugin jars and load Docker images.
  - Start compose stack with `--no-build`.
  - Load fixtures and seed analyzers.
  - Run harness shards; run `demo` on shard 1 only.
  - Upload `blob-report` per shard.
- Merge in `merge-reports`:
  - Download all shard blob reports.
  - Merge to a single HTML report with `playwright merge-reports`.
- Enforce in `analyzer-e2e-gate`:
  - Required gate fails if shard or merge jobs fail.

### Core Playwright workflow

- Build images with Buildx cache scope `playwright-core`.
- Start compose stack with `--no-build`.
- Run `core-app` Playwright project.
- Upload blob report, merge to HTML in a fan-in job.
- Enforce success through `playwright-e2e-gate`.

## Video and demo test policy

- CI must run `demo` for validation speed and stability.
- `demo-video` is local-only and intentionally slower.
- Canonical local command:

```bash
cd frontend
CLEANUP=false TEST_USER=admin TEST_PASS='<password>' npm run pw:test:video
```

- Current script implementation:
  - `pw:test:video` runs `demo-video` with `PLAYWRIGHT_VIDEO=on` and
    `PLAYWRIGHT_SLOWMO=500`.
  - The command targets Linux/macOS shells; on Windows, run it in WSL.

## Test tiers and intent

- `core-app`: fast smoke checks on shared app behaviors.
- `harness`: analyzer bridge/simulator/plugin integration checks.
- `demo`: end-to-end feature workflow validation in CI.
- `demo-video`: local demonstration capture only.

## Branch protection guidance

To keep Playwright required without relying on matrix shard internals, require
these stable checks in branch protection:

- `Analyzer E2E Required Gate`
- `Playwright E2E Required Gate`

These checks are designed to fail if upstream shard/test/merge jobs fail.
