SET search_path TO clinlims;

-- ============================================================================
-- FILE Import E2E Fixtures — Madagascar UAT scope
--
-- Prepares the environment for E2E tests that CREATE analyzers through the UI.
-- Only deactivates legacy analyzer types so the plugin dropdown shows exactly
-- 3 entries: Generic ASTM, Generic HL7, Generic File.
--
-- Analyzers, file import configs, and plugin configs are created by the
-- Playwright tests themselves (not pre-seeded here).
-- ============================================================================

-- Clean up any previous E2E-FILE fixtures (from older test runs)
DELETE FROM analyzer_results
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM file_import_configuration
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer_plugin_config
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer
WHERE name LIKE 'E2E-FILE-%';

DELETE FROM analyzer_type
WHERE name LIKE 'E2E-FILE-%';

-- Also clean up any E2E analyzers created by Playwright (timestamped names)
DELETE FROM analyzer_results
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE '%E2E %');

DELETE FROM file_import_configuration
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE '%E2E %');

DELETE FROM analyzer_plugin_config
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE '%E2E %');

DELETE FROM analyzer
WHERE name LIKE '%E2E %';

-- Deactivate all legacy (non-generic) analyzer types for clean dashboard
UPDATE analyzer_type
SET is_active = false
WHERE name NOT IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- Ensure the 3 generic types are active
UPDATE analyzer_type
SET is_active = true
WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');

-- ─── Verification ───────────────────────────────────────────────────────────
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count,
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = false) AS inactive_type_count,
  (SELECT string_agg(name, ', ' ORDER BY name) FROM analyzer_type WHERE is_active = true) AS active_types;
