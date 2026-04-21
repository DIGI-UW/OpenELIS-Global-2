-- =============================================================================
-- Analyzer harness lane fixtures (deterministic demo data)
-- =============================================================================
-- Loaded AFTER storage-e2e fixtures by load-test-fixtures.sh (--profile=harness).
-- Uses accession numbers with prefix DEV0126 (valid SiteYearNum) (isolated from storage E2E*** samples).
--
-- See: projects/analyzer-harness/LANE-IDENTIFIERS.md
-- =============================================================================
SET search_path TO clinlims;

-- Samples + items + sample_human (reuse patient 1000 from storage-e2e)
INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, sys_user_id,
                    received_date, lastupdated, is_confirmation)
VALUES
  (1050, 'DEV01261000000000001', '660e8400-e29b-41d4-a716-446655440050', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1051, 'DEV01262000000000001', '660e8400-e29b-41d4-a716-446655440051', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1052, 'DEV01262000000000002', '660e8400-e29b-41d4-a716-446655440052', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1053, 'DEV01262000000000003', '660e8400-e29b-41d4-a716-446655440053', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1054, 'DEV01262000000000004', '660e8400-e29b-41d4-a716-446655440054', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1055, 'DEV01262000000000005', '660e8400-e29b-41d4-a716-446655440055', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1056, 'DEV01262000000000007', '660e8400-e29b-41d4-a716-446655440056', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1057, 'DEV01262100000000001', '660e8400-e29b-41d4-a716-446655440057', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1058, 'DEV01262100000000002', '660e8400-e29b-41d4-a716-446655440058', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1059, 'DEV01262100000000005', '660e8400-e29b-41d4-a716-446655440059', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1060, 'DEV01263000000000001', '660e8400-e29b-41d4-a716-446655440060', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1061, 'DEV01263000000000002', '660e8400-e29b-41d4-a716-446655440061', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1062, 'DEV01263000000000003', '660e8400-e29b-41d4-a716-446655440062', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE SET
  accession_number = EXCLUDED.accession_number,
  lastupdated      = EXCLUDED.lastupdated;

INSERT INTO sample_item (id, samp_id, sort_order, external_id, typeosamp_id, collection_date,
                         collector, quantity, status_id, voided, rejected, lastupdated)
VALUES
  (10600, 1050, 1, 'GX-TUBE-001', 1, CURRENT_TIMESTAMP, 'Harness-GX', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10601, 1051, 1, 'QS7-TUBE-001', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10602, 1052, 1, 'QS7-TUBE-002', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10603, 1053, 1, 'QS7-TUBE-003', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10604, 1054, 1, 'QS7-TUBE-004', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10605, 1055, 1, 'QS7-TUBE-005', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10606, 1056, 1, 'QS7-TUBE-007', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10607, 1057, 1, 'QS5-TUBE-001', 1, CURRENT_TIMESTAMP, 'Harness-QS5', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10608, 1058, 1, 'QS5-TUBE-002', 1, CURRENT_TIMESTAMP, 'Harness-QS5', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10609, 1059, 1, 'QS5-TUBE-005', 1, CURRENT_TIMESTAMP, 'Harness-QS5', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10610, 1060, 1, 'FC-TUBE-001', 1, CURRENT_TIMESTAMP, 'Harness-FC', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10611, 1061, 1, 'FC-TUBE-002', 1, CURRENT_TIMESTAMP, 'Harness-FC', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10612, 1062, 1, 'FC-TUBE-003', 1, CURRENT_TIMESTAMP, 'Harness-FC', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  samp_id     = EXCLUDED.samp_id,
  external_id = EXCLUDED.external_id,
  lastupdated = EXCLUDED.lastupdated;

INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
VALUES
  (1050, 1050, 1000, CURRENT_TIMESTAMP),
  (1051, 1051, 1000, CURRENT_TIMESTAMP),
  (1052, 1052, 1000, CURRENT_TIMESTAMP),
  (1053, 1053, 1000, CURRENT_TIMESTAMP),
  (1054, 1054, 1000, CURRENT_TIMESTAMP),
  (1055, 1055, 1000, CURRENT_TIMESTAMP),
  (1056, 1056, 1000, CURRENT_TIMESTAMP),
  (1057, 1057, 1000, CURRENT_TIMESTAMP),
  (1058, 1058, 1000, CURRENT_TIMESTAMP),
  (1059, 1059, 1000, CURRENT_TIMESTAMP),
  (1060, 1060, 1000, CURRENT_TIMESTAMP),
  (1061, 1061, 1000, CURRENT_TIMESTAMP),
  (1062, 1062, 1000, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  samp_id    = EXCLUDED.samp_id,
  patient_id = EXCLUDED.patient_id,
  lastupdated = EXCLUDED.lastupdated;

-- Analyses: GeneXpert ASTM harness path and QuantStudio file-import (LOINC 94500-6).
-- GX demo uses COVID-only ASTM from e2e-fixtures/genexpert_astm.json so one analysis
-- row is sufficient for accept → results in AccessionResults.
DO $$
DECLARE
  not_started_id NUMERIC;
  test_sect_id   NUMERIC;
  test_covid_id  NUMERIC;
BEGIN
  SELECT id
    INTO not_started_id
    FROM status_of_sample
   WHERE name = 'Not Tested' AND status_type = 'ANALYSIS'
   LIMIT 1;

  SELECT id
    INTO test_sect_id
    FROM test_section
   WHERE is_active = 'Y'
   ORDER BY id
   LIMIT 1;

  SELECT id INTO test_covid_id FROM test WHERE loinc = '94500-6' AND is_active = 'Y' ORDER BY id LIMIT 1;

  IF not_started_id IS NULL THEN
    RAISE EXCEPTION 'analyzer-harness-lane-data.sql: missing ANALYSIS status Not Tested';
  END IF;
  IF test_covid_id IS NULL THEN
    RAISE EXCEPTION 'analyzer-harness-lane-data.sql: missing COVID/PCR test with LOINC 94500-6';
  END IF;

  INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, analysis_type,
                        entry_date, started_date, completed_date, is_reportable, lastupdated)
  VALUES
    (21110, 10600, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21111, 10601, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21112, 10602, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21113, 10603, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21114, 10604, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21115, 10605, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21116, 10606, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21117, 10607, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21118, 10608, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21119, 10609, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21120, 10610, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21121, 10611, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP),
    (21122, 10612, test_covid_id, test_sect_id, not_started_id, '1', 'MANUAL',
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    sampitem_id = EXCLUDED.sampitem_id,
    test_id     = EXCLUDED.test_id,
    status_id   = EXCLUDED.status_id,
    lastupdated = EXCLUDED.lastupdated;
END $$;
