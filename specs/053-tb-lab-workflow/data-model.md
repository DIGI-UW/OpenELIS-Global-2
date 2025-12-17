# Data Model - Tuberculosis Laboratory Workflow

## Core Entities

- **TbSampleRegistration** (1:1 with `sample_item`)

  - Fields: `sample_item_id` (FK), `document_number`, `specimen_type`
    (SPUTUM/BODY_FLUID/SWAB/TISSUE/OTHER), `specimen_quality`,
    `referring_facility`, `treatment_history`, `physician_phone`,
    `patient_phone`, `consent_status`, `test_requested` (JSONB array),
    `received_site`, `received_datetime`, audit columns.
  - Index: `(sample_item_id)`.

- **TbQualityCheck** (1:1 with `sample_item` per QC attempt)

  - Fields: `sample_item_id` (FK), `qc_date`, pass/fail checks
    (leak/temperature/packaging/labeling/volume/request_match), `overall_result`
    (PASS/FAIL_DISCARD/FAIL_PROCEED), `rejection_reason`, `rejection_remarks`,
    `checked_by`.
  - Index: `(sample_item_id)`; constraint on `overall_result` and per-item
    enumerations.

- **TbCultureReading** (1:M per sample, weeks 1–8)

  - Fields: `sample_item_id` (FK), `week_number` (1–8), `reading_date`,
    `culture_method` (LJ/MGIT/BOTH), `growth_observation`
    (NO_GROWTH/GROWTH_DETECTED/CONTAMINATED), optional `lj_observation`,
    `mgit_observation`, `read_by`, `notes`.
  - Index: `(sample_item_id, week_number)` unique; indexes on `sample_item_id`.

- **TbSmearResult** (0:1 per smear attempt)

  - Fields: `sample_item_id` (FK), `method` (ZN/CONCENTRATED/FLUORESCENT/OTHER),
    `afb_result` (NEGATIVE/SCANTY/PLUS1/PLUS2/PLUS3), `result_date`,
    `tested_by`, `reviewed_by`.
  - Index: `(sample_item_id)`.

- **TbIdentificationResult** (0:1)

  - Fields: `sample_item_id` (FK), `result` (MTB/NTM/NEGATIVE/CONTAMINATED),
    `method` (SMEAR_MORPHOLOGY/BHI_BA/RAPID_TEST_KIT), `result_date`,
    `tested_by`, `reviewed_by`.
  - Index: `(sample_item_id)`.

- **TbGeneXpertResult** (0:1)

  - Fields: `sample_item_id` (FK), `result`
    (MTB_NOT_DETECTED/MTB_RIF_SENSITIVE/MTB_RIF_RESISTANT/MTB_RIF_INDETERMINATE),
    `method` (GENEXPERT/REALTIME_PCR/OTHER), `result_date`, `tested_by`,
    `reviewed_by`.
  - Index: `(sample_item_id)`.

- **TbDstResult** (0:1 per DST run)

  - Fields: `sample_item_id` (FK), `method`
    (PHENOTYPIC_1ST/PHENOTYPIC_2ND/MOLECULAR_1ST), first-line results
    (`inh_result`, `rmp_result`, `stm_result`, `emb_result`, `pza_result`),
    `second_line_results` (JSONB), `mdr_flag`, `result_date`, `tested_by`,
    `reviewed_by`.
  - Index: `(sample_item_id)`.

- **TbIsolateStorage** (0:1 current assignment, allow history)
  - Fields: `sample_item_id` (FK), `room`, `fridge_id`, `compartment`, `rack`,
    `box`, `storage_date`, `stored_by`, `retrieval_date`, `retrieved_by`.
  - Indexes: `(sample_item_id)`, `(room, fridge_id)`.

## Extended Entities

- **SampleItem**: add `tb_sample_id` (NNN/YY), optional `external_id`; unique
  index on `tb_sample_id`; generation via yearly sequence.
- **NotebookPage**: add TB page types (Registration, QC, Labeling, Processing,
  Culture Tracking, Smear, Identification, GeneXpert, DST, Isolate Storage,
  Result Compilation).
- **NotebookPageSample / NotebookEntry**: reused for per-sample per-page status;
  no schema change.

## Relationships

- SampleItem 1:1 TbSampleRegistration; 1:1 TbQualityCheck; 1:M TbCultureReading;
  optional 1:1 per downstream result entity.
- TbIsolateStorage links SampleItem to hierarchical storage; integrates with
  SampleStorageService for lookup UI.
- Result compilation aggregates Tb\* result rows + QC status + storage for
  reporting/export.

## Derived Rules

- `mdr_flag` computed when INH and RMP are Resistant; service also raises alert
  on save.
- QC failures flagged at service layer and passed to UI for warnings across
  pages.
- Culture status derived from readings: Growth triggers "Culture Positive" and
  eligibility for identification/DST; Week 8 No Growth => "Culture Negative".
