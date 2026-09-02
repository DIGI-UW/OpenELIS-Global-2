-- V1 absorption (OGC-608): one synthetic provider cycle per legacy
-- distribution that has no cycle link. Executed by qa/022's qa-073 changeset
-- and by EQAV1AbsorptionMigrationTest, so the migration and its test run the
-- same statement. Idempotent: only rows with cycle_id IS NULL are touched.
WITH legacy AS (
    SELECT d.id AS dist_id, d.eqa_program_id, d.distribution_name,
           d.distribution_date, d.deadline, d.status AS dist_status,
           d.created_by, d.sys_user_id,
           ROW_NUMBER() OVER (PARTITION BY d.eqa_program_id
                              ORDER BY d.distribution_date, d.id) AS seq_in_scheme
    FROM clinlims.eqa_distribution d
    WHERE d.cycle_id IS NULL
), numbered AS (
    SELECT l.*,
           COALESCE((SELECT MAX(c.cycle_number) FROM clinlims.eqa_cycle c
                     WHERE c.scheme_id = l.eqa_program_id), 0) + l.seq_in_scheme AS cycle_number,
           CASE l.dist_status
               WHEN 'PREPARED' THEN 'READY_TO_SHIP'
               WHEN 'SHIPPED' THEN 'SHIPPED'
               WHEN 'COMPLETED' THEN 'CLOSED'
               ELSE 'PLANNED'
           END AS cycle_status
    FROM legacy l
), created AS (
    INSERT INTO clinlims.eqa_cycle
        (id, fhir_uuid, scheme_id, cycle_number, cycle_name,
         planned_start_date, planned_end_date, status, created_by, sys_user_id)
    SELECT nextval('clinlims.eqa_cycle_seq'), gen_random_uuid(), n.eqa_program_id,
           n.cycle_number, n.distribution_name,
           n.distribution_date::date, n.deadline::date,
           n.cycle_status, n.created_by, n.sys_user_id
    FROM numbered n
    RETURNING id, scheme_id, cycle_number
), linked AS (
    -- The outer query cannot read rows the 'created' CTE inserted (same-statement
    -- snapshot), so the audit insert takes every field from RETURNING instead of
    -- re-reading eqa_cycle.
    UPDATE clinlims.eqa_distribution d
    SET cycle_id = c.id
    FROM created c
    JOIN numbered n
      ON n.eqa_program_id = c.scheme_id AND n.cycle_number = c.cycle_number
    WHERE d.id = n.dist_id
    RETURNING d.id AS dist_id, d.cycle_id, n.cycle_status, n.sys_user_id
)
INSERT INTO clinlims.eqa_cycle_state_transition
    (id, cycle_id, prior_state, new_state, state_machine, trigger_type,
     trigger_event, reason, occurred_at, sys_user_id)
SELECT nextval('clinlims.eqa_cycle_state_transition_seq'), l.cycle_id, NULL,
       l.cycle_status, 'PROVIDER', 'AUTO', 'V1_BACKFILL',
       'Cycle created from legacy distribution ' || l.dist_id, now(), l.sys_user_id
FROM linked l
