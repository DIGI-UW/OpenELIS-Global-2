-- Unified analyzer harness fixtures for CI E2E.
-- Scope: ASTM (012), HL7 (013), FILE (014).
SET search_path TO clinlims;

-- Clean FILE fixtures by name.
DELETE FROM file_import_configuration
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer_plugin_config
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer
WHERE name LIKE 'E2E-FILE-%';

DELETE FROM analyzer_type
WHERE name = 'E2E-FILE-GenericFile';

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

INSERT INTO analyzer_type (
    id,
    name,
    description,
    protocol,
    plugin_class_name,
    is_generic_plugin,
    is_active,
    sys_user_id,
    last_updated
)
VALUES (
    nextval('analyzer_type_seq'),
    'E2E-FILE-GenericFile',
    'E2E GenericFile analyzer type',
    'FILE',
    'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
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

-- Seed FILE analyzers.
INSERT INTO analyzer (
    id,
    name,
    analyzer_type,
    description,
    is_active,
    protocol_version,
    status,
    analyzer_type_id,
    last_updated
)
VALUES (
    nextval('analyzer_seq'),
    'E2E-FILE-CSV-Analyzer',
    'MOLECULAR',
    'E2E file import analyzer',
    true,
    NULL,
    'ACTIVE',
    (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'),
    NOW()
), (
    nextval('analyzer_seq'),
    'E2E-FILE-QuantStudio-Analyzer',
    'MOLECULAR',
    'E2E QuantStudio file import analyzer',
    true,
    NULL,
    'ACTIVE',
    (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'),
    NOW()
);

INSERT INTO file_import_configuration (
    id,
    analyzer_id,
    import_directory,
    file_pattern,
    archive_directory,
    error_directory,
    column_mappings,
    delimiter,
    has_header,
    active,
    fhir_uuid,
    sys_user_id,
    last_updated
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    (SELECT id FROM analyzer WHERE name = 'E2E-FILE-CSV-Analyzer'),
    '/data/analyzer-imports/e2e-csv/incoming',
    '*.csv',
    '/data/analyzer-imports/e2e-csv/processed',
    '/data/analyzer-imports/e2e-csv/errors',
    '{"Sample_ID":"sampleId","Test_Code":"testCode","Result":"result"}',
    ',',
    true,
    true,
    '11111111-1111-1111-1111-111111111111',
    '1',
    NOW()
), (
    '22222222-2222-2222-2222-222222222222',
    (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio-Analyzer'),
    '/data/analyzer-imports/e2e-qs/incoming',
    '*.xls',
    '/data/analyzer-imports/e2e-qs/processed',
    '/data/analyzer-imports/e2e-qs/errors',
    '{"Sample Name":"sampleId","Assay":"testCode","CT":"result","Units":"units"}',
    ',',
    true,
    true,
    '22222222-2222-2222-2222-222222222222',
    '1',
    NOW()
)
ON CONFLICT (id) DO UPDATE
SET
    analyzer_id = EXCLUDED.analyzer_id,
    import_directory = EXCLUDED.import_directory,
    file_pattern = EXCLUDED.file_pattern,
    archive_directory = EXCLUDED.archive_directory,
    error_directory = EXCLUDED.error_directory,
    column_mappings = EXCLUDED.column_mappings,
    delimiter = EXCLUDED.delimiter,
    has_header = EXCLUDED.has_header,
    active = EXCLUDED.active,
    last_updated = NOW();

-- Set file_format if column exists (added by PR #3036 Liquibase migration).
-- Gracefully skipped on develop where the column does not yet exist.
DO $$
BEGIN
    UPDATE file_import_configuration SET file_format = 'CSV'
    WHERE id = '11111111-1111-1111-1111-111111111111';
    UPDATE file_import_configuration SET file_format = 'EXCEL'
    WHERE id = '22222222-2222-2222-2222-222222222222';
EXCEPTION WHEN undefined_column THEN
    NULL;
END $$;

INSERT INTO analyzer_plugin_config (
    analyzer_id,
    config,
    sys_user_id,
    last_updated
)
VALUES (
    (SELECT id FROM analyzer WHERE name = 'E2E-FILE-CSV-Analyzer'),
    '{
      "profileMeta":{"id":"e2e-file-csv","version":"1.0","displayName":"E2E File CSV"},
      "protocol":{"name":"FILE","format":"CSV"},
      "column_mapping":{"Sample_ID":"sampleId","Test_Code":"testCode","Result":"result"},
      "default_test_mappings":{"VL":"HIV-VL"},
      "configDefaults":{"fileFormat":"CSV","delimiter":",","hasHeader":true}
    }'::jsonb,
    '1',
    NOW()
), (
    (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio-Analyzer'),
    '{
      "profileMeta":{"id":"quantstudio","version":"1.0.0","displayName":"QuantStudio QS5/QS7"},
      "protocol":{"name":"FILE","format":"EXCEL"},
      "column_mapping":{"Sample Name":"sampleId","Assay":"testCode","CT":"result","Units":"units"},
      "default_test_mappings":{"VL":"HIV-1 VL (LOINC 20447-9)","CT":"Cycle Threshold"},
      "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetIndex":0}
    }'::jsonb,
    '1',
    NOW()
)
ON CONFLICT (analyzer_id) DO UPDATE SET
    config = EXCLUDED.config,
    last_updated = NOW();

-- Verification summary.
SELECT
    (SELECT COUNT(*) FROM analyzer WHERE id IN (2006, 2007, 2013)) AS core_analyzer_count,
    (SELECT COUNT(*) FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'E2E-FILE-GenericFile')) AS analyzer_type_count,
    (SELECT COUNT(*) FROM analyzer WHERE name LIKE 'E2E-FILE-%') AS file_analyzer_count,
    (SELECT COUNT(*) FROM file_import_configuration fic JOIN analyzer a ON fic.analyzer_id = a.id WHERE a.name LIKE 'E2E-FILE-%') AS file_config_count,
    (SELECT COUNT(*) FROM analyzer_test_map WHERE analyzer_id = '2013') AS genexpert_map_count;
