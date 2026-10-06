-- Session-local migration helper. It is never installed in the application schema.
CREATE OR REPLACE FUNCTION pg_temp.amr_clinical_fingerprint() RETURNS jsonb
LANGUAGE plpgsql AS $fingerprint$
DECLARE
    item record;
    expression text;
    predicate text;
    digest text;
    fingerprints jsonb := '{}'::jsonb;
BEGIN
    FOR item IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'clinlims'
          AND tablename LIKE 'micro\_%' ESCAPE '\'
          AND tablename NOT IN ('micro_culture_setup', 'micro_case_specimen')
        ORDER BY tablename
    LOOP
        expression := 'to_jsonb(r)';
        predicate := '';
        IF item.tablename = 'micro_case' THEN
            expression := expression || ' - ARRAY[''workflow_type'',''sample_item_id'',''culture_method_id'',
                ''sample_id'',''sample_type_id'',''test_section_id'',''program_id'',''migration_review_required'']';
        ELSIF item.tablename = 'micro_ast_panel' THEN
            expression := expression || ' - ''workflow_type''';
        ELSIF item.tablename = 'micro_case_order_detail' THEN
            expression := expression || ' - ARRAY[''sample_id'',''culture_method_id'',''discarded_at'',''discarded_by'']';
            predicate := ' WHERE case_id IS NOT NULL';
        ELSIF item.tablename = 'micro_case_activity' THEN
            predicate := ' WHERE activity_type <> ''V2_MIGRATED''';
        END IF;
        EXECUTE format('SELECT md5(coalesce(string_agg((%s)::text,''|'' ORDER BY id),'''')) FROM clinlims.%I r%s',
            expression, item.tablename, predicate) INTO digest;
        fingerprints := fingerprints || jsonb_build_object(item.tablename, digest);
    END LOOP;
    FOR item IN SELECT unnest(ARRAY['analysis','result']) AS tablename LOOP
        IF item.tablename='analysis' THEN
            predicate := ' WHERE id IN (SELECT analysis_id FROM clinlims.micro_case_analysis)';
        ELSE
            predicate := ' WHERE analysis_id IN (SELECT analysis_id FROM clinlims.micro_case_analysis)';
        END IF;
        EXECUTE format('SELECT md5(coalesce(string_agg(to_jsonb(r)::text,''|'' ORDER BY id),'''')) FROM clinlims.%I r%s',
            item.tablename,predicate) INTO digest;
        fingerprints := fingerprints || jsonb_build_object(item.tablename,digest);
    END LOOP;
    RETURN fingerprints;
END;
$fingerprint$;
