# Pathology Sample Import CSV Template

This document describes the CSV format for importing samples into the Pathology
Laboratory module.

## Field Requirements

### MANDATORY FIELDS (All Samples)

| Field                | Description                                    | Format                                                              | Example             |
| -------------------- | ---------------------------------------------- | ------------------------------------------------------------------- | ------------------- |
| `firstName`          | **MANDATORY** - Patient/participant first name | Text                                                                | John                |
| `sampleCategory`     | Sample category                                | "Clinical diagnostic" or "Research"                                 | Clinical diagnostic |
| `receivedDateTime`   | Date and time sample was received at lab       | YYYY-MM-DD HH:MM                                                    | 2025-01-15 09:30    |
| `receivedBy`         | Name of receiving staff member                 | Text                                                                | Jane Smith          |
| `sourceFacility`     | Source facility                                | "Alert Hospital", "Research project", "External clinic", or "Other" | Alert Hospital      |
| `specimenType`       | Type of specimen                               | See specimen types below                                            | Fresh biopsy        |
| `specimenSite`       | Anatomical site of specimen                    | Text (e.g., "liver biopsy", "lung FNA")                             | Liver - right lobe  |
| `collectionDateTime` | Date and time specimen was collected           | YYYY-MM-DD HH:MM                                                    | 2025-01-15 08:00    |

### OPTIONAL FIELDS (All Samples)

| Field        | Description                       | Format | Example   |
| ------------ | --------------------------------- | ------ | --------- |
| `surname`    | Surname/Last Name (NOT mandatory) | Text   | Garcia    |
| `nationalId` | National ID (NOT mandatory)       | Text   | 123456789 |

### CLINICAL SAMPLE FIELDS (Required when sampleCategory = "Clinical diagnostic")

| Field                 | Description                        | Format | Example                            |
| --------------------- | ---------------------------------- | ------ | ---------------------------------- |
| `patientId`           | Hospital/clinic patient identifier | Text   | PAT-2025-001                       |
| `requestingClinician` | Name of ordering clinician         | Text   | Dr. Michael Brown                  |
| `clinicalDetails`     | Clinical history and indication    | Text   | Suspected hepatocellular carcinoma |

### RESEARCH SAMPLE FIELDS (Required when sampleCategory = "Research")

| Field                 | Description                        | Format | Example          |
| --------------------- | ---------------------------------- | ------ | ---------------- |
| `studyId`             | Research study/protocol identifier | Text   | LEPROSY-2025     |
| `piName`              | Principal Investigator name        | Text   | Dr. James Wilson |
| `participantAnimalId` | Participant or animal identifier   | Text   | PART-001         |
| `ethicalApprovalRef`  | IRB/Ethics approval number         | Text   | IRB-2025-0042    |

## Valid Specimen Types

### Histopathology

- FFPE tissue block
- Fresh biopsy
- Surgical resection

### Cytopathology

- Fine needle aspirate (FNA)
- Pleural fluid
- Peritoneal fluid
- Pericardial fluid
- CSF for cytology
- Urine for cytology
- Sputum for cytology
- Cervical smear

### Hematology

- EDTA blood (peripheral smear)

### Research

- Human tissue (fresh)
- Human tissue (frozen)
- Human tissue (FFPE)
- Animal tissue (mouse footpad)
- Animal tissue (nerve)
- Bacterial/cellular pellet
- Primary cell culture

## Important Notes

1. **First Name is MANDATORY** - The first name field must be filled for order
   acceptance
2. **Surname/Last Name is OPTIONAL** - Not required for order acceptance
3. **National ID is OPTIONAL** - Not required for order acceptance
4. **Clinical samples** must have patientId, requestingClinician, and
   clinicalDetails filled
5. **Research samples** must have studyId, piName, participantAnimalId, and
   ethicalApprovalRef filled
6. Leave clinical fields empty for research samples and vice versa
7. The system will automatically generate a unique lab accession number and
   barcode

## Example CSV

```csv
firstName,surname,nationalId,sampleCategory,receivedDateTime,receivedBy,sourceFacility,specimenType,specimenSite,collectionDateTime,patientId,requestingClinician,clinicalDetails,studyId,piName,participantAnimalId,ethicalApprovalRef
John,,,"Clinical diagnostic",2025-01-15 09:30,Jane Smith,Alert Hospital,Fresh biopsy,Liver - right lobe,2025-01-15 08:00,PAT-2025-001,Dr. Michael Brown,"Suspected hepatocellular carcinoma, elevated AFP",,,,
,,,"Research",2025-01-15 14:00,Mary Johnson,Research project,Human tissue (fresh),Skin lesion - forearm,2025-01-15 13:00,,,,"LEPROSY-2025",Dr. James Wilson,PART-001,IRB-2025-0042
```
