-- Public synthetic Referral UAT records; requires reporting-repeated-results.sql.
-- May 7 sent-date period: two returned readings (450 each) and one pending row.
-- A draft and a May 8 referral are deliberately outside that period.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;
DO $fixture$
DECLARE
    analysis_value numeric;
    test_value numeric;
    type_value numeric;
    referral_value numeric;
    result_value numeric;
    fixture_uuid uuid;
    fixture_index integer;
    reading_index integer;
BEGIN
    SELECT id, test_id INTO STRICT analysis_value, test_value FROM analysis
      WHERE fhir_uuid = '47900000-0000-4000-8000-000000000004';
    SELECT id INTO type_value FROM referral_type WHERE name = 'Reporting UAT';
    IF type_value IS NULL THEN
        type_value := nextval('referral_type_seq');
        INSERT INTO referral_type (id, name, lastupdated)
          VALUES (type_value, 'Reporting UAT', timestamp '2026-05-07 12:00:00');
    END IF;
    FOR fixture_index IN 1..4 LOOP
        fixture_uuid := ('47900000-0000-4000-8000-' || lpad((100 + fixture_index)::text, 12, '0'))::uuid;
        SELECT id INTO referral_value FROM referral WHERE fhir_uuid = fixture_uuid;
        IF referral_value IS NULL THEN
            referral_value := nextval('referral_seq');
            INSERT INTO referral (id, fhir_uuid, analysis_id, referral_type_id, organization_name,
              referral_request_date, sent_date, status, lastupdated)
              VALUES (referral_value, fixture_uuid, analysis_value, type_value, 'Synthetic Reference Lab',
                timestamp '2026-05-05 10:00:00',
                CASE fixture_index WHEN 3 THEN NULL WHEN 4 THEN timestamp '2026-05-08 12:00:00'
                  ELSE timestamp '2026-05-07 12:00:00' END,
                CASE fixture_index WHEN 1 THEN 'COMPLETED' WHEN 3 THEN 'DRAFT' ELSE 'REQUESTED' END,
                timestamp '2026-05-07 12:00:00');
        END IF;
        IF fixture_index = 1 THEN
            FOR reading_index IN 0..1 LOOP
                SELECT id INTO STRICT result_value FROM result WHERE fhir_uuid =
                  CASE reading_index WHEN 0 THEN '47900000-0000-4000-8000-000000000005'::uuid
                    ELSE '47900000-0000-4000-8000-000000000006'::uuid END;
                IF NOT EXISTS (SELECT 1 FROM referral_result WHERE referral_id = referral_value AND result_id = result_value) THEN
                    INSERT INTO referral_result (id, referral_id, test_id, result_id, referral_report_date, lastupdated)
                      VALUES (nextval('referral_result_seq'), referral_value, test_value, result_value,
                        timestamp '2026-05-08 12:00:00' + reading_index * interval '1 day', timestamp '2026-05-09 12:00:00');
                END IF;
            END LOOP;
        END IF;
    END LOOP;
END $fixture$;
COMMIT;
