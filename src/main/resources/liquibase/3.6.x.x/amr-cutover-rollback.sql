-- A rollback is a pre-runtime rehearsal operation. Refuse it after new clinical
-- work rather than erase records or guess how to represent them in the old model.
DO $rollback$
DECLARE
    snapshot jsonb;
    item record;
BEGIN
    LOCK TABLE clinlims.micro_case, clinlims.micro_case_specimen, clinlims.micro_case_activity,
        clinlims.micro_case_order_detail, clinlims.test, clinlims.micro_ast_panel,
        clinlims.analysis, clinlims.result IN ACCESS EXCLUSIVE MODE;
    FOR item IN SELECT tablename FROM pg_tables WHERE schemaname='clinlims'
        AND tablename LIKE 'micro\_%' ESCAPE '\' ORDER BY tablename
    LOOP
        EXECUTE format('LOCK TABLE clinlims.%I IN ACCESS EXCLUSIVE MODE',item.tablename);
    END LOOP;
    SELECT summary::jsonb INTO STRICT snapshot FROM clinlims.configuration_import_run
        WHERE id='amr-v2-cutover-20261006' AND source='AMR_V2_CUTOVER';
    IF pg_temp.amr_clinical_fingerprint() <> snapshot->'clinicalFingerprint'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case c) <> snapshot->'targetCases'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case_specimen c) <> snapshot->'targetMembers'
       OR (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case_activity a
           WHERE activity_type='V2_MIGRATED') <> snapshot->'targetMigrationActivities'
       OR (SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'opens',opens_microbiology_case,
            'role',microbiology_case_role) ORDER BY id),'[]'::jsonb) FROM clinlims.test) <> snapshot->'targetTests' THEN
        RAISE EXCEPTION 'AMR rollback refused: clinical work changed after cutover';
    END IF;

    ALTER TABLE clinlims.micro_case ADD COLUMN sample_item_id numeric(10), ADD COLUMN workflow_type varchar(40),
        ADD COLUMN culture_method_id numeric(10);
    UPDATE clinlims.micro_case c SET sample_item_id=old.sample_item_id,workflow_type=old.workflow_type,culture_method_id=old.culture_method_id
    FROM jsonb_to_recordset(snapshot->'cases') AS old(id varchar(36),sample_item_id numeric,workflow_type text,culture_method_id numeric)
    WHERE c.id=old.id;
    ALTER TABLE clinlims.micro_case ALTER COLUMN sample_item_id SET NOT NULL, ALTER COLUMN workflow_type SET NOT NULL,
        ADD CONSTRAINT fk_micro_case_sample_item FOREIGN KEY(sample_item_id) REFERENCES clinlims.sample_item(id),
        ADD CONSTRAINT fk_micro_case_method FOREIGN KEY(culture_method_id) REFERENCES clinlims.method(id),
        ADD CONSTRAINT uq_micro_case_sample_workflow UNIQUE(sample_item_id,workflow_type),
        ADD CONSTRAINT micro_case_workflow_type_chk CHECK(workflow_type IN ('BACTERIOLOGY','MYCOBACTERIOLOGY_TB','MYCOLOGY','UNASSIGNED'));
    CREATE INDEX idx_micro_case_sample_item ON clinlims.micro_case(sample_item_id);

    ALTER TABLE clinlims.test ADD COLUMN culture_workflow_type varchar(40);
    UPDATE clinlims.test t SET culture_workflow_type=old.culture_workflow_type
    FROM jsonb_to_recordset(snapshot->'tests') AS old(id numeric,culture_workflow_type text) WHERE t.id=old.id;
    ALTER TABLE clinlims.test ADD CONSTRAINT test_culture_workflow_type_chk
        CHECK(culture_workflow_type IS NULL OR culture_workflow_type IN ('BACTERIOLOGY','MYCOBACTERIOLOGY_TB','MYCOLOGY'));
    ALTER TABLE clinlims.micro_ast_panel ADD COLUMN workflow_type varchar(40);
    UPDATE clinlims.micro_ast_panel p SET workflow_type=old.workflow_type
    FROM jsonb_to_recordset(snapshot->'panels') AS old(id varchar(36),workflow_type text) WHERE p.id=old.id;
    ALTER TABLE clinlims.micro_ast_panel ALTER COLUMN workflow_type SET NOT NULL,
        ADD CONSTRAINT micro_ast_panel_workflow_type_chk CHECK(workflow_type IN ('BACTERIOLOGY','MYCOBACTERIOLOGY_TB','MYCOLOGY'));

    CREATE TABLE clinlims.micro_culture_setup (
        id varchar(36) CONSTRAINT pk_micro_culture_setup PRIMARY KEY,
        method_id numeric(10) NOT NULL, name varchar(255) NOT NULL, workflow_type varchar(40) NOT NULL,
        media_defaults text, incubation_defaults text, atmosphere_defaults text,
        is_active varchar(2) NOT NULL DEFAULT 'Y',lastupdated timestamp NOT NULL DEFAULT now(),last_updated timestamp,
        reportable_test_analyte_id numeric(10),last_updated_by varchar(20),
        incubation_hours integer,subculture_at_hours integer,max_incubation_days integer,
        CONSTRAINT fk_micro_culture_setup_method FOREIGN KEY(method_id) REFERENCES clinlims.method(id),
        CONSTRAINT fk_micro_culture_setup_reportable_analyte FOREIGN KEY(reportable_test_analyte_id) REFERENCES clinlims.test_analyte(id),
        CONSTRAINT uq_micro_culture_setup_method_workflow UNIQUE(method_id,workflow_type),
        CONSTRAINT micro_culture_setup_workflow_type_chk CHECK(workflow_type IN ('BACTERIOLOGY','MYCOBACTERIOLOGY_TB','MYCOLOGY')),
        CONSTRAINT micro_culture_setup_incubation_hours_chk CHECK(incubation_hours IS NULL OR incubation_hours>0),
        CONSTRAINT micro_culture_setup_subculture_hours_chk CHECK(subculture_at_hours IS NULL OR subculture_at_hours>0),
        CONSTRAINT micro_culture_setup_max_days_chk CHECK(max_incubation_days IS NULL OR max_incubation_days>0)
    );
    INSERT INTO clinlims.micro_culture_setup SELECT * FROM jsonb_populate_recordset(NULL::clinlims.micro_culture_setup,snapshot->'setups');

    ALTER TABLE clinlims.micro_case_order_detail ALTER COLUMN case_id DROP NOT NULL,
        ADD COLUMN sample_id numeric(10),ADD COLUMN culture_method_id varchar(20),
        ADD COLUMN discarded_at timestamp,ADD COLUMN discarded_by varchar(20);
    UPDATE clinlims.micro_case_order_detail detail SET sample_id=old.sample_id,culture_method_id=old.culture_method_id,
        discarded_at=old.discarded_at,discarded_by=old.discarded_by
    FROM jsonb_to_recordset(snapshot->'details') AS old(id varchar(36),sample_id numeric,culture_method_id text,discarded_at timestamp,discarded_by text)
    WHERE detail.id=old.id;
    INSERT INTO clinlims.micro_case_order_detail
    SELECT old.* FROM jsonb_populate_recordset(NULL::clinlims.micro_case_order_detail,snapshot->'details') old WHERE old.case_id IS NULL;
    ALTER TABLE clinlims.micro_case_order_detail
        ADD CONSTRAINT ck_micro_case_order_detail_owner CHECK((case_id IS NULL) <> (sample_id IS NULL)),
        ADD CONSTRAINT uq_micro_case_order_detail_sample UNIQUE(sample_id),
        ADD CONSTRAINT fk_micro_case_order_detail_sample FOREIGN KEY(sample_id) REFERENCES clinlims.sample(id);

    DELETE FROM clinlims.micro_case_activity WHERE activity_type='V2_MIGRATED'
        AND id IN (SELECT md5('amr-v2-migration:'||old.id)::uuid::text FROM jsonb_to_recordset(snapshot->'cases') AS old(id text));
    DROP TABLE clinlims.micro_case_specimen;
    ALTER TABLE clinlims.micro_case DROP COLUMN sample_id,DROP COLUMN sample_type_id,DROP COLUMN test_section_id,
        DROP COLUMN program_id,DROP COLUMN migration_review_required;
    ALTER TABLE clinlims.test DROP COLUMN opens_microbiology_case,DROP COLUMN microbiology_case_role;
    DELETE FROM clinlims.configuration_import_run WHERE id='amr-v2-cutover-20261006';
END;
$rollback$;
