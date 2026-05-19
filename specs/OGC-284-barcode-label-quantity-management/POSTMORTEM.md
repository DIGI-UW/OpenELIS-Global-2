# OGC-284 Postmortem — How v1 Shipped With Gaps

**Author:** Piotr Mankowski
**Date written:** 2026-05-19
**Source:** Triage canvas (session artifact at `/tmp/ogc-285-triage.{html,tsx}`,
2026-05-19) snapshotted into this file for durable in-repo reference.

This document records the timeline, root causes, and forward guardrails for why
OGC-284 (Barcode Labels v1) shipped with most of its acceptance criteria
implemented in the schema but missing from the UI/service layers. It exists
primarily so future engineers picking up OGC-285 (and adjacent work) inherit the
context rather than rediscover it.

## TL;DR — Casey's own retro (Jira, 2026-05-18)

> "Although this ticket is marked Done, a May 2026 audit found that only the
> Barcode Configuration admin updates landed in production. The Order Entry
> Labels section, post-save print dialog, and per-type print PDFs are missing."

> "A gap-closing ticket covering the missing v1 functionality is being scoped —
> pending Piotr's call on whether to file as Bug, new Story, or reopen this
> ticket."

The implicit answer became [OGC-285](https://uwdigi.atlassian.net/browse/OGC-285).
Casey rewrote the spec the next day rather than reopen 284.

## Smoking-gun datapoint

PR [#3055](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3055)'s
`reviewDecision` field **at merge time** was `REVIEW_REQUIRED`. 130 files,
+11,060 / −921 LOC, merged 4 days after opening, with **only an automated
Copilot review on record**. No human reviewer approved the PR before the author
self-merged it.

## Timeline (20 events · 2025-12-17 → 2026-05-19)

| Date | Actor | Type | Event |
|---|---|---|---|
| 2025-12-17 | Casey | scope | Writes OGC-284 spec — 7 ACs incl. Labels section + Skip-Print-Later. |
| 2026-01-06 | Caleb | milestone | Takes the ticket; first commits land (add freezer label, expand barcode fields). |
| 2026-01-30 | Caleb | problem | [PR #2658](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2658) closed without merging — Caleb leaves UW. |
| 2026-02-11 | Casey | discovery | Slack: "did you finish this, or do we need to reassign it?" |
| 2026-02-12 | Piotr | decision | Replies: "lower priority than analyzers." Assigns himself anyway. |
| 2026-02-14 | Cursor Agent | risk | Three AI "careful squash rebase onto develop" commits onto the inherited half-done branch. |
| 2026-03-10 | Piotr | decision | Jira comment recasts work as "assessment-driven hardening on top of already-landed work, not first-pass implementation." |
| 2026-03-10–11 | Piotr | risk | Opens 6 milestone PRs (M2–M7) in 24h — all closed without merging. |
| 2026-03-11 | Piotr + Cursor | risk | Commit titled literally "OGC-284 M5: deferred gaps FR-004a, FR-002b, FR-012a". |
| 2026-03-12 02:28 | Piotr | milestone | Opens consolidator [PR #3055](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3055) — 130 files, +11,060 / −921. Body asks "which are addressed vs deferred when reviewing." |
| 2026-03-12 02:34 | Copilot | review | Only PR review ever recorded. 8 file-level concerns flagged. |
| 2026-03-12 07:13 | Piotr | review | Posts 8 own replies to Copilot. State = COMMENTED, never APPROVED. |
| 2026-03-12 13:35 | Piotr | decision | DMs Male Samuel for review; thread mixes "trying to push out analyzers" + "my birthday tomorrow." |
| 2026-03-16 06:32 | Piotr | merge | Self-merges PR #3055. `reviewDecision: REVIEW_REQUIRED`. No human approval ever recorded. |
| 2026-04-01 21:12 | Piotr | merge | Marks OGC-284 status Done + resolution Done in Jira (self-resolution, 3 weeks post-merge). |
| 2026-04-09 | Romain (Mekom) | bug | First post-merge bug report from Madagascar — barcode printing errors. |
| 2026-04-16 | Moses | bug | Commit `8018a7b` "fix bugs on patient entry" (#3431) touches LabelsSection. |
| 2026-05-01 → 05-03 | Herman+Ian+Piotr | bug | [PR #3528](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3528) fixes 3 production bugs: qty hardcoded to 1, freezer produces empty PDFs, per-sample counts multiplied by N. |
| 2026-05-18 | Casey | discovery | Posts gap-analysis Jira comment on OGC-284: "only the admin updates landed. Order Entry Labels, post-save dialog, per-type PDFs all missing." |
| 2026-05-19 | Casey | scope | Rewrites OGC-285 with full gap list; assigns to Piotr. |

## Root causes — process, not people

### 1. Inheritance discontinuity
Caleb owned design + first implementation pass; left UW mid-stream.
Reassignment 12 days later was a status check (Casey: "did you finish this?"),
not a design transfer. New owner inherited a branch they hadn't designed.

### 2. Deprioritization that never updated the AC contract
Piotr deprioritized OGC-284 vs analyzers in Feb — a legitimate call. But the
7-AC list in Jira was never trimmed to match the narrower target. Spec said
7 ACs; work plan implicitly aimed at fewer.

### 3. Reframed from "implement spec" to "harden what's there"
March 10 Jira comment: "assessment-driven hardening on top of already-landed
work, not first-pass implementation." Once the lens flips, you stop checking
ACs against code — you check code against code. **Missing surfaces become
invisible by definition.**

### 4. AI squash-rebases on a half-finished branch
Three Cursor-authored "careful squash rebase onto develop (feature scoped)"
commits on 2026-02-14. Squash-rebasing consolidates only what's **already
committed**; ACs that were never implemented vanish from the diff entirely.
The diff doesn't show what's missing.

### 5. Six milestone PRs closed unreviewed, swallowed into a 130-file consolidator
M2–M7 each had a chance to scope-check a milestone against its FRs. None
landed individually; the consolidator became the only review surface. At
+11k LOC nobody reads line-by-line. The milestone discipline that would have
caught gaps was discarded structurally.

### 6. Self-merged with no recorded human approval
PR #3055 `reviewDecision = REVIEW_REQUIRED` at merge time. Only Copilot
reviewed. Author's own PR comments are replies to Copilot, not approvals from
another engineer. PR body literally asked "which are addressed vs deferred
when reviewing" — the merging engineer was the only person who could have
answered that question.

### 7. "Done" flag was a self-resolution, 3 weeks post-merge
No external sign-off, no QA pass tied to the resolution, no acceptance
walkthrough. Same author who merged also flipped status In Progress → Done
on 2026-04-01 — despite his own March 10 Jira note saying "do not mark fully
complete until artifacts and code are aligned."

### 8. Deployment feedback loop took 9 weeks to surface the gaps
Madagascar reported quantity bugs (Apr 9) — got fixed in PR #3528 (May 3).
Those bugs masked the bigger gap: "are the Labels surfaces even there?"
wasn't asked until Casey's May 18 audit. Surface-level bug reports never
expose missing-feature gaps; only an AC-by-AC walkthrough does.

## Lessons forward (applied to OGC-285)

These rules are codified in [OGC-285 plan.md](../OGC-285-barcode-label-presets/plan.md)
and in every milestone PR template:

1. **No self-merge without non-Copilot human review.** Merge author MUST NOT
   be the same as the only reviewer. `gh pr view --json reviewDecision` must
   read `APPROVED`, not `REVIEW_REQUIRED`.
2. **AC checklist in PR body.** Each FRS acceptance criterion the PR claims
   to close gets an explicit checkbox; reviewer ticks them.
3. **No mid-stream rescoping.** If a milestone grows during implementation,
   open a follow-up issue and ship original scope; do not silently expand.
4. **No Jira self-resolve.** Status moves to Done at PR merge by the merge
   author or automation, not by the original author later.
5. **Milestone-sized PRs.** ≤ 30 files / ≤ 2,500 LOC net per milestone PR.
   Larger means slice further. The OGC-284 omnibus was 130 files / +11,060
   LOC — directly attributable to the gaps.
6. **Tests precede implementation** per Constitution Principle V. Red →
   Green → Refactor; never skip tests to land green.
7. **Open PR as draft early** so CI catches failures while implementation
   continues. PR is the working artifact, not the finishing artifact.
8. **AC-by-AC walkthrough at PR-ready.** Author and reviewer step through
   the spec's acceptance scenarios against the running UI before merge — not
   "do the tests pass" but "does the feature exist."
