-- Madagascar UAT Analyzer Fixtures
-- Seeds exactly 4 analyzers for the Madagascar validation scope:
--   2013: GeneXpert (ASTM)
--   2014: QuantStudio 5 (FILE, *.xls)
--   2015: QuantStudio 7 (FILE, *.xlsx)
--   2016: FluoroCycler XT (FILE, *.xlsx)
--
-- Schema: 2-table model (analyzer_type + analyzer) after PR #2802.
-- Self-contained — no dependency on xml-to-sql.py or external scripts.

SET search_path TO clinlims;

-- =============================================================================
-- 1. ANALYZER TYPES (Plugin Capability — fallback for test environments)
-- =============================================================================
-- PluginRegistryService auto-creates these at startup when plugin JARs load.
-- These INSERTs are fallback for environments without the full plugin lifecycle.

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
-- 2. ANALYZERS (4 Madagascar scope)
-- =============================================================================

INSERT INTO analyzer (id, name, analyzer_type, description, is_active,
                      ip_address, port, protocol_version, status,
                      identifier_pattern, last_updated)
VALUES
  -- GeneXpert ASTM Mode: GenericASTM, molecular, TCP/IP
  (2013, 'Cepheid GeneXpert (ASTM Mode)', 'MOLECULAR', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9600, 'ASTM_LIS2_A2', 'ACTIVE',
   'GENEXPERT.*|CEPHEID.*', NOW()),
  -- QuantStudio 5: GenericFile, molecular, FILE import (EXCEL, *.xls)
  (2014, 'QuantStudio 5', 'MOLECULAR', 'QuantStudio QS5 Real-Time PCR (FILE import)', true,
   NULL, NULL, NULL, 'ACTIVE',
   NULL, NOW()),
  -- QuantStudio 7: GenericFile, molecular, FILE import (EXCEL, *.xlsx)
  (2015, 'QuantStudio 7', 'MOLECULAR', 'QuantStudio QS7 Real-Time PCR (FILE import)', true,
   NULL, NULL, NULL, 'ACTIVE',
   NULL, NOW()),
  -- FluoroCycler XT: GenericFile, molecular, FILE import (EXCEL, *.xlsx)
  (2016, 'FluoroCycler XT', 'MOLECULAR', 'Bruker FluoroCycler XT (FILE import)', true,
   NULL, NULL, NULL, 'ACTIVE',
   NULL, NOW())
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  analyzer_type = EXCLUDED.analyzer_type,
  description = EXCLUDED.description,
  is_active = EXCLUDED.is_active,
  ip_address = EXCLUDED.ip_address,
  port = EXCLUDED.port,
  protocol_version = EXCLUDED.protocol_version,
  status = EXCLUDED.status,
  identifier_pattern = EXCLUDED.identifier_pattern,
  last_updated = EXCLUDED.last_updated;

-- =============================================================================
-- 3. LINK ANALYZERS TO ANALYZER_TYPE (idempotent)
-- =============================================================================

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'
) WHERE id = 2013 AND analyzer_type_id IS NULL;

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic File'
) WHERE id IN (2014, 2015, 2016) AND analyzer_type_id IS NULL;

-- =============================================================================
-- 4. TEST MAPPINGS for GeneXpert ASTM
-- =============================================================================
-- Composite PK: (analyzer_type_id, analyzer_test_name)
-- test_id references clinlims.test (Liquibase-seeded): 175=DNA PCR, 38=ARV resistance,
-- 313=HIV VIRAL LOAD, 300=COVID-19 PCR.

INSERT INTO analyzer_test_map (analyzer_type_id, analyzer_id, analyzer_test_name, test_id, last_updated)
VALUES
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'MTB-RIF',  '175', NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'RIF',      '38',  NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'HIV-VL',   '313', NOW()),
  ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'COVID19',  '300', NOW())
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO NOTHING;

-- =============================================================================
-- 5. FILE IMPORT CONFIGURATIONS (3 FILE analyzers)
-- =============================================================================

-- QuantStudio 5 (*.xls)
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'a0000000-0000-0000-0000-000000002014',
  2014,
  '/data/analyzer-imports/qs5/incoming',
  '*.xls',
  '/data/analyzer-imports/qs5/processed',
  '/data/analyzer-imports/qs5/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t', true, true,
  'a0000000-0000-0000-0000-000000002014', '1', NOW(), 'EXCEL'
) ON CONFLICT (id) DO UPDATE SET
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  archive_directory = EXCLUDED.archive_directory,
  error_directory = EXCLUDED.error_directory,
  column_mappings = EXCLUDED.column_mappings,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- QuantStudio 7 (*.xlsx)
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'a0000000-0000-0000-0000-000000002015',
  2015,
  '/data/analyzer-imports/qs7/incoming',
  '*.xlsx',
  '/data/analyzer-imports/qs7/processed',
  '/data/analyzer-imports/qs7/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t', true, true,
  'a0000000-0000-0000-0000-000000002015', '1', NOW(), 'EXCEL'
) ON CONFLICT (id) DO UPDATE SET
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  archive_directory = EXCLUDED.archive_directory,
  error_directory = EXCLUDED.error_directory,
  column_mappings = EXCLUDED.column_mappings,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- FluoroCycler XT (*.xlsx)
INSERT INTO file_import_configuration (
  id, analyzer_id, import_directory, file_pattern,
  archive_directory, error_directory, column_mappings,
  delimiter, has_header, active, fhir_uuid, sys_user_id,
  last_updated, file_format
) VALUES (
  'a0000000-0000-0000-0000-000000002016',
  2016,
  '/data/analyzer-imports/fluorocycler/incoming',
  '*.xlsx',
  '/data/analyzer-imports/fluorocycler/processed',
  '/data/analyzer-imports/fluorocycler/errors',
  '{"SampleID":"sampleId","TargetName":"testCode","WellPosition":"position","CP":"result","Interpretation":"interpretation","RunDate":"testDate"}',
  E'\t', true, true,
  'a0000000-0000-0000-0000-000000002016', '1', NOW(), 'EXCEL'
) ON CONFLICT (id) DO UPDATE SET
  import_directory = EXCLUDED.import_directory,
  file_pattern = EXCLUDED.file_pattern,
  archive_directory = EXCLUDED.archive_directory,
  error_directory = EXCLUDED.error_directory,
  column_mappings = EXCLUDED.column_mappings,
  active = EXCLUDED.active,
  last_updated = EXCLUDED.last_updated;

-- =============================================================================
-- 6. ANALYZER PLUGIN CONFIGS (profile metadata for each FILE analyzer)
-- =============================================================================

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  2014,
  '{"profileMeta":{"id":"quantstudio","version":"1.1.0","displayName":"QuantStudio QS5/QS7"},
    "protocol":{"name":"FILE","format":"EXCEL"},
    "column_mapping":{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"},
    "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetIndex":0}}'::jsonb,
  '1', NOW()
) ON CONFLICT (analyzer_id) DO NOTHING;

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  2015,
  '{"profileMeta":{"id":"quantstudio","version":"1.1.0","displayName":"QuantStudio QS5/QS7"},
    "protocol":{"name":"FILE","format":"EXCEL"},
    "column_mapping":{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"},
    "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetIndex":0}}'::jsonb,
  '1', NOW()
) ON CONFLICT (analyzer_id) DO NOTHING;

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES (
  2016,
  '{"profileMeta":{"id":"fluorocycler-xt","version":"1.0.0","displayName":"Bruker FluoroCycler XT"},
    "protocol":{"name":"FILE","format":"EXCEL"},
    "column_mapping":{"SampleID":"sampleId","TargetName":"testCode","WellPosition":"position","CP":"result","Interpretation":"interpretation","RunDate":"testDate"},
    "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetName":"Results"}}'::jsonb,
  '1', NOW()
) ON CONFLICT (analyzer_id) DO NOTHING;

-- =============================================================================
-- 7. ADVANCE SEQUENCE (avoid ID collisions with future inserts)
-- =============================================================================

SELECT setval('analyzer_seq', GREATEST(
  (SELECT COALESCE(MAX(id), 0) FROM analyzer)::bigint,
  nextval('analyzer_seq')
));

-- =============================================================================
-- 8. VERIFICATION
-- =============================================================================

DO $$
DECLARE
  v_analyzer_count   INTEGER;
  v_type_count       INTEGER;
  v_map_count        INTEGER;
  v_linked_count     INTEGER;
  v_file_cfg_count   INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_analyzer_count FROM analyzer WHERE id IN (2013, 2014, 2015, 2016);
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');
  SELECT COUNT(*) INTO v_map_count FROM analyzer_test_map WHERE analyzer_id = '2013';
  SELECT COUNT(*) INTO v_linked_count FROM analyzer WHERE id IN (2013, 2014, 2015, 2016) AND analyzer_type_id IS NOT NULL;
  SELECT COUNT(*) INTO v_file_cfg_count FROM file_import_configuration WHERE analyzer_id IN (2014, 2015, 2016);

  RAISE NOTICE 'analyzer-minimal.sql verification:';
  RAISE NOTICE '  analyzers:      % / 4 expected', v_analyzer_count;
  RAISE NOTICE '  analyzer_types: % / 3 expected', v_type_count;
  RAISE NOTICE '  test_mappings:  % / 4 expected', v_map_count;
  RAISE NOTICE '  type_linked:    % / 4 expected', v_linked_count;
  RAISE NOTICE '  file_configs:   % / 3 expected', v_file_cfg_count;
END $$;
