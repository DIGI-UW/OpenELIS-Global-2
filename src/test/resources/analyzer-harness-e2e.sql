-- Analyzer harness fixtures for CI E2E.
-- Scope: GeneXpert (ASTM) + Mindray BC-5380 (HL7). FILE fixtures in fixtures/file-import-e2e.sql.
-- Self-contained: creates analyzer_type rows if missing (fallback for environments
-- where plugin startup hasn't run or completed yet).
SET search_path TO clinlims;

-- ============================================================================
-- 0. ANALYZER TYPES (idempotent fallback — PluginRegistryService creates these
--    at startup from plugin JARs, but CI fixture loading may run before plugins
--    finish initializing)
-- ============================================================================

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic ASTM', 'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic HL7', 'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic File', 'Generic File - Dashboard-configurable file import analyzer',
        'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
        'FILE', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

-- ============================================================================
-- 1. GeneXpert ASTM Analyzer (ID 2013)
-- ============================================================================

-- Clean GeneXpert analyzer row by fixed ID.
DELETE FROM analyzer_test_map WHERE analyzer_id = '2013';
DELETE FROM analyzer_results WHERE analyzer_id = 2013;
DELETE FROM analyzer WHERE id = 2013;

-- Seed GeneXpert ASTM analyzer.
INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    ip_address, port, protocol_version, status,
    identifier_pattern, analyzer_type_id, last_updated
)
VALUES (
    2013,
    'Cepheid GeneXpert (ASTM Mode)',
    'MOLECULAR',
    'ASTM LIS2-A2 over TCP/IP',
    true,
    '172.21.1.100',
    9600,
    'ASTM_LIS2_A2',
    'ACTIVE',
    'GENEXPERT.*|CEPHEID.*',
    (SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'),
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    port = EXCLUDED.port,
    protocol_version = EXCLUDED.protocol_version,
    status = EXCLUDED.status,
    identifier_pattern = EXCLUDED.identifier_pattern,
    analyzer_type_id = EXCLUDED.analyzer_type_id,
    last_updated = NOW();

-- GeneXpert test mappings.
INSERT INTO analyzer_test_map (
    analyzer_type_id, analyzer_id, analyzer_test_name, test_id, last_updated
)
VALUES
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'MTB-RIF',  '3',   NOW()),
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'RIF',      '5',   NOW()),
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'HIV-VL',   '192', NOW()),
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2013), '2013', 'COVID19',  '3',   NOW())
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO UPDATE
SET
    analyzer_id = EXCLUDED.analyzer_id,
    test_id = EXCLUDED.test_id,
    last_updated = NOW();

-- ============================================================================
-- Mindray BC-5380 HL7 analyzer (for analyzer-hl7-simulate.spec.ts)
-- ============================================================================

DELETE FROM analyzer_test_map WHERE analyzer_id = '2014';
DELETE FROM analyzer_results WHERE analyzer_id = 2014;
DELETE FROM analyzer WHERE id = 2014;

INSERT INTO analyzer (
    id, name, analyzer_type, description, is_active,
    ip_address, port, protocol_version, status,
    identifier_pattern, analyzer_type_id, last_updated
)
VALUES (
    2014,
    'Mindray BC-5380',
    'HEMATOLOGY',
    'HL7 v2.5.1 over MLLP',
    true,
    '172.21.1.101',
    2575,
    'HL7_V2_5',
    'ACTIVE',
    'MINDRAY.*|BC-5380.*',
    (SELECT id FROM analyzer_type WHERE name = 'Generic HL7'),
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    port = EXCLUDED.port,
    protocol_version = EXCLUDED.protocol_version,
    status = EXCLUDED.status,
    identifier_pattern = EXCLUDED.identifier_pattern,
    analyzer_type_id = EXCLUDED.analyzer_type_id,
    last_updated = NOW();

-- Mindray BC-5380 test mappings (hematology panel).
INSERT INTO analyzer_test_map (
    analyzer_type_id, analyzer_id, analyzer_test_name, test_id, last_updated
)
VALUES
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2014), '2014', 'WBC',  '3',   NOW()),
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2014), '2014', 'HGB',  '5',   NOW()),
    ((SELECT analyzer_type_id FROM analyzer WHERE id = 2014), '2014', 'PLT',  '192', NOW())
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO UPDATE
SET
    analyzer_id = EXCLUDED.analyzer_id,
    test_id = EXCLUDED.test_id,
    last_updated = NOW();

-- Verification summary.
SELECT
    (SELECT COUNT(*) FROM analyzer WHERE id IN (2013, 2014)) AS harness_analyzer_count,
    (SELECT COUNT(*) FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File')) AS analyzer_type_count,
    (SELECT COUNT(*) FROM analyzer_test_map WHERE analyzer_id IN ('2013', '2014')) AS harness_map_count;
