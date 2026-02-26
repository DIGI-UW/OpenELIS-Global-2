-- Analyzer Minimal Fixture (2-Table Model)
-- Self-contained, hand-authored SQL for focused plugin testing.
-- Creates 3 analyzers (2006, 2007, 2013) with config columns on the analyzer row.
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
--   GenericASTMAnalyzer → "Generic ASTM"
--   GenericHL7Analyzer  → "Generic HL7"
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
  (2013, 'Cepheid GeneXpert (ASTM Mode)', 'MOLECULAR', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.110', 9600, 'ASTM_LIS2_A2', 'ACTIVE',
   'GENEXPERT|CEPHEID', NOW())
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

-- =============================================================================
-- 4. TEST MAPPINGS for GeneXpert ASTM (analyzer_test_map composite PK)
-- =============================================================================
-- analyzer_test_map has composite PK: (analyzer_id, analyzer_test_name)
-- test_id references clinlims.test (FK constraint).
-- Using Liquibase-seeded test IDs: 3=Glucose, 5=Amylase, 192=CD4 absolute count.
-- These are placeholder mappings for E2E routing validation, not clinical accuracy.

INSERT INTO analyzer_test_map (analyzer_id, analyzer_test_name, test_id, last_updated)
VALUES
  ('2013', 'MTB-RIF',  '3',   NOW()),
  ('2013', 'RIF',      '5',   NOW()),
  ('2013', 'HIV-VL',   '192', NOW()),
  ('2013', 'COVID19',  '3',   NOW())
ON CONFLICT (analyzer_id, analyzer_test_name) DO NOTHING;

-- =============================================================================
-- 5. ADVANCE SEQUENCE (avoid ID collisions with future inserts)
-- =============================================================================

SELECT setval('analyzer_seq', GREATEST(
  (SELECT COALESCE(MAX(id), 0) FROM analyzer)::bigint,
  nextval('analyzer_seq')
));

-- =============================================================================
-- 6. VERIFICATION (DO block — raises NOTICE, never fails)
-- =============================================================================

DO $$
DECLARE
  v_analyzer_count   INTEGER;
  v_type_count       INTEGER;
  v_map_count        INTEGER;
  v_linked_count     INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_analyzer_count FROM analyzer WHERE id IN (2006, 2007, 2013);
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7');
  SELECT COUNT(*) INTO v_map_count FROM analyzer_test_map WHERE analyzer_id = '2013';
  SELECT COUNT(*) INTO v_linked_count FROM analyzer WHERE id IN (2006, 2007, 2013) AND analyzer_type_id IS NOT NULL;

  RAISE NOTICE 'analyzer-minimal.sql verification:';
  RAISE NOTICE '  analyzers:      % / 3 expected', v_analyzer_count;
  RAISE NOTICE '  analyzer_types: % / 2 expected', v_type_count;
  RAISE NOTICE '  test_mappings:  % / 4 expected', v_map_count;
  RAISE NOTICE '  type_linked:    % / 3 expected', v_linked_count;
END $$;
