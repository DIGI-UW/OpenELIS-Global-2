SET search_path TO clinlims;

-- ============================================================================
-- FILE Import E2E Fixtures — Madagascar UAT scope
--
-- Seeds:
--   1. Three E2E-FILE analyzers (CSV, QuantStudio5, QuantStudio7) with
--      FileImportConfiguration rows — used by harness project tests
--   2. Deactivates legacy analyzer types for clean dashboard — used by
--      demo project tests that CREATE analyzers through the UI
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

-- ─── Seed E2E-FILE analyzers for harness tests ────────────────────────────
-- These are used by file-import.spec.ts (harness project) to verify
-- FileImportConfiguration existence, persistence, and column mappings.

INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    analyzer_type_id, last_updated
)
VALUES
  (3001, 'E2E-FILE-CSV-Analyzer', 'CHEMISTRY',
   'CSV file import E2E test analyzer', true,
   (SELECT id FROM analyzer_type WHERE name = 'Generic File'), NOW()),
  (3002, 'E2E-FILE-QuantStudio5-Analyzer', 'MOLECULAR',
   'QuantStudio 5 EXCEL file import E2E test analyzer', true,
   (SELECT id FROM analyzer_type WHERE name = 'Generic File'), NOW()),
  (3003, 'E2E-FILE-QuantStudio7-Analyzer', 'MOLECULAR',
   'QuantStudio 7 EXCEL file import E2E test analyzer', true,
   (SELECT id FROM analyzer_type WHERE name = 'Generic File'), NOW())
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  analyzer_type = EXCLUDED.analyzer_type,
  description = EXCLUDED.description,
  is_active = EXCLUDED.is_active,
  analyzer_type_id = EXCLUDED.analyzer_type_id,
  last_updated = EXCLUDED.last_updated;

-- FileImportConfiguration for CSV analyzer
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'e2e-file-cfg-csv-00000000-0001',
  3001,
  '/data/analyzer-imports/e2e-csv/incoming',
  '*.csv',
  '/data/analyzer-imports/e2e-csv/processed',
  '/data/analyzer-imports/e2e-csv/errors',
  '{"Sample_ID":"sampleId","Test_Code":"testCode","Result":"result"}',
  ',',
  true,
  true,
  'e2e00000-0000-4000-a001-000000000001',
  '1',
  NOW(),
  'CSV'
) ON CONFLICT (id) DO UPDATE SET
  analyzer_id = EXCLUDED.analyzer_id,
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  file_format = EXCLUDED.file_format,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- FileImportConfiguration for QuantStudio 5 (EXCEL, *.xls)
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'e2e-file-cfg-qs5-00000000-0002',
  3002,
  '/data/analyzer-imports/e2e-qs5/incoming',
  '*.xls',
  '/data/analyzer-imports/e2e-qs5/processed',
  '/data/analyzer-imports/e2e-qs5/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t',
  true,
  true,
  'e2e00000-0000-4000-a002-000000000002',
  '1',
  NOW(),
  'EXCEL'
) ON CONFLICT (id) DO UPDATE SET
  analyzer_id = EXCLUDED.analyzer_id,
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  file_format = EXCLUDED.file_format,
  column_mappings = EXCLUDED.column_mappings,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- FileImportConfiguration for QuantStudio 7 (EXCEL, *.xlsx)
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'e2e-file-cfg-qs7-00000000-0003',
  3003,
  '/data/analyzer-imports/e2e-qs7/incoming',
  '*.xlsx',
  '/data/analyzer-imports/e2e-qs7/processed',
  '/data/analyzer-imports/e2e-qs7/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t',
  true,
  true,
  'e2e00000-0000-4000-a003-000000000003',
  '1',
  NOW(),
  'EXCEL'
) ON CONFLICT (id) DO UPDATE SET
  analyzer_id = EXCLUDED.analyzer_id,
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  file_format = EXCLUDED.file_format,
  column_mappings = EXCLUDED.column_mappings,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- ─── Verification ───────────────────────────────────────────────────────────
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE name LIKE 'E2E-FILE-%') AS e2e_file_analyzer_count,
  (SELECT COUNT(*) FROM file_import_configuration WHERE analyzer_id IN (3001, 3002, 3003)) AS e2e_file_config_count,
  (SELECT string_agg(name, ', ' ORDER BY name) FROM analyzer_type WHERE is_active = true) AS active_types;
