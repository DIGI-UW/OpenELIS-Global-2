# PRFlow — Phase 1 & 2 Plan, Evidence, and PR Checklist

**Repo:** `DIGI-UW/OpenELIS-Global-2`  
**Base branch:** `develop`  
**Date:** 2026-04-23  
**Author context:** Proposal triggered by maintainer feedback that PR/review cycles are a blocker (`@pmanko`, Thursday discussion).

**Related artifacts:**
- Triage snapshot: [prflow-pr-triage-2026-04-23.md](./prflow-pr-triage-2026-04-23.md)
- Existing automation: [.github/workflows/label-merge-conflict.yml](../../.github/workflows/label-merge-conflict.yml)

---

## 1. Problem statement (talk post, condensed)

OpenELIS PR flow shows recurring patterns:

| Pattern | What it means |
| -------- | -------------- |
| Burst reviews | Work piles up, then clears in spikes — not steady |
| Reviewer concentration | Few maintainers carry most review load |
| Idle “ready” PRs | Technically fine PRs sit without merge |
| Conflict loops | Long-lived PRs rebased repeatedly, never land |
| Contributor timing | Feedback arrives after author is unavailable |
| Large PR aging | Big diffs get harder to review over time |

**Two approaches considered:**

| | Approach A: Extend GitHub Actions | Approach B: PRFlow service |
| -- | --------------------------------- | --------------------------- |
| **Idea** | Better labels, auto-assign, smarter stale, `ready-to-merge` | External layer: classify → score → assign → act |
| **Strength** | Fits existing stack, fast to ship | Context-aware, load balancing, real lifecycle |
| **Limit** | Rule-based; no dynamic priority; signals ≠ decisions | More ops, design, and buy-in |

**Recommendation:** Start with a **hybrid Phase 1** (Actions + scheduled digest). Only add a service in Phase 2 if Phase 1 plateaus.

---

## 2. Evidence from triage (do not re-litigate)

Snapshot: **253 open PRs** on `develop` (2026-04-23). Deep analysis on **31** PRs (10 community + 11 dependabot + 10 stale).

### Repo-wide labels (all 253 PRs)

| Label | Count |
| ----- | ----: |
| `merge conflict` | **96** |
| `stale` | **33** |
| `community` | 179 |
| `backend` | 105 |
| `frontend` | 69 |
| `test` | 51 |

### Analyzed subset (31 PRs)

| Metric | Value |
| ------ | ----: |
| With **0 approvals** | **29** |
| With **≥1 approval** | **2** (both in stale list, then stuck) |
| Community “ready” (Section A) | 10 |
| Dependabot open | 11 |
| Stale candidates (Section C) | 10 |
| Dependabot CI-green (auto-merge after rebase) | **8** |
| Dependabot needs human triage | **3** (#3410, #3402, #3476) |

### Headline finding

**Bottleneck = review approvals + branch drift (`behind`), not merge conflicts.**

- Sections A + B: **0 approvals** across 21 PRs; most CI-green; almost all `mergeable_state = behind` or `blocked` (not `dirty`).
- Unauthenticated API once reported `clean` — **live auth query showed `behind`**; always refresh merge state with token.

### Notable edge cases (drives lifecycle design)

| PR | Issue |
| -- | ----- |
| #2880 | `CHANGES_REQUESTED` — not “ready” despite green CI |
| #3432 | CI green, tiny diff, **`blocked`** — branch protection, needs approval |
| #2573, #2612 | **Approved**, then conflict loop — `approved-stale` |
| #2518 | Label `stale` but **`behind` + CI green** — mis-triage |
| #3410 + #3402 | Log4j pair — **same CI failures**; merge/triage together |
| catalyst-mcp / catalyst-agents | Sibling dependabot PRs — batch, don’t review in isolation |

Full PR tables: [prflow-pr-triage-2026-04-23.md](./prflow-pr-triage-2026-04-23.md).

---

## 3. Best approach for Phase 1 (recommended)

**Phase 1 = GitHub Actions + one scheduled job + labels/comments only. No external service. No auto-merge without explicit maintainer policy.**

### Why this is the best Phase 1

1. **Matches what already exists** — `label-merge-conflict.yml` proves the team accepts Action-driven PR automation.
2. **Evidence says quick wins are mechanical** — rebase (`behind`), dependabot batching, digest for zero-approval queue.
3. **Low risk** — comments and labels first; no bot merging until policy is agreed.
4. **Shows value in one PR** — you can open a **proposal + 1–2 workflows** and point at the triage report in the PR description.
5. **Defers Phase 2** until you measure: time-to-first-review, time-to-merge for small PRs, stale mis-label rate.

### Phase 1 scope (4 workflows, priority order)

| Priority | Workflow | Trigger | What it does |
| -------- | -------- | ------- | ------------- |
| **P0** | `prflow-queue-digest.yml` | `cron` daily (e.g. 09:00 UTC) | Top 15 PRs by score → comment on a pinned tracking issue or workflow summary |
| **P1** | `prflow-classify.yml` | `pull_request` opened / synchronize | Labels: `size/*`, `area/*`, `type/*` from paths + title |
| **P2** | `prflow-ready-dependabot.yml` | PR sync + schedule | If `dependabot[bot]` + CI success + not dirty → label `ready-to-merge` + comment “rebase then merge” |
| **P3** | `prflow-behind-nudge.yml` | PR sync | If `behind` + CI green + no `changes requested` → comment with `gh pr update-branch` hint (optional: enable update-branch if org allows) |

**Explicitly out of Phase 1:**
- External webhook service
- Auto-merge (except maybe later: dependabot patch-only with rules)
- ML / LLM classification
- Replacing human reviewers

### Lifecycle labels (Phase 1 — add to repo label set)

Use **one** lifecycle label per PR (mutually exclusive):

| Label | Meaning |
| ----- | ------- |
| `prflow/awaiting-review` | CI ok, needs reviewer |
| `prflow/awaiting-author` | Changes requested |
| `prflow/behind` | Needs update branch |
| `prflow/blocked` | Branch protection / approval |
| `prflow/ready-to-merge` | Low-risk, green CI, no blockers |
| `prflow/conflicted` | Align with existing `merge conflict` |
| `prflow/approved-stale` | Had approval, now conflicted |
| `prflow/stale-active` | Stale label but recent human commit |
| `prflow/ci-triage` | Failing checks need human |

Phase 1 can set these in the **digest job** first (read-only scoring), then automate in P1–P3.

### Priority score v1 (digest only)

```
score =
  (age_days * 2)
  + (files <= 3 && additions+deletions < 100 ? -10 : 0)
  + (files > 20 || additions+deletions > 1000 ? +20 : 0)
  + (ci_all_success ? -15 : +30)
  + (approvals == 0 && ci_all_success ? +25)
  + (changes_requested ? -50)
  + (author == dependabot[bot] && ci_all_success ? +40)
```

Sort descending; post top 15 with: PR link, author, score, mstate, approvals, CI, suggested action.

### What Phase 1 does **not** solve (Phase 2)

- Reviewer **load balancing** across maintainers (needs history DB or CODEOWNERS + round-robin service)
- **Burst review** scheduling (needs calendar integration or maintainer preferences)
- **Cross-repo** or org-wide metrics

---

## 4. Phase 2 (after Phase 1 metrics)

| Component | Purpose |
| --------- | ------- |
| Small **PRFlow service** (or GitHub App) | Webhook on `pull_request`, `check_run`, `pull_request_review` |
| **State store** | Last human commit, last review, current lifecycle (not just `updated_at`) |
| **Reviewer assignment API** | Pools from CODEOWNERS + load from open review requests |
| **Smarter stale** | Replace age-only bots: `approved-stale`, `stale-active`, `stale-abandoned` |
| Optional **auto-merge** | Dependabot patch + CI + 0 required failures only, with allowlist |

**Gate to Phase 2:** 4–6 weeks of digest data + maintainer feedback (“digest useful / noisy / wrong”).

---

## 5. Architecture (one diagram)

```text
Phase 1 (now)
  PR events / cron
       ↓
  GitHub Actions (classify | digest | dependabot-ready | behind-nudge)
       ↓
  GitHub API (labels, comments, optional update-branch)

Phase 2 (later)
  PR events → Webhook → PRFlow service → Decision engine → GitHub API
                              ↑
                         state DB (human activity, review load)
```

---

## 6. Checklists

### A. Before you open the PR (proposal PR)

- [ ] Triage report committed: `prflow-pr-triage-2026-04-23.md`
- [ ] This plan committed: `prflow-phase1-phase2-plan.md`
- [ ] Branch from latest `develop` (`git pull upstream develop` or equivalent)
- [ ] No secrets in repo (never commit `GITHUB_TOKEN` / `ghp_*`)
- [ ] Revoke any token pasted in chat; use `gh auth login` locally
- [ ] Confirm worktree: `git worktree list` if parallel branches exist
- [ ] PR targets **`develop`**, not `main`
- [ ] PR title references issue if you have one (e.g. `docs: PRFlow phase 1 proposal and PR triage evidence`)

### B. Phase 1 implementation (follow-up PRs after proposal merges)

- [ ] Labels created in repo: `prflow/*` (or document “create labels” step for maintainers)
- [ ] `prflow-queue-digest.yml` — read-only, posts to issue #___ (agree number with team)
- [ ] `prflow-classify.yml` — path rules documented in workflow comments
- [ ] `prflow-ready-dependabot.yml` — never label failing CI as ready
- [ ] `prflow-behind-nudge.yml` — skip if `changes_requested`
- [ ] Permissions: `pull-requests: write`, `issues: write` (same as merge-conflict workflow)
- [ ] `if: github.repository == 'DIGI-UW/OpenELIS-Global-2'` guard on all jobs
- [ ] Test on a draft PR before announcing

### C. Phase 2 readiness

- [ ] Digest used for ≥4 weeks
- [ ] Maintainers confirm top-N list matches intuition
- [ ] Documented false positives (#2518-style mis-stale)
- [ ] Security review for GitHub App / webhook host
- [ ] ADR or spec PR for external service scope

---

## 7. Step-by-step: raise the **proposal PR** (show comment / start discussion)

Use this for a **docs-only PR** first — no workflow files required in PR #1 if you want minimal review friction. Or include P0 digest workflow in the same PR if you want something runnable immediately.

### Step 1 — Branch

```bash
git fetch upstream develop   # or origin, per your remote setup
git checkout develop
git pull --rebase upstream develop
git checkout -b docs/prflow-phase1-proposal
```

### Step 2 — Add files (minimum)

```
.specify/reports/prflow-pr-triage-2026-04-23.md      # evidence (already exists)
.specify/reports/prflow-phase1-phase2-plan.md        # this file
```

Optional fourth file: `docs/prflow.md` — 1-page summary linking both reports (nice for reviewers who don’t open `.specify/`).

### Step 3 — Commit

```bash
git add .specify/reports/prflow-pr-triage-2026-04-23.md
git add .specify/reports/prflow-phase1-phase2-plan.md
git commit -m "docs: add PRFlow triage evidence and phase 1/2 plan"
```

Run `mvn spotless:apply` only if you touched Java (not needed for MD-only).

### Step 4 — Push

```bash
git push -u origin docs/prflow-phase1-proposal
```

### Step 5 — Open PR on GitHub

- **Base:** `develop`
- **Compare:** `docs/prflow-phase1-proposal`
- **Reviewers:** `@pmanko` + 1 other maintainer if known
- **Labels:** `docs`, `community` (if applicable)

### Step 6 — PR description (copy-paste)

```markdown
## Summary

Proposal to improve PR flow in OpenELIS (PRFlow), triggered by discussion that review cycles are a current blocker.

This PR is **evidence + plan only** — no runtime automation yet. It includes:
- Empirical triage of 253 open PRs (31 deep-analyzed)
- Recommended Phase 1 (GitHub Actions + daily digest)
- Phase 2 outline (service/app if Phase 1 isn’t enough)

## Key findings

- **253** open PRs; **96** with `merge conflict`, **33** with `stale`
- On a focused sample of **31** PRs: **29 had zero approvals**
- Bottleneck is **review routing + branch drift (`behind`)**, not conflict complexity
- **8** dependabot PRs are CI-green and could move after rebase; **3** need triage (log4j pair + one python bump)
- **2** stale PRs were approved then stuck in conflict — needs lifecycle state, not age-only stale bot

## Proposed Phase 1 (hybrid, lightweight)

1. Daily **queue digest** (top 15 PRs by score) — addresses burst review
2. **Auto-classify** labels (size / area / type)
3. **Dependabot ready** labeling when CI green
4. **Behind-branch nudge** when rebase is the only blocker

## Ask from maintainers

1. Is Phase 1 scope acceptable as follow-up PRs?
2. Where should the digest post — pinned issue vs Discussion vs Slack?
3. Policy on auto `update-branch` vs comment-only?
4. Auto-merge for dependabot patch PRs — yes/no?

## Artifacts

- [Triage report](.specify/reports/prflow-pr-triage-2026-04-23.md)
- [Phase 1/2 plan](.specify/reports/prflow-phase1-phase2-plan.md)

## Test plan

- [ ] Docs render correctly on GitHub
- [ ] Links resolve
- [ ] No secrets in diff
```

### Step 7 — Comment on the discussion thread (talk post)

Short reply you can paste under the original post:

```markdown
I ran a small triage on open PRs and opened a docs PR with the numbers + a concrete Phase 1 plan (Actions + daily digest, no big service yet).

Main takeaway: on a sample of 31 PRs, 29 had **zero approvals** — most are `behind` (need update branch), not conflicted. Happy to walk through the report if useful.

PR: <link>
```

### Step 8 — Follow-up PRs (after proposal discussion)

| PR | Contents |
| -- | -------- |
| PR 2 | `prflow-queue-digest.yml` only |
| PR 3 | `prflow-classify.yml` |
| PR 4 | `prflow-ready-dependabot.yml` + `prflow-behind-nudge.yml` |

Keeps review small and shows incremental delivery.

---

## 8. Single prompt for a future implementation window

Copy everything below into a new Cursor chat when ready to **implement** (not for the proposal PR):

```text
Implement PRFlow Phase 1 for DIGI-UW/OpenELIS-Global-2 per:
- .specify/reports/prflow-pr-triage-2026-04-23.md (evidence)
- .specify/reports/prflow-phase1-phase2-plan.md (plan)

Phase 1 only:
1. prflow-queue-digest.yml — cron, score formula in plan, post top 15 to issue (use placeholder issue number in comment)
2. prflow-classify.yml — labels from paths/title
3. prflow-ready-dependabot.yml — ready-to-merge when CI green, not dirty
4. prflow-behind-nudge.yml — comment when behind + CI green + no changes requested

Rules:
- Match permissions pattern of label-merge-conflict.yml
- if: github.repository == 'DIGI-UW/OpenELIS-Global-2'
- No auto-merge in v1
- No secrets in repo; use GITHUB_TOKEN in Actions
- git worktree list before editing

Do NOT build Phase 2 service. One PR or split per plan section 7 step 8.

After implementation, update prflow-phase1-phase2-plan.md checklist section B.
```

---

## 9. Total counts (quick reference)

| Item | Count |
| ---- | ----: |
| Open PRs (repo) | **253** |
| PRs deep-analyzed | **31** |
| Community ready-to-move (Section A) | **10** |
| Dependabot (Section B) | **11** |
| Stale reviewed (Section C) | **10** |
| Zero approvals in analyzed set | **29** |
| With approval in analyzed set | **2** |
| Dependabot CI-green candidates | **8** |
| Dependabot needs triage | **3** |
| Repo PRs with `merge conflict` label | **96** |
| Repo PRs with `stale` label | **33** |

---

## 10. Open questions for maintainers (carry into PR comments)

1. Digest destination: pinned issue number vs GitHub Discussions?
2. Auto `update-branch` — allowed by org policy?
3. Dependabot auto-merge — patch only, or never?
4. Exclude maintainer self-PRs from community digest?
5. Phase 2 trigger: what metric defines success of Phase 1?

---

*This document is the single source for “what to build next.” Implementation files (workflows, skills) are intentionally not included here — ship via follow-up PRs per Section 7.*
