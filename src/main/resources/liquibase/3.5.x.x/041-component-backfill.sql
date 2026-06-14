-- OGC-949 M1 / OGC-937 — result-component backfill (idempotent).
-- Executed by changeset 041-result-components.xml (sqlFile) AND by
-- ComponentBackfillMigrationTest (single source of truth).
--
-- 1) One PRIMARY component per test, copying the legacy per-test shape:
--    uom from TEST.UOM_ID; result type + significant digits from the test's
--    first TEST_RESULT row (legacy tests have a single result type).
INSERT INTO clinlims.test_result_component
    (id, test_id, code, label, display_order, result_type, uom_id,
     significant_digits, allow_multiple_readings, is_active, lastupdated)
SELECT gen_random_uuid()::varchar, t.id, 'PRIMARY', t.name, 0,
       (SELECT tr.tst_rslt_type FROM clinlims.test_result tr
         WHERE tr.test_id = t.id AND tr.tst_rslt_type IS NOT NULL
         ORDER BY tr.id LIMIT 1),
       t.uom_id,
       (SELECT tr.significant_digits FROM clinlims.test_result tr
         WHERE tr.test_id = t.id AND tr.significant_digits IS NOT NULL
         ORDER BY tr.id LIMIT 1),
       false, 'Y', now()
  FROM clinlims.test t
 WHERE NOT EXISTS (SELECT 1 FROM clinlims.test_result_component c
                    WHERE c.test_id = t.id AND c.code = 'PRIMARY');

-- 2) Repoint legacy range rows (FRS alias test_range -> RESULT_LIMITS).
UPDATE clinlims.result_limits rl
   SET component_id = c.id
  FROM clinlims.test_result_component c
 WHERE c.test_id = rl.test_id AND c.code = 'PRIMARY'
   AND rl.component_id IS NULL;

-- 3) Repoint legacy result options (FRS alias test_select_list_option -> TEST_RESULT).
UPDATE clinlims.test_result tr
   SET component_id = c.id
  FROM clinlims.test_result_component c
 WHERE c.test_id = tr.test_id AND c.code = 'PRIMARY'
   AND tr.component_id IS NULL;
