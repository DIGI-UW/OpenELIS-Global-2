# OGC-782 M1 + M2 UAT Contract In The Combined Review

## Review Target

- Site: `https://amr.openelis-global.org`
- Review overlay: the `Review` button on the AMR site
- M2 source branch: `feat/782-ogc-782-microbiology-m8-clinical-completeness`
- M2 source SHA: `0d963f3fe925c3d2bc90818e109ce4aafb030031`
- Current combined app branch:
  `feat/782-ogc-782-microbiology-m9-reference-mapping-admin`
- Current combined app target: `fe7ca789f4f9026e6a679e496a06c3e860da8c12`
- Current deployment ID: `20260804T200840Z-fe7ca789f4f9`
- Review-tooling SHA: `72eb003155db91f08a90d5e853e7811f86d3c642`
- Checklist revision:
  `c3a490ab422180d87ada093cf05a2cc727413a01bc6234c3217fb99c466e7c3c`
- Combined inventory: 13 stories, 33 steps, 32 required, 1 optional
- M2 exact-target live precheck: 2/2 passed
- M2 deployed lot journey: 2/2 passed; desktop screenshots inspected
- M2 visual evidence:
  [44.2-second MP4](https://amr.openelis-global.org/__review/evidence/ogc-782/m2/0d963f3f/walkthrough.mp4),
  [reviewed contact sheet](https://amr.openelis-global.org/__review/evidence/ogc-782/m2/0d963f3f/contact-sheet.png)
- Human UAT: pending

M1 and M2 are reviewed as one ordered clinical journey. Complete the M1 steps
through final release, then continue with the same case through the M2
correction workflow. Automated Playwright verifies the deployed identity,
checklist rendering, and stable navigation, but does not provide human
Pass/Fail/N/A rulings.

## Published Stories

| Story                                                | Milestone | Required steps                       | Optional steps |
| ---------------------------------------------------- | --------- | ------------------------------------ | -------------- |
| `AMR-S01` Find and route microbiology work           | M1        | `AMR-1`, `AMR-2`                     | -              |
| `AMR-S02` Work the seeded bacteriology case          | M1        | `AMR-3`, `AMR-4`, `AMR-5`            | -              |
| `AMR-S03` AST, critical communication, and reporting | M1        | `AMR-6`, `AMR-7`, `AMR-16`, `AMR-20` | -              |
| `AMR-S04` Shared-specimen reflection                 | M1        | -                                    | `AMR-21`       |
| `AMR-S05` Open a controlled correction               | M2        | `AMR-22`, `AMR-23`                   | -              |
| `AMR-S06` Preserve repeat and retest AST attempts    | M2        | `AMR-24`, `AMR-25`                   | -              |
| `AMR-S07` Release and verify corrected results       | M2        | `AMR-26`, `AMR-27`                   | -              |
| `AMR-S08` Review the workflow by keyboard            | M2        | `AMR-28`                             | -              |
| `AMR-S09` Trace bench consumable lots                | M2        | `AMR-29`, `AMR-30`, `AMR-31`         | -              |

The live combined overlay now includes M3, but the M1/M2 acceptance gate remains
these 19 required steps. `AMR-21` is an optional TB reflection and cannot block
M1 or M2 acceptance. `AMR-29` and `AMR-30` qualify policy-neutral traceability
only, as does `AMR-31`: `PRIMARY / SECONDARY` remains visible catalog role
metadata and is not interpreted as mandatory/optional/substitute policy.

## Fixture

The deployed fixture is created through the property-gated
`MicrobiologyUatScenarioService`; it does not use SQL, fixed primary keys, DAO
bypass, or a production fixture endpoint.

- Scenario key: `review-amr-microbiology-mvp`
- Accession: `UATMICRO01C82736AB`
- Primary bacteriology case: `a65d620c-c96b-4627-9d69-9c00ba310551`
- Sibling case: `3491532f-7dba-40f5-863b-7f4e3287d505`

The worklist is the supported entry point; case identifiers are recorded here
for evidence and diagnosis, not embedded in application or Playwright logic.
The fixture also creates expired and eligible culture-media lots plus eligible
AST-card lots through Inventory services; no lot identifier is fixed in a UI or
test contract.

## Reviewer Ruling

Piotr records Pass, Fail, or N/A and notes in the live combined Review overlay
for the 19 M1/M2 required steps. A failed step must identify the observed
behavior and the expected behavior. The downloaded Markdown/JSON report is the
acceptance record and is attached to PR #3972 or linked from this directory.
