-- Analyzer Minimal Fixture (2-Table Model)
-- Self-contained, hand-authored SQL for focused plugin testing.
-- Creates 4 analyzers (2006, 2007, 2013, 2014) with config columns on the analyzer row.
-- No dependency on xml-to-sql.py or load-analyzer-test-data.sh.
--
-- Schema: 2-table model (analyzer_type + analyzer) after PR #2802 merge.
-- Enum values: ProtocolVersion (ASTM_LIS2_A2, HL7_V2_3_1, HL7_V2_5)
--              AnalyzerStatus  (INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE, DELETED)

SET search_path TO clinlims;

-- =============================================================================
-- 1. ANALYZER TYPES (Plugin Capability — normally created at startup)
-- =============================================================================
-- PluginRegistryService auto-creates these at startup when plugin JARs are loaded.
-- Names MUST match PluginRegistryService.derivePluginName() output:
--   GenericASTMAnalyzer  → "Generic ASTM"
--   GenericHL7Analyzer   → "Generic HL7"
--   GenericFileAnalyzer  → "Generic File"
-- These INSERTs are a fallback for environments where plugins aren't loaded
-- (e.g., unit tests without the full plugin JAR lifecycle).

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic ASTM', 'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic HL7', 'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic File', 'Generic File - Dashboard-configurable file import analyzer',
        'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
        'FILE', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 2. ANALYZERS (Device Instance + Config — merged 2-table model)
-- =============================================================================
-- Config columns (ip_address, port, protocol_version, status, identifier_pattern)
-- live directly on the analyzer row since PR #2802.

INSERT INTO analyzer (id, name, analyzer_type, description, is_active,
                      ip_address, port, protocol_version, status,
                      identifier_pattern, last_updated)
VALUES
  -- Mindray BA-88A: GenericASTM, chemistry, RS232
  (2006, 'Mindray BA-88A', 'CHEMISTRY', 'ASTM over RS232 Serial', true,
   '172.20.1.100', 9600, 'ASTM_LIS2_A2', 'ACTIVE',
   'MINDRAY.*BA-88A|BA88A', NOW()),
  -- Mindray BC-5380: GenericHL7, hematology, TCP/MLLP
  (2007, 'Mindray BC-5380', 'HEMATOLOGY', 'HL7 v2.3.1 over TCP/IP (MLLP)', true,
   '172.20.1.101', 5562, 'HL7_V2_3_1', 'ACTIVE',
   'MINDRAY.*BC.?5380|BC5380', NOW()),
  -- GeneXpert ASTM Mode: GenericASTM, molecular, TCP/IP
  -- Same IP as astm-simulator (172.20.1.100) — one mock serves all ASTM templates.
  -- OE identifies the analyzer from the ASTM H-record, not the source IP.
  (2013, 'Cepheid GeneXpert (ASTM Mode)', 'MOLECULAR', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9600, 'ASTM_LIS2_A2', 'ACTIVE',
   'GENEXPERT.*|CEPHEID.*', NOW()),
  -- QuantStudio 5: GenericFile, molecular, FILE import (EXCEL)
  (2014, 'QuantStudio 5', 'MOLECULAR', 'QuantStudio QS5 Real-Time PCR (FILE import)', true,
   NULL, NULL, NULL, 'ACTIVE',
   NULL, NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 3. LINK ANALYZERS TO ANALYZER_TYPE (idempotent)
-- =============================================================================

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'
) WHERE id IN (2006, 2013) AND analyzer_type_id IS NULL;

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic HL7'
) WHERE id = 2007 AND analyzer_type_id IS NULL;

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic File'
) WHERE id = 2014 AND analyzer_type_id IS NULL;

-- =============================================================================
-- 4. TEST MAPPINGS for GeneXpert ASTM (analyzer_test_map composite PK)
-- =============================================================================
-- analyzer_test_map has composite PK: (analyzer_type_id, analyzer_test_name)
-- test_id references clinlims.test (FK constraint).
-- Using Liquibase-seeded test IDs: 3=Glucose, 5=Amylase, 192=CD4 absolute count.
-- These are placeholder mappings for E2E routing validation, not clinical accuracy.

INSERT INTO analyzer_test_map (analyzer_type_id, analyzer_id, analyzer_test_name, test_id, last_updated)
VALUES
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'MTB-RIF',  '3',   NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'RIF',      '5',   NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'HIV-VL',   '192', NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'COVID19',  '3',   NOW())
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO NOTHING;

-- =============================================================================
-- 5. FILE IMPORT CONFIGURATION for QuantStudio 5
-- =============================================================================

INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'a0000000-0000-0000-0000-000000002014',
  2014,
  '/data/analyzer-imports/quantstudio-5/incoming',
  '*.xls',
  '/data/analyzer-imports/quantstudio-5/processed',
  '/data/analyzer-imports/quantstudio-5/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t',
  true,
  true,
  'a0000000-0000-0000-0000-000000002014',
  '1',
  NOW(),
  'EXCEL'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  2014,
  '{
    "profileMeta":{"id":"quantstudio","version":"1.1.0","displayName":"QuantStudio QS5/QS7"},
    "protocol":{"name":"FILE","format":"EXCEL"},
    "column_mapping":{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"},
    "default_test_mappings":{"VIH-1":"HIV-1 VL (LOINC 20447-9)","IC":"Internal Control"},
    "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetIndex":0}
  }'::jsonb,
  '1',
  NOW()
) ON CONFLICT (analyzer_id) DO NOTHING;

-- =============================================================================
-- 6. ADVANCE SEQUENCE (avoid ID collisions with future inserts)
-- =============================================================================

SELECT setval('analyzer_seq', GREATEST(
  (SELECT COALESCE(MAX(id), 0) FROM analyzer)::bigint,
  nextval('analyzer_seq')
));

-- =============================================================================
-- 7. VERIFICATION (DO block — raises NOTICE, never fails)
-- =============================================================================

DO $$
DECLARE
  v_analyzer_count   INTEGER;
  v_type_count       INTEGER;
  v_map_count        INTEGER;
  v_linked_count     INTEGER;
  v_file_cfg_count   INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_analyzer_count FROM analyzer WHERE id IN (2006, 2007, 2013, 2014);
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');
  SELECT COUNT(*) INTO v_map_count FROM analyzer_test_map WHERE analyzer_id = '2013';
  SELECT COUNT(*) INTO v_linked_count FROM analyzer WHERE id IN (2006, 2007, 2013, 2014) AND analyzer_type_id IS NOT NULL;
  SELECT COUNT(*) INTO v_file_cfg_count FROM file_import_configuration WHERE analyzer_id = 2014;

  RAISE NOTICE 'analyzer-minimal.sql verification:';
  RAISE NOTICE '  analyzers:      % / 4 expected', v_analyzer_count;
  RAISE NOTICE '  analyzer_types: % / 3 expected', v_type_count;
  RAISE NOTICE '  test_mappings:  % / 4 expected', v_map_count;
  RAISE NOTICE '  type_linked:    % / 4 expected', v_linked_count;
  RAISE NOTICE '  file_configs:   % / 1 expected', v_file_cfg_count;
END $$;
