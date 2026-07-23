-- Storage in-progress order seed — PR #3430 / storage-assign-order-label.spec.ts.
--
-- The storage-e2e.xml fixture intentionally omits <analysis> rows because
-- its author was unsure which test_id / status_id would exist at load time.
-- `PatientDashBoardProvider.ORDERS_IN_PROGRESS` returns analyses whose
-- status maps to AnalysisStatus.NotStarted (resolved via StatusService to
-- the status_of_sample row named "Not Tested"). Without at least one such
-- analysis linked to a known sample, the test's precondition check at
-- storage-assign-order-label.spec.ts:33-39 fails.
--
-- This fixture inserts exactly one analysis linked to sample_item 10001
-- (pre-existing in storage-e2e.xml), dynamically resolving the test row
-- and status row at load time so the SQL works against whatever tests are
-- seeded by ConfigurationInitializationService at app startup.
--
-- Idempotent: the insert is guarded by NOT EXISTS on sampitem_id and by
-- EXISTS on each FK lookup, so re-runs no-op and a stack missing the
-- prerequisites no-ops rather than fatally failing fixture load.

SET search_path TO clinlims;

INSERT INTO analysis
  (id, sampitem_id, test_id, status_id, analysis_type,
   entry_date, is_reportable, revision, lastupdated)
SELECT
  nextval('analysis_seq'),
  10001,
  (SELECT id FROM test WHERE is_active = 'Y' ORDER BY id LIMIT 1),
  (SELECT id FROM status_of_sample WHERE name = 'Not Tested' LIMIT 1),
  'MANUAL',
  NOW(),
  'N',
  0,
  NOW()
WHERE NOT EXISTS (
        SELECT 1 FROM analysis WHERE sampitem_id = 10001
      )
  AND EXISTS (SELECT 1 FROM sample_item WHERE id = 10001)
  AND EXISTS (SELECT 1 FROM test WHERE is_active = 'Y')
  AND EXISTS (SELECT 1 FROM status_of_sample WHERE name = 'Not Tested');
