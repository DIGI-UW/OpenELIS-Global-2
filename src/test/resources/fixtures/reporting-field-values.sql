-- Public synthetic export qualification; requires reporting-repeated-results.sql.
-- Copies only the explicitly identified synthetic baseline. No clinical rows are changed.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;
DO $fixture$
DECLARE
    specimen sample_item%ROWTYPE;
    ordered_sample sample%ROWTYPE;
    ordered_analysis analysis%ROWTYPE;
    subject_id numeric;
    test_id_value numeric;
    section_id numeric;
    option_id numeric;
    option_value text;
    field_kind text;
    fixture_accession text;
    identity_prefix text;
    expected_value text;
    result_identity uuid;
    occurrence integer;
BEGIN
    SELECT patient_id INTO STRICT subject_id FROM sample_human
      WHERE samp_id = (SELECT id FROM sample WHERE fhir_uuid = '47900000-0000-4000-8000-000000000002');
    FOREACH field_kind IN ARRAY ARRAY['A', 'D'] LOOP
        fixture_accession := CASE field_kind WHEN 'A' THEN 'REPORTING-MVP-TEXT' ELSE 'REPORTING-MVP-DICTIONARY' END;
        identity_prefix := CASE field_kind WHEN 'A' THEN '47910000-0000-4000-8000-00000000002' ELSE '47910000-0000-4000-8000-00000000003' END;
        SELECT id, test_section_id INTO STRICT test_id_value, section_id FROM test
          WHERE guid = CASE field_kind WHEN 'A' THEN 'b50d156e-0f6f-40cd-921c-4e831602a623'
            ELSE 'c1afd23c-c30f-42d7-af48-4321c069f48f' END AND is_active = 'Y';
        SELECT tr.id, tr.value INTO STRICT option_id, option_value FROM test_result tr
          WHERE tr.test_id = test_id_value AND tr.tst_rslt_type = field_kind
            AND (field_kind = 'A' OR EXISTS (SELECT 1 FROM dictionary d WHERE d.id::text=tr.value AND d.dict_entry='Positive'));
        expected_value := CASE field_kind WHEN 'A' THEN left(E'Operator says "repeat, please"\r\n<tag> & ' || repeat('untruncated text ',20),200)
          ELSE option_value END;
        IF NOT EXISTS (SELECT 1 FROM sample WHERE accession_number = fixture_accession) THEN
            SELECT * INTO STRICT ordered_sample FROM sample WHERE fhir_uuid = '47900000-0000-4000-8000-000000000002';
            ordered_sample.id := nextval('sample_seq');
            ordered_sample.fhir_uuid := (identity_prefix || '1')::uuid;
            ordered_sample.accession_number := fixture_accession;
            ordered_sample.entered_date := timestamp '2026-05-08 08:00:00';
            ordered_sample.collection_date := timestamp '2026-05-08 07:00:00';
            ordered_sample.received_date := timestamp '2026-05-08 09:00:00';
            ordered_sample.released_date := timestamp '2026-05-08 10:00:00';
            INSERT INTO sample SELECT ordered_sample.*;
            INSERT INTO sample_human(id,samp_id,patient_id,lastupdated)
              VALUES(nextval('sample_human_seq'),ordered_sample.id,subject_id,now());
            SELECT * INTO STRICT specimen FROM sample_item WHERE fhir_uuid = '47900000-0000-4000-8000-000000000003';
            specimen.id := nextval('sample_item_seq');
            specimen.fhir_uuid := (identity_prefix || '2')::uuid;
            specimen.samp_id := ordered_sample.id;
            specimen.collection_date := timestamp '2026-05-08 07:00:00';
            specimen.received_date := timestamptz '2026-05-08 09:00:00+00';
            INSERT INTO sample_item SELECT specimen.*;
            SELECT * INTO STRICT ordered_analysis FROM analysis WHERE fhir_uuid = '47900000-0000-4000-8000-000000000004';
            ordered_analysis.id := nextval('analysis_seq');
            ordered_analysis.fhir_uuid := (identity_prefix || '3')::uuid;
            ordered_analysis.sampitem_id := specimen.id;
            ordered_analysis.test_id := test_id_value;
            ordered_analysis.test_sect_id := section_id;
            ordered_analysis.completed_date := timestamp '2026-05-08 10:00:00';
            ordered_analysis.released_date := timestamp '2026-05-08 10:00:00';
            ordered_analysis.started_date := timestamp '2026-05-08 09:30:00';
            ordered_analysis.entry_date := timestamp '2026-05-08 09:30:00';
            INSERT INTO analysis SELECT ordered_analysis.*;
            FOR occurrence IN 4..5 LOOP
                result_identity := (identity_prefix || occurrence::text)::uuid;
                INSERT INTO result(id,fhir_uuid,analysis_id,test_result_id,sort_order,is_reportable,result_type,value,"grouping",lastupdated)
                  VALUES(nextval('result_seq'),result_identity,ordered_analysis.id,option_id,occurrence,'Y',field_kind,expected_value,0,now());
            END LOOP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM sample WHERE accession_number=fixture_accession AND fhir_uuid=(identity_prefix || '1')::uuid)
          OR (SELECT count(*) FROM result r JOIN analysis a ON a.id=r.analysis_id JOIN sample_item si ON si.id=a.sampitem_id
              JOIN sample s ON s.id=si.samp_id WHERE s.accession_number=fixture_accession
                AND si.collection_date::date=date '2026-05-08'
                AND r.value=expected_value AND r.result_type=field_kind AND r.test_result_id=option_id) <> 2 THEN
            RAISE EXCEPTION 'Synthetic field fixture identity or values do not match: %',fixture_accession;
        END IF;
    END LOOP;
END
$fixture$;
COMMIT;
