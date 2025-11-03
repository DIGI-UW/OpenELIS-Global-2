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

**Cypress Configuration** (existing):

```javascript
// cypress.config.js
const { defineConfig } = require("cypress");

module.exports = defineConfig({
  defaultCommandTimeout: 8000,
  viewportWidth: 1200,
  viewportHeight: 700,
  video: false,
  watchForFileChanges: false,
  e2e: {
    baseUrl: "https://localhost",
    testIsolation: false,
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
```

**Test Structure** (follow existing pattern):

```
frontend/cypress/e2e/
├── storageAssignment.cy.js (P1 - Storage Assignment)
├── storageSearch.cy.js (P2A - Sample Search/Retrieval)
└── storageMovement.cy.js (P2B - Sample Movement, including bulk)
```

**Example Test Pattern** (based on existing patientEntry.cy.js):

```javascript
// storageAssignment.cy.js
import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let sampleEntryPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Sample Storage Assignment (P1)", function () {
  it("User navigates to Sample Entry Page", () => {
    homePage = loginPage.goToHomePage();
    sampleEntryPage = homePage.goToSampleEntry();
  });

  it("Should assign sample using cascading dropdowns", function () {
    // Complete sample accessioning
    sampleEntryPage.getAccessionNumberInput().type("S-2025-001");
    // ... fill other sample fields ...

    // Open Storage Location Selector
    cy.get('[data-testid="storage-location-selector"]').should("be.visible");

    // Select room
    cy.get('[data-testid="room-dropdown"]').click();
    cy.contains("Main Laboratory").click();

    // Select device
    cy.get('[data-testid="device-dropdown"]').should("not.be.disabled");
    cy.get('[data-testid="device-dropdown"]').click();
    cy.contains("Freezer Unit 1").click();

    // Select shelf
    cy.get('[data-testid="shelf-dropdown"]').should("not.be.disabled");
    cy.get('[data-testid="shelf-dropdown"]').click();
    cy.contains("Shelf-A").click();

    // Select rack
    cy.get('[data-testid="rack-dropdown"]').should("not.be.disabled");
    cy.get('[data-testid="rack-dropdown"]').click();
    cy.contains("Rack R1").click();

    // Enter position
    cy.get('[data-testid="position-input"]').type("A5");

    // Verify hierarchical path display
    cy.get('[data-testid="location-path"]').should(
      "contain.text",
      "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
    );

    // Save assignment
    cy.get('[data-testid="save-button"]').click();

    // Verify success notification
    cy.get('div[role="status"]').should("be.visible");
  });
});
```

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

**Run Commands**:

```bash
# Run all Cypress tests
npm run cy:run

# Run specific test file
npx cypress run --spec "cypress/e2e/storageAssignment.cy.js"

# Open Cypress UI for development
npx cypress open

# Run headed mode (see browser)
npx cypress run --headed
```

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

## Summary of Research Findings

| Question                    | Answer                                                                                                                                                                                                                        | Source                                                          |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Hibernate XML Mapping       | StringSequenceGenerator + LIMSStringNumberUserType, version on lastupdated, dynamic-update=true                                                                                                                               | Existing .hbm.xml files                                         |
| FHIR Location Structure     | R4 Location resource with partOf hierarchy, physicalType codes (ro/ve/co), IHE mCSD compliance                                                                                                                                | FHIR R4 spec + IHE mCSD                                         |
| Carbon Dropdown Cascading   | Controlled components, useEffect for child data fetching, disabled until parent selected                                                                                                                                      | @carbon/react Dropdown API                                      |
| Barcode Scanner Integration | USB HID keyboard events, character buffer with 50ms timeout, detect Enter key                                                                                                                                                 | Browser keyboard event handling                                 |
| Frontend Data Fetching      | Custom `getFromOpenElisServer` utility with useState/useEffect (NOT SWR)                                                                                                                                                      | Existing OpenELIS hooks                                         |
| Cypress E2E Setup           | Use existing Cypress 12.17.3 framework, follow patientEntry.cy.js pattern                                                                                                                                                     | Existing OpenELIS E2E tests                                     |
| Certificate Architecture    | Self-signed certs via certgen container, distributed via Docker volumes to nginx/proxy and Java services. Let's Encrypt setup requires Certbot container, nginx ACME challenge handling, and subdomain-specific server blocks | dev.docker-compose.yml, nginx.conf, certificate-setup-report.md |

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

**Next Steps**: Proceed to Phase 1 design artifacts (data-model.md, contracts/,
quickstart.md)
