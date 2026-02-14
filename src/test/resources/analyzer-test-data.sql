-- Analyzer Test Data Fixture
-- Creates sample analyzers, fields, mappings, and errors for E2E/manual testing
-- Task Reference: Test Data Management (Phase 3)
--
-- IMPORTANT: Analyzer 1000 (ASTM Simulator) is configured as "newly added" state:
--   - Status: SETUP (no mappings configured yet)
--   - No analyzer fields (fields should be queried via "Query Analyzer" button)
--   - No field mappings (mappings should be created via Field Mappings page)
--   - No errors (clean state for testing initial workflow)
-- This allows testing the complete workflow from newly-added analyzer to fully configured

-- Ensure unqualified names resolve to clinlims (CI and local psql may differ)
SET search_path TO clinlims;

-- Note: This assumes the analyzer table already exists (legacy table)
-- We'll create test analyzers with IDs starting from 1000 to avoid conflicts

-- Insert test analyzers (analyzer table: id, name, analyzer_type, is_active, last_updated after Liquibase 047)
-- Using INSERT ... ON CONFLICT DO NOTHING to allow re-running this script
-- Note: analyzer_type must match form dropdown options: HEMATOLOGY, CHEMISTRY, IMMUNOLOGY, MICROBIOLOGY, OTHER
INSERT INTO analyzer (id, name, analyzer_type, is_active, last_updated)
VALUES
  (1000, 'Mock Hematology Analyzer (ASTM Simulator)', 'HEMATOLOGY', true, NOW()),
  (1001, 'Chemistry Analyzer 1', 'CHEMISTRY', true, NOW()),
  (1002, 'Immunology Analyzer 1', 'IMMUNOLOGY', true, NOW()),
  (1003, 'Microbiology Analyzer 1', 'MICROBIOLOGY', true, NOW()),
  (1004, 'Hematology Analyzer 2 (Inactive)', 'HEMATOLOGY', false, NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer configurations
-- Note: status field uses unified AnalyzerStatus enum: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE
-- Analyzer 1000 uses the mock server IP (172.20.1.100:5000) for testing
-- Analyzer 1000 is configured as "newly added" (SETUP status, no fields, no mappings) for testing the initial workflow
INSERT INTO analyzer_configuration (id, analyzer_id, ip_address, port, protocol_version, test_unit_ids, status, sys_user_id, last_updated)
VALUES
  ('CONFIG-001', 1000, '172.20.1.100', 5000, 'ASTM LIS2-A2', '1,2,3', 'SETUP', '1', NOW()),
  ('CONFIG-002', 1001, '192.168.1.101', 8081, 'ASTM LIS2-A2', '1,2', 'ERROR_PENDING', '1', NOW()),
  ('CONFIG-003', 1002, '192.168.1.102', 8082, 'ASTM LIS2-A2', '1,2,3,4', 'VALIDATION', '1', NOW()),
  ('CONFIG-004', 1003, '192.168.1.103', 8083, 'ASTM LIS2-A2', '1,2', 'SETUP', '1', NOW()),
  ('CONFIG-005', 1004, '192.168.1.104', 8084, 'ASTM LIS2-A2', NULL, 'INACTIVE', '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer fields (expanded for better testing)
-- NOTE: Analyzer 1000 (ASTM Simulator) has NO fields - fields should be queried via "Query Analyzer" button
-- This simulates a newly-added analyzer that hasn't been queried yet
INSERT INTO analyzer_field (id, analyzer_id, field_name, astm_ref, field_type, unit, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 (Hematology) fields - REMOVED: Analyzer 1000 starts with no fields (newly added state)
  -- Fields will be populated when user clicks "Query Analyzer" button in the Field Mappings page
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
-- Note: mapping_type must be one of: 'TEST_LEVEL', 'RESULT_LEVEL', 'METADATA' (per constraint chk_mapping_type)
-- Note: openelis_field_type must be one of: 'TEST', 'PANEL', 'RESULT', 'ORDER', 'SAMPLE', 'QC', 'METADATA', 'UNIT' (per constraint chk_openelis_field_type)
-- Note: analyzer_id is required (added in changeset 004-016)
-- NOTE: Analyzer 1000 (ASTM Simulator) has NO mappings - simulates newly-added analyzer
INSERT INTO analyzer_field_mapping (id, analyzer_id, analyzer_field_id, openelis_field_id, openelis_field_type, mapping_type, is_required, is_active, sys_user_id, last_updated)
VALUES
  -- Analyzer 1 (Hematology) mappings - REMOVED: Analyzer 1000 starts with no mappings (newly added state)
  -- Mappings will be created when user configures them via the Field Mappings page
  -- Analyzer 2 (Chemistry) mappings
  ('MAPPING-011', 1001, 'FIELD-011', 'sample.accession_number', 'SAMPLE', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-012', 1001, 'FIELD-012', 'patient.id', 'METADATA', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-013', 1001, 'FIELD-013', 'test.code', 'TEST', 'TEST_LEVEL', true, true, '1', NOW()),
  ('MAPPING-014', 1001, 'FIELD-014', 'result.glucose', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-015', 1001, 'FIELD-015', 'result.creatinine', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-016', 1001, 'FIELD-016', 'result.urea', 'RESULT', 'RESULT_LEVEL', true, false, '1', NOW()), -- Draft
  ('MAPPING-017', 1001, 'FIELD-017', 'result.cholesterol', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-018', 1001, 'FIELD-018', 'result.triglycerides', 'RESULT', 'RESULT_LEVEL', false, true, '1', NOW()),
  ('MAPPING-019', 1001, 'FIELD-019', 'result.alt', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-020', 1001, 'FIELD-020', 'result.ast', 'RESULT', 'RESULT_LEVEL', true, false, '1', NOW()), -- Draft
  -- Analyzer 3 (Immunology) mappings
  ('MAPPING-021', 1002, 'FIELD-021', 'sample.accession_number', 'SAMPLE', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-022', 1002, 'FIELD-022', 'patient.id', 'METADATA', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-023', 1002, 'FIELD-023', 'test.code', 'TEST', 'TEST_LEVEL', true, true, '1', NOW()),
  ('MAPPING-024', 1002, 'FIELD-024', 'result.hiv_antibody', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-025', 1002, 'FIELD-025', 'result.hbsag', 'RESULT', 'RESULT_LEVEL', true, false, '1', NOW()), -- Draft
  ('MAPPING-026', 1002, 'FIELD-026', 'result.hcv_antibody', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  ('MAPPING-027', 1002, 'FIELD-027', 'result.syphilis_rpr', 'RESULT', 'RESULT_LEVEL', false, true, '1', NOW()),
  ('MAPPING-028', 1002, 'FIELD-028', 'result.cd4_count', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW()),
  -- Analyzer 4 (Microbiology) mappings
  ('MAPPING-029', 1003, 'FIELD-029', 'sample.accession_number', 'SAMPLE', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-030', 1003, 'FIELD-030', 'patient.id', 'METADATA', 'METADATA', true, true, '1', NOW()),
  ('MAPPING-031', 1003, 'FIELD-031', 'test.code', 'TEST', 'TEST_LEVEL', true, true, '1', NOW()),
  ('MAPPING-032', 1003, 'FIELD-032', 'result.culture_result', 'RESULT', 'RESULT_LEVEL', true, false, '1', NOW()), -- Draft
  ('MAPPING-033', 1003, 'FIELD-033', 'result.antibiotic_sensitivity', 'RESULT', 'RESULT_LEVEL', false, true, '1', NOW()),
  ('MAPPING-034', 1003, 'FIELD-034', 'result.organism_id', 'RESULT', 'RESULT_LEVEL', true, true, '1', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert analyzer errors (for error dashboard testing - expanded with more variety)
-- NOTE: Analyzer 1000 (ASTM Simulator) has NO errors - simulates newly-added analyzer
INSERT INTO analyzer_error (id, analyzer_id, error_type, severity, error_message, raw_message, status, sys_user_id, last_updated)
VALUES
  -- Unacknowledged errors - Analyzer 1 (Hematology) - REMOVED: Analyzer 1000 starts with no errors
  -- Unacknowledged errors - Analyzer 2 (Chemistry)
  ('ERROR-004', 1001, 'CONNECTION', 'CRITICAL', 'Connection timeout after 30 seconds', NULL, 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '45 minutes'),
  ('ERROR-005', 1001, 'MAPPING', 'ERROR', 'No mapping found for field: LDH', 'R|1|^^^LDH|250|IU/L|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '20 minutes'),
  ('ERROR-006', 1001, 'VALIDATION', 'WARNING', 'Value out of range: Glucose 450 mg/dL (normal: 70-100)', 'R|1|^^^GLUCOSE|450|mg/dL|H', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '10 minutes'),
  -- Unacknowledged errors - Analyzer 3 (Immunology)
  ('ERROR-007', 1002, 'PROTOCOL', 'ERROR', 'Invalid ASTM message format', 'INVALID|MESSAGE|FORMAT|DATA', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '5 minutes'),
  ('ERROR-008', 1002, 'MAPPING', 'ERROR', 'No mapping found for field: VDRL', 'R|1|^^^VDRL|NEGATIVE|N|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '2 minutes'),
  -- Acknowledged errors - Analyzer 1000 errors removed (newly added state)
  ('ERROR-010', 1001, 'VALIDATION', 'WARNING', 'Unit mismatch: expected mg/dL, received mmol/L', 'R|1|^^^CREATININE|0.8|mmol/L|N', 'ACKNOWLEDGED', '1', NOW() - INTERVAL '3 hours'),
  -- Resolved errors
  ('ERROR-011', 1002, 'MAPPING', 'ERROR', 'No mapping found for field: TPHA', 'R|1|^^^TPHA|POSITIVE|P|N', 'RESOLVED', '1', NOW() - INTERVAL '1 day'),
  -- Recent errors (last 24 hours) - various types (Analyzer 1000 errors removed)
  ('ERROR-013', 1001, 'CONNECTION', 'CRITICAL', 'Failed to establish connection: Connection refused', NULL, 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '30 minutes'),
  ('ERROR-014', 1003, 'MAPPING', 'ERROR', 'No mapping found for field: GRAM_STAIN', 'R|1|^^^GRAM_STAIN|GRAM_POSITIVE|TEXT|N', 'UNACKNOWLEDGED', '1', NOW() - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- Update acknowledged errors with acknowledgment details
UPDATE analyzer_error
SET acknowledged_by = '1', acknowledged_at = NOW() - INTERVAL '1 hour'
WHERE id IN ('ERROR-010');  -- ERROR-009 removed (analyzer 1000)

-- Update resolved error with resolution details
UPDATE analyzer_error
SET resolved_at = NOW() - INTERVAL '12 hours'
WHERE id = 'ERROR-011';

-- =============================================================================
-- QUALITATIVE RESULT MAPPINGS (spec.md FR-005: Many-to-one mapping support)
-- =============================================================================
-- Maps analyzer qualitative values (POS, POSITIVE, REACTIVE, +, etc.) to
-- OpenELIS standard codes. Supports many-to-one pattern and default values.
-- Reference: specs/004-astm-analyzer-mapping/spec.md FR-005

INSERT INTO qualitative_result_mapping (id, analyzer_field_id, analyzer_value, openelis_code, is_default, sys_user_id, last_updated)
VALUES
  -- HIV Antibody mappings (FIELD-024) - many-to-one pattern
  ('QUAL-001', 'FIELD-024', 'POS', 'POSITIVE', false, '1', NOW()),
  ('QUAL-002', 'FIELD-024', 'POSITIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-003', 'FIELD-024', 'REACTIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-004', 'FIELD-024', '+', 'POSITIVE', false, '1', NOW()),
  ('QUAL-005', 'FIELD-024', 'NEG', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-006', 'FIELD-024', 'NEGATIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-007', 'FIELD-024', 'NON-REACTIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-008', 'FIELD-024', '-', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-009', 'FIELD-024', 'IND', 'INDETERMINATE', true, '1', NOW()),  -- Default for unmapped
  ('QUAL-010', 'FIELD-024', 'INDETERMINATE', 'INDETERMINATE', false, '1', NOW()),
  ('QUAL-011', 'FIELD-024', 'EQUIVOCAL', 'INDETERMINATE', false, '1', NOW()),
  
  -- HBsAg mappings (FIELD-025)
  ('QUAL-012', 'FIELD-025', 'POS', 'POSITIVE', false, '1', NOW()),
  ('QUAL-013', 'FIELD-025', 'POSITIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-014', 'FIELD-025', 'REACTIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-015', 'FIELD-025', 'NEG', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-016', 'FIELD-025', 'NEGATIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-017', 'FIELD-025', 'NON-REACTIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-018', 'FIELD-025', 'IND', 'INDETERMINATE', true, '1', NOW()),  -- Default
  
  -- HCV Antibody mappings (FIELD-026)
  ('QUAL-019', 'FIELD-026', 'POS', 'POSITIVE', false, '1', NOW()),
  ('QUAL-020', 'FIELD-026', 'POSITIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-021', 'FIELD-026', 'REACTIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-022', 'FIELD-026', 'NEG', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-023', 'FIELD-026', 'NEGATIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-024', 'FIELD-026', 'NON-REACTIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-025', 'FIELD-026', 'IND', 'INDETERMINATE', true, '1', NOW()),  -- Default
  
  -- Syphilis RPR mappings (FIELD-027)
  ('QUAL-026', 'FIELD-027', 'POS', 'POSITIVE', false, '1', NOW()),
  ('QUAL-027', 'FIELD-027', 'POSITIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-028', 'FIELD-027', 'REACTIVE', 'POSITIVE', false, '1', NOW()),
  ('QUAL-029', 'FIELD-027', 'NEG', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-030', 'FIELD-027', 'NEGATIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-031', 'FIELD-027', 'NON-REACTIVE', 'NEGATIVE', false, '1', NOW()),
  ('QUAL-032', 'FIELD-027', 'WEAKLY REACTIVE', 'WEAK_POSITIVE', false, '1', NOW()),
  ('QUAL-033', 'FIELD-027', 'IND', 'INDETERMINATE', true, '1', NOW())   -- Default
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- UNIT MAPPINGS WITH CONVERSION FACTORS (spec.md FR-004, data-model.md Section 5)
-- =============================================================================
-- Maps analyzer units to OpenELIS units with optional conversion factors.
-- Reference: specs/004-astm-analyzer-mapping/spec.md FR-004

-- Note: unique constraint on (analyzer_field_id, analyzer_unit) - each source unit maps to ONE target
INSERT INTO unit_mapping (id, analyzer_field_id, analyzer_unit, openelis_unit, conversion_factor, reject_if_mismatch, sys_user_id, last_updated)
VALUES
  -- Glucose unit mappings (FIELD-014) - convert to mg/dL
  ('UNIT-001', 'FIELD-014', 'mg/dL', 'mg/dL', 1.0, false, '1', NOW()),           -- Identity
  ('UNIT-002', 'FIELD-014', 'mmol/L', 'mg/dL', 18.0182, false, '1', NOW()),      -- SI to conventional
  
  -- Creatinine unit mappings (FIELD-015) - convert to mg/dL
  ('UNIT-004', 'FIELD-015', 'mg/dL', 'mg/dL', 1.0, false, '1', NOW()),           -- Identity
  ('UNIT-005', 'FIELD-015', 'μmol/L', 'mg/dL', 0.0113, false, '1', NOW()),       -- SI to conventional
  
  -- Cholesterol unit mappings (FIELD-017) - convert to mg/dL
  ('UNIT-015', 'FIELD-017', 'mg/dL', 'mg/dL', 1.0, false, '1', NOW()),           -- Identity
  ('UNIT-016', 'FIELD-017', 'mmol/L', 'mg/dL', 38.67, false, '1', NOW()),        -- SI to conventional
  
  -- CD4 Count unit mappings (FIELD-028) - convert to cells/μL
  ('UNIT-018', 'FIELD-028', 'cells/μL', 'cells/μL', 1.0, false, '1', NOW()),     -- Identity
  ('UNIT-019', 'FIELD-028', 'cells/mm3', 'cells/μL', 1.0, false, '1', NOW()),    -- Equivalent (1:1)
  ('UNIT-020', 'FIELD-028', 'x10^6/L', 'cells/μL', 1.0, false, '1', NOW())       -- Equivalent (1:1)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- RAW ASTM MESSAGES FOR REPROCESSING (spec.md FR-017, US3)
-- =============================================================================
-- Update existing errors with raw_message content for reprocessing tests.
-- Reference: specs/004-astm-analyzer-mapping/spec.md FR-017

-- Raw messages for analyzer errors (ERROR-001, ERROR-002, ERROR-003 removed - analyzer 1000 has no errors)

-- Chemistry unmapped field for ERROR-005
UPDATE analyzer_error SET raw_message = 
'H|\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250101150000
P|1||PATIENT-004|||F|19851210
O|1|SAMPLE-004^LAB||LIVER|||||||||||||||||
R|1|^^^LDH|250|IU/L|N||F|20250101150100
L|1|N'
WHERE id = 'ERROR-005';

-- Out of range glucose for ERROR-006
UPDATE analyzer_error SET raw_message = 
'H|\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250101160000
P|1||PATIENT-005|||M|19650830
O|1|SAMPLE-005^LAB||GLUCOSE|||||||||||||||||
R|1|^^^GLUCOSE|450|mg/dL|H||F|20250101160100
L|1|N'
WHERE id = 'ERROR-006';

-- Invalid format message for ERROR-007
UPDATE analyzer_error SET raw_message = 
'INVALID|MESSAGE|FORMAT|DATA|||MALFORMED'
WHERE id = 'ERROR-007';

-- Immunology unmapped VDRL for ERROR-008
UPDATE analyzer_error SET raw_message = 
'H|\^&|||Abbott^Architect^i2000|||||||LIS2-A2|20250101170000
P|1||PATIENT-006|||F|19920725
O|1|SAMPLE-006^LAB||VDRL|||||||||||||||||
R|1|^^^VDRL|NEGATIVE||N||F|20250101170100
L|1|N'
WHERE id = 'ERROR-008';

-- =============================================================================
-- Q-SEGMENT TEST DATA (spec.md FR-021: QC Processing)
-- =============================================================================
-- Errors containing Q-segments for quality control result testing.
-- Reference: specs/004-astm-analyzer-mapping/research.md Q-segment format

-- QC error with normal and high control results
INSERT INTO analyzer_error (id, analyzer_id, error_type, severity, error_message, raw_message, status, sys_user_id, last_updated)
VALUES 
  -- Chemistry QC with multiple control levels
  ('ERROR-QC-001', 1001, 'MAPPING', 'ERROR', 'QC mapping incomplete for glucose controls',
'H|\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250101080000
Q|1|GLUCOSE^QC-LOT-2025A^NORMAL|105.2|mg/dL|20250101080000|N
Q|2|GLUCOSE^QC-LOT-2025A^HIGH|210.5|mg/dL|20250101080100|N
Q|3|GLUCOSE^QC-LOT-2025A^LOW|55.8|mg/dL|20250101080200|N
L|1|N',
   'UNACKNOWLEDGED', '1', NOW() - INTERVAL '2 hours'),
   
  -- Hematology QC with CBC controls - REMOVED: Analyzer 1000 has no errors (newly added state)
   
  -- Immunology QC with screening controls
  ('ERROR-QC-003', 1002, 'MAPPING', 'ERROR', 'QC mapping incomplete for immunology controls',
'H|\^&|||Abbott^Architect^i2000|||||||LIS2-A2|20250101100000
Q|1|HIV^QC-SCREEN-001^NEGATIVE|NEGATIVE||20250101100000|N
Q|2|HIV^QC-SCREEN-001^POSITIVE|POSITIVE||20250101100100|N
Q|3|HBSAG^QC-SCREEN-001^NEGATIVE|NEGATIVE||20250101100200|N
Q|4|HBSAG^QC-SCREEN-001^POSITIVE|POSITIVE||20250101100300|N
L|1|N',
   'UNACKNOWLEDGED', '1', NOW() - INTERVAL '4 hours'),

  -- Mixed patient and QC results in same message
  ('ERROR-QC-004', 1001, 'VALIDATION', 'WARNING', 'Mixed patient/QC message requires validation',
'H|\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250101110000
P|1||PATIENT-007|||M|19880315
O|1|SAMPLE-007^LAB||LIPID|||||||||||||||||
R|1|^^^CHOLESTEROL|185|mg/dL|N||F|20250101110100
R|2|^^^TRIGLYCERIDES|120|mg/dL|N||F|20250101110200
Q|1|CHOLESTEROL^QC-LIPID-001^NORMAL|180.5|mg/dL|20250101110300|N
Q|2|TRIGLYCERIDES^QC-LIPID-001^NORMAL|115.2|mg/dL|20250101110400|N
L|1|N',
   'UNACKNOWLEDGED', '1', NOW() - INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- ADDITIONAL ERROR SCENARIOS FOR COMPREHENSIVE TESTING
-- =============================================================================

-- Duplicate sample ID scenario - REMOVED: Analyzer 1000 has no errors (newly added state)

-- Patient ID mismatch scenario
INSERT INTO analyzer_error (id, analyzer_id, error_type, severity, error_message, raw_message, status, sys_user_id, last_updated)
VALUES 
  ('ERROR-PAT-001', 1001, 'VALIDATION', 'CRITICAL', 'Patient ID mismatch between order and result',
'H|\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250102090000
P|1||PATIENT-999|||M|19700101
O|1|SAMPLE-008^LAB||CHEM|||||||||||||||||
R|1|^^^GLUCOSE|95|mg/dL|N||F|20250102090100
L|1|N',
   'UNACKNOWLEDGED', '1', NOW() - INTERVAL '45 minutes')
ON CONFLICT (id) DO NOTHING;

