-- Public synthetic queue states for the same May 5 repeat workflow.
-- Run only on a development/UAT database, supplying the current source JSON as
-- the psql source_definition variable. Existing stable records are never reset.
\set ON_ERROR_STOP on
-- Optional fresh failed-only fixture; defaults preserve existing CI setup.
\if :{?failed_job_id}
\else
\set failed_job_id '47900000-0000-4000-8000-000000000101'
\endif
\if :{?failed_only}
\else
\set failed_only false
\endif
BEGIN;
SET LOCAL search_path TO clinlims, public;
SELECT set_config('reporting.fixture_definition', :'source_definition', true) AS fixture_definition \gset
SELECT set_config('reporting.failed_job_id', :'failed_job_id', true) AS failed_job_id_setting \gset
SELECT set_config('reporting.failed_only', :'failed_only', true) AS failed_only_setting \gset
DO $fixture$
DECLARE
    source_definition jsonb := current_setting('reporting.fixture_definition')::jsonb;
    test_id_value text;
    section_id_value text;
    status_id_value text;
    owner_id_value text;
    snapshot jsonb;
    fixture_id text;
    fixture_state text;
    fixture_client_key text;
BEGIN
    SELECT id::text, test_section_id::text INTO STRICT test_id_value, section_id_value
      FROM test WHERE guid = 'b50d156e-0f6f-40cd-921c-4e831602a623' AND is_active = 'Y';
    SELECT id::text INTO STRICT status_id_value FROM status_of_sample
      WHERE display_key = 'status.test.valid' AND status_type = 'ANALYSIS' AND is_active = 'Y';
    SELECT id::text INTO STRICT owner_id_value FROM system_user WHERE login_name = 'admin';
    IF NOT EXISTS (SELECT 1 FROM sample WHERE accession_number = 'REPORTING-MVP-REPEAT'
        AND fhir_uuid = '47900000-0000-4000-8000-000000000002') THEN
        RAISE EXCEPTION 'Load the synthetic reporting repeat specimen before queue fixtures';
    END IF;
    snapshot := jsonb_build_object(
      'definition', source_definition, 'layout', 'SPREADSHEET', 'timezone', 'UTC',
      'variables', jsonb_build_array(
        jsonb_build_object('id','accessionNumber','label','Accession Number','type','text','group','sample',
          'measurement',false,'layouts',jsonb_build_array('SPREADSHEET','RESULT_LIST')),
        jsonb_build_object('id','test:' || test_id_value,'label','Viral Load','type','result','group','tests',
          'measurement',true,'layouts',jsonb_build_array('SPREADSHEET','RESULT_LIST'))),
      'filterSpec', jsonb_build_object('dateFrom','2026-05-05','dateTo','2026-05-05',
        'labSectionIds',jsonb_build_array(section_id_value),'testIds',jsonb_build_array(test_id_value),
        'resultStatuses',jsonb_build_array('FINALIZED')),
      'statusIds', jsonb_build_array(status_id_value));
    FOR fixture_id, fixture_state IN SELECT * FROM (VALUES
        (current_setting('reporting.failed_job_id')::uuid::text,'FAILED'),
        ('47900000-0000-4000-8000-000000000102','EXPIRED')) AS fixtures(id, state)
        WHERE state = 'FAILED' OR NOT current_setting('reporting.failed_only')::boolean LOOP
        fixture_client_key := 'synthetic-recovery-' || fixture_state;
        IF fixture_state = 'FAILED' AND fixture_id <> '47900000-0000-4000-8000-000000000101' THEN
            fixture_client_key := 'synthetic-recovery-' || fixture_id;
        END IF;
        IF EXISTS (SELECT 1 FROM reporting_export_job WHERE id = fixture_id
            AND (client_request_id <> fixture_client_key OR owner_id <> owner_id_value)) THEN
            RAISE EXCEPTION 'Reporting queue fixture identity is already in use';
        END IF;
        INSERT INTO reporting_export_job(id, owner_id, client_request_id, source_id, layout, request_json,
          request_hash, state, submitted_at, started_at, completed_at, expires_at, failure_code, last_updated)
        VALUES (fixture_id, owner_id_value, fixture_client_key, 'SAMPLE_TESTING',
          'SPREADSHEET', snapshot::text, md5(snapshot::text), fixture_state,
          now() - interval '8 days', now() - interval '8 days', now() - interval '8 days',
          CASE WHEN fixture_state = 'EXPIRED' THEN now() - interval '1 day' END,
          CASE WHEN fixture_state = 'FAILED' THEN 'reporting.job.interrupted' END, now())
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END;
$fixture$;
COMMIT;
