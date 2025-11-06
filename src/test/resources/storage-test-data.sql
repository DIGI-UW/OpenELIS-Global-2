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
-- Each room has unique devices with descriptive names
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated) VALUES
-- Main Laboratory devices
(10, gen_random_uuid(), 'Main Lab Freezer Unit 1', 'MAIN-FRZ01', 'freezer', -80.0, 500, true, 1, 1, CURRENT_TIMESTAMP),
(11, gen_random_uuid(), 'Main Lab Refrigerator Unit 1', 'MAIN-REF01', 'refrigerator', 4.0, 300, true, 1, 1, CURRENT_TIMESTAMP),
-- Secondary Laboratory devices
(12, gen_random_uuid(), 'Secondary Lab Cabinet Unit 1', 'SEC-CAB01', 'cabinet', NULL, NULL, true, 2, 1, CURRENT_TIMESTAMP),
(14, gen_random_uuid(), 'Secondary Lab Freezer Unit 1', 'SEC-FRZ01', 'freezer', -20.0, 200, true, 2, 1, CURRENT_TIMESTAMP),
-- Inactive Room device
(13, gen_random_uuid(), 'Inactive Freezer', 'INACTIVE-FRZ', 'freezer', NULL, NULL, false, 3, 1, CURRENT_TIMESTAMP);

-- Insert Test Shelves
-- Each device has uniquely named shelves
INSERT INTO storage_shelf (id, fhir_uuid, label, capacity_limit, active, parent_device_id, sys_user_id, last_updated) VALUES
-- Main Lab Freezer Unit 1 shelves
(20, gen_random_uuid(), 'Main Freezer Shelf-A', 50, true, 10, 1, CURRENT_TIMESTAMP),
(21, gen_random_uuid(), 'Main Freezer Shelf-B', 50, true, 10, 1, CURRENT_TIMESTAMP),
-- Main Lab Refrigerator Unit 1 shelves
(22, gen_random_uuid(), 'Main Refrigerator Shelf-1', 30, true, 11, 1, CURRENT_TIMESTAMP),
(24, gen_random_uuid(), 'Main Refrigerator Shelf-2', 30, true, 11, 1, CURRENT_TIMESTAMP),
-- Secondary Lab Cabinet Unit 1 shelves
(23, gen_random_uuid(), 'Secondary Cabinet Shelf-1', 40, true, 12, 1, CURRENT_TIMESTAMP),
-- Secondary Lab Freezer Unit 1 shelves
(25, gen_random_uuid(), 'Secondary Freezer Shelf-A', 25, true, 14, 1, CURRENT_TIMESTAMP);

-- Insert Test Racks
-- Each shelf has uniquely named racks
INSERT INTO storage_rack (id, fhir_uuid, label, rows, columns, position_schema_hint, active, parent_shelf_id, sys_user_id, last_updated) VALUES
-- Main Freezer Shelf-A racks
(30, gen_random_uuid(), 'Main Freezer Shelf-A Rack 1', 8, 12, 'A1', true, 20, 1, CURRENT_TIMESTAMP),
(31, gen_random_uuid(), 'Main Freezer Shelf-A Rack 2', 10, 10, '1-1', true, 20, 1, CURRENT_TIMESTAMP),
-- Main Freezer Shelf-B racks
(32, gen_random_uuid(), 'Main Freezer Shelf-B Rack 1', 0, 0, NULL, true, 21, 1, CURRENT_TIMESTAMP),
-- Main Refrigerator Shelf-1 racks
(33, gen_random_uuid(), 'Main Refrigerator Shelf-1 Rack 1', 8, 12, NULL, true, 22, 1, CURRENT_TIMESTAMP),
-- Secondary Cabinet Shelf-1 racks
(34, gen_random_uuid(), 'Secondary Cabinet Shelf-1 Rack 1', 8, 12, 'A1', true, 23, 1, CURRENT_TIMESTAMP),
-- Secondary Freezer Shelf-A racks
(35, gen_random_uuid(), 'Secondary Freezer Shelf-A Rack 1', 6, 8, 'A1', true, 25, 1, CURRENT_TIMESTAMP);

-- Insert Test Positions
-- Each rack has unique positions
-- Note: After migration, positions require parent_device_id (required) and optionally parent_shelf_id and parent_rack_id
-- Rack 30: Main Freezer Shelf-A Rack 1 -> Shelf 20 -> Device 10
-- Rack 31: Main Freezer Shelf-A Rack 2 -> Shelf 20 -> Device 10
-- Rack 32: Main Freezer Shelf-B Rack 1 -> Shelf 21 -> Device 10
-- Rack 33: Main Refrigerator Shelf-1 Rack 1 -> Shelf 22 -> Device 11
-- Rack 34: Secondary Cabinet Shelf-1 Rack 1 -> Shelf 23 -> Device 12
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_device_id, parent_shelf_id, parent_rack_id, sys_user_id, last_updated) VALUES
-- Main Freezer Shelf-A Rack 1 (rack 30) - 8x12 grid positions
-- Shelf 20, Device 10
(100, gen_random_uuid(), 'A1', 1, 1, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(101, gen_random_uuid(), 'A2', 1, 2, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(102, gen_random_uuid(), 'A3', 1, 3, true, 10, 20, 30, 1, CURRENT_TIMESTAMP), -- Occupied for testing
(103, gen_random_uuid(), 'A4', 1, 4, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(104, gen_random_uuid(), 'A5', 1, 5, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(105, gen_random_uuid(), 'A6', 1, 6, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(106, gen_random_uuid(), 'A7', 1, 7, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),
(107, gen_random_uuid(), 'A8', 1, 8, false, 10, 20, 30, 1, CURRENT_TIMESTAMP),

-- Main Freezer Shelf-A Rack 2 (rack 31) - 10x10 grid, first position
-- Shelf 20, Device 10
(200, gen_random_uuid(), '1-1', 1, 1, false, 10, 20, 31, 1, CURRENT_TIMESTAMP),

-- Main Freezer Shelf-B Rack 1 (rack 32) - flexible positions (no grid)
-- Shelf 21, Device 10
(110, gen_random_uuid(), 'RED-01', NULL, NULL, false, 10, 21, 32, 1, CURRENT_TIMESTAMP),
(111, gen_random_uuid(), 'RED-02', NULL, NULL, false, 10, 21, 32, 1, CURRENT_TIMESTAMP),
(112, gen_random_uuid(), 'RED-03', NULL, NULL, false, 10, 21, 32, 1, CURRENT_TIMESTAMP),

-- Main Refrigerator Shelf-1 Rack 1 (rack 33) - positions
-- Shelf 22, Device 11
(120, gen_random_uuid(), 'X1', NULL, NULL, false, 11, 22, 33, 1, CURRENT_TIMESTAMP),
(121, gen_random_uuid(), 'A1', 1, 1, false, 11, 22, 33, 1, CURRENT_TIMESTAMP),

-- Secondary Cabinet Shelf-1 Rack 1 (rack 34) - positions
-- Shelf 23, Device 12
(130, gen_random_uuid(), 'A1', 1, 1, false, 12, 23, 34, 1, CURRENT_TIMESTAMP),
(131, gen_random_uuid(), 'A2', 1, 2, false, 12, 23, 34, 1, CURRENT_TIMESTAMP),
(132, gen_random_uuid(), 'A3', 1, 3, false, 12, 23, 34, 1, CURRENT_TIMESTAMP);

-- Add more positions to Main Freezer Shelf-A Rack 2 (rack 31) for capacity testing (80 occupied out of 100 = 80%)
-- Shelf 20, Device 10
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_device_id, parent_shelf_id, parent_rack_id, sys_user_id, last_updated)
SELECT 
    200 + (row_num - 1) * 10 + col_num,
    gen_random_uuid(),
    row_num || '-' || col_num,
    row_num,
    col_num,
    CASE WHEN ((row_num - 1) * 10 + col_num) <= 80 THEN true ELSE false END,
    10,  -- parent_device_id
    20,  -- parent_shelf_id
    31,  -- parent_rack_id
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
-- Note: entered_birth_date must be in MM/DD/YYYY format for OpenELIS
INSERT INTO patient (id, person_id, race, gender, birth_date, birth_time, national_id, 
                     ethnicity, external_id, entered_birth_date, lastupdated) VALUES
(1000, 1000, 'black', 'M', '1990-01-15'::timestamp, '1990-01-15 10:00:00'::timestamp, 'E2E-PAT-001', 
 'U', 'E2E-PAT-001', '01/15/1990', CURRENT_TIMESTAMP),
(1001, 1001, 'white', 'F', '1985-05-20'::timestamp, '1985-05-20 14:30:00'::timestamp, 'E2E-PAT-002',
 'U', 'E2E-PAT-002', '05/20/1985', CURRENT_TIMESTAMP),
(1002, 1002, 'asian', 'M', '1992-11-10'::timestamp, '1992-11-10 09:15:00'::timestamp, 'E2E-PAT-003',
 'U', 'E2E-PAT-003', '11/10/1992', CURRENT_TIMESTAMP)
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
  -- Sample 1: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A1
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, 
                       received_date, lastupdated, is_confirmation)
  VALUES 
  (1000, 'E2E-001', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 2: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A2
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1001, 'E2E-002', gen_random_uuid(), 'H', status_id_val,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 3: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A4
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

  -- Sample 5: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A5
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

  -- Create storage assignments (using new location_id + location_type model)
  -- Assignment 1: Sample E2E-001 to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A1
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A1')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date, 
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1000, 1000, 30, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 2: Sample E2E-002 to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A2
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A2')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1001, 1001, 30, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 3: Sample E2E-003 to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A4
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A4')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1002, 1002, 30, 'rack', 'A4', CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 4: Sample E2E-005 to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A5
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A5')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1003, 1004, 30, 'rack', 'A5', CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Create storage movement audit logs (using new flexible assignment model)
  -- These movements reference the rack locations where samples were assigned
  -- Position 100, 101, 103, 104 are all in Rack 30 (location_id = 30, location_type = 'rack')
  INSERT INTO sample_storage_movement (id, sample_id, previous_location_id, previous_location_type, previous_position_coordinate,
                                       new_location_id, new_location_type, new_position_coordinate,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1000, 1000, NULL, NULL, NULL, 30, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1001, 1001, NULL, NULL, NULL, 30, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1002, 1002, NULL, NULL, NULL, 30, 'rack', 'A4', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1003, 1004, NULL, NULL, NULL, 30, 'rack', 'A5', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_location_id = EXCLUDED.new_location_id,
    new_location_type = EXCLUDED.new_location_type,
    new_position_coordinate = EXCLUDED.new_position_coordinate,
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

  -- Assignments for new samples (using new location_id + location_type model):
  -- Assignment 5: Sample E2E-006 to Secondary Lab > Secondary Lab Cabinet Unit 1 > Secondary Cabinet Shelf-1 > Secondary Cabinet Shelf-1 Rack 1
  -- Rack 34 (location_id = 34, location_type = 'rack', position_coordinate = 'A1')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1004, 1005, 34, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 6: Sample E2E-007 to Secondary Lab > Secondary Lab Cabinet Unit 1 > Secondary Cabinet Shelf-1 > Secondary Cabinet Shelf-1 Rack 1
  -- Rack 34 (location_id = 34, location_type = 'rack', position_coordinate = 'A2')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1005, 1006, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 7: Sample E2E-008 to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A6
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A6')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1006, 1007, 30, 'rack', 'A6', CURRENT_TIMESTAMP, 1, 'E2E test - Disposed sample', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 8: Sample E2E-009 to Secondary Lab > Secondary Lab Cabinet Unit 1 > Secondary Cabinet Shelf-1 > Secondary Cabinet Shelf-1 Rack 1
  -- Rack 34 (location_id = 34, location_type = 'rack', position_coordinate = 'A3')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1007, 1008, 34, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'E2E test - Disposed in Secondary', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 9: Sample E2E-010 to Main Lab > Main Lab Refrigerator Unit 1 > Main Refrigerator Shelf-1 > Main Refrigerator Shelf-1 Rack 1
  -- Rack 33 (location_id = 33, location_type = 'rack', position_coordinate = 'A1')
  INSERT INTO sample_storage_assignment (id, sample_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1008, 1009, 33, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test - Refrigerator', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Create storage movement audit logs for new samples (using flexible assignment model)
  -- Position 130, 131, 132 are in Rack 34 (location_id = 34, location_type = 'rack')
  -- Position 105 is in Rack 31 (location_id = 31, location_type = 'rack')
  -- Position 121 is in Rack 33 (location_id = 33, location_type = 'rack')
  INSERT INTO sample_storage_movement (id, sample_id, previous_location_id, previous_location_type, previous_position_coordinate,
                                       new_location_id, new_location_type, new_position_coordinate,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1004, 1005, NULL, NULL, NULL, 34, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1005, 1006, NULL, NULL, NULL, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1006, 1007, NULL, NULL, NULL, 31, 'rack', '1-1', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1007, 1008, NULL, NULL, NULL, 34, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1008, 1009, NULL, NULL, NULL, 33, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_location_id = EXCLUDED.new_location_id,
    new_location_type = EXCLUDED.new_location_type,
    new_position_coordinate = EXCLUDED.new_position_coordinate,
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
    CASE 
      WHEN ssa.location_type = 'rack' THEN
        r.code || ' > ' || d.code || ' > ' || sh.label || ' > ' || k.label || 
        COALESCE(' > ' || ssa.position_coordinate, '')
      WHEN ssa.location_type = 'shelf' THEN
        r.code || ' > ' || d.code || ' > ' || sh.label ||
        COALESCE(' > ' || ssa.position_coordinate, '')
      WHEN ssa.location_type = 'device' THEN
        r.code || ' > ' || d.code ||
        COALESCE(' > ' || ssa.position_coordinate, '')
      ELSE 'Unassigned'
    END AS location_path
FROM sample s
JOIN sample_human sh_link ON s.id = sh_link.samp_id
JOIN patient pt ON sh_link.patient_id = pt.id
JOIN person p ON pt.person_id = p.id
LEFT JOIN sample_storage_assignment ssa ON s.id = ssa.sample_id
LEFT JOIN storage_room r ON 
  (ssa.location_type = 'device' AND EXISTS (SELECT 1 FROM storage_device WHERE id = ssa.location_id AND parent_room_id = r.id))
  OR (ssa.location_type = 'shelf' AND EXISTS (SELECT 1 FROM storage_shelf WHERE id = ssa.location_id AND parent_device_id IN (SELECT id FROM storage_device WHERE parent_room_id = r.id)))
  OR (ssa.location_type = 'rack' AND EXISTS (SELECT 1 FROM storage_rack WHERE id = ssa.location_id AND parent_shelf_id IN (SELECT id FROM storage_shelf WHERE parent_device_id IN (SELECT id FROM storage_device WHERE parent_room_id = r.id))))
LEFT JOIN storage_device d ON 
  (ssa.location_type = 'device' AND d.id = ssa.location_id)
  OR (ssa.location_type = 'shelf' AND d.id IN (SELECT parent_device_id FROM storage_shelf WHERE id = ssa.location_id))
  OR (ssa.location_type = 'rack' AND d.id IN (SELECT parent_device_id FROM storage_shelf WHERE id IN (SELECT parent_shelf_id FROM storage_rack WHERE id = ssa.location_id)))
LEFT JOIN storage_shelf sh ON 
  (ssa.location_type = 'shelf' AND sh.id = ssa.location_id)
  OR (ssa.location_type = 'rack' AND sh.id IN (SELECT parent_shelf_id FROM storage_rack WHERE id = ssa.location_id))
LEFT JOIN storage_rack k ON ssa.location_type = 'rack' AND k.id = ssa.location_id
WHERE s.accession_number LIKE 'E2E-%'
ORDER BY s.accession_number;

\echo ''
\echo '✅ Complete test fixtures loaded successfully!'
\echo '   Storage Hierarchy:'
\echo '   - 3 Rooms (Main Laboratory, Secondary Laboratory, Inactive Room)'
\echo '   - 5 Devices (Main Lab Freezer, Main Lab Refrigerator, Secondary Lab Cabinet, Secondary Lab Freezer, Inactive Freezer)'
\echo '   - 6 Shelves (each uniquely named per device)'
\echo '   - 6 Racks (each uniquely named per shelf)'
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

