DO $preflight$
BEGIN
    LOCK TABLE clinlims.micro_case, clinlims.micro_case_analysis, clinlims.micro_case_specimen,
        clinlims.micro_isolate, clinlims.analysis, clinlims.test IN ACCESS EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM clinlims.micro_case c WHERE
        (c.stage IN ('POSITIVE_SIGNAL','GROWTH_DETECTED','NO_GROWTH_READY','IDENTIFICATION',
            'AST_READY','AST_IN_PROGRESS','REVIEW_READY','LOST_SPECIMEN_POSITIVE',
            'PRELIM_RELEASED','FINAL_RELEASED','AMENDED')
         OR EXISTS (SELECT 1 FROM clinlims.micro_report_version v WHERE v.case_id=c.id)
         OR EXISTS (SELECT 1 FROM clinlims.micro_case_activity a WHERE a.case_id=c.id
            AND (CASE WHEN a.activity_type='STAGE_CHANGED' THEN a.structured_data::jsonb->>'to' END) IN
            ('POSITIVE_SIGNAL','GROWTH_DETECTED','NO_GROWTH_READY','IDENTIFICATION',
             'AST_READY','AST_IN_PROGRESS','REVIEW_READY','LOST_SPECIMEN_POSITIVE')))
        AND (SELECT count(*) FROM clinlims.micro_case_specimen m WHERE m.case_id=c.id) <> 1) THEN
        RAISE EXCEPTION 'AMR membership migration requires an unambiguous source specimen for each existing culture observation';
    END IF;
    IF EXISTS (SELECT analysis_id FROM clinlims.micro_case_analysis GROUP BY analysis_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'AMR membership migration requires one owner for each analysis';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.micro_isolate i WHERE
        (SELECT count(*) FROM clinlims.micro_case_specimen m WHERE m.case_id=i.case_id) <> 1) THEN
        RAISE EXCEPTION 'AMR membership migration requires an unambiguous source specimen for each existing isolate';
    END IF;
    LOCK TABLE clinlims.micro_case_inoculation IN ACCESS EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM clinlims.micro_case_inoculation i WHERE
        (SELECT count(*) FROM clinlims.micro_case_specimen m WHERE m.case_id=i.case_id) <> 1) THEN
        RAISE EXCEPTION 'AMR membership migration requires an unambiguous source specimen for each existing culture';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.micro_case_inoculation child
        JOIN clinlims.micro_case_inoculation parent ON parent.id=child.source_inoculation_id
        WHERE child.case_id <> parent.case_id) THEN
        RAISE EXCEPTION 'AMR membership migration requires a subculture parent in the same case';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.configuration_import_run WHERE id='amr-v2-membership-20261006') THEN
        RAISE EXCEPTION 'AMR membership migration history identifier already exists';
    END IF;
END;
$preflight$;
