# Plan: Introduce `ProtocolVersion` Enum

## Context

PR reviewer flagged that `protocolVersion` is a free-text `VARCHAR(20)` with no
valueset. Three code paths produce inconsistent values for the same protocol
(`"ASTM LIS2-A2"` vs bare `"LIS2-A2"`). Backend routing uses fragile
`contains()`/`startsWith()` string matching. No single source of truth exists.

---

## Current Architecture & Role of `protocolVersion`

### What the field represents

`protocolVersion` is supposed to describe the **message format** an analyzer
speaks — i.e., how to parse/construct the content of messages:

- **ASTM LIS2-A2** (CLSI standard) — pipe-delimited records: `H|\^&|||...`,
  `R|1|^^^WBC|...|NUMERIC`
- **HL7 v2.x** — segment-based messages: `MSH|^~\&|...`, `OBX|1|NM|...`

### How it flows through the system

```
                          ┌─────────────────────────┐
                          │   Frontend Form (JSX)    │
                          │                          │
                          │  Plugin selected (ASTM)  │
                          │  → protocolMap sets       │
                          │    "ASTM LIS2-A2"        │
                          │                          │
                          │  Default config loaded   │
                          │  → BUG: sets bare        │
                          │    "LIS2-A2" for ASTM    │
                          └──────────┬───────────────┘
                                     │ POST/PUT body
                                     ▼
                          ┌─────────────────────────┐
                          │   AnalyzerRestController │
                          │                          │
                          │  CREATE/UPDATE:          │
                          │  stored as-is, no        │
                          │  validation              │
                          │                          │
                          │  TEST-CONNECTION:        │
                          │  string-match routing:   │
                          │  "HL7..."  → testHl7()   │
                          │  "ASTM..." → testAstm()  │
                          │  "FILE"    → testFile()   │
                          │  "RS232"   → testSerial() │
                          └──────────┬───────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    ▼                ▼                 ▼
           ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
           │ TCP/IP       │ │ FileImport   │ │ SerialPort   │
           │ (ip + port   │ │ Config       │ │ Config       │
           │  on Analyzer)│ │ (own table)  │ │ (own table)  │
           └──────┬───────┘ └──────────────┘ └──────────────┘
                  │
                  ▼
           ┌──────────────────────────────────────────┐
           │         Analyzer Bridge (submodule)       │
           │                                           │
           │  Owns its OWN protocol model:             │
           │  • Protocol enum: ASTM / HL7 / CSV        │
           │  • ASTMVersion enum: LIS01_A / E1381_95   │
           │  • Handles ALL transport: TCP, Serial,     │
           │    File                                    │
           │                                           │
           │  OE sends messages → Bridge handles        │
           │  framing, checksums, handshake             │
           └──────────────────────────────────────────┘
```

### The problem: two concerns in one field

`protocolVersion` currently encodes **two orthogonal things**:

| Concern                                   | Examples                           | Should live where            |
| ----------------------------------------- | ---------------------------------- | ---------------------------- |
| **Message format** (how to parse content) | ASTM LIS2-A2, HL7 v2.3.1, HL7 v2.5 | `protocolVersion` field      |
| **Transport type** (how messages arrive)  | FILE, RS232                        | Derived from config entities |

Transport is **already modeled separately** by dedicated entities:

- `Analyzer.ipAddress` + `port` → TCP/IP
- `FileImportConfiguration` (own table) → file-based import
- `SerialPortConfiguration` (own table) → RS-232 serial

The bridge (submodule) handles all transport concerns. OE core should only care
about message format. FILE and RS232 are not message formats — they're transport
mechanisms.

### Where protocolVersion is consumed

1. **Test-connection routing** (`AnalyzerRestController:235-256`) — uses
   string-matching to dispatch. FILE and RS232 branches actually query their own
   config entities (`FileImportConfiguration`, `SerialPortConfiguration`) for
   the real test logic. `protocolVersion` is just the routing key.

2. **Query-service guard** (`AnalyzerQueryServiceImpl:85-92`) — blocks push-only
   analyzers (FILE/RS232) from being queried. This is a transport concern, not a
   message-format concern.

3. **Bridge communication** — the bridge has its own `Protocol` enum
   (`ASTM`/`HL7`/`CSV`) and `ASTMVersion` enum (`LIS01_A`/`E1381_95`). OpenELIS
   does NOT pass `protocolVersion` to the bridge — the bridge detects protocol
   from message content.

---

## Approach

**Separate message format from transport detection.**

### Enum: message formats only

| Constant       | Label (display) |
| -------------- | --------------- |
| `ASTM_LIS2_A2` | "ASTM LIS2-A2"  |
| `HL7_V2_3_1`   | "HL7 v2.3.1"    |
| `HL7_V2_5`     | "HL7 v2.5"      |

No FILE or RS232 — those are transport, not message format.

### Transport detection: derive from config entities

```java
// Test-connection routing (replaces string matching):
if (fileImportService.getByAnalyzerId(id).isPresent()) {
    return testFileConfiguration(analyzer);
} else if (serialPortService.getByAnalyzerId(id).isPresent()) {
    return testSerialConfiguration(id);
} else if (analyzer.getIpAddress() != null && analyzer.getPort() != null) {
    // Use message format to pick handshake type:
    if (analyzer.getProtocolVersion().isHl7()) return testHl7Connection(analyzer);
    else return testAstmTcpConnection(analyzer);
}

// Query-service guard (replaces string matching):
if (fileImportService.getByAnalyzerId(id).isPresent()
    || serialPortService.getByAnalyzerId(id).isPresent()) {
    throw new LIMSRuntimeException("Push-only transport, cannot query");
}
```

Follow existing `AnalyzerStatus` pattern: `@Enumerated(EnumType.STRING)`, enum
constant names stored in DB, frontend maps constants to display labels.

---

## File Changes

### 1. New: `src/main/java/org/openelisglobal/analyzer/valueholder/ProtocolVersion.java`

```java
public enum ProtocolVersion {
    ASTM_LIS2_A2("ASTM LIS2-A2"),
    HL7_V2_3_1("HL7 v2.3.1"),
    HL7_V2_5("HL7 v2.5");

    private final String label;
    // constructor, getLabel()

    /** Accept enum name OR legacy label string. Returns null if unrecognized. */
    public static ProtocolVersion fromValue(String v) { ... }

    /** Message format family helpers (for routing). */
    public boolean isAstm() { return this == ASTM_LIS2_A2; }
    public boolean isHl7()  { return this == HL7_V2_3_1 || this == HL7_V2_5; }
}
```

`fromValue()` accepts both `"ASTM_LIS2_A2"` (enum name) and `"ASTM LIS2-A2"`
(legacy label) for backward compat. Also handles legacy variants like bare
`"LIS2-A2"`.

### 2. Edit: `Analyzer.java` (valueholder)

```java
// Before:
@Column(name = "protocol_version", length = 20)
private String protocolVersion = "ASTM LIS2-A2";

// After:
@Column(name = "protocol_version", length = 20)
@Enumerated(EnumType.STRING)
private ProtocolVersion protocolVersion = ProtocolVersion.ASTM_LIS2_A2;
```

### 3. Edit: `AnalyzerForm.java` (DTO)

**Keep as `String`.** The DTO sits at the API boundary — accepts raw input from
frontend. Controller validates and converts to enum. This allows backward compat
with any client still sending legacy label strings.

### 4. Edit: `AnalyzerRestController.java`

**Create (line ~177):**

```java
ProtocolVersion pv = ProtocolVersion.fromValue(form.getProtocolVersion());
if (pv == null) {
    // return 400 with list of valid values
}
analyzer.setProtocolVersion(pv);
```

**Update (line ~379):** Same validation/conversion.

**Test connection routing (lines 235-256):** Replace string matching with
transport-first detection:

```java
// 1. Check transport config entities first
if (fileImportService.getByAnalyzerId(id).isPresent()) {
    response = testFileConfiguration(analyzer);
} else if (serialPortService.getByAnalyzerId(id).isPresent()) {
    response = testSerialConfiguration(id);
} else if (analyzer.getIpAddress() != null && analyzer.getPort() != null) {
    // 2. Use message format to pick handshake
    ProtocolVersion pv = analyzer.getProtocolVersion();
    if (pv.isHl7()) response = testHl7Connection(analyzer);
    else response = testAstmTcpConnection(analyzer);
} else {
    response = error("No transport configured");
}
```

**Response mapping (line ~505):** Return enum name (matches status pattern):

```java
map.put("protocolVersion", analyzer.getProtocolVersion().name());
```

### 5. Edit: `AnalyzerQueryServiceImpl.java` (lines 85-92)

Replace `contains("FILE") || contains("RS232")` with transport entity checks:

```java
if (fileImportService.getByAnalyzerId(analyzerId).isPresent()
    || serialPortService.getByAnalyzerId(analyzerId).isPresent()) {
    throw new LIMSRuntimeException("Push-only transport, cannot be queried");
}
```

Inject `FileImportService` and `SerialPortService` (or their DAOs).

### 6. New: Liquibase migration `src/main/resources/liquibase/3.4.x.x/028-normalize-protocol-version-enum.xml`

```xml
<!-- Normalize legacy string values to enum constant names -->
<update tableName="analyzer">
  <column name="protocol_version" value="ASTM_LIS2_A2"/>
  <where>protocol_version IN ('ASTM LIS2-A2','LIS2-A2')
         OR protocol_version IS NULL
         OR UPPER(protocol_version) LIKE '%FILE%'
         OR UPPER(protocol_version) LIKE '%RS232%'
         OR UPPER(protocol_version) LIKE '%RS-232%'
         OR UPPER(protocol_version) LIKE '%SERIAL%'</where>
</update>
<update tableName="analyzer">
  <column name="protocol_version" value="HL7_V2_3_1"/>
  <where>protocol_version LIKE 'HL7%2.3%'</where>
</update>
<update tableName="analyzer">
  <column name="protocol_version" value="HL7_V2_5"/>
  <where>protocol_version LIKE 'HL7%2.5%'</where>
</update>
<!-- Catch-all: default remaining unknowns to ASTM -->
<update tableName="analyzer">
  <column name="protocol_version" value="ASTM_LIS2_A2"/>
  <where>protocol_version NOT IN ('ASTM_LIS2_A2','HL7_V2_3_1','HL7_V2_5')</where>
</update>
<!-- Update column default -->
<addDefaultValue tableName="analyzer" columnName="protocol_version"
                 defaultValue="ASTM_LIS2_A2"/>
```

Note: FILE/RS232 analyzers get defaulted to ASTM_LIS2_A2 (their actual message
format). Transport info is already in their config entities.

Include in `liquibase-3.4.x.x-master.xml`.

### 7. New: `frontend/src/components/analyzers/constants.js`

```javascript
export const PROTOCOL_VERSIONS = [
  { value: "ASTM_LIS2_A2", label: "ASTM LIS2-A2" },
  { value: "HL7_V2_3_1", label: "HL7 v2.3.1" },
  { value: "HL7_V2_5", label: "HL7 v2.5" },
];

// Map plugin protocol family → default protocol version constant
export const PLUGIN_PROTOCOL_DEFAULTS = {
  ASTM: "ASTM_LIS2_A2",
  HL7: "HL7_V2_3_1",
  FILE: "ASTM_LIS2_A2", // FILE is transport, default message format is ASTM
};
```

### 8. Edit: `AnalyzerForm.jsx`

- Import `PROTOCOL_VERSIONS` and `PLUGIN_PROTOCOL_DEFAULTS` from `constants.js`
- Replace all hardcoded `"ASTM LIS2-A2"` defaults with `"ASTM_LIS2_A2"`
- Replace inline `protocolMap` (lines 451-455) with `PLUGIN_PROTOCOL_DEFAULTS`
- **Fix default config loading** (lines 250-253): normalize to enum constant
  using `PLUGIN_PROTOCOL_DEFAULTS[protocol.toUpperCase()]` instead of raw
  `configData.protocol_version`
- Replace `protocolVersion` TextInput with Carbon `Dropdown` using
  `PROTOCOL_VERSIONS` as items (matches status dropdown pattern)

### 9. Edit: Test files

- `AnalyzerForm.defaultConfigs.test.jsx` — update expected values to enum
  constant names
- `AnalyzerForm.test.jsx` — update any protocolVersion assertions

---

## Verification

1. **Backend build:** `mvn clean install -DskipTests -Dmaven.test.skip=true`
2. **Frontend lint/format:** `cd frontend && npm run format && cd ..`
3. **Backend formatting:** `mvn spotless:apply`
4. **Frontend tests:** `cd frontend && npx react-scripts test --watchAll=false`
5. **Verify migration:** Check that changeset XML is valid and included in
   master
6. **Manual smoke test:** Create/edit analyzer form — protocolVersion dropdown
   should show 3 options (ASTM/HL7 variants only), plugin selection should
   auto-set the default, default config template should set the correct value
