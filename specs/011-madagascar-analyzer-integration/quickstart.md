# Quickstart: Madagascar Analyzer Integration (Feature 011)

**Feature**: 011-madagascar-analyzer-integration **Purpose**: Developer
onboarding guide for implementing Madagascar analyzer integration
**Prerequisite**: Familiarity with Feature 004 (ASTM Analyzer Mapping)

---

## Overview

This feature extends Feature 004 to support:

- **HL7 v2.x protocol** for Mindray, Sysmex, Abbott analyzers
- **RS232 serial communication** for Horiba, Stago, legacy analyzers
- **File-based import** for QuantStudio and FluoroCycler thermocyclers
- **Order export workflow** for bidirectional communication
- **Enhanced metadata** for instrument tracking

---

## Quick Setup

### 1. Development Environment

```bash
# Ensure Java 21
java -version  # Should show "openjdk version 21.x.x"

# Clone and setup (if not already done)
git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2
git checkout demo/madagascar

# Build submodules
cd dataexport && mvn clean install -DskipTests && cd ..

# Build OpenELIS
mvn clean install -DskipTests -Dmaven.test.skip=true

# Start dev environment
docker compose -f dev.docker-compose.yml up -d
```

### 2. Feature 004 Verification

Before starting Feature 011, verify Feature 004 is working:

```bash
# Access OpenELIS
open https://localhost/

# Navigate to: Analyzers > Analyzers Dashboard
# Verify: ASTM analyzer can be configured and receives results
```

### 3. Multi-Protocol Simulator

For development without physical analyzers:

```bash
# Bring up OpenELIS with the ASTM/HL7 simulator (from repo root)
docker compose -f dev.docker-compose.yml -f docker-compose.astm-test.yml up -d

# ASTM simulator: TCP port 5000 (openelis-astm-simulator)
# OpenELIS: https://localhost/ (or your DOMAIN)
```

**Abbott Architect HL7 scenario**

The simulator can generate HL7 ORU^R01 messages from the Abbott template so
OpenELIS routes them to the Abbott plugin (MSH-3=ARCHITECT, MSH-4=LAB). Run the
simulator locally with HL7 push:

```bash
# From repo root, ensure OpenELIS is up, then:
cd tools/analyzer-mock-server
pip install -r requirements.txt   # optional: for template validation
python server.py --hl7 --push https://localhost:8443 --hl7-template abbott_architect_hl7

# One-shot: push one message
python server.py --hl7 --push https://localhost:8443 --push-count 1

# With HTTP API (generate or push via curl):
python server.py --push https://localhost:8443 --api-port 8080
curl http://localhost:8080/simulate/hl7/abbott_architect_hl7   # generate one message
curl -X POST http://localhost:8080/simulate/hl7/abbott_architect_hl7 \
  -H "Content-Type: application/json" \
  -d '{"count": 1, "destination": "https://localhost:8443"}'
```

See `tools/analyzer-mock-server/README.md` for full HL7 options.

---

## Milestone Development Guide

### M1: HL7 Adapter

**Goal**: Parse HL7 ORU^R01 results, generate HL7 ORM^O01 orders

**Key Files to Create**:

```
src/main/java/org/openelisglobal/
├── analyzerimport/analyzerreaders/HL7AnalyzerReader.java
└── analyzer/service/HL7MessageService.java
```

**Implementation Steps**:

1. **Create HL7AnalyzerReader**:

   ```java
   // Extend existing pattern from ASTMAnalyzerReader
   public class HL7AnalyzerReader extends AnalyzerReader {
       private final HapiContext hapiContext = new DefaultHapiContext();

       public void readStream(InputStream inputStream) {
           Parser parser = hapiContext.getPipeParser();
           Message message = parser.parse(readAllFromStream(inputStream));

           if (message instanceof ORU_R01) {
               processResultMessage((ORU_R01) message);
           }
       }
   }
   ```

2. **Create HL7MessageService**:

   ```java
   @Service
   @Transactional
   public class HL7MessageServiceImpl implements HL7MessageService {
       public String generateOrderMessage(Sample sample, Analyzer analyzer) {
           ORM_O01 orm = new ORM_O01();
           // Populate MSH, PID, ORC, OBR segments
           return hapiContext.getPipeParser().encode(orm);
       }
   }
   ```

3. **Write Unit Tests**:
   ```java
   @Test
   public void testParseORU_R01_ExtractsPatientId() {
       String hl7Message = loadTestMessage("mindray-cbc-result.hl7");
       HL7AnalyzerReader reader = new HL7AnalyzerReader();
       // ...
   }
   ```

**Verification**:

- Unit tests pass for ORU^R01 parsing
- Unit tests pass for ORM^O01 generation
- Integration test: HL7 message → Field mapping → Results staging

---

### M2: RS232 Adapter

**Goal**: Serial port configuration and ASTM message reception

**Key Files to Create**:

```
src/main/java/org/openelisglobal/analyzer/
├── valueholder/SerialPortConfiguration.java
├── dao/SerialPortConfigurationDAO.java
├── service/SerialPortService.java
└── controller/SerialPortRestController.java
```

**Dependencies**:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.10.4</version>
</dependency>
```

**Implementation Steps**:

1. **Create Entity and DAO** (follow existing patterns)

2. **Create SerialPortService**:

   ```java
   @Service
   public class SerialPortServiceImpl implements SerialPortService {
       private final Map<Integer, SerialPort> openPorts = new ConcurrentHashMap<>();

       public void openPort(SerialPortConfiguration config) {
           SerialPort port = SerialPort.getCommPort(config.getPortName());
           port.setBaudRate(config.getBaudRate());
           // ... set other parameters
           port.openPort();
           openPorts.put(config.getAnalyzerId(), port);
       }
   }
   ```

3. **Docker Configuration** for serial pass-through:
   ```yaml
   # dev.docker-compose.yml additions
   services:
     oe.openelis.org:
       devices:
         - /dev/ttyUSB0:/dev/ttyUSB0
   ```

**Testing with Virtual Serial Ports**:

```bash
# Create virtual serial port pair
socat -d -d pty,raw,echo=0 pty,raw,echo=0
# Note the /dev/pts/X and /dev/pts/Y created
```

---

### M3: File Adapter

**Goal**: Watch directories for CSV/TXT files, parse and import results

**Key Files to Create**:

```
src/main/java/org/openelisglobal/analyzer/
├── valueholder/FileImportConfiguration.java
├── service/FileImportService.java
└── analyzerimport/analyzerreaders/FileAnalyzerReader.java
```

**Implementation Steps**:

1. **Create FileImportService with WatchService**:

   ```java
   @Service
   public class FileImportServiceImpl implements FileImportService {
       @PostConstruct
       public void startWatching() {
           ExecutorService executor = Executors.newSingleThreadExecutor();
           executor.submit(() -> {
               WatchService watchService = FileSystems.getDefault().newWatchService();
               // Register directories and watch for ENTRY_CREATE
           });
       }
   }
   ```

2. **Create FileAnalyzerReader for CSV parsing**:
   ```java
   public class FileAnalyzerReader extends AnalyzerReader {
       public void readFile(Path file, FileImportConfiguration config) {
           try (CSVParser parser = CSVFormat.DEFAULT
                   .withHeader()
                   .withDelimiter(config.getDelimiter().charAt(0))
                   .parse(new FileReader(file.toFile()))) {
               for (CSVRecord record : parser) {
                   processRecord(record, config.getColumnMappings());
               }
           }
       }
   }
   ```

**Test Data**:

```csv
# test-quantstudio-results.csv
Well,Sample Name,Target,Ct
A1,Patient001,Gene1,25.5
A2,Patient002,Gene1,28.3
```

---

### M14: Order Export Workflow

**Goal**: Manual order export with status tracking

**Key Files to Create**:

```
src/main/java/org/openelisglobal/analyzer/
├── valueholder/OrderExport.java
├── dao/OrderExportDAO.java
├── service/OrderExportService.java
└── controller/OrderExportRestController.java

frontend/src/components/analyzers/
├── OrderExport/
│   ├── OrderExportList.jsx
│   └── OrderExportModal.jsx
```

**Backend Implementation**:

1. **Create OrderExport entity** (see data-model.md)

2. **Create OrderExportService**:

   ```java
   @Service
   @Transactional
   public class OrderExportServiceImpl implements OrderExportService {

       @Async
       public void exportOrders(List<Integer> sampleIds, Integer analyzerId) {
           for (Integer sampleId : sampleIds) {
               OrderExport export = createExport(sampleId, analyzerId);
               try {
                   String message = generateMessage(export);
                   sendToAnalyzer(message, analyzerId);
                   export.setStatus(OrderExportStatus.SENT);
               } catch (Exception e) {
                   handleExportError(export, e);
               }
           }
       }
   }
   ```

**Frontend Implementation**:

1. **OrderExportList.jsx**:

   ```jsx
   import { DataTable, Button, Tag } from "@carbon/react";

   const OrderExportList = ({ analyzerId }) => {
     const { data: exports } = useSWR(
       `/rest/analyzer/order-export?analyzerId=${analyzerId}`
     );

     return (
       <DataTable rows={exports} headers={headers}>
         {/* Status column with color-coded Tags */}
       </DataTable>
     );
   };
   ```

2. **OrderExportModal.jsx**:

   ```jsx
   const OrderExportModal = ({ analyzerId, onExport }) => {
     const [selectedOrders, setSelectedOrders] = useState([]);

     return (
       <ComposedModal>
         <ModalHeader
           title={intl.formatMessage({ id: "analyzer.orderExport.title" })}
         />
         <ModalBody>{/* Pending orders table with checkboxes */}</ModalBody>
         <ModalFooter>
           <Button onClick={() => onExport(selectedOrders)}>
             {intl.formatMessage({ id: "analyzer.orderExport.export" })}
           </Button>
         </ModalFooter>
       </ComposedModal>
     );
   };
   ```

---

## Testing Guide

### Unit Tests

```bash
# Run specific test class
mvn test -Dtest=HL7MessageServiceTest

# Run all analyzer tests
mvn test -Dtest="org.openelisglobal.analyzer.*Test"
```

### Integration Tests

```bash
# Run with simulator
docker compose -f dev.docker-compose.yml up -d
mvn verify -Pit

# Check integration test coverage
open target/site/jacoco/index.html
```

### E2E Tests (Cypress)

```bash
# Run individual test (during development)
cd frontend
npm run cy:run -- --spec "cypress/e2e/orderExport.cy.js"

# Run with headed browser for debugging
npm run cy:open
```

---

## Common Patterns

### Extending Analyzer Configuration UI

Add new protocol-specific configuration tabs:

```jsx
// In AnalyzerConfiguration.jsx
<Tabs>
  <Tab label="General">{/* existing */}</Tab>
  <Tab label="HL7 Settings">
    <HL7Configuration />
  </Tab>
  <Tab label="Serial Port">
    <SerialConfiguration />
  </Tab>
  <Tab label="File Import">
    <FileImportConfiguration />
  </Tab>
</Tabs>
```

### Adding New Translation Keys

1. Add to `frontend/src/languages/en.json`:

   ```json
   {
     "analyzer.hl7.title": "HL7 Configuration"
   }
   ```

2. Add to `frontend/src/languages/fr.json`:

   ```json
   {
     "analyzer.hl7.title": "Configuration HL7"
   }
   ```

3. Use in component:
   ```jsx
   <h2>{intl.formatMessage({ id: "analyzer.hl7.title" })}</h2>
   ```

### Service Layer Transaction Pattern

```java
@Service
@Transactional  // Class-level for all public methods
public class MyServiceImpl implements MyService {

    @Transactional(readOnly = true)  // Override for read-only methods
    public List<Entity> findAll() {
        return dao.findAll();
    }

    // Write methods use class-level @Transactional
    public void save(Entity entity) {
        dao.save(entity);
    }
}
```

---

## Troubleshooting

### HL7 Parsing Errors

```java
// Enable HAPI debug logging
System.setProperty("ca.uhn.hl7v2.llp.debug", "true");

// Check for encoding issues
String message = new String(bytes, StandardCharsets.UTF_8);
```

### Serial Port Access Denied

```bash
# Linux: Add user to dialout group
sudo usermod -a -G dialout $USER
# Logout and login again

# Docker: Ensure device mapping
docker inspect oe.openelis.org | grep -A5 "Devices"
```

### File Import Not Detecting Files

```java
// Check WatchService registration
Path dir = Paths.get("/data/imports");
System.out.println("Watching: " + dir.toAbsolutePath());
System.out.println("Exists: " + Files.exists(dir));
System.out.println("Readable: " + Files.isReadable(dir));
```

---

## Reference Documentation

- **Feature 004 Spec**: `specs/004-astm-analyzer-mapping/spec.md`
- **Feature 011 Spec**: `specs/011-madagascar-analyzer-integration/spec.md`
- **Plan**: `specs/011-madagascar-analyzer-integration/plan.md`
- **Data Model**: `specs/011-madagascar-analyzer-integration/data-model.md`
- **Research**: `specs/011-madagascar-analyzer-integration/research.md`
- **Constitution**: `.specify/memory/constitution.md`
- **Testing Roadmap**: `.specify/guides/testing-roadmap.md`

---

**Quickstart Created**: 2026-01-22 **Next Step**: Begin with M1 (HL7 Adapter) or
M2/M3/M4 (parallel foundation milestones)
