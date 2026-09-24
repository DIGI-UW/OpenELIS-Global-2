# Workplan and EQA orders

How blinded in-house EQA panels and participant EQA orders appear on the
Workplan, and the one rule that keeps blinding intact.

## What the Workplan shows

An in-house blinded panel (EQA V2.4, OGC-612) is distributed as standard orders.
Each order's accession number is the panel sample's blind code
(`IH-{panel}-{nn}`), so the Workplan lists the order under that code in the
sample column, exactly as it lists any other order. Nothing about the row
reveals the panel, the analyte's target value, or the acceptance range: those
are sealed and only the unblind step reads them.

An EQA order carries the EQA badge (`Workplan.jsx`, the same `EQABadge` the
result-entry page renders). The badge marks the row as proficiency-testing
material; it does not say which panel or scheme it belongs to.

The analyst assigned to a blinded sample sees it in the Workplan and on the
result-entry page like any other work and enters the result through the standard
pipeline. Scoring happens later, at unblind, against the sealed target (see
`docs/eqa/analyst-competency-rules.md` for what a scored result means for the
analyst).

## The rule: never add an "exclude EQA" filter

FR-V2.4-15 requires blinded orders to sit among routine work. The Workplan must
not grow a filter, tab, or default that hides EQA rows: an analyst who could
tell a proficiency sample from a patient sample by where it appears would defeat
the blinding. Filters that already exist (test section, priority, date) apply to
EQA rows the same way they apply to everything else.

The EQA badge is deliberately the only EQA-specific element on the row. It
exists so a supervisor reviewing the Workplan can account for the material; it
stays acceptable because the analyst working the row is also the one who will be
scored on it, so knowing it is EQA material grants no advantage on the value.

## Unblinding and timing

`eqa_panel.unblind_date` is a date, not a date-time. The scheduler unblinds a
distributed panel on its first pass after midnight of that date, or a holder of
`qa.eqa.inhouse.unblind` does it earlier with **Unblind now**. Results entered
after the unblind are still scored, but the participant result is flagged
`MISSED_DEADLINE` and the analyst receives an `IN_HOUSE_MISSED_DEADLINE`
competency event. The Workplan does not change at unblind; the orders stay where
they are until they finish through the normal result and validation steps.

## Where to look

- `frontend/src/components/workplan/Workplan.jsx`: the EQA badge on a row.
- `frontend/src/components/eqa/InHouse/`: the blinding wizard that creates the
  orders.
- `src/main/java/org/openelisglobal/eqa/service/EQABlindingServiceImpl.java`:
  blind-code generation, the order writes, and scoring at unblind.
- `src/main/java/org/openelisglobal/eqa/scheduler/EQADeadlineAlertScheduler.java`:
  the scheduled unblind pass.
