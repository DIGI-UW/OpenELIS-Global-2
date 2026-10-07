DO $observation_preflight$
DECLARE
    activity record;
    transition jsonb;
BEGIN
    LOCK TABLE clinlims.micro_case,clinlims.micro_case_activity IN ACCESS EXCLUSIVE MODE;
    FOR activity IN SELECT id,structured_data FROM clinlims.micro_case_activity
        WHERE activity_type='STAGE_CHANGED'
    LOOP
        BEGIN
            transition := activity.structured_data::jsonb;
        EXCEPTION WHEN invalid_text_representation THEN
            RAISE EXCEPTION 'AMR cutover requires resolvable stage history: %',activity.id;
        END;
        IF transition->>'to' IS NULL OR transition->>'to' NOT IN
            ('RECEIVED','SETUP_RECORDED','INCUBATING','POSITIVE_SIGNAL','GROWTH_DETECTED','NO_GROWTH_READY',
             'IDENTIFICATION','AST_READY','AST_IN_PROGRESS','REVIEW_READY','PRELIM_RELEASED','FINAL_RELEASED',
             'AMENDED','REJECTED','LOST_SPECIMEN','LOST_SPECIMEN_POSITIVE') THEN
            RAISE EXCEPTION 'AMR cutover requires resolvable stage history: %',activity.id;
        END IF;
    END LOOP;
END;
$observation_preflight$;
