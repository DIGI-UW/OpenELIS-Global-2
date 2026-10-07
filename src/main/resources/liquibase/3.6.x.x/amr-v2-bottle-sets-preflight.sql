DO $$ BEGIN
    LOCK TABLE clinlims.sample_item, clinlims.sample_type_request IN ACCESS EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'clinlims'
        AND table_name IN ('sample_item', 'sample_type_request') AND column_name = 'culture_set_number') THEN
        RAISE EXCEPTION 'AMR bottle-set target column culture_set_number already exists';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'clinlims'
        AND table_name = 'sample_type_request' AND column_name IN ('container', 'body_site', 'collection_date', 'collection_time')) THEN
        RAISE EXCEPTION 'AMR requested bottle-detail target columns already exist';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname IN
        ('ck_request_culture_set_positive', 'ck_sample_culture_set_positive')) THEN
        RAISE EXCEPTION 'AMR bottle-set target constraint already exists';
    END IF;
END $$;
