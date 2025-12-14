# Data Model: Pharmaceuticals Laboratory Workflow

## Entities

- **PharmaceuticalSample**

  - Fields: id, uniqueSampleId, sampleName, iupacName, gradeSpec, lotBatch,
    manufactureDate, expiryOrRetestDate, storageCondition, ownerRequester,
    chainOfCustodyRef, clinicalTrialId (opt), patientId (opt), fhirUuid (if
    exposed).
  - State: Registered → QCFailed | Processing → Stored → InAssay → Completed |
    Disposed.
  - Relations: one-to-many Aliquot; one-to-many QCCheck; one-to-many
    ProcessingStep; one-to-many ChainOfCustodyEvent; one-to-many DisposalRecord.

- **Aliquot**

  - Fields: id, parentSampleId, aliquotCode (A01...), volumeWeight, units,
    freezeThawCount, labelData, status (Available, Reserved, Consumed,
    Disposed), storageLocationId, fhirUuid (if exposed).
  - Relations: many-to-one PharmaceuticalSample; many-to-one StorageLocation;
    one-to-many ChainOfCustodyEvent.

- **QCCheck**

  - Fields: id, sampleId, labType (pharma/biological/micro), checklistVersion,
    outcome (Pass/Fail), reasons, reviewer, reviewedAt.
  - Relations: many-to-one PharmaceuticalSample.

- **ProcessingStep**

  - Fields: id, sampleId, stepType (weighing, grinding, dissolution,
    centrifugation, streaking/filtration), operator, startedAt, completedAt,
    notes, outputRef.
  - Relations: many-to-one PharmaceuticalSample.

- **AssayRun (Notebook Page)**

  - Fields: id, sampleId, aliquotId (opt), assayType (HPLC potency, TLC ID,
    dissolution, etc.), templateId, controls, replicates, calculations
    (%RSD/CV), acceptanceCriteria, oosFlag, status (Draft, Submitted, Approved,
    Rejected).
  - Relations: many-to-one PharmaceuticalSample; many-to-one Aliquot;
    one-to-many DeviationCAPA; review records.

- **DeviationCAPA**

  - Fields: id, assayRunId, type (Deviation/OOS), reason, rootCause,
    correctiveAction, preventiveAction, status, owner, closedAt.
  - Relations: many-to-one AssayRun.

- **StorageLocation** (reuse existing)

  - Fields: id, type (room/device/rack/box/position), code/path, capacity,
    status.
  - Relations: one-to-many Aliquot assignments.

- **ChainOfCustodyEvent**

  - Fields: id, sampleId (opt), aliquotId (opt), action (Retrieval, Shipment,
    Transfer, Return), handler, condition, timestamp, notes, approvalRef.
  - Relations: many-to-one PharmaceuticalSample or Aliquot; approvals.

- **DisposalRecord**
  - Fields: id, sampleId (opt), aliquotId (opt), reason (Expiry, Exhausted, QC
    Fail, Safety), method (Incineration, Autoclave, Other), approvedBy,
    disposedAt, retentionUntil.
  - Relations: many-to-one PharmaceuticalSample or Aliquot.

## State & Validation Notes

- Sample registration requires mandatory metadata (name, IUPAC, lot, grade/spec,
  manufacture, expiry/retest, storage condition, requester).
- QC outcome Pass/Fail blocks or allows progression to Processing/Storage.
- Aliquot status transitions respect freeze–thaw limits; retrieval blocked when
  limit exceeded unless supervisor override.
- AssayRun cannot finalize if OOS without linked DeviationCAPA.
- Disposal requires approvals and method appropriate to material type (pharma vs
  biological).
