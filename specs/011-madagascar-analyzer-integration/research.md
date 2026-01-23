# Research: Madagascar Analyzer Integration

**Feature**: 150-madagascar-analyzer-integration **Date**: 2026-01-22
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

### Decision: Expand astm-mock-server to Multi-Protocol Analyzer Simulator

**Rationale**: The existing astm-mock-server (Python) provides ASTM simulation.
Expanding it to support HL7, RS232, and file-based protocols enables
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

```sql
-- Instrument metadata extension
CREATE TABLE instrument_metadata (
    id VARCHAR(36) PRIMARY KEY,
    analyzer_id INT NOT NULL REFERENCES analyzer(id),
    installation_date DATE,
    warranty_expiration DATE,
    software_version VARCHAR(50),
    calibration_due_date DATE,
    service_status VARCHAR(20),
    notes TEXT,
    fhir_uuid UUID NOT NULL UNIQUE,
    sys_user_id INT NOT NULL,
    lastupdated TIMESTAMP DEFAULT NOW()
);

-- Order export tracking
CREATE TABLE order_export (
    id VARCHAR(36) PRIMARY KEY,
    sample_id INT NOT NULL REFERENCES sample(id),
    analyzer_id INT NOT NULL REFERENCES analyzer(id),
    status VARCHAR(20) NOT NULL,  -- PENDING, SENT, ACKNOWLEDGED, RESULTS_RECEIVED, FAILED, EXPIRED
    message_type VARCHAR(10),      -- ASTM, HL7
    message_content TEXT,
    sent_timestamp TIMESTAMP,
    acknowledged_timestamp TIMESTAMP,
    results_received_timestamp TIMESTAMP,
    retry_count INT DEFAULT 0,
    error_message TEXT,
    fhir_uuid UUID NOT NULL UNIQUE,
    sys_user_id INT NOT NULL,
    lastupdated TIMESTAMP DEFAULT NOW()
);

-- RS232 configuration
CREATE TABLE serial_port_configuration (
    id VARCHAR(36) PRIMARY KEY,
    analyzer_id INT NOT NULL REFERENCES analyzer(id),
    port_name VARCHAR(50) NOT NULL,
    baud_rate INT DEFAULT 9600,
    data_bits INT DEFAULT 8,
    stop_bits VARCHAR(10) DEFAULT 'ONE',
    parity VARCHAR(10) DEFAULT 'NONE',
    flow_control VARCHAR(20) DEFAULT 'NONE',
    active BOOLEAN DEFAULT true,
    fhir_uuid UUID NOT NULL UNIQUE,
    sys_user_id INT NOT NULL,
    lastupdated TIMESTAMP DEFAULT NOW()
);

-- File import configuration
CREATE TABLE file_import_configuration (
    id VARCHAR(36) PRIMARY KEY,
    analyzer_id INT NOT NULL REFERENCES analyzer(id),
    import_directory VARCHAR(255) NOT NULL,
    file_pattern VARCHAR(100) DEFAULT '*.csv',
    archive_directory VARCHAR(255),
    error_directory VARCHAR(255),
    column_mappings JSONB,       -- {"Sample_ID": "sampleId", "Result": "result"}
    delimiter VARCHAR(10) DEFAULT ',',
    has_header BOOLEAN DEFAULT true,
    active BOOLEAN DEFAULT true,
    fhir_uuid UUID NOT NULL UNIQUE,
    sys_user_id INT NOT NULL,
    lastupdated TIMESTAMP DEFAULT NOW()
);

-- Instrument location history
CREATE TABLE instrument_location_history (
    id VARCHAR(36) PRIMARY KEY,
    instrument_metadata_id VARCHAR(36) NOT NULL REFERENCES instrument_metadata(id),
    organization_id INT REFERENCES organization(id),
    room_detail VARCHAR(100),
    effective_from DATE NOT NULL,
    effective_to DATE,
    moved_by_user_id INT,
    notes TEXT,
    fhir_uuid UUID NOT NULL UNIQUE,
    sys_user_id INT NOT NULL,
    lastupdated TIMESTAMP DEFAULT NOW()
);
```

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

## Summary of Key Decisions

| Area               | Decision                         | Rationale                              |
| ------------------ | -------------------------------- | -------------------------------------- |
| HL7 Library        | HAPI HL7 v2                      | Industry standard, already in codebase |
| Serial Library     | jSerialComm 2.x                  | Pure Java, Docker-friendly             |
| File Watching      | WatchService + Commons CSV       | Native OS support, reliable parsing    |
| Order Export       | Manual trigger, async processing | Per clarification, deadline scope      |
| Plugin Integration | Wrapper pattern                  | Backward compatible, non-invasive      |
| Simulator          | Expand astm-mock-server          | Reuse existing, add protocols          |
| Location Hierarchy | Reuse Organization/Location      | Per clarification, simpler integration |

---

**Research Completed**: 2026-01-22 **All NEEDS CLARIFICATION items resolved**:
Yes (via spec clarification session)
