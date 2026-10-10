# Step 0: One analyzer spec folder and this roadmap

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: one OGC-agnostic analyzer folder holds a short overview of the target
setup and the analyzer work outside this remediation; this roadmap is the one
plan for the remediation; nothing unfinished from the earlier roadmap is lost.

### Facts

- Before this step, `develop` has `specs/OGC-1054-analyzer-qc-config/`
  (`spec.md`, 289 lines, and `delivery-receipts.md`, 38 lines; the folder name
  is left over from a June branch, the content is the OGC-1054 analyzer
  management spec) and `specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md`
  (134 lines), which #4495 (1 October) made the one analyzer roadmap.
- Six analyzer spec folders existed over the past year (004, 011, 012, 013,
  015 and the OGC-1054 one); #4495 deleted five because each described a design
  the next rework replaced. One folder that changes with the code avoids that.
- The old roadmap's unfinished items outside this remediation: FILE and HL7
  qualification, durable delivery (queue outage, restart, replay, #4421), core
  qualification with recordings, the Madagascar distro, review items F-REV1,
  2, 4 and 5, and related PRs #4421, #3974, Madagascar test harness #4, #9,
  #10, #11, review tooling #16, #31. #4497 merged on 1 October and #4072 is
  closed. F-REV3 and F-REV6 and the remaining items are covered here.
- `delivery-receipts.md` is true on `develop` (checked against
  `AnalyzerDeliveryReceipt.java`); its invariant lives in the spec's "What
  must always hold".
- `specs/OGC-41-westgard-qc/spec.md` links to the OGC-1054 spec at lines 7
  and 37.

### Build

```
- [x] T0.1 Create worktree .worktrees/analyzer-baseline-plan; run scripts/setup-workspace.sh
- [x] T0.2 Add this roadmap as specs/roadmaps/analyzer-baseline-roadmap.md with one file per step
- [x] T0.3 Create specs/analyzers/spec.md: a short overview of the target setup (how it works, who owns what, what must always hold, where the detail is in openelis-work and the Bridge)
- [x] T0.4 Create specs/analyzers/roadmap.md from the old roadmap: each unfinished item kept, moved into this remediation, or dropped as done, with the reason
- [x] T0.5 Delete specs/OGC-1054-analyzer-qc-config/ and specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md; repoint the Westgard spec's two links to specs/analyzers/spec.md
- [ ] T0.6 Format cold; commit; push; the PR stays at the bottom of the stack
```

### Verify

```bash
ls specs/OGC-1054-analyzer-qc-config specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md 2>&1   # No such file
grep -rn "OGC-1054-analyzer-qc-config\|ogc-1054-analyzer-feature-roadmap" --include='*.md' docs specs AGENTS.md CLAUDE.md README.md | grep -v "specs/roadmaps/analyzer-baseline/\|specs/analyzers/roadmap.md"   # no hits
gh pr checks <PR>
```

### Done when

1. One analyzer spec folder exists, named for no ticket, with a short
   overview and the outside-remediation roadmap. (`ls`, read)
2. Every unfinished item of the old roadmap is kept, moved or dropped with a
   stated reason. (read `specs/analyzers/roadmap.md`)
3. No link points to a removed file. (`grep`)
4. All three CI checkpoints pass. (`gh pr checks`)
