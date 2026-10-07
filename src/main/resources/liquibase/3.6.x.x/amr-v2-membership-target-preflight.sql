-- Run before the structural cutover as well as standalone membership migration.
-- A later changeset failure cannot roll back an already committed cutover.
DO $targets$
DECLARE
    collision text;
BEGIN
    LOCK TABLE clinlims.configuration_import_run IN ACCESS EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM clinlims.configuration_import_run WHERE id='amr-v2-membership-20261006') THEN
        RAISE EXCEPTION 'AMR membership migration history identifier already exists';
    END IF;
    SELECT c.table_name || '.' || c.column_name INTO collision
    FROM information_schema.columns c
    JOIN (VALUES
        ('micro_case_activity','result_source_sample_item_id'),
        ('test','collected_in_sets'),
        ('micro_case_analysis','case_role'),
        ('micro_case_analysis','collected_in_sets'),
        ('micro_isolate','source_sample_item_id'),
        ('micro_case_inoculation','source_sample_item_id')
    ) AS target(table_name,column_name)
      ON c.table_name=target.table_name AND c.column_name=target.column_name
    WHERE c.table_schema='clinlims' LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR membership target column already exists: %', collision;
    END IF;
    SELECT relname INTO collision FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='clinlims' AND relname IN (
        'idx_micro_activity_result_source','idx_micro_isolate_source',
        'uq_micro_case_analysis_owner','uq_micro_culture_specimen') LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR membership target relation already exists: %', collision;
    END IF;
    SELECT conname INTO collision FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace
    WHERE n.nspname='clinlims' AND conname IN (
        'fk_micro_activity_result_source','fk_micro_isolate_source_member',
        'fk_micro_culture_source_member','fk_micro_subculture_source_member',
        'uq_micro_case_analysis_owner','uq_micro_culture_specimen') LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR membership target constraint already exists: %', collision;
    END IF;
END;
$targets$;
