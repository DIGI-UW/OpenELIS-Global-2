-- Validate the later Program/export migration before structural cutover commits.
DO $targets$
DECLARE
    collision text;
BEGIN
    LOCK TABLE clinlims.program IN ACCESS EXCLUSIVE MODE;
    SELECT column_name INTO collision FROM information_schema.columns
    WHERE table_schema='clinlims' AND table_name='program'
      AND column_name IN ('show_on_microbiology_case','reporting_track_id') LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR Program target column already exists: %', collision;
    END IF;
    SELECT relname INTO collision FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='clinlims' AND relname IN (
        'micro_export_reporting_track','micro_export_reporting_track_pkey',
        'uq_micro_export_reporting_track') LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR Program/export target relation already exists: %', collision;
    END IF;
    SELECT conname INTO collision FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace
    WHERE n.nspname='clinlims' AND conname IN (
        'fk_program_reporting_track','fk_micro_export_reporting_track',
        'micro_export_reporting_track_pkey','uq_micro_export_reporting_track') LIMIT 1;
    IF collision IS NOT NULL THEN
        RAISE EXCEPTION 'AMR Program/export target constraint already exists: %', collision;
    END IF;
END;
$targets$;
