# Iterate Plan

When the user invokes `/iterate-plan` (optionally with arguments), perform an
**evidence-based plan iteration** that evaluates the existing plan against
current signals, identifies stale sections, and produces an updated plan.

This command runs **in plan mode** (read-only except the plan file). It is the
proper way to update a plan between iterations — it prevents anchoring bias,
context drift, and silent carry-forward of stale decisions.

## User Input

```text
$ARGUMENTS
```

Support these patterns:

- `/iterate-plan` → Full freshness evaluation + rewrite of current plan
- `/iterate-plan --zero-based` → Ignore existing plan entirely; derive fresh
  from goals + principles + current state, then diff against old plan
- `/iterate-plan --checkpoint` → Quick freshness check only; report stale
  sections without rewriting
- `/iterate-plan "focus on X"` → Iterate with a specific focus area or
  user-provided direction

## Why This Command Exists

LLM anchoring bias research (Nguyen et al., 2024) shows that stronger models
exhibit MORE consistent anchoring to initial plan content, not less. Plans
presented as authoritative context are treated as ground truth.
Chain-of-thought, reflection prompts, and "ignore the anchor" instructions all
fail to mitigate this. The only effective strategy is **dual-anchor prompting**
— explicitly presenting competing perspectives (old plan vs. new feedback vs.
principles) and forcing evaluation between them.

Without structured iteration, plans drift toward:

- **Append-only accumulation**: New sections added, stale sections preserved
- **Sunk-cost anchoring**: Effort already spent on a section makes it feel valid
- **Legacy carry-forward**: Decisions from iteration N survive to iteration N+5
  without re-evaluation, pulling implementation toward outdated designs

## Signal Hierarchy (non-negotiable)

When signals conflict, higher-ranked signals ALWAYS win. The plan file is the
LOWEST authority — it must justify its continued existence against all higher
signals.

| Rank | Signal                     | Source                            | Example                            |
| ---- | -------------------------- | --------------------------------- | ---------------------------------- |
| 1    | **User's latest feedback** | Current conversation              | "Why are we keeping legacy code?"  |
| 2    | **Project constitution**   | `.specify/memory/constitution.md` | Principle X: Legacy Code Removal   |
| 3    | **Current codebase state** | `git diff`, file reads            | Bridge already handles CSV parsing |
| 4    | **Empirical evidence**     | CI results, test output, logs     | E2E tests pass locally with bridge |
| 5    | **Existing plan sections** | Plan file                         | "Defer FIC removal to follow-up"   |

A plan section that conflicts with signal rank 1-4 is **STALE by definition**,
regardless of how much work went into it.

## Workflow

### Phase 0: Gather Current Signals (read-only)

Before touching the plan file, collect all current signals. Do NOT read the
existing plan yet — this prevents anchoring.

**Checkpoint 1 — User feedback:** Scan the recent conversation for:

- Direct corrections ("why are we keeping X?", "no, remove it")
- Expressed frustrations ("this is legacy", "we built the same thing twice")
- Stated goals ("I want green CI and demo videos")
- Architectural direction ("bridge owns parsing", "OE owns config")

Summarize in 3-5 bullet points: "The user has told us:"

**Checkpoint 2 — Project principles:** Read `.specify/memory/constitution.md`
and extract principles relevant to the current work. Specifically check:

- Any NEW principles added during this session
- Any principles the user referenced in feedback
- Any principles violated by the existing plan

Summarize: "The constitution requires:"

**Checkpoint 3 — Current codebase state:** Run quick reads/greps to understand
what actually exists now:

- `git diff --stat` (what's changed)
- Key file existence checks
- CI status if relevant

Summarize: "The codebase currently has:"

**Checkpoint 4 — Empirical evidence:** Check test results, CI status, local
verification:

- Do tests pass?
- What does CI say?
- What works locally?

Summarize: "Evidence shows:"

### Phase 1: Read and Classify Existing Plan

NOW read the existing plan file. For **each section**, apply the five freshness
questions:

| Question               | What it checks                                   | STALE if...                                          |
| ---------------------- | ------------------------------------------------ | ---------------------------------------------------- |
| **Source test**        | Where did this decision come from?               | Source was an early assumption, not evidence         |
| **Recency test**       | When was this last validated?                    | Not validated since user gave contradicting feedback |
| **Contradiction test** | Does any rank 1-4 signal contradict this?        | User said "remove it" but plan says "defer"          |
| **Necessity test**     | Would you write this section from scratch today? | No — you'd write something different                 |
| **Dependency test**    | What breaks if this section is removed/changed?  | Nothing meaningful, or only other stale sections     |

Classify each section as:

- **VALID** ✓ — Passes all 5 tests. Keep with one-line justification.
- **STALE** ✗ — Fails contradiction or necessity test. Delete or rewrite.
- **PARTIAL** ⚠ — Core intent valid but details outdated. Rewrite details.
- **NEW NEEDED** 🆕 — Gap identified by rank 1-4 signals. Write new section.

### Phase 2: Produce Freshness Report

Before rewriting, present the classification to the user:

```markdown
## Plan Freshness Report

### Signals Collected

- **User feedback**: [3-5 bullets]
- **Constitution**: [relevant principles]
- **Codebase state**: [key facts]
- **Evidence**: [test/CI results]

### Section Classification

| Section            | Status    | Reason                                           |
| ------------------ | --------- | ------------------------------------------------ |
| Phase 1: ...       | ✓ VALID   | Aligns with user direction and passes all tests  |
| Phase 2: ...       | ✗ STALE   | User explicitly said "remove, don't defer"       |
| Phase 3: ...       | ⚠ PARTIAL | Goal valid, but approach contradicts Principle X |
| [new] Phase 4: ... | 🆕 NEW    | Required by user feedback, not in current plan   |

### Conflicts Detected

- Plan says "defer FIC removal" but user said "why are we creating issues?"
- Plan preserves OE readers but Principle X says "never extend legacy"
```

If `--checkpoint` was passed, stop here. Otherwise proceed to Phase 3.

### Phase 3: Rewrite Plan (Dual-Anchor Method)

For each section that is STALE or PARTIAL, apply **dual-anchor prompting**:

1. State the OLD plan decision explicitly: "The previous plan said: ..."
2. State the CONTRADICTING signal explicitly: "But the
   user/constitution/evidence says: ..."
3. Derive the NEW decision from the higher-ranked signal
4. Write the new section

For NEW NEEDED sections:

1. State the gap: "No plan section addresses: ..."
2. State the signal that requires it: "The user said: ..."
3. Write the section derived from first principles

For VALID sections:

1. Preserve with a one-line justification: "Kept because: [reason]"
2. Do NOT expand, elaborate, or "improve" valid sections — stability is a
   feature

**Critical anti-pattern to avoid**: Do NOT merge old and new content. If a
section is STALE, replace it entirely. Do not try to "update" it by modifying a
few words — anchoring bias will preserve the old structure and framing.

### Phase 4: Validate Rewritten Plan

Before presenting:

- Verify no section references deleted/stale concepts from the old plan
- Verify every section traces to a rank 1-4 signal
- Verify no "defer to follow-up" or "create issue for later" unless the user
  explicitly approved deferral for that specific item
- Verify the plan is self-consistent (no section contradicts another)

### Phase 5: Present for Approval

Write the updated plan to the plan file and call ExitPlanMode.

## Zero-Based Replan (`--zero-based`)

When `--zero-based` is passed, skip Phase 1-2 entirely. Instead:

1. Rename the existing plan file to `{name}.previous.md`
2. Start from a blank plan file
3. Using ONLY the Phase 0 signals (user feedback, constitution, codebase,
   evidence), write a complete plan from scratch
4. After writing, diff against `{name}.previous.md`
5. Report what changed and why
6. Delete `{name}.previous.md`

This is the nuclear option for when plan drift is severe. Use when:

- Multiple iterations have accumulated stale sections
- User expresses frustration with the plan's direction
- The plan's assumptions no longer match reality

## Carryover Rules

Items from the old plan that are "in progress" or "done" get special handling:

- **DONE items**: Move to a "Completed" section at the top. Do not re-evaluate.
- **IN PROGRESS items**: Re-evaluate against freshness tests. In-progress does
  NOT grant immunity from staleness — sunk cost is not a valid justification.
- **NOT STARTED items**: Full freshness evaluation. No carry-forward without
  justification.

## Safety Rules

- **Never auto-carry sections**: Every preserved section needs explicit
  justification traced to a rank 1-4 signal.
- **Never expand valid sections**: Anchoring bias manifests as "improving"
  sections that should stay stable. If it's valid, leave it alone.
- **Never defer what the user said to do now**: If the user said "remove X" and
  the plan says "track X for later," the plan is wrong. The user's instruction
  is rank 1.
- **Never treat plan history as authority**: "The plan has always said X" is not
  a justification. Plans are hypotheses, not commitments.
