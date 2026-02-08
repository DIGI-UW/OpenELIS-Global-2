# Universal Analyzer Bridge Implementation Plan

**Goal**: Extend `tools/openelis-analyzer-bridge` to become a Universal Analyzer
Bridge that handles ALL analyzer communication, forwarding normalized messages
to OpenELIS via HTTP.

**Core Principle**: OpenELIS receives ONE standard HTTP format regardless of how
the analyzer sent data.

---

## Repository Workflow (CRITICAL - READ FIRST)

### Two Separate Repositories (Updated: Hybrid Approach)

| Repository                                                                                  | Purpose                            | Milestones                           |
| ------------------------------------------------------------------------------------------- | ---------------------------------- | ------------------------------------ |
| **[DIGI-UW/openelis-analyzer-bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge)** | Protocol bridge (Java Spring Boot) | **M1-M8** (listeners, routing)       |
| **[OpenELIS-Global-2](https://github.com/I-TECH-UW/OpenELIS-Global-2)**                     | Main OpenELIS application          | **M2, M4, M9** (new endpoints, docs) |

### Branch Naming Convention

| Milestone | Bridge Repo Branch                  | OpenELIS Repo Branch                     | Notes                                     |
| --------- | ----------------------------------- | ---------------------------------------- | ----------------------------------------- |
| M1        | `feat/universal-bridge-foundation`  | N/A                                      | Foundation types - ✅ **MERGED**          |
| M2        | `feat/universal-bridge-mllp`        | `feat/011-universal-bridge-hl7-endpoint` | MLLP listener + HL7 endpoint              |
| M3        | `feat/universal-bridge-serial`      | N/A                                      | Serial listener (uses ASTM/HL7 endpoints) |
| M4        | `feat/universal-bridge-file`        | `feat/011-universal-bridge-csv-endpoint` | File watcher + CSV endpoint               |
| M5        | `feat/universal-bridge-http-input`  | N/A                                      | HTTP input (routes to existing endpoints) |
| M6        | ✅ **MERGED** (PR #4)               | N/A                                      | ASTM source IP (done)                     |
| M7        | `feat/universal-bridge-normalizer`  | N/A                                      | Message normalizer/router                 |
| M8        | `feat/universal-bridge-integration` | N/A                                      | Testing, Docker, README                   |
| M9        | N/A                                 | `docs/universal-bridge-spec-alignment`   | Update spec docs                          |

### PR Dependency Chain

```
BRIDGE REPO (DIGI-UW/openelis-analyzer-bridge):
┌──────────────────────────────────────────────────────────────┐
│ PR #1: M1 → main                                             │
│   feat/universal-bridge-foundation → main                    │
│   [Foundation types, enums, DTOs]                            │
└────────────────────────┬─────────────────────────────────────┘
                         │ MERGE & TAG v2.0.0-foundation
                         ▼
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐           ┌──────────────────┐
│ PR #2: M2 → main │           │ PR #3: M3 → main │  (and M4, M5, M6 in parallel)
│   feat/..-mllp   │           │   feat/..-serial │
└────────┬─────────┘           └────────┬─────────┘
         │                               │
         └───────────────┬───────────────┘
                         │ ALL MERGE
                         ▼
         ┌───────────────────────────────┐
         │ PR #7: M7 → main              │
         │   feat/..-normalizer          │
         └────────────────┬──────────────┘
                          │
                          ▼
         ┌───────────────────────────────┐
         │ PR #8: M8 → main              │
         │   feat/..-integration         │
         │   [FINAL BRIDGE PR]           │
         └────────────────┬──────────────┘
                          │ MERGE & TAG v2.0.0
                          │ UPDATE SUBMODULE IN OPENELIS
                          ▼
OPENELIS REPO (I-TECH-UW/OpenELIS-Global-2):
┌──────────────────────────────────────────────────────────────┐
│ PR #9: M9 → develop                                          │
│   docs/universal-bridge-spec-alignment → develop             │
│   [Update specs 011 & 004 research.md to reference bridge]  │
└──────────────────────────────────────────────────────────────┘
```

### Critical Rules

1. **M1-M8 = Bridge Repo ONLY** - ALL Java code changes go to
   `DIGI-UW/openelis-analyzer-bridge`
2. **M9 = OpenELIS Repo ONLY** - Documentation updates to spec files
3. **No OpenELIS code changes** - Bridge handles ALL protocol/transport logic
4. **Submodule update** - After M8 merges, update
   `tools/openelis-analyzer-bridge` submodule in OpenELIS to v2.0.0

### Working with the Bridge Submodule

**You can work directly in the submodule!**
`OpenELIS-Global-2/tools/openelis-analyzer-bridge/` is a full git repository.

```bash
# For M1-M8 (Bridge work) - work directly in submodule:
cd OpenELIS-Global-2/tools/openelis-analyzer-bridge/
git remote -v  # Should show: https://github.com/DIGI-UW/openelis-analyzer-bridge.git

# For M9 (OpenELIS docs) - work in parent repo:
cd OpenELIS-Global-2/
git remote -v  # Should show: https://github.com/I-TECH-UW/OpenELIS-Global-2.git
```

**After merging bridge PRs**, update the parent repo's submodule reference:

```bash
# After PR #1 (M1) merges to bridge repo:
cd OpenELIS-Global-2/tools/openelis-analyzer-bridge/
git pull origin main  # Pull latest bridge changes

cd ../../  # Back to OpenELIS-Global-2/
git add tools/openelis-analyzer-bridge  # Update submodule reference
git commit -m "Update bridge submodule after M1 merge"
git push origin your-branch

# Repeat after each bridge PR merge (M2-M8)
```

---

## Architecture

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                UNIVERSAL ANALYZER BRIDGE (Single Java Process)                │
│                     tools/openelis-analyzer-bridge (extended)                         │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐      │
│  │ ASTM/TCP  │ │ HL7/MLLP  │ │  Serial   │ │   File    │ │   HTTP    │      │
│  │ Listener  │ │ Listener  │ │ Listener  │ │  Watcher  │ │  Input    │      │
│  │ :5001     │ │ :2575     │ │ jSerial   │ │ WatchSvc  │ │  :8080    │      │
│  │ [existing]│ │ [HAPI]    │ │ [NEW]     │ │ [NEW]     │ │  [NEW]    │      │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘      │
│        │             │             │             │             │             │
│        │ ASTM/TCP    │ HL7/MLLP    │ ASTM or HL7 │ ASTM/HL7/CSV│ Any/HTTP   │
│        │             │             │ over RS232  │ from files  │             │
│        └──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴─────┘       │
│               │             │             │             │                    │
│               ▼             ▼             ▼             ▼                    │
│       ┌──────────────────────────────────────────────────────────┐          │
│       │                  MESSAGE NORMALIZER                       │          │
│       │  • Detect protocol (ASTM vs HL7 vs CSV)                   │          │
│       │  • Identify analyzer (IP, serial port, header, file path) │          │
│       │  • Add metadata (source, timestamp, transport)            │          │
│       │  • Forward to OpenELIS via HTTP POST                      │          │
│       └─────────────────────────┬────────────────────────────────┘          │
│                                 │                                            │
└─────────────────────────────────┼────────────────────────────────────────────┘
                                  │ HTTP POST (JSON)
                                  ▼
                     ┌──────────────────────────┐
                     │        OPENELIS          │
                     │  POST /api/analyzer/     │
                     │        results           │
                     │  {                       │
                     │    "analyzerId": "...",  │
                     │    "protocol": "ASTM",   │
                     │    "transport": "TCP",   │
                     │    "message": "H|...",   │
                     │    "sourceId": "...",    │
                     │    "timestamp": "..."    │
                     │  }                       │
                     └──────────────────────────┘
```

---

## Hybrid Architecture (Bridge ↔ OpenELIS Integration)

### Message Flow Pattern

**Bridge Internal:** Uses `NormalizedMessage` DTO for routing **Bridge →
OpenELIS:** Sends raw message + headers (protocol-specific endpoints)

```
Analyzer → Bridge Listener → MessageEnvelope (internal)
                             ↓
                    Message Normalizer (M7)
                             ↓
                    Protocol Router (inspects envelope.protocol)
                             ↓
              ┌──────────────┼──────────────┐
              ↓              ↓              ↓
        ASTM Protocol   HL7 Protocol   CSV Protocol
              ↓              ↓              ↓
   POST /analyzer/astm  POST /analyzer/hl7  POST /analyzer/csv
   [raw ASTM + headers] [raw HL7 + headers] [CSV + headers]
              ↓              ↓              ↓
         OpenELIS        OpenELIS       OpenELIS
       ASTM Parser     HL7 Parser     CSV Parser
```

### HTTP Forward Format

**ASTM (existing - unchanged):**

```http
POST /api/OpenELIS-Global/analyzer/astm
X-Source-Analyzer-IP: 192.168.1.10
X-Message-Transport: TCP
Content-Type: text/plain

H|\^&|||MINDRAY|||...
```

**HL7 (M2 - new endpoint):**

```http
POST /api/OpenELIS-Global/analyzer/hl7
X-Source-Analyzer-IP: 192.168.1.11
X-Message-Transport: MLLP
Content-Type: text/plain

MSH|^~\&|SYSMEX|||...
```

**CSV (M4 - new endpoint):**

```http
POST /api/OpenELIS-Global/analyzer/csv
X-Source-Analyzer-IP: 192.168.1.12
X-Message-Transport: FILE
Content-Type: text/csv

SampleID,TestCode,Result,Units
12345,GLU,95,mg/dL
```

### Why This Approach?

✅ **Backward Compatible** - Existing ASTM workflow unchanged ✅ **Gradual
Migration** - Add protocols incrementally ✅ **Protocol-Specific Parsing** -
Each endpoint knows its parser ✅ **Independent Development** - Bridge and
OpenELIS PRs can be parallel ✅ **Future-Ready** - Can migrate to unified JSON
endpoint later

---

## Protocol vs Transport Matrix

| Transport                | Protocol     | Example Analyzers                |
| ------------------------ | ------------ | -------------------------------- |
| **TCP (raw)**            | ASTM LIS2-A2 | Mindray BC-5380, GeneXpert       |
| **MLLP (TCP + framing)** | HL7 v2.x     | Sysmex XN, Abbott Architect      |
| **Serial (RS232)**       | ASTM         | Horiba Pentra 60, Mindray BA-88A |
| **Serial (RS232)**       | HL7          | Some legacy Siemens analyzers    |
| **File**                 | ASTM/HL7/CSV | QuantStudio, Hain FluoroCycler   |
| **HTTP POST**            | ASTM or HL7  | Modern analyzers with REST API   |

**Key Insight**:

- **Protocol** = message format (ASTM, HL7, CSV)
- **Transport** = delivery method (TCP, MLLP, Serial, File, HTTP)
- Serial & File listeners auto-detect protocol from content

---

## Milestone Dependency Graph (Parallelizable Work)

```
                    ┌─────────────────────────────────────────────────┐
                    │  M1: Foundation (MUST be first - shared types)  │
                    └────────────────────────┬────────────────────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
              ▼                              ▼                              ▼
    ┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
    │   M2: MLLP      │           │   M3: Serial    │           │   M4: File      │
    │   (HL7/TCP)     │           │   (RS232)       │           │   Watcher       │
    │   [WORKTREE A]  │           │   [WORKTREE B]  │           │   [WORKTREE C]  │
    └────────┬────────┘           └────────┬────────┘           └────────┬────────┘
             │                              │                              │
             │ ┌────────────────────────────┼──────────────────────────────┘
             │ │                            │
             ▼ ▼                            ▼
    ┌─────────────────┐           ┌─────────────────┐
    │   M5: HTTP      │           │   M6: ASTM      │
    │   Input         │           │   Enhancements  │
    │   [WORKTREE D]  │           │   [WORKTREE E]  │
    └────────┬────────┘           └────────┬────────┘
             │                              │
             └──────────────┬───────────────┘
                            │
                            ▼
              ┌─────────────────────────────┐
              │   M7: Message Normalizer    │
              │   (integrates all listeners)│
              └────────────────┬────────────┘
                               │
                               ▼
              ┌─────────────────────────────┐
              │   M8: Integration & Deploy  │
              │   (tests, docker, docs)     │
              └────────────────┬────────────┘
                               │
                               ▼
              ┌─────────────────────────────┐
              │   M9: Spec Documentation    │
              │   (update research.md, etc) │
              └─────────────────────────────┘
```

### Parallelization Strategy

| Milestone | Repository      | Branch                                 | Worktree   | Depends On | Days |
| --------- | --------------- | -------------------------------------- | ---------- | ---------- | ---- |
| M1        | **Bridge**      | `feat/universal-bridge-foundation`     | main       | -          | 2    |
| M2        | **Bridge**      | `feat/universal-bridge-mllp`           | worktree-a | M1         | 3    |
| M3        | **Bridge**      | `feat/universal-bridge-serial`         | worktree-b | M1         | 4    |
| M4        | **Bridge**      | `feat/universal-bridge-file`           | worktree-c | M1         | 3    |
| M5        | **Bridge**      | `feat/universal-bridge-http-input`     | worktree-d | M2 or M3   | 2    |
| M6        | **Bridge**      | `feat/universal-bridge-astm-enhance`   | worktree-e | M1         | 2    |
| M7        | **Bridge**      | `feat/universal-bridge-normalizer`     | main       | M2-M6      | 3    |
| M8        | **Bridge**      | `feat/universal-bridge-integration`    | main       | M7         | 4    |
| M9        | **OpenELIS** ⚠️ | `docs/universal-bridge-spec-alignment` | N/A        | M8         | 1    |

**Total**: ~24 days sequential, **~14 days with parallelization**

**Repository Summary**:

- **M1-M8**: `DIGI-UW/openelis-analyzer-bridge` (8 milestones, ALL code)
- **M9**: `I-TECH-UW/OpenELIS-Global-2` (1 milestone, docs only)

---

## Milestones

### M1: Foundation (Days 1-2) — BLOCKING

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-foundation` **Worktree**: main (blocking - must complete
first)

| Task | Description                                              | File                                |
| ---- | -------------------------------------------------------- | ----------------------------------- |
| T01  | Add HAPI HL7 v2 + jSerialComm + Commons CSV dependencies | `pom.xml`                           |
| T02  | Create `Protocol` enum (ASTM, HL7, CSV, UNKNOWN)         | `model/Protocol.java`               |
| T03  | Create `Transport` enum (TCP, MLLP, SERIAL, FILE, HTTP)  | `model/Transport.java`              |
| T04  | Create `MessageEnvelope` DTO (internal routing)          | `normalizer/MessageEnvelope.java`   |
| T05  | Create `NormalizedMessage` DTO (output to OpenELIS)      | `normalizer/NormalizedMessage.java` |
| T06  | Create `ProtocolDetector` utility                        | `util/ProtocolDetector.java`        |
| T07  | Unit tests for foundation classes                        | `test/FoundationTest.java`          |

**Protocol Detection**:

```java
public static Protocol detect(String message) {
    if (message.charAt(0) == 0x02 || message.startsWith("H|\\^&"))
        return Protocol.ASTM;
    if (message.startsWith("MSH|"))
        return Protocol.HL7;
    if (message.contains(",") && message.split("\n")[0].split(",").length > 3)
        return Protocol.CSV;
    return Protocol.UNKNOWN;
}
```

---

### M2: MLLP Listener + HL7 Endpoint (Days 3-5) — PARALLEL REPOS

#### M2a: Bridge - MLLP Listener

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-mllp` **Worktree**: bridge-worktree-a **Depends on**: M1

| Task | Description                                     | File                         |
| ---- | ----------------------------------------------- | ---------------------------- |
| T08  | Create `MLLPListener` using HAPI `SimpleServer` | `listener/MLLPListener.java` |
| T09  | Implement `ReceivingApplication` callback       | `listener/MLLPListener.java` |
| T10  | Extract source IP from MLLP connection          | `listener/MLLPListener.java` |
| T11  | Add ACK/NAK response generation (HAPI built-in) | `listener/MLLPListener.java` |
| T12  | Configure MLLP port in `application.yml`        | `application.yml`            |
| T13  | Unit tests with mock HL7 messages               | `test/MLLPListenerTest.java` |

**HAPI MLLP Server** (handles VT/FS/CR framing automatically):

```java
@Component
public class MLLPListener {
    @PostConstruct
    public void start() {
        server = new SimpleServer(2575, false);
        server.registerApplication("*", "*", (message, metadata) -> {
            normalizer.process(MessageEnvelope.builder()
                .protocol(Protocol.HL7)
                .transport(Transport.MLLP)
                .sourceId((String) metadata.get("SENDING_IP"))
                .rawMessage(message.encode())
                .build());
            return message.generateACK();
        });
        server.start();
    }
}
```

#### M2b: OpenELIS - HL7 Endpoint

**Repository**: `I-TECH-UW/OpenELIS-Global-2` **Branch**:
`feat/011-universal-bridge-hl7-endpoint` (off `demo/madagascar`) **Worktree**:
openelis-worktree-hl7 **Depends on**: M2a (can develop in parallel)

| Task | Description                                    | File                             |
| ---- | ---------------------------------------------- | -------------------------------- |
| T14  | Create HL7 controller endpoint `/analyzer/hl7` | `AnalyzerHL7Controller.java`     |
| T15  | Create HL7 parser service (using HAPI)         | `HL7ParserService.java`          |
| T16  | Extract OBX segments → TestResult entities     | `HL7ResultExtractor.java`        |
| T17  | Handle X-Source-Analyzer-IP header             | `AnalyzerHL7Controller.java`     |
| T18  | Integration tests with mock HL7 messages       | `AnalyzerHL7ControllerTest.java` |

**Endpoint Implementation**:

```java
@RestController
@RequestMapping("/api/OpenELIS-Global/analyzer")
public class AnalyzerHL7Controller {
    @PostMapping("/hl7")
    public ResponseEntity<String> receiveHL7(
        @RequestBody String hl7Message,
        @RequestHeader("X-Source-Analyzer-IP") String sourceIp,
        @RequestHeader(value = "X-Message-Transport", required = false) String transport
    ) {
        // Parse HL7, extract results, create TestResult entities
        hl7ParserService.parseAndSaveResults(hl7Message, sourceIp);
        return ResponseEntity.ok("ACK");
    }
}
```

---

### M3: Serial Listener (Days 3-6) — BRIDGE ONLY

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-serial` **Worktree**: bridge-worktree-b **Depends on**:
M1

| Task | Description                                            | File                                |
| ---- | ------------------------------------------------------ | ----------------------------------- |
| T14  | Create `SerialListener` with jSerialComm               | `listener/SerialListener.java`      |
| T15  | Implement serial port config (baud, parity, stop bits) | `listener/SerialListener.java`      |
| T16  | Create `SerialFrameDetector` (ASTM/HL7 framing)        | `listener/SerialFrameDetector.java` |
| T17  | Integrate `ProtocolDetector` for auto-detection        | `listener/SerialListener.java`      |
| T18  | Configure serial ports in `application.yml`            | `application.yml`                   |
| T19  | Add connection status monitoring                       | `listener/SerialListener.java`      |
| T20  | Handle disconnection/reconnection                      | `listener/SerialListener.java`      |
| T21  | Unit tests with virtual serial ports (socat)           | `test/SerialListenerTest.java`      |

**Message Framing**:

- ASTM: `<STX>frame<ETX>checksum<CR><LF>` or `<ENQ>`/`<EOT>`
- HL7: `<VT>message<FS><CR>` (same as MLLP)

---

### M4: File Watcher + CSV Endpoint (Days 3-5) — PARALLEL REPOS

#### M4a: Bridge - File Watcher

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-file` **Worktree**: bridge-worktree-c **Depends on**: M1

| Task | Description                                       | File                                |
| ---- | ------------------------------------------------- | ----------------------------------- |
| T22  | Create `FileWatcherListener` using `WatchService` | `listener/FileWatcherListener.java` |
| T23  | Configure watch directories in `application.yml`  | `application.yml`                   |
| T24  | Implement file type detection (HL7, ASTM, CSV)    | `listener/FileWatcherListener.java` |
| T25  | Add CSV parsing with Apache Commons CSV           | `listener/FileWatcherListener.java` |
| T26  | Implement file archival after processing          | `listener/FileWatcherListener.java` |
| T27  | Implement error directory for failed files        | `listener/FileWatcherListener.java` |
| T28  | Add duplicate detection (hash-based)              | `listener/FileWatcherListener.java` |
| T29  | Unit tests with temp directories                  | `test/FileWatcherListenerTest.java` |

#### M4b: OpenELIS - CSV Endpoint

**Repository**: `I-TECH-UW/OpenELIS-Global-2` **Branch**:
`feat/011-universal-bridge-csv-endpoint` (off `demo/madagascar`) **Worktree**:
openelis-worktree-csv **Depends on**: M4a (can develop in parallel)

| Task | Description                                    | File                             |
| ---- | ---------------------------------------------- | -------------------------------- |
| T30  | Create CSV controller endpoint `/analyzer/csv` | `AnalyzerCSVController.java`     |
| T31  | Create CSV parser service (using Commons CSV)  | `CSVParserService.java`          |
| T32  | Map CSV columns → TestResult entities          | `CSVResultExtractor.java`        |
| T33  | Handle X-Source-Analyzer-IP header             | `AnalyzerCSVController.java`     |
| T34  | Integration tests with mock CSV data           | `AnalyzerCSVControllerTest.java` |

**Endpoint Implementation**:

```java
@RestController
@RequestMapping("/api/OpenELIS-Global/analyzer")
public class AnalyzerCSVController {
    @PostMapping("/csv")
    public ResponseEntity<String> receiveCSV(
        @RequestBody String csvData,
        @RequestHeader("X-Source-Analyzer-IP") String sourceIp,
        @RequestHeader(value = "X-Message-Transport", required = false) String transport
    ) {
        // Parse CSV, extract results, create TestResult entities
        csvParserService.parseAndSaveResults(csvData, sourceIp);
        return ResponseEntity.ok("OK");
    }
}
```

---

### M5: HTTP Input Listener (Days 7-8) — BRIDGE ONLY

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-http-input` **Worktree**: bridge-worktree-d **Depends
on**: M2 OR M3 (needs normalizer interface pattern)

| Task | Description                            | File                                      |
| ---- | -------------------------------------- | ----------------------------------------- |
| T30  | Create HTTP input endpoint `/input`    | `controller/AnalyzerInputController.java` |
| T31  | Accept raw message body (ASTM/HL7/CSV) | `controller/AnalyzerInputController.java` |
| T32  | Auto-detect protocol from content      | `controller/AnalyzerInputController.java` |
| T33  | Extract source IP from HTTP request    | `controller/AnalyzerInputController.java` |
| T34  | Unit tests for HTTP input              | `test/AnalyzerInputControllerTest.java`   |

---

### M6: ASTM Enhancements (Days 3-4) — WORKTREE E

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-astm-enhance` **Worktree**: worktree-e (parallel with M2,
M3, M4) **Depends on**: M1

| Task | Description                                       | File                                              |
| ---- | ------------------------------------------------- | ------------------------------------------------- |
| T35  | Add `X-Source-Analyzer-IP` header to HTTP forward | `handler/DefaultForwardingASTMToHTTPHandler.java` |
| T36  | Extract source IP from socket in receive thread   | `astm/handling/ASTMReceiveThread.java`            |
| T37  | Pass source IP through handler chain              | `service/ASTMHandlerService.java`                 |
| T38  | Unit tests for source IP extraction               | `test/ASTMReceiveThreadTest.java`                 |

---

### M7: Message Normalizer (Days 9-11) — INTEGRATION

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-normalizer` **Worktree**: main (integration work after
all listeners complete) **Depends on**: M2, M3, M4, M5, M6

| Task | Description                                         | File                                 |
| ---- | --------------------------------------------------- | ------------------------------------ |
| T39  | Create `MessageNormalizer` service (central router) | `normalizer/MessageNormalizer.java`  |
| T40  | Create `AnalyzerIdentifier` (multi-strategy)        | `normalizer/AnalyzerIdentifier.java` |
| T41  | Implement IP-based identification                   | `normalizer/AnalyzerIdentifier.java` |
| T42  | Implement header-based ID (ASTM H-segment, HL7 MSH) | `normalizer/AnalyzerIdentifier.java` |
| T43  | Implement serial-port-based identification          | `normalizer/AnalyzerIdentifier.java` |
| T44  | Implement file-path-pattern identification          | `normalizer/AnalyzerIdentifier.java` |
| T45  | Create `OpenELISForwarder` HTTP client              | `normalizer/OpenELISForwarder.java`  |
| T46  | Add retry logic with exponential backoff            | `normalizer/OpenELISForwarder.java`  |
| T47  | Add audit logging                                   | `normalizer/MessageNormalizer.java`  |
| T48  | Unit tests for normalizer                           | `test/MessageNormalizerTest.java`    |

---

### M8: Integration & Deployment (Days 12-15)

**Repository**: `DIGI-UW/openelis-analyzer-bridge` **Branch**:
`feat/universal-bridge-integration` **Worktree**: main (final bridge repo work)
**Depends on**: M7

| Task | Description                                   | File                               |
| ---- | --------------------------------------------- | ---------------------------------- |
| T49  | Create unified configuration schema           | `application.yml`                  |
| T50  | Add health check endpoint (all listeners)     | `controller/HealthController.java` |
| T51  | Add Prometheus metrics                        | `config/MetricsConfiguration.java` |
| T52  | Update Dockerfile (expose all ports)          | `Dockerfile`                       |
| T53  | Create Docker Compose for testing             | `docker-compose-test.yml`          |
| T54  | Integration tests (all transports → OpenELIS) | `test/integration/`                |
| T55  | Update bridge README with all transports      | `README.md`                        |

---

### M9: Spec Documentation Updates (Day 16)

**Repository**: `I-TECH-UW/OpenELIS-Global-2` ⚠️ **DIFFERENT REPO** **Branch**:
`docs/universal-bridge-spec-alignment` **Worktree**: N/A (OpenELIS main repo)
**Depends on**: M8 (bridge v2.0.0 tagged and submodule updated)

| Task | Description                                                        | Target File                                             |
| ---- | ------------------------------------------------------------------ | ------------------------------------------------------- |
| T56  | Update spec 011 research.md with protocol/transport clarifications | `specs/011-madagascar-analyzer-integration/research.md` |
| T57  | Add "Protocol vs Transport" section to 011 research.md             | `specs/011-madagascar-analyzer-integration/research.md` |
| T58  | Document MLLP (HL7 transport) architecture decision                | `specs/011-madagascar-analyzer-integration/research.md` |
| T59  | Update spec 004 research.md to align with Universal Bridge         | `specs/004-astm-analyzer-mapping/research.md`           |
| T60  | Cross-reference Universal Bridge from spec 004 plan.md             | `specs/004-astm-analyzer-mapping/plan.md`               |
| T61  | Update tool architecture clarification (bridge ≠ mock server)      | `specs/011-madagascar-analyzer-integration/research.md` |

**Documentation Updates Content**:

For **spec 011 research.md**, add section:

```markdown
## Protocol vs Transport Architecture (Universal Bridge)

### Key Clarification (Feb 2026)

The original spec incorrectly implied HL7 analyzers send directly via HTTP. This
was WRONG.

**Correct Understanding**:

- **Protocol** = message format (ASTM LIS2-A2, HL7 v2.x, CSV)
- **Transport** = delivery method (TCP, MLLP, Serial, File, HTTP)

**HL7 Requires Bridge**: Most HL7 analyzers use MLLP (TCP with VT/FS/CR framing)
or serial connections, NOT direct HTTP. The bridge translates these transports
to HTTP for OpenELIS.

### Transport Matrix

| Transport          | Protocol     | Handled By                         |
| ------------------ | ------------ | ---------------------------------- |
| TCP (raw)          | ASTM         | ASTMListener (existing)            |
| MLLP (TCP+framing) | HL7          | MLLPListener (HAPI)                |
| Serial (RS232)     | ASTM or HL7  | SerialListener (jSerialComm)       |
| File               | ASTM/HL7/CSV | FileWatcherListener (WatchService) |
| HTTP POST          | Any          | AnalyzerInputController            |

See: `/home/ubuntu/.claude/plans/universal-analyzer-bridge.md`
```

For **spec 004 research.md**, add section:

```markdown
## Universal Analyzer Bridge Integration

### Architecture Change (Feb 2026)

The ASTM-HTTP bridge has been extended to become a **Universal Analyzer Bridge**
that handles ALL analyzer transports, not just ASTM/TCP.

**Impact on Feature 004**:

- The `X-Source-Analyzer-IP` header implementation (Section 13) is now part of
  M6: ASTM Enhancements in the Universal Bridge plan
- Analyzer identification strategies (Section 13) apply to all transports, not
  just ASTM
- Message processing workflow (Section 15) remains unchanged for ASTM; new
  transports follow same pattern

**Cross-Reference**: See Universal Bridge plan at
`/home/ubuntu/.claude/plans/universal-analyzer-bridge.md`
```

---

## Configuration Schema

```yaml
# tools/openelis-analyzer-bridge/application.yml
bridge:
  # OpenELIS connection (output)
  openelis:
    url: https://openelis:8443/api/analyzer/results
    username: bridge
    password: ${BRIDGE_PASSWORD}
    timeout: 30000
    retry:
      maxAttempts: 3
      backoffMs: 1000

  # ASTM TCP listener (existing, enhanced)
  astm:
    enabled: true
    port: 5001

  # HL7 MLLP listener (new)
  mllp:
    enabled: true
    port: 2575

  # Serial listeners (new)
  serial:
    enabled: true
    ports:
      - name: /dev/ttyUSB0
        baudRate: 9600
        dataBits: 8
        stopBits: 1
        parity: NONE
        protocol: AUTO # AUTO, ASTM, or HL7
      - name: /dev/ttyUSB1
        baudRate: 19200
        protocol: ASTM

  # File watcher (new)
  file:
    enabled: true
    watchDirectories:
      - /mnt/analyzer-import/quantstudio
      - /mnt/analyzer-import/hain
    archiveDirectory: /mnt/analyzer-archive
    errorDirectory: /mnt/analyzer-error
    pollIntervalMs: 5000

  # HTTP input listener (new)
  http:
    enabled: true
    port: 8080

  # Analyzer identification
  analyzers:
    "192.168.1.10":
      id: MINDRAY-BC5380-001
      name: "Mindray BC-5380"
      expectedProtocol: ASTM

    "192.168.1.11":
      id: SYSMEX-XN-001
      name: "Sysmex XN-1000"
      expectedProtocol: HL7

    "/dev/ttyUSB0":
      id: HORIBA-PENTRA60-001
      name: "Horiba Pentra 60"
      expectedProtocol: ASTM

    "quantstudio-*":
      id: QUANTSTUDIO-001
      name: "QuantStudio 7 Flex"
      expectedProtocol: CSV
      filePattern: ".*/quantstudio-.*\\.csv"
```

---

## Dependencies (pom.xml additions)

```xml
<!-- HL7 v2.x + MLLP -->
<dependency>
    <groupId>ca.uhn.hapi</groupId>
    <artifactId>hapi-base</artifactId>
    <version>2.5.1</version>
</dependency>
<dependency>
    <groupId>ca.uhn.hapi</groupId>
    <artifactId>hapi-structures-v251</artifactId>
    <version>2.5.1</version>
</dependency>

<!-- Serial communication -->
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.11.0</version>
</dependency>

<!-- CSV parsing -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>
```

---

## Git Workflow (Worktrees)

### Branch Structure

```
main (or develop)
├── feat/universal-bridge-foundation     # M1
├── feat/universal-bridge-mllp           # M2 (worktree-a)
├── feat/universal-bridge-serial         # M3 (worktree-b)
├── feat/universal-bridge-file           # M4 (worktree-c)
├── feat/universal-bridge-http-input     # M5 (worktree-d)
├── feat/universal-bridge-astm-enhance   # M6 (worktree-e)
├── feat/universal-bridge-normalizer     # M7
├── feat/universal-bridge-integration    # M8
└── feat/universal-bridge-docs           # M9
```

### Worktree Setup Commands

```bash
# Work directly in the submodule
cd OpenELIS-Global-2/tools/openelis-analyzer-bridge/

# Create M1 branch and complete foundation work
git checkout -b feat/universal-bridge-foundation
# [complete M1 tasks, create PR to bridge repo, merge to main]

# After M1 merges to bridge main, pull changes and create worktrees:
git checkout main
git pull origin main

# Create worktrees for parallel work (relative to submodule directory):
git worktree add ../../bridge-worktree-a feat/universal-bridge-mllp          # M2
git worktree add ../../bridge-worktree-b feat/universal-bridge-serial        # M3
git worktree add ../../bridge-worktree-c feat/universal-bridge-file          # M4
git worktree add ../../bridge-worktree-e feat/universal-bridge-astm-enhance  # M6

# Later (after M2 or M3 establishes normalizer interface):
git worktree add ../../bridge-worktree-d feat/universal-bridge-http-input    # M5

# Each worktree is independent and can have different branches checked out
cd ../../bridge-worktree-a  # Work on M2 (MLLP)
cd ../bridge-worktree-b     # Work on M3 (Serial)
# etc.
```

### PR Strategy (Repository-Specific)

**Bridge Repo PRs** (`DIGI-UW/openelis-analyzer-bridge`):

```
PR #1: feat/universal-bridge-foundation → main (M1)
  Foundation types, enums, DTOs

PR #2: feat/universal-bridge-mllp → main (M2)
  MLLP listener using HAPI

PR #3: feat/universal-bridge-serial → main (M3)
  Serial listener with jSerialComm

PR #4: feat/universal-bridge-file → main (M4)
  File watcher with WatchService

PR #5: feat/universal-bridge-http-input → main (M5)
  HTTP input endpoint

PR #6: feat/universal-bridge-astm-enhance → main (M6)
  X-Source-Analyzer-IP header

PR #7: feat/universal-bridge-normalizer → main (M7)
  Message normalizer integration

PR #8: feat/universal-bridge-integration → main (M8)
  Tests, Docker, README, Prometheus metrics
  Tag: v2.0.0
```

**OpenELIS Repo PRs** (`I-TECH-UW/OpenELIS-Global-2`):

```
PR #9: Update tools/openelis-analyzer-bridge submodule to v2.0.0
  (after bridge PR #8 merges and is tagged)

PR #10: docs/universal-bridge-spec-alignment → develop (M9)
  Update specs 011 & 004 research.md
  References bridge v2.0.0 architecture
```

### Submodule Update Workflow

**After each bridge PR merges**, update the OpenELIS parent repo to reference
the new commit:

```bash
# 1. Pull latest bridge changes in the submodule
cd OpenELIS-Global-2/tools/openelis-analyzer-bridge/
git checkout main
git pull origin main

# 2. Go to parent repo and commit the submodule reference update
cd ../../  # Back to OpenELIS-Global-2/
git status  # Will show: modified: tools/openelis-analyzer-bridge (new commits)

git add tools/openelis-analyzer-bridge
git commit -m "Update bridge submodule: <describe what merged>"
# Example: "Update bridge submodule: Add MLLP listener (M2)"

# 3. Push to your OpenELIS branch (or create tracking branch)
git push origin your-branch
```

**Why this matters**: The parent repo (`OpenELIS-Global-2`) stores a specific
commit SHA of the submodule. When you update the submodule reference, you're
telling OpenELIS "use this newer version of the bridge." This ensures:

- Other developers get the latest bridge when they clone OpenELIS
- CI/CD uses the correct bridge version
- Deployment environments stay synchronized

**Timing**: You can update the submodule reference after each PR merge, or batch
multiple updates. The final update (after M8/v2.0.0) is mandatory before
starting M9.

---

## Task Summary by Milestone

| Milestone             | Tasks   | Days | Parallel?        |
| --------------------- | ------- | ---- | ---------------- |
| M1. Foundation        | T01-T07 | 2    | No (blocking)    |
| M2. MLLP Listener     | T08-T13 | 3    | Yes (worktree A) |
| M3. Serial Listener   | T14-T21 | 4    | Yes (worktree B) |
| M4. File Watcher      | T22-T29 | 3    | Yes (worktree C) |
| M5. HTTP Input        | T30-T34 | 2    | Yes (worktree D) |
| M6. ASTM Enhancements | T35-T38 | 2    | Yes (worktree E) |
| M7. Normalizer        | T39-T48 | 3    | No (integration) |
| M8. Integration       | T49-T55 | 4    | No (testing)     |
| M9. Documentation     | T56-T61 | 1    | No (final)       |

**Total: 61 tasks, ~24 days sequential, ~14 days with parallelization**

---

## Success Criteria

1. Single bridge handles: ASTM/TCP, HL7/MLLP, Serial (both protocols), File,
   HTTP
2. OpenELIS receives identical JSON regardless of transport
3. All Madagascar analyzers work through bridge
4. <100ms p95 latency for message forwarding
5. Auto-reconnection on serial disconnect
6. Prometheus metrics for all listeners
7. Zero protocol code in OpenELIS (HTTP-only)
8. Spec documentation updated with protocol/transport clarifications

---

## Quick Start

```bash
# Build
cd tools/openelis-analyzer-bridge && mvn clean install -DskipTests

# Run
java -jar target/astm-http-app.jar

# Test MLLP (HL7)
echo -e "\x0bMSH|^~\\&|TEST|||...\x1c\r" | nc localhost 2575

# Test ASTM
echo "H|\\^&|||TEST|||..." | nc localhost 5001

# Test HTTP input
curl -X POST http://localhost:8080/input -d "MSH|^~\\&|TEST|||..."
```
