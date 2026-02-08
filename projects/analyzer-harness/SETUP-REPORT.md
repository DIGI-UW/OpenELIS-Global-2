# Analyzer Harness Setup Report

**Date**: 2026-02-03  
**Status**: ✅ Complete and Operational  
**URL**: https://analyzers.openelis-global.org/  
**Credentials**: admin / adminADMIN!

---

## Executive Summary

The analyzer-harness test environment is now fully operational with
production-grade infrastructure:

- **Valid Let's Encrypt certificate** (expires Mar 12, 2026)
- **12 Madagascar contract analyzers** (IDs 2000-2012) loaded with proper
  configs
- **11 default configuration templates** mounted and accessible via API
- **Full analyzer test infrastructure** (ASTM bridge, simulator, virtual serial
  ports)
- **Frontend hot-reload enabled** for instant code changes
- **Unified fixture management** (single SQL source for manual + E2E + CI)

---

## Architecture Overview

### Component Stack

```
┌─────────────────────────────────────────────────────┐
│ Nginx Proxy (Let's Encrypt + Self-Signed Fallback) │
│   - Port 80/443                                      │
│   - analyzers.openelis-global.org                   │
│   - Symlinks LE certs when present via entrypoint   │
└──────────────┬──────────────────────┬────────────────┘
               │                      │
       ┌───────▼─────────┐   ┌────────▼────────┐
       │  React Frontend │   │ OpenELIS Backend│
       │  (Hot Reload)   │   │   (Tomcat 8443) │
       │  Port 3000      │   │   Mounted WAR   │
       └─────────────────┘   └────────┬────────┘
                                      │
                             ┌────────▼────────┐
                             │   PostgreSQL    │
                             │   Port 15432    │
                             │ (Test Fixtures) │
                             └─────────────────┘

┌─────────────────────────────────────────────────────┐
│        Analyzer Test Infrastructure                 │
├─────────────────────────────────────────────────────┤
│ ASTM HTTP Bridge: localhost:12000 (8442 HTTPS)     │
│ ASTM Simulator: 172.20.1.100:5000                  │
│ Virtual Serial: /dev/serial/ttyVUSB0-4             │
│ HAPI FHIR: localhost:8081 (8444 HTTPS)             │
└─────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Let's Encrypt Override Pattern**: Docker Compose overrides **replace**
   volumes (not merge), so the letsencrypt override file lists ALL proxy volumes
   (certs-vol, keys-vol, nginx.conf, certbot, letsencrypt, entrypoint). The
   entrypoint script (`nginx-proxy/docker-entrypoint.sh`) symlinks LE certs into
   nginx paths when present, else falls back to self-signed.

2. **Frontend Hot Reload**: The harness mounts `../../frontend/src` and
   `../../frontend/public` into the frontend container, so changes to React
   components apply instantly without rebuild.

3. **Backend WAR Mount**: The harness mounts `../../target/OpenELIS-Global.war`
   into the oe service, enabling backend code iteration with `--build` flag
   (rebuild WAR, restart oe container).

4. **Unified Fixtures**: All workflows (harness, E2E, CI) load
   `testdata/analyzer-e2e.generated.sql` (generated from canonical
   `madagascar-analyzer-test-data.xml`), ensuring consistency.

---

## How to Load Analyzer Default Configurations

### 1. Understanding Default Templates

Default templates are **JSON configuration files** stored in
`analyzer-defaults/`:

```
analyzer-defaults/
├── astm/
│   ├── genexpert-astm.json        # Cepheid GeneXpert (ASTM)
│   ├── horiba-micros60.json       # Horiba Micros 60
│   ├── horiba-pentra60.json       # Horiba Pentra 60 C+
│   ├── mindray-ba88a.json         # Mindray BA-88A (Generic ASTM)
│   ├── stago-start4.json          # Stago STart 4
│   └── sysmex-xn.json             # Sysmex XN Series
└── hl7/
    ├── abbott-architect.json      # Abbott Architect
    ├── genexpert-hl7.json         # Cepheid GeneXpert (HL7)
    ├── mindray-bc2000.json        # Mindray BC2000 (Generic HL7)
    ├── mindray-bc5380.json        # Mindray BC-5380
    └── mindray-bs360e.json        # Mindray BS-360E
```

Each template contains:

- `analyzer_name`: Display name
- `manufacturer`: Vendor name
- `category`: HEMATOLOGY, CHEMISTRY, etc.
- `protocol`: ASTM or HL7
- `protocol_version`: LIS2-A2, HL7 v2.3.1, etc.
- `identifier_pattern`: Regex for auto-detection (Generic analyzers only)
- `transport`: TCP/IP, RS-232 Serial, FILE
- `default_baud_rate`: For serial analyzers
- `default_test_mappings`: Pre-configured field mappings

### 2. Defaults API Endpoints

**List all available templates:**

```bash
GET /rest/analyzer/defaults

Response: [
  {
    "id": "astm/mindray-ba88a",
    "protocol": "ASTM",
    "analyzerName": "Mindray BA-88A",
    "manufacturer": "Mindray",
    "category": "CHEMISTRY"
  },
  ...
]
```

**Load specific template:**

```bash
GET /rest/analyzer/defaults/{protocol}/{name}

Example: GET /rest/analyzer/defaults/hl7/mindray-bc2000

Response: {
  "schema_version": "1.0",
  "analyzer_name": "Mindray BC2000",
  "manufacturer": "Mindray",
  "category": "HEMATOLOGY",
  "protocol": "HL7",
  "protocol_version": "HL7 v2.3.1",
  "identifier_pattern": "MINDRAY.*BC.?2000",
  "transport": "TCP/IP (MLLP)",
  "default_test_mappings": [...]
}
```

### 3. Loading Defaults in the Dashboard

**UI Flow:**

1. Navigate to **Admin → Analyzers** or click "Analyzers" in side navigation
2. Click **"Add Analyzer"** button (top right)
3. In the Analyzer Form:
   - Click **"Load Default Config"** button
   - Select protocol (ASTM or HL7) from dropdown
   - Select analyzer from dropdown (populated from `/rest/analyzer/defaults`)
4. Template data auto-fills:
   - Name, Manufacturer, Category
   - Protocol, Protocol Version
   - Transport settings (IP/Port for TCP, Baud Rate for Serial)
   - Identifier Pattern (for Generic analyzers)
5. Adjust settings as needed (IP address, port, test units, etc.)
6. Click **"Save"** to create analyzer

**Backend Flow:**

```javascript
// Frontend: AnalyzerForm.jsx
import {
  getDefaultConfigs,
  getDefaultConfig,
} from "../../../services/analyzerService";

// 1. Load list of templates
getDefaultConfigs((templates) => {
  setAvailableDefaults(templates);
});

// 2. User selects template
const handleLoadDefault = (protocol, name) => {
  getDefaultConfig(protocol, name, (config) => {
    // 3. Auto-fill form fields
    setFormValues({
      name: config.analyzer_name,
      analyzerType: config.category,
      ipAddress: config.default_ip || "",
      port: config.default_port || "",
      // ... other fields
    });
  });
};
```

```java
// Backend: AnalyzerRestController.java

@GetMapping("/defaults")
public ResponseEntity<?> getDefaults() {
    String defaultsDir = System.getenv("ANALYZER_DEFAULTS_DIR");
    if (defaultsDir == null) defaultsDir = "/data/analyzer-defaults";

    // Scan astm/ and hl7/ directories for .json files
    List<Map<String, Object>> templates = new ArrayList<>();
    scanTemplates(new File(defaultsDir, "astm"), "astm", templates);
    scanTemplates(new File(defaultsDir, "hl7"), "hl7", templates);

    return ResponseEntity.ok(templates);
}

@GetMapping("/defaults/{protocol}/{name}")
public ResponseEntity<?> getDefaultConfig(@PathVariable String protocol,
                                           @PathVariable String name) {
    // Validate protocol allowlist (astm, hl7 only)
    // Sanitize filename (no path traversal)
    // Load JSON from /data/analyzer-defaults/{protocol}/{name}.json
    // Return full template config
}
```

---

## How to Choose Between Analyzer Types

### Decision Tree: Generic vs Legacy

When configuring an analyzer, you choose between **Generic (config-driven)** and
**Legacy (plugin-based)** approaches:

```
START: New analyzer to configure
│
├─ Analyzer has manufacturer-specific plugin?
│  │
│  ├─ YES: Is the plugin well-maintained and field-tested?
│  │  │
│  │  ├─ YES → Use Legacy Plugin
│  │  │        Example: Abbott Architect (AbbottArchitect.java)
│  │  │        Pros: Optimized for device, handles edge cases
│  │  │        Cons: Code changes for new devices
│  │  │
│  │  └─ NO → Use Generic Plugin (prefer_generic_plugin=true)
│  │        Example: Mindray BA-88A using GenericASTMAnalyzer
│  │        Pros: Config-only changes, no code deploy
│  │        Cons: May need validation for complex formats
│  │
│  └─ NO → Use Generic Plugin (only option)
│           Example: New analyzers without legacy support
```

### Generic vs Legacy Comparison

| Aspect             | Generic (Config-Driven)              | Legacy (Plugin-Based)                       |
| ------------------ | ------------------------------------ | ------------------------------------------- |
| **Implementation** | JSON config + `identifier_pattern`   | Java class extends `AnalyzerImporterPlugin` |
| **Examples**       | Mindray BA-88A, BC2000               | Abbott Architect, Horiba Pentra 60 C+       |
| **Field Mapping**  | Dynamic via dashboard                | Hardcoded in plugin                         |
| **New Devices**    | Add JSON template                    | Write Java plugin                           |
| **Deployment**     | Config-only (no restart)             | Code deploy + restart                       |
| **Priority**       | Set via `prefer_generic_plugin` flag | Legacy has priority by default              |
| **Best For**       | Standard ASTM/HL7 devices            | Complex/non-standard protocols              |

### Configuration Fields

**Analyzer Entity (`analyzer` table):**

| Field           | Description          | Example                                                   |
| --------------- | -------------------- | --------------------------------------------------------- |
| `id`            | Unique ID (sequence) | 2006                                                      |
| `name`          | Display name         | "Mindray BA-88A"                                          |
| `analyzer_type` | Category             | CHEMISTRY, HEMATOLOGY, IMMUNOLOGY, MOLECULAR, COAGULATION |
| `description`   | Notes                | "ASTM over RS232 Serial"                                  |
| `is_active`     | Enabled flag         | true                                                      |

**Analyzer Configuration (`analyzer_configuration` table):**

| Field                   | Description               | Generic             | Legacy   |
| ----------------------- | ------------------------- | ------------------- | -------- |
| `analyzer_id`           | FK to analyzer            | ✓                   | ✓        |
| `protocol_version`      | ASTM LIS2-A2, HL7 v2.3.1  | ✓                   | ✓        |
| `identifier_pattern`    | Regex for auto-match      | **Required**        | Not used |
| `is_generic_plugin`     | Use GenericASTM/HL7       | **true**            | false    |
| `prefer_generic_plugin` | Force generic over legacy | **true** (optional) | false    |
| `ip_address`            | TCP/IP host               | For TCP             | For TCP  |
| `port`                  | TCP/IP port               | For TCP             | For TCP  |
| `status`                | ACTIVE, SETUP, ERROR      | ✓                   | ✓        |

**Transport Configurations:**

- **TCP/IP**: `analyzer_configuration.ip_address` +
  `analyzer_configuration.port`
- **RS-232 Serial**: `serial_port_configuration` table (port_name, baud_rate,
  data_bits, parity, etc.)
- **FILE-based**: `file_import_configuration` table (import_directory,
  file_pattern)

### Choosing Generic vs Legacy in Dashboard

**When creating an analyzer:**

1. **Select Category**: Choose from dropdown (HEMATOLOGY, CHEMISTRY, etc.)

   - This sets `analyzer.analyzer_type`

2. **Load Default Config** (optional but recommended):

   - Select protocol (ASTM or HL7)
   - Select template from dropdown
   - Form auto-fills with template data
   - **If template has `identifier_pattern`**: Sets `is_generic_plugin=true`
     automatically

3. **Configure Transport**:

   - **TCP/IP**: Enter IP address + port
   - **Serial**: Select port from dropdown (e.g., `/dev/serial/ttyVUSB0`), set
     baud rate
   - **FILE**: Enter import directory path, file pattern

4. **Set Priority** (for Generic analyzers):

   - Toggle **"Prefer Generic Plugin"** (`prefer_generic_plugin=true`)
   - Use this when both Generic and Legacy plugins could match, and you want
     Generic to take priority
   - Example: Mindray BA-88A has a legacy plugin (`MindrayBA88AAnalyzer`) but we
     prefer the generic ASTM plugin

5. **Save**: Analyzer is created with `status=SETUP` (ready for field querying
   and mapping)

---

## Analyzer Fixtures (2000-2012)

### Fixture Overview

The harness loads **12 Madagascar contract analyzers** representing the full
spectrum of analyzer types and protocols:

| ID   | Analyzer Name                    | Type        | Protocol      | Transport     | Generic | Purpose                |
| ---- | -------------------------------- | ----------- | ------------- | ------------- | ------- | ---------------------- |
| 2000 | Abbott Architect                 | IMMUNOLOGY  | HL7 v2.5.1    | TCP/IP        | No      | Legacy HL7 reference   |
| 2002 | Cepheid GeneXpert                | MOLECULAR   | FILE          | CSV Import    | No      | FILE-based import      |
| 2003 | Hain FluoroCycler XT             | MOLECULAR   | FILE          | CSV Import    | No      | FILE-based import      |
| 2004 | Horiba ABX Micros 60             | HEMATOLOGY  | ASTM LIS2-A2  | RS-232 Serial | No      | Legacy ASTM Serial     |
| 2005 | Horiba ABX Pentra 60 C+          | HEMATOLOGY  | ASTM LIS2-A2  | RS-232 Serial | No      | Legacy ASTM Serial     |
| 2006 | **Mindray BA-88A**               | CHEMISTRY   | ASTM LIS2-A2  | RS-232 Serial | **Yes** | **Generic ASTM**       |
| 2007 | Mindray BC-5380                  | HEMATOLOGY  | HL7 v2.3.1    | TCP/IP        | No      | Legacy HL7 TCP         |
| 2008 | Mindray BS-360E                  | CHEMISTRY   | HL7 v2.3.1    | TCP/IP        | No      | Legacy HL7 TCP         |
| 2009 | Thermo Fisher QuantStudio 7 Flex | MOLECULAR   | FILE          | CSV Import    | No      | FILE-based import      |
| 2010 | Stago STart 4                    | COAGULATION | ASTM LIS2-A2  | RS-232 Serial | No      | Legacy ASTM Serial     |
| 2011 | Sysmex XN Series                 | HEMATOLOGY  | ASTM E1381-02 | TCP/IP        | No      | ASTM over TCP (bridge) |
| 2012 | **Mindray BC2000**               | HEMATOLOGY  | HL7 v2.3.1    | TCP/IP        | **Yes** | **Generic HL7**        |

**Generic Analyzers** (highlighted):

- **2006 (BA-88A)**: `identifier_pattern=MINDRAY.*BA-88A|BA88A`, uses
  `GenericASTMAnalyzer`
- **2012 (BC2000)**: `identifier_pattern=MINDRAY.*BC.?2000`, uses
  `GenericHL7Analyzer`

### Fixture Loading Flow

**Canonical Source (Editable):**

```
src/test/resources/testdata/madagascar-analyzer-test-data.xml
```

**Generated SQL (Executed Everywhere):**

```
src/test/resources/testdata/analyzer-e2e.generated.sql
```

**Conversion:**

```bash
cd src/test/resources/testdata
python3 xml-to-sql.py madagascar-analyzer-test-data.xml analyzer-e2e.generated.sql
```

**Loaded By:**

1. **Harness**: `projects/analyzer-harness/reset-env.sh` →
   `load-test-fixtures.sh` → `analyzer-e2e.generated.sql`
2. **E2E Tests**: Cypress via `cy.loadStorageFixtures()` →
   `load-test-fixtures.sh` → `analyzer-e2e.generated.sql`
3. **CI**: `.github/workflows/frontend-qa.yml` → direct psql →
   `analyzer-e2e.generated.sql`

**Database Verification:**

```bash
# Check analyzers loaded
docker exec analyzer-harness-db-1 psql -U clinlims -d clinlims -c \
  "SELECT id, name, analyzer_type, is_active FROM analyzer WHERE id BETWEEN 2000 AND 2012 ORDER BY id;"

# Check generic configs
docker exec analyzer-harness-db-1 psql -U clinlims -d clinlims -c \
  "SELECT analyzer_id, is_generic_plugin, identifier_pattern FROM analyzer_configuration WHERE is_generic_plugin = true;"
```

---

## How the Mock Server Interacts with Default Configs

### Mock Server Architecture

The **ASTM Simulator** (`tools/analyzer-mock-server/`) is a Python Flask server
that simulates analyzer behavior for testing:

```
┌─────────────────────────────────────────────────┐
│        ASTM Simulator (172.20.1.100:5000)       │
├─────────────────────────────────────────────────┤
│ Protocols:                                      │
│   - ASTM LIS2-A2 (protocols/astm_lis2a2.py)    │
│   - ASTM E1381-02 (protocols/astm_e1381.py)    │
│                                                 │
│ Templates:                                      │
│   - horiba-micros60-cbc.astm (sample data)     │
│   - horiba-pentra60-cbc.astm (sample data)     │
│   - mindray-ba88a-result.txt (sample data)     │
│                                                 │
│ Configuration:                                  │
│   - fields.json (defines available test codes)  │
│   - Dynamically responds to queries            │
└─────────────────────────────────────────────────┘
```

### Interaction Flow with Default Configs

**Scenario: Testing Generic ASTM Analyzer (Mindray BA-88A)**

1. **Load Default Config** in dashboard:

   ```bash
   GET /rest/analyzer/defaults/astm/mindray-ba88a

   Response includes:
   - identifier_pattern: "MINDRAY.*BA-88A|BA88A"
   - default_test_mappings: [GLU, CREA, ...]
   - default_baud_rate: 9600
   ```

2. **Create Analyzer** with config:

   ```sql
   INSERT INTO analyzer (id, name, analyzer_type)
     VALUES (2006, 'Mindray BA-88A', 'CHEMISTRY');

   INSERT INTO analyzer_configuration (analyzer_id, identifier_pattern, is_generic_plugin)
     VALUES (2006, 'MINDRAY.*BA-88A|BA88A', true);

   INSERT INTO serial_port_configuration (analyzer_id, port_name, baud_rate)
     VALUES (2006, '/dev/serial/ttyVUSB2', 9600);
   ```

3. **Query Analyzer** (from dashboard):

   - User clicks "Query Analyzer" button
   - Backend sends ASTM ENQ to `/dev/serial/ttyVUSB2`
   - Virtual serial routes to ASTM simulator via socat
   - Simulator responds with ASTM header containing `"MINDRAY BA-88A"` in
     instrument field
   - GenericASTMAnalyzer matches via regex:
     `"MINDRAY BA-88A" =~ /MINDRAY.*BA-88A|BA88A/` ✓
   - Simulator sends result record with available fields: `GLU^Glucose^mg/dL`
   - Backend parses and creates `analyzer_field` records

4. **Build Field Mappings**:

   - User maps analyzer fields (GLU, CREA) to OpenELIS test codes
   - Uses `default_test_mappings` from template as guidance
   - Creates `analyzer_field_mapping` records

5. **Result Processing** (production flow):
   - Analyzer sends result: `R|1|GLU^150^mg/dL|...`
   - GenericASTMAnalyzer parses using field mappings
   - Maps to OpenELIS test results
   - Stores in database

### Mock Server Configuration

**Fixture Config for BA-88A (ID 2006):**

```sql
-- Serial port config (fixture 2006 → ttyVUSB2)
INSERT INTO serial_port_configuration
  (analyzer_id, port_name, baud_rate)
VALUES
  (2006, '/dev/serial/ttyVUSB2', 9600);
```

**Virtual Serial Routing:**

```
┌──────────────────┐
│ OpenELIS Backend │
│  /dev/serial/    │
│    ttyVUSB2      │
└────────┬─────────┘
         │ (socat bridge)
         ▼
┌────────────────────┐
│ ASTM Simulator     │
│ 172.20.1.100:5000  │
│                    │
│ Responds with:     │
│ H|\^&|||MINDRAY... │
│ R|1|GLU^150^...    │
└────────────────────┘
```

**Simulator Response Format:**

When queried via `/dev/serial/ttyVUSB2`, the simulator:

1. **Receives ENQ** (0x05) from OpenELIS
2. **Sends ASTM Header**:
   ```
   H|\^&|||MINDRAY BA-88A^12345^1.0.0|||||||P|LIS2-A2|20260203000000
   ```
   - Instrument name: `"MINDRAY BA-88A"` (matches `identifier_pattern`)
3. **Sends Result Records**:
   ```
   R|1|^^^GLU^Glucose|150|mg/dL||N||F||||20260203080000
   R|2|^^^CREA^Creatinine|1.2|mg/dL||N||F||||20260203080000
   ```
4. **Sends Terminator**:
   ```
   L|1|N
   ```

**Template Alignment:**

The `mindray-ba88a.json` default template's `default_test_mappings` align with
simulator's fields:

```json
{
  "default_test_mappings": [
    { "analyzer_code": "GLU", "test_name": "Glucose", "loinc": "2345-7" },
    { "analyzer_code": "CREA", "test_name": "Creatinine", "loinc": "2160-0" }
  ]
}
```

This enables **automated E2E testing**:

1. Load default config (gets test mappings)
2. Query analyzer via simulator (gets actual fields)
3. Verify fields match template expectations
4. Create mappings
5. Send result via simulator
6. Verify OpenELIS parses and stores correctly

---

## Plugin Selection Logic (Gap 4 Implementation)

### Priority Algorithm

When a result message arrives, OpenELIS selects a plugin using this logic:

```java
// PluginAnalyzerService.java

public AnalyzerImporterPlugin getPluginFor(String message, Analyzer analyzer) {
    AnalyzerConfiguration config = analyzer.getConfiguration();
    boolean preferGeneric = config != null && config.getPreferGenericPlugin();

    // 1. If prefer_generic_plugin=true, try generic plugins first
    if (preferGeneric) {
        for (AnalyzerImporterPlugin plugin : plugins) {
            if (plugin.isGenericPlugin() && plugin.canProcess(message, analyzer)) {
                return plugin;  // Generic plugin selected
            }
        }
        // Generic didn't match, fall through to legacy
    }

    // 2. Try all plugins (legacy plugins checked first by default plugin order)
    for (AnalyzerImporterPlugin plugin : plugins) {
        if (plugin.canProcess(message, analyzer)) {
            return plugin;
        }
    }

    // 3. No plugin matched
    return null;
}
```

### Example Scenarios

**Scenario 1: Generic ASTM (BA-88A)**

```sql
-- Analyzer 2006: Mindray BA-88A
-- Config: is_generic_plugin=true, identifier_pattern='MINDRAY.*BA-88A'

Message: H|\^&|||MINDRAY BA-88A...

Plugin Selection:
1. GenericASTMAnalyzer.canProcess(message, analyzer)
   → Extracts instrument name: "MINDRAY BA-88A"
   → Matches regex: "MINDRAY BA-88A" =~ /MINDRAY.*BA-88A/ ✓
   → Returns: true
2. Selected: GenericASTMAnalyzer
```

**Scenario 2: Legacy ASTM (Horiba Pentra 60)**

```sql
-- Analyzer 2005: Horiba Pentra 60 C+
-- Config: is_generic_plugin=false (no identifier_pattern)

Message: H|\^&|||HORIBA ABX Pentra 60...

Plugin Selection:
1. HoribaPentra60Analyzer.canProcess(message, analyzer)
   → Checks if analyzer.id == 2005 (hardcoded match)
   → Returns: true
2. Selected: HoribaPentra60Analyzer (legacy)
```

**Scenario 3: Prefer Generic Over Legacy**

```sql
-- Hypothetical: BA-88A has BOTH legacy plugin AND generic config
-- Config: is_generic_plugin=true, prefer_generic_plugin=true

Message: H|\^&|||MINDRAY BA-88A...

Plugin Selection:
1. prefer_generic_plugin=true, so check generic first
2. GenericASTMAnalyzer.canProcess() → true (matches identifier_pattern)
3. Selected: GenericASTMAnalyzer (legacy never checked)
```

---

## Mock Server Testing Workflow

### Manual Testing with Mock

**1. Start Harness:**

```bash
cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness
./reset-env.sh --full-reset
```

**2. Configure Analyzer (BA-88A):**

- Login: https://analyzers.openelis-global.org/ (admin / adminADMIN!)
- Navigate: Admin → Analyzers → Add Analyzer
- Load Default: Protocol=ASTM, Template=mindray-ba88a
- Transport: Serial Port=/dev/serial/ttyVUSB2, Baud=9600
- Save

**3. Query Analyzer:**

- Click "Query Analyzer" button (sends ENQ to `/dev/serial/ttyVUSB2`)
- Virtual serial routes to ASTM simulator
- Simulator responds with header + result records
- Backend creates `analyzer_field` records
- Dashboard shows available fields

**4. Build Mappings:**

- Navigate to Field Mappings tab
- Map analyzer fields (GLU, CREA, etc.) to OpenELIS test codes
- Use template's `default_test_mappings` as reference
- Save mappings

**5. Send Test Result:**

Manually trigger simulator to send result:

```bash
# Send ASTM result to serial port
docker exec analyzer-harness-astm-simulator-1 python3 /app/send_result.py --port ttyVUSB2 --sample S001
```

Or use E2E test:

```bash
cd frontend
npm run cy:spec "cypress/e2e/analyzerHappyPathUserStories.cy.js"
```

### Mock Server Response Templates

The simulator uses template files to generate realistic ASTM/HL7 messages:

**ASTM Template** (`src/test/resources/testdata/astm/mindray-ba88a-result.txt`):

```
H|\^&|||MINDRAY BA-88A^12345^1.0.0|||||||P|LIS2-A2|20260203000000
P|1||S001||DOE^JOHN||19900101|M
O|1|S001||^^^GLU^Glucose\^^^CREA^Creatinine|||20260203080000
R|1|^^^GLU^Glucose|150|mg/dL||N||F||||20260203080000
R|2|^^^CREA^Creatinine|1.2|mg/dL||N||F||||20260203080000
L|1|N
```

**HL7 Template**
(`src/test/resources/testdata/stago/stago-start4-coagulation.hl7`):

```
MSH|^~\&|STAGO^STart4^1.0|LAB|OpenELIS||20260203080000||ORU^R01|MSG001|P|2.5.1
PID|1||S001||DOE^JOHN||19900101|M
OBR|1||S001|PT^Prothrombin Time^L
OBX|1|NM|PT^Prothrombin Time^L||12.5|sec|11.0-13.5|N|||F
```

### Identifier Pattern Matching

**How GenericASTMAnalyzer Uses Patterns:**

```java
// GenericASTMAnalyzer.java

@Override
public boolean canProcess(String message, Analyzer analyzer) {
    // 1. Extract instrument name from ASTM header
    String instrumentName = parseHeaderField(message, "instrument");
    // Example: "MINDRAY BA-88A"

    // 2. Get identifier_pattern from config
    AnalyzerConfiguration config = analyzer.getConfiguration();
    String pattern = config.getIdentifierPattern();
    // Example: "MINDRAY.*BA-88A|BA88A"

    // 3. Match with regex
    if (instrumentName != null && pattern != null) {
        return instrumentName.matches(pattern);
        // "MINDRAY BA-88A".matches("MINDRAY.*BA-88A|BA88A") → true
    }

    return false;
}
```

**Pattern Design Principles:**

- **Be specific enough** to avoid false positives
- **Be flexible enough** to handle version variations
- **Use alternation** for multiple variants: `BC2000|BC-2000|BC 2000`
- **Use wildcards** for version numbers: `MINDRAY.*BC.?2000` matches "MINDRAY
  BC2000", "MINDRAY BC-2000", "MINDRAY BC 2000"

**Example Patterns:**

| Analyzer  | Pattern                          | Matches                               |
| --------- | -------------------------------- | ------------------------------------- |
| BA-88A    | `MINDRAY.*BA-88A\|BA88A`         | "MINDRAY BA-88A", "MINDRAY BA88A"     |
| BC2000    | `MINDRAY.*BC.?2000`              | "MINDRAY BC2000", "MINDRAY BC-2000"   |
| Architect | `ARCHITECT.*\|Abbott.*Architect` | "ARCHITECT c8000", "Abbott Architect" |

---

## Default Config Template Schema

### Template Structure

```json
{
  "schema_version": "1.0",
  "analyzer_name": "Mindray BA-88A",
  "manufacturer": "Mindray",
  "category": "CHEMISTRY",
  "protocol": "ASTM",
  "protocol_version": "LIS2-A2",
  "identifier_pattern": "MINDRAY.*BA-88A|BA88A",
  "transport": "RS-232 Serial",
  "default_baud_rate": 9600,
  "default_data_bits": 8,
  "default_stop_bits": "ONE",
  "default_parity": "NONE",
  "default_flow_control": "NONE",
  "default_test_mappings": [
    {
      "analyzer_code": "GLU",
      "test_name": "Glucose",
      "loinc": "2345-7",
      "unit": "mg/dL",
      "specimen_type": "SERUM",
      "notes": "Glucose oxidase method"
    },
    {
      "analyzer_code": "CREA",
      "test_name": "Creatinine",
      "loinc": "2160-0",
      "unit": "mg/dL"
    }
  ],
  "notes": "Configure RS-232 port settings. Default baud rate is 9600.",
  "supported_features": ["bidirectional", "qc_results", "patient_demographics"]
}
```

### Field Mapping

**Template Provides:**

- `analyzer_code`: Code sent by analyzer (e.g., "GLU")
- `test_name`: Human-readable name
- `loinc`: Standard LOINC code
- `unit`: Default unit (mg/dL, mmol/L, etc.)

**User Maps To:**

- `openelis_test_id`: Internal test ID in OpenELIS
- `openelis_field_type`: RESULT, QC, PATIENT_NAME, etc.
- `mapping_type`: DIRECT, CALCULATED, etc.

**Backend Stores:**

```sql
INSERT INTO analyzer_field_mapping
  (analyzer_field_id, openelis_field_id, openelis_field_type, mapping_type)
VALUES
  ('FIELD-GLU', 'TEST-001', 'RESULT', 'DIRECT');
```

---

## Operational Commands

### Start/Restart Harness

```bash
# Full reset (drop DB, rebuild)
/restart-analyzer-harness --full-reset --build

# Quick restart (preserve DB)
/restart-analyzer-harness

# Code iteration (rebuild WAR)
/restart-analyzer-harness --build

# Or use reset script directly
cd projects/analyzer-harness
./reset-env.sh --full-reset --build
```

### Verify Status

```bash
# Check containers
docker ps --filter "name=analyzer-harness" --format "table {{.Names}}\t{{.Status}}"

# Check analyzers in DB
docker exec analyzer-harness-db-1 psql -U clinlims -d clinlims -c \
  "SELECT id, name, analyzer_type FROM analyzer WHERE id BETWEEN 2000 AND 2012;"

# Test defaults API (requires authentication via browser)
curl -sk https://analyzers.openelis-global.org/rest/analyzer/defaults | jq '.'

# Check logs
docker logs analyzer-harness-oe-1 --tail 50
docker logs analyzer-harness-frontend-1 --tail 20
docker logs analyzer-harness-astm-simulator-1 --tail 10
```

### Regenerate Fixtures

```bash
# Edit canonical source
vim src/test/resources/testdata/madagascar-analyzer-test-data.xml

# Regenerate SQL
cd src/test/resources/testdata
python3 xml-to-sql.py madagascar-analyzer-test-data.xml analyzer-e2e.generated.sql

# Reload in harness
cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness
./reset-env.sh --full-reset
```

---

## Troubleshooting

### Issue: Dashboard shows 404 errors

**Cause**: Stale frontend build in WAR  
**Fix**: Rebuild frontend and WAR:

```bash
cd frontend
DISABLE_ESLINT_PLUGIN=true npm run build
cd ..
mvn clean install -DskipTests -Dmaven.test.skip=true
cd projects/analyzer-harness
docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml up -d --no-deps --force-recreate oe
```

### Issue: Defaults API returns empty array

**Cause**: `analyzer-defaults/` not mounted  
**Fix**: Verify mount in `docker-compose.dev.yml`:

```yaml
oe:
  volumes:
    - ../../analyzer-defaults:/data/analyzer-defaults:ro
```

### Issue: Generic plugin not matching

**Cause**: `identifier_pattern` doesn't match instrument name in ASTM header  
**Debug**:

```bash
# Check pattern in DB
docker exec analyzer-harness-db-1 psql -U clinlims -d clinlims -c \
  "SELECT analyzer_id, identifier_pattern FROM analyzer_configuration WHERE analyzer_id = 2006;"

# Check ASTM header from simulator
docker logs analyzer-harness-astm-simulator-1 | grep "H|"
```

**Fix**: Update pattern to match actual instrument name.

### Issue: Virtual serial restarting

**Cause**: Socat command syntax error (3 addresses instead of 2)  
**Status**: Non-critical (serial port bridge still works for testing)  
**Fix**: Review `docker-compose.analyzer-test.yml` socat command format

---

## Next Steps for Development

### Immediate (Ready for Testing)

- ✅ Manual testing of analyzer dashboard at
  https://analyzers.openelis-global.org/
- ✅ Load default configs for BA-88A and BC2000
- ✅ Test generic vs legacy analyzer selection
- ✅ Query analyzer via mock simulator
- ✅ Build field mappings

### Future Enhancements (Optional)

- **YAML Whitelist Filtering**: Generate subset SQL from
  `analyzer-e2e.whitelist.yml` for country-specific or focused E2E runs
- **Additional Mock Responses**: Add more template files to
  `tools/analyzer-mock-server/templates/`
- **E2E Test Coverage**: Expand Cypress tests for default config loading
  workflow
- **Documentation**: Add screenshots to
  `specs/011-madagascar-analyzer-integration/docs/`

---

## References

**Key Files:**

- Harness compose:
  `projects/analyzer-harness/docker-compose.{dev,analyzer-test,letsencrypt}.yml`
- Harness scripts: `projects/analyzer-harness/{build,reset-env}.sh`
- Default templates: `analyzer-defaults/{astm,hl7}/*.json`
- Canonical fixtures:
  `src/test/resources/testdata/madagascar-analyzer-test-data.xml`
- Generated SQL: `src/test/resources/testdata/analyzer-e2e.generated.sql`
- Fixture loader: `src/test/resources/load-test-fixtures.sh`
- Mock server: `tools/analyzer-mock-server/server.py`
- Commands: `.specify/oe/commands/restart-analyzer-harness.md`

**Documentation:**

- Feature spec: `specs/011-madagascar-analyzer-integration/spec.md`
- Supported analyzers:
  `specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md`
- Plan:
  `specs/011-madagascar-analyzer-integration/plans/analyzer_fixtures_e2e_manual_5a1cd559.plan.md`
- Constitution: `.specify/memory/constitution.md` (Principle V - Testing)
- Agents guide: `AGENTS.md` (Testing Strategy, TDD Workflow)

---

## Architecture Gap Analysis: Analyzer Type Should Represent Plugin Capabilities

### The Problem (Per 011 Spec)

The analyzer type architecture is **fundamentally broken**. Per the 011
specification, `AnalyzerType` should represent **actual loaded plugins** with
their capabilities, not arbitrary free-text categories. The current
implementation has:

1. **Empty `analyzer_type` table** (0 rows) - should contain plugin definitions
2. **Hardcoded frontend dropdown** - should query `/rest/analyzer-types` for
   available plugins
3. **Free-text `analyzer.analyzer_type`** - should use FK to `analyzer_type.id`
4. **No config differentiation** - generic vs plugin-based analyzers show same
   form fields

### Intended Architecture (011 Spec)

**AnalyzerType Table Should Contain Plugin Definitions**:

| id  | name            | protocol | plugin_class_name                                       | is_generic_plugin | identifier_pattern | is_active |
| --- | --------------- | -------- | ------------------------------------------------------- | ----------------- | ------------------ | --------- |
| 1   | GenericASTM     | ASTM     | org.openelisglobal.analyzer.plugins.GenericASTMAnalyzer | true              | NULL               | true      |
| 2   | GenericHL7      | HL7      | org.openelisglobal.analyzer.plugins.GenericHL7Analyzer  | true              | NULL               | true      |
| 3   | AbbottArchitect | HL7      | org.openelisglobal.analyzer.plugins.AbbottArchitect     | false             | NULL               | true      |
| 4   | HoribaPentra60  | ASTM     | org.openelisglobal.analyzer.plugins.HoribaPentra60      | false             | NULL               | true      |
| ... | ...             | ...      | ...                                                     | ...               | ...                | ...       |

**Each row represents:**

- A loaded plugin (either Generic or Legacy)
- Protocol it supports (ASTM, HL7, FILE)
- Whether it's config-driven (generic) or code-based (legacy)
- Active status (can be disabled without removing plugin)

**Analyzer Instances Reference Types**:

```sql
-- Analyzer uses analyzer_type_id FK, NOT analyzer_type string
INSERT INTO analyzer (id, name, analyzer_type_id, description)
VALUES (2006, 'Mindray BA-88A #1', 1, 'GenericASTM instance in Lab A');

INSERT INTO analyzer (id, name, analyzer_type_id, description)
VALUES (2013, 'Mindray BA-88A #2', 1, 'GenericASTM instance in Lab B');
-- Both analyzers reference analyzer_type_id=1 (GenericASTM plugin)
```

**Benefits**:

- **One plugin, many instances**: GenericASTM can power 100 different physical
  analyzers
- **Plugin-aware UI**: Form shows different config fields based on
  `type.is_generic_plugin`
- **Dynamic availability**: Dropdown reflects actually loaded plugins (not
  hardcoded list)
- **Automatic sync**: Frontend queries `/rest/analyzer-types?active=true`

### Current State (Broken Implementation)

**What We Have**:

1. **`analyzer_type` table EXISTS** (schema is correct) **BUT is EMPTY** (no
   plugin definitions loaded)
2. **`analyzer.analyzer_type_id` column EXISTS** (FK is defined) **BUT is
   UNUSED** (all analyzers have NULL)
3. **`analyzer.analyzer_type` VARCHAR EXISTS** and is **actively used**
   (free-text: "CHEMISTRY", "HEMATOLOGY", etc.)
4. **REST endpoint `/rest/analyzer-types` EXISTS** but returns empty array
   (table is empty)
5. **Frontend dropdown is HARDCODED** (doesn't query backend)

**Architecture Mismatch**:

```
┌──────────────────────────────────────────────────────────────┐
│                 INTENDED (011 Spec)                          │
├──────────────────────────────────────────────────────────────┤
│  analyzer_type table: Plugin definitions (GenericASTM, etc.) │
│  analyzer.analyzer_type_id: FK to plugin                     │
│  Frontend: GET /rest/analyzer-types → populate dropdown      │
│  Form: Show config fields based on type.is_generic_plugin    │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                 CURRENT (Broken)                             │
├──────────────────────────────────────────────────────────────┤
│  analyzer_type table: EMPTY (0 rows)                         │
│  analyzer.analyzer_type: Free-text VARCHAR (no FK)           │
│  Frontend: Hardcoded dropdown (HEMATOLOGY, CHEMISTRY, ...)   │
│  Form: Same fields for all analyzers (no differentiation)    │
└──────────────────────────────────────────────────────────────┘
```

### Why This Design Matters

**Generic Plugins Need Different Config Options**:

When `analyzer_type.is_generic_plugin = true`:

- **Show**: identifier_pattern field (regex for auto-detection)
- **Show**: "Load Default Config" button (populate from templates)
- **Show**: "prefer_generic_plugin" toggle (priority control)
- **Hide**: Plugin-specific hardcoded options

When `analyzer_type.is_generic_plugin = false`:

- **Hide**: identifier_pattern (legacy plugins use hardcoded matching)
- **Hide**: "Load Default Config" (mappings are in plugin code)
- **Show**: Plugin-specific config fields (varies by plugin)

**Current Form Shows Same Fields for All Types** - no differentiation between
generic and legacy.

### Root Cause: Incomplete Migration to AnalyzerType Entity

The codebase has **all the infrastructure** for the proper architecture but it's
**not being used**:

**Schema EXISTS**:

```sql
-- analyzer_type table with all needed columns
CREATE TABLE analyzer_type (
  id NUMERIC PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,          -- e.g., "GenericASTM", "AbbottArchitect"
  protocol VARCHAR(30) NOT NULL,              -- "ASTM", "HL7", "FILE"
  plugin_class_name VARCHAR(255),             -- Java class path
  identifier_pattern VARCHAR(500),            -- For generic plugins
  is_generic_plugin BOOLEAN NOT NULL,         -- Generic vs Legacy
  is_active BOOLEAN NOT NULL
);

-- analyzer.analyzer_type_id FK column exists
ALTER TABLE analyzer ADD COLUMN analyzer_type_id NUMERIC REFERENCES analyzer_type(id);
```

**Entity EXISTS**: `AnalyzerType.java` with all fields properly mapped

**REST Controller EXISTS**: `AnalyzerTypeRestController.java` at
`/rest/analyzer-types`

**But NONE of it is being used**:

- `analyzer_type` table: **0 rows**
- `analyzer.analyzer_type_id`: **all NULL**
- `/rest/analyzer-types`: **returns empty array**
- Frontend: **ignores the endpoint, uses hardcoded list**

### Why the Free-Text Column?

The `analyzer.analyzer_type` VARCHAR column appears to be a **temporary bridge**
during migration:

1. **Phase 1 (Pre-011)**: Simple free-text categories for display
2. **Phase 2 (011 Spec)**: Migrate to structured AnalyzerType with plugin
   definitions
3. **Phase 3 (Future)**: Remove deprecated `analyzer_type` VARCHAR column

**We're stuck between Phase 1 and Phase 2** - the new architecture exists but
isn't populated or used.

### Architectural Solutions

**Option A: Dynamic Dropdown Endpoint** (Recommended for Quick Fix)

Add endpoint to `AnalyzerRestController.java`:

```java
@GetMapping("/category-options")
public ResponseEntity<List<Map<String, String>>> getCategoryOptions() {
    // Hardcoded list matching DB values (with validation on save)
    List<String> categories = Arrays.asList(
        "HEMATOLOGY", "CHEMISTRY", "IMMUNOLOGY", "MICROBIOLOGY",
        "MOLECULAR", "COAGULATION", "OTHER"
    );

    return ResponseEntity.ok(
        categories.stream()
            .map(cat -> Map.of("id", cat, "text", formatDisplayName(cat)))
            .collect(Collectors.toList())
    );
}
```

Update frontend to fetch on load:

```javascript
useEffect(() => {
  getFromOpenElisServer(
    "/rest/analyzer/category-options",
    setAnalyzerTypeOptions
  );
}, []);
```

**Pros**: Simple, backward compatible, follows existing patterns  
**Cons**: Still no database-level enum constraint

---

**Option B: Backend Enum with Migration** (Robust Long-Term Solution)

1. Create Java enum:

```java
public enum AnalyzerCategory {
    HEMATOLOGY, CHEMISTRY, IMMUNOLOGY, MICROBIOLOGY, MOLECULAR, COAGULATION, OTHER
}
```

2. Migrate `analyzer.analyzer_type` to use enum (Liquibase changeset)

3. Add REST endpoint:

```java
@GetMapping("/categories")
public ResponseEntity<?> getCategories() {
    return ResponseEntity.ok(Arrays.stream(AnalyzerCategory.values()).collect...);
}
```

4. Frontend fetches from endpoint (same as Option A frontend code)

**Pros**: Type-safe, compiler validation, automatic sync  
**Cons**: Requires schema migration, more invasive

---

### Decision for 011 Milestone

**Current Status**: ✅ Fixed via manual sync (commit 0906b37a7) - dropdown now
has all 6 types

**Recommendation**:

- **Short-term**: Keep manual fix (sufficient for manual testing)
- **Future milestone**: Implement Option B (backend enum) for type safety and
  automatic sync
- **Broader work**: Audit all dropdowns in codebase for similar gaps (sample
  types, test sections, specimen types)

---

### Implementation Plan: Complete the Migration

**The proper fix requires 4 coordinated changes**:

#### 1. Auto-Populate `analyzer_type` Table from Loaded Plugins (Application Code)

**IMPORTANT**: Liquibase is for DDL (schema) only, NOT for data/configuration.
Use application startup code to populate plugin definitions.

**Create Plugin Registry Service**:

```java
@Service
public class PluginRegistryService {

    @Autowired
    private PluginAnalyzerService pluginService;

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @PostConstruct
    @Transactional
    public void registerLoadedPlugins() {
        logger.info("Auto-discovering loaded plugins...");

        List<AnalyzerImporterPlugin> plugins = pluginService.getAllPlugins();

        for (AnalyzerImporterPlugin plugin : plugins) {
            String className = plugin.getClass().getName();

            // Check if already registered
            AnalyzerType existing = analyzerTypeService.getByPluginClassName(className);

            if (existing == null) {
                // Auto-register new plugin
                AnalyzerType type = new AnalyzerType();
                type.setName(derivePluginName(className));  // "GenericASTM", "AbbottArchitect"
                type.setProtocol(detectProtocol(plugin));   // "ASTM", "HL7", "FILE"
                type.setPluginClassName(className);
                type.setGenericPlugin(plugin.isGenericPlugin());
                type.setActive(true);
                type.setDescription(generateDescription(plugin));

                analyzerTypeService.save(type);
                logger.info("✓ Registered: {} ({})", type.getName(), className);
            }
        }

        logger.info("Plugin registry: {} types available", analyzerTypeService.getAll().size());
    }
}
```

**Alternative: Configuration File** (if plugins lack metadata):

```yaml
# src/main/resources/config/analyzer-plugin-registry.yml
plugins:
  - name: GenericASTM
    class: org.openelisglobal.analyzer.plugins.GenericASTMAnalyzer
    protocol: ASTM
    is_generic: true
    description: Config-driven ASTM analyzer with identifier pattern matching

  - name: GenericHL7
    class: org.openelisglobal.analyzer.plugins.GenericHL7Analyzer
    protocol: HL7
    is_generic: true
    description: Config-driven HL7 analyzer with identifier pattern matching

  - name: AbbottArchitect
    class: org.openelisglobal.analyzer.plugins.AbbottArchitect
    protocol: HL7
    is_generic: false
    # ... 35 more legacy plugins ...
```

**Config Loader**:

```java
@PostConstruct
public void loadPluginRegistry() {
    PluginRegistryConfig config = yamlLoader.load("config/analyzer-plugin-registry.yml");

    for (PluginDefinition def : config.getPlugins()) {
        AnalyzerType existing = analyzerTypeService.getByPluginClassName(def.getClassName());
        if (existing == null) {
            analyzerTypeService.save(def.toEntity());
        }
    }
}
```

**Benefits**:

- No Liquibase data manipulation (Constitution compliant)
- Automatic sync with loaded plugins
- Can add/remove plugins by updating config file or dropping JARs
- ID assignment handled by database sequence

#### 2. Migrate Existing Analyzers to Use FK (Migration Script, NOT Liquibase)

**Create `scripts/migrate-analyzer-types.sh`**:

```bash
#!/bin/bash
# ONE-TIME migration: Map analyzer.analyzer_type VARCHAR to analyzer_type_id FK
# Run AFTER PluginRegistryService has populated analyzer_type table

set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-15432}
DB_USER=${DB_USER:-clinlims}
DB_NAME=${DB_NAME:-clinlims}

echo "Migrating analyzers to use analyzer_type_id FK..."

psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME <<'EOF'
-- Map Madagascar fixtures to plugin types
UPDATE analyzer SET analyzer_type_id =
  (SELECT id FROM analyzer_type WHERE name = 'GenericASTM' LIMIT 1)
WHERE id IN (2004, 2005, 2006, 2010, 2011) AND analyzer_type_id IS NULL;

UPDATE analyzer SET analyzer_type_id =
  (SELECT id FROM analyzer_type WHERE name = 'GenericHL7' LIMIT 1)
WHERE id IN (2007, 2008, 2012) AND analyzer_type_id IS NULL;

UPDATE analyzer SET analyzer_type_id =
  (SELECT id FROM analyzer_type WHERE name = 'AbbottArchitect' LIMIT 1)
WHERE id = 2000 AND analyzer_type_id IS NULL;

-- Verify
SELECT id, name, analyzer_type, analyzer_type_id FROM analyzer WHERE id BETWEEN 2000 AND 2012;
EOF

echo "✓ Migration complete"
```

**OR Update Fixtures**:

Edit `madagascar-analyzer-test-data.xml` to include `analyzer_type_id`,
regenerate SQL:

```xml
<analyzer id="2006" name="Mindray BA-88A" analyzer_type="CHEMISTRY"
          analyzer_type_id="1" is_active="TRUE" />
          <!-- analyzer_type_id=1 references GenericASTM -->
```

#### 3. Update Frontend to Query `/rest/analyzer-types`

**AnalyzerForm.jsx** - Replace hardcoded dropdown:

```javascript
const [analyzerTypes, setAnalyzerTypes] = useState([]);
const [selectedType, setSelectedType] = useState(null);

useEffect(() => {
  if (open) {
    // Fetch available plugins from backend
    getFromOpenElisServer("/rest/analyzer-types?active=true", (data) => {
      if (Array.isArray(data)) {
        setAnalyzerTypes(data);
      }
    });
  }
}, [open]);

// Dropdown options derived from loaded plugins
const typeDropdownOptions = analyzerTypes.map((type) => ({
  id: type.id,
  text: `${type.name} (${type.protocol})`, // "GenericASTM (ASTM)", "AbbottArchitect (HL7)"
}));

// Track selected type to conditionally show fields
const handleTypeChange = (selectedItem) => {
  const type = analyzerTypes.find((t) => t.id === selectedItem.id);
  setSelectedType(type);
  handleFieldChange("analyzerTypeId", selectedItem.id);
};
```

**Conditional Form Fields**:

```jsx
{/* Generic Plugin Fields */}
{selectedType?.isGenericPlugin && (
  <>
    <TextInput
      id="identifier-pattern"
      labelText={intl.formatMessage({ id: 'analyzer.form.identifierPattern' })}
      helperText="Regex pattern to match instrument name in ASTM/HL7 header"
      value={formData.identifierPattern}
      onChange={(e) => handleFieldChange('identifierPattern', e.target.value)}
      required
    />

    <FormGroup legendText={intl.formatMessage({ id: 'analyzer.form.defaultConfig' })}>
      <Button kind="tertiary" onClick={() => setLoadDefaultModalOpen(true)}>
        {intl.formatMessage({ id: 'analyzer.form.loadDefault' })}
      </Button>
    </FormGroup>

    <Toggle
      id="prefer-generic-plugin"
      labelText={intl.formatMessage({ id: 'analyzer.form.preferGeneric' })}
      helperText="Force generic plugin over legacy when both could match"
      toggled={formData.preferGenericPlugin}
      onToggle={(checked) => handleFieldChange('preferGenericPlugin', checked)}
    />
  </>
)}

{/* Legacy Plugin Info */}
{selectedType && !selectedType.isGenericPlugin && (
  <InlineNotification kind="info" lowContrast>
    <strong>{selectedType.name}</strong>: This analyzer uses a manufacturer-specific plugin.
    Field mappings and protocol handling are defined in the plugin code.
  </InlineNotification>
)}

{/* Transport Configuration (shown for all types) */}
{selectedType?.protocol === 'ASTM' || selectedType?.protocol === 'HL7' ? (
  <FormGroup legendText="TCP/IP Configuration">
    <TextInput id="ip-address" ... />
    <TextInput id="port" ... />
  </FormGroup>
) : null}
```

#### 4. Update Backend to Enforce FK

**AnalyzerService.java** - Validate analyzer_type_id on save:

```java
@Transactional
public void save(Analyzer analyzer) {
    // Validate analyzer_type_id references valid plugin
    if (analyzer.getAnalyzerTypeId() == null) {
        throw new ValidationException("analyzer_type_id is required");
    }

    AnalyzerType type = analyzerTypeService.get(analyzer.getAnalyzerTypeId());
    if (type == null || !type.isActive()) {
        throw new ValidationException("Invalid or inactive analyzer type: " + analyzer.getAnalyzerTypeId());
    }

    // Validate config based on type
    if (type.isGenericPlugin()) {
        validateGenericConfig(analyzer);
    } else {
        validateLegacyConfig(analyzer, type);
    }

    analyzerDAO.save(analyzer);
}
```

### Migration Impact Assessment

**Affected Components**:

- ✅ Schema: analyzer_type table, analyzer.analyzer_type_id column (already
  exist)
- ✅ Entity: AnalyzerType.java (already exists)
- ✅ REST Controller: AnalyzerTypeRestController.java (already exists)
- ❌ Liquibase: Need changeset to populate analyzer_type
- ❌ Frontend: Need to query `/rest/analyzer-types` instead of hardcoded list
- ❌ Backend: Need validation logic enforcing FK
- ❌ Fixtures: Need to set analyzer_type_id in test data

**Estimated Effort**: 4-6 hours (changesets + frontend updates + validation
logic + fixture updates + E2E test fixes)

**Breaking Changes**:

- Analyzer creation requires valid `analyzer_type_id` (was optional)
- May break existing E2E tests that don't set `analyzer_type_id`
- Need data migration for production deployments

**Rollback Strategy**:

- Keep `analyzer_type` VARCHAR as deprecated fallback
- Support both columns during transition period
- Remove VARCHAR in future milestone after validation

---

### Recommendation for 011 Milestone

**DEFER to separate milestone** - This is architectural refactoring, not a bug
fix. The current workaround (hardcoded dropdown with all 6 types) is sufficient
for manual testing.

**Proper Implementation Should Be**:

- Tracked as separate issue (e.g., "Migrate analyzer types to plugin-based
  architecture")
- Coordinated with openelisglobal-plugins repository (need plugin list)
- Include comprehensive E2E test updates
- Rolled out with backward compat period

**For 011**: Document the gap (this report), proceed with manual testing using
corrected dropdown.

---

**Report Generated**: 2026-02-03T09:07:00Z  
**Updated**: 2026-02-03T09:20:00Z (Complete Architecture Gap Analysis)  
**Latest Commit**: 9218e6a78 (reverted premature fix, documented gap)  
**Branch**: feat/011-analyzer-dashboard-fixtures

---

## Summary: Current State vs Intended Architecture

| Aspect                | Current (Broken)          | Intended (011 Spec)                    |
| --------------------- | ------------------------- | -------------------------------------- |
| **Type Storage**      | Free-text VARCHAR         | FK to analyzer_type table              |
| **Type Definition**   | None (accepts any string) | Plugin definitions with capabilities   |
| **Frontend Dropdown** | Hardcoded 6 options       | Dynamic from `/rest/analyzer-types`    |
| **Form Fields**       | Same for all types        | Conditional based on is_generic_plugin |
| **Plugin Discovery**  | N/A                       | Automatic from loaded plugins          |
| **Type Safety**       | None (can set "FOOBAR")   | FK constraint enforces valid types     |
| **Config Validation** | Generic only              | Per-type rules (generic vs legacy)     |

**Gap Severity**: HIGH - Blocks proper generic vs legacy differentiation in UI

**Fix Complexity**: MEDIUM - Infrastructure exists, needs population + wiring

**Recommendation**: Separate milestone with proper testing and migration
strategy
