# Research: Madagascar Analyzer Integration

**Feature**: 011-madagascar-analyzer-integration **Date**: 2026-01-22
**Purpose**: Resolve technical unknowns and document design decisions

---

## 1. HL7 v2.x Protocol Implementation

### Decision: Use HAPI HL7 v2 Library

**Rationale**: HAPI (HL7 Application Programming Interface) is the
industry-standard Java library for HL7 v2.x message processing. It's already
used in OpenELIS for existing HL7 functionality (`HL7MessageOutService`,
`HL7OrderInterpreter`).

**Alternatives Considered**: | Option | Pros | Cons | Decision |
|--------|------|------|----------| | HAPI HL7 v2 | Industry standard, already
in codebase, good documentation | Learning curve for complex messages | ✅
Selected | | Manual parsing | Full control, simpler dependencies | Error-prone,
no validation, maintenance burden | ❌ Rejected | | HL7 FHIR conversion | Modern
standard | Overkill for analyzer communication, complexity | ❌ Rejected |

**Implementation Pattern**:

```java
// ORU^R01 Result Message Parsing
HapiContext context = new DefaultHapiContext();
Parser parser = context.getPipeParser();
Message message = parser.parse(rawHL7String);

if (message instanceof ORU_R01) {
    ORU_R01 oru = (ORU_R01) message;
    // Extract patient, order, observation segments
    String patientId = oru.getPATIENT_RESULT().getPATIENT().getPID().getPatientID().getIDNumber().getValue();
    // ... extract test results from OBX segments
}

// ORM^O01 Order Message Generation
ORM_O01 orm = new ORM_O01();
orm.getMSH().getMessageType().getTriggerEvent().setValue("O01");
// ... populate patient, order details
String orderMessage = parser.encode(orm);
```

### HL7 Message Types Required

| Message Type | Direction           | Purpose                 | Implementation           |
| ------------ | ------------------- | ----------------------- | ------------------------ |
| ORU^R01      | Analyzer → OpenELIS | Results from analyzer   | `HL7AnalyzerReader.java` |
| ORM^O01      | OpenELIS → Analyzer | Test orders to analyzer | `HL7MessageService.java` |
| ACK          | Bidirectional       | Acknowledgment          | Built-in HAPI support    |

### Analyzer-Specific HL7 Variations

Based on research of existing plugins and vendor documentation:

| Analyzer         | MSH Sending Application | OBX Format Notes             |
| ---------------- | ----------------------- | ---------------------------- |
| Mindray BC-5380  | "MINDRAY"               | Standard CBC OBX segments    |
| Mindray BS-360E  | "MINDRAY"               | Chemistry panel OBX segments |
| Mindray BC2000   | "MINDRAY"               | Standard hematology OBX      |
| Sysmex XN Series | "SYSMEX"                | Extended differential OBX    |
| Abbott Architect | "ARCHITECT"             | Immunoassay OBX segments     |

**Key Research Finding**: Existing `GeneXpertHL7` plugin provides reference
implementation for HL7 v2.x parsing that can be adapted for other analyzers.

---

## 2. RS232 Serial Communication

### Decision: Use jSerialComm Library

**Rationale**: jSerialComm is a modern, pure-Java serial communication library
that works across Windows, Linux, and macOS without native library dependencies.
This is critical for Docker container deployment.

**Alternatives Considered**: | Option | Pros | Cons | Decision |
|--------|------|------|----------| | jSerialComm 2.x | Pure Java,
Docker-friendly, active maintenance | Requires USB passthrough config | ✅
Selected | | RXTX | Long history, many examples | Abandoned, native libs
required | ❌ Rejected | | Java Communications API | Official API |
Discontinued, poor support | ❌ Rejected |

**Implementation Pattern**:

```java
// Serial port configuration
SerialPort port = SerialPort.getCommPort("/dev/ttyUSB0");
port.setBaudRate(9600);
port.setNumDataBits(8);
port.setNumStopBits(SerialPort.ONE_STOP_BIT);
port.setParity(SerialPort.NO_PARITY);
port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

// Open connection
port.openPort();
port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 30000, 0);

// Read ASTM message
InputStream in = port.getInputStream();
// ... read until ETX character (ASTM frame termination)
```

### Docker Serial Port Passthrough

**Configuration** (docker-compose.yml):

```yaml
services:
  openelis:
    devices:
      - /dev/ttyUSB0:/dev/ttyUSB0
      - /dev/ttyUSB1:/dev/ttyUSB1
    privileged: false # Use specific devices, not privileged mode
```

**Virtual Serial Ports for Testing** (using socat):

```bash
# Create virtual serial port pair
socat -d -d pty,raw,echo=0 pty,raw,echo=0
# Creates /dev/pts/X and /dev/pts/Y as connected pair
```

### RS232 Analyzer Configuration

| Analyzer         | Default Baud | Data Bits | Parity | Stop Bits | Protocol         |
| ---------------- | ------------ | --------- | ------ | --------- | ---------------- |
| Horiba Pentra 60 | 9600         | 8         | None   | 1         | ASTM             |
| Horiba Micros 60 | 9600         | 8         | None   | 1         | ASTM             |
| Mindray BA-88A   | 9600         | 8         | None   | 1         | Proprietary/ASTM |
| Stago STart 4    | 9600         | 8         | None   | 1         | ASTM/HL7         |
| Abbott Architect | 9600         | 8         | None   | 1         | HL7              |

---

## 3. File-Based Import

### Decision: Use Java WatchService + Apache Commons CSV

**Rationale**: Java's WatchService provides native OS-level file watching.
Apache Commons CSV is a robust CSV parsing library already used elsewhere in
enterprise Java applications.

**Alternatives Considered**: | Option | Pros | Cons | Decision |
|--------|------|------|----------| | WatchService + Commons CSV | Native,
efficient, reliable parsing | Slight complexity for CSV edge cases | ✅ Selected
| | Polling + OpenCSV | Simple implementation | CPU overhead, less efficient |
❌ Rejected | | Apache Camel | Enterprise integration, many connectors |
Heavyweight for simple use case | ❌ Rejected |

**Implementation Pattern**:

```java
// Directory watcher
WatchService watchService = FileSystems.getDefault().newWatchService();
Path importDir = Paths.get(configuration.getImportDirectory());
importDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

// Watch loop
while (true) {
    WatchKey key = watchService.poll(1, TimeUnit.MINUTES);
    if (key != null) {
        for (WatchEvent<?> event : key.pollEvents()) {
            Path file = importDir.resolve((Path) event.context());
            if (matchesPattern(file, configuration.getFilePattern())) {
                processFile(file);
            }
        }
        key.reset();
    }
}

// CSV parsing
try (CSVParser parser = CSVFormat.DEFAULT
        .withHeader()
        .withIgnoreHeaderCase()
        .parse(new FileReader(file))) {
    for (CSVRecord record : parser) {
        String sampleId = record.get("Sample_ID");
        String testCode = record.get("Test_Code");
        String result = record.get("Result");
        // ... apply mappings
    }
}
```

### File Formats by Analyzer

| Analyzer             | Format | Key Columns                                 | Notes                          |
| -------------------- | ------ | ------------------------------------------- | ------------------------------ |
| QuantStudio 7 Flex   | CSV    | Well, Sample Name, Target, Ct               | Tab-delimited variant possible |
| Hain FluoroCycler XT | CSV    | Position, Sample ID, Result, Interpretation | Semi-colon delimiter           |

---

## 4. Order Export Workflow

### Decision: Manual Trigger with Asynchronous Processing

**Rationale**: Per clarification session, manual export is required for
deadline. This aligns with the existing ASTM bridge pattern where OpenELIS
initiates communication.

**Workflow Design**:

```
1. User selects pending samples in UI
2. User clicks "Export to Analyzer"
3. System creates OrderExport records (status: PENDING)
4. Background job sends messages to analyzers
5. Status updated: SENT → ACKNOWLEDGED (if supported) → RESULTS_RECEIVED
6. Results matched via sample/accession ID
```

**State Machine**:

```
PENDING → SENT → ACKNOWLEDGED → RESULTS_RECEIVED
    ↓         ↓           ↓
  FAILED   TIMEOUT    EXPIRED
    ↓         ↓
 (Retry up to 3x with exponential backoff)
```

**Message Generation**:

- **ASTM**: O-segment generation (Patient|Order|Result structure)
- **HL7**: ORM^O01 message generation

---

## 5. Integration with Existing Plugins

### Decision: Wrapper Pattern (MappingAwareAnalyzerLineInserter)

**Rationale**: Feature 004 established the `MappingAwareAnalyzerLineInserter`
wrapper pattern that applies field mappings without modifying original plugin
code. This pattern will be extended for HL7 and RS232 inputs.

**Integration Points**:

1. **HL7AnalyzerReader** → Parses HL7 → Extracts fields → Passes to
   MappingAwareAnalyzerLineInserter
2. **SerialAnalyzerReader** → Receives RS232 → Detects protocol (ASTM/HL7) →
   Routes to appropriate reader
3. **FileAnalyzerReader** → Reads CSV → Extracts fields → Passes to mapping
   infrastructure

**Plugin Compatibility**: | Plugin | Protocol Support | Mapping Integration |
Status | |--------|------------------|---------------------|--------| | Mindray
| HL7 native | Override via mappings | ✅ Compatible | | SysmexXN-L | HL7 native
| Override via mappings | ✅ Compatible | | GeneXpert | ASTM native | Full
mapping | ✅ Compatible | | GeneXpertHL7 | HL7 native | Full mapping | ✅
Compatible | | GeneXpertFile | File native | Full mapping | ✅ Compatible | |
QuantStudio3 | File native | Adaptation needed | ⚠️ Needs M8 |

---

## 6. Multi-Protocol Simulator Design

### Decision: Expand analyzer-mock-server to Multi-Protocol Analyzer Simulator

**Rationale**: The existing analyzer-mock-server (Python) provides ASTM
simulation. Expanding it to support HL7, RS232, and file-based protocols enables
comprehensive testing without physical analyzers.

**Architecture**:

```
┌─────────────────────────────────────────────────────┐
│           Multi-Protocol Analyzer Simulator          │
├──────────┬──────────┬──────────┬──────────┬─────────┤
│   ASTM   │   HL7    │  RS232   │   File   │  HTTP   │
│  Server  │  Server  │  Server  │Generator │   API   │
├──────────┴──────────┴──────────┴──────────┴─────────┤
│                  Message Templates                   │
│   (Mindray, Sysmex, GeneXpert, QuantStudio, etc.)   │
├─────────────────────────────────────────────────────┤
│                Configuration Manager                 │
│        (Analyzer selection, message types)          │
└─────────────────────────────────────────────────────┘
```

**HTTP API Mode** (for CI/CD):

```
POST /simulate/hl7/mindray-bc5380
Body: { "patientId": "P001", "sampleId": "S001", "tests": ["WBC", "RBC"] }
Response: { "status": "sent", "messageId": "MSG-001" }

POST /simulate/file/quantstudio
Body: { "sampleCount": 10, "targetDirectory": "/import" }
Response: { "status": "file_generated", "path": "/import/results_001.csv" }
```

---

## 7. Database Schema Design

### New Tables

See [data-model.md](data-model.md) for complete schema definitions (Liquibase
changesets, entity relationships, validation rules).

Key tables: `instrument_metadata` (M16), `order_export` (M15),
`serial_port_configuration` (M2), `file_import_configuration` (M3),
`instrument_location_history` (M16). All use UUID PKs and FHIR UUIDs per
Constitution IV.

---

## 8. Performance Considerations

### Message Processing Targets

| Metric                  | Target | Measurement              |
| ----------------------- | ------ | ------------------------ |
| HL7 message parsing     | <100ms | Unit test timing         |
| RS232 message reception | <500ms | Integration test timing  |
| File detection          | <60s   | E2E test timing          |
| Order export            | <5s    | Integration test timing  |
| Concurrent analyzers    | 5+     | Load test with simulator |
| System uptime           | 99%+   | Production monitoring    |

### Optimization Strategies

1. **Message Parsing**: Use streaming parsers, avoid full message loading into
   memory
2. **File Watching**: Use OS-level WatchService, not polling
3. **Database Queries**: Batch inserts for high-volume analyzers
4. **Connection Pooling**: Reuse serial port connections (don't open/close per
   message)
5. **Async Processing**: Background jobs for order export, don't block UI

---

## 9. Security Considerations

### Input Validation

| Input              | Validation                     | Risk Mitigated              |
| ------------------ | ------------------------------ | --------------------------- |
| HL7 messages       | HAPI parser validation         | Malformed message injection |
| Serial port names  | Whitelist valid ports          | Path traversal              |
| Import directories | Restrict to configured paths   | Directory traversal         |
| File content       | Size limits, format validation | DoS via large files         |

### Access Control

| Action             | Required Role  | Audit Logged |
| ------------------ | -------------- | ------------ |
| Configure analyzer | LAB_SUPERVISOR | Yes          |
| Export orders      | LAB_SUPERVISOR | Yes          |
| View results       | LAB_TECHNICIAN | Yes          |
| Modify mappings    | LAB_SUPERVISOR | Yes          |

---

## 10. Internationalization

### New Translation Keys

```json
// en.json
{
  "analyzer.hl7.title": "HL7 Configuration",
  "analyzer.hl7.mshSenderId": "MSH Sending Application",
  "analyzer.serial.title": "Serial Port Configuration",
  "analyzer.serial.portName": "Port Name",
  "analyzer.serial.baudRate": "Baud Rate",
  "analyzer.serial.parity": "Parity",
  "analyzer.serial.stopBits": "Stop Bits",
  "analyzer.serial.flowControl": "Flow Control",
  "analyzer.file.title": "File Import Configuration",
  "analyzer.file.importDirectory": "Import Directory",
  "analyzer.file.filePattern": "File Pattern",
  "analyzer.file.archiveDirectory": "Archive Directory",
  "analyzer.orderExport.title": "Order Export",
  "analyzer.orderExport.selectOrders": "Select Orders to Export",
  "analyzer.orderExport.export": "Export to Analyzer",
  "analyzer.orderExport.status.pending": "Pending",
  "analyzer.orderExport.status.sent": "Sent",
  "analyzer.orderExport.status.acknowledged": "Acknowledged",
  "analyzer.orderExport.status.resultsReceived": "Results Received",
  "analyzer.orderExport.status.failed": "Failed",
  "analyzer.orderExport.status.expired": "Expired",
  "analyzer.metadata.title": "Instrument Details",
  "analyzer.metadata.installationDate": "Installation Date",
  "analyzer.metadata.warrantyExpiration": "Warranty Expiration",
  "analyzer.metadata.softwareVersion": "Software Version",
  "analyzer.metadata.calibrationDueDate": "Calibration Due Date",
  "analyzer.metadata.location": "Location"
}

// fr.json
{
  "analyzer.hl7.title": "Configuration HL7",
  "analyzer.hl7.mshSenderId": "Application d'envoi MSH",
  "analyzer.serial.title": "Configuration du port série",
  "analyzer.serial.portName": "Nom du port",
  "analyzer.serial.baudRate": "Débit en bauds",
  "analyzer.serial.parity": "Parité",
  "analyzer.serial.stopBits": "Bits d'arrêt",
  "analyzer.serial.flowControl": "Contrôle de flux",
  "analyzer.file.title": "Configuration d'importation de fichiers",
  "analyzer.file.importDirectory": "Répertoire d'importation",
  "analyzer.file.filePattern": "Modèle de fichier",
  "analyzer.file.archiveDirectory": "Répertoire d'archive",
  "analyzer.orderExport.title": "Exportation des commandes",
  "analyzer.orderExport.selectOrders": "Sélectionner les commandes à exporter",
  "analyzer.orderExport.export": "Exporter vers l'analyseur",
  "analyzer.orderExport.status.pending": "En attente",
  "analyzer.orderExport.status.sent": "Envoyé",
  "analyzer.orderExport.status.acknowledged": "Accusé de réception",
  "analyzer.orderExport.status.resultsReceived": "Résultats reçus",
  "analyzer.orderExport.status.failed": "Échoué",
  "analyzer.orderExport.status.expired": "Expiré",
  "analyzer.metadata.title": "Détails de l'instrument",
  "analyzer.metadata.installationDate": "Date d'installation",
  "analyzer.metadata.warrantyExpiration": "Expiration de la garantie",
  "analyzer.metadata.softwareVersion": "Version du logiciel",
  "analyzer.metadata.calibrationDueDate": "Date d'échéance de l'étalonnage",
  "analyzer.metadata.location": "Emplacement"
}
```

---

## 11. Analyzer Template Schema (Multi-Protocol Simulator)

### Purpose

The template schema defines a standardized format for analyzer message
configuration in the multi-protocol simulator (analyzer-mock-server). Each
template describes how to generate realistic messages for a specific analyzer
type.

### Template Schema Definition

**Location**: `tools/analyzer-mock-server/templates/schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Analyzer Message Template",
  "type": "object",
  "required": ["analyzer", "protocol", "identification", "fields"],
  "properties": {
    "analyzer": {
      "type": "object",
      "description": "Analyzer identification metadata",
      "required": ["name", "model", "manufacturer"],
      "properties": {
        "name": { "type": "string", "description": "Display name" },
        "model": { "type": "string", "description": "Model number" },
        "manufacturer": { "type": "string" }
      }
    },
    "protocol": {
      "type": "object",
      "description": "Communication protocol configuration",
      "required": ["type", "version", "transport"],
      "properties": {
        "type": { "enum": ["ASTM", "HL7", "RS232", "FILE"] },
        "version": { "type": "string" },
        "transport": { "enum": ["TCP", "HTTP", "SERIAL", "FILE"] }
      }
    },
    "identification": {
      "type": "object",
      "description": "How OpenELIS identifies this analyzer",
      "properties": {
        "msh_sender": { "type": "string", "description": "HL7 MSH-3 field" },
        "astm_header": {
          "type": "string",
          "description": "ASTM H-segment pattern"
        },
        "ip_pattern": { "type": "string", "description": "IP address/range" },
        "file_pattern": {
          "type": "string",
          "description": "Glob pattern for files"
        }
      }
    },
    "fields": {
      "type": "array",
      "description": "Test fields supported by this analyzer",
      "items": {
        "type": "object",
        "required": ["name", "code", "type"],
        "properties": {
          "name": { "type": "string" },
          "code": { "type": "string", "description": "LOINC or analyzer code" },
          "type": { "enum": ["NUMERIC", "QUALITATIVE", "TEXT"] },
          "unit": { "type": "string" },
          "normalRange": { "type": "string" },
          "possibleValues": { "type": "array", "items": { "type": "string" } }
        }
      }
    },
    "serial_config": {
      "type": "object",
      "description": "RS232 configuration (if protocol.transport=SERIAL)",
      "properties": {
        "baud_rate": { "type": "integer", "default": 9600 },
        "data_bits": { "type": "integer", "default": 8 },
        "parity": { "enum": ["NONE", "EVEN", "ODD"], "default": "NONE" },
        "stop_bits": { "type": "number", "default": 1 }
      }
    },
    "file_config": {
      "type": "object",
      "description": "File format configuration (if protocol.transport=FILE)",
      "properties": {
        "format": { "enum": ["CSV", "TSV", "TXT", "XLS"] },
        "delimiter": { "type": "string", "default": "," },
        "has_header": { "type": "boolean", "default": true },
        "column_mapping": { "type": "object" }
      }
    }
  }
}
```

### Example Template: Mindray BC-5380

**Location**: `tools/analyzer-mock-server/templates/mindray_bc5380.json`

```json
{
  "analyzer": {
    "name": "Mindray BC-5380",
    "model": "BC-5380",
    "manufacturer": "Mindray"
  },
  "protocol": {
    "type": "HL7",
    "version": "2.5",
    "transport": "TCP"
  },
  "identification": {
    "msh_sender": "MINDRAY",
    "ip_pattern": "192.168.1.*"
  },
  "fields": [
    {
      "name": "WBC",
      "code": "6690-2",
      "type": "NUMERIC",
      "unit": "10^3/uL",
      "normalRange": "4.5-11.0"
    },
    {
      "name": "RBC",
      "code": "789-8",
      "type": "NUMERIC",
      "unit": "10^6/uL",
      "normalRange": "4.5-5.5"
    },
    {
      "name": "HGB",
      "code": "718-7",
      "type": "NUMERIC",
      "unit": "g/dL",
      "normalRange": "13.5-17.5"
    },
    {
      "name": "HCT",
      "code": "4544-3",
      "type": "NUMERIC",
      "unit": "%",
      "normalRange": "40-54"
    },
    {
      "name": "PLT",
      "code": "777-3",
      "type": "NUMERIC",
      "unit": "10^3/uL",
      "normalRange": "150-400"
    }
  ]
}
```

### Example Template: Horiba Pentra 60 (RS232)

**Location**: `tools/analyzer-mock-server/templates/horiba_pentra60.json`

```json
{
  "analyzer": {
    "name": "Horiba Pentra 60",
    "model": "Pentra 60",
    "manufacturer": "Horiba ABX"
  },
  "protocol": {
    "type": "ASTM",
    "version": "LIS2-A2",
    "transport": "SERIAL"
  },
  "identification": {
    "astm_header": "PENTRA"
  },
  "serial_config": {
    "baud_rate": 9600,
    "data_bits": 8,
    "parity": "NONE",
    "stop_bits": 1
  },
  "fields": [
    {
      "name": "WBC",
      "code": "WBC",
      "type": "NUMERIC",
      "unit": "10^3/uL",
      "normalRange": "4.0-10.0"
    },
    {
      "name": "RBC",
      "code": "RBC",
      "type": "NUMERIC",
      "unit": "10^6/uL",
      "normalRange": "4.0-5.5"
    },
    {
      "name": "HGB",
      "code": "HGB",
      "type": "NUMERIC",
      "unit": "g/dL",
      "normalRange": "12.0-17.5"
    }
  ]
}
```

### Example Template: QuantStudio 7 Flex (File-based)

**Location**: `tools/analyzer-mock-server/templates/quantstudio7.json`

```json
{
  "analyzer": {
    "name": "QuantStudio 7 Flex",
    "model": "7 Flex",
    "manufacturer": "Thermo Fisher"
  },
  "protocol": {
    "type": "FILE",
    "version": "1.0",
    "transport": "FILE"
  },
  "identification": {
    "file_pattern": "QS7_*.csv"
  },
  "file_config": {
    "format": "CSV",
    "delimiter": "\t",
    "has_header": true,
    "column_mapping": {
      "sample_id": "Sample Name",
      "test_code": "Target",
      "result": "Ct"
    }
  },
  "fields": [
    { "name": "SARS-CoV-2", "code": "SARS2", "type": "NUMERIC", "unit": "Ct" },
    {
      "name": "Internal Control",
      "code": "IC",
      "type": "NUMERIC",
      "unit": "Ct"
    }
  ]
}
```

### Template File Inventory

See [contracts/supported-analyzers.md](contracts/supported-analyzers.md) for the
authoritative analyzer inventory (13 Madagascar analyzers, fixture IDs
2000-2012) and protocol details.

---

## 12. Horiba ABX Analyzer Implementation (M9-M10)

### Overview

The Horiba ABX Pentra 60 and Micros 60 are hematology analyzers used in
Madagascar facilities. Both use ASTM LIS2-A2 protocol over RS232 serial
connections. Since they share the same manufacturer and protocol, they are
implemented together in M9-M10.

### Analyzer Specifications

| Feature            | Horiba Pentra 60 C+        | Horiba Micros 60           |
| ------------------ | -------------------------- | -------------------------- |
| **Manufacturer**   | Horiba ABX                 | Horiba ABX                 |
| **Type**           | 5-Part Differential        | 3-Part Differential        |
| **Throughput**     | 60 samples/hour            | 60 samples/hour            |
| **Parameters**     | 26 (CBC + 5-DIFF)          | 18 (CBC + 3-DIFF)          |
| **Protocol**       | ASTM LIS2-A2               | ASTM LIS2-A2               |
| **Transport**      | RS232 Serial               | RS232 Serial               |
| **Baud Rate**      | 9600                       | 9600                       |
| **Data Bits**      | 8                          | 8                          |
| **Parity**         | None                       | None                       |
| **Stop Bits**      | 1                          | 1                          |
| **ASTM Header ID** | `ABX^PENTRA60` or `PENTRA` | `ABX^MICROS60` or `MICROS` |

### Test Parameters

**Horiba Pentra 60 (26 Parameters)**:

- CBC: WBC, RBC, HGB, HCT, MCV, MCH, MCHC, PLT, RDW, PDW, PCT, MPV
- 5-Part Differential: LYM%, LYM#, MON%, MON#, NEU%, NEU#, EOS%, EOS#, BAS%,
  BAS#
- Additional: LIC (Large Immature Cells), ALY (Atypical Lymphocytes)

**Horiba Micros 60 (18 Parameters)**:

- CBC: WBC, RBC, HGB, HCT, MCV, MCH, MCHC, PLT, RDW, PDW, PCT, MPV
- 3-Part Differential: LYM%, LYM#, MXD%, MXD#, NEU%, NEU#

### ASTM Message Format

Both analyzers follow the standard ASTM LIS2-A2 format. Sample message:

```astm
H|\^&|||ABX^PENTRA60^V2.0|||||||LIS2-A2|20250128080000
P|1||PAT-001|Rakoto^Jean||M|19800515
O|1|SAMPLE-001^LAB|CBC||20250128075500
R|1|^^^WBC|5.8|10^3/uL|4.0-10.0|N||F|20250128080100
R|2|^^^RBC|4.92|10^6/uL|4.0-5.5|N||F|20250128080100
R|3|^^^HGB|14.8|g/dL|12.0-17.5|N||F|20250128080100
R|4|^^^HCT|43.2|%|36-54|N||F|20250128080100
R|5|^^^MCV|87.8|fL|80-100|N||F|20250128080100
R|6|^^^MCH|30.1|pg|27-33|N||F|20250128080100
R|7|^^^MCHC|34.3|g/dL|32-36|N||F|20250128080100
R|8|^^^PLT|245|10^3/uL|150-400|N||F|20250128080100
R|9|^^^LYM%|32.5|%|20-40|N||F|20250128080100
R|10|^^^LYM#|1.89|10^3/uL|1.0-4.0|N||F|20250128080100
R|11|^^^MON%|6.2|%|2-8|N||F|20250128080100
R|12|^^^MON#|0.36|10^3/uL|0.2-0.8|N||F|20250128080100
R|13|^^^NEU%|58.1|%|40-70|N||F|20250128080100
R|14|^^^NEU#|3.37|10^3/uL|2.0-7.0|N||F|20250128080100
L|1|N
```

### Analyzer Identification Strategy

The plugins identify messages by examining the ASTM Header (H) segment:

1. **Primary**: Parse `H|...|ABX^PENTRA60^...` for manufacturer/model
2. **Fallback**: Check for `PENTRA` or `MICROS` in the sender field
3. **IP-based**: Use ASTM-HTTP bridge source IP for identification

### Implementation Architecture (Data Flow)

> **NOTE**: This diagram shows RUNTIME data flow. For plugin DEPLOYMENT
> architecture (where plugin files live, how they're built and loaded), see the
> "Plugin Deployment Architecture" section below.

```
┌──────────────────────────────────────────────────────────────────────┐
│                    RS232 → OpenELIS Data Flow                         │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐    ┌─────────────────┐    ┌─────────────────────┐  │
│  │ Horiba      │    │ ASTM-HTTP       │    │ OpenELIS            │  │
│  │ Pentra/     │───▶│ Bridge          │───▶│ /analyzer/astm      │  │
│  │ Micros 60   │    │ (RS232→TCP)     │    │ POST endpoint       │  │
│  └─────────────┘    └─────────────────┘    └──────────┬──────────┘  │
│       RS232              HTTP/ASTM                    │             │
│                                                       ▼             │
│                                          ┌─────────────────────┐    │
│                                          │ ASTMAnalyzerReader  │    │
│                                          │ → identifies plugin │    │
│                                          └──────────┬──────────┘    │
│                                                     │               │
│                          ┌──────────────────────────┴───────┐       │
│                          ▼                                  ▼       │
│                ┌─────────────────────┐      ┌─────────────────────┐ │
│                │ Pentra60Analyzer    │      │ Micros60Analyzer    │ │
│                │ LineInserter        │      │ LineInserter        │ │
│                └──────────┬──────────┘      └──────────┬──────────┘ │
│                          │                            │             │
│                          └────────────┬───────────────┘             │
│                                       ▼                             │
│                          ┌─────────────────────┐                    │
│                          │ MappingAware        │                    │
│                          │ AnalyzerLineInserter│                    │
│                          │ (wraps if mappings) │                    │
│                          └──────────┬──────────┘                    │
│                                     │                               │
│                                     ▼                               │
│                          ┌─────────────────────┐                    │
│                          │ AnalyzerResults     │                    │
│                          │ (persisted to DB)   │                    │
│                          └─────────────────────┘                    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Plugin Class Structure

Each Horiba plugin consists of:

1. **`HoribaXxxAnalyzer.java`**: Implements `AnalyzerImporterPlugin` interface

   - `isTargetAnalyzer(List<String> lines)`: Check ASTM header for
     identification
   - `isAnalyzerResult(List<String> lines)`: Check for R-segments (vs queries)
   - `getAnalyzerLineInserter()`: Return the line inserter instance
   - `getAnalyzerResponder()`: Return null (no bidirectional queries supported)

2. **`HoribaXxxAnalyzerLineInserter.java`**: Extends `AnalyzerLineInserter`
   - `insert(List<String> lines, String currentUserId)`: Parse ASTM and persist
   - `getError()`: Return error message if insert fails

### Test Field Mappings

| ASTM Code | LOINC Code | OpenELIS Test Name | Units   |
| --------- | ---------- | ------------------ | ------- |
| WBC       | 6690-2     | White Blood Cells  | 10^3/μL |
| RBC       | 789-8      | Red Blood Cells    | 10^6/μL |
| HGB       | 718-7      | Hemoglobin         | g/dL    |
| HCT       | 4544-3     | Hematocrit         | %       |
| MCV       | 787-2      | MCV                | fL      |
| MCH       | 785-6      | MCH                | pg      |
| MCHC      | 786-4      | MCHC               | g/dL    |
| PLT       | 777-3      | Platelet Count     | 10^3/μL |
| LYM%      | 736-9      | Lymphocyte %       | %       |
| LYM#      | 731-0      | Lymphocyte Count   | 10^3/μL |
| NEU%      | 770-8      | Neutrophil %       | %       |
| NEU#      | 751-8      | Neutrophil Count   | 10^3/μL |
| MON%      | 5905-5     | Monocyte %         | %       |
| MON#      | 742-7      | Monocyte Count     | 10^3/μL |
| EOS%      | 713-8      | Eosinophil %       | %       |
| EOS#      | 711-2      | Eosinophil Count   | 10^3/μL |
| BAS%      | 706-2      | Basophil %         | %       |
| BAS#      | 704-7      | Basophil Count     | 10^3/μL |

### Plugin Deployment Architecture (CRITICAL)

Per `docs/analyzer.md` (lines 5-8):

> "The older model, which we are phasing out, has the code for importing
> analyzer results as part of the core of OpenELIS. **The newer model which all
> new analyzers should use is a plugin model.**"

**External Plugin JAR Pattern (MANDATORY for all new analyzers):**

| Aspect           | Detail                                                            |
| ---------------- | ----------------------------------------------------------------- |
| **Location**     | `plugins/analyzers/{PluginName}/` (git submodule)                 |
| **Build**        | Maven with `pom.xml` → standalone JAR                             |
| **Deploy**       | Copy JAR to `/var/lib/openelis-global/plugins/`                   |
| **Discovery**    | PluginLoader scans `/var/lib/openelis-global/plugins/` at startup |
| **Registration** | `connect()` method called by PluginLoader                         |
| **Spring**       | NO `@Component`, `@PostConstruct`, or `@DependsOn`                |
| **Package**      | `uw.edu.itech.{PluginName}` (convention from WeberAnalyzer)       |

**Registration Code Pattern:**

```java
// In plugins/analyzers/{PluginName}/src/main/java/.../
public class MyAnalyzer implements AnalyzerImporterPlugin {

    @Override
    public boolean connect() {
        // Called by PluginLoader AFTER Spring context is ready
        List<TestMapping> mappings = createTestMappings();
        PluginAnalyzerService.getInstance()
            .addAnalyzerDatabaseParts(name, desc, mappings, true);
        PluginAnalyzerService.getInstance().registerAnalyzer(this);
        return true;
    }
}
```

**Anti-Pattern (DO NOT USE):**

```java
// DO NOT put analyzer code in src/main/java/
@Component  // ❌ WRONG
@DependsOn("pluginAnalyzerService")  // ❌ WRONG
public class MyAnalyzer implements AnalyzerImporterPlugin {
    @PostConstruct  // ❌ WRONG
    public void register() { ... }
}
```

**Canonical References:**

- `docs/analyzer.md` — External plugin model documentation
- `docs/astm.md` — ASTM protocol and bridge setup
- Commit 511754dae (2021) — Removed ALL bundled JARs from
  `src/main/resources/plugin/`
- `plugins/analyzers/WeberAnalyzer/` — Reference implementation

### References

- **`docs/analyzer.md`** — External plugin model (MANDATORY reading)
- **`docs/astm.md`** — ASTM protocol configuration
- **`plugins/analyzers/WeberAnalyzer/`** — Reference plugin implementation
- [Horiba ABX Pentra 60 C+ Brochure](https://www.cardinalhealth.com/content/dam/corp/web/documents/brochure/horiba-hematology-ABX-pentra-60C-plus-brochure.pdf)
- [ABX Micros 60 Product Page](https://www.horiba.com/usa/healthcare/products/detail/action/show/Product/abx-micros-60-1835/)
- [CLSI LIS02-A2 Standard](https://clsi.org/shop/standards/lis02/)

---

## Summary of Key Decisions

| Area               | Decision                             | Rationale                                |
| ------------------ | ------------------------------------ | ---------------------------------------- |
| HL7 Library        | HAPI HL7 v2                          | Industry standard, already in codebase   |
| Serial Library     | jSerialComm 2.x                      | Pure Java, Docker-friendly               |
| File Watching      | WatchService + Commons CSV           | Native OS support, reliable parsing      |
| Order Export       | Manual trigger, async processing     | Per clarification, deadline scope        |
| Plugin Integration | Wrapper pattern                      | Backward compatible, non-invasive        |
| Simulator          | Expand analyzer-mock-server (Python) | Multi-protocol support, CI/CD ready      |
| Location Hierarchy | Reuse Organization/Location          | Per clarification, simpler integration   |
| Template Schema    | JSON-based analyzer templates        | Standardized config for all 12 analyzers |

### Tool Architecture Clarification

**CRITICAL**: Two separate tools exist with different purposes:

| Tool                         | Purpose                                                           | Language |
| ---------------------------- | ----------------------------------------------------------------- | -------- |
| **openelis-analyzer-bridge** | Production ASTM adapter (between physical analyzers and OpenELIS) | Java     |
| **analyzer-mock-server**     | Testing simulator (simulates analyzers for development/CI)        | Python   |

Feature 011 expands **analyzer-mock-server** to support multiple protocols (HL7,
RS232, File) for comprehensive testing. The production
**openelis-analyzer-bridge** remains unchanged.

---

**Research Completed**: 2026-01-22 | **Updated**: 2026-01-23 (added template
schema, tool architecture clarification) **All NEEDS CLARIFICATION items
resolved**: Yes (via spec clarification session)
