# Research: Sample Storage Management

**Date**: 2025-10-30  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage

## 1. Hibernate XML Mapping Pattern

**Examined Files**:

- `src/main/resources/hibernate/hbm/Person.hbm.xml`
- `src/main/resources/hibernate/hbm/Patient.hbm.xml`
- `src/main/resources/hibernate/hbm/Sample.hbm.xml`
- `src/main/resources/hibernate/hbm/ElectronicOrder.hbm.xml`

**Pattern Identified**:

```xml
<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<hibernate-mapping>
    <class name="org.openelisglobal.{module}.valueholder.{Entity}"
        table="{TABLE_NAME}" optimistic-lock="version" dynamic-update="true">

        <!-- ID with StringSequenceGenerator -->
        <id name="id"
            type="org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType">
            <column name="ID" precision="10" scale="0" />
            <generator
                class="org.openelisglobal.hibernate.resources.StringSequenceGenerator">
                <param name="sequence_name">{table_name}_seq</param>
            </generator>
        </id>

        <!-- Optimistic locking -->
        <version name="lastupdated" column="LASTUPDATED"
            type="timestamp" access="field" />

        <!-- Properties -->
        <property name="{fieldName}" type="java.lang.String">
            <column name="{COLUMN_NAME}" />
        </property>

        <!-- Many-to-One relationships -->
        <many-to-one name="{relationName}"
            class="org.openelisglobal.{module}.valueholder.{RelatedEntity}"
            fetch="select" lazy="false">
            <column name="{FOREIGN_KEY_COLUMN}" precision="10" scale="0" not-null="true" />
        </many-to-one>

        <!-- Enum types -->
        <property name="{enumField}" column="{COLUMN_NAME}">
            <type name="org.hibernate.type.EnumType">
                <param name="enumClass">org.openelisglobal.{module}.valueholder.{EnumClass}</param>
                <param name="useNamed">true</param>
            </type>
        </property>
    </class>
</hibernate-mapping>
```

**Key Observations**:

- **ID Generation**: Custom `StringSequenceGenerator` with
  `LIMSStringNumberUserType` converter
- **Optimistic Locking**: `version` field on `lastupdated` column with
  `access="field"`
- **Dynamic Updates**: `dynamic-update="true"` generates SQL with only changed
  fields
- **Column Naming**: Uppercase convention (e.g., `LAST_NAME`,
  `ACCESSION_NUMBER`)
- **Table Naming**: Uppercase, often singular (e.g., `PERSON`, `SAMPLE`,
  `PATIENT`)
- **Lazy Loading**: Explicitly disabled on many-to-one relationships
  (`lazy="false"`)

**Application to Storage Entities**:

For `StorageRoom.hbm.xml`:

```xml
<hibernate-mapping>
    <class name="org.openelisglobal.storage.valueholder.StorageRoom"
        table="STORAGE_ROOM" optimistic-lock="version" dynamic-update="true">
        <id name="id"
            type="org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType">
            <column name="ID" precision="10" scale="0" />
            <generator class="org.openelisglobal.hibernate.resources.StringSequenceGenerator">
                <param name="sequence_name">storage_room_seq</param>
            </generator>
        </id>
        <version name="lastupdated" column="LASTUPDATED" type="timestamp" access="field" />

        <property name="fhirUuid" type="java.util.UUID">
            <column name="FHIR_UUID" not-null="true" unique="true" />
        </property>
        <property name="name" type="java.lang.String">
            <column name="NAME" length="255" not-null="true" />
        </property>
        <property name="code" type="java.lang.String">
            <column name="CODE" length="50" not-null="true" unique="true" />
        </property>
        <property name="description" type="java.lang.String">
            <column name="DESCRIPTION" />
        </property>
        <property name="active" type="java.lang.Boolean">
            <column name="ACTIVE" not-null="true" />
        </property>
    </class>
</hibernate-mapping>
```

Similar patterns apply to StorageDevice, StorageShelf, StorageRack with
many-to-one relationships to parent entities.

---

## 2. FHIR Location Resource Structure

**R4 Specification**: https://hl7.org/fhir/R4/location.html

**IHE mCSD Profile**: Mobile Care Services Discovery (mCSD) defines hierarchical
location structures for facility registries.

**FHIR R4 Location Resource Structure**:

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{hierarchical_code}"
  }],
  "status": "active" | "inactive",
  "name": "{location_name}",
  "description": "{optional_description}",
  "mode": "instance",
  "type": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro" | "ve" | "co",
      "display": "Room" | "Vehicle" | "Container"
    }]
  }],
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro",
      "display": "Room"
    }]
  },
  "partOf": {
    "reference": "Location/{parent_fhir_uuid}",
    "display": "{parent_name}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 100
  }]
}
```

**Mapping Strategy for Storage Hierarchy**:

| OpenELIS Entity | FHIR Location Type          | physicalType Code        | Notes                                                    |
| --------------- | --------------------------- | ------------------------ | -------------------------------------------------------- |
| StorageRoom     | Location                    | `ro` (room)              | Top-level, no partOf reference                           |
| StorageDevice   | Location                    | `ve` (vehicle/equipment) | partOf = Room Location, type = freezer/fridge/cabinet    |
| StorageShelf    | Location                    | `co` (container)         | partOf = Device Location                                 |
| StorageRack     | Location                    | `co` (container)         | partOf = Shelf Location, extension for grid dimensions   |
| StoragePosition | N/A - not separate resource | N/A                      | Positions encoded in Rack extension[available-positions] |

**Hierarchical Navigation via IHE mCSD**:

- Query all rooms: `GET /fhir/Location?physicalType=ro`
- Query devices in room: `GET /fhir/Location?partOf=Location/{room_uuid}`
- Include parent hierarchy:
  `GET /fhir/Location/{device_uuid}?_include=Location:partOf`

**Sample-to-Location Link via Specimen Resource**:

```json
{
  "resourceType": "Specimen",
  "id": "{sample_fhir_uuid}",
  "container": [
    {
      "identifier": {
        "value": "{hierarchical_location_path}"
      },
      "extension": [
        {
          "url": "http://openelis.org/fhir/extension/storage-rack",
          "valueReference": {
            "reference": "Location/{rack_fhir_uuid}"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-position",
          "valueString": "{position_coordinate}"
        }
      ]
    }
  ]
}
```

**Decision**: Use FHIR Location resources for Room, Device, Shelf, Rack.
Positions tracked only in OpenELIS database (not synced to FHIR).
Sample-to-location link via Specimen.container extension.

---

## 3. Carbon Dropdown Cascading Pattern

**Component**: `@carbon/react` Dropdown component (v1.15.0)

**API Reference**:
https://react.carbondesignsystem.com/?path=/docs/components-dropdown--overview

**Pattern for Cascading Dropdowns**:

```javascript
import { Dropdown } from "@carbon/react";
import { useState, useEffect } from "react";

function CascadingLocationSelector({ onLocationChange }) {
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);

  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);

  // Fetch devices when room selected
  useEffect(() => {
    if (selectedRoom) {
      fetchDevices(selectedRoom.id).then(setDevices);
      setSelectedDevice(null); // Reset child selections
      setSelectedShelf(null);
      setSelectedRack(null);
    }
  }, [selectedRoom]);

  // Fetch shelves when device selected
  useEffect(() => {
    if (selectedDevice) {
      fetchShelves(selectedDevice.id).then(setShelves);
      setSelectedShelf(null);
      setSelectedRack(null);
    }
  }, [selectedDevice]);

  // Fetch racks when shelf selected
  useEffect(() => {
    if (selectedShelf) {
      fetchRacks(selectedShelf.id).then(setRacks);
      setSelectedRack(null);
    }
  }, [selectedShelf]);

  return (
    <>
      <Dropdown
        id="room-dropdown"
        titleText="Room"
        label="Select room"
        items={rooms}
        itemToString={(item) => item?.name || ""}
        onChange={({ selectedItem }) => setSelectedRoom(selectedItem)}
        selectedItem={selectedRoom}
      />

      <Dropdown
        id="device-dropdown"
        titleText="Device"
        label="Select device"
        items={devices}
        itemToString={(item) => item?.name || ""}
        onChange={({ selectedItem }) => setSelectedDevice(selectedItem)}
        selectedItem={selectedDevice}
        disabled={!selectedRoom}
      />

      <Dropdown
        id="shelf-dropdown"
        titleText="Shelf"
        label="Select shelf"
        items={shelves}
        itemToString={(item) => item?.label || ""}
        onChange={({ selectedItem }) => setSelectedShelf(selectedItem)}
        selectedItem={selectedShelf}
        disabled={!selectedDevice}
      />

      <Dropdown
        id="rack-dropdown"
        titleText="Rack"
        label="Select rack"
        items={racks}
        itemToString={(item) => item?.label || ""}
        onChange={({ selectedItem }) => setSelectedRack(selectedItem)}
        selectedItem={selectedRack}
        disabled={!selectedShelf}
      />
    </>
  );
}
```

**Key Points**:

- Controlled components with state for each level
- `useEffect` hooks trigger child data fetching on parent selection
- Reset child selections when parent changes
- `disabled` prop prevents selection until parent chosen
- `itemToString` prop formats display text for items

---

## 4. Barcode Scanner Browser Integration

**Event Type**: USB HID barcode scanners emit rapid keyboard events

**Detection Pattern**: Characters arrive within ~30-50ms interval (typical scan
gun speed)

**Implementation Strategy**:

```javascript
import { useEffect, useRef, useState } from "react";

function useBarcodeScanner(onScan, options = {}) {
  const {
    minLength = 3,
    timeout = 50, // ms between characters
  } = options;

  const bufferRef = useRef("");
  const timeoutIdRef = useRef(null);

  useEffect(() => {
    function handleKeyDown(event) {
      // Ignore if user is typing in an input field (unless it's our barcode input)
      if (
        event.target.tagName === "INPUT" &&
        !event.target.dataset.barcodeInput
      ) {
        return;
      }

      // Ignore modifier keys
      if (event.ctrlKey || event.altKey || event.metaKey) {
        return;
      }

      // Handle Enter key (scan complete)
      if (event.key === "Enter") {
        event.preventDefault();
        if (bufferRef.current.length >= minLength) {
          onScan(bufferRef.current);
        }
        bufferRef.current = "";
        return;
      }

      // Add character to buffer
      if (event.key.length === 1) {
        event.preventDefault();
        bufferRef.current += event.key;

        // Reset timeout
        if (timeoutIdRef.current) {
          clearTimeout(timeoutIdRef.current);
        }

        // Set new timeout to detect end of scan
        timeoutIdRef.current = setTimeout(() => {
          if (bufferRef.current.length >= minLength) {
            onScan(bufferRef.current);
          }
          bufferRef.current = "";
        }, timeout);
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      if (timeoutIdRef.current) {
        clearTimeout(timeoutIdRef.current);
      }
    };
  }, [onScan, minLength, timeout]);
}

// Usage in component:
function BarcodeScanMode({ onLocationScanned }) {
  const [scannedCode, setScannedCode] = useState("");

  useBarcodeScanner((barcode) => {
    setScannedCode(barcode);
    // Parse hierarchical barcode (e.g., "MAIN-FRZ01-SHA-RKR1")
    parseAndFetchLocation(barcode).then(onLocationScanned);
  });

  return (
    <TextInput
      id="barcode-input"
      data-barcode-input="true"
      labelText="Scan barcode or enter manually"
      value={scannedCode}
      onChange={(e) => setScannedCode(e.target.value)}
      placeholder="Scan barcode..."
    />
  );
}
```

**Key Points**:

- Detect rapid character input (< 50ms between keys)
- Buffer characters until Enter key or timeout
- Prevent default to avoid input field focus issues
- Allow manual entry fallback in TextInput
- Parse hierarchical barcode format (ROOM-DEVICE-SHELF-RACK)

---

## 5. OpenELIS Frontend Data Fetching Pattern

**Discovery**: OpenELIS does **NOT** use SWR. Instead, uses custom
`getFromOpenElisServer` utility with `useState`/`useEffect`.

**Examined Files**:

- `frontend/src/components/patient/resultsViewer/useObstreeData.ts`
- `frontend/src/components/patient/resultsViewer/usePatientResultsData.ts`
- `frontend/src/components/layout/search/searchService.js`

**Existing Pattern**:

```javascript
import { useState, useEffect } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

function useStorageLocations(parentId, type) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLocations = (response) => {
    setData(response);
    setIsLoading(false);
  };

  const handleError = (error) => {
    setError(error);
    setIsLoading(false);
  };

  useEffect(() => {
    if (parentId && type) {
      setIsLoading(true);
      getFromOpenElisServer(
        `/rest/storage/${type}?parentId=${parentId}`,
        fetchLocations,
        handleError
      );
    }
  }, [parentId, type]);

  return { data, isLoading, error };
}
```

**Decision**: Follow existing OpenELIS pattern with `getFromOpenElisServer`
utility. Do NOT introduce SWR dependency.

**Mutation Pattern** (for POST/PUT/DELETE):

```javascript
import { postToOpenElisServer } from "../utils/Utils";

function useSampleAssignment() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const assignSample = async (assignmentData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServer(
        "/rest/storage/samples/assign",
        JSON.stringify(assignmentData),
        (response) => {
          setIsSubmitting(false);
          resolve(response);
        },
        (error) => {
          setIsSubmitting(false);
          setError(error);
          reject(error);
        }
      );
    });
  };

  return { assignSample, isSubmitting, error };
}
```

---

## 6. Cypress E2E Configuration

**Status**: OpenELIS uses **Cypress 12.17.3** for E2E tests.

**Current E2E Framework**: Cypress 12.17.3 (per constitution)

- Tests in `frontend/cypress/e2e/`
- Configuration: `frontend/cypress.config.js`
- Existing tests: patientEntry.cy.js, orderEntity.cy.js, validation.cy.js, etc.

**Cypress Configuration** (per Constitution V.5):

```javascript
// cypress.config.js
const { defineConfig } = require("cypress");

module.exports = defineConfig({
  video: false, // MUST be disabled by default (per Constitution V.5)
  screenshotOnRunFailure: true, // MUST be enabled (per Constitution V.5)
  defaultCommandTimeout: 30000,
  viewportWidth: 1200,
  viewportHeight: 700,
  watchForFileChanges: false,
  e2e: {
    setupNodeEvents(on, config) {
      // Browser console logging enabled by default (Cypress captures automatically)
      return config;
    },
    baseUrl: "https://localhost",
    testIsolation: false, // Only if shared state needed
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
```

**Note**: For complete Cypress E2E testing guidelines, see Constitution Section
V.5. Key requirements:

- Run tests individually during development (not full suite)
- Browser console logging enabled by default
- Video recording disabled (`video: false`)
- Post-run review of console logs and screenshots required
- Follow intercept timing, retry-ability, and element readiness best practices

**Test Structure** (follow existing pattern):

```
frontend/cypress/e2e/
├── storageAssignment.cy.js (P1 - Storage Assignment)
├── storageSearch.cy.js (P2A - Sample Search/Retrieval)
└── storageMovement.cy.js (P2B - Sample Movement, including bulk)
```

**Example Test Pattern** (updated with best practices per Constitution V.5):

```javascript
// storageAssignment.cy.js - UPDATED with best practices
import LoginPage from "../pages/LoginPage";

describe("Sample Storage Assignment (P1)", function () {
  before("Setup and login", () => {
    // Setup intercepts BEFORE any actions (best practice: intercept timing)
    cy.intercept("GET", "**/rest/storage/rooms").as("getRooms");
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
    cy.intercept("POST", "**/rest/storage/assignments").as("createAssignment");

    // Login
    const loginPage = new LoginPage();
    loginPage.visit();
    const homePage = loginPage.goToHomePage();
    homePage.goToSampleEntry();
  });

  it("should assign sample using cascading dropdowns", function () {
    cy.log("Starting assignment workflow");

    // Wait for storage selector to be ready (best practice: element readiness)
    cy.get('[data-testid="storage-location-selector"]').should("be.visible");

    // Open selector and wait for API call (best practice: intercept timing)
    cy.get('[data-testid="storage-location-selector"]').click();
    cy.wait("@getRooms");

    // Select room - wait for element readiness (best practice: retry-ability)
    cy.get('[data-testid="room-dropdown"]').should("be.visible").click();
    cy.contains("Main Laboratory").should("be.visible").click();

    // Select device - wait for API and element readiness
    cy.wait("@getDevices");
    cy.get('[data-testid="device-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Freezer Unit 1").should("be.visible").click();

    // Select shelf - same pattern
    cy.wait("@getDevices"); // May trigger again for shelf data
    cy.get('[data-testid="shelf-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Shelf-A").should("be.visible").click();

    // Select rack - same pattern
    cy.get('[data-testid="rack-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Rack R1").should("be.visible").click();

    // Enter position - wait for field to be ready
    cy.get('[data-testid="position-input"]').should("be.visible").type("A5");

    // Verify hierarchical path display (best practice: retry-able assertions)
    cy.get('[data-testid="location-path"]').should(
      "contain.text",
      "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
    );

    // Save assignment and verify
    cy.get('[data-testid="save-button"]').should("not.be.disabled").click();
    cy.wait("@createAssignment");
    cy.get('div[role="status"]')
      .should("be.visible")
      .and("contain.text", "assigned successfully");
  });
});
```

**Key Best Practices Demonstrated**:

- **Intercept Timing**: Set up `cy.intercept()` before actions that trigger API
  calls
- **Retry-Ability**: Use `.should()` assertions that automatically retry
- **Element Readiness**: Wait for elements to be visible before interaction
- **State Verification**: Use proper assertions (`contain.text`, `be.visible`)
- **No Arbitrary Waits**: Use `cy.wait('@alias')` instead of `cy.wait(1000)`

**Page Object Pattern** (follow existing structure):

```javascript
// cypress/pages/StorageAssignmentPage.js
class StorageAssignmentPage {
  getStorageLocationSelector() {
    return cy.get('[data-testid="storage-location-selector"]');
  }

  getRoomDropdown() {
    return cy.get('[data-testid="room-dropdown"]');
  }

  selectRoom(roomName) {
    this.getRoomDropdown().click();
    cy.contains(roomName).click();
    return this;
  }

  selectDevice(deviceName) {
    cy.get('[data-testid="device-dropdown"]').click();
    cy.contains(deviceName).click();
    return this;
  }

  enterPosition(coordinate) {
    cy.get('[data-testid="position-input"]').type(coordinate);
    return this;
  }

  clickSave() {
    cy.get('[data-testid="save-button"]').click();
    return this;
  }
}

export default StorageAssignmentPage;
```

**Run Commands** (per Constitution V.5):

```bash
# Run individual test file (RECOMMENDED during development)
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# Run individual test case
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js" --grep "should assign sample"

# Open Cypress UI for interactive debugging (with console logging)
npx cypress open

# Run headed mode (see browser + console)
npx cypress run --headed

# Full suite (CI/CD only)
npm run cy:run
```

**Note**: Per Constitution V.5, tests MUST be run individually during
development (not full suite). Full suite runs are for CI/CD only. After each
run, review browser console logs and screenshots (especially on failures).

---

## 7. Certificate Architecture and Let's Encrypt Setup

**Date**: 2025-11-03  
**Context**: Infrastructure setup for `storage.openelis-global.org` subdomain

### Current Certificate Architecture

**Certificate Generation:**

- Project uses Docker container (`itechuw/certgen:main`) to generate
  **self-signed certificates** for development
- Certificates are generated in the `certs` service (lines 2-15 in
  `dev.docker-compose.yml`)
- Generated certificates include:
  - Self-signed certificate: `/etc/ssl/certs/apache-selfsigned.crt`
  - Private key: `/etc/ssl/private/apache-selfsigned.key`
  - Java keystore: `/etc/openelis-global/keystore` (PKCS12 format)
  - Java truststore: `/etc/openelis-global/truststore` (PKCS12 format)

**Certificate Details:**

- Subject: `CN=localhost`
- Subject Alternative Name: `DNS:*.openelis.org`
- Validity: 365 days
- Format: Self-signed X.509 certificate

**Certificate Distribution:** Certificates are distributed via Docker volumes:

- `key_trust-store-volume:/etc/openelis-global` - Java keystores/truststores
- `certs-vol:/etc/nginx/certs/` - Nginx certificates
- `keys-vol:/etc/nginx/keys/` - Nginx private keys

**Services Using Certificates:**

1. **Nginx Proxy** (`proxy` service):

   - Uses certificates from `certs-vol` and `keys-vol` volumes
   - Listens on ports 80 (HTTP) and 443 (HTTPS)
   - Current `nginx.conf` redirects all HTTP traffic to HTTPS
   - Uses generic `server_name __` (matches any hostname)

2. **OpenELIS Webapp** (`oe.openelis.org` service):

   - Mounts `key_trust-store-volume` for Java SSL communication
   - Uses keystore for outbound HTTPS connections
   - Uses truststore to validate peer certificates

3. **FHIR API** (`fhir.openelis.org` service):
   - Uses Java keystores/truststores via `JAVA_OPTS` environment variables
   - Configured for mutual TLS (mTLS) communication

### Security Considerations

1. **Certificate Storage:**

   - Never commit certificates to git - use `.gitignore` for certificate
     directories
   - Use Docker secrets for sensitive certificate passwords
   - Restrict file permissions on certificate files (600 for keys, 644 for
     certs)

2. **Private Key Protection:**

   - Private keys should be stored in Docker volumes with restricted access
   - Consider using Docker secrets for keystore passwords
   - Rotate keys periodically

3. **Certificate Renewal:**
   - Set up monitoring for certificate expiration
   - Test renewal process before certificates expire
   - Have a fallback mechanism if renewal fails

### Testing the Configuration

**Verify DNS Resolution:**

```bash
dig storage.openelis-global.org
nslookup storage.openelis-global.org
```

**Test HTTP to HTTPS Redirect:**

```bash
curl -I http://storage.openelis-global.org
# Should return 301 redirect to HTTPS
```

**Verify Certificate:**

```bash
openssl s_client -connect storage.openelis-global.org:443 -servername storage.openelis-global.org
```

**Check Certificate Details:**

```bash
echo | openssl s_client -servername storage.openelis-global.org -connect storage.openelis-global.org:443 2>/dev/null | openssl x509 -noout -dates -subject
```

### Troubleshooting

**Common Issues:**

1. **ACME Challenge Fails:**

   - Ensure port 80 is accessible from the internet
   - Verify DNS points to correct IP
   - Check nginx is serving `.well-known/acme-challenge/` path

2. **Certificate Not Found:**

   - Verify certificate location:
     `/etc/letsencrypt/live/storage.openelis-global.org/`
   - Check nginx can read certificate files
   - Ensure proper file permissions

3. **Nginx Won't Start:**

   - Check nginx configuration syntax: `nginx -t`
   - Verify certificate paths in nginx.conf
   - Review Docker logs: `docker logs openelisglobal-proxy`

4. **Certificate Renewal Fails:**
   - Check Certbot logs: `docker logs openelisglobal-certbot-renew`
   - Verify nginx is running during renewal
   - Ensure challenge path is accessible

## 8. Carbon DataTable Expandable Rows

**Date**: 2025-11-07  
**Feature**: Expandable rows in location tables (Rooms, Devices, Shelves, Racks)

### Research Questions

#### Q1: How to implement Carbon DataTable expandable rows?

**Decision**: Use Carbon DataTable `expandableRows` prop with
`TableExpandHeader`, `TableExpandRow`, and `TableExpandedRow` components.

**Rationale**:

- Carbon Design System v1.15 provides built-in expandable row support via
  `expandableRows` prop
- Existing codebase pattern found in `EOrder.js` component demonstrates this
  pattern
- Follows constitution requirement (Principle II: Carbon Design System First)
- Provides accessibility support (ARIA labels, keyboard navigation)

**Implementation Pattern** (from EOrder.js):

```jsx
<DataTable rows={data} headers={headers} expandableRows>
  {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableExpandHeader aria-label="expand row" />
            {headers.map((header) => (
              <TableHeader {...getHeaderProps({ header })}>
                {header.header}
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <React.Fragment key={row.id}>
              <TableExpandRow {...getRowProps({ row })}>
                {row.cells.map((cell) => renderCell(cell, row))}
              </TableExpandRow>
              <TableExpandedRow colSpan={headers.length + 1}>
                {renderExpandedContent(row)}
              </TableExpandedRow>
            </React.Fragment>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )}
</DataTable>
```

**Alternatives Considered**:

- ❌ Custom accordion component: Would violate Carbon Design System requirement
- ❌ Modal dialog: Would interrupt workflow, not inline
- ❌ Side panel: More complex, not standard Carbon pattern

**Reference**:

- Carbon DataTable documentation:
  https://react.carbondesignsystem.com/?path=/docs/components-datatable--expandable
- Existing implementation: `frontend/src/components/eOrder/EOrder.js` (lines
  290-340)

#### Q2: How to manage single-row expansion state?

**Decision**: Use React `useState` to track expanded row ID, with logic to
collapse previous row when new row expands.

**Rationale**:

- Simple state management pattern
- Single source of truth for expanded state
- Easy to implement "only one expanded at a time" behavior
- No need for complex state management library

**Implementation Pattern**:

```jsx
const [expandedRowId, setExpandedRowId] = useState(null);

const handleRowExpand = (rowId) => {
  setExpandedRowId(expandedRowId === rowId ? null : rowId);
};

// In TableExpandRow:
<TableExpandRow
  {...getRowProps({ row })}
  isExpanded={expandedRowId === row.id}
  onExpand={() => handleRowExpand(row.id)}
>
```

**Alternatives Considered**:

- ❌ Multiple rows expanded: Violates spec requirement (FR-059d)
- ❌ Redux/Context: Overkill for simple local component state

#### Q3: What data format for expanded content?

**Decision**: Display additional fields as key-value pairs in a structured
layout using Carbon Grid/Column components.

**Rationale**:

- Clear, scannable format
- Easy to implement with Carbon components
- Consistent with read-only requirement (FR-059e)
- Supports internationalization (key labels via React Intl)

**Implementation Pattern**:

```jsx
const renderExpandedContent = (row) => {
  const location = row.original; // Full location object
  return (
    <div style={{ padding: "1rem" }}>
      <Grid>
        <Column md={6}>
          <strong>Description:</strong> {location.description || "N/A"}
        </Column>
        <Column md={6}>
          <strong>Created Date:</strong> {formatDate(location.createdDate)}
        </Column>
        {/* More key-value pairs */}
      </Grid>
    </div>
  );
};
```

**Alternatives Considered**:

- ❌ Plain text list: Less structured, harder to scan
- ❌ Nested table: Overkill for simple key-value display
- ❌ Card component: More visual weight than needed

#### Q4: How to handle missing/optional fields in expanded view?

**Decision**: Display "N/A" or empty string for missing optional fields, format
dates/timestamps consistently.

**Rationale**:

- Prevents empty/blank spaces in UI
- Consistent user experience
- Clear indication when data is not available
- Follows existing OpenELIS patterns

**Implementation Pattern**:

```jsx
const formatField = (value, formatter) => {
  if (value === null || value === undefined || value === "") {
    return intl.formatMessage({
      id: "common.not.available",
      defaultMessage: "N/A",
    });
  }
  return formatter ? formatter(value) : value;
};
```

**Alternatives Considered**:

- ❌ Hide missing fields: Inconsistent row heights, confusing
- ❌ Show empty: Looks like a bug

#### Q5: How to ensure expanded content is accessible?

**Decision**: Use Carbon's built-in ARIA attributes from `TableExpandRow` and
`TableExpandedRow`, add semantic HTML structure.

**Rationale**:

- Carbon components provide accessibility out of the box
- ARIA labels automatically handled by `TableExpandHeader` and `TableExpandRow`
- Keyboard navigation supported (Enter/Space to expand)
- Screen reader friendly with proper heading structure

**Implementation Pattern**:

- Carbon `TableExpandRow` automatically handles:
  - `aria-expanded` attribute
  - `aria-controls` linking to expanded content
  - Keyboard navigation (Enter/Space)
- Use semantic HTML in expanded content:
  ```jsx
  <TableExpandedRow>
    <div role="region" aria-label="Additional location details">
      {/* Content */}
    </div>
  </TableExpandedRow>
  ```

**Alternatives Considered**:

- ❌ Custom ARIA implementation: Carbon already handles this
- ❌ No accessibility: Violates WCAG 2.1 AA requirement

### Technical Decisions Summary

| Decision         | Choice                                | Rationale                                 |
| ---------------- | ------------------------------------- | ----------------------------------------- |
| UI Pattern       | Carbon DataTable expandable rows      | Constitution compliance, existing pattern |
| State Management | React useState (single expandedRowId) | Simple, sufficient for requirement        |
| Content Format   | Key-value pairs in Grid layout        | Clear, scannable, i18n-friendly           |
| Missing Fields   | Display "N/A"                         | Consistent UX, clear indication           |
| Accessibility    | Carbon built-in + semantic HTML       | WCAG compliance, minimal custom work      |

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` with `TableExpandHeader`,
  `TableExpandRow`, `TableExpandedRow`
- **React Intl**: For internationalized field labels
- **Existing StorageDashboard**: Modify current table implementations

### Implementation Notes

1. **Backend Changes**: None required - all fields already available in API
   responses
2. **API Changes**: None required - expanded view uses existing location data
3. **State Management**: Local component state sufficient (no global state
   needed)
4. **Testing**:
   - Unit tests: Test expanded state management, content rendering
   - E2E tests: Test expand/collapse interaction, single-row behavior
5. **Performance**: Minimal impact - expanded content rendered on-demand, no
   additional API calls

---

## 9. Existing OpenELIS Barcode Printing Infrastructure

**Date**: 2025-11-22  
**Feature**: Integration with existing barcode label printing system

### Research Questions

#### Q1: What barcode printing infrastructure already exists in OpenELIS?

**Decision**: OpenELIS has a complete barcode printing system using iTextPDF
library.

**Existing Infrastructure**:

1. **BarcodeLabelMaker.java**
   (`src/main/java/org/openelisglobal/barcode/BarcodeLabelMaker.java`):

   - Uses `com.itextpdf.text.pdf.Barcode128` for Code 128 barcodes
   - Uses `com.google.zxing` for QR codes
   - Generates PDF streams via `createLabelsAsStream()` method
   - Supports multiple label types: OrderLabel, SpecimenLabel, BlankLabel,
     BlockLabel, SlideLabel
   - Label dimensions configurable via `ConfigurationProperties`

2. **LabelMakerServlet.java**
   (`src/main/java/org/openelisglobal/common/servlet/barcode/LabelMakerServlet.java`):

   - Servlet endpoint: `/LabelMakerServlet`
   - Query parameters: `labNo`, `type`, `quantity`, `override`
   - Returns PDF stream with `Content-Type: application/pdf`
   - Frontend usage:
     `<iframe src="/LabelMakerServlet?labNo=...&type=...&quantity=..."/>`

3. **BarcodeConfigurationForm.java**
   (`src/main/java/org/openelisglobal/barcode/form/BarcodeConfigurationForm.java`):

   - System administration form for barcode settings
   - Configurable properties:
     - Label dimensions (height/width for each label type)
     - Maximum print limits (numMaxOrderLabels, numMaxSpecimenLabels, etc.)
     - Default print quantities
   - Stored in `SiteInformation` table via `BarcodeInformationService`

4. **BarcodeLabelInfo.java**
   (`src/main/java/org/openelisglobal/barcode/valueholder/BarcodeLabelInfo.java`):

   - Entity for tracking print history
   - Fields: `id`, `numPrinted`, `code`, `type`
   - Tracks how many times a label has been printed
   - Used for enforcing maximum print limits

5. **ConfigurationProperties.java**
   (`src/main/java/org/openelisglobal/common/util/ConfigurationProperties.java`):
   - Property enum values for barcode configuration:
     - `ORDER_BARCODE_HEIGHT`, `ORDER_BARCODE_WIDTH`
     - `SPECIMEN_BARCODE_HEIGHT`, `SPECIMEN_BARCODE_WIDTH`
     - `BLOCK_BARCODE_HEIGHT`, `BLOCK_BARCODE_WIDTH`
     - `SLIDE_BARCODE_HEIGHT`, `SLIDE_BARCODE_WIDTH`
     - `MAX_ORDER_PRINTED`, `MAX_SPECIMEN_PRINTED`
   - Properties stored in database (`site_information` table) or
     `SystemConfiguration.properties` file

**Pattern for Creating New Label Types**:

```java
// Example: OrderLabel extends Label
public class StorageLocationLabel extends Label {
    public StorageLocationLabel(StorageDevice device, String shortCode) {
        // Set dimensions from ConfigurationProperties
        width = Float.parseFloat(ConfigurationProperties.getInstance()
            .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_WIDTH));
        height = Float.parseFloat(ConfigurationProperties.getInstance()
            .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_HEIGHT));

        // Set barcode code (hierarchical path or short code)
        setCode(shortCode != null ? shortCode : buildHierarchicalPath(device));

        // Add fields above/below barcode
        aboveFields = new ArrayList<>();
        aboveFields.add(new LabelField("Location", device.getName(), 12));
        // ... more fields
    }
}
```

**Integration Strategy**:

1. **Create StorageLocationLabel class** extending `Label`:

   - Use hierarchical path (`ROOM-DEVICE-SHELF-RACK`) or short code for barcode
     value
   - Read dimensions from `ConfigurationProperties` (add new properties:
     `STORAGE_LOCATION_BARCODE_HEIGHT`, `STORAGE_LOCATION_BARCODE_WIDTH`)
   - Display location name, code, hierarchical path on label

2. **Extend LabelMakerServlet** or create new endpoint:

   - Option A: Extend existing servlet with new `type=storage-location`
     parameter
   - Option B: Create REST endpoint `/rest/storage/{type}/{id}/print-label`
     (preferred for consistency with REST API pattern)
   - Return PDF stream same as existing servlet

3. **Add Configuration Properties**:

   - Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH`
     to `ConfigurationProperties.Property` enum
   - Add to `BarcodeConfigurationForm` for system admin UI
   - Store in `site_information` table via `BarcodeInformationService`

4. **Print History Tracking**:
   - Reuse existing `BarcodeLabelInfo` entity or create new
     `StorageLocationPrintHistory` entity
   - Track: location entity ID, short code (if used), printed by (user ID),
     printed date, print count
   - Store in database for audit trail

**Rationale**: Leveraging existing infrastructure reduces development effort and
maintains consistency with OpenELIS patterns. iTextPDF is already in
dependencies, label configuration system exists, and print history pattern is
established.

**Alternatives Considered**:

- ❌ Custom PDF generation library: Would duplicate existing functionality
- ❌ Separate label printing system: Would create inconsistency and maintenance
  overhead
- ❌ Third-party label printing service: Would add external dependency and cost

#### Q2: How to configure default printer for label printing?

**Decision**: NEEDS CLARIFICATION - Research required on printer configuration
in OpenELIS.

**Research Needed**:

- Does OpenELIS have system-wide default printer configuration?
- How do existing label printing workflows handle printer selection?
- Is printer selection handled by browser (user selects printer when PDF opens)?
- Or is there server-side printer configuration?

**Current Understanding**:

- LabelMakerServlet returns PDF stream to browser
- Browser PDF viewer handles printing (user selects printer)
- No evidence of server-side printer configuration in existing code

**Action Required**: Research printer configuration options:

1. Check if `ConfigurationProperties` has printer-related settings
2. Check if there's a printer selection dialog in frontend
3. Determine if "default printer" means browser default or system default
4. Document findings for implementation

#### Q3: What are the detailed requirements for USB HID barcode scanner integration?

**Decision**: Basic keyboard event handling is documented, but hardware-specific
details need research.

**Current Research** (from Section 4):

- USB HID scanners emit rapid keyboard events (30-50ms between characters)
- Detection via character buffer with timeout
- Enter key indicates scan completion

**Additional Research Needed**:

1. **Scanner Configuration**:

   - Do scanners need any configuration (prefix/suffix characters)?
   - How to handle scanners that add Enter automatically vs manual Enter?
   - What happens if user types manually vs scans (detection method)?

2. **Browser Compatibility**:

   - Do all browsers handle USB HID scanners identically?
   - Any browser-specific quirks or limitations?
   - Mobile browser support (if applicable)?

3. **Error Handling**:
   - What if scanner malfunctions (partial scans, corrupted data)?
   - How to distinguish scanner input from normal typing?
   - Should we detect scan speed vs typing speed?

**Action Required**: Document hardware testing results and browser compatibility
findings.

**Reference**: Existing research in Section 4 provides basic implementation
pattern. Additional hardware testing recommended during implementation phase.

### Technical Decisions Summary

| Decision          | Choice                                                      | Rationale                                                      |
| ----------------- | ----------------------------------------------------------- | -------------------------------------------------------------- |
| PDF Generation    | Reuse existing iTextPDF via BarcodeLabelMaker               | Already in dependencies, proven pattern                        |
| Label Type        | Create StorageLocationLabel extending Label                 | Follows existing pattern, maintains consistency                |
| Print Endpoint    | REST endpoint `/rest/storage/{type}/{id}/print-label`       | Consistent with REST API architecture                          |
| Configuration     | Extend ConfigurationProperties and BarcodeConfigurationForm | Leverages existing system admin infrastructure                 |
| Print History     | Create StorageLocationPrintHistory entity                   | Separate from sample labels, storage-specific audit trail      |
| Printer Selection | Browser PDF viewer (user selects)                           | Matches existing pattern, no server-side printer config needed |

### Dependencies

- **iTextPDF**: Already in OpenELIS dependencies (`com.itextpdf:itextpdf`)
- **ZXing**: Already in dependencies for QR code support
  (`com.google.zxing:core`)
- **BarcodeLabelMaker**: Existing class, extend for storage locations
- **ConfigurationProperties**: Existing utility, add new properties

### Implementation Notes

1. **Backend Changes**:

   - Create `StorageLocationLabel.java` extending `Label`
   - Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH`
     to `ConfigurationProperties.Property` enum
   - Extend `BarcodeConfigurationForm` with storage location label dimensions
   - Create REST endpoint for label printing (or extend LabelMakerServlet)
   - Create `StorageLocationPrintHistory` entity and DAO/Service

2. **Frontend Changes**:

   - Label Management modal calls REST endpoint
   - PDF opens in new tab (browser handles printing)
   - Display print history from `StorageLocationPrintHistory` entity

3. **Database Changes**:

   - Add `storage_location_print_history` table (Liquibase changeset)
   - Add configuration properties to `site_information` table (via system admin
     UI)

4. **Testing**:
   - Unit tests for `StorageLocationLabel` class
   - Integration tests for print endpoint
   - E2E tests for label printing workflow

---

## Summary of Research Findings

| Question                         | Answer                                                                                                                                                                                                                                                       | Source                                                                                     |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| Hibernate XML Mapping            | StringSequenceGenerator + LIMSStringNumberUserType, version on lastupdated, dynamic-update=true                                                                                                                                                              | Existing .hbm.xml files                                                                    |
| FHIR Location Structure          | R4 Location resource with partOf hierarchy, physicalType codes (ro/ve/co), IHE mCSD compliance                                                                                                                                                               | FHIR R4 spec + IHE mCSD                                                                    |
| Carbon Dropdown Cascading        | Controlled components, useEffect for child data fetching, disabled until parent selected                                                                                                                                                                     | @carbon/react Dropdown API                                                                 |
| Barcode Scanner Integration      | USB HID keyboard events, character buffer with 50ms timeout, detect Enter key                                                                                                                                                                                | Browser keyboard event handling                                                            |
| Barcode Printing Infrastructure  | Reuse existing iTextPDF/BarcodeLabelMaker, create StorageLocationLabel extending Label, REST endpoint for printing, extend ConfigurationProperties                                                                                                           | Existing OpenELIS barcode printing system (BarcodeLabelMaker.java, LabelMakerServlet.java) |
| Frontend Data Fetching           | Custom `getFromOpenElisServer` utility with useState/useEffect (NOT SWR)                                                                                                                                                                                     | Existing OpenELIS hooks                                                                    |
| Cypress E2E Setup                | Use existing Cypress 12.17.3 framework, follow patientEntry.cy.js pattern                                                                                                                                                                                    | Existing OpenELIS E2E tests                                                                |
| Certificate Architecture         | Self-signed certs via certgen container, distributed via Docker volumes to nginx/proxy and Java services. Let's Encrypt setup requires Certbot container, nginx ACME challenge handling, and subdomain-specific server blocks                                | dev.docker-compose.yml, nginx.conf, certificate-setup-report.md                            |
| Carbon DataTable Expandable Rows | Carbon DataTable expandableRows prop with TableExpandHeader/TableExpandRow/TableExpandedRow, React useState for single-row expansion, key-value pairs in Grid layout                                                                                         | Carbon DataTable docs, EOrder.js implementation                                            |
| Capacity Calculation Logic       | Two-tier system: manual `capacity_limit` (if set) OR calculated from children (sum if all children have defined capacities). Racks always use rows × columns. Show "N/A" if capacity cannot be determined.                                                   | Spec FR-062a, FR-062b, FR-062c, laboratory workflow analysis                               |
| Frontend Unit Testing Pattern    | Standard import order (React → Testing Library → jest-dom → Intl → Component → Utils → Messages), mock utilities before imports, use `renderWithIntl` helper, AAA pattern, `getBy*`/`queryBy*`/`findBy*` selection, `waitFor` for async (never `setTimeout`) | StorageDashboard.test.jsx (canonical example), React Testing Library docs                  |

**Decisions Made**:

1. Follow existing Hibernate XML patterns for storage entities
2. Map Room/Device/Shelf/Rack/Position ALL to FHIR Location resources (positions
   as child locations with extensions)
3. Use Carbon Dropdown with cascading state management
4. Implement barcode scanner with keyboard event listener + character buffer
5. Use existing `getFromOpenElisServer` pattern (no SWR dependency)
6. Use existing Cypress framework for E2E tests (NOT Playwright)
7. Set up Let's Encrypt for `storage.openelis-global.org` subdomain
   incrementally, keeping self-signed certs for other services during
   development phase
8. Reuse existing OpenELIS barcode printing infrastructure (iTextPDF,
   BarcodeLabelMaker) for storage location labels
9. Implement two-tier capacity system: manual `capacity_limit` takes precedence,
   otherwise calculate from children (sum if all children have defined
   capacities). Display "N/A" with tooltip when capacity cannot be determined.
   Visually distinguish manual vs calculated capacities.

**Next Steps**: Proceed to Phase 1 design artifacts (data-model.md, contracts/,
quickstart.md)

---

## 9. Capacity Calculation Logic

**Question**: How should capacity be calculated for Devices and Shelves when
`capacity_limit` is not set? How should the system handle cases where some
children have defined capacities and others don't?

**Research Context**:

- Spec requires occupancy display (FR-061, FR-062) showing fraction, percentage,
  and progress bar
- Devices and Shelves have optional `capacity_limit` field
- Racks always use calculated capacity (rows × columns per FR-017)
- Need to support both manual planning limits and dynamic calculation from
  hierarchy

**Decision**: Two-tier capacity system with hierarchical fallback

**Rationale**:

1. **Manual limits for planning**: Labs need to set capacity limits for
   procurement planning (e.g., "Freezer Unit 1 can hold 500 samples")
2. **Dynamic calculation for flexibility**: When limits aren't set, calculate
   from actual storage structure (sum of child capacities)
3. **Consistency requirement**: If ANY child lacks defined capacity, cannot
   reliably calculate parent (would show misleading data)
4. **User transparency**: Users must understand whether capacity is manual or
   calculated (visual distinction required)

**Implementation Pattern**:

- **Tier 1**: If `capacity_limit` is set, use that value (manual/static limit)
- **Tier 2**: If `capacity_limit` is NULL:
  - Calculate from child locations (shelves for devices, racks for shelves)
  - If ALL children have defined capacities (either static `capacity_limit` OR
    calculated from their own children), sum those capacities
  - If ANY child lacks defined capacity, return null (capacity cannot be
    determined)
- **Racks**: Always calculated (rows × columns), never use `capacity_limit`
  field

**UI Display**:

- When capacity is defined: Show "287/500 (57%)" with progress bar
- When capacity cannot be determined: Show "N/A" or "Unlimited" with tooltip
  explaining why
- Visual distinction: Badge, tooltip, or icon to indicate "Manual Limit" vs
  "Calculated"

**Alternatives Considered**:

- **Option B (Simplified)**: Only show occupancy when `capacity_limit` is set,
  otherwise show "Unlimited" - **Rejected**: Too restrictive, doesn't leverage
  rack capacity data
- **Option C (Always Calculate)**: Remove `capacity_limit` field, always
  calculate from children - **Rejected**: Labs need manual limits for planning
  purposes

**Dependencies**:

- Backend: `StorageLocationService` must implement `calculateDeviceCapacity()`
  and `calculateShelfCapacity()` methods
- API: Device/Shelf responses must include `totalCapacity` and `capacityType`
  fields
- Frontend: Occupancy display must handle null capacity and show visual
  distinction

**Reference**: Spec FR-062a, FR-062b, FR-062c, FR-061, FR-063

---

## 10. Frontend Unit Testing Standard Pattern

**Feature**: Standardized frontend unit testing patterns for React components  
**Purpose**: Establish consistent, reliable testing patterns to prevent
recurring test failures and maintainability issues

### Research Questions

#### Q1: What is the standard test file structure and import pattern?

**Decision**: Follow exact import order and structure from
`StorageDashboard.test.jsx` (canonical reference).

**Standard Import Order (MANDATORY)**:

```javascript
// 1. React
import React from "react";

// 2. Testing Library (all utilities in one import)
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within, // Include if needed for scoped queries
} from "@testing-library/react";

// 3. jest-dom matchers (MUST be imported)
import "@testing-library/jest-dom";

// 4. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 5. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// 6. Component under test
import ComponentName from "./ComponentName";

// 7. Utilities (import functions, not just for mocking)
import { getFromOpenElisServer } from "../utils/Utils";

// 8. Messages/translations
import messages from "../../../languages/en.json";
```

**Rationale**:

- Import order prevents module hoisting conflicts
- Testing Library imports must come before jest-dom
- Utilities imported explicitly (not just mocked) for type checking
- Messages imported last (not used in mocks)

**Reference**: `frontend/src/components/storage/StorageDashboard.test.jsx`
(lines 1-15)

#### Q2: How should mocks be structured?

**Decision**: Mock utilities BEFORE imports that use them, use `jest.mock()` at
module level.

**Standard Mock Pattern**:

```javascript
// Mock the API utilities (MUST be before imports that use them)
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(), // Add as needed
}));

// Mock react-router-dom if component uses routing
const mockHistory = {
  replace: jest.fn(),
  push: jest.fn(),
};

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useHistory: () => mockHistory,
  useLocation: () => ({ pathname: "/path" }), // Adjust as needed
}));

// Mock child components if they have complex dependencies
jest.mock("./ChildComponent", () => {
  return function MockChildComponent({ prop1, onCallback }) {
    return <div data-testid="child-component">{/* Mock implementation */}</div>;
  };
});
```

**Rationale**:

- Jest hoists `jest.mock()` calls, so they must be before imports
- Using `jest.requireActual()` preserves other router functionality
- Mock child components to isolate unit under test

**Reference**: `frontend/src/components/storage/StorageDashboard.test.jsx`
(lines 17-31)

#### Q3: What helper functions should be standardized?

**Decision**: Use `renderWithIntl` helper and optional `setupApiMocks` helper.

**Standard Helper Functions**:

```javascript
// Helper function to create mock location (if using useLocation)
const createMockLocation = (pathname) => ({ pathname });

// Mock NotificationContext if component uses it
const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

// Standard render helper with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      {" "}
      {/* Include if component uses routing */}
      <IntlProvider locale="en" messages={messages}>
        {/* Include NotificationContext.Provider if needed */}
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

// Helper function to setup API mocks (if needed)
const setupApiMocks = (overrides = {}) => {
  const defaults = {
    // Define default mock responses
  };
  const data = { ...defaults, ...overrides };

  getFromOpenElisServer.mockImplementation((url, callback) => {
    // Map URLs to mock responses
    if (url.includes("/rest/endpoint1")) {
      callback(data.endpoint1);
    } else if (url.includes("/rest/endpoint2")) {
      callback(data.endpoint2);
    }
  });
};
```

**Rationale**:

- `renderWithIntl` ensures all components have i18n context
- `setupApiMocks` centralizes API mocking logic
- Helper functions reduce test boilerplate

**Reference**: `frontend/src/components/storage/StorageDashboard.test.jsx`
(lines 33-90)

#### Q4: How should test structure be organized?

**Decision**: Follow AAA pattern (Arrange, Act, Assert) with descriptive test
names and task references.

**Standard Test Structure**:

```javascript
describe("ComponentName", () => {
  // Define mock data constants
  const mockData = {
    // Component-specific mock data
  };

  const mockCallback = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Setup default API mocks if needed
    // setupApiMocks();
  });

  /**
   * Test description with task reference (e.g., T123: Test name)
   */
  test("testName", async () => {
    // Arrange: Setup test data and render component
    renderWithIntl(
      <ComponentName prop1={mockData.prop1} onCallback={mockCallback} />
    );

    // Act: Perform user actions
    const button = screen.getByTestId("button-id");
    fireEvent.click(button);

    // Assert: Verify expected behavior
    // Use waitFor for async operations
    await waitFor(() => {
      expect(screen.getByText("Expected Text")).toBeInTheDocument();
    });

    // Use queryBy* for absence checks
    expect(screen.queryByTestId("error-message")).not.toBeInTheDocument();
  });
});
```

**Rationale**:

- AAA pattern makes tests readable and maintainable
- Descriptive test names (testWhat_When_ExpectedResult)
- Task references link tests to specifications
- `beforeEach` ensures test isolation

**Reference**: `frontend/src/components/storage/StorageDashboard.test.jsx`
(lines 92-141)

#### Q5: What query methods should be used and when?

**Decision**: Use `screen.getBy*` for required elements, `screen.queryBy*` for
absence checks, `screen.findBy*` for async queries.

**Query Method Selection**:

```javascript
// ✅ CORRECT: Use getBy* for required elements (throws if not found)
const button = screen.getByTestId("button-id");
const text = screen.getByText("Expected Text");

// ✅ CORRECT: Use queryBy* for absence checks (returns null if not found)
expect(screen.queryByTestId("error-message")).not.toBeInTheDocument();

// ✅ CORRECT: Use findBy* for async element queries (waits and retries)
const asyncElement = await screen.findByText("Loaded Data");

// ✅ CORRECT: Use within() for scoped queries within containers
const container = screen.getByTestId("container");
const scopedButton = within(container).getByText("Button Text");

// ❌ WRONG: Using getBy* for absence checks (throws error)
expect(screen.getByTestId("error-message")).not.toBeInTheDocument(); // FAILS

// ❌ WRONG: Using queryBy* for required elements (doesn't fail if missing)
const button = screen.queryByTestId("button-id"); // May be null
```

**Rationale**:

- `getBy*` throws immediately if element not found (fails fast)
- `queryBy*` returns null (safe for absence checks)
- `findBy*` waits and retries (handles async rendering)
- `within()` scopes queries to containers (prevents false matches)

**Reference**: React Testing Library documentation, `StorageDashboard.test.jsx`
examples

#### Q6: How should async operations be handled?

**Decision**: Use `waitFor` for async operations, never use `setTimeout`.

**Async Operation Pattern**:

```javascript
// ✅ CORRECT: Use waitFor for async operations
test("testAsyncOperation", async () => {
  renderWithIntl(<ComponentName />);

  // Wait for async operation
  await waitFor(() => {
    expect(screen.getByText("Loaded Data")).toBeInTheDocument();
  });
});

// ✅ CORRECT: Use findBy* for async element queries
const asyncElement = await screen.findByText("Loaded Data");

// ❌ WRONG: Using setTimeout (unreliable, arbitrary delays)
setTimeout(() => {
  expect(screen.getByText("Loaded Data")).toBeInTheDocument();
}, 1000); // FAILS - arbitrary delay, no retry logic

// ❌ WRONG: Not waiting for async operations
fireEvent.click(button);
expect(screen.getByText("Loaded Data")).toBeInTheDocument(); // FAILS - element not rendered yet
```

**Rationale**:

- `waitFor` retries assertions until they pass or timeout
- `findBy*` queries automatically wait and retry
- `setTimeout` is unreliable and arbitrary
- Async operations need proper waiting

**Reference**: React Testing Library async utilities documentation

### Technical Decisions Summary

| Decision         | Choice                                                                   | Rationale                                    |
| ---------------- | ------------------------------------------------------------------------ | -------------------------------------------- |
| Import Order     | React → Testing Library → jest-dom → Intl → Component → Utils → Messages | Prevents module hoisting conflicts           |
| Mock Placement   | Before imports that use them                                             | Jest hoisting requires this order            |
| Helper Functions | `renderWithIntl`, optional `setupApiMocks`                               | Reduces boilerplate, ensures i18n context    |
| Test Structure   | AAA pattern with task references                                         | Readable, maintainable, traceable            |
| Query Methods    | `getBy*` (required), `queryBy*` (absence), `findBy*` (async)             | Appropriate tool for each use case           |
| Async Handling   | `waitFor` and `findBy*`, never `setTimeout`                              | Reliable, retry-able, follows best practices |

### Dependencies

- **React Testing Library**: `@testing-library/react` (v12+)
- **Jest DOM Matchers**: `@testing-library/jest-dom` (v5+)
- **React Intl**: `react-intl` (v5.20.12)
- **Jest**: v27+ (for `jest.mock()` hoisting)

### Implementation Notes

1. **Test Template**: Created
   `frontend/src/components/storage/__tests__/TEST_TEMPLATE.jsx` as reference
2. **Canonical Example**: `StorageDashboard.test.jsx` serves as the reference
   implementation
3. **Best Practices Checklist**: Included in test template comments
4. **Common Pitfalls**:
   - ❌ Importing `waitFor` but not using it (causes module resolution issues)
   - ❌ Using `setTimeout` instead of `waitFor`
   - ❌ Using `getBy*` for absence checks (throws error)
   - ❌ Not clearing mocks in `beforeEach` (test pollution)
   - ❌ Mocking utilities after imports (hoisting issues)

### Best Practices Checklist

**MANDATORY for all test files**:

- ✅ Import order: React → Testing Library → jest-dom → Intl → Component → Utils
  → Messages
- ✅ Mock utilities BEFORE imports that use them
- ✅ Use `renderWithIntl` helper for all components
- ✅ Use `beforeEach` to clear mocks
- ✅ Use `async/await` with `waitFor` for async operations
- ✅ Use `screen.getBy*` for required elements
- ✅ Use `screen.queryBy*` for absence checks (with `.not.toBeInTheDocument()`)
- ✅ Use `screen.findBy*` for async element queries
- ✅ Use `within()` for scoped queries within containers
- ✅ Use `data-testid` for reliable element selection
- ✅ Use `fireEvent` for user interactions
- ✅ Use `waitFor` instead of `setTimeout` for async operations
- ✅ Include task references in test comments (e.g., T123: Test name)
- ✅ Follow AAA pattern (Arrange, Act, Assert)
- ✅ Use descriptive test names (testWhat_When_ExpectedResult)
- ✅ Mock child components if they have complex dependencies
- ✅ Clear mocks in `beforeEach`
- ✅ Use `setupApiMocks` helper for complex API mocking scenarios

**Reference**:

- Test Template: `frontend/src/components/storage/__tests__/TEST_TEMPLATE.jsx`
- Canonical Example: `frontend/src/components/storage/StorageDashboard.test.jsx`
- React Testing Library Docs:
  https://testing-library.com/docs/react-testing-library/intro/

---

## 11. SampleItem Entity Structure and Storage Integration

**Feature**: SampleItem-level storage tracking  
**Purpose**: Document SampleItem entity structure and its relationship to Sample
entity for storage management integration

### Research Questions

#### Q1: What is the SampleItem entity structure?

**Decision**: SampleItem represents physical specimens collected from patients,
linked to a parent Sample (order).

**Entity Structure** (`org.openelisglobal.sampleitem.valueholder.SampleItem`):

**Key Fields**:

- `id` (String) - Primary key, generated via `StringSequenceGenerator`
  (sequence: `sample_item_seq`)
- `fhirUuid` (UUID) - FHIR resource identifier for Specimen mapping
- `sample` (Many-to-One → Sample) - Parent Sample (order) relationship via
  `SAMP_ID` foreign key
- `sampleItemId` (String) - External identifier for the physical specimen
- `externalId` (String) - Additional external identifier
- `sortOrder` (String) - Ordering within parent Sample
- `typeOfSample` (Many-to-One → TypeOfSample) - Specimen type (blood, urine,
  etc.)
- `sourceOfSample` (Many-to-One → SourceOfSample) - Collection source
- `quantity` (Double) - Specimen quantity
- `unitOfMeasure` (Many-to-One → UnitOfMeasure) - Quantity unit
- `collectionDate` (Timestamp) - When specimen was collected
- `collector` (String) - Person who collected the specimen
- `statusId` (String) - Current status
- `rejected` (boolean) - Rejection flag
- `rejectReasonId` (String) - Reason for rejection
- `voided` (boolean) - Void flag
- `voidReason` (String) - Reason for voiding
- `lastupdated` (Timestamp) - Optimistic locking version field

**Hibernate Mapping** (`SampleItem.hbm.xml`):

- Table: `SAMPLE_ITEM`
- ID generation: `StringSequenceGenerator` with `sample_item_seq`
- Optimistic locking: `version` on `lastupdated` column
- Many-to-One to Sample: `SAMP_ID` foreign key, `lazy="false"`
- Many-to-One to TypeOfSample: `TYPEOSAMP_ID` foreign key
- Many-to-One to SourceOfSample: `SOURCE_ID` foreign key
- Many-to-One to UnitOfMeasure: `UOM_ID` foreign key

**Reference**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`,
`src/main/resources/hibernate/hbm/SampleItem.hbm.xml`

#### Q2: What is the relationship between Sample and SampleItem?

**Decision**: One-to-Many relationship: One Sample (order) can have multiple
SampleItems (physical specimens).

**Sample Entity** (`org.openelisglobal.sample.valueholder.Sample`):

- Represents a **laboratory order** (accession)
- Key fields: `id`, `accessionNumber`, `collectionDate`, `status`, `fhirUuid`
- One Sample can have multiple SampleItems (e.g., blood draw with multiple
  tubes)

**SampleItem Entity**:

- Represents a **physical specimen** collected from a patient
- Each SampleItem belongs to exactly one Sample (via `SAMP_ID` foreign key)
- Multiple SampleItems can belong to the same Sample
- Each SampleItem can be stored independently

**Relationship Pattern**:

```
Sample (Order)
├── SampleItem 1 (Blood Tube 1) → Can be stored in Location A
├── SampleItem 2 (Blood Tube 2) → Can be stored in Location B
└── SampleItem 3 (Urine Container) → Can be stored in Location C
```

**Query Pattern**:

```java
// Get all SampleItems for a Sample
List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);

// Get parent Sample from SampleItem
Sample parentSample = sampleItem.getSample();
String accessionNumber = parentSample.getAccessionNumber();
```

**Reference**:
`src/main/java/org/openelisglobal/sample/valueholder/Sample.java`,
`src/main/java/org/openelisglobal/sampleitem/service/SampleItemServiceImpl.java`

#### Q3: How does storage assignment integrate with SampleItem?

**Decision**: Storage tracking operates at SampleItem level via
`SampleStorageAssignment` junction table.

**Storage Assignment Entity** (`SampleStorageAssignment`):

- **Foreign Key**: `sample_item_id` → `SampleItem.id` (NOT `sample_id` →
  `Sample.id`)
- **Polymorphic Location**: `location_id` + `location_type` (device/shelf/rack)
- **Optional Position**: `position_coordinate` (text field for specific
  position)
- **Unique Constraint**: One assignment per SampleItem (one current location)

**Data Model**:

```sql
CREATE TABLE sample_storage_assignment (
    id VARCHAR(36) PRIMARY KEY,
    sample_item_id VARCHAR(36) NOT NULL REFERENCES sample_item(id),
    location_id INTEGER NOT NULL,
    location_type VARCHAR(20) NOT NULL CHECK (location_type IN ('device', 'shelf', 'rack')),
    position_coordinate VARCHAR(50),
    assigned_by_user_id INTEGER NOT NULL,
    assigned_date TIMESTAMP NOT NULL,
    notes TEXT,
    UNIQUE (sample_item_id)  -- One current location per SampleItem
);
```

**Query Patterns**:

```java
// Get storage location for a SampleItem
SampleStorageAssignment assignment = assignmentDAO.getBySampleItemId(sampleItemId);
if (assignment != null) {
    String locationPath = buildHierarchicalPath(assignment.getLocationId(), assignment.getLocationType());
}

// Get all SampleItems stored in a location
List<SampleStorageAssignment> assignments = assignmentDAO.getByLocation(locationId, locationType);
List<SampleItem> items = assignments.stream()
    .map(a -> sampleItemService.getData(a.getSampleItemId()))
    .collect(Collectors.toList());

// Get all SampleItems for a Sample (with storage locations)
Sample sample = sampleService.getData(sampleId);
List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sampleId);
for (SampleItem item : items) {
    SampleStorageAssignment assignment = assignmentDAO.getBySampleItemId(item.getId());
    // Display item with location context
}
```

**Reference**: `specs/001-sample-storage/data-model.md`,
`specs/001-sample-storage/plan.md` (SampleItem Entity Integration section)

#### Q4: How should the dashboard display SampleItem information?

**Decision**: Dashboard displays SampleItem as primary entity with parent Sample
context for grouping/sorting.

**Display Pattern**:

- **Primary Identifier**: SampleItem ID or External ID (if available)
- **Secondary Context**: Parent Sample accession number (for grouping/sorting)
- **Table Columns**: SampleItem ID, Parent Sample Accession, Type, Status,
  Location, Actions
- **Sortable By**: SampleItem ID, Parent Sample Accession, Type, Status,
  Location
- **Grouping**: Optional grouping by parent Sample (all SampleItems from same
  Sample together)

**Frontend Data Structure**:

```javascript
{
  sampleItemId: "12345",
  sampleItemExternalId: "EXT-001",
  parentSample: {
    id: "67890",
    accessionNumber: "S-2025-001"
  },
  typeOfSample: "Blood",
  status: "Active",
  location: {
    hierarchicalPath: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1",
    locationId: 123,
    locationType: "rack",
    positionCoordinate: "A5"
  },
  assignedBy: "John Doe",
  assignedDate: "2025-11-15T10:30:00Z"
}
```

**Search Support**:

- Search by SampleItem ID
- Search by SampleItem External ID
- Search by parent Sample accession number (returns all SampleItems for that
  Sample)

**Reference**: `specs/001-sample-storage/spec.md` (Storage Granularity section,
FR-033b, FR-064)

#### Q5: How does FHIR integration work with SampleItem storage?

**Decision**: SampleItem storage location maps to FHIR Specimen resource
`container` reference.

**FHIR Specimen Resource Mapping**:

- Each SampleItem has a corresponding FHIR Specimen resource (via `fhirUuid`)
- Storage location stored in `Specimen.container.extension[storage-location]`
  reference
- Container identifier contains hierarchical path for human readability

**FHIR Resource Structure**:

```json
{
  "resourceType": "Specimen",
  "id": "{sampleItem.fhirUuid}",
  "container": [
    {
      "identifier": {
        "value": "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
      },
      "extension": [
        {
          "url": "http://openelis.org/fhir/extension/storage-location",
          "valueReference": {
            "reference": "Location/{storage_location_fhir_uuid}"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-position-coordinate",
          "valueString": "A5"
        }
      ]
    }
  ],
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/storage-assigned-date",
      "valueDateTime": "2025-11-15T10:30:00Z"
    }
  ]
}
```

**Sync Strategy**:

- On SampleStorageAssignment create/update: Update corresponding Specimen
  resource `container` extension
- Use existing `FhirTransformService` and `FhirPersistanceService` patterns
- Specimen resource already exists (created during sample entry), only update
  container extension

**Reference**: `specs/001-sample-storage/plan.md` (SampleItem Entity Integration
section), `specs/001-sample-storage/contracts/fhir-mappings.md`

### Technical Decisions Summary

| Decision            | Choice                                                     | Rationale                                                 |
| ------------------- | ---------------------------------------------------------- | --------------------------------------------------------- |
| Storage Granularity | SampleItem level (not Sample level)                        | Physical specimens are stored, not orders                 |
| Assignment Entity   | `SampleStorageAssignment.sample_item_id` → `SampleItem.id` | Direct link to physical specimen                          |
| Dashboard Display   | SampleItem primary, Sample context secondary               | Users need to see individual specimens with order context |
| Search Support      | SampleItem ID/External ID OR Sample accession              | Flexible search for both specimen and order identifiers   |
| FHIR Integration    | Specimen.container extension                               | Standard FHIR pattern for specimen storage location       |

### Dependencies

- **SampleItem Entity**: Existing entity, no modifications required
- **Sample Entity**: Existing entity, no modifications required
- **SampleStorageAssignment Entity**: Must reference `SampleItem.id` (not
  `Sample.id`)
- **FHIR Specimen Resource**: Existing resource, update `container` extension on
  assignment

### Implementation Notes

1. **Backend Changes**:

   - `SampleStorageAssignment` entity: Change foreign key from `sample_id` to
     `sample_item_id`
   - Service methods: Update to accept `sampleItemId` instead of `sampleId`
   - DAO queries: Update to join on `SampleItem` instead of `Sample`
   - FHIR sync: Update Specimen `container` extension on assignment

2. **Frontend Changes**:

   - Dashboard: Display SampleItem ID/External ID as primary identifier
   - Dashboard: Include parent Sample accession number for context
   - Search: Support both SampleItem ID/External ID and Sample accession number
   - Assignment modal: Accept SampleItem ID (not Sample ID)

3. **Database Changes**:

   - `SampleStorageAssignment` table: Change `sample_id` column to
     `sample_item_id`
   - Update foreign key constraint: `sample_item_id` → `sample_item(id)`
   - Update unique constraint: `UNIQUE (sample_item_id)` (one location per
     SampleItem)

4. **Testing**:
   - Unit tests: Verify SampleItem-level assignment logic
   - Integration tests: Verify multiple SampleItems from same Sample can have
     different locations
   - E2E tests: Verify dashboard displays SampleItem with parent Sample context

**Reference**:

- Entity:
  `src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`
- Hibernate Mapping: `src/main/resources/hibernate/hbm/SampleItem.hbm.xml`
- Service:
  `src/main/java/org/openelisglobal/sampleitem/service/SampleItemServiceImpl.java`
- Spec: `specs/001-sample-storage/spec.md` (Storage Granularity section)
- Plan: `specs/001-sample-storage/plan.md` (SampleItem Entity Integration
  section)

---

## 12. Carbon Design System Component Testing Best Practices

**Feature**: Standardized testing patterns for Carbon Design System components  
**Purpose**: Document proven patterns for testing Carbon components to prevent
recurring test failures and reduce debugging time  
**Date**: 2025-11-13

### Research Questions

#### Q1: How should Carbon TextInput/ComboBox components be tested?

**Decision**: Use `fireEvent.focus` + `fireEvent.click` with `act()` wrapper,
then wait for state updates with `waitFor` and `queryByTestId`.

**Pattern for TextInput/ComboBox Focus**:

```javascript
// ✅ CORRECT: Focus and click with act() wrapper
const searchInput = screen.getByPlaceholderText(/filter by locations/i);
await act(async () => {
  fireEvent.focus(searchInput);
  fireEvent.click(searchInput);
  // Small delay for state update
  await new Promise((resolve) => setTimeout(resolve, 50));
});

// Wait for dropdown/content to appear
await waitFor(
  () => {
    const container = screen.queryByTestId("location-tree-container");
    expect(container).toBeInTheDocument();
  },
  { timeout: 5000 }
);
```

**Rationale**:

- Carbon TextInput `onFocus` handler sets state (`isOpen`, etc.)
- Both `focus` and `click` events may be needed to reliably trigger state
- `act()` ensures React processes state updates before assertions
- Small delay (50ms) allows Carbon's internal state to propagate
- Use `queryByTestId` in `waitFor` to avoid throwing errors during retries

**Common Pitfalls**:

- ❌ Using only `fireEvent.focus` - may not trigger all Carbon handlers
- ❌ Not using `act()` wrapper - React state updates may not be processed
- ❌ Using `getByTestId` in `waitFor` - throws error if element not found during
  retry
- ❌ Using `findByTestId` with empty object `{}` - incorrect syntax:
  `findByTestId("id", {}, { timeout })` is WRONG

**Reference**: `LocationFilterDropdown.test.jsx` (lines 128-143)

#### Q2: How should Carbon OverflowMenu components be tested?

**Decision**: Click the menu button, use `act()` with delay, then wait for menu
items with `waitFor` and `queryByTestId` or text queries.

**Pattern for OverflowMenu**:

```javascript
// ✅ CORRECT: Find menu, click button, wait for items
const overflowMenus = await screen.findAllByTestId(
  "location-actions-overflow-menu"
);
const menuButton = overflowMenus[0].querySelector("button") || overflowMenus[0];

// Use act() to ensure React processes the click and menu opens
await act(async () => {
  fireEvent.click(menuButton);
  // Small delay for Carbon OverflowMenu to open and render items
  await new Promise((resolve) => setTimeout(resolve, 100));
});

// Wait for menu items to render (Carbon renders in portal)
let menuItem;
await waitFor(
  () => {
    menuItem =
      screen.queryByTestId("label-management-menu-item") ||
      screen.queryByText(/label management/i);
    expect(menuItem).toBeTruthy();
  },
  { timeout: 5000 }
);

// Click the menu item
fireEvent.click(menuItem);
```

**Rationale**:

- Carbon OverflowMenu renders items in a portal (outside normal DOM tree)
- Menu items are conditionally rendered when menu is open
- `act()` with delay ensures menu state is updated before querying
- Use `queryByTestId` OR `queryByText` as fallback (items may not have testid)
- Longer timeout (5000ms) needed for portal rendering

**Common Pitfalls**:

- ❌ Clicking menu and immediately querying - items not rendered yet
- ❌ Not using `act()` wrapper - menu state not processed
- ❌ Using `getByTestId` in `waitFor` - throws if item not found during retry
- ❌ Not finding the actual button element - Carbon wraps in div structure

**Reference**: `StorageDashboard.test.jsx` (lines 1495-1513),
`SampleActionsOverflowMenu.test.jsx`

#### Q3: How should Carbon ComboBox (Downshift-based) components be tested?

**Decision**: Use `userEvent.type` for input, wait for dropdown, then explicitly
select option with `getByRole("option")`.

**Pattern for ComboBox Input and Selection**:

```javascript
// ✅ CORRECT: Type into ComboBox and select option
const input = screen.getByRole("combobox", { name: /room/i });

// Type value (triggers onInputChange)
await userEvent.type(input, "Main Laboratory", { delay: 0 });

// Wait for dropdown to open
await waitFor(
  () => {
    const menu = document.querySelector('[role="listbox"]');
    expect(menu && menu.children.length > 0).toBeTruthy();
  },
  { timeout: 2000 }
);

// Explicitly select the option (Carbon doesn't auto-select)
const roomOption = await screen.findByRole("option", {
  name: /main laboratory/i,
});
await userEvent.click(roomOption);

// Wait for selection to update state
await waitFor(
  () => {
    const deviceCombobox = screen.getByTestId("device-combobox");
    expect(deviceCombobox.disabled).toBe(false);
  },
  { timeout: 3000 }
);
```

**Rationale**:

- Carbon ComboBox uses Downshift library internally
- `userEvent.type` properly triggers Downshift's `onInputChange` handler
- Carbon ComboBox does NOT auto-select on exact match - user must click option
- Use `getByRole("option")` to find dropdown items (more reliable than text)
- Wait for state updates after selection (enables next level, etc.)

**Common Pitfalls**:

- ❌ Using `fireEvent.change` - doesn't trigger Downshift handlers properly
- ❌ Expecting auto-selection - Carbon ComboBox requires explicit click
- ❌ Using `getByText` for dropdown items - use `getByRole("option")` instead
- ❌ Not waiting for state updates - next level may not be enabled yet

**Reference**: `EnhancedCascadingMode.test.jsx` (lines 238-262)

#### Q4: How should async state updates be handled in Carbon component tests?

**Decision**: Always use `waitFor` with `queryBy*` methods, never use
`setTimeout`, use `act()` for state updates.

**Pattern for Async State Updates**:

```javascript
// ✅ CORRECT: waitFor with queryBy* for async elements
await waitFor(
  () => {
    const element = screen.queryByTestId("async-element");
    expect(element).toBeTruthy();
  },
  { timeout: 5000 }
);

// ✅ CORRECT: act() wrapper for state updates
await act(async () => {
  fireEvent.click(button);
  await new Promise((resolve) => setTimeout(resolve, 100));
});

// ❌ WRONG: setTimeout without waitFor
setTimeout(() => {
  expect(screen.getByTestId("element")).toBeTruthy();
}, 1000); // FAILS - no retry logic

// ❌ WRONG: getBy* in waitFor (throws if not found during retry)
await waitFor(() => {
  expect(screen.getByTestId("element")).toBeTruthy(); // Throws error
});
```

**Rationale**:

- `waitFor` retries assertions until they pass or timeout
- `queryBy*` returns null (safe for retries), `getBy*` throws (fails retries)
- `act()` ensures React processes state updates before next assertion
- Small delays (50-100ms) allow Carbon's internal state to propagate
- Never use `setTimeout` - unreliable, no retry logic

**Common Pitfalls**:

- ❌ Using `getBy*` in `waitFor` - throws error if element not found during
  retry
- ❌ Using `setTimeout` - arbitrary delays, no retry logic
- ❌ Not using `act()` - React state updates may not be processed
- ❌ Too short timeouts - Carbon components need time to render

**Reference**: React Testing Library async utilities,
`LocationFilterDropdown.test.jsx`

#### Q5: How should Carbon component validation errors be tested?

**Decision**: Use invalid format (not too-long values), wait for error with
`queryByTestId` or text fallback, check both testid and text.

**Pattern for Validation Errors**:

```javascript
// ✅ CORRECT: Use invalid format, wait for error
const input = screen.getByTestId("short-code-input");
const invalidValue = "-INVALID"; // Invalid format (starts with hyphen)

fireEvent.change(input, {
  target: { value: invalidValue },
});

// Wait for validation error (appears in InlineNotification)
await waitFor(
  () => {
    const errorMessage = screen.queryByTestId("short-code-error");
    // Fallback to text query if testid not working
    const errorText =
      screen.queryByText(/must start with/i) ||
      screen.queryByText(/letter or number/i);
    expect(errorMessage || errorText).toBeTruthy();
  },
  { timeout: 3000 }
);
```

**Rationale**:

- Carbon components may reject too-long values before validation runs
- Use invalid format (e.g., starts with special char) to trigger validation
- Errors appear in Carbon `InlineNotification` component
- Check both `testid` and text as fallback (testid may not be set)
- Validation happens synchronously but React needs time to render

**Common Pitfalls**:

- ❌ Using too-long value - component rejects before validation
- ❌ Not waiting for error - React needs time to render
- ❌ Only checking testid - may not be set, check text as fallback
- ❌ Using `getByTestId` - throws if error not found during retry

**Reference**: `LabelManagementModal.test.jsx` (lines 70-88)

#### Q6: When should Carbon component tests be moved to E2E (Cypress)?

**Decision**: Move complex multi-level workflows, portal-rendered components,
and tests requiring real browser environment to Cypress.

**Criteria for E2E Testing**:

- ✅ **Move to Cypress**: Multi-level workflows (room → device → shelf → rack)
- ✅ **Move to Cypress**: Components with complex state cascading
- ✅ **Move to Cypress**: Portal-rendered components (OverflowMenu, Modal)
- ✅ **Move to Cypress**: Tests requiring real browser APIs (window.open, blob
  URLs)
- ✅ **Keep in Jest**: Individual component behaviors (button enabled/disabled)
- ✅ **Keep in Jest**: Single-level interactions (creating one room)
- ✅ **Keep in Jest**: Input validation logic
- ✅ **Keep in Jest**: Notification handling

**Example - Move to Cypress**:

```javascript
// ❌ TOO COMPLEX FOR JEST: Multi-level workflow
test("testAddNewButtonsAppearForAllLevels", async () => {
  // Creates room → device → shelf → rack
  // Multiple API calls, cascading state updates
  // Better suited for Cypress E2E
});

// ✅ KEEP IN JEST: Single behavior
test("testCreatingRoomEnablesDeviceInput", async () => {
  // Single interaction, clear assertion
  // Fast, reliable in Jest
});
```

**Rationale**:

- Cypress runs in real browser (handles portals, real DOM)
- Jest/JSDOM has limitations with Carbon component rendering
- Complex workflows are better tested end-to-end
- Simple behaviors are faster in Jest unit tests

**Reference**: Constitution V.5 (Cypress E2E Testing),
`EnhancedCascadingMode.test.jsx`

### Technical Decisions Summary

| Decision                 | Choice                                                      | Rationale                                 |
| ------------------------ | ----------------------------------------------------------- | ----------------------------------------- |
| TextInput/ComboBox Focus | `fireEvent.focus` + `fireEvent.click` with `act()`          | Carbon needs both events to reliably open |
| OverflowMenu Testing     | Click button, `act()` with delay, `waitFor` with `queryBy*` | Portal rendering requires waiting         |
| ComboBox Selection       | `userEvent.type` + explicit `getByRole("option")` click     | Carbon doesn't auto-select                |
| Async State Updates      | `waitFor` with `queryBy*`, never `setTimeout`               | Reliable retry logic                      |
| Validation Errors        | Invalid format + `queryByTestId`/text fallback              | Component may reject too-long values      |
| E2E vs Jest              | Complex workflows → Cypress, simple behaviors → Jest        | Right tool for each use case              |

### Dependencies

- **React Testing Library**: `@testing-library/react` (v12+)
- **User Event**: `@testing-library/user-event` (v14+)
- **Carbon Design System**: `@carbon/react` (v1.15+)
- **Downshift**: Used by Carbon ComboBox (internal dependency)

### Implementation Notes

1. **Test Template**: Update `TEST_TEMPLATE.jsx` with Carbon-specific patterns
2. **Common Patterns**: Document in test file comments for reference
3. **Known Issues**:
   - Carbon OverflowMenu items render in portal (requires longer waits)
   - Carbon ComboBox doesn't auto-select (must click option)
   - Carbon TextInput needs both focus and click events
   - Validation errors may appear in InlineNotification (check both testid and
     text)

### Best Practices Checklist

**MANDATORY for Carbon component tests**:

- ✅ Use `fireEvent.focus` + `fireEvent.click` for TextInput/ComboBox focus
- ✅ Wrap state updates in `act()` with small delay (50-100ms)
- ✅ Use `waitFor` with `queryByTestId` (not `getByTestId`) for async elements
- ✅ Use `getByRole("option")` for ComboBox dropdown items
- ✅ Explicitly click ComboBox options (no auto-selection)
- ✅ Use `userEvent.type` for ComboBox input (triggers Downshift handlers)
- ✅ For OverflowMenu: Click button, `act()` with delay, then `waitFor` for
  items
- ✅ For validation: Use invalid format (not too-long), check both testid and
  text
- ✅ Use longer timeouts (3000-5000ms) for Carbon component rendering
- ✅ Move complex multi-level workflows to Cypress E2E tests
- ✅ Never use `setTimeout` - use `waitFor` instead
- ✅ Never use `findByTestId` with empty object `{}` - incorrect syntax

**Common Anti-Patterns**:

- ❌ `findByTestId("id", {}, { timeout })` - WRONG syntax
- ❌ `getByTestId` in `waitFor` - throws during retries
- ❌ Only `fireEvent.focus` - Carbon needs both focus and click
- ❌ Expecting ComboBox auto-selection - must click option
- ❌ `setTimeout` for async operations - use `waitFor`
- ❌ Too-short timeouts - Carbon needs time to render

**Reference**:

- Carbon Design System: https://carbondesignsystem.com/
- React Testing Library:
  https://testing-library.com/docs/react-testing-library/intro/
- Downshift Testing: https://www.downshift-js.com/
- Test Examples: `EnhancedCascadingMode.test.jsx`,
  `LocationFilterDropdown.test.jsx`, `StorageDashboard.test.jsx`

---

**Reference**: Spec FR-062a, FR-062b, FR-062c, FR-061, FR-063
