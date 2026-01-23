# Data Model: Medical Laboratory Workflow

**Feature**: 001-medical-lab-workflow **Date**: 2024-12-14 **Purpose**: Document
entity relationships and database schema

## Entity Relationship Diagram

```
┌───────────────────────────────────────────────────────────────────────────────────────┐
│                           MEDICAL LABORATORY WORKFLOW                                   │
└───────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────┐
                                    │   Patient   │
                                    │  (EXISTING) │
                                    └──────┬──────┘
                                           │ 1
                                           │
                                           │ *
                                    ┌──────▼──────┐
                                    │  LabOrder   │
                                    │  (EXISTING) │
                                    └──────┬──────┘
                                           │ 1
                                           │
                                           │ *
                         ┌─────────────────┼─────────────────┐
                         │                 │                 │
                         ▼                 ▼                 ▼
                  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
                  │ SampleItem  │   │ SampleItem  │   │ SampleItem  │
                  │  (EXTEND)   │   │  (EXTEND)   │   │  (EXTEND)   │
                  └──────┬──────┘   └─────────────┘   └─────────────┘
                         │
        ┌────────────────┼────────────────┬───────────────────┐
        │                │                │                   │
        ▼                ▼                ▼                   ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ QualityCheck  │ │ Processing    │ │ SampleStorage │ │ DisposalRecord│
│    (NEW)      │ │   Record      │ │  Assignment   │ │    (NEW)      │
│               │ │    (NEW)      │ │  (EXISTING)   │ │               │
└───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘

┌───────────────────────────────────────────────────────────────────────────────────────┐
│                              TRANSPORT PACKAGING                                        │
└───────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────┐
│        TransportPackaging           │
│              (NEW)                  │
├─────────────────────────────────────┤
│ - shipment_id                       │
│ - primary_container_type            │
│ - primary_seal_status               │
│ - primary_barcode_present           │
│ - primary_absorbent_present         │
│ - secondary_packaging_type          │
│ - secondary_integrity               │
│ - secondary_watertight              │
│ - secondary_container_count         │
│ - secondary_inspector_id            │
│ - secondary_inspection_time         │
│ - secondary_receipt_condition       │
│ - tertiary_box_type                 │
│ - tertiary_labeling_status          │
│ - tertiary_temperature_logger_id    │
│ - tertiary_courier_company          │
│ - tertiary_tracking_number          │
│ - tertiary_arrival_condition        │
│ - iata_pi650_compliant              │
│ - transportation_status             │
└─────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────────┐
│                           ENVIRONMENTAL MONITORING                                      │
└───────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐         ┌─────────────────────────┐
│    StorageLocation      │ 1     * │  EnvironmentalReading   │
│      (EXISTING)         │─────────│         (NEW)           │
├─────────────────────────┤         ├─────────────────────────┤
│ - id                    │         │ - id                    │
│ - name                  │         │ - location_id (FK)      │
│ - location_type         │         │ - temperature           │
│ - parent_id             │         │ - reading_type (AM/PM)  │
│                         │         │ - reading_date          │
│                         │         │ - reading_time          │
│                         │         │ - recorded_by (FK)      │
│                         │         │ - is_excursion          │
│                         │         │ - excursion_reason      │
└─────────────────────────┘         └─────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────────┐
│                              QUALITY CONTROL                                            │
└───────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐         ┌─────────────────────────┐
│        Test             │ 1     * │      QCResult           │
│      (EXISTING)         │─────────│        (NEW)            │
├─────────────────────────┤         ├─────────────────────────┤
│ - id                    │         │ - id                    │
│ - name                  │         │ - test_id (FK)          │
│ - unit                  │         │ - qc_level (NORMAL/PATH)│
│                         │         │ - lot_number            │
│                         │         │ - expiry_date           │
│                         │         │ - target_value          │
│                         │         │ - acceptable_range_low  │
│                         │         │ - acceptable_range_high │
│                         │         │ - result_value          │
│                         │         │ - result_date           │
│                         │         │ - performed_by (FK)     │
│                         │         │ - pass_fail             │
│                         │         │ - westgard_rule_violated│
│                         │         │ - corrective_action     │
└─────────────────────────┘         └─────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────────┐
│                           RESULT VALIDATION                                             │
└───────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐         ┌─────────────────────────┐
│        Result           │ 1     1 │   ValidationRecord      │
│      (EXISTING)         │─────────│        (NEW)            │
├─────────────────────────┤         ├─────────────────────────┤
│ - id                    │         │ - id                    │
│ - value                 │         │ - result_id (FK)        │
│ - test_id               │         │ - department_id         │
│ - sample_item_id        │         │ - action (APPROVE/      │
│                         │         │          REJECT/RETEST) │
│                         │         │ - action_reason         │
│                         │         │ - actioned_by (FK)      │
│                         │         │ - action_time           │
│                         │         │ - previous_value        │
│                         │         │ - modified_value        │
└─────────────────────────┘         └─────────────────────────┘
```

## Entity Definitions

### 1. QualityCheck (NEW)

**Purpose**: Track quality control checks performed during sample reception

| Column             | Type         | Constraints   | Description                         |
| ------------------ | ------------ | ------------- | ----------------------------------- |
| id                 | INTEGER      | PK, AUTO      | Primary key                         |
| sample_item_id     | INTEGER      | FK, NOT NULL  | Reference to sample_item            |
| check_date         | TIMESTAMP    | NOT NULL      | When check was performed            |
| checked_by         | INTEGER      | FK, NOT NULL  | User who performed check            |
| sample_type        | VARCHAR(50)  | NOT NULL      | Type of sample (Chemistry, etc.)    |
| delay_minutes      | INTEGER      | NULL          | Minutes since collection            |
| delay_passed       | BOOLEAN      | DEFAULT TRUE  | Within acceptable delay             |
| volume_ml          | DECIMAL(5,2) | NULL          | Volume in milliliters               |
| volume_passed      | BOOLEAN      | DEFAULT TRUE  | Volume sufficient                   |
| hemolysis          | BOOLEAN      | DEFAULT FALSE | Hemolysis present (Chemistry)       |
| lipemia            | BOOLEAN      | DEFAULT FALSE | Lipemia present (Chemistry)         |
| icterus            | BOOLEAN      | DEFAULT FALSE | Icterus present (Chemistry)         |
| clotting           | BOOLEAN      | DEFAULT FALSE | Clotting present (Hematology)       |
| anticoagulant_type | VARCHAR(20)  | NULL          | EDTA, citrate, heparin, fluoride    |
| contamination      | BOOLEAN      | DEFAULT FALSE | Visible contamination               |
| container_proper   | BOOLEAN      | DEFAULT TRUE  | Proper container used               |
| leakage            | BOOLEAN      | DEFAULT FALSE | Sample leaked                       |
| overall_status     | VARCHAR(10)  | NOT NULL      | ACCEPTED or REJECTED                |
| rejection_reason   | VARCHAR(255) | NULL          | Reason if rejected                  |
| corrective_action  | VARCHAR(50)  | NULL          | RECOLLECTION or RETURN_TO_SUBMITTER |
| fhir_uuid          | UUID         | UNIQUE        | FHIR resource identifier            |
| lastupdated        | TIMESTAMP    | NOT NULL      | Audit timestamp                     |

**Indexes**:

- `idx_qc_sample_item` ON (sample_item_id)
- `idx_qc_status` ON (overall_status)
- `idx_qc_date` ON (check_date)

### 2. TransportPackaging (NEW)

**Purpose**: Track IATA PI650 compliant packaging for sample transport

| Column                      | Type         | Constraints  | Description                    |
| --------------------------- | ------------ | ------------ | ------------------------------ |
| id                          | INTEGER      | PK, AUTO     | Primary key                    |
| shipment_id                 | VARCHAR(50)  | NOT NULL     | Unique shipment identifier     |
| primary_container_type      | VARCHAR(50)  | NOT NULL     | vacutainer, cryovial, etc.     |
| primary_seal_status         | VARCHAR(20)  | NOT NULL     | intact, leaking, damaged       |
| primary_barcode_present     | BOOLEAN      | NOT NULL     | Barcode/ID applied             |
| primary_absorbent_present   | BOOLEAN      | NOT NULL     | Absorbent material present     |
| secondary_packaging_type    | VARCHAR(50)  | NOT NULL     | biohazard bag, canister, etc.  |
| secondary_integrity         | VARCHAR(20)  | NOT NULL     | intact, torn, wet, leaking     |
| secondary_watertight        | BOOLEAN      | NOT NULL     | Watertight/pressure-resistant  |
| secondary_container_count   | INTEGER      | NOT NULL     | Number of primary containers   |
| secondary_inspector_id      | INTEGER      | FK, NOT NULL | Inspector user ID              |
| secondary_inspection_time   | TIMESTAMP    | NOT NULL     | Inspection timestamp           |
| secondary_receipt_condition | VARCHAR(20)  | NOT NULL     | acceptable, rejected           |
| tertiary_box_type           | VARCHAR(50)  | NOT NULL     | insulated shipper, rigid box   |
| tertiary_labeling_status    | VARCHAR(100) | NOT NULL     | biohazard, arrows, sender/recv |
| tertiary_temp_logger_id     | VARCHAR(50)  | NULL         | Temperature logger ID if used  |
| tertiary_courier_company    | VARCHAR(100) | NULL         | Courier company name           |
| tertiary_tracking_number    | VARCHAR(50)  | NULL         | Tracking number                |
| tertiary_arrival_condition  | VARCHAR(20)  | NOT NULL     | intact, damaged                |
| iata_pi650_compliant        | BOOLEAN      | NOT NULL     | Overall compliance status      |
| transportation_status       | VARCHAR(20)  | NOT NULL     | on_time, delayed               |
| collection_temperature      | VARCHAR(20)  | NULL         | room_temp, frozen, na          |
| received_date               | TIMESTAMP    | NOT NULL     | When shipment was received     |
| fhir_uuid                   | UUID         | UNIQUE       | FHIR resource identifier       |
| lastupdated                 | TIMESTAMP    | NOT NULL     | Audit timestamp                |

**Indexes**:

- `idx_transport_shipment` ON (shipment_id)
- `idx_transport_received` ON (received_date)

### 3. EnvironmentalReading (NEW)

**Purpose**: Track temperature readings for storage locations

| Column           | Type         | Constraints   | Description                    |
| ---------------- | ------------ | ------------- | ------------------------------ |
| id               | INTEGER      | PK, AUTO      | Primary key                    |
| location_id      | INTEGER      | FK, NOT NULL  | Reference to storage_location  |
| temperature      | DECIMAL(5,2) | NOT NULL      | Temperature in Celsius         |
| reading_type     | VARCHAR(10)  | NOT NULL      | AM or PM                       |
| reading_date     | DATE         | NOT NULL      | Date of reading                |
| reading_time     | TIME         | NOT NULL      | Time of reading                |
| recorded_by      | INTEGER      | FK, NOT NULL  | User who recorded              |
| is_excursion     | BOOLEAN      | DEFAULT FALSE | Temperature out of range       |
| excursion_reason | VARCHAR(255) | NULL          | Reason for excursion           |
| min_acceptable   | DECIMAL(5,2) | NOT NULL      | Minimum acceptable temperature |
| max_acceptable   | DECIMAL(5,2) | NOT NULL      | Maximum acceptable temperature |
| data_logger_id   | VARCHAR(50)  | NULL          | External logger ID if used     |
| notes            | TEXT         | NULL          | Additional notes               |
| fhir_uuid        | UUID         | UNIQUE        | FHIR resource identifier       |
| lastupdated      | TIMESTAMP    | NOT NULL      | Audit timestamp                |

**Indexes**:

- `idx_env_location_date` ON (location_id, reading_date)
- `idx_env_excursion` ON (is_excursion)

### 4. ProcessingRecord (NEW)

**Purpose**: Track sample processing steps and aliquot creation

| Column              | Type         | Constraints  | Description                   |
| ------------------- | ------------ | ------------ | ----------------------------- |
| id                  | INTEGER      | PK, AUTO     | Primary key                   |
| sample_item_id      | INTEGER      | FK, NOT NULL | Reference to sample_item      |
| processing_type     | VARCHAR(50)  | NOT NULL     | centrifugation, smear, etc.   |
| processing_method   | VARCHAR(100) | NULL         | Specific method details       |
| stain_used          | VARCHAR(50)  | NULL         | Wright, Giemsa, Gram, etc.    |
| start_time          | TIMESTAMP    | NOT NULL     | Processing start time         |
| end_time            | TIMESTAMP    | NULL         | Processing end time           |
| performed_by        | INTEGER      | FK, NOT NULL | User who performed processing |
| parent_sample_id    | INTEGER      | FK, NULL     | Parent sample if aliquot      |
| aliquot_sequence    | INTEGER      | NULL         | Aliquot number (1, 2, 3...)   |
| derived_sample_type | VARCHAR(50)  | NULL         | serum, sediment, smear, etc.  |
| output_container    | VARCHAR(50)  | NULL         | tube, slide, plate, vial      |
| notes               | TEXT         | NULL         | Processing notes              |
| fhir_uuid           | UUID         | UNIQUE       | FHIR resource identifier      |
| lastupdated         | TIMESTAMP    | NOT NULL     | Audit timestamp               |

**Indexes**:

- `idx_proc_sample` ON (sample_item_id)
- `idx_proc_parent` ON (parent_sample_id)

### 5. QCResult (NEW)

**Purpose**: Track quality control results for Levey-Jennings charting

| Column                 | Type          | Constraints   | Description                  |
| ---------------------- | ------------- | ------------- | ---------------------------- |
| id                     | INTEGER       | PK, AUTO      | Primary key                  |
| test_id                | INTEGER       | FK, NOT NULL  | Reference to test definition |
| analyzer_id            | INTEGER       | FK, NULL      | Reference to analyzer        |
| qc_level               | VARCHAR(20)   | NOT NULL      | NORMAL or PATHOLOGIC         |
| lot_number             | VARCHAR(50)   | NOT NULL      | Control lot number           |
| expiry_date            | DATE          | NULL          | Control expiry date          |
| target_value           | DECIMAL(10,4) | NOT NULL      | Expected target value        |
| sd_value               | DECIMAL(10,4) | NOT NULL      | Standard deviation           |
| acceptable_range_low   | DECIMAL(10,4) | NOT NULL      | Lower acceptable limit       |
| acceptable_range_high  | DECIMAL(10,4) | NOT NULL      | Upper acceptable limit       |
| result_value           | DECIMAL(10,4) | NOT NULL      | Actual result value          |
| result_date            | DATE          | NOT NULL      | Date of QC run               |
| result_time            | TIME          | NOT NULL      | Time of QC run               |
| performed_by           | INTEGER       | FK, NOT NULL  | User who performed QC        |
| pass_fail              | VARCHAR(10)   | NOT NULL      | PASS or FAIL                 |
| westgard_rule_violated | VARCHAR(20)   | NULL          | 1:2s, 1:3s, 2:2s, R:4s, etc. |
| corrective_action      | VARCHAR(255)  | NULL          | Action taken if failed       |
| action_taken_by        | INTEGER       | FK, NULL      | User who took action         |
| action_date            | TIMESTAMP     | NULL          | When action was taken        |
| is_calibration         | BOOLEAN       | DEFAULT FALSE | Is this a calibration record |
| fhir_uuid              | UUID          | UNIQUE        | FHIR resource identifier     |
| lastupdated            | TIMESTAMP     | NOT NULL      | Audit timestamp              |

**Indexes**:

- `idx_qc_test_date` ON (test_id, result_date)
- `idx_qc_passfail` ON (pass_fail)
- `idx_qc_lot` ON (lot_number)

### 6. ValidationRecord (NEW)

**Purpose**: Track result validation and approval workflow

| Column         | Type          | Constraints  | Description                     |
| -------------- | ------------- | ------------ | ------------------------------- |
| id             | INTEGER       | PK, AUTO     | Primary key                     |
| result_id      | INTEGER       | FK, NOT NULL | Reference to result             |
| department_id  | INTEGER       | FK, NOT NULL | Department for approval         |
| action         | VARCHAR(20)   | NOT NULL     | APPROVED, REJECTED, RETEST      |
| action_reason  | VARCHAR(255)  | NULL         | Reason for action               |
| actioned_by    | INTEGER       | FK, NOT NULL | User who took action            |
| action_time    | TIMESTAMP     | NOT NULL     | When action was taken           |
| previous_value | VARCHAR(100)  | NULL         | Value before modification       |
| modified_value | VARCHAR(100)  | NULL         | New value if modified           |
| reference_low  | DECIMAL(10,4) | NULL         | Reference range low             |
| reference_high | DECIMAL(10,4) | NULL         | Reference range high            |
| result_flag    | VARCHAR(10)   | NULL         | H (high), L (low), C (critical) |
| fhir_uuid      | UUID          | UNIQUE       | FHIR resource identifier        |
| lastupdated    | TIMESTAMP     | NOT NULL     | Audit timestamp                 |

**Indexes**:

- `idx_val_result` ON (result_id)
- `idx_val_department_action` ON (department_id, action)
- `idx_val_time` ON (action_time)

### 7. DisposalRecord (NEW)

**Purpose**: Track sample disposal with compliance documentation

| Column                 | Type         | Constraints  | Description                         |
| ---------------------- | ------------ | ------------ | ----------------------------------- |
| id                     | INTEGER      | PK, AUTO     | Primary key                         |
| sample_item_id         | INTEGER      | FK, NOT NULL | Reference to sample_item            |
| disposal_date          | DATE         | NOT NULL     | Date of disposal                    |
| disposal_time          | TIME         | NOT NULL     | Time of disposal                    |
| disposal_reason        | VARCHAR(50)  | NOT NULL     | expiry, exhaustion, qc_fail, safety |
| disposal_method        | VARCHAR(50)  | NOT NULL     | incineration, chemical_treatment    |
| facility_name          | VARCHAR(200) | NOT NULL     | Disposal facility name              |
| facility_license       | VARCHAR(100) | NOT NULL     | Facility license number             |
| facility_accreditation | VARCHAR(100) | NULL         | Accreditation details               |
| disposed_by            | INTEGER      | FK, NOT NULL | User who performed disposal         |
| verified_by            | INTEGER      | FK, NULL     | User who verified disposal          |
| notes                  | TEXT         | NULL         | Additional notes                    |
| fhir_uuid              | UUID         | UNIQUE       | FHIR resource identifier            |
| lastupdated            | TIMESTAMP    | NOT NULL     | Audit timestamp                     |

**Indexes**:

- `idx_disposal_sample` ON (sample_item_id)
- `idx_disposal_date` ON (disposal_date)
- `idx_disposal_method` ON (disposal_method)

### 8. EquipmentUsageLog (NEW)

**Purpose**: Track laboratory equipment/instrument usage

| Column            | Type         | Constraints   | Description                    |
| ----------------- | ------------ | ------------- | ------------------------------ |
| id                | INTEGER      | PK, AUTO      | Primary key                    |
| analyzer_id       | INTEGER      | FK, NOT NULL  | Reference to analyzer          |
| usage_date        | DATE         | NOT NULL      | Date of usage                  |
| start_time        | TIME         | NOT NULL      | When equipment started         |
| end_time          | TIME         | NULL          | When equipment stopped         |
| operator_id       | INTEGER      | FK, NOT NULL  | User operating equipment       |
| purpose           | VARCHAR(100) | NOT NULL      | Testing, calibration, QC, etc. |
| samples_processed | INTEGER      | NULL          | Number of samples processed    |
| maintenance_done  | BOOLEAN      | DEFAULT FALSE | Maintenance performed          |
| maintenance_notes | TEXT         | NULL          | Maintenance details            |
| error_occurred    | BOOLEAN      | DEFAULT FALSE | Error during usage             |
| error_description | TEXT         | NULL          | Error details                  |
| fhir_uuid         | UUID         | UNIQUE        | FHIR resource identifier       |
| lastupdated       | TIMESTAMP    | NOT NULL      | Audit timestamp                |

**Indexes**:

- `idx_equip_analyzer_date` ON (analyzer_id, usage_date)
- `idx_equip_operator` ON (operator_id)

### 9. SampleAllocation (NEW)

**Purpose**: Track sample allocation to laboratory departments

| Column         | Type        | Constraints       | Description                    |
| -------------- | ----------- | ----------------- | ------------------------------ |
| id             | INTEGER     | PK, AUTO          | Primary key                    |
| sample_item_id | INTEGER     | FK, NOT NULL      | Reference to sample_item       |
| department     | VARCHAR(50) | NOT NULL          | hematology, chemistry, etc.    |
| allocated_date | TIMESTAMP   | NOT NULL          | When allocated                 |
| allocated_by   | INTEGER     | FK, NOT NULL      | User who allocated             |
| status         | VARCHAR(20) | NOT NULL          | pending, in_progress, complete |
| priority       | VARCHAR(20) | DEFAULT 'ROUTINE' | STAT, ROUTINE                  |
| notes          | TEXT        | NULL              | Allocation notes               |
| fhir_uuid      | UUID        | UNIQUE            | FHIR resource identifier       |
| lastupdated    | TIMESTAMP   | NOT NULL          | Audit timestamp                |

**Indexes**:

- `idx_alloc_sample` ON (sample_item_id)
- `idx_alloc_dept_status` ON (department, status)

## Extended Existing Entities

### SampleItem (EXTEND)

Add the following columns to existing sample_item table:

| Column           | Type        | Description                     |
| ---------------- | ----------- | ------------------------------- |
| sample_category  | VARCHAR(20) | patient, participant            |
| collection_temp  | VARCHAR(20) | room_temp, refrigerated, frozen |
| medlab_status    | VARCHAR(30) | collected, received, processed  |
| parent_sample_id | INTEGER FK  | For aliquot relationships       |

### Patient (EXTEND)

Add the following columns for participant support:

| Column            | Type        | Description                    |
| ----------------- | ----------- | ------------------------------ |
| is_participant    | BOOLEAN     | Research participant flag      |
| study_id          | VARCHAR(50) | Study enrollment ID            |
| enrollment_status | VARCHAR(20) | enrolled, withdrawn, completed |
| enrollment_date   | DATE        | When enrolled in study         |

## Liquibase Changeset Order

1. `001-quality-check.xml` - QualityCheck table
2. `002-transport-packaging.xml` - TransportPackaging table
3. `003-environmental-reading.xml` - EnvironmentalReading table
4. `004-processing-record.xml` - ProcessingRecord table
5. `005-qc-result.xml` - QCResult table
6. `006-validation-record.xml` - ValidationRecord table
7. `007-disposal-record.xml` - DisposalRecord table
8. `008-equipment-usage-log.xml` - EquipmentUsageLog table
9. `009-sample-allocation.xml` - SampleAllocation table
10. `010-sample-item-medlab-columns.xml` - Extend SampleItem
11. `011-patient-participant-columns.xml` - Extend Patient

## FHIR Resource Mappings

| Entity           | FHIR Resource     | Notes                       |
| ---------------- | ----------------- | --------------------------- |
| Patient          | Patient           | Existing mapping            |
| SampleItem       | Specimen          | Existing mapping, extend    |
| QualityCheck     | Observation       | New mapping, profile needed |
| QCResult         | Observation       | QC observation profile      |
| ValidationRecord | DiagnosticReport  | Extend existing mapping     |
| DisposalRecord   | Specimen (status) | Update specimen status      |
