# OGC-788 M1 And M2 UAT Contract

Grist `UAT_Meta`, `UAT_Stories`, and `UAT_Steps` remain the live source of
truth. This file records the repository-side contract that must match the AMR
Review overlay after publication.

## Published State

- Instance: `amr`
- Live checklist revision:
  `af9383f5a260695f022720cff23d2e919cecb742e41fbab16042077ed9b0375e`
- Stories: 16 (`AMR-S01` through `AMR-S16`)
- Required steps: 50
- Optional steps: 1 (`AMR-21`, TB shared-specimen reflection)
- Grist read: `https://grist.openelis-global.org/uat/amr.json`
- AMR overlay read:
  `https://amr.openelis-global.org/__review/uat-amr.json`

The two live reads return the same revision. Publication preserved all 14 prior
stories and 39 prior step keys, then added `AMR-S15`, `AMR-S16`, and exactly
`AMR-51` through `AMR-62`.

## M1 Story

| Field      | Value                                                                                                                                            |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Key        | `AMR-S15`                                                                                                                                        |
| Title      | OGC-788 M1 - Manage and use shared phrases                                                                                                       |
| Order      | 14                                                                                                                                               |
| Version    | 1.0                                                                                                                                              |
| Jira       | OGC-788                                                                                                                                          |
| PR         | https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3990                                                                                           |
| Mock       | https://digi-uw.github.io/openelis-work/designs/microbiology/m-08-macro-library.html                                                             |
| User story | As a laboratory administrator and bench user, I can maintain shared phrases and expand eligible text in context without hiding what is recorded. |

## Required Steps

### AMR-51 - Restore administration state

**Do:** Open Macro Library from Admin Management, search for `.uat_ng24`,
select All contexts, All statuses, and Code A-Z, then reload the resulting URL.

**Expect:** The Macro Library breadcrumb, selected controls, matching phrase
row, and complete canonical URL return unchanged after reload.

**Route:**
`/admin/MacroLibrary?q=.uat_ng24&context=all&status=all&sort=code%3Aasc&page=1&pageSize=20`

### AMR-52 - Maintain the reusable phrase

**Do:** Edit `.uat_ng24`, retain the text `No growth at 24 hours`, select
Culture activity, Clinical history, and Antibiotic exposure, keep it Active,
and save.

**Expect:** The saved row remains searchable as an active phrase and reopening
it shows the same text and three field contexts.

**Route:** The canonical Macro Library URL from AMR-51.

### AMR-53 - Expand in culture work

**Do:** Open the seeded bacteriology case, go to Setup, type
`Culture observation: .uat_ng24` in Activity note, and choose the suggestion
with the keyboard.

**Expect:** Only the shortcut is replaced, the prefix is preserved, the
expanded phrase remains editable, and focus returns to Activity note.

**Route:** `/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest`

### AMR-54 - Preserve the recorded meaning

**Do:** Save the setup activity, open Timeline, copy the case URL, and reload
it.

**Expect:** Timeline still shows
`Culture observation: No growth at 24 hours` as ordinary text, and the same
case section returns from the copied URL.

### AMR-55 - Reuse field contexts

**Do:** Open order entry, select the seeded UAT microbiology culture test, and
expand `.uat_ng24` once in Clinical history and once in Antibiotic exposure.

**Expect:** Both fields offer the active phrase because their contexts were
selected, and each field receives independent editable expanded text.

**Route:** `/SamplePatientEntry`

### AMR-56 - Review responsive behavior

**Do:** At a narrow mobile-width window, inspect the filtered Macro Library and
then open the seeded case Activity note suggestion for `.uat_ng24`.

**Expect:** Shortcut, phrase, status, and row action remain readable without
horizontal table scrolling, and the suggestion does not cover the Activity
note or Start inoculation action.

**Route:** The canonical Macro Library URL from AMR-51.

## M2 Story

| Field      | Value                                                                                                                                               |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Key        | `AMR-S16`                                                                                                                                           |
| Title      | OGC-788 M2 - Administer and export shared phrases                                                                                                   |
| Order      | 15                                                                                                                                                  |
| Version    | 1.0                                                                                                                                                 |
| Jira       | OGC-788                                                                                                                                             |
| PR         | https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3992                                                                                              |
| Mock       | https://digi-uw.github.io/openelis-work/designs/microbiology/m-08-macro-library.html                                                                |
| User story | As a laboratory administrator, I can safely administer several shared phrases together and export the effective library for review and portability. |

## M2 Required Steps

### AMR-57 - Restore the bulk-administration view

**Do:** Open Macro Library, search for `.uat_bulk`, select All contexts, All
statuses, and Code A-Z, then reload the resulting URL.

**Expect:** The filtered library and all selected controls return unchanged from
the canonical URL. Prepare two local active phrases named `.uat_bulk_a` and
`.uat_bulk_b` through the editor if they are not already present.

**Route:**
`/admin/MacroLibrary?q=.uat_bulk&context=all&status=all&sort=code%3Aasc&page=1&pageSize=20`

### AMR-58 - Review an explicit bulk action

**Do:** Select `.uat_bulk_a` and `.uat_bulk_b`, choose Deactivate, inspect the
confirmation, and cancel once before opening it again.

**Expect:** The confirmation names the action and both phrases. Cancel changes
nothing and returns focus to the library; reopening presents the same selection.

### AMR-59 - Deactivate and restore several phrases

**Do:** Confirm Deactivate, reload the canonical URL, select the same two rows,
and confirm Activate.

**Expect:** Both rows become inactive together, remain inactive after reload,
and then become active together. No partial update is visible.

### AMR-60 - Export a reviewable effective library

**Do:** Choose Export CSV and open the downloaded file.

**Expect:** The UTF-8 CSV has one header row, is ordered by code, includes
contexts, active state, and source/provenance, and contains no database IDs or
audit actor identifiers.

### AMR-61 - Restrict irreversible removal

**Do:** Select only `.uat_bulk_b`, choose Remove local phrases, inspect the
danger confirmation, and confirm. Leave `.uat_bulk_a` active for repeat review.

**Expect:** The confirmation identifies irreversible local removal,
`.uat_bulk_b` disappears after reload, and `.uat_bulk_a` remains available.

### AMR-62 - Review responsive batch behavior

**Do:** At a narrow mobile-width window, search for `.uat_bulk`, select the
remaining phrase, and open a bulk confirmation.

**Expect:** The phrase, status, compact selected-count state, and confirmation
remain readable without horizontal page overflow; focusable controls are not
hidden from assistive technology.

## Publication Result

- [x] Added both stories and all twelve required steps through Grist authoring.
- [x] Updated the AMR checklist title and intro to name OGC-788 M1 and M2.
- [x] Verified both `https://grist.openelis-global.org/uat/amr.json` and
      `https://amr.openelis-global.org/__review/uat-amr.json` return `200`, have the
      same revision, retain the 39 prior steps, and add exactly `AMR-51` through
      `AMR-62` under `AMR-S15` and `AMR-S16`.
- [x] Recorded automated evidence separately from human Pass/Fail/N/A marks.
- [x] Published an exact-SHA AMR deployment and verified its target metadata,
      application smoke checks, and live overlay. Deployment identity remains
      authoritative at `https://amr.openelis-global.org/__review/target.json` and
      in the implementation PR to avoid stale duplicated runtime state here.
- [ ] Human Pass/Fail/N/A review remains pending.
