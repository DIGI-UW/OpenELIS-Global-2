# OGC-285 — Barcode Labels v2: Configurable Label Preset Management

**Status:** In Progress (spec authoring in flight)
**Jira:** https://uwdigi.atlassian.net/browse/OGC-285
**Authoritative design (FRS):** https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md
**FRS version pinned:** v2.5 · 2026-05-19 17:45 UTC · Casey Iiams-Hauser
**Predecessor:** [../OGC-284-barcode-label-quantity-management/](../OGC-284-barcode-label-quantity-management/) — superseded by this spec
**OGC-284 postmortem:** [../OGC-284-barcode-label-quantity-management/POSTMORTEM.md](../OGC-284-barcode-label-quantity-management/POSTMORTEM.md)

## How this directory is structured

This directory is the **engineering contract** for OGC-285. It is a
transformation of Casey's FRS into SpecKit format — **NOT a copy**. Prose
narrative stays in the FRS; testable requirements transform into numbered FRs
and acceptance criteria here.

Files (populated by `/speckit.specify`; see § "SpecKit kickoff" below):

- `spec.md` — user stories, acceptance criteria, functional requirements,
  constitution compliance, success criteria, clarifications
- `plan.md` — milestone breakdown (M1–M6) with branch + worktree + test contract
- `tasks.md` — task-level breakdown with `[T###] [P?] [US#]` IDs and AC
  traceability
- `research.md` — FRS snapshot pin (SHA `7cf6f65`), Open Question resolutions
  (Q1–Q4), rationale for the divergences from Casey's FRS
- `data-model.md` — entity-relationship description with DDL excerpts from
  FRS §7
- `contracts/openapi.yaml` — OpenAPI 3 spec for the 10 endpoints from FRS §8
- `quickstart.md` — workflow inventory + end-to-end verification recipe per
  milestone
- `checklists/` — review/AC tracking sheets

## Pin discipline

`research.md` cites the FRS by SHA. When the FRS in
[DIGI-UW/openelis-work](https://github.com/DIGI-UW/openelis-work) is updated
and we want to absorb changes, the protocol is:

1. Bump the SHA in `research.md`.
2. Diff the FRS between old and new SHA.
3. Reconcile any affected FRs/ACs in `spec.md`.
4. Commit the SHA bump + reconciliation atomically in one PR.

This is the **only** path FRS changes enter the engineering contract.

## Locked decisions for the spec (resolved Open Questions)

The FRS leaves four Open Questions for the engineering team. They are
resolved in `research.md` as follows (rationale recorded there):

| # | FRS question | Locked answer |
|---|---|---|
| Q1 | Snapshot vs current preset for reprint | **Snapshot only** (AC-20 canonical) |
| Q2 | Allow-override toggle home | **Per-test only** (link + master); no per-lab layer in v2 |
| Q3 | A11y drag-drop for content fields | **Keyboard + native HTML5 drag**; no react-aria dependency |
| Q4 | Future custom-field shape | **Single `source_type` column** (v2 schema unchanged) |

Two deliberate divergences from Casey's FRS are recorded in `spec.md`
Assumptions & Constraints (and `research.md` rationale):

- **Editable post-save quantities** is locked **YES** per Casey's 2026-05-19
  Jira edit; FRS markdown is silent on this and Jira tightens the design.
  Post-save dialog uses Carbon `<NumberInput>` with min 0, max from
  `order_label_request.qty` at save time.
- **OGC-284 Order Entry quantity UI gap** is **NOT deferred** (contra FRS
  §1.5); it is absorbed by US4 because M5's LabelsSection rewrite removes
  the `applicableLabelTypes: ["specimen"]` hardcode as a side effect.

## SpecKit kickoff

To author the SpecKit artifacts in this directory, run:

```
/speckit.specify
```

with the prompt recorded in the plan file at
`~/.claude/plans/toasty-hugging-sparkle.md` § "SpecKit kickoff prompt". The
prompt is self-contained — SpecKit only needs that text + read access to
this repo.

After `/speckit.specify` generates the artifacts, a hand-edit pass folds in:

- US4 (Migration & continuity + OGC-284 gap absorption) — Casey's FRS does
  not have this user story; it is engineering-owned and must be added.
- The 4 Q-resolutions in `spec.md` Clarifications.
- The 2 design divergences in `spec.md` Assumptions & Constraints.
- Constitution Compliance section CR-001..CR-008 per the OGC-284 spec
  template.

## Postmortem guardrails apply to every milestone PR

From [POSTMORTEM.md](../OGC-284-barcode-label-quantity-management/POSTMORTEM.md)
§"Lessons forward":

1. No self-merge without non-Copilot human review.
2. AC checklist in PR body.
3. No mid-stream rescoping.
4. No Jira self-resolve.
5. ≤ 30 files / ≤ 2,500 LOC net per milestone PR.
6. Tests precede implementation (Constitution Principle V).
7. Open PR as draft early.
8. AC-by-AC walkthrough at PR-ready.
