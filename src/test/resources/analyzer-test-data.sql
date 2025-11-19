-- Analyzer Test Data Fixture
-- Creates sample analyzers, fields, mappings, and errors for E2E/manual testing
-- Task Reference: Test Data Management (Phase 3)

-- Note: This assumes the analyzer table already exists (legacy table)
-- We'll create test analyzers with IDs starting from 1000 to avoid conflicts

-- Insert test analyzers (analyzer table has: id, name, analyzer_type, is_active, lastupdated)
-- Using INSERT ... ON CONFLICT DO NOTHING to allow re-running this script
-- Note: analyzer_type must match form dropdown options: HEMATOLOGY, CHEMISTRY, IMMUNOLOGY, MICROBIOLOGY, OTHER
INSERT INTO analyzer (id, name, analyzer_type, is_active, lastupdated)
VALUES
  (1000, 'Hematology Analyzer 1', 'HEMATOLOGY', true, NOW()),
  (1001, 'Chemistry Analyzer 1', 'CHEMISTRY', true, NOW()),
  (1002, 'Immunology Analyzer 1', 'IMMUNOLOGY', true, NOW()),
  (1003, 'Microbiology Analyzer 1', 'MICROBIOLOGY', true, NOW()),
  (1004, 'Hematology Analyzer 2 (Inactive)', 'HEMATOLOGY', false, NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer configurations
INSERT INTO analyzer_configuration (id, analyzer_id, ip_address, port, protocol_version, test_unit_ids, sys_user_id, last_updated)
VALUES
  ('CONFIG-001', 1000, '192.168.1.100', 8080, 'ASTM LIS2-A2', '1,2,3', '1', NOW()),
  ('CONFIG-002', 1001, '192.168.1.101', 8081, 'ASTM LIS2-A2', '1,2', '1', NOW()),
  ('CONFIG-003', 1002, '192.168.1.102', 8082, 'ASTM LIS2-A2', '1,2,3,4', '1', NOW()),
  ('CONFIG-004', 1003, '192.168.1.103', 8083, 'ASTM LIS2-A2', '1,2', '1', NOW()),
  ('CONFIG-005', 1004, '192.168.1.104', 8084, 'ASTM LIS2-A2', NULL, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer fields (expanded for better testing)
INSERT INTO analyzer_field (id, analyzer_id, field_name, astm_ref, field_type, unit, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 (Hematology) fields - 10 fields
  ('FIELD-001', 1000, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-002', 1000, 'Patient ID', 'P|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-003', 1000, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-004', 1000, 'WBC Count', 'R|1|^^^WBC', 'NUMERIC', '10^3/μL', true, '1', NOW()),
  ('FIELD-005', 1000, 'RBC Count', 'R|1|^^^RBC', 'NUMERIC', '10^6/μL', true, '1', NOW()),
  ('FIELD-006', 1000, 'Hemoglobin', 'R|1|^^^HGB', 'NUMERIC', 'g/dL', true, '1', NOW()),
  ('FIELD-007', 1000, 'Hematocrit', 'R|1|^^^HCT', 'NUMERIC', '%', true, '1', NOW()),
  ('FIELD-008', 1000, 'Platelet Count', 'R|1|^^^PLT', 'NUMERIC', '10^3/μL', true, '1', NOW()),
  ('FIELD-009', 1000, 'MCV', 'R|1|^^^MCV', 'NUMERIC', 'fL', true, '1', NOW()),
  ('FIELD-010', 1000, 'MCH', 'R|1|^^^MCH', 'NUMERIC', 'pg', true, '1', NOW()),
  -- Analyzer 2 (Chemistry) fields - 10 fields
  ('FIELD-011', 1001, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-012', 1001, 'Patient ID', 'P|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-013', 1001, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-014', 1001, 'Glucose', 'R|1|^^^GLUCOSE', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-015', 1001, 'Creatinine', 'R|1|^^^CREATININE', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-016', 1001, 'Urea', 'R|1|^^^UREA', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-017', 1001, 'Total Cholesterol', 'R|1|^^^CHOLESTEROL', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-018', 1001, 'Triglycerides', 'R|1|^^^TRIGLYCERIDES', 'NUMERIC', 'mg/dL', true, '1', NOW()),
  ('FIELD-019', 1001, 'ALT', 'R|1|^^^ALT', 'NUMERIC', 'IU/L', true, '1', NOW()),
  ('FIELD-020', 1001, 'AST', 'R|1|^^^AST', 'NUMERIC', 'IU/L', true, '1', NOW()),
  -- Analyzer 3 (Immunology) fields - 8 fields
  ('FIELD-021', 1002, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-022', 1002, 'Patient ID', 'P|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-023', 1002, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-024', 1002, 'HIV Antibody', 'R|1|^^^HIV', 'QUALITATIVE', NULL, true, '1', NOW()),
  ('FIELD-025', 1002, 'HBsAg', 'R|1|^^^HBSAG', 'QUALITATIVE', NULL, true, '1', NOW()),
  ('FIELD-026', 1002, 'HCV Antibody', 'R|1|^^^HCV', 'QUALITATIVE', NULL, true, '1', NOW()),
  ('FIELD-027', 1002, 'Syphilis RPR', 'R|1|^^^SYPHILIS', 'QUALITATIVE', NULL, true, '1', NOW()),
  ('FIELD-028', 1002, 'CD4 Count', 'R|1|^^^CD4', 'NUMERIC', 'cells/μL', true, '1', NOW()),
  -- Analyzer 4 (Microbiology) fields - 6 fields
  ('FIELD-029', 1003, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-030', 1003, 'Patient ID', 'P|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-031', 1003, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-032', 1003, 'Culture Result', 'R|1|^^^CULTURE', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-033', 1003, 'Antibiotic Sensitivity', 'R|1|^^^SENSITIVITY', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-034', 1003, 'Organism ID', 'R|1|^^^ORGANISM', 'TEXT', NULL, true, '1', NOW()),
  -- Analyzer 5 (Inactive) fields - 3 fields
  ('FIELD-035', 1004, 'Sample ID', 'H|1', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-036', 1004, 'Test Code', 'O|1|', 'TEXT', NULL, true, '1', NOW()),
  ('FIELD-037', 1004, 'Result Value', 'R|1|', 'NUMERIC', 'mg/dL', true, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer field mappings (mix of active and draft for comprehensive testing)
INSERT INTO analyzer_field_mapping (id, analyzer_field_id, openelis_field_id, openelis_field_type, mapping_type, is_required, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 (Hematology) mappings - mix of active and draft
  ('MAPPING-001', 'FIELD-001', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-002', 'FIELD-002', 'patient.id', 'PATIENT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-003', 'FIELD-003', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-004', 'FIELD-004', 'result.wbc_count', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-005', 'FIELD-005', 'result.rbc_count', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-006', 'FIELD-006', 'result.hemoglobin', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-007', 'FIELD-007', 'result.hematocrit', 'RESULT', 'DIRECT', true, false, '1', NOW()), -- Draft
  ('MAPPING-008', 'FIELD-008', 'result.platelet_count', 'RESULT', 'DIRECT', true, false, '1', NOW()), -- Draft
  ('MAPPING-009', 'FIELD-009', 'result.mcv', 'RESULT', 'DIRECT', false, true, '1', NOW()),
  ('MAPPING-010', 'FIELD-010', 'result.mch', 'RESULT', 'DIRECT', false, false, '1', NOW()), -- Draft
  -- Analyzer 2 (Chemistry) mappings
  ('MAPPING-011', 'FIELD-011', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-012', 'FIELD-012', 'patient.id', 'PATIENT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-013', 'FIELD-013', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-014', 'FIELD-014', 'result.glucose', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-015', 'FIELD-015', 'result.creatinine', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-016', 'FIELD-016', 'result.urea', 'RESULT', 'DIRECT', true, false, '1', NOW()), -- Draft
  ('MAPPING-017', 'FIELD-017', 'result.cholesterol', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-018', 'FIELD-018', 'result.triglycerides', 'RESULT', 'DIRECT', false, true, '1', NOW()),
  ('MAPPING-019', 'FIELD-019', 'result.alt', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-020', 'FIELD-020', 'result.ast', 'RESULT', 'DIRECT', true, false, '1', NOW()), -- Draft
  -- Analyzer 3 (Immunology) mappings
  ('MAPPING-021', 'FIELD-021', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-022', 'FIELD-022', 'patient.id', 'PATIENT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-023', 'FIELD-023', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-024', 'FIELD-024', 'result.hiv_antibody', 'QUALITATIVE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-025', 'FIELD-025', 'result.hbsag', 'QUALITATIVE', 'DIRECT', true, false, '1', NOW()), -- Draft
  ('MAPPING-026', 'FIELD-026', 'result.hcv_antibody', 'QUALITATIVE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-027', 'FIELD-027', 'result.syphilis_rpr', 'QUALITATIVE', 'DIRECT', false, true, '1', NOW()),
  ('MAPPING-028', 'FIELD-028', 'result.cd4_count', 'RESULT', 'DIRECT', true, true, '1', NOW()),
  -- Analyzer 4 (Microbiology) mappings
  ('MAPPING-029', 'FIELD-029', 'sample.accession_number', 'SAMPLE', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-030', 'FIELD-030', 'patient.id', 'PATIENT', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-031', 'FIELD-031', 'test.code', 'TEST', 'DIRECT', true, true, '1', NOW()),
  ('MAPPING-032', 'FIELD-032', 'result.culture_result', 'TEXT', 'DIRECT', true, false, '1', NOW()), -- Draft
  ('MAPPING-033', 'FIELD-033', 'result.antibiotic_sensitivity', 'TEXT', 'DIRECT', false, true, '1', NOW()),
  ('MAPPING-034', 'FIELD-034', 'result.organism_id', 'TEXT', 'DIRECT', true, true, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer errors (for error dashboard testing - expanded with more variety)
INSERT INTO analyzer_error (id, analyzer_id, error_type, severity, error_message, raw_message, status, sys_user_id, last_updated)
VALUES
  -- Unacknowledged errors - Analyzer 1 (Hematology)
  ('ERROR-001', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: MCHC', 'R|1|^^^MCHC|32.5|g/dL|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '1 hour'),
  ('ERROR-002', 1000, 'VALIDATION', 'WARNING', 'Unit mismatch: expected 10^3/μL, received /mm3', 'R|1|^^^WBC|5.2|/mm3|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '30 minutes'),
  ('ERROR-003', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: RDW', 'R|1|^^^RDW|14.2|%|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '15 minutes'),
  -- Unacknowledged errors - Analyzer 2 (Chemistry)
  ('ERROR-004', 1001, 'CONNECTION', 'CRITICAL', 'Connection timeout after 30 seconds', NULL, 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '45 minutes'),
  ('ERROR-005', 1001, 'MAPPING', 'ERROR', 'No mapping found for field: LDH', 'R|1|^^^LDH|250|IU/L|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '20 minutes'),
  ('ERROR-006', 1001, 'VALIDATION', 'WARNING', 'Value out of range: Glucose 450 mg/dL (normal: 70-100)', 'R|1|^^^GLUCOSE|450|mg/dL|H', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '10 minutes'),
  -- Unacknowledged errors - Analyzer 3 (Immunology)
  ('ERROR-007', 1002, 'PROTOCOL', 'ERROR', 'Invalid ASTM message format', 'INVALID|MESSAGE|FORMAT|DATA', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '5 minutes'),
  ('ERROR-008', 1002, 'MAPPING', 'ERROR', 'No mapping found for field: VDRL', 'R|1|^^^VDRL|NEGATIVE|N|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '2 minutes'),
  -- Acknowledged errors
  ('ERROR-009', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: MPV', 'R|1|^^^MPV|8.5|fL|N', 'ACKNOWLEDGED', '1', NOW() - INTERVAL '2 hours'),
  ('ERROR-010', 1001, 'VALIDATION', 'WARNING', 'Unit mismatch: expected mg/dL, received mmol/L', 'R|1|^^^CREATININE|0.8|mmol/L|N', 'ACKNOWLEDGED', '1', NOW() - INTERVAL '3 hours'),
  -- Resolved errors
  ('ERROR-011', 1002, 'MAPPING', 'ERROR', 'No mapping found for field: TPHA', 'R|1|^^^TPHA|POSITIVE|P|N', 'RESOLVED', '1', NOW() - INTERVAL '1 day'),
  -- Recent errors (last 24 hours) - various types
  ('ERROR-012', 1000, 'MAPPING', 'ERROR', 'No mapping found for field: BASOPHILS', 'R|1|^^^BASOPHILS|0.1|%|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '1 hour'),
  ('ERROR-013', 1001, 'CONNECTION', 'CRITICAL', 'Failed to establish connection: Connection refused', NULL, 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '30 minutes'),
  ('ERROR-014', 1003, 'MAPPING', 'ERROR', 'No mapping found for field: GRAM_STAIN', 'R|1|^^^GRAM_STAIN|GRAM_POSITIVE|TEXT|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- Update acknowledged errors with acknowledgment details
UPDATE analyzer_error
SET acknowledged_by = '1', acknowledged_at = NOW() - INTERVAL '1 hour'
WHERE id IN ('ERROR-009', 'ERROR-010');

-- Update resolved error with resolution details
UPDATE analyzer_error
SET resolved_at = NOW() - INTERVAL '12 hours'
WHERE id = 'ERROR-011';

