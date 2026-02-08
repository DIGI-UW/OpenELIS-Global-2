# Feature 011: Plans & Artifacts Directory

**Purpose:** Archive of implementation plans, status reports, and analysis
documents for Feature 011 (Madagascar Analyzer Integration)

---

## Current Plans

### Architecture Remediation (2026-02-02)

**Plan:**
[architecture-remediation-2026-02-02.plan.md](architecture-remediation-2026-02-02.plan.md)  
**Status Report:** [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md)  
**Changelog:** [../CHANGELOG-2026-02-02.md](../CHANGELOG-2026-02-02.md)

**Summary:**

- Contract corrections based on internet research
- Generic-First architecture (GenericASTM/GenericHL7 with default configs)
- BC2000 promoted P2→P1 (fixture ID 2012)
- Global analyzer inventory (36 plugins, IDs 3000-3035)
- QuantStudio 7 Flex plugin documented (PR #42)
- GenericHL7 architecture spec created (M19 pending)
- GenericFile DEFERRED (requires research)

**Implementation Status:**

- ✅ Phases 0-3, 5, 6 Complete
- 🚧 Phase 4: GenericHL7 architecture documented, Java implementation pending
  (M19)
- ❌ GenericFile deferred

**Key Deliverables:**

- 11 default config templates (`analyzer-defaults/`)
- Global inventory fixture (`global-analyzer-inventory.xml`)
- Verification checklist for deployment team
- Updated specification documents (spec.md, plan.md, tasks.md)

---

### M9-M10 Dashboard Plan

**Plan:** [m9-m10-dashboard-plan.md](m9-m10-dashboard-plan.md)

**Summary:** Dashboard testing and validation for Horiba analyzers (Micros 60,
Pentra 60)

---

## Related Artifacts

### In Parent Directory

- [VERIFICATION-CHECKLIST.md](../templates/VERIFICATION-CHECKLIST.md) - Field
  verification guide for deployment team
- [CHANGELOG-2026-02-02.md](../milestones/CHANGELOG-2026-02-02.md) -
  Architecture remediation changelog
- [VERIFICATION-GUIDE.md](../research/VERIFICATION-GUIDE.md) - General
  verification procedures
- [GENERIC-TEST-RECIPE.md](../templates/GENERIC-TEST-RECIPE.md) - Generic
  testing guide

### In milestones/ Directory

- Current milestone implementation summaries
- Testing documentation
- Analysis reports

### In contracts/ Directory

- [supported-analyzers.md](../contracts/supported-analyzers.md) - Authoritative
  analyzer inventory (v1.1.0)
- [order-export-api.yaml](../contracts/order-export-api.yaml) - Order export API
  contract
- [template-fixture-mapping.md](../contracts/template-fixture-mapping.md) -
  Fixture mapping guide

---

## Plan Organization Guidelines

### Naming Convention

```
{topic}-{YYYY-MM-DD}.plan.md
```

Examples:

- `architecture-remediation-2026-02-02.plan.md`
- `m9-m10-dashboard-2026-01-27.plan.md`

### Required Sections

Each plan should include:

1. **Context** - Problem statement and current state
2. **Phases/Milestones** - Structured implementation steps
3. **File Change Summary** - New and modified files
4. **Dependency Order** - Task dependencies
5. **Risk Mitigation** - Known risks and mitigation strategies

### Associated Artifacts

For each plan, create:

- Status report (e.g., `IMPLEMENTATION-STATUS.md`)
- Changelog (in parent directory as `CHANGELOG-{date}.md`)
- Verification documents (in parent directory)

---

## Quick Links

**Main Specification Documents:**

- [spec.md](../spec.md) - Feature specification
- [plan.md](../plan.md) - Implementation plan (M0-M21)
- [tasks.md](../tasks.md) - Detailed task breakdown (T001-T314+)

**Key Architecture Documents:**

- [plugins/analyzers/GenericHL7/ARCHITECTURE.md](../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md) -
  GenericHL7 plugin spec
- [analyzer-defaults/README.md](../../../projects/analyzer-harness/analyzer-defaults/README.md) -
  Default config templates guide

**Test Infrastructure:**

- [src/test/resources/testdata/global-analyzer-inventory.xml](../../../src/test/resources/testdata/global-analyzer-inventory.xml) -
  Global fixtures
- [src/test/resources/load-analyzer-test-data.sh](../../../src/test/resources/load-analyzer-test-data.sh) -
  Fixture loader

---

**Last Updated:** 2026-02-02  
**Maintained By:** OpenELIS Global Feature 011 Team
