# Analyzer Deployment Verification Checklist

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Purpose:** Field verification of actual analyzer models and connectivity
options

---

## Overview

Internet research (2026-02-02) revealed potential inconsistencies between the
contract specifications and actual analyzer capabilities. This checklist guides
deployment teams in verifying:

1. Actual analyzer models deployed
2. Available LIS connectivity protocols
3. Physical interface options (RS-232, Ethernet, USB)
4. Protocol configuration requirements

---

## Critical Verifications Required

### 1. Mindray BA-88A (CHEMISTRY) - HIGH PRIORITY ⚠️

**Contract Specification:** ASTM/RS232  
**Research Finding:** BA-88A is a 2008-era semi-automatic analyzer with limited
public documentation of LIS capabilities. May have been confused with BS-series
(BS-120, BS-360E) which have full LIS support.

**Verification Tasks:**

- [ ] **Confirm exact model number** on analyzer front panel/label

  - Is it actually BA-88A or a BS-series model (BS-120, BS-200, BS-360E)?
  - Record full model name:
    \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

- [ ] **Check physical connectivity ports**

  - [ ] RS-232 serial port present (DB9 or similar)
  - [ ] Ethernet port present (RJ45)
  - [ ] USB port present
  - [ ] Other: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

- [ ] **Verify LIS capabilities** (check analyzer menu/settings)

  - [ ] LIS communication menu available
  - [ ] Protocol options: ASTM [ ] HL7 [ ] Other [ ] None
  - [ ] Bidirectional capability (query + results) [ ] Results only [ ]

- [ ] **Test basic connectivity**
  - [ ] Analyzer can establish connection
  - [ ] Sample result transmission successful
  - [ ] Protocol tested:
        \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

**If analyzer has NO LIS or limited connectivity:**

- Document manual entry workflow requirement
- Estimate impact on lab throughput
- Consider alternative models if available

---

### 2. Abbott Architect (IMMUNOLOGY) - MEDIUM PRIORITY ⚠️

**Contract Specification:** HL7 v2.5.1 over TCP/IP  
**Research Finding:** Abbott documentation indicates RS-232 serial is the
primary interface. TCP/IP may require Abbott AlinIQ middleware.

**Verification Tasks:**

- [ ] **Confirm exact model variant**

  - Full model name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_
  - Serial number: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

- [ ] **Check available interfaces**

  - [ ] RS-232 port (primary per Abbott docs)
  - [ ] Ethernet port
  - [ ] Abbott AlinIQ middleware installed/available

- [ ] **Verify protocol configuration**

  - Protocol in use: [ ] HL7 via RS-232 [ ] HL7 via TCP/IP [ ] Other
  - If TCP/IP: Is AlinIQ middleware required? [ ] Yes [ ] No

- [ ] **Test connectivity**
  - Interface tested: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_
  - Result transmission successful: [ ] Yes [ ] No
  - Middleware required: [ ] Yes [ ] No

**If RS-232 is primary:**

- OpenELIS supports RS-232 via ASTM-HTTP Bridge
- Update fixture configuration for serial transport

---

## Verification Workflow

### Step 1: Pre-Deployment (Lab Manager/IT)

1. Review analyzer purchase orders and specifications
2. Confirm exact models ordered vs received
3. Document physical connectivity options
4. Check for middleware requirements (e.g., Abbott AlinIQ)

### Step 2: On-Site Verification (Field Engineer)

1. Record analyzer serial numbers and model labels
2. Navigate analyzer menus to verify LIS settings
3. Document available protocols and transport options
4. Test basic connectivity (ping, serial loopback, etc.)

### Step 3: Integration Testing (OpenELIS Team)

1. Configure OpenELIS fixtures based on verified capabilities
2. Test sample result transmission end-to-end
3. Validate test mappings and result formats
4. Document any protocol deviations from contract

### Step 4: Documentation Update

1. Update `supported-analyzers.md` with verified configurations
2. Adjust test fixtures (`madagascar-analyzer-test-data.xml`)
3. Modify plugin configurations if protocol changes required
4. Update deployment documentation for future sites

---

## Reporting Template

Use this template to report verification findings:

```markdown
# Analyzer Verification Report

**Site:** [Lab name/location] **Date:** [YYYY-MM-DD] **Verifier:** [Name and
role]

## Analyzer: [Canonical Name]

**Contract Specification:**

- Model: [From contract]
- Protocol: [From contract]
- Transport: [From contract]

**Actual Deployment:**

- Model: [Verified model number from label]
- Serial Number: [From analyzer]
- Protocol: [Verified from analyzer settings]
- Transport: [Verified - RS-232/TCP-IP/other]
- Physical Ports: [List available]

**LIS Configuration:**

- Bidirectional: [Yes/No]
- Protocol Version: [e.g., HL7 v2.3.1]
- Baud Rate (if RS-232): [e.g., 9600]

**Test Results:**

- Connection Test: [Pass/Fail]
- Sample Transmission: [Pass/Fail]
- Result Format Valid: [Pass/Fail]

**Deviations from Contract:** [List any differences and proposed resolutions]

**Action Items:**

- [ ] [Action 1]
- [ ] [Action 2]
```

---

**Version History:**

| Version | Date       | Changes                                      |
| ------- | ---------- | -------------------------------------------- |
| 1.0.0   | 2026-02-02 | Initial checklist based on research findings |
