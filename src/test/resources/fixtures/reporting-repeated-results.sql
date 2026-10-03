-- Synthetic reporting acceptance fixture. Only load into a development/test database.
-- One specimen has two independent, identical finalized Viral Load readings.
-- Both must survive export; equality of values does not imply duplication.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;
DO $fixture$
DECLARE
    patient_id_value numeric;
    person_id_value numeric;
    sample_id_value numeric;
    item_id_value numeric;
    analysis_id_value numeric;
    test_id_value numeric;
    section_id_value numeric;
    type_id_value numeric;
    sample_status_value numeric;
    finalized_status_value numeric;
    result_uuid uuid;
    analysis_uuid uuid;
    reading_index integer;
BEGIN
    SELECT id, test_section_id INTO STRICT test_id_value, section_id_value
      FROM test WHERE guid = 'b50d156e-0f6f-40cd-921c-4e831602a623' AND is_active = 'Y';
    SELECT id INTO STRICT type_id_value FROM type_of_sample
      WHERE display_key = 'sample.type.Sang' AND is_active = true;
    SELECT id INTO STRICT sample_status_value FROM status_of_sample
      WHERE display_key = 'status.sample.finished' AND is_active = 'Y';
    SELECT id INTO STRICT finalized_status_value FROM status_of_sample
      WHERE display_key = 'status.test.valid' AND status_type = 'ANALYSIS' AND is_active = 'Y';

    SELECT id INTO patient_id_value FROM patient WHERE external_id = 'REPORTING-MVP-SYNTHETIC';
    IF patient_id_value IS NULL THEN
        person_id_value := nextval('person_seq');
        INSERT INTO person (id, first_name, last_name, lastupdated)
          VALUES (person_id_value, 'Synthetic', 'Reporting Fixture', timestamp '2026-05-05 10:00:00');
        patient_id_value := nextval('patient_seq');
        INSERT INTO patient (id, person_id, external_id, gender, birth_date, fhir_uuid, lastupdated)
          VALUES (patient_id_value, person_id_value, 'REPORTING-MVP-SYNTHETIC', 'F', date '1990-01-01',
            '47900000-0000-4000-8000-000000000001', timestamp '2026-05-05 10:00:00');
    END IF;

    SELECT id INTO sample_id_value FROM sample WHERE accession_number = 'REPORTING-MVP-REPEAT';
    IF sample_id_value IS NULL THEN
        sample_id_value := nextval('sample_seq');
        INSERT INTO sample (id, fhir_uuid, accession_number, domain, next_item_sequence, revision,
          entered_date, received_date, collection_date, status, status_id, released_date, sys_user_id, lastupdated)
          VALUES (sample_id_value, '47900000-0000-4000-8000-000000000002', 'REPORTING-MVP-REPEAT',
            'H', 2, 0, timestamp '2026-05-05 08:00:00', timestamp '2026-05-05 09:00:00',
            timestamp '2026-05-05 07:00:00', sample_status_value::text, sample_status_value,
            timestamp '2026-05-05 10:00:00', 1, timestamp '2026-05-05 10:00:00');
        INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
          VALUES (nextval('sample_human_seq'), sample_id_value, patient_id_value, timestamp '2026-05-05 10:00:00');
    ELSIF (SELECT fhir_uuid FROM sample WHERE id = sample_id_value) IS DISTINCT FROM
      '47900000-0000-4000-8000-000000000002'::uuid THEN
        RAISE EXCEPTION 'Reporting fixture accession is already in use';
    END IF;

    SELECT id INTO item_id_value FROM sample_item WHERE fhir_uuid = '47900000-0000-4000-8000-000000000003';
    IF item_id_value IS NULL THEN
        item_id_value := nextval('sample_item_seq');
        INSERT INTO sample_item (id, fhir_uuid, sort_order, samp_id, typeosamp_id, quantity,
          collection_date, received_date, status_id, collector, lastupdated)
          VALUES (item_id_value, '47900000-0000-4000-8000-000000000003', 1, sample_id_value,
            type_id_value, 1, timestamp '2026-05-05 07:00:00', timestamptz '2026-05-05 09:00:00+00',
            sample_status_value, 'Synthetic reporting fixture', timestamp '2026-05-05 10:00:00');
    END IF;

    SELECT id INTO analysis_id_value FROM analysis WHERE fhir_uuid = '47900000-0000-4000-8000-000000000004';
    IF analysis_id_value IS NULL THEN
        analysis_id_value := nextval('analysis_seq');
        INSERT INTO analysis (id, fhir_uuid, sampitem_id, test_sect_id, test_id, revision, status,
          started_date, completed_date, released_date, is_reportable, analysis_type, lastupdated,
          status_id, entry_date, referred_out, type_of_sample_name, corrected)
          VALUES (analysis_id_value, '47900000-0000-4000-8000-000000000004', item_id_value,
            section_id_value, test_id_value, 0, finalized_status_value::text,
            timestamp '2026-05-05 09:30:00', timestamp '2026-05-05 10:00:00', timestamp '2026-05-05 10:00:00',
            'Y', 'NORMAL', timestamp '2026-05-05 10:00:00', finalized_status_value,
            timestamp '2026-05-05 09:30:00', false, 'Whole Blood', false);
    END IF;

    FOREACH result_uuid IN ARRAY ARRAY['47900000-0000-4000-8000-000000000005'::uuid,
      '47900000-0000-4000-8000-000000000006'::uuid] LOOP
        IF NOT EXISTS (SELECT 1 FROM result WHERE fhir_uuid = result_uuid) THEN
            INSERT INTO result (id, fhir_uuid, analysis_id, sort_order, is_reportable, result_type,
              value, significant_digits, "grouping", lastupdated)
              VALUES (nextval('result_seq'), result_uuid, analysis_id_value, 1, 'Y', 'N', '450',
                0, 0, timestamp '2026-05-05 10:00:00');
        END IF;
    END LOOP;
    IF (SELECT count(*) FROM result WHERE analysis_id = analysis_id_value AND value = '450') <> 2 THEN
        RAISE EXCEPTION 'Expected exactly two independent readings with value 450';
    END IF;

    -- A separate collection date keeps the original two-reading oracle stable.
    -- Equal values in distinct analyses finish at different times (30/90 min).
    SELECT id INTO sample_id_value FROM sample WHERE accession_number = 'REPORTING-MVP-TURNAROUND';
    IF sample_id_value IS NULL THEN
        sample_id_value := nextval('sample_seq');
        INSERT INTO sample (id, fhir_uuid, accession_number, domain, next_item_sequence, revision,
          entered_date, received_date, collection_date, status, status_id, released_date, sys_user_id, lastupdated)
          VALUES (sample_id_value, '47900000-0000-4000-8000-000000000011', 'REPORTING-MVP-TURNAROUND',
            'H', 2, 0, timestamp '2026-05-06 08:00:00', timestamp '2026-05-06 09:00:00',
            timestamp '2026-05-06 07:00:00', sample_status_value::text, sample_status_value,
            timestamp '2026-05-06 14:00:00', 1, timestamp '2026-05-06 14:00:00');
        INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
          VALUES (nextval('sample_human_seq'), sample_id_value, patient_id_value, timestamp '2026-05-06 14:00:00');
    ELSIF NOT EXISTS (SELECT 1 FROM sample WHERE id = sample_id_value
        AND fhir_uuid = '47900000-0000-4000-8000-000000000011') THEN
        RAISE EXCEPTION 'Synthetic turnaround accession already in use';
    END IF;
    SELECT id INTO item_id_value FROM sample_item WHERE fhir_uuid = '47900000-0000-4000-8000-000000000013';
    IF item_id_value IS NULL THEN
        item_id_value := nextval('sample_item_seq');
        INSERT INTO sample_item (id, fhir_uuid, sort_order, samp_id, typeosamp_id, quantity,
          collection_date, received_date, status_id, collector, lastupdated)
          VALUES (item_id_value, '47900000-0000-4000-8000-000000000013', 1, sample_id_value,
            type_id_value, 1, timestamp '2026-05-06 07:00:00', timestamptz '2026-05-06 09:00:00+00',
            sample_status_value, 'Synthetic reporting fixture', timestamp '2026-05-06 09:00:00');
    END IF;
    FOR reading_index IN 0..1 LOOP
        analysis_uuid := CASE reading_index WHEN 0 THEN '47900000-0000-4000-8000-000000000014'::uuid
            ELSE '47900000-0000-4000-8000-000000000017'::uuid END;
        result_uuid := CASE reading_index WHEN 0 THEN '47900000-0000-4000-8000-000000000015'::uuid
            ELSE '47900000-0000-4000-8000-000000000016'::uuid END;
        SELECT id INTO analysis_id_value FROM analysis WHERE fhir_uuid = analysis_uuid;
        IF analysis_id_value IS NULL THEN
            analysis_id_value := nextval('analysis_seq');
            INSERT INTO analysis (id, fhir_uuid, sampitem_id, test_sect_id, test_id, revision, status,
              completed_date, released_date, is_reportable, analysis_type, lastupdated, status_id,
              entry_date, referred_out, type_of_sample_name, corrected)
              VALUES (analysis_id_value, analysis_uuid, item_id_value, section_id_value, test_id_value,
                0, finalized_status_value::text,
                timestamp '2026-05-06 10:00:00' + reading_index * interval '2 hours',
                timestamp '2026-05-06 10:30:00' + reading_index * interval '3 hours',
                'Y', 'NORMAL', timestamp '2026-05-06 14:00:00', finalized_status_value,
                timestamp '2026-05-06 09:00:00', false, 'Whole Blood', false);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM result WHERE fhir_uuid = result_uuid) THEN
            INSERT INTO result (id, fhir_uuid, analysis_id, sort_order, is_reportable, result_type,
              value, significant_digits, "grouping", lastupdated)
              VALUES (nextval('result_seq'), result_uuid, analysis_id_value, 1, 'Y', 'N', '450',
                0, 0, timestamp '2026-05-06 14:00:00');
        END IF;
    END LOOP;
END
$fixture$;

-- Persisted form of fixtures/reporting-sources/sample-summary.json. The backend
-- integration test exercises the configuration loader; browser tests consume
-- its stored definition through the same catalog and queue as Sample & Testing.
INSERT INTO report_definition
  (id, name, category, definition_json, report_type, is_active, is_public, created_by, created_date, last_updated)
VALUES ('SAMPLE_SUMMARY', 'Sample summary', 'Data export', $config$
{
  "id": "SAMPLE_SUMMARY", "version": 1, "label": "Sample summary",
  "source": "SAMPLE_TESTING", "dateAnchor": "collectionDate",
  "layouts": ["SPREADSHEET", "RESULT_LIST"],
  "attributes": ["specimenId", "accessionNumber", "resultValue"],
  "catalogs": ["tests"], "filters": ["labSectionIds", "testIds", "resultStatuses"],
  "defaultColumns": {
    "SPREADSHEET": ["specimenId", "accessionNumber"],
    "RESULT_LIST": ["accessionNumber", "resultValue"]
  }
}
$config$, 'CSV_SOURCE', true, true, '1', now(), now())
ON CONFLICT (id) DO NOTHING;

-- Persisted form of reporting-sources/finalized-sample-summary.json. This
-- preset offers only the period and columns; the source's finalized/all-tests
-- defaults apply even after the user selected filters in another report.
INSERT INTO report_definition
  (id, name, category, definition_json, report_type, is_active, is_public, created_by, created_date, last_updated)
VALUES ('FINALIZED_SAMPLE_SUMMARY', 'Finalized sample summary', 'Data export', $config$
{
  "id": "FINALIZED_SAMPLE_SUMMARY", "version": 1, "label": "Finalized sample summary",
  "source": "SAMPLE_TESTING", "dateAnchor": "collectionDate",
  "layouts": ["SPREADSHEET", "RESULT_LIST"],
  "attributes": ["specimenId", "accessionNumber", "resultValue"],
  "catalogs": ["tests"], "filters": [],
  "defaultColumns": {
    "SPREADSHEET": ["specimenId", "accessionNumber"],
    "RESULT_LIST": ["accessionNumber", "resultValue"]
  }
}
$config$, 'CSV_SOURCE', true, true, '1', now(), now())
ON CONFLICT (id) DO NOTHING;
COMMIT;
