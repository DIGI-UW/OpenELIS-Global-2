SET search_path TO clinlims;

-- ============================================================================
-- FILE Import E2E Fixtures — Madagascar UAT scope
--
-- Creates exactly 3 FILE analyzers: QuantStudio 5, QuantStudio 7, FluoroCycler XT
-- All use the E2E-FILE-GenericFile analyzer type.
--
-- Plugin configs include line_field_order and configDefaults.hasHeader so
-- GenericFileLineInserter maps fields correctly and skips the header line.
-- ============================================================================

-- Clean up any previous E2E-FILE fixtures
DELETE FROM file_import_configuration
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer_plugin_config
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer
WHERE name LIKE 'E2E-FILE-%';

DELETE FROM analyzer_type
WHERE name LIKE 'E2E-FILE-%';

-- Advance sequence past any existing analyzer IDs to avoid collisions
SELECT setval('analyzer_seq', GREATEST(
  (SELECT COALESCE(MAX(id::integer), 0) FROM analyzer),
  nextval('analyzer_seq') - 1
) + 1);

-- Deactivate all legacy (non-generic) analyzer types for clean dashboard
UPDATE analyzer_type
SET is_active = false
WHERE name NOT IN ('Generic ASTM', 'Generic HL7', 'Generic File')
  AND name NOT LIKE 'E2E-%';

-- ─── Analyzer Type ──────────────────────────────────────────────────────────
INSERT INTO analyzer_type (
  id, name, description, protocol, plugin_class_name,
  is_generic_plugin, is_active, sys_user_id, last_updated
) VALUES (
  nextval('analyzer_type_seq'),
  'E2E-FILE-GenericFile',
  'E2E GenericFile analyzer type',
  'FILE',
  'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
  true, true, '1', NOW()
);

-- ─── QuantStudio 5 (.xls) ──────────────────────────────────────────────────
INSERT INTO analyzer (
  id, name, analyzer_type, description, is_active,
  protocol_version, status, analyzer_type_id, last_updated
) VALUES (
  nextval('analyzer_seq'),
  'E2E-FILE-QuantStudio5-Analyzer',
  'MOLECULAR',
  'QuantStudio 5 file import (.xls)',
  true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'),
  NOW()
);

INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format
) VALUES (
  '22222222-2222-2222-2222-222222222225',
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio5-Analyzer'),
  '/data/analyzer-imports/e2e-qs5/incoming', '*.xls',
  '/data/analyzer-imports/e2e-qs5/processed',
  '/data/analyzer-imports/e2e-qs5/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t', true, true,
  '22222222-2222-2222-2222-222222222225',
  '1', NOW(), 'EXCEL'
);

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio5-Analyzer'),
  '{
    "profileMeta": {"id": "quantstudio", "version": "1.1.0", "displayName": "QuantStudio QS5"},
    "protocol": {"name": "FILE", "format": "EXCEL"},
    "column_mapping": {"Sample Name": "sampleId", "Target Name": "testCode", "Quantity Mean": "result", "CT": "ctValue", "Well Position": "position"},
    "line_field_order": ["sampleId", "testCode", "result", "interpretation", "position", "testDate", "testTime"],
    "default_test_mappings": {"VIH-1": "HIV-1 VL (LOINC 20447-9)", "IC": "Internal Control"},
    "configDefaults": {"fileFormat": "EXCEL", "hasHeader": true, "sheetIndex": 0}
  }'::jsonb,
  '1', NOW()
);

-- ─── QuantStudio 7 (.xlsx) ─────────────────────────────────────────────────
INSERT INTO analyzer (
  id, name, analyzer_type, description, is_active,
  protocol_version, status, analyzer_type_id, last_updated
) VALUES (
  nextval('analyzer_seq'),
  'E2E-FILE-QuantStudio7-Analyzer',
  'MOLECULAR',
  'QuantStudio 7 file import (.xlsx)',
  true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'),
  NOW()
);

INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format
) VALUES (
  '22222222-2222-2222-2222-222222222222',
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio7-Analyzer'),
  '/data/analyzer-imports/e2e-qs7/incoming', '*.xlsx',
  '/data/analyzer-imports/e2e-qs7/processed',
  '/data/analyzer-imports/e2e-qs7/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t', true, true,
  '22222222-2222-2222-2222-222222222222',
  '1', NOW(), 'EXCEL'
);

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio7-Analyzer'),
  '{
    "profileMeta": {"id": "quantstudio", "version": "1.1.0", "displayName": "QuantStudio QS7"},
    "protocol": {"name": "FILE", "format": "EXCEL"},
    "column_mapping": {"Sample Name": "sampleId", "Target Name": "testCode", "Quantity Mean": "result", "CT": "ctValue", "Well Position": "position"},
    "line_field_order": ["sampleId", "testCode", "result", "interpretation", "position", "testDate", "testTime"],
    "default_test_mappings": {"VIH-1": "HIV-1 VL (LOINC 20447-9)", "IC": "Internal Control"},
    "configDefaults": {"fileFormat": "EXCEL", "hasHeader": true, "sheetIndex": 0}
  }'::jsonb,
  '1', NOW()
);

-- ─── FluoroCycler XT (.xlsx) ───────────────────────────────────────────────
INSERT INTO analyzer (
  id, name, analyzer_type, description, is_active,
  protocol_version, status, analyzer_type_id, last_updated
) VALUES (
  nextval('analyzer_seq'),
  'E2E-FILE-FluoroCycler-XT',
  'MOLECULAR',
  'Bruker FluoroCycler XT file import (.xlsx)',
  true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'),
  NOW()
);

INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format
) VALUES (
  '55555555-5555-5555-5555-555555555555',
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-FluoroCycler-XT'),
  '/data/analyzer-imports/e2e-fluorocycler/incoming', '*.xlsx',
  '/data/analyzer-imports/e2e-fluorocycler/processed',
  '/data/analyzer-imports/e2e-fluorocycler/errors',
  '{"SampleID":"sampleId","TargetName":"testCode","WellPosition":"position","CP":"result","Interpretation":"interpretation","RunDate":"testDate"}',
  E'\t', true, true,
  '55555555-5555-5555-5555-555555555555',
  '1', NOW(), 'EXCEL'
);

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-FluoroCycler-XT'),
  '{
    "profileMeta": {"id": "fluorocycler-xt", "version": "1.0.0", "displayName": "Bruker FluoroCycler XT"},
    "protocol": {"name": "FILE", "format": "EXCEL"},
    "column_mapping": {"SampleID": "sampleId", "TargetName": "testCode", "WellPosition": "position", "CP": "result", "Interpretation": "interpretation", "RunDate": "testDate"},
    "line_field_order": ["sampleId", "testCode", "result", "interpretation", "position", "testDate", "testTime"],
    "default_test_mappings": {},
    "configDefaults": {"fileFormat": "EXCEL", "hasHeader": true}
  }'::jsonb,
  '1', NOW()
);

-- ─── Verification ───────────────────────────────────────────────────────────
SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile') AS e2e_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE name LIKE 'E2E-FILE-%') AS e2e_analyzer_count,
  (SELECT COUNT(*) FROM file_import_configuration fic
     JOIN analyzer a ON fic.analyzer_id = a.id
   WHERE a.name LIKE 'E2E-FILE-%') AS file_config_count,
  (SELECT COUNT(*) FROM analyzer_plugin_config apc
     JOIN analyzer a ON apc.analyzer_id = a.id
   WHERE a.name LIKE 'E2E-FILE-%') AS plugin_config_count,
  (SELECT COUNT(*) FROM analyzer_type WHERE is_active = true) AS active_type_count;
