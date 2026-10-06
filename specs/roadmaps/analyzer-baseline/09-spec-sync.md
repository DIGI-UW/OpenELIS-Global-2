# Step 9: Validate and sync the spec

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: `specs/analyzers/spec.md` describes the analyzer setup that landed, so
it can be read without this roadmap.

### Facts

- The spec is a short overview written before the code
  (`specs/analyzers/spec.md`, step 0). Its first paragraph names this roadmap
  as the authoritative plan while the setup is being built.
- This step runs after the whole stack and the Bridge and mock releases have
  landed, as its own PR on `develop`.
- Rules 1 to 19 are the decisions; the spec states them in plain words. Where a
  rule and the landed code disagree, the code is wrong and gets a fix PR; the
  spec is not bent to the code.

### Build

```
- [ ] T9.1 For each rule 1 to 19: read the landed code it governs; confirm the spec's sentence for it is true; add or correct a sentence where the spec is silent or wrong
- [ ] T9.2 Check every link in the spec (openelis-work paths, Bridge docs, docs/analyzers, the harness); fix or remove dead ones
- [ ] T9.3 Remove the "while that setup is being built" paragraph; the spec stands on its own
- [ ] T9.4 Read docs/analyzers/*.md against the code; fix or delete anything stale
- [ ] T9.5 Format cold; commit; PR on develop
```

### Verify

```bash
grep -n "being built\|authoritative plan" specs/analyzers/spec.md   # 0
```

### Done when

1. Every rule has a true sentence in the spec, checked against the landed
   code. (T9.1, read)
2. No dead link in the spec or `docs/analyzers`. (T9.2, T9.4)
3. The spec no longer refers to this roadmap as the plan in progress. (`grep`)
