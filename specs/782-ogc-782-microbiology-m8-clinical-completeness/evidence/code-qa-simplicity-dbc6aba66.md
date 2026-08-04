# Code QA: Simplicity Review

- Feature commit: `dbc6aba66`
- `DIGI-UW/code-qa`: `30528d176bd128b4765242d130f38ca9fb85d7b8`
- Verdict: **lean for the completed clinical slices**

## Finding Resolved

| ID   | Finding                                                                                                                                                    | Simpler form                                                                                                                              | Result                                           |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| S-01 | `MicrobiologyQualificationDataService` was a production-scanned Spring bean despite having only manually constructed test callers and no runtime endpoint. | Keep the explicitly enabled builder in `src/test`, where it still creates data through application services but is excluded from the WAR. | Resolved in `28ba63ba6`; 4/4 focused tests pass. |

## Kept On Purpose

- Amendment, report-version/source, and identification-event records are
  essential append-only clinical history, not parallel reporting systems. The
  active report remains the standard Analysis/Result path.
- Separate AST attempts are domain records required to prevent overwriting;
  attempt type/source/reason fields are not speculative extension points.
- The worklist batch-loading change is retained because measured API evidence
  showed the prior relationship-loading cost. No cache or speculative index was
  introduced.
- `AmendmentHistoryPanel` and `AstAttemptTable` are direct feature components;
  no generic workflow framework, plugin system, or new state library was added.
- Lot behavior was not guessed or partially scaffolded while its product
  semantics remain contradictory.

No unused runtime fixture endpoint, duplicate report surface, alternate reagent
store, broad package upgrade, or unrelated refactor remains in the completed
slices.
