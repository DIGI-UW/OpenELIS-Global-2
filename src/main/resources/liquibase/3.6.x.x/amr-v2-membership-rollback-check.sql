DO $rollback_check$
DECLARE
    snapshot jsonb;
    item record;
BEGIN
    LOCK TABLE clinlims.test, clinlims.analysis, clinlims.result IN ACCESS EXCLUSIVE MODE;
    FOR item IN SELECT tablename FROM pg_tables WHERE schemaname='clinlims'
        AND tablename LIKE 'micro\_%' ESCAPE '\' ORDER BY tablename
    LOOP
        EXECUTE format('LOCK TABLE clinlims.%I IN ACCESS EXCLUSIVE MODE',item.tablename);
    END LOOP;
    SELECT summary::jsonb INTO STRICT snapshot FROM clinlims.configuration_import_run
        WHERE id='amr-v2-membership-20261006' AND source='AMR_V2_MEMBERSHIP';
    IF pg_temp.amr_clinical_fingerprint() <> snapshot->'clinicalFingerprint'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY id),'[]'::jsonb)
           FROM clinlims.micro_case_activity a) <> snapshot->'activities'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb)
           FROM clinlims.micro_case c) <> snapshot->'cases'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(m) ORDER BY id),'[]'::jsonb)
           FROM clinlims.micro_case_specimen m) <> snapshot->'members'
       OR (SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'opens',opens_microbiology_case,
            'role',microbiology_case_role,'sets',collected_in_sets) ORDER BY id),'[]'::jsonb)
           FROM clinlims.test) <> snapshot->'catalog' THEN
        RAISE EXCEPTION 'AMR membership rollback refused: clinical work or catalog changed after migration';
    END IF;
END;
$rollback_check$;
