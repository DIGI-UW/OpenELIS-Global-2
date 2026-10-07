-- Interpret historical stage data only during cutover. Runtime eligibility uses
-- the explicit sample owner, retaining the original activity and clinical data.
UPDATE clinlims.micro_case_activity a SET result_source_sample_item_id=m.sample_item_id
FROM clinlims.micro_case_specimen m
WHERE m.case_id=a.case_id AND a.activity_type='STAGE_CHANGED'
    AND (CASE WHEN a.activity_type='STAGE_CHANGED' THEN a.structured_data::jsonb->>'to' END)
        IN ('POSITIVE_SIGNAL','GROWTH_DETECTED','NO_GROWTH_READY',
        'IDENTIFICATION','AST_READY','AST_IN_PROGRESS','REVIEW_READY','LOST_SPECIMEN_POSITIVE');

-- Some historical cases retain a current clinical state or released report
-- without an earlier stage activity. Attribute that existing evidence to the
-- sole original member using the cutover entry; do not invent a result or date.
UPDATE clinlims.micro_case_activity a SET result_source_sample_item_id=m.sample_item_id
FROM clinlims.micro_case_specimen m,clinlims.micro_case c
WHERE a.case_id=c.id AND m.case_id=c.id AND a.activity_type='V2_MIGRATED'
    AND (c.stage IN ('POSITIVE_SIGNAL','GROWTH_DETECTED','NO_GROWTH_READY','IDENTIFICATION',
        'AST_READY','AST_IN_PROGRESS','REVIEW_READY','LOST_SPECIMEN_POSITIVE',
        'PRELIM_RELEASED','FINAL_RELEASED','AMENDED')
        OR EXISTS (SELECT 1 FROM clinlims.micro_report_version v WHERE v.case_id=c.id));
