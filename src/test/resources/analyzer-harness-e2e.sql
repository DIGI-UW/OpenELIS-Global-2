-- Unified analyzer harness fixtures for CI E2E.
-- Scope: ASTM (012), HL7 (013). FILE (014) fixtures live in fixtures/file-import-e2e.sql.
SET search_path TO clinlims;

-- Clean core ASTM/HL7 analyzer rows by fixed IDs.
DELETE FROM analyzer_test_map
WHERE analyzer_id IN ('2006', '2007', '2013');

DELETE FROM analyzer
WHERE id IN (2006, 2007, 2013);

-- Seed analyzer types.
INSERT INTO analyzer_type (
    id,
    name,
    description,
    plugin_class_name,
    protocol,
    is_generic_plugin,
    is_active,
    sys_user_id,
    last_updated
)
VALUES (
    nextval('analyzer_type_seq'),
    'Generic ASTM',
    'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
    'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
    'ASTM',
    true,
    true,
    '1',
    NOW()
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO analyzer_type (
    id,
    name,
    description,
    plugin_class_name,
    protocol,
    is_generic_plugin,
    is_active,
    sys_user_id,
    last_updated
)
VALUES (
    nextval('analyzer_type_seq'),
    'Generic HL7',
    'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
    'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
    'HL7',
    true,
    true,
    '1',
    NOW()
)
ON CONFLICT (name) DO NOTHING;

-- Seed ASTM + HL7 analyzers.
INSERT INTO analyzer (
    id,
    name,
    analyzer_type,
    description,
    is_active,
    ip_address,
    port,
    protocol_version,
    status,
    identifier_pattern,
    analyzer_type_id,
    last_updated
)
VALUES (
    2006,
    'Mindray BA-88A',
    'CHEMISTRY',
    'ASTM over TCP/IP for CI harness',
    true,
    '172.21.1.100',
    9600,
    'ASTM_LIS2_A2',
    'ACTIVE',
    'MINDRAY.*BA-88A|BA88A',
    (SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'),
    NOW()
), (
    2007,
    'Mindray BC-5380',
    'HEMATOLOGY',
    'HL7 over TCP/IP (MLLP) for CI harness',
    true,
    '172.21.1.100',
    5000,
    'HL7_V2_3_1',
    'ACTIVE',
    'MINDRAY.*BC.?5380|BC5380',
    (SELECT id FROM analyzer_type WHERE name = 'Generic HL7'),
    NOW()
), (
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

-- Minimal analyzer test map for simulator and connectivity checks.
DELETE FROM analyzer_test_map
WHERE analyzer_type_id = (SELECT id FROM analyzer_type WHERE name = 'Generic ASTM')
  AND analyzer_test_name IN ('MTB-RIF', 'RIF', 'HIV-VL', 'COVID19');

INSERT INTO analyzer_test_map (
    analyzer_type_id,
    analyzer_id,
    analyzer_test_name,
    test_id,
    last_updated
)
VALUES (
    (SELECT analyzer_type_id FROM analyzer WHERE id = 2013),
    '2013',
    'MTB-RIF',
    '3',
    NOW()
), (
    (SELECT analyzer_type_id FROM analyzer WHERE id = 2013),
    '2013',
    'RIF',
    '5',
    NOW()
), (
    (SELECT analyzer_type_id FROM analyzer WHERE id = 2013),
    '2013',
    'HIV-VL',
    '192',
    NOW()
), (
    (SELECT analyzer_type_id FROM analyzer WHERE id = 2013),
    '2013',
    'COVID19',
    '3',
    NOW()
)
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO UPDATE
SET
    analyzer_id = EXCLUDED.analyzer_id,
    test_id = EXCLUDED.test_id,
    last_updated = NOW();

-- Verification summary.
SELECT
    (SELECT COUNT(*) FROM analyzer WHERE id IN (2006, 2007, 2013)) AS core_analyzer_count,
    (SELECT COUNT(*) FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7')) AS analyzer_type_count,
    (SELECT COUNT(*) FROM analyzer_test_map WHERE analyzer_id = '2013') AS genexpert_map_count;
