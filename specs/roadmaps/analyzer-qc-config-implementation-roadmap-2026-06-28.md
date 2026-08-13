# Analyzer QC and Configuration Foundation Roadmap

**Updated:** 2026-08-13
**Status:** Historical foundation record; superseded as product roadmap
**Branch:** `codex/ogc-1054-analyzer-qc-mvp`
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)

The authoritative feature roadmap is now
[OGC-1054 Analyzer Feature Authoritative Roadmap](ogc-1054-analyzer-feature-roadmap.md).
This file records the narrower Analyzer QC/configuration foundation implemented
and demonstrated in July 2026. It does not define or accept the OGC-1054 MVP.

Git history retains the earlier roadmap and acceptance wording for provenance.
That wording reduced the feature definition to match the branch and therefore
must not be used for current planning or status.

## Source Boundary

`DIGI-UW/openelis-work` was and remains functional/visual reference material
only. Its mocks and product documents may be used to assess visible user
outcomes and gaps. They do not define persistence, entities, APIs, routes,
repository ownership, runtime architecture, or implementation sequence.

## Foundation Delivered by the Branch

| Foundation area | Branch implementation | Current classification |
| --- | --- | --- |
| Stabilization | Routed QC loading, string-safe lot payloads, deterministic Bridge collections, and no OpenELIS FILE poller | Implemented foundation |
| Profile bootstrap | Shipped-file summaries and create-time `defaultConfigId` application | Implemented foundation; not reusable Analyzer Type lifecycle |
| Guided shell | Inline creation entry plus bookmarkable Instrument, Verify, Connect, and Review routes | Implemented foundation; not the complete guided product story |
| Mapping safeguards | Catalog-bound result options, legacy-unbound handling, pending resolvers, fingerprints, and audit | Implemented foundation; full editor and traffic producer absent |
| Operational QC | Analyzer QC rules, control lots, readiness, activation blockers, and Bridge resync | Implemented foundation; distinct from Bridge profile QC identification |
| Remote evidence | Eight UI steps and MP4 against the exact July application/harness build | Historical foundation evidence only |

## Architecture Context

- Bridge is the analyzer runtime and portable profile owner. It owns listeners,
  parsing, connection probes, protocol execution, FILE watching/transport, and
  normalized FHIR output.
- OpenELIS owns lab-facing orchestration, local Test/Result Option bindings,
  analyzer instances and lab units, audit, operational QC, activation, held
  clinical results, and alerts/review.
- The analyzer mock must exercise real Bridge transports for target
  architecture acceptance.
- No implementation decision is sourced from `openelis-work`.

## PR #3792 Disposition Gate

1. Rename the PR and branch-facing spec/evidence from MVP to foundation.
2. Rebase onto current `develop` and review a range-diff.
3. Retain only code compatible with the fixed Bridge boundary and canonical
   roadmap.
4. Remove or explicitly migrate touched duplicate/legacy paths.
5. Either merge a green, reviewable foundation PR or extract compatible commits
   into the milestone PRs and supersede #3792.

The PR cannot merge based on July checks or evidence: it is currently
conflicting, review-required, behind `develop`, and the deployed build is not
current branch HEAD.

## Historical Evidence

[The July evidence record](ogc-1054-analyzer-qc-config-mvp-evidence-2026-07-28.md)
proves only the recorded foundation workflow against application SHA
`2c840a55b03b238a2ad00c987181504c2bef6ef6`. It does not prove reusable Analyzer
Types, complete mapping, production unknown-traffic capture/hold, integrated
mock-to-Bridge traffic, or the full MVP.

## Next Work

Follow F0, E0, M1-M4, and G0 in the authoritative roadmap. The first product
review gate is the integrated full MVP deployment with current Grist steps and
MP4, not another reduced-scope acceptance pass.
