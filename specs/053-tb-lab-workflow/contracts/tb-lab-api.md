# TB Lab Workflow API Contracts

Base path: `/rest/tb`

## Sample Registration
- `POST /samples/register`
  - Request: `documentNumber`, `specimenType` (SPUTUM/BODY_FLUID/SWAB/TISSUE/OTHER), patient + contact metadata, `consentStatus`, `testRequested` array, `receivedSite`, `receivedDatetime`.
  - Response: `sampleId`, `tbSampleId (NNN/YY)`, `status` (REGISTERED).
- `GET /samples/{sampleId}` → aggregate TB sample with registration, QC, culture, smear, identification, GeneXpert, DST, storage, report summaries.

## Quality Check
- `POST /samples/{sampleId}/qc`
  - Request: individual check results, `overallResult` (PASS/FAIL_DISCARD/FAIL_PROCEED), optional `rejectionReason`, `rejectionRemarks`.
  - Response: qc id + `overallResult`, flag for proceed vs discard.
- `GET /samples/{sampleId}/qc` → current QC state + warning flag for failures.

## Culture Tracking
- `POST /samples/{sampleId}/culture/reading`
  - Request: `weekNumber` (1–8), `readingDate`, `cultureMethod` (LJ/MGIT/BOTH), `growthObservation` (NO_GROWTH/GROWTH_DETECTED/CONTAMINATED), optional `ljObservation`, `mgitObservation`, `notes`.
  - Response: `readingId`, `nextReadingDue`, derived status (IN_PROGRESS/CULTURE_POSITIVE/CULTURE_NEGATIVE/CONTAMINATED).
- `GET /samples/{sampleId}/culture/readings` → list of week rows.
- `GET /samples/{sampleId}/culture/status` → summary with current week, positive week (if any), method.

## Smear Microscopy
- `POST /samples/{sampleId}/smear`
  - Request: `method` (ZN/CONCENTRATED/FLUORESCENT/OTHER), `afbResult` (NEGATIVE/SCANTY/PLUS1/PLUS2/PLUS3), `resultDate`, `testedBy`, `reviewedBy`.
  - Response: `smearId`, stored result.

## Species Identification
- `POST /samples/{sampleId}/identification`
  - Request: `result` (MTB/NTM/NEGATIVE/CONTAMINATED), `method` (SMEAR_MORPHOLOGY/BHI_BA/RAPID_TEST_KIT), `resultDate`, `testedBy`, `reviewedBy`.
  - Response: `identificationId`, `eligibleForDst` boolean.

## GeneXpert / Molecular
- `POST /samples/{sampleId}/genexpert`
  - Request: `result` (MTB_NOT_DETECTED/MTB_RIF_SENSITIVE/MTB_RIF_RESISTANT/MTB_RIF_INDETERMINATE), `method` (GENEXPERT/REALTIME_PCR/OTHER), `resultDate`, `testedBy`, `reviewedBy`.
  - Response: `geneXpertId`, `rifResistanceAlert` boolean.

## Drug Susceptibility Testing
- `POST /samples/{sampleId}/dst`
  - Request: `method` (PHENOTYPIC_1ST/PHENOTYPIC_2ND/MOLECULAR_1ST), per-drug results (INH/RMP/STM/EMB/PZA), `resultDate`, `testedBy`, `reviewedBy`.
  - Response: `dstId`, `mdrFlag` (true when INH-R + RMP-R), `mdrAlert` message.
- `POST /samples/{sampleId}/dst/second-line`
  - Request: `method` (PHENOTYPIC_2ND or MOLECULAR_1ST), `secondLineResults` JSON map (FLQ, KAN/AMK/CAP, etc.), `resultDate`, `testedBy`, `reviewedBy`.

## Isolate Storage
- `POST /samples/{sampleId}/storage`
  - Request: `room`, `fridgeId`, optional `compartment`, `rack`, `box`, `storageDate` (default now), `storedBy`.
  - Response: `storageId`, `location` string (Room > Fridge > Compartment > Rack > Box).
- `POST /samples/{sampleId}/storage/retrieve`
  - Request: `retrievalDate`, optional `reason`, `retrievedBy`.

## Result Compilation & Export
- `GET /samples/{sampleId}/report` → compiled report sections (registration, QC flag, culture status, smear, identification, GeneXpert, DST, storage, audit fields).
- `POST /samples/{sampleId}/report/finalize` → sets `reportedBy`, `reviewedBy`, `comments`, `reportDate`, status FINALIZED.
- `GET /samples/export?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD&format=csv|xlsx` → CSV/Excel export for REDCap; includes `exportedToRedcapAt` + user when finalized.
