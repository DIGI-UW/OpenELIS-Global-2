-- Cross-table clinical transformation must be atomic with the structural cutover.
DO $cutover$
DECLARE
    snapshot jsonb;
    item record;
BEGIN
    -- These locks also exclude application writes while the preservation snapshot
    -- is taken. Liquibase's own lock only excludes other migrations.
    LOCK TABLE clinlims.micro_case, clinlims.micro_case_analysis, clinlims.micro_case_activity,
        clinlims.micro_case_order_detail, clinlims.micro_culture_setup, clinlims.test,
        clinlims.micro_ast_panel, clinlims.sample_item, clinlims.analysis, clinlims.result IN ACCESS EXCLUSIVE MODE;
    FOR item IN SELECT tablename FROM pg_tables WHERE schemaname='clinlims'
        AND tablename LIKE 'micro\_%' ESCAPE '\' ORDER BY tablename
    LOOP
        EXECUTE format('LOCK TABLE clinlims.%I IN ACCESS EXCLUSIVE MODE',item.tablename);
    END LOOP;

    -- Reject ownership that cannot be carried into canonical membership.
    IF EXISTS (SELECT analysis_id FROM clinlims.micro_case_analysis GROUP BY analysis_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'AMR cutover requires one owner for each analysis';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.micro_case_inoculation child
        JOIN clinlims.micro_case_inoculation parent ON parent.id=child.source_inoculation_id
        WHERE child.case_id <> parent.case_id) THEN
        RAISE EXCEPTION 'AMR cutover requires a subculture parent in the same case';
    END IF;

    IF EXISTS (
        SELECT 1 FROM clinlims.micro_case c
        LEFT JOIN clinlims.amr_cutover_case_map mapping ON mapping.case_id = c.id
        LEFT JOIN clinlims.test_section unit ON unit.id = mapping.test_section_id
        LEFT JOIN clinlims.program program ON program.id = mapping.program_id
        JOIN clinlims.sample_item specimen ON specimen.id = c.sample_item_id
        WHERE unit.id IS NULL OR program.id IS NULL OR specimen.samp_id IS NULL OR specimen.typeosamp_id IS NULL
    ) THEN
        RAISE EXCEPTION 'AMR cutover requires a Program and lab unit for every existing case';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.amr_cutover_case_map mapping
        LEFT JOIN clinlims.micro_case c ON c.id = mapping.case_id WHERE c.id IS NULL) THEN
        RAISE EXCEPTION 'AMR cutover mapping refers to an unknown case';
    END IF;
    IF EXISTS (SELECT 1 FROM clinlims.micro_case_analysis link
        JOIN clinlims.micro_case c ON c.id = link.case_id
        JOIN clinlims.analysis analysis ON analysis.id = link.analysis_id
        WHERE analysis.sampitem_id IS DISTINCT FROM c.sample_item_id) THEN
        RAISE EXCEPTION 'AMR cutover found conflicting case and analysis specimen ownership';
    END IF;
    IF '${amr.cutover.actorId}' !~ '^[0-9]+$' OR NOT EXISTS (
        SELECT 1 FROM clinlims.system_user WHERE id = '${amr.cutover.actorId}'::numeric
    ) THEN
        RAISE EXCEPTION 'AMR cutover requires an existing audit actor';
    END IF;
    -- Force date validation before any clinical changes.
    PERFORM '${amr.cutover.at}'::timestamp;
    IF EXISTS (SELECT 1 FROM clinlims.configuration_import_run WHERE id = 'amr-v2-cutover-20261006') THEN
        RAISE EXCEPTION 'AMR cutover history identifier already exists';
    END IF;

    snapshot := jsonb_build_object(
        'cases', (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case c),
        'tests', (SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'culture_workflow_type',culture_workflow_type)
            ORDER BY id),'[]'::jsonb) FROM clinlims.test),
        'panels', (SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'workflow_type',workflow_type)
            ORDER BY id),'[]'::jsonb) FROM clinlims.micro_ast_panel),
        'setups', (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_culture_setup c),
        'details', (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case_order_detail c),
        'clinicalFingerprint', pg_temp.amr_clinical_fingerprint());
    INSERT INTO clinlims.configuration_import_run(id,source,status,started_at,finished_at,summary,sys_user_id)
    VALUES ('amr-v2-cutover-20261006','AMR_V2_CUTOVER','EXPORTED','${amr.cutover.at}'::timestamp,
        '${amr.cutover.at}'::timestamp,snapshot::text,'${amr.cutover.actorId}'::numeric);

    ALTER TABLE clinlims.micro_case
        ADD COLUMN sample_id numeric(10), ADD COLUMN sample_type_id numeric(10),
        ADD COLUMN test_section_id numeric(10), ADD COLUMN program_id numeric(10),
        ADD COLUMN migration_review_required boolean NOT NULL DEFAULT false;
    UPDATE clinlims.micro_case c SET sample_id = specimen.samp_id, sample_type_id = specimen.typeosamp_id,
        test_section_id = mapping.test_section_id, program_id = mapping.program_id, migration_review_required = true
    FROM clinlims.sample_item specimen, clinlims.amr_cutover_case_map mapping
    WHERE c.sample_item_id = specimen.id AND mapping.case_id = c.id;
    ALTER TABLE clinlims.micro_case
        ALTER COLUMN sample_id SET NOT NULL, ALTER COLUMN sample_type_id SET NOT NULL,
        ALTER COLUMN test_section_id SET NOT NULL,
        ADD CONSTRAINT fk_micro_case_order FOREIGN KEY(sample_id) REFERENCES clinlims.sample(id),
        ADD CONSTRAINT fk_micro_case_type FOREIGN KEY(sample_type_id) REFERENCES clinlims.type_of_sample(id),
        ADD CONSTRAINT fk_micro_case_unit FOREIGN KEY(test_section_id) REFERENCES clinlims.test_section(id),
        ADD CONSTRAINT fk_micro_case_program FOREIGN KEY(program_id) REFERENCES clinlims.program(id);

    CREATE TABLE clinlims.micro_case_specimen (
        id varchar(36) PRIMARY KEY, case_id varchar(36) NOT NULL REFERENCES clinlims.micro_case(id),
        sample_item_id numeric(10) NOT NULL REFERENCES clinlims.sample_item(id),
        created_at timestamp NOT NULL, created_by varchar(20),
        lastupdated timestamp NOT NULL DEFAULT now(), last_updated timestamp,
        CONSTRAINT uq_micro_case_specimen UNIQUE(case_id,sample_item_id)
    );
    INSERT INTO clinlims.micro_case_specimen(id,case_id,sample_item_id,created_at,created_by,lastupdated,last_updated)
    SELECT md5('amr-v2-member:'||id)::uuid::text,id,sample_item_id,created_at,created_by,lastupdated,last_updated
    FROM clinlims.micro_case;
    INSERT INTO clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by,note,structured_data)
    SELECT md5('amr-v2-migration:'||id)::uuid::text,id,'V2_MIGRATED','${amr.cutover.at}'::timestamp,
        '${amr.cutover.actorId}', 'V2 cutover: identity retained; Program and lab unit explicitly mapped; review required',
        jsonb_build_object('previousWorkflow',workflow_type,'programId',program_id,'labUnitId',test_section_id,
            'sharedGroupingKey', EXISTS (SELECT 1 FROM clinlims.micro_case sibling WHERE sibling.id <> c.id
                AND sibling.sample_id=c.sample_id AND sibling.sample_type_id=c.sample_type_id
                AND sibling.test_section_id=c.test_section_id))::text
    FROM clinlims.micro_case c;

    ALTER TABLE clinlims.test ADD COLUMN opens_microbiology_case boolean NOT NULL DEFAULT false,
        ADD COLUMN microbiology_case_role varchar(20) NOT NULL DEFAULT 'DIRECT',
        ADD CONSTRAINT test_microbiology_case_role_chk CHECK(microbiology_case_role IN ('CULTURE','DIRECT','CASE'));
    -- The retired attribute explicitly marked culture-capable tests. Other tests
    -- retain the new switch's No / Direct defaults until the catalog is configured.
    UPDATE clinlims.test SET opens_microbiology_case=true,microbiology_case_role='CULTURE'
    WHERE culture_workflow_type IS NOT NULL;
    ALTER TABLE clinlims.test DROP COLUMN culture_workflow_type;
    ALTER TABLE clinlims.micro_ast_panel DROP COLUMN workflow_type;
    DROP TABLE clinlims.micro_culture_setup;
    ALTER TABLE clinlims.micro_case DROP COLUMN workflow_type, DROP COLUMN culture_method_id, DROP COLUMN sample_item_id;
    CREATE INDEX idx_micro_case_group ON clinlims.micro_case(sample_id,sample_type_id,test_section_id);
    CREATE INDEX idx_micro_case_specimen_item ON clinlims.micro_case_specimen(sample_item_id);

    -- Pre-case drafts become attributable cutover history, never a live second editor.
    DELETE FROM clinlims.micro_case_order_detail WHERE case_id IS NULL;
    ALTER TABLE clinlims.micro_case_order_detail DROP CONSTRAINT ck_micro_case_order_detail_owner,
        DROP COLUMN sample_id, DROP COLUMN culture_method_id, DROP COLUMN discarded_at, DROP COLUMN discarded_by,
        ALTER COLUMN case_id SET NOT NULL;
    DROP TABLE clinlims.amr_cutover_case_map;

    IF pg_temp.amr_clinical_fingerprint() <> snapshot->'clinicalFingerprint' THEN
        RAISE EXCEPTION 'AMR cutover changed retained clinical data';
    END IF;
    UPDATE clinlims.configuration_import_run SET summary = (snapshot || jsonb_build_object(
        'targetCases', (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case c),
        'targetMembers', (SELECT coalesce(jsonb_agg(to_jsonb(c) ORDER BY id),'[]'::jsonb) FROM clinlims.micro_case_specimen c),
        'targetMigrationActivities', (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY id),'[]'::jsonb)
            FROM clinlims.micro_case_activity a WHERE activity_type='V2_MIGRATED'),
        'targetTests', (SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'opens',opens_microbiology_case,
            'role',microbiology_case_role) ORDER BY id),'[]'::jsonb) FROM clinlims.test)
    ))::text WHERE id='amr-v2-cutover-20261006';
END;
$cutover$;
