-- Test Data for Storage Management Integration Testing
-- Run this script to populate test storage hierarchy for testing P1 user story
-- Usage: psql -U clinlims -d clinlims -f storage-test-data.sql
--
-- Fixture Data Ranges:
--   Storage: IDs 1-999 (fixtures), 1000+ (test-created)
--   Samples: E2E* and TEST-* accession numbers (no dashes in base accession numbers)
--   Patients: E2E-PAT-* external IDs
--   Sample items: IDs 10000-20000 (fixtures), 20000+ (test-created)
--   Analyses: IDs 20000-30000 (fixtures)
--   Results: IDs 30000-40000 (fixtures)

-- Dependency validation: Check required tables and data exist
DO $$
DECLARE
  type_count INTEGER;
  status_count INTEGER;
  room_count INTEGER;
BEGIN
  -- Check type_of_sample table exists and has data
  SELECT COUNT(*) INTO type_count FROM type_of_sample;
  IF type_count < 3 THEN
    RAISE EXCEPTION 'type_of_sample table has fewer than 3 rows (found: %). Required for test fixtures. Please ensure database is properly initialized.', type_count;
  END IF;

  -- Check status_of_sample table has required statuses
  -- Note: Entered may be EXTERNAL_ORDER or SAMPLE depending on database initialization
  SELECT COUNT(*) INTO status_count 
  FROM status_of_sample 
  WHERE (name = 'Entered' OR (name IN ('Not Tested', 'Finalized') AND status_type = 'ANALYSIS'));
  
  IF status_count < 3 THEN
    RAISE EXCEPTION 'status_of_sample table missing required statuses (found: % matching rows, need at least 3). Required statuses: Entered (any type), Not Tested (ANALYSIS), Finalized (ANALYSIS). Please ensure database is properly initialized.', status_count;
  END IF;

  -- Check storage hierarchy exists (from Liquibase)
  -- This script only loads E2E test data - storage hierarchy must be loaded by Liquibase first
  SELECT COUNT(*) INTO room_count FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');
  IF room_count < 3 THEN
    RAISE EXCEPTION 'Storage hierarchy not found. Expected 3 test rooms (MAIN, SEC, INACTIVE) from Liquibase. Found: %. Please ensure Liquibase has run with context="test" to load foundation data.', room_count;
  END IF;
END $$;

-- Clean up existing test data (if any)
-- Clean up E2E test data (patients, samples, sample items, assignments, analyses, results)
DELETE FROM result WHERE analysis_id IN (
  SELECT id FROM analysis WHERE sampitem_id IN (
    SELECT id FROM sample_item WHERE samp_id IN (
      SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
    )
  )
);
DELETE FROM analysis WHERE sampitem_id IN (
  SELECT id FROM sample_item WHERE samp_id IN (
    SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
  )
);
DELETE FROM sample_storage_movement WHERE sample_item_id IN (
  SELECT id FROM sample_item WHERE samp_id IN (
    SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
  )
);
DELETE FROM sample_storage_assignment WHERE sample_item_id IN (
  SELECT id FROM sample_item WHERE samp_id IN (
    SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'
  )
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
-- Note: Storage hierarchy cleanup removed - Liquibase manages foundation data
-- Only E2E test data is cleaned above (patients, samples, sample items, assignments, analyses, results)
-- 
-- Storage hierarchy (rooms, devices, shelves, racks, positions) is loaded by Liquibase
-- See: src/main/resources/liquibase/3.3.x.x/004-insert-test-storage-data.xml

-- ============================================================================
-- E2E Test Fixtures: Referring Clinics (Organizations)
-- ============================================================================
-- Add referring clinic organizations for order entry tests
-- These organizations appear in the siteName autocomplete dropdown

-- Ensure "referring clinic" organization type exists
INSERT INTO organization_type (id, short_name, description, name_display_key, lastupdated)
VALUES (100, 'referring clinic', 'Referring Clinic Organization', 'referring clinic', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  short_name = EXCLUDED.short_name,
  description = EXCLUDED.description,
  name_display_key = EXCLUDED.name_display_key,
  lastupdated = CURRENT_TIMESTAMP;

-- Add CAMES MAN organization (used in dashboard E2E tests)
-- Set short_name to NULL so it displays as just "CAMES MAN" (not " - CAMES MAN")
INSERT INTO organization (id, name, short_name, is_active, lastupdated)
VALUES (1000, 'CAMES MAN', NULL, 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  short_name = NULL,
  is_active = EXCLUDED.is_active,
  lastupdated = CURRENT_TIMESTAMP;

-- Link CAMES MAN to referring clinic type
INSERT INTO organization_organization_type (org_id, org_type_id)
VALUES (1000, 100)
ON CONFLICT (org_id, org_type_id) DO NOTHING;

-- Add Optimus provider (used for requester in dashboard E2E tests)
-- Providers are loaded from the practitioner_persons list which requires:
-- 1. A person record
-- 2. A provider record linked to that person
-- Format: "LastName, FirstName"

-- Add Optimus person
-- Note: person table doesn't have is_active field, that's only on provider
INSERT INTO person (id, last_name, first_name, lastupdated)
VALUES (2000, 'Optimus', 'Prime', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  last_name = EXCLUDED.last_name,
  first_name = EXCLUDED.first_name,
  lastupdated = CURRENT_TIMESTAMP;

-- Add Optimus provider (linked to person)
-- Note: active is a boolean field, not is_active
INSERT INTO provider (id, person_id, provider_type, active, lastupdated)
VALUES (2000, 2000, 'P', true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
  person_id = EXCLUDED.person_id,
  provider_type = EXCLUDED.provider_type,
  active = EXCLUDED.active,
  lastupdated = CURRENT_TIMESTAMP;

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
(1000, 'TEST-Smith', 'John', 'Test', 'Test City', 'Test State', '12345', 'USA', 
 '555-0101', '555-0102', '555-0103', '555-0101', 'john.test@test.com', CURRENT_TIMESTAMP),
(1001, 'TEST-Jones', 'Jane', 'Test', 'Test City', 'Test State', '12345', 'USA',
 '555-0201', '555-0202', '555-0203', '555-0201', 'jane.test@test.com', CURRENT_TIMESTAMP),
(1002, 'TEST-Williams', 'Bob', 'Test', 'Test City', 'Test State', '12345', 'USA',
 '555-0301', '555-0302', '555-0303', '555-0301', 'bob.test@test.com', CURRENT_TIMESTAMP)
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
  birth_date = EXCLUDED.birth_date,
  birth_time = EXCLUDED.birth_time,
  entered_birth_date = EXCLUDED.entered_birth_date,
  lastupdated = CURRENT_TIMESTAMP;

-- Get sample type IDs (using first available if specific types not found)
DO $$
DECLARE
  serum_type_id INTEGER;
  urine_type_id INTEGER;
  blood_type_id INTEGER;
  status_id_val INTEGER;
  -- Variables for analysis and result creation
  test_id_val INTEGER;
  test_section_id_val INTEGER;
  not_started_status_id INTEGER;
  finalized_status_id INTEGER;
  tech_accept_status_id INTEGER;
  canceled_status_id INTEGER;
  test_result_id_val INTEGER;
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

  -- Get status ID once (validated in dependency check above)
  -- Try SAMPLE type first, then EXTERNAL_ORDER (database may have either)
  SELECT id INTO status_id_val FROM status_of_sample WHERE name = 'Entered' AND status_type = 'SAMPLE' LIMIT 1;
  
  -- Fallback to EXTERNAL_ORDER if SAMPLE not found
  IF status_id_val IS NULL THEN
    SELECT id INTO status_id_val FROM status_of_sample WHERE name = 'Entered' AND status_type = 'EXTERNAL_ORDER' LIMIT 1;
  END IF;
  
  -- Final fallback: try SampleEntered
  IF status_id_val IS NULL THEN
    SELECT id INTO status_id_val FROM status_of_sample WHERE name = 'SampleEntered' AND status_type = 'SAMPLE' LIMIT 1;
  END IF;
  
  IF status_id_val IS NULL THEN
    RAISE EXCEPTION 'Status "Entered" (SAMPLE or EXTERNAL_ORDER type) or "SampleEntered" (SAMPLE type) not found. This should have been caught by dependency validation.';
  END IF;

  -- Insert test samples with storage assignments
  -- Sample 1: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A1
  -- Use fixed dates matching test search criteria: collection_date = 30/04/2025, received_date = 01/05/2025
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, 
                       received_date, lastupdated, is_confirmation)
  VALUES 
  (1000, 'E2E001', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 1 SampleItems: Multiple items for this sample (blood tube + serum aliquot)
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10001, 1000, 1, NULL, 'E2E001-TUBE-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-001', 5.0, status_id_val, CURRENT_TIMESTAMP),
  (10002, 1000, 2, 10001, 'E2E001-ALIQUOT-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-001', 2.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 2: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A2
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1001, 'E2E002', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 2 SampleItems: Single blood tube
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10011, 1001, 1, NULL, 'E2E002-TUBE-1', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-002', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 3: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A4
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1002, 'E2E003', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 3 SampleItems: Urine sample with multiple aliquots
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10021, 1002, 1, NULL, 'E2E003-URINE-1', urine_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-003', 50.0, status_id_val, CURRENT_TIMESTAMP),
  (10022, 1002, 2, 10021, 'E2E003-ALIQUOT-1', urine_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-003', 10.0, status_id_val, CURRENT_TIMESTAMP),
  (10023, 1002, 3, 10021, 'E2E003-ALIQUOT-2', urine_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-003', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 4: Not assigned (for testing assignment workflow)
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1003, 'E2E004', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 4 SampleItems: Single item, not yet assigned
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10031, 1003, 1, NULL, 'E2E004-TUBE-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-004', 5.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 5: Assigned to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A5
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1004, 'E2E005', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 5 SampleItems: Blood sample with multiple tubes
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10041, 1004, 1, NULL, 'E2E005-TUBE-1', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-005', 10.0, status_id_val, CURRENT_TIMESTAMP),
  (10042, 1004, 2, 10041, 'E2E005-TUBE-2', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-005', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 6: 3-character accession number "E2E" for Result By Order search tests
  -- The backend query requires exact length match, so "E2E" (3 chars) is needed
  -- Use status_id = 1 (Test Entered, ORDER type) to match OrderStatus.Entered
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1005, 'E2E', gen_random_uuid(), 'H', 1,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = 'E2E',
    status_id = 1,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 6 SampleItems: Single serum sample
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10051, 1005, 1, NULL, 'E2E-TUBE-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-006', 5.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    samp_id = EXCLUDED.samp_id,
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create sample_human links (samples to patients) - must be done before assignments
  INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
  VALUES
  (1000, 1000, 1000, CURRENT_TIMESTAMP),
  (1001, 1001, 1001, CURRENT_TIMESTAMP),
  (1002, 1002, 1002, CURRENT_TIMESTAMP),
  (1003, 1003, 1000, CURRENT_TIMESTAMP),
  (1004, 1004, 1001, CURRENT_TIMESTAMP),
  (1005, 1005, 1000, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    patient_id = EXCLUDED.patient_id,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create storage assignments (using new location_id + location_type model, SampleItem-level tracking)
  -- Assignment 1: SampleItem 10001 (from Sample E2E001) to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A1
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A1')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date, 
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1000, 10001, 30, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test assignment - blood tube', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 1b: SampleItem 10002 (from Sample E2E001) to different location - serum aliquot in refrigerator
  -- Rack 33 (Main Refrigerator Shelf-1 Rack 1)
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1001, 10002, 33, 'rack', 'X1', CURRENT_TIMESTAMP, 1, 'E2E test assignment - serum aliquot', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 2: SampleItem 10011 (from Sample E2E002) to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A2
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A2')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1002, 10011, 30, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'E2E test assignment', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 3: SampleItem 10021 (from Sample E2E003) to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A4
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A4')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1003, 10021, 30, 'rack', 'A4', CURRENT_TIMESTAMP, 1, 'E2E test assignment - urine sample', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 3b: SampleItem 10022 (from Sample E2E003) to same rack, different position
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1004, 10022, 30, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'E2E test assignment - urine aliquot 1', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 4: SampleItem 10041 (from Sample E2E005) to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A5
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A5')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1005, 10041, 30, 'rack', 'A5', CURRENT_TIMESTAMP, 1, 'E2E test assignment - blood tube 1', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 4b: SampleItem 10042 (from Sample E2E005) to same rack, different position
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1006, 10042, 30, 'rack', 'A7', CURRENT_TIMESTAMP, 1, 'E2E test assignment - blood tube 2', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Create storage movement audit logs (using new flexible assignment model, SampleItem-level tracking)
  -- These movements reference the rack locations where SampleItems were assigned
  INSERT INTO sample_storage_movement (id, sample_item_id, previous_location_id, previous_location_type, previous_position_coordinate,
                                       new_location_id, new_location_type, new_position_coordinate,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1000, 10001, NULL, NULL, NULL, 30, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment - blood tube', CURRENT_TIMESTAMP),
  (1001, 10002, NULL, NULL, NULL, 33, 'rack', 'X1', CURRENT_TIMESTAMP, 1, 'Initial assignment - serum aliquot', CURRENT_TIMESTAMP),
  (1002, 10011, NULL, NULL, NULL, 30, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1003, 10021, NULL, NULL, NULL, 30, 'rack', 'A4', CURRENT_TIMESTAMP, 1, 'Initial assignment - urine sample', CURRENT_TIMESTAMP),
  (1004, 10022, NULL, NULL, NULL, 30, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'Initial assignment - urine aliquot 1', CURRENT_TIMESTAMP),
  (1005, 10041, NULL, NULL, NULL, 30, 'rack', 'A5', CURRENT_TIMESTAMP, 1, 'Initial assignment - blood tube 1', CURRENT_TIMESTAMP),
  (1006, 10042, NULL, NULL, NULL, 30, 'rack', 'A7', CURRENT_TIMESTAMP, 1, 'Initial assignment - blood tube 2', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_location_id = EXCLUDED.new_location_id,
    new_location_type = EXCLUDED.new_location_type,
    new_position_coordinate = EXCLUDED.new_position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- ============================================================================
  -- Additional samples for filter testing (different locations and statuses)
  -- ============================================================================

  -- Sample 6: In Secondary Laboratory (active status)
  -- NOTE: Sample 1005 is already defined above as 'E2E' for Result By Order tests
  -- This sample should use a different ID or be removed if not needed
  -- For now, we'll skip this duplicate definition
  -- INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
  --                      received_date, lastupdated, is_confirmation)
  -- VALUES
  -- (1005, 'E2E006', gen_random_uuid(), 'H', status_id_val,
  --  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
  -- ON CONFLICT (id) DO UPDATE SET
  --   accession_number = EXCLUDED.accession_number,
  --   lastupdated = CURRENT_TIMESTAMP;

  -- Sample 6 SampleItems: Multiple serum aliquots
  -- NOTE: Sample items for sample 1005 commented out since sample 1005 is 'E2E' (not 'E2E006')
  -- INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
  --                          collection_date, collector, quantity, status_id, lastupdated)
  -- VALUES
  -- (10051, 1005, 1, NULL, 'E2E006-ALIQUOT-1', serum_type_id, CURRENT_TIMESTAMP, 'Tech-006', 2.0, status_id_val, CURRENT_TIMESTAMP),
  -- (10052, 1005, 2, 10051, 'E2E006-ALIQUOT-2', serum_type_id, CURRENT_TIMESTAMP, 'Tech-006', 2.0, status_id_val, CURRENT_TIMESTAMP),
  -- (10053, 1005, 3, 10051, 'E2E006-ALIQUOT-3', serum_type_id, CURRENT_TIMESTAMP, 'Tech-006', 2.0, status_id_val, CURRENT_TIMESTAMP)
  -- ON CONFLICT (id) DO UPDATE SET
  --   external_id = EXCLUDED.external_id,
  --   lastupdated = CURRENT_TIMESTAMP;

  -- Sample 7: In Secondary Laboratory (active status)
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1006, 'E2E007', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 7 SampleItems: Single blood tube
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10061, 1006, 1, NULL, 'E2E007-TUBE-1', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-007', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 8: In Main Laboratory (for testing - will have NULL status = active)
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1007, 'E2E008', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 8 SampleItems: Urine sample with multiple aliquots
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10071, 1007, 1, NULL, 'E2E008-URINE-1', urine_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-008', 50.0, status_id_val, CURRENT_TIMESTAMP),
  (10072, 1007, 2, 10071, 'E2E008-ALIQUOT-1', urine_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-008', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 9: In Secondary Laboratory (for testing - will have NULL status = active)
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1008, 'E2E009', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 9 SampleItems: Serum sample with multiple aliquots
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10081, 1008, 1, NULL, 'E2E009-SERUM-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-009', 5.0, status_id_val, CURRENT_TIMESTAMP),
  (10082, 1008, 2, 10081, 'E2E009-ALIQUOT-1', serum_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-009', 2.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 10: In Main Laboratory > Refrigerator (different device)
  -- Use fixed dates matching test search criteria
  INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date,
                       received_date, lastupdated, is_confirmation)
  VALUES
  (1009, 'E2E010', gen_random_uuid(), 'H', status_id_val,
   '2025-04-30 10:00:00'::timestamp, '2025-05-01 10:00:00'::timestamp, CURRENT_TIMESTAMP, false)
  ON CONFLICT (id) DO UPDATE SET
    accession_number = EXCLUDED.accession_number,
    entered_date = EXCLUDED.entered_date,
    received_date = EXCLUDED.received_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Sample 10 SampleItems: Blood sample with multiple tubes
  -- Use fixed collection_date = 30/04/2025 to match test search criteria
  INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, 
                           collection_date, collector, quantity, status_id, lastupdated)
  VALUES
  (10091, 1009, 1, NULL, 'E2E010-TUBE-1', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-010', 10.0, status_id_val, CURRENT_TIMESTAMP),
  (10092, 1009, 2, 10091, 'E2E010-TUBE-2', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-010', 10.0, status_id_val, CURRENT_TIMESTAMP),
  (10093, 1009, 3, 10091, 'E2E010-TUBE-3', blood_type_id, '2025-04-30 10:00:00'::timestamp, 'Tech-010', 10.0, status_id_val, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    external_id = EXCLUDED.external_id,
    collection_date = EXCLUDED.collection_date,
    lastupdated = CURRENT_TIMESTAMP;

  -- Create sample_human links for new samples
  -- NOTE: Sample 1005 link already created above (sample 1005 is 'E2E', not 'E2E006')
  INSERT INTO sample_human (id, samp_id, patient_id, lastupdated)
  VALUES
  -- (1005, 1005, 1000, CURRENT_TIMESTAMP), -- Already linked above
  (1006, 1006, 1001, CURRENT_TIMESTAMP),
  (1007, 1007, 1002, CURRENT_TIMESTAMP),
  (1008, 1008, 1000, CURRENT_TIMESTAMP),
  (1009, 1009, 1001, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    patient_id = EXCLUDED.patient_id,
    lastupdated = CURRENT_TIMESTAMP;

  -- Assignments for new samples (using new location_id + location_type model, SampleItem-level tracking):
  -- NOTE: Assignments for sample items 10051, 10052, 10053 commented out since sample 1005 is 'E2E' (not 'E2E006')
  -- Assignment 5: SampleItem 10051 (from Sample E2E006) - COMMENTED OUT
  -- INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
  --                                        assigned_by_user_id, notes, last_updated)
  -- VALUES (1007, 10051, 34, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab - aliquot 1', CURRENT_TIMESTAMP)
  -- ON CONFLICT (id) DO UPDATE SET
  --   location_id = EXCLUDED.location_id,
  --   location_type = EXCLUDED.location_type,
  --   position_coordinate = EXCLUDED.position_coordinate,
  --   last_updated = CURRENT_TIMESTAMP;

  -- Assignment 5b: SampleItem 10052 - COMMENTED OUT
  -- INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
  --                                        assigned_by_user_id, notes, last_updated)
  -- VALUES (1008, 10052, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab - aliquot 2', CURRENT_TIMESTAMP)
  -- ON CONFLICT (id) DO UPDATE SET
  --   location_id = EXCLUDED.location_id,
  --   location_type = EXCLUDED.location_type,
  --   position_coordinate = EXCLUDED.position_coordinate,
  --   last_updated = CURRENT_TIMESTAMP;

  -- Assignment 6: SampleItem 10061 (from Sample E2E007) to Secondary Lab > Secondary Lab Cabinet Unit 1 > Secondary Cabinet Shelf-1 > Secondary Cabinet Shelf-1 Rack 1
  -- Rack 34 (location_id = 34, location_type = 'rack', position_coordinate = 'A2')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1009, 10061, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'E2E test - Secondary Lab', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 7: SampleItem 10071 (from Sample E2E008) to Main Lab > Main Lab Freezer Unit 1 > Main Freezer Shelf-A > Main Freezer Shelf-A Rack 1 > A6
  -- Rack 30 (location_id = 30, location_type = 'rack', position_coordinate = 'A6')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1010, 10071, 30, 'rack', 'A6', CURRENT_TIMESTAMP, 1, 'E2E test - Disposed sample', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 8: SampleItem 10081 (from Sample E2E009) to Secondary Lab > Secondary Lab Cabinet Unit 1 > Secondary Cabinet Shelf-1 > Secondary Cabinet Shelf-1 Rack 1
  -- Rack 34 (location_id = 34, location_type = 'rack', position_coordinate = 'A3')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1011, 10081, 34, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'E2E test - Disposed in Secondary', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 9: SampleItem 10091 (from Sample E2E010) to Main Lab > Main Lab Refrigerator Unit 1 > Main Refrigerator Shelf-1 > Main Refrigerator Shelf-1 Rack 1
  -- Rack 33 (location_id = 33, location_type = 'rack', position_coordinate = 'A1')
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1012, 10091, 33, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'E2E test - Refrigerator - tube 1', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Assignment 9b: SampleItem 10092 to same rack, different position
  INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date,
                                         assigned_by_user_id, notes, last_updated)
  VALUES (1013, 10092, 33, 'rack', 'X1', CURRENT_TIMESTAMP, 1, 'E2E test - Refrigerator - tube 2', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    location_type = EXCLUDED.location_type,
    position_coordinate = EXCLUDED.position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- Create storage movement audit logs for new samples (using flexible assignment model, SampleItem-level tracking)
  -- NOTE: Movement for sample_item 10052 commented out since that sample_item is not inserted (sample 1005 uses 'E2E' not 'E2E006')
  INSERT INTO sample_storage_movement (id, sample_item_id, previous_location_id, previous_location_type, previous_position_coordinate,
                                       new_location_id, new_location_type, new_position_coordinate,
                                       movement_date, moved_by_user_id, reason, last_updated)
  VALUES
  (1007, 10051, NULL, NULL, NULL, 34, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment - aliquot 1', CURRENT_TIMESTAMP),
  -- (1008, 10052, NULL, NULL, NULL, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'Initial assignment - aliquot 2', CURRENT_TIMESTAMP), -- Commented: sample_item 10052 not inserted
  (1009, 10061, NULL, NULL, NULL, 34, 'rack', 'A2', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1010, 10071, NULL, NULL, NULL, 30, 'rack', 'A6', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1011, 10081, NULL, NULL, NULL, 34, 'rack', 'A3', CURRENT_TIMESTAMP, 1, 'Initial assignment', CURRENT_TIMESTAMP),
  (1012, 10091, NULL, NULL, NULL, 33, 'rack', 'A1', CURRENT_TIMESTAMP, 1, 'Initial assignment - tube 1', CURRENT_TIMESTAMP),
  (1013, 10092, NULL, NULL, NULL, 33, 'rack', 'X1', CURRENT_TIMESTAMP, 1, 'Initial assignment - tube 2', CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO UPDATE SET
    new_location_id = EXCLUDED.new_location_id,
    new_location_type = EXCLUDED.new_location_type,
    new_position_coordinate = EXCLUDED.new_position_coordinate,
    last_updated = CURRENT_TIMESTAMP;

  -- ============================================================================
  -- Analysis and Result Test Fixtures
  -- ============================================================================
  -- Create analysis records (test orders) for E2E sample items
  -- Create result records (test results) for finalized analyses
  -- ============================================================================

  -- Get test and test section IDs (dynamic lookup)
  SELECT id INTO test_id_val FROM test WHERE is_active = 'Y' ORDER BY id LIMIT 1;
  
  -- Get first available active test section (optional, can be NULL)
  SELECT id INTO test_section_id_val FROM test_section WHERE is_active = 'Y' ORDER BY id LIMIT 1;
  
  -- Get analysis status IDs from status_of_sample table
  SELECT id INTO not_started_status_id FROM status_of_sample WHERE name = 'Not Tested' AND status_type = 'ANALYSIS' LIMIT 1;
  SELECT id INTO finalized_status_id FROM status_of_sample WHERE name = 'Finalized' AND status_type = 'ANALYSIS' LIMIT 1;
  SELECT id INTO tech_accept_status_id FROM status_of_sample WHERE name = 'Technical Acceptance' AND status_type = 'ANALYSIS' LIMIT 1;
  SELECT id INTO canceled_status_id FROM status_of_sample WHERE name = 'Test Canceled' AND status_type = 'ANALYSIS' LIMIT 1;
  
  -- Get first available test_result for dictionary type (optional, can be NULL)
  SELECT id INTO test_result_id_val FROM test_result WHERE tst_rslt_type = 'D' AND is_active = true LIMIT 1;
  
  -- Only create analyses if we have a test
  IF test_id_val IS NOT NULL THEN
    -- Create analyses for E2E sample items
    -- E2E001 SampleItem 10001: Finalized analysis (with result)
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20001, 10001, test_id_val, test_section_id_val, finalized_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '2 days',
     CURRENT_TIMESTAMP - INTERVAL '1 day', 'Y', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- E2E001 SampleItem 10001: Not started analysis (no result)
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20002, 10001, test_id_val, test_section_id_val, not_started_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- E2E SampleItem 10051: Not started analysis (for Result By Order search - unfinished status)
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20010, 10051, test_id_val, test_section_id_val, not_started_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, NULL, NULL, 'Y', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- E2E002 SampleItem 10011: Technical acceptance analysis
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20003, 10011, test_id_val, test_section_id_val, tech_accept_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day',
     CURRENT_TIMESTAMP - INTERVAL '12 hours', 'Y', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- E2E003 SampleItem 10021: Canceled analysis
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20004, 10021, test_id_val, test_section_id_val, canceled_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '3 days',
     NULL, 'N', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- E2E005 SampleItem 10041: Finalized analysis (with result)
    INSERT INTO analysis (id, sampitem_id, test_id, test_sect_id, status_id, status, 
                          analysis_type, entry_date, started_date, completed_date, 
                          is_reportable, lastupdated)
    VALUES
    (20005, 10041, test_id_val, test_section_id_val, finalized_status_id, '1',
     'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day',
     CURRENT_TIMESTAMP - INTERVAL '6 hours', 'Y', CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      status_id = EXCLUDED.status_id,
      lastupdated = CURRENT_TIMESTAMP;
    
    -- Create results for finalized analyses only
    -- Result for analysis 20001 (finalized) - Dictionary type
    IF test_result_id_val IS NOT NULL THEN
      INSERT INTO result (id, analysis_id, test_result_id, value, result_type, 
                          is_reportable, sort_order, lastupdated)
      VALUES
      (30001, 20001, test_result_id_val, 'Positive', 'D', 'Y', 1, CURRENT_TIMESTAMP)
      ON CONFLICT (id) DO UPDATE SET
        value = EXCLUDED.value,
        lastupdated = CURRENT_TIMESTAMP;
    ELSE
      -- Fallback: Create text result if no dictionary test_result available
      INSERT INTO result (id, analysis_id, value, result_type, 
                          is_reportable, sort_order, lastupdated)
      VALUES
      (30001, 20001, 'Positive', 'T', 'Y', 1, CURRENT_TIMESTAMP)
      ON CONFLICT (id) DO UPDATE SET
        value = EXCLUDED.value,
        lastupdated = CURRENT_TIMESTAMP;
    END IF;
    
    -- Result for analysis 20005 (finalized) - Numeric type
    INSERT INTO result (id, analysis_id, value, result_type, 
                        is_reportable, sort_order, lastupdated)
    VALUES
    (30002, 20005, '125.5', 'N', 'Y', 1, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
      value = EXCLUDED.value,
      lastupdated = CURRENT_TIMESTAMP;
  END IF;

END $$;

-- Update sequences to avoid conflicts with test data
SELECT setval('person_seq', 2000, false);
SELECT setval('patient_seq', 2000, false);
SELECT setval('sample_seq', 2000, false);
SELECT setval('sample_human_seq', 2000, false);
SELECT setval('sample_item_seq', 10100, false);
SELECT setval('analysis_seq', 20000, false);
SELECT setval('result_seq', 30000, false);

\echo ''
\echo 'E2E Test Fixtures Summary:'
SELECT 'Patients' AS entity, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
UNION ALL
SELECT 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
UNION ALL
SELECT 'SampleItems', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000
UNION ALL
SELECT 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
UNION ALL
SELECT 'Storage Movements', COUNT(*) FROM sample_storage_movement WHERE id >= 1000
UNION ALL
SELECT 'Analyses', COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000
UNION ALL
SELECT 'Results', COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;

\echo ''
\echo 'Sample Assignments:'
SELECT 
    s.accession_number,
    si.id AS sample_item_id,
    si.external_id AS sample_item_external_id,
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
LEFT JOIN sample_item si ON s.id = si.samp_id
LEFT JOIN sample_storage_assignment ssa ON si.id = ssa.sample_item_id
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
ORDER BY s.accession_number, si.sort_order;

-- Verification queries: Verify all fixtures loaded correctly
\echo ''
\echo '========================================'
\echo 'Verification Summary'
\echo '========================================'
\echo ''

-- Storage Hierarchy Verification (from Liquibase)
\echo 'Storage Hierarchy (loaded by Liquibase):'
SELECT 
    'Rooms' AS type, 
    COUNT(*) AS count,
    CASE WHEN COUNT(*) >= 3 THEN '✅' ELSE '❌' END AS status
FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')
UNION ALL
SELECT 
    'Devices', 
    COUNT(*),
    CASE WHEN COUNT(*) >= 5 THEN '✅' ELSE '❌' END
FROM storage_device WHERE id BETWEEN 10 AND 20
UNION ALL
SELECT 
    'Shelves', 
    COUNT(*),
    CASE WHEN COUNT(*) >= 6 THEN '✅' ELSE '❌' END
FROM storage_shelf WHERE id BETWEEN 20 AND 30
UNION ALL
SELECT 
    'Racks', 
    COUNT(*),
    CASE WHEN COUNT(*) >= 6 THEN '✅' ELSE '❌' END
FROM storage_rack WHERE id BETWEEN 30 AND 40
UNION ALL
SELECT 
    'Positions', 
    COUNT(*),
    CASE WHEN COUNT(*) >= 99 THEN '✅' ELSE '❌' END
FROM storage_position WHERE id BETWEEN 100 AND 10000;

\echo ''
\echo 'E2E Test Data:'
SELECT 
    'Patients' AS type,
    COUNT(*) AS count,
    CASE WHEN COUNT(*) >= 3 THEN '✅' ELSE '❌' END AS status
FROM patient WHERE external_id LIKE 'E2E-%'
UNION ALL
SELECT 
    'Samples',
    COUNT(*),
    CASE WHEN COUNT(*) >= 10 THEN '✅' ELSE '❌' END
FROM sample WHERE accession_number LIKE 'E2E-%'
UNION ALL
SELECT 
    'Sample Items',
    COUNT(*),
    CASE WHEN COUNT(*) >= 20 THEN '✅' ELSE '❌' END
FROM sample_item WHERE id BETWEEN 10000 AND 20000
UNION ALL
SELECT 
    'Storage Assignments',
    COUNT(*),
    CASE WHEN COUNT(*) >= 15 THEN '✅' ELSE '❌' END
FROM sample_storage_assignment WHERE id >= 1000
UNION ALL
SELECT 
    'Analyses',
    COUNT(*),
    CASE WHEN COUNT(*) >= 5 THEN '✅' ELSE '❌' END
FROM analysis WHERE id BETWEEN 20000 AND 30000
UNION ALL
SELECT 
    'Results',
    COUNT(*),
    CASE WHEN COUNT(*) >= 2 THEN '✅' ELSE '❌' END
FROM result WHERE id BETWEEN 30000 AND 40000;

\echo ''
\echo '========================================'
\echo '✅ Complete test fixtures loaded successfully!'
\echo '========================================'
\echo ''
\echo 'Storage Hierarchy (loaded by Liquibase):'
\echo '   - 3 Rooms (Main Laboratory, Secondary Laboratory, Inactive Room)'
\echo '   - 5 Devices (Main Lab Freezer, Main Lab Refrigerator, Secondary Lab Cabinet, Secondary Lab Freezer, Inactive Freezer)'
\echo '   - 6 Shelves (each uniquely named per device)'
\echo '   - 6 Racks (each uniquely named per shelf)'
\echo '   - 100+ Positions (mix of occupied/unoccupied)'
\echo ''
\echo 'E2E Test Data:'
\echo '   - 3 test patients (John TEST-Smith, Jane TEST-Jones, Bob TEST-Williams)'
\echo '   - 9 test samples (E2E001, E2E002, E2E003, E2E004, E2E005, E2E, E2E007, E2E008, E2E009, E2E010)'
\echo '   - 20+ test SampleItems (multiple items per sample, various types)'
\echo '   - 15+ SampleItems with storage assignments'
\echo '   - 1 unassigned SampleItem (ID: 10031 from E2E004) for testing assignment workflow'
\echo '   - 5 test analyses (orders) for E2E sample items'
\echo '   - 2 test results for finalized analyses'
\echo ''
\echo 'Test patients can be searched by:'
\echo '   - Name: Smith, Jones, or Williams'
\echo '   - External ID: E2E-PAT-001, E2E-PAT-002, E2E-PAT-003'
\echo ''
\echo 'Test samples can be found by accession number:'
\echo '   - E2E001, E2E002, E2E003, E2E004, E2E005, E2E, E2E007, E2E008, E2E009, E2E010'
\echo ''
\echo 'Test SampleItems can be found by:'
\echo '   - SampleItem ID: 10001, 10002, 10011, etc. (numeric IDs)'
\echo '   - External ID: E2E001-TUBE-1, E2E001-ALIQUOT-1, E2E002-TUBE-1, etc.'
\echo '   - Parent Sample accession: E2E001, E2E002, etc.'
\echo ''
\echo 'SampleItem Variety:'
\echo '   - Blood samples: tubes (E2E002, E2E005, E2E007, E2E010)'
\echo '   - Serum samples: tubes and aliquots (E2E001, E2E009)'
\echo '   - Urine samples: containers and aliquots (E2E003, E2E008)'
\echo '   - Multiple items per sample: E2E001 (2 items), E2E003 (3 items), E2E005 (2 items), etc.'

