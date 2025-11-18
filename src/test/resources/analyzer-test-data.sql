-- Analyzer Test Data Fixture
-- Creates sample analyzers, fields, mappings, and errors for E2E/manual testing
-- Task Reference: Test Data Management (Phase 3)

-- Note: This assumes the analyzer table already exists (legacy table)
-- We'll create test analyzers with IDs starting from 1000 to avoid conflicts

-- Insert test analyzers (analyzer table has: id, name, analyzer_type, is_active, lastupdated)
-- Using INSERT ... ON CONFLICT DO NOTHING to allow re-running this script
INSERT INTO analyzer (id, name, analyzer_type, is_active, lastupdated)
VALUES
  (1000, 'Test Analyzer 1', 'ASTM', true, NOW()),
  (1001, 'Test Analyzer 2', 'ASTM', true, NOW()),
  (1002, 'Test Analyzer 3 (Inactive)', 'ASTM', false, NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer configurations
INSERT INTO analyzer_configuration (id, analyzer_id, ip_address, port, protocol_version, test_unit_ids, sys_user_id, last_updated)
VALUES
  ('CONFIG-001', 1000, '192.168.1.100', 8080, 'ASTM LIS2-A2', '1,2,3', '1', NOW()),
  ('CONFIG-002', 1001, '192.168.1.101', 8081, 'ASTM LIS2-A2', '1,2', '1', NOW()),
  ('CONFIG-003', 1002, '192.168.1.102', 8082, 'ASTM LIS2-A2', NULL, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer fields
INSERT INTO analyzer_field (id, analyzer_id, field_name, astm_ref, field_type, unit, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 fields
  ('FIELD-001', 1000, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-002', 1000, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-003', 1000, 'Result Value', 'R|1|', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-004', 1000, 'Unit', 'R|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-005', 1000, 'Qualitative Result', 'R|1|', 'QUALITATIVE', NULL, true, '1', NOW()),
  -- Analyzer 2 fields
  ('FIELD-006', 1001, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-007', 1001, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-008', 1001, 'Result Value', 'R|1|', 'NUMERIC', 'IU/L', true, '1', NOW()),
  -- Analyzer 3 fields (inactive analyzer)
  ('FIELD-009', 1002, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer field mappings (some active, some draft)
INSERT INTO analyzer_field_mapping (id, analyzer_field_id, openelis_field_id, openelis_field_type, mapping_type, is_required, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 mappings (active)
  ('MAPPING-001', 'FIELD-001', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-002', 'FIELD-002', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-003', 'FIELD-003', 'result.value', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  -- Analyzer 1 mappings (draft)
  ('MAPPING-004', 'FIELD-004', 'result.unit', 'UNIT', 'DIRECT', false, false, '1', NOW()),
  ('MAPPING-005', 'FIELD-005', 'result.qualitative', 'QUALITATIVE', 'DIRECT', false, false, '1', NOW()),
  -- Analyzer 2 mappings (active)
  ('MAPPING-006', 'FIELD-006', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-007', 'FIELD-007', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-008', 'FIELD-008', 'result.value', 'RESULT', 'DIRECT', true, true, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer errors (for error dashboard testing)
INSERT INTO analyzer_error (id, analyzer_id, error_type, severity, error_message, raw_message, status, sys_user_id, last_updated)
VALUES
  -- Unacknowledged errors
  ('ERROR-001', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: UNMAPPED_FIELD_001', 'H|1|UNMAPPED_FIELD_001', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '1 hour'),
  ('ERROR-002', 1000, 'VALIDATION', 'WARNING', 'Unit mismatch: expected mg/dL, received g/L', 'R|1|123.45|g/L', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '30 minutes'),
  ('ERROR-003', 1001, 'CONNECTION', 'CRITICAL', 'Connection timeout after 30 seconds', NULL, 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '15 minutes'),
  -- Acknowledged errors
  ('ERROR-004', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: UNMAPPED_FIELD_002', 'H|1|UNMAPPED_FIELD_002', 'ACKNOWLEDGED', '1', NOW() - INTERVAL '2 hours'),
  -- Recent error (last 24 hours)
  ('ERROR-005', 1001, 'PROTOCOL', 'ERROR', 'Invalid ASTM message format', 'INVALID|MESSAGE|FORMAT', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '5 minutes')
ON CONFLICT (id) DO NOTHING;

-- Update acknowledged error with acknowledgment details
UPDATE analyzer_error
SET acknowledged_by = '1', acknowledged_at = NOW() - INTERVAL '1 hour'
WHERE id = 'ERROR-004';

