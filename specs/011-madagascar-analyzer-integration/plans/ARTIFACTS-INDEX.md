# Feature 011: Artifacts Index

**Feature:** Madagascar Analyzer Integration **Last Updated:** 2026-02-08
**Purpose:** Comprehensive index of all feature artifacts and documentation

---

## Core Specification Documents

| Document                          | Purpose                                    | Status                  |
| --------------------------------- | ------------------------------------------ | ----------------------- |
| [spec.md](../spec.md)             | Feature specification with user stories    | ✅ v1.2 (Generic-First) |
| [plan.md](../plan.md)             | Implementation plan with M0-M21 milestones | ✅ Updated with M19-M21 |
| [tasks.md](../tasks.md)           | Detailed task breakdown (T001-T314+)       | ✅ T200-T239 added      |
| [data-model.md](../data-model.md) | Entity relationship documentation          | ✅ Current              |
| [quickstart.md](../quickstart.md) | Developer onboarding guide                 | ✅ Current              |
| [research.md](../research.md)     | Research findings                          | ✅ Current              |

---

## Plans & Status Reports

### plans/ Directory (this directory)

| Document                                                                                     | Date       | Status         |
| -------------------------------------------------------------------------------------------- | ---------- | -------------- |
| [architecture-remediation-2026-02-02.plan.md](architecture-remediation-2026-02-02.plan.md)   | 2026-02-02 | ✅ Implemented |
| [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md)                                         | 2026-02-02 | ✅ Complete    |
| [comprehensive-remediation-2026-02-03.plan.md](comprehensive-remediation-2026-02-03.plan.md) | 2026-02-03 | Superseded     |
| [m9-m10-dashboard-plan.md](m9-m10-dashboard-plan.md)                                         | 2026-01-27 | ✅ Complete    |
| [m19-m20-immediate-execution.plan.md](m19-m20-immediate-execution.plan.md)                   | 2026-02-03 | ✅ Complete    |
| [astm-genericplugin-audit-2026-02-03.plan.md](astm-genericplugin-audit-2026-02-03.plan.md)   | 2026-02-03 | Reference      |
| [astm-flows-audit-report.md](astm-flows-audit-report.md)                                     | 2026-02-03 | Reference      |
| [m8-implementation-prompt.md](m8-implementation-prompt.md)                                   | 2026-02-03 | Reference      |
| [universal-analyzer-bridge-v2.md](universal-analyzer-bridge-v2.md)                           | 2026-02-06 | ✅ Current     |

---

## Contracts & Agreements

### contracts/ Directory

| Document                                                                | Version | Purpose                                                       |
| ----------------------------------------------------------------------- | ------- | ------------------------------------------------------------- |
| [supported-analyzers.md](../contracts/supported-analyzers.md)           | v1.1.0  | Authoritative analyzer inventory (13 required + 23 supported) |
| [order-export-api.yaml](../contracts/order-export-api.yaml)             | v1.0    | Order export REST API specification                           |
| [template-fixture-mapping.md](../contracts/template-fixture-mapping.md) | v1.0    | Mock template to fixture mapping                              |

---

## Verification & Testing

| Document                                                            | Purpose                        | For             |
| ------------------------------------------------------------------- | ------------------------------ | --------------- |
| [VERIFICATION-CHECKLIST.md](../templates/VERIFICATION-CHECKLIST.md) | Field verification procedures  | Deployment team |
| [VERIFICATION-GUIDE.md](../research/VERIFICATION-GUIDE.md)          | General verification guide     | All             |
| [GENERIC-TEST-RECIPE.md](../templates/GENERIC-TEST-RECIPE.md)       | Generic analyzer testing steps | QA/Testing      |
| [testing-matrix.md](../checklists/testing-matrix.md)                | Test coverage matrix           | All             |

---

## Research & Analysis

### research/ Directory

| Document                                                                                     | Purpose                           |
| -------------------------------------------------------------------------------------------- | --------------------------------- |
| [hibernate-mapping-analysis.md](../research/hibernate-mapping-analysis.md)                   | ORM mapping analysis              |
| [xml-migration-scope.md](../research/xml-migration-scope.md)                                 | XML to annotation migration scope |
| [xml-to-annotations-guide.md](../research/xml-to-annotations-guide.md)                       | Migration guide                   |
| [hl7-analyzer-messaging-validation.md](../research/hl7-analyzer-messaging-validation.md)     | HL7 protocol validation           |
| [pre-implementation-analysis.md](../research/pre-implementation-analysis.md)                 | Initial analysis                  |
| [analyzer-plugin-architecture-report.md](../research/analyzer-plugin-architecture-report.md) | Plugin architecture analysis      |
| [metadata-management-analysis-report.md](../research/metadata-management-analysis-report.md) | Metadata management analysis      |

---

## Checklists

### checklists/ Directory

| Document                                                               | Purpose                 |
| ---------------------------------------------------------------------- | ----------------------- |
| [requirements.md](../checklists/requirements.md)                       | Requirements checklist  |
| [CONSTITUTION-COMPLIANCE.md](../checklists/CONSTITUTION-COMPLIANCE.md) | Constitution compliance |
| [testing-matrix.md](../checklists/testing-matrix.md)                   | Test coverage matrix    |

---

## Milestone Documentation

### milestones/ Directory

| Document                                                                                   | Milestone | Purpose                     |
| ------------------------------------------------------------------------------------------ | --------- | --------------------------- |
| [current-state-analysis-2026-02-02.md](../milestones/current-state-analysis-2026-02-02.md) | General   | Current state analysis      |
| [IMPLEMENTATION-SUMMARY.md](../milestones/IMPLEMENTATION-SUMMARY.md)                       | General   | Implementation summary      |
| [REMEDIATION-REPORT.md](../milestones/REMEDIATION-REPORT.md)                               | General   | Remediation report          |
| [CHANGELOG-2026-02-02.md](../milestones/CHANGELOG-2026-02-02.md)                           | General   | Architecture changes        |
| [analyzer-harness-setup-report.md](../milestones/analyzer-harness-setup-report.md)         | General   | Test harness setup          |
| [m9-m10-manual-testing-guide.md](../milestones/m9-m10-manual-testing-guide.md)             | M9-M10    | Horiba testing guide        |
| [m9-m10-testing-infrastructure.md](../milestones/m9-m10-testing-infrastructure.md)         | M9-M10    | Testing infrastructure      |
| [m11-simulator-dashboard-analysis.md](../milestones/m11-simulator-dashboard-analysis.md)   | M11       | Stago simulator analysis    |
| [m11-stago-implementation-analysis.md](../milestones/m11-stago-implementation-analysis.md) | M11       | Stago implementation        |
| [m11-stago-testing/](../milestones/m11-stago-testing/)                                     | M11       | Stago testing documentation |

---

## External Artifacts

### analyzer-defaults/ (Analyzer Harness)

**Location:** `/projects/analyzer-harness/analyzer-defaults/`

**Contents:** 11 default configuration templates for GenericASTM and GenericHL7
plugins

**Index:**
[analyzer-defaults/README.md](../../../projects/analyzer-harness/analyzer-defaults/README.md)

### Test Fixtures (Test Resources)

**Location:** `/src/test/resources/testdata/`

| Fixture                             | IDs       | Analyzers | Purpose                           |
| ----------------------------------- | --------- | --------- | --------------------------------- |
| `madagascar-analyzer-test-data.xml` | 2000-2012 | 13        | Madagascar contract (Feature 011) |
| `global-analyzer-inventory.xml`     | 3000-3035 | 36        | All supported plugins             |
| `analyzer-mapping-test-data.xml`    | 1000-1004 | 5         | Feature 004 (ASTM mapping)        |

---

## Navigation Guide

### For Developers

1. Start with [spec.md](../spec.md) for feature overview
2. Read [plan.md](../plan.md) for milestone structure
3. Check [tasks.md](../tasks.md) for specific tasks
4. Review
   [contracts/supported-analyzers.md](../contracts/supported-analyzers.md) for
   analyzer details

### For Deployment Teams

1. Read [VERIFICATION-CHECKLIST.md](../templates/VERIFICATION-CHECKLIST.md) for
   field procedures
2. Use [contracts/supported-analyzers.md](../contracts/supported-analyzers.md)
   for analyzer specs
3. Follow [GENERIC-TEST-RECIPE.md](../templates/GENERIC-TEST-RECIPE.md) for
   testing

### For Architecture Review

1. Read
   [architecture-remediation-2026-02-02.plan.md](architecture-remediation-2026-02-02.plan.md)
2. Review
   [plugins/analyzers/GenericHL7/ARCHITECTURE.md](../../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md)
3. Check
   [analyzer-defaults/README.md](../../../projects/analyzer-harness/analyzer-defaults/README.md)

---

**Maintained By:** OpenELIS Global Feature 011 Team **Repository:**
`DIGI-UW/OpenELIS-Global-2`
