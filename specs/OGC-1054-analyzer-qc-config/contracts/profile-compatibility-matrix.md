# OGC-1054 Profile Compatibility Proof Matrix

This contract assigns proof for preserving the established analyzer profile
system while moving catalog authority to Bridge. It is not a progress ledger.
Git, tests, and the owning PR hold execution evidence.

## Inputs

The unmodified profile documents at these paths are the initial blocking inputs:

- `projects/analyzer-profiles/astm/genexpert-astm.json`
- `projects/analyzer-profiles/file/fluorocycler-xt.json`

Their names and values are fixture data only. No validator, consumer, runtime
handler, UI component, or mock implementation may branch on either fixture name,
profile ID, manufacturer/model, analyzer code, or vendor value. Every proof below
must parameterize the same generic path used by any other profile.

Compatibility does not mean mechanically preserving every mapping row. A
behaviorally used field cannot disappear silently; an incorrect, duplicate,
unsupported, split, or alias row changes only through the E0 evidence-based
curation disposition.

## Layered Proof

| Checkpoint/layer            | Input and action                                                                                                            | Deterministic pass condition                                                                                                                                      | Cannot prove                                                   |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| F0 profile characterization | Load both full documents through `analyzer-profile-compatibility.test.js`; run metadata-only and hardcoded-source negatives | Both profile jobs are present; thin projections, copied defaults, profile-specific branches, and new boundary violations fail generically                         | Target Bridge schema, catalog persistence, or runtime behavior |
| BR-E0 contract              | Validate both full documents under the evolved protocol-discriminated schema and semantic validator                         | No communication/default field is dropped; generated metadata is separate; recognition is valid `RULES`/affirmed `NONE`; forbidden site/OE/QC content is rejected | OE consumption or catalog lifecycle                            |
| OE-E0 consumer              | Consume the same producer fixtures as profile ID/revision plus complete detail                                              | OE stores the pin, site values, and local bindings; no profile snapshot, `defaultConfigId`, classifier copy, fallback inference, or implicit revision move occurs | Bridge runtime or visible setup                                |
| BR-M1 catalog               | Seed, read, duplicate, publish, retire, and restart using the curated documents                                             | Catalog returns complete immutable revisions/defaults, retains referenced revisions, and runs no named special case                                               | OE default application or mock traffic                         |
| OE-M1 setup                 | Select each returned revision in RTL with a real router and save through the generic setup handler                          | Every visible/defaulted value comes from profile detail; URL/pin/site values persist; no frontend/server default constant supplies selected-profile behavior      | Protocol execution or end-to-end result flow                   |
| Analyzer mock and Bridge    | Derive mock traffic from each accepted revision and send through real ASTM/FILE transport                                   | Bridge uses the pinned profile for listener/parser/recognition behavior and emits expected normalized patient/control context without hidden fallback             | OE visible result/QC routing                                   |
| Assembled OE/Bridge/mock    | Create each analyzer through UI, inspect effective registration, emit traffic, and inspect the visible destination          | Form defaults, pin, registration, runtime traffic, and patient/control outcome agree for one unchanged candidate                                                  | Remote human acceptance                                        |

## Removal Gates

- F0 blocks new dependencies on `AnalyzerQcRule`, OE profile-copy/application,
  OE FILE watching, copied profile defaults, named profile special cases, and
  hidden control-classifier fallbacks.
- M1 removes selected-profile frontend/server constants and the OE-hosted
  profile authority only after assembled parity.
- M2 removes the complete `AnalyzerQcRule` runtime/UI/schema path.
- M4/G0 run full-repository absence guards and the assembled visible story.

Passing schema validation alone, comparing metadata-only projections, or replaying
an old video never satisfies this matrix.
