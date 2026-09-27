-- Public synthetic workload for the disposable local or dedicated Reporting UAT stack.
-- Load through qualify-workload.py (local) or prepare-cancellation-workload.py.
-- 5,000 specimens have two analyses with four readings each (40,000 results).
-- One additional specimen has 10,000 readings, exercising a large repeat group.
-- Adjacent readings have equal values but distinct identities. No rows are deleted.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;
DO $fixture$
DECLARE
    test_value numeric;
    section_value numeric;
    type_value numeric;
    sample_status numeric;
    final_status numeric;
    person_value numeric;
    patient_value numeric;
BEGIN
    SELECT id, test_section_id INTO STRICT test_value, section_value FROM test
      WHERE guid = 'b50d156e-0f6f-40cd-921c-4e831602a623' AND is_active = 'Y';
    SELECT id INTO STRICT type_value FROM type_of_sample
      WHERE display_key = 'sample.type.Sang' AND is_active = true;
    SELECT id INTO STRICT sample_status FROM status_of_sample
      WHERE display_key = 'status.sample.finished' AND is_active = 'Y';
    SELECT id INTO STRICT final_status FROM status_of_sample
      WHERE display_key = 'status.test.valid' AND status_type = 'ANALYSIS' AND is_active = 'Y';

    IF EXISTS (SELECT 1 FROM sample WHERE accession_number LIKE 'RPT50K-V1-%') THEN
        IF (SELECT count(*) FROM sample WHERE fhir_uuid::text LIKE '47950000-0000-4000-8001-%') = 5001
          AND (SELECT count(*) FROM result WHERE fhir_uuid::text LIKE '47950000-0000-4000-8004-%') = 50000 THEN
            RETURN; -- The runner independently verifies every identity, value and relationship.
        END IF;
        RAISE EXCEPTION 'Workload fixture is incomplete or its accessions are already in use';
    END IF;
    IF EXISTS (SELECT 1 FROM sample_item WHERE collection_date >= timestamptz '2026-05-07 00:00:00+00'
        AND collection_date < timestamptz '2026-05-08 00:00:00+00') THEN
        RAISE EXCEPTION 'Workload date is already used; do not replace existing records';
    END IF;

    person_value := nextval('person_seq');
    INSERT INTO person (id, first_name, last_name, lastupdated)
      VALUES (person_value, 'Synthetic', 'Reporting Workload', timestamp '2026-05-07 10:00:00');
    patient_value := nextval('patient_seq');
    INSERT INTO patient (id, person_id, external_id, gender, birth_date, fhir_uuid, lastupdated)
      VALUES (patient_value, person_value, 'RPT50K-V1-SYNTHETIC', 'F', date '1990-01-01',
        '47950000-0000-4000-8006-000000000001', timestamp '2026-05-07 10:00:00');

    CREATE TEMP TABLE reporting_workload_samples ON COMMIT DROP AS
      SELECT i, nextval('sample_seq') AS sample_id, nextval('sample_item_seq') AS item_id
      FROM generate_series(1, 5001) AS series(i);
    INSERT INTO sample (id, fhir_uuid, accession_number, domain, next_item_sequence, revision,
      entered_date, received_date, collection_date, status, status_id, released_date, sys_user_id, lastupdated)
      SELECT sample_id, ('47950000-0000-4000-8001-' || lpad(i::text, 12, '0'))::uuid,
        'RPT50K-V1-' || lpad(i::text, 5, '0'), 'H', 2, 0,
        timestamp '2026-05-07 08:00:00', timestamp '2026-05-07 09:00:00',
        timestamp '2026-05-07 07:00:00', sample_status::text, sample_status,
        timestamp '2026-05-07 14:00:00', 1, timestamp '2026-05-07 14:00:00'
      FROM reporting_workload_samples;
    INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
      SELECT nextval('sample_human_seq'), sample_id, patient_value, timestamp '2026-05-07 14:00:00'
      FROM reporting_workload_samples;
    INSERT INTO sample_item (id, fhir_uuid, sort_order, samp_id, typeosamp_id, quantity,
      collection_date, received_date, status_id, collector, lastupdated)
      SELECT item_id, ('47950000-0000-4000-8002-' || lpad(i::text, 12, '0'))::uuid,
        1, sample_id, type_value, 1, timestamptz '2026-05-07 07:00:00+00',
        timestamptz '2026-05-07 09:00:00+00', sample_status, 'Synthetic workload',
        timestamp '2026-05-07 14:00:00'
      FROM reporting_workload_samples;

    CREATE TEMP TABLE reporting_workload_analyses ON COMMIT DROP AS
      SELECT s.i, s.item_id, slot, nextval('analysis_seq') AS analysis_id
      FROM reporting_workload_samples s
      CROSS JOIN LATERAL generate_series(1, CASE WHEN s.i = 5001 THEN 1 ELSE 2 END) AS slots(slot);
    INSERT INTO analysis (id, fhir_uuid, sampitem_id, test_sect_id, test_id, revision, status,
      completed_date, released_date, is_reportable, analysis_type, lastupdated, status_id,
      entry_date, referred_out, type_of_sample_name, corrected)
      SELECT analysis_id, ('47950000-0000-4000-8003-' || lpad(((i - 1) * 2 + slot)::text, 12, '0'))::uuid,
        item_id, section_value, test_value, 0, final_status::text,
        timestamp '2026-05-07 10:00:00',
        timestamp '2026-05-07 10:00:00' + (CASE slot WHEN 1 THEN 30 ELSE 90 END) * interval '1 minute',
        'Y', 'NORMAL', timestamp '2026-05-07 14:00:00', final_status,
        timestamp '2026-05-07 09:00:00', false, 'Whole Blood', false
      FROM reporting_workload_analyses;
    INSERT INTO result (id, fhir_uuid, analysis_id, sort_order, is_reportable, result_type,
      value, significant_digits, "grouping", lastupdated)
      SELECT nextval('result_seq'),
        ('47950000-0000-4000-8004-' || lpad((CASE WHEN i = 5001 THEN 40000 + ordinal
          ELSE (i - 1) * 8 + (slot - 1) * 4 + ordinal END)::text, 12, '0'))::uuid,
        analysis_id, ordinal, 'Y', 'N',
        (CASE WHEN i = 5001 THEN 100 + ((ordinal - 1) / 2) % 50
          ELSE i % 1000 + (ordinal - 1) / 2 END)::text,
        0, 0, timestamp '2026-05-07 14:00:00'
      FROM reporting_workload_analyses
      CROSS JOIN LATERAL generate_series(1, CASE WHEN i = 5001 THEN 10000 ELSE 4 END) AS readings(ordinal);
    IF (SELECT count(*) FROM result WHERE fhir_uuid::text LIKE '47950000-0000-4000-8004-%') <> 50000 THEN
        RAISE EXCEPTION 'Expected 50,000 workload results';
    END IF;
END
$fixture$;
COMMIT;
