DO $$ BEGIN
    IF to_regclass('clinlims.micro_case_requested_test') IS NOT NULL
       OR to_regclass('clinlims.idx_micro_request_case') IS NOT NULL
       OR to_regclass('clinlims.uq_micro_request_test') IS NOT NULL THEN
        RAISE EXCEPTION 'AMR requested-test membership target table already exists';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname IN
        ('pk_micro_case_requested_test', 'uq_micro_request_test', 'fk_micro_request_case',
         'fk_micro_request_request', 'fk_micro_request_test', 'ck_micro_request_role', 'ck_micro_request_cancellation')) THEN
        RAISE EXCEPTION 'AMR requested-test membership target constraint already exists';
    END IF;
END $$;
