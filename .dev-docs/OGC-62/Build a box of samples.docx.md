OpenELIS   
Design/Requirements   
**Change/Feature Name :**	**Shipping Manifest and Features**

Date : **1/12/2022**

Target Release:  **OE v.3.1.3.1**

**Status: DRAFT**

Contents  
[Background	1](#heading=h.gjdgxs)

[User Stories	1](#heading=h.30j0zll)

[Business Rules:	1](#heading=h.1fob9te)

[Admin main menu:	2](#heading=h.4d34og8)

[Manage Users menu	2](#heading=h.3znysh7)

[User Account screen	2](#heading=h.2s8eyo1)

[User Roles:	2](#heading=h.17dp8vu)

[Localization	3](#localization)

[Considerations	3](#considerations)

[Traceability	3](#traceability)

[Signatures	3](#signatures)

# **Functional Specification: Sample Box Management System**

## **1\. Overview**

The system enables reception and lab staff to **build, track, and manage boxes of samples** sent to reference laboratories. It covers creation, labeling, tracking, reporting, and reconciliation of sample boxes.

---

## **2\. Core User Roles**

* **Reception Worker**: Creates and manages boxes, generates manifests, prints labels.

* **Lab Technician (Receiving)**: Scans boxes upon arrival, checks in samples, records non-conformities.

* **Administrator**: Defines label prefixes, manages reference lab destinations, sets business rules.

---

## **3\. UI Elements**

### **3.1 Box Management Dashboard**

* **Search/Scan Box ID input** (manual entry \+ barcode scanner input).

* **Create New Box** button.

* **List of Active/Unsent Boxes** with status indicators (Draft, Ready to Send, Sent, Received).

* **Filters**: Date range, destination, status.

### **3.2 Create/Edit Box Screen**

* **Box ID field** (auto-generated or scan existing).

* **Label Prefix dropdown** (configurable by admin).

* **Destination dropdown** (from facility registry).

* **Add Samples section**:

  * Manual entry of accession \# / sample ID.

  * Barcode scan input.

  * Table showing current samples in box.

* **Save Draft** button.

* **Mark as Ready to Send** button (with warning modal).

* **Print Label** button.

* **Generate Packing List / Manifest** button (printable & electronic).

### **3.3 Box Details / Manifest View**

* Box ID, destination, created by, date/time.

* Table of samples:

  * Accession \# / Sample ID.

  * Status (Pending, Scanned, Received).

  * Non-conformity checkbox \+ notes.

* Buttons: **Print Manifest**, **Resend Manifest**, **Regenerate Packing List**.

### **3.4 Receiving Screen**

* **Scan Box ID** input.

* List of samples with tick boxes.

* **Scan Sample IDs** → auto-checks off entries.

* Manual override checkbox if sample barcode cannot be scanned.

* Non-conformity recording: per-sample and “Apply to All” option.

### **3.5 Reports Module**

* **Report Filters**:

  * By Box ID.

  * By Date Range.

  * By Destination.

  * By Reference Lab.

* **Columns**: Box ID, contents, sent date/time, sender, received status, receiving user.

* Export options: **PDF, Excel**.

### **3.6 Administration Panel**

* Manage Label Prefixes.

* Manage Facility Registry (reference labs).

* Manage Users (link to OpenELIS menu).

* Configure FHIR mapping options (supply delivery, specimen container types).

---

## **4\. User Stories & Acceptance Criteria**

1. **Create a Box**

   * *As a reception worker, I want to create a box with a unique ID so that I can start adding samples.*

   * **Acceptance Criteria**:

     * System generates or accepts scanned Box ID.

     * User can assign destination from facility registry.

     * Box saved as “Draft” until sent.

2. **Add Samples to a Box**

   * *As a reception worker, I want to add samples to a box by scanning or typing IDs so that I can build a manifest.*

   * **Acceptance Criteria**:

     * Each sample appears in a list with status \= Pending.

     * Duplicate sample IDs rejected.

     * Box can be saved without sending.

3. **Print Label & Manifest**

   * *As a worker, I want to print a label with Box ID and manifest so the box can be tracked.*

   * **Acceptance Criteria**:

     * Label uses configurable prefix.

     * Manifest includes Box ID, sample list, user, date/time.

4. **Send a Box**

   * *As a worker, I want to mark a box as sent so it can be tracked at the reference lab.*

   * **Acceptance Criteria**:

     * Warning modal displayed before final confirmation.

     * Box status changes to “Sent.”

     * Packing list generated (with barcoded specimen IDs).

5. **Receive a Box**

   * *As a lab technician, I want to scan a box and reconcile samples so I know what arrived.*

   * **Acceptance Criteria**:

     * Box status updates to “Received.”

     * Scanning a sample marks it as received.

     * Manual override available.

     * Non-conformities can be logged.

6. **Reporting**

   * *As a user, I want to generate reports by Box ID, date, destination, or lab so I can track shipments.*

   * **Acceptance Criteria**:

     * Report includes box metadata and sample details.

     * Export available in PDF/Excel.

---

## **5\. Business Rules**

1. **Box ID Management**

   * Must be unique.

   * Can be auto-generated or manually scanned.

   * Prefix configurable by admin.

2. **Sample Management**

   * Each sample ID can belong to only one active box at a time.

   * Duplicates not allowed.

   * Supports accession number or “.1” suffix sample IDs.

3. **Packing List Requirements**

   * Must be barcoded.

   * Must include service location and temperature designation.

   * Can be resent electronically across the interface.

   * Regeneration allowed up to 24 hours; recall allowed within 7 days.

4. **Send Workflow**

   * Warning modal before sending is mandatory.

   * Once sent, a packing list is generated and locked (except for allowed resends/regeneration).

5. **Receiving Workflow**

   * Sample must be reconciled via scan or manual check.

   * Non-conformity must be recorded if marked.

6. **FHIR Alignment**

   * Use `SupplyDelivery.status` for box state (in-progress, completed, abandoned, etc.).

   * Use `Specimen.container.type` for sample container classification.

   * Non-conformities can map to SNOMED CT codes as needed.

7. **Audit Logging**

   * All box creation, sample additions, send/receive events, and non-conformities must be timestamped with user ID.

---

## **6\. Non-Functional Requirements**

* **Performance**: Box creation and scanning must respond within \<1 second.

* **Security**: User authentication required (integrates with OpenELIS user management).

* **Auditability**: Full history of box/sample events maintained for compliance.

* **Scalability**: System must support 10,000+ samples/day.

---

# Localization {#localization}

| English | French |
| :---- | :---- |
| Too Many, please see  |  |
|  |  |

# Considerations {#considerations}

## Traceability {#traceability}

## Signatures {#signatures}

| Role | Signature | Date |
| :---- | :---- | :---- |
| Stakeholder |  |  |
| Feature Lead **Casey**  |  |  |
| Dev |  |  |

