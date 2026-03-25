-- ASTM Full Fixture: additional analyzers for multi-port mock (2014–2017).
-- Load after analyzer-minimal.sql. Mock ports: 9601=Mindray, 9602=Stago, 9603=Horiba Micros, 9604=Horiba Pentra.
-- All point at 172.20.1.100 with port 9601..9604 for request-based template selection.

SET search_path TO clinlims;

INSERT INTO analyzer (id, name, analyzer_type, description, is_active,
                      ip_address, port, protocol_version, status,
                      identifier_pattern, last_updated)
VALUES
  (2014, 'Mindray BA-88A (port 9601)', 'HEMATOLOGY', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9601, 'ASTM_LIS2_A2', 'ACTIVE',
   'MINDRAY', NOW()),
  (2015, 'Stago STart 4', 'CHEMISTRY', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9602, 'ASTM_LIS2_A2', 'ACTIVE',
   'STAGO.*|START4', NOW()),
  (2016, 'Horiba ABX Micros 60', 'HEMATOLOGY', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9603, 'ASTM_LIS2_A2', 'ACTIVE',
   'HORIBA.*|ABX.*MICROS', NOW()),
  (2017, 'Horiba ABX Pentra 60', 'HEMATOLOGY', 'ASTM LIS2-A2 over TCP/IP', true,
   '172.20.1.100', 9604, 'ASTM_LIS2_A2', 'ACTIVE',
   'HORIBA.*|ABX.*PENTRA', NOW())
ON CONFLICT (id) DO NOTHING;

UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'
) WHERE id IN (2014, 2015, 2016, 2017) AND analyzer_type_id IS NULL;

-- Test mappings: PK (analyzer_type_id, analyzer_test_name). One row per test name per type.
INSERT INTO analyzer_test_map (analyzer_type_id, analyzer_id, analyzer_test_name, test_id, last_updated)
VALUES
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2014', 'WBC',  '3',  NOW()),
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2014', 'RBC',  '5',  NOW()),
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2015', 'PT',   '3',  NOW()),
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2015', 'INR',  '5',  NOW()),
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2016', 'HGB',  '3',  NOW()),
  ((SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'), '2017', 'HCT',  '5',  NOW())
ON CONFLICT (analyzer_type_id, analyzer_test_name) DO NOTHING;
