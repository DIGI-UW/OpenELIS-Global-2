SET search_path TO clinlims;

DELETE FROM file_import_configuration
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer_plugin_config
WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'E2E-FILE-%');

DELETE FROM analyzer
WHERE name LIKE 'E2E-FILE-%';

DELETE FROM analyzer_type
WHERE name LIKE 'E2E-FILE-%';

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
) VALUES (
  nextval('analyzer_type_seq'),
  'E2E-FILE-GenericFile',
  'E2E GenericFile analyzer type',
  'FILE',
  'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
  true,
  true,
  '1',
  NOW()
);

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
) VALUES (
  nextval('analyzer_seq'),
  'E2E-FILE-CSV-Analyzer',
  'MOLECULAR',
  'E2E file import analyzer',
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
  last_updated,
  file_format
) VALUES (
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
  NOW(),
  'CSV'
);

INSERT INTO analyzer_plugin_config (
  analyzer_id,
  config,
  sys_user_id,
  last_updated
) VALUES (
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
);

-- E2E-FILE-QuantStudio-Analyzer (GenericFile + QuantStudio profile)
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
) VALUES (
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
  last_updated,
  file_format
) VALUES (
  '22222222-2222-2222-2222-222222222222',
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio-Analyzer'),
  '/data/analyzer-imports/e2e-qs/incoming',
  '*.xls',
  '/data/analyzer-imports/e2e-qs/processed',
  '/data/analyzer-imports/e2e-qs/errors',
  '{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue","Well Position":"position"}',
  E'\t',
  true,
  true,
  '22222222-2222-2222-2222-222222222222',
  '1',
  NOW(),
  'EXCEL'
);

INSERT INTO analyzer_plugin_config (
  analyzer_id,
  config,
  sys_user_id,
  last_updated
) VALUES (
  (SELECT id FROM analyzer WHERE name = 'E2E-FILE-QuantStudio-Analyzer'),
  '{
    "profileMeta":{"id":"quantstudio","version":"1.0.0","displayName":"QuantStudio QS5/QS7"},
    "protocol":{"name":"FILE","format":"EXCEL"},
    "column_mapping":{"Sample Name":"sampleId","Target Name":"testCode","Quantity Mean":"result","CT":"ctValue"},
    "default_test_mappings":{"VIH-1":"HIV-1 VL (LOINC 20447-9)","IC":"Internal Control"},
    "configDefaults":{"fileFormat":"EXCEL","hasHeader":true,"sheetIndex":0}
  }'::jsonb,
  '1',
  NOW()
);

SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile') AS analyzer_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE name LIKE 'E2E-FILE-%') AS analyzer_count,
  (SELECT COUNT(*) FROM file_import_configuration fic JOIN analyzer a ON fic.analyzer_id = a.id WHERE a.name LIKE 'E2E-FILE-%') AS file_config_count,
  (SELECT COUNT(*) FROM analyzer_plugin_config apc JOIN analyzer a ON apc.analyzer_id = a.id WHERE a.name LIKE 'E2E-FILE-%') AS plugin_config_count;
