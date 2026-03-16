SET search_path TO clinlims;

-- ============================================================================
-- E2E Cleanup + Dashboard Preparation
--
-- Purpose: (1) Clean up stale/out-of-scope analyzers for a clean dashboard,
--          (2) Deactivate all non-generic analyzer types.
--
-- Analyzer creation is handled by seed-analyzers.sh via REST API, NOT SQL.
-- ============================================================================

-- 1. Clean up stale E2E analyzers created by Playwright (timestamped names)
DELETE FROM analyzer_results
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE '%E2E %' OR name LIKE 'E2E-FILE-%');
DELETE FROM notebook_analysers
WHERE analyser_id IN (SELECT id FROM analyzer WHERE name LIKE '%E2E %' OR name LIKE 'E2E-FILE-%');
DELETE FROM analyzer WHERE name LIKE '%E2E %';
DELETE FROM analyzer WHERE name LIKE 'E2E-FILE-%';

-- 2. Clean up legacy 1000-series analyzers (Feature 004, fully retired)
DELETE FROM analyzer_results WHERE analyzer_id IN (1000,1001,1002,1003,1004);
DELETE FROM notebook_analysers WHERE analyser_id IN (1000,1001,1002,1003,1004);
DELETE FROM analyzer WHERE id IN (1000,1001,1002,1003,1004);

-- 3. Clean up old fixture-loaded analyzers (replaced by REST API seeding)
DELETE FROM analyzer_test_map WHERE analyzer_id IN ('2013', '2014', '3001', '3002', '3003');
DELETE FROM file_import_configuration WHERE analyzer_id IN (3001, 3002, 3003);
DELETE FROM analyzer_results WHERE analyzer_id IN (2013, 2014, 3001, 3002, 3003);
DELETE FROM notebook_analysers WHERE analyser_id IN (2013, 2014, 3001, 3002, 3003);
DELETE FROM analyzer WHERE id IN (2013, 2014, 3001, 3002, 3003);

-- 4. Clean up Mindray analyzers from 011 fixtures (not in current UAT scope)
DELETE FROM analyzer_results WHERE analyzer_id IN (2006,2007,2008,2012);
DELETE FROM notebook_analysers WHERE analyser_id IN (2006,2007,2008,2012);
DELETE FROM analyzer WHERE id IN (2006,2007,2008,2012);

-- 5. Deactivate all legacy (non-generic) analyzer types for clean dashboard
UPDATE analyzer_type SET is_active = false
WHERE name NOT IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- 6. Ensure the 3 generic types are active
UPDATE analyzer_type SET is_active = true
WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- Verification
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE is_active = true) AS active_analyzer_count,
  (SELECT string_agg(name, ', ' ORDER BY name) FROM analyzer_type WHERE is_active = true) AS active_types;
