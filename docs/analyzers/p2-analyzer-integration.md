# P2 Analyzer Integration Guide

**Feature**: 011-madagascar-analyzer-integration **Milestone**: M14 - P2
Analyzer Validation **Date**: 2026-02-02

## Overview

This document describes the integration of P2 priority analyzers for the
Madagascar contract:

| Analyzer         | Protocol                    | Plugin            | Priority | Status    |
| ---------------- | --------------------------- | ----------------- | -------- | --------- |
| Mindray BC2000   | HL7 v2.3.1 over TCP/IP      | Mindray plugin    | P2       | Validated |
| Sysmex XN Series | ASTM (E1381-02) over TCP/IP | SysmexXN-L plugin | P2       | Validated |

## Mindray BC2000

### Protocol Details

The Mindray BC2000 uses the same HL7 v2.3.1 protocol as the BC-5380:

- **Message Type**: ORU^R01 (results), ORM^O01 (orders)
- **Transport**: TCP/IP on port 2575 (default)
- **Character Set**: ASCII

### Configuration

1. Create an Analyzer record with name matching MSH sending application (e.g.,
   "MINDRAY")
2. Configure AnalyzerConfiguration with IP address and port
3. Field mappings automatically use LOINC codes from the Mindray plugin

### Test Codes Supported

The Mindray plugin supports both hematology analyzers (BC-5380, BC2000) and
chemistry analyzers (BS-360E). For BC2000 hematology results, the
HL7AnalyzerReader with field mappings handles test code translation.

Common test codes:

- WBC (6690-2)
- RBC (789-8)
- HGB (718-7)
- HCT (4544-3)
- PLT (777-3)

## Sysmex XN Series

### Protocol Details

The Sysmex XN-L series uses ASTM E1381-02:

- **Protocol**: ASTM LIS2-A2 (E1381-02)
- **Transport**: TCP/IP (default port varies by site)
- **Character Set**: ASCII

### Configuration

1. Create an Analyzer record with name "SYSMEX" (or matching MSH-3)
2. Configure AnalyzerConfiguration with IP address and port
3. The SysmexXN-L plugin handles HL7 message parsing and test code mapping

### Test Codes Supported

The SysmexXN-L plugin supports comprehensive CBC with differential:

| Test Code | LOINC  | Description                               |
| --------- | ------ | ----------------------------------------- |
| WBC       | 6690-2 | White Blood Cell Count                    |
| RBC       | 789-8  | Red Blood Cell Count                      |
| HGB       | 718-7  | Hemoglobin                                |
| HCT       | 4544-3 | Hematocrit                                |
| MCV       | 787-2  | Mean Corpuscular Volume                   |
| MCH       | 785-6  | Mean Corpuscular Hemoglobin               |
| MCHC      | 786-4  | Mean Corpuscular Hemoglobin Concentration |
| PLT       | 777-3  | Platelet Count                            |
| NEUT%     | 770-8  | Neutrophil Percentage                     |
| LYMPH%    | 736-9  | Lymphocyte Percentage                     |
| MONO%     | 5905-5 | Monocyte Percentage                       |
| EO%       | 713-8  | Eosinophil Percentage                     |
| BASO%     | 706-2  | Basophil Percentage                       |

## Integration with HL7AnalyzerReader

Both P2 analyzers use the HL7AnalyzerReader implemented in M1:

1. **Message Parsing**: HAPI HL7 v2 library parses ORU^R01 messages
2. **Analyzer Identification**: MSH-3 (sending application) identifies the
   analyzer
3. **Result Insertion**: HL7AnalyzerLineInserter creates AnalyzerResults records
4. **Field Mapping**: MappingAwareHL7AnalyzerLineInserter applies
   user-configured overrides

## Override Mappings

To customize test code mappings:

1. Navigate to **Administration > Analyzer Configuration**
2. Select the analyzer (Mindray or Sysmex)
3. Add field mappings with:
   - Source Field: HL7 test identifier (e.g., "WBC")
   - Target Test: OpenELIS test (by LOINC code)
4. Override mappings take precedence over plugin defaults

## Troubleshooting

### No Results Appearing

1. Verify analyzer configuration exists with correct IP
2. Check MSH-3 sending application matches analyzer name
3. Review error dashboard for mapping errors

### Unmapped Test Codes

1. Check error dashboard for unmatched codes
2. Add field mapping for the missing code
3. Reprocess the error queue

## References

- [HL7 v2.5.1 Specification](http://www.hl7.org/implement/standards/product_brief.cfm?product_id=144)
- [Sysmex XN-L User Manual - LIS Interface](https://www.sysmex.com/)
- [Mindray BC Series LIS Communication Guide](https://www.mindray.com/)
