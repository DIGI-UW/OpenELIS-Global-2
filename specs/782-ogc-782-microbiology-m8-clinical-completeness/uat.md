# OGC-782 M1 + M2 Live UAT Contract

## Review Target

- Site: `https://amr.openelis-global.org`
- Review overlay: the `Review` button on the AMR site
- App branch: `feat/782-ogc-782-microbiology-m8-clinical-completeness`
- Deployed app SHA: `cd833663cedb147d534e0eea90b3de7bd3877946`
- Deployment ID: `20260804T054912Z-cd833663cedb`
- Review-tooling SHA: `3d52ac103853e349df017828689b788880631d21`
- Checklist revision:
  `00242ff2e232998d6bab03844de68975a8dd6ef4117fd81eb51af997db4d5afe`
- Automated live precheck: passed
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

The acceptance gate is 19 required steps. `AMR-21` is an optional TB
reflection and cannot block M1 or M2 acceptance. `AMR-29` and `AMR-30` qualify
policy-neutral traceability only, as does `AMR-31`: `PRIMARY / SECONDARY`
remains visible catalog role metadata and is not interpreted as
mandatory/optional/substitute policy.

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

Piotr records Pass, Fail, or N/A and notes in the live Review overlay for all
19 required steps. A failed step must identify the observed behavior and the
expected behavior. The downloaded Markdown/JSON report is the acceptance
record and is attached to PR #3972 or linked from this directory.
