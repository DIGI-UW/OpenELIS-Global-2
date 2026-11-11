-- Rollback for qa-073 (OGC-608). The V1_BACKFILL audit rows identify the
-- synthetic cycles. The rollback refuses to run if any of them has been used
-- since the migration: a transition that is not the backfill row itself, or a
-- round, panel, roster, receipt, result, follow-up or shipping box hanging off
-- it. Deleting those would destroy real work, so the operator has to resolve
-- them first. Executed by the changeset and by EQAV1AbsorptionMigrationTest.
DO $$
DECLARE
    backfilled numeric[];
    blocked text;
BEGIN
    SELECT array_agg(DISTINCT t.cycle_id) INTO backfilled
    FROM clinlims.eqa_cycle_state_transition t
    WHERE t.trigger_event = 'V1_BACKFILL';

    IF backfilled IS NULL THEN
        RETURN;
    END IF;

    SELECT string_agg(c.id::text, ', ' ORDER BY c.id) INTO blocked
    FROM clinlims.eqa_cycle c
    WHERE c.id = ANY (backfilled)
      AND (EXISTS (SELECT 1 FROM clinlims.eqa_cycle_state_transition t
                   WHERE t.cycle_id = c.id AND t.trigger_event <> 'V1_BACKFILL')
        OR EXISTS (SELECT 1 FROM clinlims.eqa_round r WHERE r.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.eqa_panel p WHERE p.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.eqa_cycle_participant cp WHERE cp.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.eqa_panel_receipt pr WHERE pr.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.eqa_participant_result res WHERE res.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.eqa_participant_followup f WHERE f.cycle_id = c.id)
        OR EXISTS (SELECT 1 FROM clinlims.shipping_box b WHERE b.eqa_cycle_id = c.id));

    IF blocked IS NOT NULL THEN
        RAISE EXCEPTION 'V1 backfill rollback refused: cycles % have activity recorded after the migration', blocked;
    END IF;

    UPDATE clinlims.eqa_distribution SET cycle_id = NULL WHERE cycle_id = ANY (backfilled);
    DELETE FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ANY (backfilled);
    DELETE FROM clinlims.eqa_cycle WHERE id = ANY (backfilled);
END
$$
