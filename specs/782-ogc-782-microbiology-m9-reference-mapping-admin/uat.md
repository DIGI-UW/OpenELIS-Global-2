# M3 UAT Contract: Reference and Breakpoint Administration

These stories are additive to the existing AMR M1/M2 checklist. Human rulings
remain Pass / Fail / N/A with notes; automation is pre-UAT evidence only.

## M3-S01 - Organism and Antibiotic Vocabularies

- Open Admin and navigate to Microbiology Reference Data without pasting a URL.
- Search organisms, open one record, change a non-identity field, save, and
  confirm the bookmarkable list state is retained.
- Deactivate a synthetic organism after reading the impact warning; confirm it
  disappears from a new isolate picker while the existing UAT case still names it.
- Reactivate the organism and confirm it is selectable again.
- Repeat the edit/deactivate behavior for a synthetic antibiotic.

## M3-S02 - Versioned AST Panels

- Open an AST panel and inspect antibiotic order, tier, and report behavior.
- Publish a changed panel and confirm the visible version increments.
- Confirm a new AST setup offers the current version while the seeded existing
  run still shows its original panel/version.

## M3-S03 - Breakpoint Catalog Lifecycle

- Open Breakpoint Standards and inspect Active/Loaded/Archived explanations.
- Filter a standard's rule detail and reload the URL; confirm the same rows and
  filters return.
- Activate a loaded synthetic standard with an effective date and confirm the
  former version becomes Loaded.
- Confirm the seeded reviewed AST run keeps its original standard and S/I/R.

## M3-S04 - Safe Breakpoint Import

- Upload the synthetic mixed-validity CSV from the UAT fixture.
- Confirm valid/skipped counts and row-specific errors before applying.
- Apply valid rows, download rejected rows, and confirm the imported standard is
  Loaded rather than automatically Active.
- Re-import the same file and confirm unchanged rows are not duplicated.
- Edit an imported rule as a local correction, then re-import the same rule and
  confirm the correction remains visible while the import reports the collision.

## Requiredness

All M3 steps are required for M3 acceptance. They do not change the requiredness
of existing M1/M2 steps or the optional shared-specimen TB reflection.
