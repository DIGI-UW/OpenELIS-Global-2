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

-- E2E-FILE-Tecan-F50 (Tecan Infinite F50, well-per-row CSV)
INSERT INTO analyzer (id, name, analyzer_type, description, is_active, protocol_version, status, analyzer_type_id, last_updated)
VALUES (nextval('analyzer_seq'), 'E2E-FILE-Tecan-F50', 'MOLECULAR', 'E2E Tecan F50 file import', true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'), NOW());

INSERT INTO file_import_configuration (id, analyzer_id, import_directory, file_pattern, archive_directory, error_directory, column_mappings, delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format)
VALUES ('33333333-3333-3333-3333-333333333333', (SELECT id FROM analyzer WHERE name = 'E2E-FILE-Tecan-F50'),
  '/data/analyzer-imports/e2e-tecan/incoming', '*.csv', '/data/analyzer-imports/e2e-tecan/processed', '/data/analyzer-imports/e2e-tecan/errors',
  '{"WellPosition":"position","SampleID":"sampleId","OD_450":"result"}', E'\t', true, true, '33333333-3333-3333-3333-333333333333', '1', NOW(), 'CSV');

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES ((SELECT id FROM analyzer WHERE name = 'E2E-FILE-Tecan-F50'),
  '{"profileMeta":{"id":"tecan-f50","version":"1.0.0","displayName":"Tecan Infinite F50"},"protocol":{"name":"FILE","format":"CSV"},"column_mapping":{"WellPosition":"position","SampleID":"sampleId","OD_450":"result"},"configDefaults":{"fileFormat":"CSV","delimiter":"\t"}}'::jsonb, '1', NOW());

-- E2E-FILE-Multiskan-FC (Thermo Multiskan FC, well-per-row CSV)
INSERT INTO analyzer (id, name, analyzer_type, description, is_active, protocol_version, status, analyzer_type_id, last_updated)
VALUES (nextval('analyzer_seq'), 'E2E-FILE-Multiskan-FC', 'MOLECULAR', 'E2E Multiskan FC file import', true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'), NOW());

INSERT INTO file_import_configuration (id, analyzer_id, import_directory, file_pattern, archive_directory, error_directory, column_mappings, delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format)
VALUES ('44444444-4444-4444-4444-444444444444', (SELECT id FROM analyzer WHERE name = 'E2E-FILE-Multiskan-FC'),
  '/data/analyzer-imports/e2e-multiskan/incoming', '*.csv', '/data/analyzer-imports/e2e-multiskan/processed', '/data/analyzer-imports/e2e-multiskan/errors',
  '{"Well":"position","Sample ID":"sampleId","Absorbance (450 nm)":"result"}', ',', true, true, '44444444-4444-4444-4444-444444444444', '1', NOW(), 'CSV');

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES ((SELECT id FROM analyzer WHERE name = 'E2E-FILE-Multiskan-FC'),
  '{"profileMeta":{"id":"multiskan-fc","version":"1.0.0","displayName":"Thermo Multiskan FC"},"protocol":{"name":"FILE","format":"CSV"},"column_mapping":{"Well":"position","Sample ID":"sampleId","Absorbance (450 nm)":"result"},"configDefaults":{"fileFormat":"CSV","delimiter":","}}'::jsonb, '1', NOW());

-- E2E-FILE-FluoroCycler-XT (Bruker FluoroCycler XT, Excel 12-column)
INSERT INTO analyzer (id, name, analyzer_type, description, is_active, protocol_version, status, analyzer_type_id, last_updated)
VALUES (nextval('analyzer_seq'), 'E2E-FILE-FluoroCycler-XT', 'MOLECULAR', 'E2E FluoroCycler XT file import', true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'), NOW());

INSERT INTO file_import_configuration (id, analyzer_id, import_directory, file_pattern, archive_directory, error_directory, column_mappings, delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format)
VALUES ('55555555-5555-5555-5555-555555555555', (SELECT id FROM analyzer WHERE name = 'E2E-FILE-FluoroCycler-XT'),
  '/data/analyzer-imports/e2e-fluorocycler/incoming', '*.xlsx', '/data/analyzer-imports/e2e-fluorocycler/processed', '/data/analyzer-imports/e2e-fluorocycler/errors',
  '{"SampleID":"sampleId","WellPosition":"position","CP":"result","Interpretation":"interpretation"}', ',', true, true, '55555555-5555-5555-5555-555555555555', '1', NOW(), 'EXCEL');

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES ((SELECT id FROM analyzer WHERE name = 'E2E-FILE-FluoroCycler-XT'),
  '{"profileMeta":{"id":"fluorocycler-xt","version":"1.0.0","displayName":"Bruker FluoroCycler XT"},"protocol":{"name":"FILE","format":"EXCEL"},"column_mapping":{"SampleID":"sampleId","WellPosition":"position","CP":"result","Interpretation":"interpretation"},"configDefaults":{"fileFormat":"EXCEL","hasHeader":true}}'::jsonb, '1', NOW());

-- E2E-FILE-DT-Prime (DNA Technology DT-Prime, XML)
INSERT INTO analyzer (id, name, analyzer_type, description, is_active, protocol_version, status, analyzer_type_id, last_updated)
VALUES (nextval('analyzer_seq'), 'E2E-FILE-DT-Prime', 'MOLECULAR', 'E2E DT-Prime XML file import', true, NULL, 'ACTIVE',
  (SELECT id FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile'), NOW());

INSERT INTO file_import_configuration (id, analyzer_id, import_directory, file_pattern, archive_directory, error_directory, column_mappings, delimiter, has_header, active, fhir_uuid, sys_user_id, last_updated, file_format)
VALUES ('66666666-6666-6666-6666-666666666666', (SELECT id FROM analyzer WHERE name = 'E2E-FILE-DT-Prime'),
  '/data/analyzer-imports/e2e-dtprime/incoming', '*.xml', '/data/analyzer-imports/e2e-dtprime/processed', '/data/analyzer-imports/e2e-dtprime/errors',
  '{"SampleID":"sampleId","WellPosition":"position","Result":"result","Interpretation":"interpretation"}', ',', true, true, '66666666-6666-6666-6666-666666666666', '1', NOW(), 'XML');

INSERT INTO analyzer_plugin_config (analyzer_id, config, sys_user_id, last_updated)
VALUES ((SELECT id FROM analyzer WHERE name = 'E2E-FILE-DT-Prime'),
  '{"profileMeta":{"id":"dtprime","version":"1.0.0","displayName":"DNA Technology DT-Prime"},"protocol":{"name":"FILE","format":"XML"},"column_mapping":{"SampleID":"sampleId","WellPosition":"position","Result":"result","Interpretation":"interpretation"},"configDefaults":{"fileFormat":"XML"}}'::jsonb, '1', NOW());

SELECT
  (SELECT COUNT(*) FROM analyzer_type WHERE name = 'E2E-FILE-GenericFile') AS analyzer_type_count,
  (SELECT COUNT(*) FROM analyzer WHERE name LIKE 'E2E-FILE-%') AS analyzer_count,
  (SELECT COUNT(*) FROM file_import_configuration fic JOIN analyzer a ON fic.analyzer_id = a.id WHERE a.name LIKE 'E2E-FILE-%') AS file_config_count,
  (SELECT COUNT(*) FROM analyzer_plugin_config apc JOIN analyzer a ON apc.analyzer_id = a.id WHERE a.name LIKE 'E2E-FILE-%') AS plugin_config_count;
