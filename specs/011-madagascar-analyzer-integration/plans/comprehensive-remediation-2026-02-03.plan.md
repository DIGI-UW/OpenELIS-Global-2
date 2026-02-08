---
name: Comprehensive Remediation Plan
overview: |
  Addresses /speckit.analyze findings, completes outstanding items from
  architecture-remediation and m19-m20 plans, and validates M19/M20
  implementation against spec/plan/tasks.
created: 2026-02-03
status: superseded
note: >
  Most phases completed or superseded by subsequent PRs. Retained as
  architectural decision record. See tasks.md PR Traceability Log for merged
  PRs.
---

# Comprehensive Remediation Plan (2026-02-03)

## Summary

Consolidated plan addressing:

1. **Analysis findings** from `/speckit.analyze` (14 issues: 1 HIGH, 6 MEDIUM, 7
   LOW)
2. **Outstanding work** from `architecture-remediation-2026-02-02.plan.md`
3. **Validation gaps** from `m19-m20-immediate-execution.plan.md`

## Phase Status

| Phase | Description                                   | Status                                  |
| ----- | --------------------------------------------- | --------------------------------------- |
| 0.1   | Fix BC2000 priority inconsistency (HIGH)      | Addressed in tasks.md updates           |
| 0.2   | Update M6/M7/M8/M14 task completion status    | Partially done — M6/M8 marked           |
| 0.3   | Reconcile task count (header vs summary)      | Pending                                 |
| 0.4   | Clarify analyzer count (12 vs 13)             | Pending                                 |
| 1.1   | Contract corrections (supported-analyzers.md) | Pending                                 |
| 1.2   | BA-88A default config template                | Pending (verify LIS capability first)   |
| 1.3   | QuantStudio7Flex PR #42 review                | Pending investigation                   |
| 1.4   | Global fixtures testing (M21 T222-T229)       | Pending                                 |
| 2     | Validate M19/M20 implementation               | M19/M20 ✅ complete, validation pending |
| 3     | Build and test verification                   | Pending                                 |
| 4     | Complete default config templates             | 10/11 exist, BA-88A pending             |
| 5     | Outstanding documentation updates             | Superseded by docs PR #2764             |

## Key Outstanding Items

### Documentation Consistency

- BC2000 priority: spec says P1-H, plan said P2 — needs alignment
- Task count: header says "~340" but summary says "~304"
- Analyzer count: spec says "13 required" but table shows 12 rows

### Implementation Gaps

- Liquibase changeset `019-generic-hl7-schema.xml` (M19 T202) — verify if
  GenericHL7 works without schema changes
- BA-88A default config — create after deployment team confirms LIS capability
- M21 global fixtures testing (T222-T229)

## Cross-References

- **Architecture remediation**: `architecture-remediation-2026-02-02.plan.md`
- **M19-M20 execution**: `m19-m20-immediate-execution.plan.md`
- **Tasks**: `tasks.md` (T001-T314 + T200-T239 remediation)
- **PR Traceability**: See tasks.md § PR Traceability Log
