-- Test Data for Storage Management Integration Testing
-- Run this script to populate test storage hierarchy for testing P1 user story
-- Usage: psql -U clinlims -d clinlims -f storage-test-data.sql

-- Clean up existing test data (if any)
-- Clean up E2E test data (patients, samples, assignments)
DELETE FROM sample_storage_movement WHERE sample_id IN (
  SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
);
DELETE FROM sample_storage_assignment WHERE sample_id IN (
  SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
);
DELETE FROM sample_item WHERE samp_id IN (
  SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
);
DELETE FROM sample_human WHERE samp_id IN (
  SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
);
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
DELETE FROM patient_identity WHERE patient_id IN (
  SELECT id FROM patient WHERE external_id LIKE 'E2E-%'
);
DELETE FROM patient WHERE external_id LIKE 'E2E-%';
DELETE FROM person WHERE id IN (
  SELECT person_id FROM patient WHERE external_id LIKE 'E2E-%'
  UNION
  SELECT id FROM person WHERE last_name LIKE 'E2E-%'
);
-- Clean up storage hierarchy test data
DELETE FROM storage_position WHERE id BETWEEN 100 AND 10000;
DELETE FROM storage_rack WHERE id BETWEEN 30 AND 100;
DELETE FROM storage_shelf WHERE id BETWEEN 20 AND 100;
DELETE FROM storage_device WHERE id BETWEEN 10 AND 100;
DELETE FROM storage_room WHERE id BETWEEN 1 AND 100;

-- Insert Test Rooms
INSERT INTO storage_room (id, fhir_uuid, name, code, description, active, sys_user_id, last_updated) VALUES
(1, gen_random_uuid(), 'Main Laboratory', 'MAIN', 'Primary laboratory storage facility', true, 1, CURRENT_TIMESTAMP),
(2, gen_random_uuid(), 'Secondary Laboratory', 'SEC', 'Secondary storage area', true, 1, CURRENT_TIMESTAMP),
(3, gen_random_uuid(), 'Inactive Room', 'INACTIVE', 'Deactivated room for testing inactive validation', false, 1, CURRENT_TIMESTAMP);

-- Insert Test Devices
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated) VALUES
(10, gen_random_uuid(), 'Freezer Unit 1', 'FRZ01', 'freezer', -80.0, 500, true, 1, 1, CURRENT_TIMESTAMP),
(11, gen_random_uuid(), 'Refrigerator Unit 1', 'REF01', 'refrigerator', 4.0, 300, true, 1, 1, CURRENT_TIMESTAMP),
(12, gen_random_uuid(), 'Cabinet Unit 1', 'CAB01', 'cabinet', NULL, NULL, true, 2, 1, CURRENT_TIMESTAMP),
(13, gen_random_uuid(), 'Inactive Freezer', 'INACTIVE-FRZ', 'freezer', NULL, NULL, false, 3, 1, CURRENT_TIMESTAMP);

-- Insert Test Shelves
INSERT INTO storage_shelf (id, fhir_uuid, label, capacity_limit, active, parent_device_id, sys_user_id, last_updated) VALUES
(20, gen_random_uuid(), 'Shelf-A', 50, true, 10, 1, CURRENT_TIMESTAMP),
(21, gen_random_uuid(), 'Shelf-B', 50, true, 10, 1, CURRENT_TIMESTAMP),
(22, gen_random_uuid(), 'Shelf-1', NULL, true, 11, 1, CURRENT_TIMESTAMP),
(23, gen_random_uuid(), 'Shelf-1', NULL, true, 12, 1, CURRENT_TIMESTAMP);

-- Insert Test Racks
INSERT INTO storage_rack (id, fhir_uuid, label, rows, columns, position_schema_hint, active, parent_shelf_id, sys_user_id, last_updated) VALUES
(30, gen_random_uuid(), 'Rack R1', 8, 12, 'A1', true, 20, 1, CURRENT_TIMESTAMP),
(31, gen_random_uuid(), 'Rack R2', 10, 10, '1-1', true, 20, 1, CURRENT_TIMESTAMP),
(32, gen_random_uuid(), 'Rack R3', 0, 0, NULL, true, 21, 1, CURRENT_TIMESTAMP),
(33, gen_random_uuid(), 'Rack R1', 8, 12, NULL, true, 22, 1, CURRENT_TIMESTAMP),
(34, gen_random_uuid(), 'Rack R1', 8, 12, 'A1', true, 23, 1, CURRENT_TIMESTAMP);

-- Insert Test Positions
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_rack_id, sys_user_id, last_updated) VALUES
-- Rack R1 (8x12 grid) - First row
(100, gen_random_uuid(), 'A1', 1, 1, false, 30, 1, CURRENT_TIMESTAMP),
(101, gen_random_uuid(), 'A2', 1, 2, false, 30, 1, CURRENT_TIMESTAMP),
(102, gen_random_uuid(), 'A3', 1, 3, true, 30, 1, CURRENT_TIMESTAMP), -- Occupied for testing
(103, gen_random_uuid(), 'A4', 1, 4, false, 30, 1, CURRENT_TIMESTAMP),
(104, gen_random_uuid(), 'A5', 1, 5, false, 30, 1, CURRENT_TIMESTAMP),
(105, gen_random_uuid(), 'A6', 1, 6, false, 30, 1, CURRENT_TIMESTAMP),
(106, gen_random_uuid(), 'A7', 1, 7, false, 30, 1, CURRENT_TIMESTAMP),
(107, gen_random_uuid(), 'A8', 1, 8, false, 30, 1, CURRENT_TIMESTAMP),

-- Rack R2 - First position
(200, gen_random_uuid(), '1-1', 1, 1, false, 31, 1, CURRENT_TIMESTAMP),

-- Rack R3 (no grid) - flexible positions
(110, gen_random_uuid(), 'RED-01', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP),
(111, gen_random_uuid(), 'RED-02', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP),
(112, gen_random_uuid(), 'RED-01', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP), -- Duplicate coordinate (allowed)

-- Position in rack 33 (Refrigerator Unit 1 > Shelf-1 > Rack R1)
(120, gen_random_uuid(), 'X1', NULL, NULL, false, 33, 1, CURRENT_TIMESTAMP),
(121, gen_random_uuid(), 'A1', 1, 1, false, 33, 1, CURRENT_TIMESTAMP),

-- Positions in rack 34 (Secondary Lab > Cabinet > Shelf-1 > Rack R1)
(130, gen_random_uuid(), 'A1', 1, 1, false, 34, 1, CURRENT_TIMESTAMP),
(131, gen_random_uuid(), 'A2', 1, 2, false, 34, 1, CURRENT_TIMESTAMP),
(132, gen_random_uuid(), 'A3', 1, 3, false, 34, 1, CURRENT_TIMESTAMP);

-- Add more positions to Rack R2 for capacity testing (80 occupied out of 100 = 80%)
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_rack_id, sys_user_id, last_updated)
SELECT 
    200 + (row_num - 1) * 10 + col_num,
    gen_random_uuid(),
    row_num || '-' || col_num,
    row_num,
    col_num,
    CASE WHEN ((row_num - 1) * 10 + col_num) <= 80 THEN true ELSE false END,
    31,
    1,
    CURRENT_TIMESTAMP
FROM generate_series(1, 10) AS row_num
CROSS JOIN generate_series(2, 10) AS col_num
WHERE ((row_num - 1) * 10 + col_num) <= 99;

-- Update sequences to avoid conflicts with test data
SELECT setval('storage_room_seq', 1000, false);
SELECT setval('storage_device_seq', 1000, false);
SELECT setval('storage_shelf_seq', 1000, false);
SELECT setval('storage_rack_seq', 1000, false);
SELECT setval('storage_position_seq', 10000, false);
SELECT setval('sample_storage_assignment_seq', 10000, false);
SELECT setval('sample_storage_movement_seq', 10000, false);

-- Verification queries
\echo 'Test Data Summary:'
SELECT 'Rooms' AS entity, COUNT(*) AS count FROM storage_room
UNION ALL
SELECT 'Devices', COUNT(*) FROM storage_device
UNION ALL
SELECT 'Shelves', COUNT(*) FROM storage_shelf
UNION ALL
SELECT 'Racks', COUNT(*) FROM storage_rack
UNION ALL
SELECT 'Positions', COUNT(*) FROM storage_position;

\echo ''
\echo 'Sample Hierarchy:'
SELECT 
    r.code AS room_code,
    d.code AS device_code,
    s.label AS shelf_label,
    k.label AS rack_label,
    COUNT(p.id) AS position_count,
    SUM(CASE WHEN p.occupied THEN 1 ELSE 0 END) AS occupied_count
FROM storage_room r
LEFT JOIN storage_device d ON d.parent_room_id = r.id
LEFT JOIN storage_shelf s ON s.parent_device_id = d.id
LEFT JOIN storage_rack k ON k.parent_shelf_id = s.id
LEFT JOIN storage_position p ON p.parent_rack_id = k.id
GROUP BY r.code, d.code, s.label, k.label
ORDER BY r.code, d.code, s.label, k.label;

-- ============================================================================
-- E2E Test Fixtures: Patients, Samples, and Storage Assignments
-- ============================================================================
-- These fixtures enable full end-to-end testing including:
-- - Patient search and selection in order entry workflow
-- - Sample assignment testing with existing samples
-- - Storage dashboard displays with real sample data
-- ============================================================================

-- Insert test persons (for patients)
INSERT INTO person (id, last_name, first_name, middle_name, city, state, zip_code, country, 
                    work_phone, home_phone, cell_phone, primary_phone, email, lastupdated) VALUES
(1000, 'E2E-Smith', 'John', 'Test', 'Test City', 'Test State', '12345', 'USA', 
 '555-0101', '555-0102', '555-0103', '555-0101', 'john.e2e@test.com', CURRENT_TIMESTAMP),
(1001, 'E2E-Jones', 'Jane', 'Test', 'Test City', 'Test State', '12345', 'USA',
 '555-0201', '555-0202', '555-0203', '555-0201', 'jane.e2e@test.com', CURRENT_TIMESTAMP),
(1002, 'E2E-Williams', 'Bob', 'Test', 'Test City', 'Test State', '12345', 'USA',
 '555-0301', '555-0302', '555-0303', '555-0301', 'bob.e2e@test.com', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  last_name = EXCLUDED.last_name,
  first_name = EXCLUDED.first_name,
  lastupdated = CURRENT_TIMESTAMP;

-- Insert test patients
INSERT INTO patient (id, person_id, race, gender, birth_date, birth_time, national_id, 
                     ethnicity, external_id, entered_birth_date, lastupdated) VALUES
(1000, 1000, 'black', 'M', '1990-01-15', '1990-01-15 10:00:00', 'E2E-PAT-001', 
 'U', 'E2E-PAT-001', '1990-01-15', CURRENT_TIMESTAMP),
(1001, 1001, 'white', 'F', '1985-05-20', '1985-05-20 14:30:00', 'E2E-PAT-002',
 'U', 'E2E-PAT-002', '1985-05-20', CURRENT_TIMESTAMP),
(1002, 1002, 'asian', 'M', '1992-11-10', '1992-11-10 09:15:00', 'E2E-PAT-003',
 'U', 'E2E-PAT-003', '1992-11-10', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  person_id = EXCLUDED.person_id,
  external_id = EXCLUDED.external_id,
  lastupdated = CURRENT_TIMESTAMP;

-- Get sample type IDs (using first available if specific types not found)
DO $$
DECLARE
  serum_type_id INTEGER;
  urine_type_id INTEGER;
  blood_type_id INTEGER;
  status_id_val INTEGER;
BEGIN
  -- Try to get sample types (adjust these based on your actual data)
  SELECT id INTO serum_type_id FROM type_of_sample WHERE description = 'Serum' LIMIT 1;
  SELECT id INTO urine_type_id FROM type_of_sample WHERE description = 'Urine' LIMIT 1;
  SELECT id INTO blood_type_id FROM type_of_sample WHERE description = 'Blood' LIMIT 1;
  
  -- Use defaults if not found
  IF serum_type_id IS NULL THEN
    SELECT id INTO serum_type_id FROM type_of_sample ORDER BY id LIMIT 1;
  END IF;
  IF urine_type_id IS NULL THEN
    SELECT id INTO urine_type_id FROM type_of_sample ORDER BY id LIMIT 1 OFFSET 1;
  END IF;
  IF blood_type_id IS NULL THEN
    SELECT id INTO blood_type_id FROM type_of_sample ORDER BY id LIMIT 1 OFFSET 2;
  END IF;

  -- Get status ID once
  SELECT id INTO status_id_val FROM status_of_sample WHERE name = 'Entered' LIMIT 1;
  
  IF status_id_val IS NULL THEN
    RAISE EXCEPTION 'Status "Entered" not found';
  END IF;

  -- Insert test samples with storage assignments
  -- Sample 1: Assigned to Main Lab > Freezer > Shelf-A > Rack R1 > A1
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, 
                       received_date, lastupdated, is_confirmation)
  VALUES 
  (1000, 'E2E-001', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 2: Assigned to Main Lab > Freezer > Shelf-A > Rack R1 > A2
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1001, 'E2E-002', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 3: Assigned to Main Lab > Freezer > Shelf-A > Rack R1 > A4
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1002, 'E2E-003', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 4: Not assigned (for testing assignment workflow)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1003, 'E2E-004', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 5: Assigned to Main Lab > Freezer > Shelf-A > Rack R1 > A5
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1004, 'E2E-005', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create sample_human links (samples to patients) - must be done before assignments
  INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
  VALUES
  (1000, 1000, 1000, CURRENT_TIMESTAMP),
  (1001, 1001, 1001, CURRENT_TIMESTAMP),
  (1002, 1002, 1002, CURRENT_TIMESTAMP),
  (1003, 1003, 1000, CURRENT_TIMESTAMP),
  (1004, 1004, 1001, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    patient_id = EXCLUDED.patient_id,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create storage assignments
  -- Assignment 1: Sample E2E-001 to position A1 (position_id = 100)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date, 
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1000, 1000, 100, CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 100;

  -- Assignment 2: Sample E2E-002 to position A2 (position_id = 101)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1001, 1001, 101, CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 101;

  -- Assignment 3: Sample E2E-003 to position A4 (position_id = 103)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1002, 1002, 103, CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 103;

  -- Assignment 4: Sample E2E-005 to position A5 (position_id = 104)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1003, 1004, 104, CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 104;

  -- Create storage movement audit logs
  INSERT INTO sample_storage_movement (id, sample_id, previous_position_id, new_position_id,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1000, 1000, NULL, 100, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1001, 1001, NULL, 101, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1002, 1002, NULL, 103, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1003, 1004, NULL, 104, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_position_id = EXCLUDED.new_position_id,
    last_updated = CURRENT_TIMESTAMP;

  -- ============================================================================
  -- Additional samples for filter testing (different locations and statuses)
  -- ============================================================================

  -- Sample 6: In Secondary Laboratory (active status)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1005, 'E2E-006', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 7: In Secondary Laboratory (active status)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1006, 'E2E-007', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 8: In Main Laboratory (for testing - will have NULL status = active)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1007, 'E2E-008', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;
  -- Note: Status column is only 1 character, so we can't set "disposed" here
  -- The code will default NULL status to "active" in the API response

  -- Sample 9: In Secondary Laboratory (for testing - will have NULL status = active)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1008, 'E2E-009', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;
  -- Note: Status column is only 1 character, so we can't set "disposed" here
  -- The code will default NULL status to "active" in the API response

  -- Sample 10: In Main Laboratory > Refrigerator (different device)
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1009, 'E2E-010', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create sample_human links for new samples
  INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
  VALUES
  (1005, 1005, 1000, CURRENT_TIMESTAMP),
  (1006, 1006, 1001, CURRENT_TIMESTAMP),
  (1007, 1007, 1002, CURRENT_TIMESTAMP),
  (1008, 1008, 1000, CURRENT_TIMESTAMP),
  (1009, 1009, 1001, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    patient_id = EXCLUDED.patient_id,
    lastupdated = CURRENT_TIMESTAMP;

  -- Assignments for new samples:
  -- Assignment 5: Sample E2E-006 to Secondary Lab > Cabinet > Shelf-1 > Rack R1 (position_id = 130)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1004, 1005, 130, CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 130;

  -- Assignment 6: Sample E2E-007 to Secondary Lab > Cabinet > Shelf-1 > Rack R1 (position_id = 131)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1005, 1006, 131, CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 131;

  -- Assignment 7: Sample E2E-008 (disposed) to Main Lab > Freezer > Shelf-A > Rack R1 > A6 (position_id = 105)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1006, 1007, 105, CURRENT_TIMESTAMP, 1, 'E2E test - Disposed sample', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 105;

  -- Assignment 8: Sample E2E-009 (disposed) to Secondary Lab > Cabinet > Shelf-1 > Rack R1 (position_id = 132)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1007, 1008, 132, CURRENT_TIMESTAMP, 1, 'E2E test - Disposed in Secondary', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 132;

  -- Assignment 9: Sample E2E-010 to Main Lab > Refrigerator > Shelf-1 > Rack R1 (position_id = 121)
  INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1008, 1009, 121, CURRENT_TIMESTAMP, 1, 'E2E test - Refrigerator', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    storage_position_id = EXCLUDED.storage_position_id,
    last_updated = CURRENT_TIMESTAMP;
  UPDATE storage_position SET occupied = true WHERE id = 121;

  -- Create storage movement audit logs for new samples
  INSERT INTO sample_storage_movement (id, sample_id, previous_position_id, new_position_id,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1004, 1005, NULL, 130, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1005, 1006, NULL, 131, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1006, 1007, NULL, 105, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1007, 1008, NULL, 132, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1008, 1009, NULL, 121, CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_position_id = EXCLUDED.new_position_id,
    last_updated = CURRENT_TIMESTAMP;

END $$;

-- Update sequences to avoid conflicts with test data
SELECT setval('person_seq', 2000, false);
SELECT setval('patient_seq', 2000, false);
SELECT setval('sample_seq', 2000, false);
SELECT setval('sample_human_seq', 2000, false);

\echo ''
\echo 'E2E Test Fixtures Summary:'
SELECT 'Patients' AS entity, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
UNION ALL
SELECT 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
UNION ALL
SELECT 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
UNION ALL
SELECT 'Storage Movements', COUNT(*) FROM sample_storage_movement WHERE id >= 1000;

\echo ''
\echo 'Sample Assignments:'
SELECT 
    s.accession_number,
    to_char(s.entered_date, 'YYYY-MM-DD') AS entered_date,
    p.last_name || ', ' || p.first_name AS patient_name,
    r.code || ' > ' || d.code || ' > ' || sh.label || ' > ' || k.label || ' > ' || pos.coordinate AS location_path
FROM sample s
JOIN sample_human sh_link ON s.id = sh_link.samp_id
JOIN patient pt ON sh_link.patient_id = pt.id
JOIN person p ON pt.person_id = p.id
LEFT JOIN sample_storage_assignment ssa ON s.id = ssa.sample_id
LEFT JOIN storage_position pos ON ssa.storage_position_id = pos.id
LEFT JOIN storage_rack k ON pos.parent_rack_id = k.id
LEFT JOIN storage_shelf sh ON k.parent_shelf_id = sh.id
LEFT JOIN storage_device d ON sh.parent_device_id = d.id
LEFT JOIN storage_room r ON d.parent_room_id = r.id
WHERE s.accession_number LIKE 'E2E-%'
ORDER BY s.accession_number;

\echo ''
\echo '✅ Complete test fixtures loaded successfully!'
\echo '   Storage Hierarchy:'
\echo '   - 3 Rooms (Main, Secondary, Inactive)'
\echo '   - 4 Devices (Freezer, Refrigerator, Cabinet, Inactive Freezer)'
\echo '   - 4 Shelves'
\echo '   - 4 Racks'
\echo '   - 100+ Positions (mix of occupied/unoccupied)'
\echo ''
\echo '   E2E Test Data:'
\echo '   - 3 test patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams)'
\echo '   - 5 test samples (E2E-001 through E2E-005)'
\echo '   - 4 samples with storage assignments'
\echo '   - 1 unassigned sample (E2E-004) for testing assignment workflow'
\echo ''
\echo 'Test patients can be searched by:'
\echo '   - Name: Smith, Jones, or Williams'
\echo '   - External ID: E2E-PAT-001, E2E-PAT-002, E2E-PAT-003'
\echo ''
\echo 'Test samples can be found by accession number:'
\echo '   - E2E-001, E2E-002, E2E-003, E2E-004, E2E-005'

