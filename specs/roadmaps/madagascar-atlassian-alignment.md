# Madagascar Atlassian Alignment Notes

## Purpose

This note captures the Atlassian-backed findings that changed the roadmap. It
exists so future stream owners can see which assumptions were corrected and why.

## Sources Reviewed

### Confluence

- Tracker page `1097531396` - `OpenELIS Global - Analyzer Integration Tracker`
- Workplan page `1111523331`
- Meeting notes page `1127153666`

### Jira

- Epic `OGC-304`
- ASTM: `OGC-337`, `OGC-335`, optional `OGC-336`
- HL7: `OGC-325`, `OGC-326`, `OGC-327`
- File/shared infrastructure: `OGC-324`, `OGC-329`
- File analyzer stories: `OGC-344`, `OGC-348`, `OGC-350`, `OGC-351`, `OGC-417`,
  `OGC-418`

## Key Corrections

### 1. `013` And `014` Are Coordination IDs, Not Jira Tickets

The live Atlassian structure does not contain one clean GenericHL7 Jira story
and one clean GenericFlatFile Jira story.

Instead:

- HL7 work is split across listener infrastructure and analyzer-specific issues
- File work is split across shared upload/configuration infrastructure and
  analyzer-specific import issues

Impact on roadmap:

- Keep `013` and `014` as stream coordination identifiers only
- Spawn issue-specific implementation branches later

### 2. `LA2M Central` Is Not An Analyzer

The earlier roadmap left `LA2M Central` in the unresolved analyzer list.
Atlassian evidence does not support that.

Confluence uses `LA2M` as a laboratory/site label:

- Workplan: "validated on high priority analyzers for LA2M and HRJA"
- Workplan: lists `LA2M Toliara` and `LA2M Androhibe - Palu` as lab/site
  locations
- Meeting notes: "final list of analyzers shared today for Phase 2 for L2AM and
  UCP pilots"

Impact on roadmap:

- Remove `LA2M Central` from the analyzer scope
- Treat it as site/workstream context, not analyzer integration work

### 3. Wondfo Has A Real Jira Conflict, Not Just Missing Data

The tracker row for Wondfo Finecare FS-205 presents:

- ASTM as one path
- CSV as a fallback path

But the Jira issue descriptions do not align cleanly with that wording:

- `OGC-344`: CSV flat-file import
- `OGC-345`: ASTM real-time connection, explicitly blocked by `OGC-344`

Impact on roadmap:

- Do not force Wondfo into one stream yet
- First resolve whether the intended phase order is CSV-first or ASTM-first

### 4. FluoroCycler XT And Attune CytPix Were Over-Confirmed

The earlier roadmap was too optimistic about both analyzers as file anchors.

Atlassian says:

- `OGC-351` FluoroCycler XT: native `.at` files are not parseable; CSV/PDF or
  documented LIMS path still needed
- `OGC-350` Attune CytPix: instrument is FCS-only; middleware or preprocessing
  is likely required

Impact on roadmap:

- Keep both in the file stream
- Do not describe either one as a ready GenericFlatFile profile

### 5. QuantStudio Is The Strongest File Anchor

`OGC-348` is validated against real Madagascar files and explicitly references
LA2M Madagascar exports.

Impact on roadmap:

- QuantStudio 5 / 7 Flex becomes the file-stream proving anchor

### 6. HL7 Is Stronger For BS-200 Than For BS-300

Atlassian clearly backs:

- `OGC-325` HL7 listener infrastructure
- `OGC-326` BS-series HL7 adapter with BS-200 explicitly included
- `OGC-327` BC-5380 HL7 adapter

But the evidence for BS-300 is weaker than the earlier roadmap implied.

Impact on roadmap:

- BS-200 stays confirmed HL7
- BS-300 remains likely HL7, but not fully validated by the same ticket set
- Shared `BS-200/BS-300` profile remains a design assumption, not a settled fact

## Issue Bundle Snapshot

### ASTM Bundle

| Issue     | Role                                                  |
| --------- | ----------------------------------------------------- |
| `OGC-337` | Generic ASTM/profile configuration work               |
| `OGC-335` | GeneXpert ASTM proving path                           |
| `OGC-336` | GeneXpert HL7 alternative path, not primary for `012` |

### HL7 Bundle

| Issue     | Role                                                         |
| --------- | ------------------------------------------------------------ |
| `OGC-325` | Shared HL7 MLLP listener                                     |
| `OGC-326` | Mindray BS-series HL7 adapter, strongest evidence for BS-200 |
| `OGC-327` | Mindray BC-5380 HL7 adapter                                  |
| `OGC-336` | Optional GeneXpert HL7 alternative                           |

### File Bundle

| Issue     | Role                                                   |
| --------- | ------------------------------------------------------ |
| `OGC-324` | Upload/review UI with slot-based preview system        |
| `OGC-329` | File analyzer configuration and watcher/admin behavior |
| `OGC-344` | Wondfo CSV import                                      |
| `OGC-348` | QuantStudio 5 / 7 Flex import                          |
| `OGC-350` | Attune CytPix import path                              |
| `OGC-351` | FluoroCycler XT import path                            |
| `OGC-417` | Tecan Infinite F50 import                              |
| `OGC-418` | Multiskan FC import                                    |

## Architecture Tension To Keep Visible

Your requested direction is a plugin-owned GenericFlatFile path with OpenELIS
holding only instance-specific setup.

The current Atlassian file stories still assume several core-owned concepts:

- File import as a first-class protocol in the OpenELIS admin UI
- Shared upload and review UI in core
- Shared watcher/configuration behavior in core
- A static preview registry compiled into the main bundle

This is not a reason to abandon the plugin-owned goal. It is a reason to force
an explicit architecture reconciliation before implementation starts.

## Recommended Stream Consequences

### ASTM

- Proceed as direct recovery of `012`
- Keep GeneXpert ASTM as the proving path

### HL7

- Start from `specify`, but include the existing Jira, Confluence, repo profile,
  and GenericHL7 documentation context directly in the prompt
- Use the existing Jira bundle as the source of truth
- Create issue-specific implementation branches after the coordination branch
  freezes the sequence

### File Import

- Start from `specify`, but include the existing Jira, Confluence, repo code,
  UI, and design-reference context directly in the prompt
- Explicitly decide how much of `OGC-324` and `OGC-329` survives unchanged if
  the file parser boundary is pushed outward into plugins
- Treat QuantStudio as the best anchor and the other file analyzers as
  progressively riskier

## Open Questions Still Worth Escalating

- Is the Wondfo Phase order actually CSV-first or ASTM-first?
- Is the user-provided `BS-300` exactly in scope, and if so, does it share the
  same documented HL7 interface family as the current BS-series Jira spec?
- Does FluoroCycler XT have a supported export format that avoids direct `.at`
  parsing?
- Does Attune CytPix have a viable CSV/statistics export path, or is middleware
  mandatory?
