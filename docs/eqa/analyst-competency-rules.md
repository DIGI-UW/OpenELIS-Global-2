# Analyst competency rules (FR-V2.3-06)

How OpenELIS turns EQA events into the Competent / Under review / Not competent
assertion on **EQA Oversight → Analyst Competency**, and why it resolves the FRS
the way it does.

Owner: `EQAAnalystCompetencyServiceImpl`. Asserted by
`EQAAnalystCompetencyIntegrationTest`.

## What the rollup reads

Two sources over a trailing 12 months, keyed on the analyst:

1. `eqa_analyst_competency_event` — the append-only log (FR-V2.1-22). Scoring,
   missed deadlines, NCE escalation and triage dismissal all write here through
   `EQAAnalystCompetencyService.record`.
2. `eqa_participant_result` rows with an `assigned_analyst_id`, which the log
   does not already speak for — in practice the acceptable ones, since scoring
   writes an event for every other verdict.

A result covered by an event is read from the event: FR-V2.3-06 makes the event
canonical.

Nothing is stored. The bands are derived on every read, so they cannot go stale
and there is no second writer to keep in step.

## One sample is one fact

Rows that share a `participant_result_id` collapse to a single assessable fact
before any counting. This matters because an unacceptable score that is then
escalated writes **two** events about **one** sample; counting both would fail
the analyst twice for it.

**The latest statement about a sample decides how it counts.** Scoring is the
instrument's verdict; a triage verdict is the supervisor's finding _about_ that
verdict, and it is recorded afterwards, so it replaces the score's counting
decision rather than being OR-ed with it. FR-V2.3-06 says as much — "the event
is the canonical row" — and one canonical row per result is not the same thing
as the OR across every row about it.

An escalation is not a counting decision: the score it escalates is already the
evaluable row, so it contributes the failure and the open-non-conformity flag
and leaves the denominator alone.

The collapsed fact carries the **worst** outcome of its rows and the latest of
their dates, so the evidence table still shows what happened. An event with no
result behind it (a cross-cycle event) is its own fact.

A sample's whole history, then:

| Sample's history                                                            | In `evaluable_n` | In `failure_n`              |
| --------------------------------------------------------------------------- | ---------------- | --------------------------- |
| scored acceptable, no triage                                                | yes              | no                          |
| scored badly, no triage                                                     | yes              | yes                         |
| scored badly → dismissed equipment / pending re-test / acceptable on review | no               | no                          |
| scored badly → dismissed transcription / other                              | yes              | yes (once, not twice)       |
| scored badly → escalated                                                    | yes              | yes, plus the open-NCE band |

Two events about one sample very often share a date — a bad score and the triage
answering it usually land the same day — so triage verdicts are ordered by date
**and then by event id**.

## Counted, failing, excused

From FR-V2.1-22's "counts against the analyst" column:

| Event                            | In `evaluable_n` | In `failure_n` |
| -------------------------------- | ---------------- | -------------- |
| `UNACCEPTABLE_SCORE`             | yes              | yes            |
| `QUESTIONABLE_SCORE`             | yes              | yes            |
| `EXTERNAL_MISSED_DEADLINE`       | yes              | yes            |
| `IN_HOUSE_MISSED_DEADLINE`       | yes              | yes            |
| `DISMISSED_TRANSCRIPTION`        | yes              | yes            |
| `DISMISSED_OTHER`                | yes              | yes            |
| `ESCALATED_TO_NCE`               | no               | yes            |
| `DISMISSED_EQUIPMENT`            | no               | no             |
| `DISMISSED_ACCEPTABLE_ON_REVIEW` | no               | no             |
| scored `ACCEPTABLE` result       | yes              | no             |

`ESCALATED_TO_NCE` fails without being evaluable because the score it escalates
is already the evaluable row — and the de-duplication above folds the two back
into one sample anyway.

`DISMISSED_EQUIPMENT` and `DISMISSED_ACCEPTABLE_ON_REVIEW` leave **both**
totals. Equipment fault is not the analyst's, and acceptable-on-review means
triage found nothing to answer for.

Because these are triage verdicts, they excuse the sample they dismiss —
including one that had already failed, which is the only case that arises in
production: a queue row exists only after a bad score. An excused sample
therefore **does** pull the denominator down, and that is the intended outcome
rather than a side effect. A broken analyser means less evidence about the
person, not more, so an analyst whose only failures were equipment faults bands
**Under review** for thin evidence rather than Competent. FR-V2.3-06's own
worked example is exactly that case: one unacceptable, three equipment
dismissals and two acceptables gives `evaluable_n` 3 and `failure_n` 1 — Under
review by the evidence floor, which is the answer the example states. Counting
the excused samples would give `evaluable_n` 6 and `failure_n` 1, which is
Competent, and the example is built to show the denominator falling below four.

## The bands, in precedence order

The FRS band table's rows overlap, and its final row ("otherwise → Competent")
would claim any analyst the earlier rows skipped. They are evaluated severest
first — the only ordering that cannot assert competence over an unanswered
failure:

1. **Not competent** — any `ESCALATED_TO_NCE` in the window whose non-conformity
   is still open (`nc_event.status` not `Closed`/`Completed`).
2. **Under review** — `failure_n ≥ 2`.
3. **Under review** — `evaluable_n < 4`. Insufficient evidence is not a pass.
4. **Competent** — everything else, which is `evaluable_n ≥ 4` and
   `failure_n ≤ 1`.

### Which rule fired travels with the band

Rows 2 and 3 both produce **Under review**, and they mean opposite things: one
analyst has failed twice, the other has not been given enough work to judge. A
reviewer who sees only the band cannot tell a performance concern from a gap in
the evidence, and the two call for opposite actions.

So `band()` emits a `reason` beside every `status`:

| `reason`                | Band          | What the reviewer should do                                                  |
| ----------------------- | ------------- | ---------------------------------------------------------------------------- |
| `OPEN_ESCALATION`       | Not competent | Close the non-conformity.                                                    |
| `REPEATED_FAILURE`      | Under review  | Review the failed results; raise a non-conformity or dismiss them on triage. |
| `INSUFFICIENT_EVIDENCE` | Under review  | Assign more proficiency testing samples. Nothing is wrong with the analyst.  |
| `MEETS_EVIDENCE`        | Competent     | Nothing.                                                                     |

**The stored status is not split.** `reason` is derived on every read, like the
band itself, so there is no second value to keep in step and no migration.

The analyst's headline row carries `statusReasons`: one entry per analyte
sitting at the headline band, each with **that analyte's** evaluable and failure
counts. The analyst's own totals span every analyte and are not the denominator
any rule fired on, so quoting them beside a rule would be arithmetic that does
not add up.

The rollup also publishes `windowStart`, `windowEnd`, `windowMonths` and
`evidenceFloor`, so the page states the window and the floor it was judged
against rather than repeating the constants in copy.

### Three clauses deliberately not implemented

- **"2+ consecutive `questionable_score`"** (FRS, band table row 4). Every
  questionable sample is already a failure, so two of them satisfy
  `failure_n ≥ 2` on the same line. Coding it would add a branch that can never
  decide anything.
- **"most recent scored result is acceptable"** (FRS, band table row 1). It only
  ever distinguishes two ways of reaching Competent, both of which the rules
  above already reach. The most recent verdict is still shown on the page — it
  is information, not a gate.
- **The mirrored write to `analysis`** (FR-V2.3-04: "`assigned_analyst_id` is
  written to the `analysis` row AND mirrored to
  `eqa_participant_result.assigned_analyst_id`"). Only the participant-result
  column is written, and `analysis.assigned_analyst_id` is not added. The column
  does not exist today, and adding it would put a second copy of the same fact
  on a core clinical table that nothing reads — every consumer of this data, the
  rollup included, goes through `eqa_participant_result`. Two writable copies of
  one attribution is exactly the drift this card was told to avoid. If a non-EQA
  consumer ever needs the analyst on the analysis row, add the column then and
  derive it, rather than writing it twice from here.

## Grain

Bands are computed **per analyte per analyst**, which is what competence is
claimed for. The headline band on the analyst's row is the **worst** of their
analytes: one analyte under review does not make a competent analyst.

The FRS says "for each `(analyst_id, scheme_id)` pair" in one paragraph and "per
analyte × analyst" in the next. Analyte grain wins because that is what ISO
15189 §6.2.3 competence attaches to, and because the scheme is already visible
in the evidence table under each row.

## Window

Trailing 12 months from today. A result's date is its score date, else its
submission date, else the cycle's planned end — a scored row with no date
anywhere would otherwise fall out of every window. Events use `event_date`.

## How an analyst gets onto a result

Three writers populate `eqa_participant_result.assigned_analyst_id`:

1. **Standard result entry** (FR-V2.3-04). When the sample's cycle belongs to a
   scheme with `per_analyst = true`, the grid grows an **Analyst (EQA)** column.
   On save, `EQACycleSubmissionService.assignAnalyst` records the choice —
   opening the draft participant-result row if the cycle sweep has not written
   it yet, and leaving a scored row alone.
2. The participant-result draft endpoint, which accepts `assignedAnalystId`.
3. The in-house blinding wizard's analyst assignment.

Two things about the result-entry path are easy to get wrong:

- **The scheme flag is never read off the posted row.**
  `TestResultItem.eqaPerAnalyst` is populated server-side so the grid can
  render, but it is not a bound field: a modified row replaces its session copy
  wholesale, so anything the binder does not accept arrives at its default. Both
  the required-analyst rule and the write re-derive the scheme from the sample.
  A rule a client can switch off by omitting a field is not a rule.
- **An unmapped analyte silently records nothing.** The analyte is resolved from
  the result row, then the enrollment's test map, then `test_analyte`. If all
  three are empty there is nothing to file the analyst against; that case is
  logged with the test id, and the fix is to map the test to its scheme analyte
  on the enrollment.
