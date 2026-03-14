SET search_path TO clinlims;

-- ============================================================================
-- FILE Import E2E Cleanup — Madagascar UAT scope
--
-- Purpose: Clean up stale/out-of-scope analyzers for a clean 4-analyzer dashboard.
-- The actual analyzer fixtures (QS5, QS7, FluoroCycler, GeneXpert) are seeded
-- by analyzer-minimal.sql. This file handles cleanup only.
--
-- FK ordering: RESTRICT tables (analyzer_results, notebook_analysers) must be
-- deleted explicitly before the analyzer. CASCADE tables (config) are handled
-- automatically by DB constraints.
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

-- 3. Clean up Mindray analyzers (not in current UAT scope)
DELETE FROM analyzer_results WHERE analyzer_id IN (2006,2007,2008,2012);
DELETE FROM notebook_analysers WHERE analyser_id IN (2006,2007,2008,2012);
DELETE FROM analyzer WHERE id IN (2006,2007,2008,2012);

-- 4. Deactivate all legacy (non-generic) analyzer types for clean dashboard
UPDATE analyzer_type SET is_active = false
WHERE name NOT IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- 5. Ensure the 3 generic types are active
UPDATE analyzer_type SET is_active = true
WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- Verification
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE is_active = true) AS active_analyzer_count,
  (SELECT string_agg(name, ', ' ORDER BY name) FROM analyzer_type WHERE is_active = true) AS active_types,
  (SELECT string_agg(name, ', ' ORDER BY id) FROM analyzer WHERE is_active = true) AS active_analyzers;
