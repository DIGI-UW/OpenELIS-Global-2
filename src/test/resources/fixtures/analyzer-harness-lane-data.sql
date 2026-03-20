-- =============================================================================
-- Analyzer harness lane fixtures (deterministic demo data)
-- =============================================================================
-- Loaded AFTER storage-e2e fixtures by load-test-fixtures.sh (--analyzers=full).
-- Uses accession numbers with prefix HARN- (isolated from storage E2E*** samples).
--
-- See: projects/analyzer-harness/LANE-IDENTIFIERS.md
-- =============================================================================
SET search_path TO clinlims;

-- Samples + items + sample_human (reuse patient 1000 from storage-e2e)
INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, sys_user_id,
                    received_date, lastupdated, is_confirmation)
VALUES
  (1050, 'HARN-GX-2026-00001', '660e8400-e29b-41d4-a716-446655440050', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1051, 'HARN-QS7-2026-00001', '660e8400-e29b-41d4-a716-446655440051', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1052, 'HARN-QS7-2026-00002', '660e8400-e29b-41d4-a716-446655440052', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1053, 'HARN-QS7-2026-00003', '660e8400-e29b-41d4-a716-446655440053', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1054, 'HARN-QS7-2026-00004', '660e8400-e29b-41d4-a716-446655440054', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1055, 'HARN-QS7-2026-00005', '660e8400-e29b-41d4-a716-446655440055', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
  (1056, 'HARN-QS7-2026-00007', '660e8400-e29b-41d4-a716-446655440056', 'H', 1,
   CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE SET
  accession_number = EXCLUDED.accession_number,
  lastupdated      = EXCLUDED.lastupdated;

INSERT INTO sample_item (id, samp_id, sort_order, external_id, typeosamp_id, collection_date,
                         collector, quantity, status_id, voided, rejected, lastupdated)
VALUES
  (10600, 1050, 1, 'HARN-GX-2026-TUBE-1', 1, CURRENT_TIMESTAMP, 'Harness-GX', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10601, 1051, 1, 'HARN-QS7-00001-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10602, 1052, 1, 'HARN-QS7-00002-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10603, 1053, 1, 'HARN-QS7-00003-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10604, 1054, 1, 'HARN-QS7-00004-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10605, 1055, 1, 'HARN-QS7-00005-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
   CURRENT_TIMESTAMP),
  (10606, 1056, 1, 'HARN-QS7-00007-TUBE', 1, CURRENT_TIMESTAMP, 'Harness-QS7', 5.0, 1, FALSE, FALSE,
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
  (1056, 1056, 1000, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  samp_id    = EXCLUDED.samp_id,
  patient_id = EXCLUDED.patient_id,
  lastupdated = EXCLUDED.lastupdated;

-- Analyses: QuantStudio / file-import only (LOINC 94500-6). GeneXpert ASTM staging
-- resolves tests via analyzer profile + DB mappings; minimal dev DBs may not ship
-- MTB/RIF/HIV-VL rows, so we do not pre-insert analyses for the GX lane.
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
     CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    sampitem_id = EXCLUDED.sampitem_id,
    test_id     = EXCLUDED.test_id,
    status_id   = EXCLUDED.status_id,
    lastupdated = EXCLUDED.lastupdated;
END $$;
