-- Emergency manual repair for biorepository_qc_inspection schema.
-- Mirrors liquibase/3.5.x.x/068-biorepository-qc-inspection-schema-repair.xml
-- Run once against clinlims if Liquibase is blocked on startup.
-- Usage: psql -h localhost -p 15432 -U clinlims -d clinlims -f scripts/repair-biorepository-qc-inspection-schema.sql

BEGIN;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS expected_location_path TEXT;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS expected_position_coordinate VARCHAR(50);

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_action_type VARCHAR(50);

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_old_coordinate TEXT;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_new_coordinate TEXT;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_reason TEXT;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_by_user VARCHAR(36);

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD COLUMN IF NOT EXISTS correction_timestamp TIMESTAMP;

UPDATE clinlims.biorepository_qc_inspection
SET discrepancy_type = 'EMPTY_POSITION_REGISTERED'
WHERE discrepancy_type = 'EMPTY_POSITION_REGISTERED_OCCUPIED';

UPDATE clinlims.biorepository_qc_inspection
SET discrepancy_type = 'SAMPLE_MISSING'
WHERE discrepancy_type = 'MISSING_SAMPLE';

UPDATE clinlims.biorepository_qc_inspection
SET discrepancy_type = 'MISPLACED_SAMPLE_FOUND'
WHERE discrepancy_type = 'MISPLACED_ITEM';

UPDATE clinlims.biorepository_qc_inspection
SET discrepancy_type = 'LABELING_ERROR'
WHERE discrepancy_type = 'DAMAGED_LABEL';

ALTER TABLE clinlims.biorepository_qc_inspection
    DROP CONSTRAINT IF EXISTS check_discrepancy_type;

ALTER TABLE clinlims.biorepository_qc_inspection
    ADD CONSTRAINT check_discrepancy_type
    CHECK (
        discrepancy_type IS NULL OR discrepancy_type IN (
            'SAMPLE_MISSING',
            'WRONG_SAMPLE_IN_POSITION',
            'MISPLACED_SAMPLE_FOUND',
            'EMPTY_POSITION_REGISTERED',
            'EMPTY_POSITION_REGISTERED_OCCUPIED',
            'LABELING_ERROR',
            'BOX_RACK_MISPLACEMENT',
            'MISSING_SAMPLE',
            'DAMAGED_LABEL',
            'MISPLACED_ITEM',
            'CONTAINER_DAMAGE',
            'VOLUME_DISCREPANCY',
            'OTHER'
        )
    );

COMMIT;

-- Verify (expect 8 rows):
-- SELECT column_name
-- FROM information_schema.columns
-- WHERE table_schema = 'clinlims'
--   AND table_name = 'biorepository_qc_inspection'
--   AND column_name IN (
--     'expected_location_path', 'expected_position_coordinate',
--     'correction_action_type', 'correction_old_coordinate',
--     'correction_new_coordinate', 'correction_reason',
--     'correction_by_user', 'correction_timestamp'
--   );
