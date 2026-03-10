# GeneXpert Multi-Protocol Support

**Feature**: 011-madagascar-analyzer-integration **Milestone**: M7 **Created**:
2026-02-02

## Overview

The Cepheid GeneXpert analyzer supports three distinct communication protocols
in OpenELIS. All three variants can coexist and operate simultaneously,
configured for different use cases or network environments.

## Protocol Variants

| Variant           | Plugin Directory                   | Protocol | Use Case                              |
| ----------------- | ---------------------------------- | -------- | ------------------------------------- |
| **GeneXpert**     | `plugins/analyzers/GeneXpert/`     | ASTM     | Real-time bidirectional communication |
| **GeneXpertHL7**  | `plugins/analyzers/GeneXpertHL7/`  | HL7 v2.5 | Network-based HL7 messaging           |
| **GeneXpertFile** | `plugins/analyzers/GeneXpertFile/` | CSV/TXT  | Batch file import from exports        |

## Plugin Architecture

### GeneXpert (ASTM)

The ASTM variant implements the LIS2-A2 protocol for real-time bidirectional
communication:

- **Identification**: ASTM Header (H) segment contains `GeneXpert` in sender
  field
- **Capabilities**:
  - Query response for pending orders
  - Real-time result transmission
  - Bidirectional acknowledgments
- **Transport**: TCP/IP via ASTM-HTTP Bridge

```text
Physical GeneXpert → ASTM-HTTP Bridge → OpenELIS /analyzer/astm
```

### GeneXpertHL7

The HL7 variant uses standard HL7 v2.5 ORU^R01 messages:

- **Identification**: MSH-3 (Sending Application) = "GENEXPERT"
- **Message Type**: ORU^R01 (Unsolicited Result)
- **Capabilities**:
  - Standard HL7 result messages
  - Patient demographics in PID segment
  - Test results in OBX segments
- **Transport**: TCP/IP directly or via HL7 listener

```text
GeneXpert HL7 Output → TCP/IP → OpenELIS /analyzer/hl7
```

### GeneXpertFile

The File variant processes exported CSV/TXT files from the GeneXpert software:

- **Identification**: File contains "GeneXpert Dx System" header
- **File Format**: Section-based CSV with "ASSAY INFORMATION" and "RESULT TABLE"
- **Capabilities**:
  - Batch processing of exported results
  - Support for multiple results per file
  - Archive/error directory management
- **Transport**: Directory watching

```text
GeneXpert Export → Shared Folder → OpenELIS FileImportService
```

## Configuration

### ASTM Configuration

1. Configure ASTM-HTTP Bridge to connect to GeneXpert IP/port
2. Register analyzer in OpenELIS with name "GeneXpertAnalyzer"
3. Configure field mappings if needed

### HL7 Configuration

1. Configure GeneXpert to send HL7 messages to OpenELIS IP/port
2. Register analyzer in OpenELIS with name matching MSH sender ("GENEXPERT")
3. Configure field mappings if needed

### File Configuration

1. Configure GeneXpert to export results to a shared directory
2. Create `file_import_configuration` entry for the analyzer:
   - `import_directory`: Path to shared folder
   - `file_pattern`: `*.csv` or `*.txt`
   - `column_mappings`: Map GeneXpert fields to OpenELIS fields
3. Enable directory watcher

## Coexistence

All three variants can operate simultaneously:

- Each variant has a unique analyzer registration
- Messages are routed based on protocol and identification
- Results from any variant are stored in the same results table
- Field mappings can be configured independently per variant

### Example: Multiple GeneXpert Instruments

```text
Lab A: GeneXpert (ASTM) → Direct ASTM connection
Lab B: GeneXpert (HL7)  → HL7 over network
Lab C: GeneXpert (File) → USB export to shared folder
```

## Test Identification

| Protocol | Test Types Supported      | LOINC Codes          |
| -------- | ------------------------- | -------------------- |
| All      | SARS-CoV-2                | 94500-6              |
| All      | HBV Viral Load            | 29615-2              |
| All      | HCV Viral Load            | 11011-4              |
| All      | HIV Viral Load            | 10351-5              |
| ASTM     | CBC (WBC, RBC, HGB, etc.) | 6690-2, 789-8, 718-7 |

## Troubleshooting

### ASTM Issues

- Verify ASTM-HTTP Bridge connectivity
- Check that GeneXpert is configured for LIS2-A2 protocol
- Verify sender field in ASTM Header matches expected pattern

### HL7 Issues

- Verify MSH-3 (Sending Application) is set to "GENEXPERT"
- Check HL7 message structure (ORU^R01)
- Verify network connectivity to HL7 listener port

### File Issues

- Verify file pattern matches exported files
- Check directory permissions (read for import, write for archive/error)
- Verify file contains expected sections ("ASSAY INFORMATION", "RESULT TABLE")

## References

- `plugins/analyzers/GeneXpert/` - ASTM plugin implementation
- `plugins/analyzers/GeneXpertHL7/` - HL7 plugin implementation
- `plugins/analyzers/GeneXpertFile/` - File plugin implementation
- `docs/analyzer.md` - External plugin architecture
- `docs/astm.md` - ASTM protocol configuration
- `tools/analyzer-mock-server/templates/genexpert.json` - Simulator template
