SET search_path TO clinlims;

-- ============================================================================
-- FILE Import E2E Fixtures
--
-- Purpose: (1) Clean up stale/out-of-scope analyzers for a clean dashboard,
--          (2) Seed 3 E2E-FILE analyzers (IDs 3001-3003) with FileImportConfigs
--              for file-import.spec.ts (harness project).
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

-- ============================================================================
-- FILE Import E2E Analyzer Seeding — 3 E2E-FILE analyzers + FileImportConfigs
--
-- These are used by file-import.spec.ts (harness project).
-- IDs 3001-3003 avoid conflict with UAT analyzers (2013-2016).
-- ============================================================================

-- 6. Seed E2E-FILE-CSV-Analyzer (ID 3001)
INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    status, analyzer_type_id, last_updated
)
VALUES (
    3001,
    'E2E-FILE-CSV-Analyzer',
    'CHEMISTRY',
    'E2E test: CSV file import',
    true,
    'ACTIVE',
    (SELECT id FROM analyzer_type WHERE name = 'Generic File'),
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, is_active = EXCLUDED.is_active,
    analyzer_type_id = EXCLUDED.analyzer_type_id, last_updated = NOW();

-- 7. Seed E2E-FILE-QuantStudio5-Analyzer (ID 3002)
INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    status, analyzer_type_id, last_updated
)
VALUES (
    3002,
    'E2E-FILE-QuantStudio5-Analyzer',
    'MOLECULAR',
    'E2E test: QuantStudio 5 Excel (.xls) import',
    true,
    'ACTIVE',
    (SELECT id FROM analyzer_type WHERE name = 'Generic File'),
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, is_active = EXCLUDED.is_active,
    analyzer_type_id = EXCLUDED.analyzer_type_id, last_updated = NOW();

-- 8. Seed E2E-FILE-QuantStudio7-Analyzer (ID 3003)
INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    status, analyzer_type_id, last_updated
)
VALUES (
    3003,
    'E2E-FILE-QuantStudio7-Analyzer',
    'MOLECULAR',
    'E2E test: QuantStudio 7 Excel (.xlsx) import',
    true,
    'ACTIVE',
    (SELECT id FROM analyzer_type WHERE name = 'Generic File'),
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name, is_active = EXCLUDED.is_active,
    analyzer_type_id = EXCLUDED.analyzer_type_id, last_updated = NOW();

-- 9. Seed FileImportConfigurations for E2E-FILE analyzers
-- Clean existing configs for these analyzers first (CASCADE would handle this,
-- but explicit delete is safer for idempotent re-runs)
DELETE FROM file_import_configuration WHERE analyzer_id IN (3001, 3002, 3003);

-- CSV config (ID 3001)
INSERT INTO file_import_configuration (
    id, analyzer_id, import_directory, file_pattern, file_format,
    archive_directory, error_directory,
    column_mappings, delimiter, has_header, active,
    fhir_uuid, sys_user_id, last_updated
)
VALUES (
    'e2e-file-csv-config-001',
    3001,
    '/data/analyzer-imports/e2e-csv/incoming',
    '*.csv',
    'CSV',
    '/data/analyzer-imports/e2e-csv/processed',
    '/data/analyzer-imports/e2e-csv/errors',
    '{"Sample_ID":"sampleId","Test_Code":"testCode","Result":"result"}',
    ',',
    true,
    true,
    'a0000001-0e2e-4000-8001-000000000001',
    '1',
    NOW()
);

-- QuantStudio 5 EXCEL config (ID 3002)
INSERT INTO file_import_configuration (
    id, analyzer_id, import_directory, file_pattern, file_format,
    archive_directory, error_directory,
    column_mappings, delimiter, has_header, active,
    fhir_uuid, sys_user_id, last_updated
)
VALUES (
    'e2e-file-qs5-config-001',
    3002,
    '/data/analyzer-imports/e2e-qs5/incoming',
    '*.xls',
    'EXCEL',
    '/data/analyzer-imports/e2e-qs5/processed',
    '/data/analyzer-imports/e2e-qs5/errors',
    '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
    ',',
    true,
    true,
    'a0000002-0e2e-4000-8002-000000000002',
    '1',
    NOW()
);

-- QuantStudio 7 EXCEL config (ID 3003)
INSERT INTO file_import_configuration (
    id, analyzer_id, import_directory, file_pattern, file_format,
    archive_directory, error_directory,
    column_mappings, delimiter, has_header, active,
    fhir_uuid, sys_user_id, last_updated
)
VALUES (
    'e2e-file-qs7-config-001',
    3003,
    '/data/analyzer-imports/e2e-qs7/incoming',
    '*.xlsx',
    'EXCEL',
    '/data/analyzer-imports/e2e-qs7/processed',
    '/data/analyzer-imports/e2e-qs7/errors',
    '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
    ',',
    true,
    true,
    'a0000003-0e2e-4000-8003-000000000003',
    '1',
    NOW()
);

-- Verification
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE is_active = true) AS active_analyzer_count,
  (SELECT COUNT(*) FROM file_import_configuration WHERE analyzer_id IN (3001, 3002, 3003)) AS e2e_file_config_count,
  (SELECT string_agg(name, ', ' ORDER BY name) FROM analyzer_type WHERE is_active = true) AS active_types,
  (SELECT string_agg(name, ', ' ORDER BY id) FROM analyzer WHERE is_active = true) AS active_analyzers;
