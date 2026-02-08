# Feature Specification: Madagascar Analyzer Integration

**Feature Branch**: `spec/011-madagascar-analyzer-integration` **Created**:
2026-01-22 **Status**: Draft **Contract Deadline**: 2026-02-28 **Scope**: 12
minimum analyzers with bidirectional communication (results + orders)
**Extends**: Feature 004-astm-analyzer-mapping

## Executive Summary

This feature extends the existing 004-astm-analyzer-mapping infrastructure to
support the Madagascar contract requirements: integrating 12+ laboratory
analyzers with bidirectional communication (receiving results AND sending test
orders). The implementation leverages 19+ existing analyzer plugins from the
openelisglobal-plugins repository to reduce development effort by approximately
50%.

### Contract Requirements Overview

| Requirement                        | Deadline      | Status      |
| ---------------------------------- | ------------- | ----------- |
| 12 minimum analyzers bidirectional | 2026-02-28    | Critical    |
| HL7 protocol support               | 2026-02-28    | Required    |
| RS232 serial support               | 2026-02-28    | Required    |
| File-based import support          | 2026-02-28    | Required    |
| Instrument metadata form           | 2026-02-28    | Required    |
| Test order export                  | 2026-02-28    | Required    |
| Maintenance tracking               | Post-deadline | Contractual |
| GeneXpert module management        | Post-deadline | Contractual |
| POCT1A protocol                    | Post-deadline | Contractual |

---

## Clarifications

### Session 2026-01-22

- Q: Order export trigger model - how should orders be sent to analyzers? → A:
  **Manual export only** (for deadline). Lab staff trigger export via UI;
  host-query mode (analyzer polling) deferred to post-deadline.
- Q: Should instrument location hierarchy reuse existing OpenELIS entities or
  create new ones? → A: **Reuse existing Organization/Location entities**. Link
  instruments to existing facility hierarchy; add room-level detail as extension
  if needed.
- Q: Should the spec include testing infrastructure requirements? → A: **Yes,
  expand ASTM mock server to multi-protocol analyzer simulator**. Added User
  Story 9, FR-025 through FR-030, SC-011 for simulator supporting ASTM, HL7
  v2.x, RS232, and file-based simulation for all 12 contract analyzers.

### Session 2026-01-27

- Q: How should the spec reconcile conflicting priorities between contract
  requirements and Romain's internal list? → A: **Use Romain's priority for
  implementation order**. Analyzers marked "NOT ON ROMAIN'S LIST" (BC2000,
  Sysmex XN Series) deprioritized to P2 since they may not be deployed in
  Madagascar labs near-term. Contract compliance still required but
  implementation order follows real-world deployment priorities.
- Q: Should the spec include additional high-priority analyzers from Romain's
  list beyond the 12 contract-minimum? → A: **Add as P3 stretch goals**. Attempt
  if time permits after P1/P2 complete. Includes Mindray variants (BC5300,
  BS-120/200/230), Bio-Rad CFX96, BioMérieux VIDAS, and other high-priority PCR
  thermocyclers from Romain's list.
- Q: How should implementation handle protocol uncertainties for analyzers with
  unclear connectivity? → A: **Build flexible adapters supporting multiple
  protocols per analyzer**. Use the adapter pattern from Feature 004
  architecture. Analyzers like Abbott Architect, Hain FluoroCycler XT, and
  others with uncertain protocols get multi-protocol adapter support (e.g.,
  HL7 + RS232, or File + Network) to handle discovery during deployment.
- Q: What validation level is required before contract go-live? → A: **Simulator
  validation sufficient for go-live**. All 12 analyzers validated via
  multi-protocol simulator; physical testing happens post-deployment in
  Madagascar labs. This accepts higher risk of in-field issues but meets the
  tight deadline. Simulator must provide comprehensive coverage including edge
  cases and error conditions.
- Q: What RS232 deployment architecture should the spec require? → A: **Extend
  ASTM-HTTP Bridge to handle RS232 locally**. The bridge runs on a lab PC with
  USB-to-serial adapter, converts RS232→TCP, and forwards to OpenELIS server.
  This keeps architecture consistent with existing ASTM bridge pattern and
  avoids additional hardware costs beyond USB adapters.

---

## User Scenarios & Testing _(mandatory)_

### User Story 1 - HL7 Analyzer Results Import (Priority: P1)

As a laboratory technician, I need to receive test results from HL7-compatible
analyzers (Mindray BC-5380, BS-360E, BC2000, Sysmex XN Series) automatically in
OpenELIS so that I can validate and finalize results without manual data entry.

**Why this priority**: HL7 analyzers represent 5 of the 12 contract-required
instruments. Without HL7 support, nearly half the analyzers cannot communicate
with OpenELIS, making contract compliance impossible.

**Independent Test**: Can be fully tested by configuring a Mindray BC-5380
analyzer, sending HL7 ORU^R01 messages, and verifying results appear in the
analyzer results import page.

**Acceptance Scenarios**:

1. **Given** a Mindray BC-5380 analyzer is configured with IP address and port
   in OpenELIS, **When** the analyzer sends an HL7 ORU^R01 message containing
   CBC test results, **Then** the results appear in the analyzer results import
   queue within 30 seconds with correct patient identification and test values.

2. **Given** HL7 field mappings are configured for the analyzer, **When**
   results arrive with analyzer-specific test codes (e.g., "WBC", "RBC"),
   **Then** the system automatically maps these to OpenELIS test codes using the
   configured mappings.

3. **Given** an HL7 message is received with an unmapped test code, **When** the
   system processes the message, **Then** an error record is created in the
   error dashboard with the raw message for reprocessing after mapping creation.

4. **Given** multiple HL7 analyzers are configured, **When** messages arrive
   from different analyzers simultaneously, **Then** each message is correctly
   identified and routed based on sender information or client IP address.

---

### User Story 2 - Test Order Export to Analyzers (Priority: P1)

As a laboratory supervisor, I need to send test orders from OpenELIS to
connected analyzers so that analysts can process samples without manually
entering order information on each instrument.

**Why this priority**: The Madagascar contract explicitly requires bidirectional
communication for all 12 analyzers. Results-only integration does not meet
contract requirements.

**Independent Test**: Can be fully tested by creating a test order in OpenELIS,
exporting it to a configured analyzer, and verifying the order appears on the
analyzer's pending work list.

**Acceptance Scenarios**:

1. **Given** a sample with pending tests exists in OpenELIS and an analyzer is
   configured for the test type, **When** the laboratory technician selects
   "Export Orders" for that analyzer, **Then** the system generates and sends
   ASTM O-segment or HL7 ORM^O01 messages to the analyzer.

2. **Given** an order is exported to an analyzer, **When** the analyzer sends
   results for that order, **Then** the system matches the results to the
   original order using sample/patient identifiers.

3. **Given** an analyzer supports bidirectional communication, **When** viewing
   the analyzer configuration page, **Then** the system displays order export
   status (pending, sent, acknowledged, results received).

4. **Given** an order export fails due to connection timeout, **When** the
   system retries, **Then** a maximum of 3 retry attempts are made with
   exponential backoff, and failures are logged in the error dashboard.

---

### User Story 3 - RS232 Serial Analyzer Connection (Priority: P1)

As a system administrator, I need to connect analyzers that use RS232 serial
communication (Horiba Pentra 60, Micros 60, Mindray BA-88A, Stago STart 4) so
that these instruments can participate in the integrated laboratory workflow.

**Why this priority**: 7+ contract-required analyzers use RS232 as their primary
or only communication method. Without RS232 support, these analyzers cannot be
integrated.

**Independent Test**: Can be fully tested by connecting a USB-to-serial adapter
to a Horiba Pentra 60, configuring the serial port parameters in OpenELIS, and
verifying message reception.

**Acceptance Scenarios**:

1. **Given** an RS232 analyzer is physically connected via USB-to-serial
   adapter, **When** the administrator configures the serial port parameters
   (baud rate, parity, stop bits, flow control) in OpenELIS, **Then** the system
   successfully establishes a serial connection.

2. **Given** a serial connection is established with a Pentra 60 analyzer,
   **When** the analyzer sends ASTM messages over RS232, **Then** the system
   receives and processes the messages using the existing ASTM mapping
   infrastructure.

3. **Given** serial port configuration is incorrect (baud rate mismatch),
   **When** the system attempts to communicate, **Then** a clear error message
   indicates "Serial configuration mismatch" with troubleshooting guidance.

4. **Given** the serial connection is interrupted (cable disconnected), **When**
   the system detects the disconnection, **Then** the analyzer status changes to
   "Offline" and an alert is generated.

---

### User Story 4 - File-Based PCR Thermocycler Integration (Priority: P2)

As a laboratory technician, I need to import results from PCR thermocyclers
(QuantStudio 7 Flex, Hain FluoroCycler XT) that export results as CSV/TXT files
so that molecular testing results are captured in the laboratory information
system.

**Why this priority**: Two contract-required analyzers use file-based export.
Without file import capability, these high-value molecular instruments cannot be
integrated.

**Independent Test**: Can be fully tested by placing a QuantStudio CSV export
file in the configured import directory and verifying results appear in the
analyzer results queue.

**Acceptance Scenarios**:

1. **Given** a file import directory is configured for a QuantStudio 7 Flex
   analyzer, **When** a CSV result file is placed in the directory, **Then** the
   system detects the file within 60 seconds and processes it.

2. **Given** file mappings are configured for QuantStudio CSV format, **When** a
   result file is processed, **Then** each row is mapped to the appropriate
   OpenELIS test and patient based on configured column mappings.

3. **Given** a result file contains duplicate sample IDs (reprocessed samples),
   **When** the system imports the file, **Then** duplicate detection warns the
   user before creating new results.

4. **Given** an imported file contains malformed data (missing required
   columns), **When** processing fails, **Then** the file is moved to an error
   directory with a log entry describing the parsing failure.

---

### User Story 5 - Comprehensive Instrument Metadata Management (Priority: P2)

As a laboratory manager, I need to capture and track comprehensive instrument
metadata (installation date, location, calibration status, warranty, software
versions) so that I can manage the laboratory's instrument inventory and
maintenance schedules.

**Why this priority**: The Madagascar contract requires comprehensive instrument
tracking beyond basic analyzer registration. This supports laboratory
accreditation (SLIPTA/ISO 15189).

**Independent Test**: Can be fully tested by registering a new analyzer with
complete metadata and verifying all fields are stored and displayed correctly.

**Acceptance Scenarios**:

1. **Given** an administrator is adding a new analyzer, **When** they complete
   the enhanced instrument details form, **Then** the system captures:
   manufacturer, model, serial number, installation date, hierarchical location
   (region/district/facility/room), warranty expiration, and software version.

2. **Given** an analyzer has module components (e.g., GeneXpert 4-module
   system), **When** viewing the analyzer details, **Then** each module is
   listed with its individual serial number, status, and test count.

3. **Given** an analyzer is relocated from one facility to another, **When** the
   administrator updates the location, **Then** the system preserves location
   history with effective dates while associating new results with the new
   location.

4. **Given** an analyzer's calibration is approaching due date, **When** the due
   date is within 30 days, **Then** the system displays a warning indicator on
   the analyzer dashboard.

---

### User Story 6 - Integration of Existing Analyzer Plugins (Priority: P2)

As a system administrator, I need to use existing analyzer plugins (Mindray,
SysmexXN-L, QuantStudio3, GeneXpertHL7) with the new mapping system so that I
can leverage proven implementations while benefiting from centralized field
mapping configuration.

**Why this priority**: 19+ existing plugins represent significant development
investment. Integrating them with the 004 mapping system maximizes value and
reduces implementation time.

**Independent Test**: Can be fully tested by enabling an existing plugin (e.g.,
Mindray) and verifying it works with the field mapping configuration interface.

**Acceptance Scenarios**:

1. **Given** the Mindray plugin is installed, **When** a Mindray analyzer sends
   results, **Then** the system processes results using the plugin's native
   parsing combined with user-configured field mappings for customization.

2. **Given** an existing plugin (SysmexXN-L) has hardcoded test mappings,
   **When** an administrator creates override mappings in the configuration
   interface, **Then** the override mappings take precedence over plugin
   defaults.

3. **Given** a new analyzer type has no existing plugin, **When** the
   administrator configures it using only the mapping system, **Then** the
   system processes messages using generic ASTM/HL7 parsing with configured
   mappings.

---

### User Story 7 - GeneXpert Module Management (Priority: P3)

As a laboratory supervisor managing GeneXpert instruments, I need to track
module status (enabled/disabled), module failures, and module replacement
history so that I can optimize instrument utilization and predict consumable
needs.

**Why this priority**: GeneXpert-specific requirements are contractual but apply
only to GeneXpert instruments. This is important for TB/HIV testing programs but
not blocking for overall contract compliance.

**Independent Test**: Can be fully tested by configuring GeneXpert module
tracking and simulating module status changes.

**Acceptance Scenarios**:

1. **Given** a GeneXpert instrument is configured with module tracking enabled,
   **When** a module is disabled on the instrument, **Then** the system detects
   the status change within the next polling interval (configurable, default 5
   minutes).

2. **Given** module tracking data is available, **When** viewing the GeneXpert
   dashboard, **Then** the system displays: tests run per module, failure rate
   per module, and predicted replacement dates.

3. **Given** a module failure rate exceeds a configurable threshold (default
   10%), **When** the threshold is breached, **Then** the system generates an
   alert recommending module replacement.

---

### User Story 8 - Maintenance Tracking (Priority: P3)

As a quality assurance officer, I need to track calibration events, preventative
maintenance schedules, and curative maintenance records for all instruments so
that I can ensure compliance with accreditation requirements.

**Why this priority**: Maintenance tracking is a contract requirement but is
deprioritized for post-deadline delivery as it does not affect analyzer
communication capability.

**Independent Test**: Can be fully tested by recording a calibration event and
verifying it appears in the instrument's maintenance history.

**Acceptance Scenarios**:

1. **Given** a maintenance event (calibration, preventative, curative) occurs,
   **When** the technician records the event, **Then** the system captures:
   event type, date, performed by, results/notes, and next scheduled date.

2. **Given** preventative maintenance is scheduled, **When** the scheduled date
   is within 7 days, **Then** the system displays a reminder on the analyzer
   dashboard and optionally sends email notification.

3. **Given** maintenance history exists for an analyzer, **When** generating a
   compliance report, **Then** the report shows all events in chronological
   order with gap analysis (missed maintenance periods).

---

### User Story 9 - Analyzer Simulator for Testing (Priority: P1)

As a developer or QA engineer, I need a multi-protocol analyzer simulator that
can generate realistic analyzer messages (ASTM, HL7, file-based) so that I can
develop and test analyzer integration without access to physical analyzers.

**Why this priority**: Testing infrastructure is essential for development
velocity and CI/CD automation. Without it, developers cannot efficiently build
or validate analyzer integrations.

**Independent Test**: Can be fully tested by starting the simulator, configuring
it for a specific analyzer type, and verifying OpenELIS receives and processes
the simulated messages.

**Acceptance Scenarios**:

1. **Given** the simulator is configured for an ASTM-protocol analyzer, **When**
   the simulator generates a result message, **Then** OpenELIS receives and
   processes the message identically to a physical analyzer.

2. **Given** the simulator is configured for an HL7 v2.x analyzer (e.g., Mindray
   BC-5380), **When** the simulator generates an ORU^R01 message, **Then**
   OpenELIS receives and processes the HL7 message correctly.

3. **Given** the simulator is configured for file-based import (e.g.,
   QuantStudio CSV), **When** the simulator generates a result file in the
   watched directory, **Then** OpenELIS detects and imports the file within 60
   seconds.

4. **Given** order export is configured, **When** OpenELIS sends an order to the
   simulator, **Then** the simulator acknowledges receipt and can optionally
   simulate returning results for that order.

5. **Given** the simulator is running in API mode, **When** a CI/CD pipeline
   triggers test scenarios via HTTP, **Then** the simulator executes the
   requested scenario and returns success/failure status.

---

### Edge Cases

- **Simultaneous messages from multiple analyzers**: System must correctly route
  and process messages concurrently without data mixing or deadlocks.
- **Network connectivity loss during message transmission**: Partial messages
  must be handled gracefully with retry mechanisms and error logging.
- **Analyzer sends results for unknown patient/sample**: System must queue
  results with warnings rather than rejecting outright, allowing manual patient
  matching.
- **Time zone differences between analyzer and server**: All timestamps must be
  stored in UTC with proper conversion for display.
- **Duplicate result submission**: System must detect and flag duplicates based
  on sample ID + test + timestamp combination.
- **RS232 buffer overflow on high-volume analyzers**: Serial communication must
  implement proper flow control to prevent data loss.
- **File import directory permissions**: System must verify write permissions
  for moving processed files to archive/error directories.
- **HL7 message encoding variations**: System must handle both ASCII and UTF-8
  encoded HL7 messages from different analyzer vendors.

---

## Requirements _(mandatory)_

### Functional Requirements

#### Protocol Support (Contract Critical - Deadline: 2026-02-28)

- **FR-001**: System MUST support HL7 v2.x message parsing and generation for
  analyzer communication, including ORU^R01 (results) and ORM^O01 (orders)
  message types.

- **FR-002**: System MUST support RS232 serial communication with configurable
  parameters (baud rate: 9600-115200, data bits: 7-8, parity: none/even/odd,
  stop bits: 1-2, flow control: none/RTS-CTS/XON-XOFF; defaults: 9600 baud, 8
  data bits, no parity, 1 stop bit, no flow control). RS232 connectivity is
  provided via the extended ASTM-HTTP Bridge running on a lab PC with
  USB-to-serial adapter, converting RS232→TCP for forwarding to OpenELIS server.

- **FR-003**: System MUST support file-based result import from configured
  directories with automatic file detection, parsing, and archival.

- **FR-004**: System MUST support bidirectional test order export to analyzers
  via ASTM O-segment and HL7 ORM^O01 messages.

- **FR-005**: System MUST identify incoming messages by multiple strategies:
  ASTM header parsing, HL7 MSH segment sender ID, client IP address, or serial
  port assignment.

  **Identification precedence (deterministic)**:

  1. **Protocol-native identity**:
     - HL7: MSH sender fields (MSH-3/MSH-4)
     - ASTM: H-segment identity fields (sender / instrument ID)
     - File: import configuration for the watched directory + file pattern
  2. **Transport identity**:
     - Network client IP (when protocol-native identity is missing/blank)
     - Serial port assignment (when receiving via RS232 bridge)
  3. **Conflict handling**:
     - If multiple analyzers match, the message MUST NOT be processed.
     - An error record MUST be created with the raw message and matching
       candidates for operator resolution.

#### Analyzer Coverage (Contract Critical - Deadline: 2026-02-28)

- **FR-006**: System MUST support all 12 contract-required analyzers with
  bidirectional communication. Implementation order based on Romain's deployment
  priorities (P1 first, then P1-M, then P2):

  **P1 - High Priority (Romain's List - High)**:

  1. Cepheid GeneXpert (ASTM/HL7)
  2. Horiba ABX Micros 60 (ASTM over RS232)
  3. Thermo Fisher QuantStudio 7 Flex (File-based)
  4. Mindray BC-5380 (HL7 over Network)
  5. Mindray BA-88A (RS232)
  6. Horiba ABX Pentra 60 (ASTM over RS232)
  7. Abbott Architect (HL7 over RS232/Network)
  8. Hain Lifescience FluoroCycler XT (File-based)
  9. Mindray BS-360E (HL7 over Network)

  **P1-M - Moderate Priority (Romain's List - Moderate)**: 10. Stago STart 4
  (ASTM/HL7 over RS232/Network)

  **P2 - Lower Priority (Not on Romain's List)**: 11. Mindray BC2000 (HL7 over
  Network) 12. Sysmex XN Series (HL7 over Network)

#### Analyzer/Protocol/Simulator Coverage Matrix

Ordered by implementation priority (Romain's deployment list):

| Analyzer             | Protocol      | Adapter (M1-M3)    | Simulator Template (M4/M16)                                 | Plugin Status                   | Priority |
| -------------------- | ------------- | ------------------ | ----------------------------------------------------------- | ------------------------------- | -------- |
| GeneXpert            | ASTM/HL7/File | Existing + M1 + M3 | `genexpert.json`                                            | ✅ Existing (3 variants)        | P1       |
| Horiba Micros 60     | RS232/ASTM    | M2 (SerialAdapter) | `horiba_micros60.json`                                      | ❌ Build new (M10)              | P1       |
| QuantStudio 7 Flex   | File          | M3 (FileAdapter)   | `quantstudio7.json`                                         | ⚠️ Adapt QuantStudio3 (M8)      | P1       |
| Mindray BC-5380      | HL7           | M1 (HL7Adapter)    | `mindray_bc5380.json`                                       | ✅ Existing (Mindray plugin)    | P1       |
| Mindray BA-88A       | RS232/ASTM    | M2 (SerialAdapter) | `mindray_ba88a.json`                                        | ✅ Existing (Mindray plugin)    | P1       |
| Horiba Pentra 60     | RS232/ASTM    | M2 (SerialAdapter) | `horiba_pentra60.json`                                      | ❌ Build new (M9)               | P1       |
| Abbott Architect     | HL7/RS232     | M1 or M2           | `abbott_architect_hl7.json`, `abbott_architect_serial.json` | ❌ Build new (M12)              | P1       |
| Hain FluoroCycler XT | File          | M3 (FileAdapter)   | `hain_fluorocycler.json`                                    | ❌ Build new (M13)              | P1       |
| Mindray BS-360E      | HL7           | M1 (HL7Adapter)    | `mindray_bs360e.json`                                       | ✅ Existing (Mindray plugin)    | P1       |
| Stago STart 4        | ASTM/HL7      | M1 or Existing     | `stago_start4.json`                                         | ❌ Build new (M11)              | P1-M     |
| Mindray BC2000       | HL7           | M1 (HL7Adapter)    | Shares BC-5380 template                                     | ✅ Existing (Mindray plugin)    | P2       |
| Sysmex XN Series     | HL7           | M1 (HL7Adapter)    | `sysmex_xn.json`                                            | ✅ Existing (SysmexXN-L plugin) | P2       |

- **FR-007**: System MUST integrate with existing analyzer plugins (Mindray,
  SysmexXN-L, GeneXpertHL7, GeneXpertFile, QuantStudio3) via the **external
  plugin JAR pattern** (`plugins/analyzers/`). All new analyzers (M9-M13) MUST
  also be external plugin JARs following this pattern. See
  [docs/analyzer.md](../../docs/analyzer.md) for the mandatory external plugin
  model documentation.

#### Instrument Metadata (Contract Critical - Deadline: 2026-02-28)

- **FR-008**: System MUST capture comprehensive instrument metadata including:
  manufacturer, model, type, serial number, installation date, and software
  version.

- **FR-009**: System MUST support hierarchical location assignment for
  instruments by linking to existing OpenELIS Organization/Location entities
  (reuse existing facility hierarchy), with optional room-level extension and
  location history tracking.

- **FR-010**: System MUST track instrument status including: calibration due
  date, warranty expiration, and service status.

- **FR-011**: System MUST support instrument relocation between facilities while
  preserving historical result associations with original test location.

#### Error Handling & Reprocessing (Extends 004)

- **FR-012**: System MUST create error records for: unmapped fields, protocol
  errors, validation failures, connection timeouts, and partial message
  reception.

- **FR-013**: System MUST support batch reprocessing of error queue items after
  mapping configuration is updated.

- **FR-014**: System MUST provide real-time connection status indicators for all
  configured analyzers.

#### Order Export Workflow

- **FR-015**: System MUST allow users to manually select pending samples/orders
  and trigger export to configured analyzers (manual trigger only for deadline;
  host-query mode where analyzers poll for orders is deferred to post-deadline).

- **FR-016**: System MUST track order export status: pending, sent,
  acknowledged, results received, expired.

  **Acknowledgment semantics (by protocol)**:

  - HL7: `ACKNOWLEDGED` means OpenELIS received a valid HL7 ACK for the exported
    ORM^O01 message (or vendor middleware ACK if middleware is used).
  - ASTM: `ACKNOWLEDGED` is supported only when the analyzer/bridge provides an
    explicit acknowledgment signal; otherwise the highest state is `SENT` until
    results are received.
  - For the contract deadline, lack of an acknowledgment mechanism MUST NOT
    block order export; status MUST still progress to `SENT` and eventually to
    `RESULTS_RECEIVED` when matching results arrive.

- **FR-017**: System MUST support order cancellation with notification to
  analyzers that support cancel messages.

- **FR-018**: System MUST automatically match incoming results to exported
  orders using sample/accession identifiers.

#### GeneXpert Module Management (Post-Deadline; Out of Scope for 2026-02-28)

- **FR-019**: System MUST track GeneXpert module status
  (enabled/disabled/replaced) with automatic detection polling.

- **FR-020**: System MUST calculate and display module statistics: tests run,
  failure rate, error distribution, and predicted replacement date.

- **FR-021**: System MUST generate alerts when module failure rate exceeds
  configurable thresholds.

#### Maintenance Tracking (Post-Deadline; Out of Scope for 2026-02-28)

- **FR-022**: System MUST record maintenance events (calibration, preventative,
  curative) with: event type, date, performer, results, and next scheduled date.

- **FR-023**: System MUST generate maintenance due reminders based on
  configurable schedules per analyzer type.

- **FR-024**: System MUST support spare parts registry with part usage tracking
  and replacement history.

#### Testing Infrastructure (Development & Validation)

- **FR-025**: Project MUST provide a multi-protocol analyzer simulator
  (expanding existing ASTM mock server) that supports ASTM, HL7 v2.x, and
  file-based result generation for development testing without physical
  analyzers.

- **FR-026**: Analyzer simulator MUST support RS232 serial communication via
  virtual serial ports, enabling full end-to-end testing of serial-connected
  analyzer workflows.

- **FR-027**: Analyzer simulator MUST provide configurable message templates for
  all 12 contract-required analyzer types, generating realistic test data
  including QC results, patient results, and error conditions.

- **FR-028**: Analyzer simulator MUST support host-query responses and order
  acknowledgment simulation for testing bidirectional communication workflows.

- **FR-029**: Analyzer simulator MUST integrate with CI/CD pipelines via HTTP
  API mode for automated regression testing of analyzer communication.

- **FR-030**: Analyzer simulator MUST support concurrent simulation of multiple
  analyzers of different types for multi-instrument load testing scenarios.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks. The analyzer configuration, error dashboard, and order
  export interfaces follow Carbon patterns established in feature 004.

- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text). Translations required for English and French (Madagascar's
  official languages).

- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form).

  - New entities (InstrumentMetadata, MaintenanceEvent, ModuleStatus) MUST use
    JPA/Hibernate annotations.
  - Integration with legacy Analyzer entity (XML-mapped) follows the manual
    relationship management pattern established in feature 004.
  - Services MUST compile all data within transactions to prevent
    LazyInitializationException.

- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML). All schema changes for new tables placed in appropriate version
  folder.

- **CR-005**: FHIR R4 integration for device/location resources if external
  system integration is required for facility registry synchronization.

- **CR-006**: Configuration-driven variation for country-specific analyzer
  configurations. No hardcoded Madagascar-specific logic.

- **CR-007**: Security: RBAC for analyzer configuration (LAB_SUPERVISOR role
  minimum), audit trail for all configuration changes, input validation for
  serial port and network parameters.

- **CR-008**: Tests MUST be included:
  - Unit tests for HL7 message parsing, RS232 protocol handling, file import
    logic
  - Integration tests for end-to-end message flow using the multi-protocol
    analyzer simulator
  - E2E tests for analyzer configuration and order export workflows
  - CI/CD automated tests using simulator API mode for regression testing

### Key Entities

- **InstrumentMetadata**: Extended instrument information including installation
  date, warranty expiration, software version, and hierarchical location.
  One-to-one relationship with existing Analyzer entity.

- **InstrumentLocation**: Links instruments to existing OpenELIS
  Organization/Location entities (reusing existing facility hierarchy). Optional
  room-level extension for more granular placement tracking within facilities.
  Implemented as a location reference (e.g. locationId) on InstrumentMetadata
  and/or InstrumentLocationHistory; no separate InstrumentLocation table
  required.

- **InstrumentLocationHistory**: Historical record of instrument locations with
  effective dates for audit trail.

- **MaintenanceEvent**: Calibration, preventative, and curative maintenance
  records with performer, date, results, and next scheduled date.

- **OrderExport**: Tracks test orders sent to analyzers with status (pending,
  sent, acknowledged, results_received, expired, cancelled).

- **GeneXpertModule**: Module-level tracking for GeneXpert instruments including
  serial number, status, test count, and failure metrics.

- **SerialPortConfiguration**: RS232 configuration parameters (port, baud rate,
  parity, stop bits, flow control) linked to AnalyzerConfiguration.

- **FileImportConfiguration**: File-based import settings including directory
  path, file pattern, column mappings, and archive/error directory paths.

---

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: All 12 contract-required analyzers successfully receive test
  results in OpenELIS within 60 seconds of analyzer transmission. For go-live,
  timing may be measured via simulator-based end-to-end tests; post-deployment
  validation with actual instruments is recommended.

- **SC-002**: All 12 contract-required analyzers successfully receive test
  orders exported from OpenELIS (verified by order appearing on analyzer pending
  work list).

- **SC-003**: System correctly identifies and routes messages from 5+ analyzers
  operating simultaneously without message misrouting or data corruption.

- **SC-004**: Less than 5% of incoming analyzer messages generate mapping errors
  after initial configuration is complete. **Measurement window**: first 1000
  messages OR first 7 calendar days of production operation, whichever comes
  first. Excludes messages during initial configuration/tuning period.

- **SC-005**: Laboratory technicians complete analyzer configuration
  (connection + basic mappings) in under 30 minutes for analyzers with existing
  plugins, under 60 minutes for new analyzer types.

- **SC-006**: File-based import detects and processes new result files within 60
  seconds of file creation in the import directory.

- **SC-007**: RS232 serial communication operates reliably at 9600 baud for 8+
  hours continuous operation without buffer overflow or data loss.

- **SC-008**: Order export retry mechanism successfully delivers orders within 3
  retry attempts for 95%+ of recoverable connection failures.

- **SC-009**: Instrument metadata form captures all required fields with
  validation preventing incomplete registrations.

- **SC-010**: System maintains 99%+ uptime for analyzer communication services
  during laboratory operating hours (verified via connection status monitoring).

- **SC-011**: Multi-protocol analyzer simulator supports all 12
  contract-required analyzer types with configurable message templates, enabling
  developers to test integrations without physical analyzers (verified by CI/CD
  pipeline integration tests passing).

- **SC-012**: Simulator validation covers all acceptance scenarios including:
  normal result flow, error conditions, edge cases (duplicate results, unknown
  patients, malformed messages), and bidirectional order export/acknowledgment.
  Simulator-only validation is sufficient for contract go-live; physical
  analyzer testing occurs post-deployment in Madagascar.

---

## Assumptions & Constraints

### Assumptions

1. **External Plugin JAR Pattern**: All analyzer plugins are external JARs in
   `plugins/analyzers/` (git submodule from I-TECH-UW/openelisglobal-plugins).
   Plugins register via `connect()` method called by PluginLoader at startup. NO
   analyzer code belongs in `src/main/java/`. See
   [docs/analyzer.md](../../docs/analyzer.md) for the mandatory external plugin
   model documentation.

2. **Physical Connectivity**: Madagascar laboratories have necessary network
   infrastructure (Ethernet switches, IP addressing) and RS232 hardware
   (USB-to-serial adapters, cables) for analyzer connections.

3. **Analyzer Documentation**: Protocol specifications and message formats are
   available from analyzer vendors (Mindray, Horiba, Sysmex, Abbott, Thermo
   Fisher, Cepheid, Hain, Stago).

4. **Feature 004 Stability**: The 004-astm-analyzer-mapping infrastructure is
   stable and provides a solid foundation for extending to new protocols.

5. **RS232 via Bridge**: RS232 analyzers connect via the extended ASTM-HTTP
   Bridge running on a lab PC. The bridge handles RS232→TCP conversion locally,
   eliminating the need for Docker serial passthrough. Each lab requires a PC
   with USB-to-serial adapter(s) running the bridge software.

### Constraints

1. **Contract Deadline**: 12 minimum analyzers must be fully operational by
   2026-02-28. No extensions available.

2. **Protocol Complexity**: HL7 v2.x message parsing is complex with many
   vendor-specific variations. Implementation must handle common variations
   gracefully.

3. **Legacy Integration**: The Analyzer entity uses XML-based Hibernate
   mappings. New entities must work around this constraint using the established
   manual relationship management pattern.

4. **Backward Compatibility**: Existing analyzer plugins must continue to
   function. The new protocol adapters (HL7, RS232, File) must not break
   existing ASTM-based integrations.

5. **Resource Availability**: Physical analyzers are NOT available for
   pre-deployment validation. Simulator validation is sufficient for go-live;
   physical testing happens post-deployment in Madagascar laboratories. This
   accepts higher risk of in-field issues but meets the contract deadline.

6. **Multi-Protocol Flexibility**: Analyzers with uncertain protocol
   specifications (Abbott Architect, Hain FluoroCycler XT, etc.) MUST be
   implemented with flexible adapter support for multiple protocols (e.g., HL7 +
   RS232, or File + Network). This enables protocol discovery during deployment
   without code changes. The adapter pattern from Feature 004 provides this
   flexibility.

   > **⚠️ Abbott Note**: Research indicates Abbott m2000/Architect series may
   > require vendor-provided middleware (Abbott AlinIQ) for HL7/ASTM
   > integration. Verify with Madagascar lab whether middleware is deployed. If
   > so, OpenELIS connects to middleware (standard HL7), not directly to
   > analyzer.

7. **External Plugin Architecture**: Per `docs/analyzer.md`, all new analyzer
   implementations MUST use the external plugin model. Plugins live in
   `plugins/analyzers/{PluginName}/`, build as standalone JARs, and deploy to
   `/var/lib/openelis-global/plugins/`. Do NOT create `@Component` classes in
   `src/main/java/` for analyzer plugins.

---

## Relationship to Feature 004

This feature **extends** the 004-astm-analyzer-mapping feature rather than
replacing it:

| Component              | Feature 004 Status | Feature 011 Action         |
| ---------------------- | ------------------ | -------------------------- |
| ASTM protocol          | ✅ Complete        | Reuse as-is                |
| Field mapping engine   | ✅ Complete        | Extend for HL7/File        |
| TCP/IP connectivity    | ✅ Complete        | Reuse via bridge           |
| Error dashboard        | ✅ Complete        | Extend for new error types |
| Analyzer configuration | ✅ Complete        | Extend with metadata       |
| HL7 protocol           | ❌ Not implemented | **NEW**                    |
| RS232 serial           | ❌ Not implemented | **NEW**                    |
| File-based import      | ❌ Not implemented | **NEW**                    |
| Order export           | ⚠️ Limited         | **ENHANCE**                |
| Instrument metadata    | ⚠️ Basic           | **ENHANCE**                |
| Maintenance tracking   | ❌ Not implemented | **NEW** (post-deadline)    |
| GeneXpert modules      | ❌ Not implemented | **NEW** (post-deadline)    |

---

## Target Analyzers Summary

Priority assignment based on Romain's internal deployment list (Session
2026-01-27 clarification):

- **P1 (High)**: Analyzers on Romain's list with high priority - immediate
  deployment expected
- **P1-M (Moderate)**: Analyzers on Romain's list with moderate priority
- **P2**: Analyzers NOT on Romain's list or file-based - contract required but
  lower deployment urgency

See FR-006 § Analyzer/Protocol/Simulator Coverage Matrix above for the full
12-analyzer table with protocol, adapter, simulator, and plugin details.

### P3 Stretch Goals (If Time Permits After Contract Requirements)

Additional high-priority analyzers from Romain's list - attempt only after P1/P2
complete:

| #   | Analyzer               | Category         | Protocol   | Connectivity       | Plugin Status            | Notes                      |
| --- | ---------------------- | ---------------- | ---------- | ------------------ | ------------------------ | -------------------------- |
| 13  | Mindray BC5300         | Hematology       | HL7        | Network            | ⚠️ Likely Mindray plugin | Similar to BC-5380         |
| 14  | Mindray BS-120         | Chemistry        | HL7        | Network            | ⚠️ Likely Mindray plugin | Similar to BS-360E         |
| 15  | Mindray BS-200         | Chemistry        | RS232/LIS  | RS232              | ⚠️ Likely Mindray plugin | RS232 bidirectional        |
| 16  | Mindray BS-230         | Chemistry        | RS232/LIS  | RS232              | ⚠️ Likely Mindray plugin | RS232 bidirectional        |
| 17  | Bio-Rad CFX96          | Real-Time PCR    | File-based | USB/Ethernet       | ❌ Build new             | PC-controlled, file export |
| 18  | BioMérieux VIDAS       | Immunoassay      | LIS Bidir  | Network            | ❌ Build new             | Protocol TBD               |
| 19  | Analytik Jena qTOWER3G | Real-Time PCR    | File-based | USB/PC             | ❌ Build new             | File-based export          |
| 20  | Applied Biosystems QS5 | Real-Time PCR    | File-based | Ethernet/USB/Wi-Fi | ⚠️ Adapt QuantStudio3    | Similar to QS7 Flex        |
| 21  | Bio-Rad C1000 Touch    | PCR Thermocycler | File-based | USB/Ethernet       | ❌ Build new             | PC-controlled              |
| 22  | Finecare Plus FIA      | Immunoassay      | LIS/HIS    | PC                 | ❌ Build new             | Protocol TBD               |

---

### Existing Plugin Coverage

| Plugin Name      | Location                           | Analyzers Covered                | Protocol  |
| ---------------- | ---------------------------------- | -------------------------------- | --------- |
| Mindray          | `plugins/analyzers/Mindray/`       | BC-5380, BS-360E, BC2000, BA-88A | HL7/RS232 |
| SysmexXN-L       | `plugins/analyzers/SysmexXN-L/`    | Sysmex XN Series                 | HL7       |
| GeneXpertHL7     | `plugins/analyzers/GeneXpertHL7/`  | Cepheid GeneXpert                | HL7       |
| GeneXpertFile    | `plugins/analyzers/GeneXpertFile/` | Cepheid GeneXpert                | File      |
| GeneXpert (ASTM) | `plugins/analyzers/GeneXpert/`     | Cepheid GeneXpert                | ASTM      |
| QuantStudio3     | `plugins/analyzers/QuantStudio3/`  | QuantStudio 7 Flex (adapted)     | File      |

> **Architecture rule**: All plugins follow the **external plugin JAR pattern**
> in `plugins/analyzers/`. NO analyzer code in `src/main/java/`. See
> [docs/analyzer.md](../../docs/analyzer.md).

**Analyzers requiring new external plugin JARs** (5 total, in
`plugins/analyzers/`):

- Horiba Pentra 60 → `plugins/analyzers/HoribaPentra60/` (M9)
- Horiba Micros 60 → `plugins/analyzers/HoribaMicros60/` (M10)
- Stago STart 4 → `plugins/analyzers/StagoSTart4/` (M11)
- Abbott Architect → `plugins/analyzers/AbbottArchitect/` (M12)
- Hain FluoroCycler XT → `plugins/analyzers/FluoroCyclerXT/` (M13)

---

## References

- **Plugin Architecture Guide (CRITICAL)**:
  [docs/analyzer.md](../../docs/analyzer.md) — External plugin JAR pattern
  documentation. All new analyzers MUST follow this guide.
- **Research Report**:
  `.specify/artifacts/ANALYZER-MADAGASCAR-RESEARCH-REPORT.md`
- **Executive Summary**:
  `.specify/artifacts/ANALYZER-MADAGASCAR-EXECUTIVE-SUMMARY.md`
- **Feature 004 Specification**: `specs/004-astm-analyzer-mapping/spec.md`
- **Existing Plugins**:
  https://github.com/DIGI-UW/openelisglobal-plugins/tree/develop/analyzers
- **ASTM-HTTP Bridge**: https://github.com/DIGI-UW/openelis-analyzer-bridge
- **Analyzer Mock Server** (Multi-Protocol Analyzer Simulator):
  https://github.com/DIGI-UW/analyzer-mock-server - Supports ASTM, HL7 v2.x,
  RS232 simulation, and file-based result generation for analyzer testing
