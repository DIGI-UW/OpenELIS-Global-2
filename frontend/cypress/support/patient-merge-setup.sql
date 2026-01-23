-- =====================================================
-- Patient Merge E2E Test Data Setup
-- Creates two patients with clinical data for merge testing
-- =====================================================

-- Clean up any existing test data first (order matters for FK constraints!)
-- 1. First delete merge audit records that reference test patients
DELETE FROM clinlims.patient_merge_audit WHERE primary_patient_id IN (
    SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%'
) OR merged_patient_id IN (
    SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%'
);

-- 2. Delete sample_human links
DELETE FROM clinlims.sample_human WHERE patient_id IN (
    SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%'
);

-- 3. Delete samples created for test patients (by accession number pattern)
DELETE FROM clinlims.sample WHERE accession_number LIKE 'MERGE-%';

-- 4. Delete patient identities
DELETE FROM clinlims.patient_identity WHERE patient_id IN (
    SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%'
);

-- 5. Delete patients
DELETE FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%';

-- 6. Delete persons (by email pattern)
DELETE FROM clinlims.person WHERE email LIKE '%@testmerge.com';

-- =====================================================
-- PATIENT 1: Alice MergeTest (will be merged INTO - primary)
-- Has samples linked for clinical data testing
-- =====================================================

-- Create Person 1 (Alice) - with specific contact info for conflict detection
-- NOTE: Alice has NO work_phone and NO fax - these will be inherited from Bob after merge
INSERT INTO clinlims.person (
    id, first_name, last_name, middle_name, city, state,
    zip_code, country, primary_phone, email, street_address,
    work_phone, fax, lastupdated
) VALUES (
    nextval('clinlims.person_seq'),
    'Alice', 'MergeTest', 'E2E',
    'Kampala', 'Central Region', '256', 'Uganda',
    '+256701234567', 'alice@testmerge.com', '123 Test Street, Kampala',
    NULL, NULL, NOW()  -- work_phone and fax are NULL - should be inherited from Bob
);

-- Create Patient 1 (Alice)
INSERT INTO clinlims.patient (
    id, person_id, race, gender, birth_date, national_id,
    external_id, entered_birth_date, lastupdated
) VALUES (
    nextval('clinlims.patient_seq'),
    currval('clinlims.person_seq'),
    'B', 'F', '1985-03-15 00:00:00', 'UG-MERGE-ALICE-001',
    'EXT-ALICE-001', '15/03/1985', NOW()
);

-- =====================================================
-- PATIENT 2: Bob MergeTarget (will be merged FROM)
-- Different demographics to test conflict detection
-- =====================================================

-- Create Person 2 (Bob) with DIFFERENT phone/email/address for conflict detection
-- NOTE: Bob has work_phone and fax that Alice doesn't have - these will be inherited by Alice
INSERT INTO clinlims.person (
    id, first_name, last_name, middle_name, city, state,
    zip_code, country, primary_phone, email, street_address,
    work_phone, fax, lastupdated
) VALUES (
    nextval('clinlims.person_seq'),
    'Bob', 'MergeTarget', 'E2E',
    'Entebbe', 'Central Region', '256', 'Uganda',
    '+256709876543', 'bob@testmerge.com', '456 Medical Road, Entebbe',
    '+256700111222', '+256700333444', NOW()  -- work_phone and fax that Alice should inherit
);

-- Create Patient 2 (Bob)
INSERT INTO clinlims.patient (
    id, person_id, race, gender, birth_date, national_id,
    external_id, entered_birth_date, lastupdated
) VALUES (
    nextval('clinlims.patient_seq'),
    currval('clinlims.person_seq'),
    'B', 'M', '1988-06-20 00:00:00', 'UG-MERGE-BOB-002',
    'EXT-BOB-002', '20/06/1988', NOW()
);

-- =====================================================
-- Create samples for both patients to test data consolidation
-- =====================================================

-- Get status IDs
DO $$
DECLARE
    alice_patient_id INTEGER;
    bob_patient_id INTEGER;
    entered_status_id INTEGER;
    alice_sample_id INTEGER;
    bob_sample_id INTEGER;
BEGIN
    -- Get patient IDs
    SELECT id INTO alice_patient_id FROM clinlims.patient WHERE national_id = 'UG-MERGE-ALICE-001';
    SELECT id INTO bob_patient_id FROM clinlims.patient WHERE national_id = 'UG-MERGE-BOB-002';

    -- Get 'Entered' status (or any available status)
    SELECT id INTO entered_status_id FROM clinlims.status_of_sample WHERE name = 'Entered' LIMIT 1;
    IF entered_status_id IS NULL THEN
        SELECT id INTO entered_status_id FROM clinlims.status_of_sample LIMIT 1;
    END IF;

    -- Create Sample 1 for Alice
    IF entered_status_id IS NOT NULL THEN
        INSERT INTO clinlims.sample (
            id, accession_number, domain, entered_date, received_date,
            collection_date, status_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_seq'),
            'MERGE-ALICE-001',
            'H',
            NOW(), NOW(), NOW(),
            entered_status_id,
            NOW()
        ) RETURNING id INTO alice_sample_id;

        -- Link Sample to Alice
        INSERT INTO clinlims.sample_human (
            id, samp_id, patient_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_human_seq'),
            alice_sample_id,
            alice_patient_id,
            NOW()
        );

        -- Create Sample 2 for Alice (older sample)
        INSERT INTO clinlims.sample (
            id, accession_number, domain, entered_date, received_date,
            collection_date, status_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_seq'),
            'MERGE-ALICE-002',
            'H',
            NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days',
            entered_status_id,
            NOW()
        ) RETURNING id INTO alice_sample_id;

        -- Link Sample 2 to Alice
        INSERT INTO clinlims.sample_human (
            id, samp_id, patient_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_human_seq'),
            alice_sample_id,
            alice_patient_id,
            NOW()
        );

        -- Create Sample for Bob
        INSERT INTO clinlims.sample (
            id, accession_number, domain, entered_date, received_date,
            collection_date, status_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_seq'),
            'MERGE-BOB-001',
            'H',
            NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days',
            entered_status_id,
            NOW()
        ) RETURNING id INTO bob_sample_id;

        -- Link Sample to Bob
        INSERT INTO clinlims.sample_human (
            id, samp_id, patient_id, lastupdated
        ) VALUES (
            nextval('clinlims.sample_human_seq'),
            bob_sample_id,
            bob_patient_id,
            NOW()
        );

        RAISE NOTICE 'Created test data: Alice (ID: %), Bob (ID: %)', alice_patient_id, bob_patient_id;
    ELSE
        RAISE NOTICE 'No status_of_sample found - skipping sample creation';
    END IF;
END $$;

-- Verify the setup
SELECT
    p.id AS patient_id,
    per.first_name,
    per.last_name,
    p.national_id,
    per.primary_phone,
    per.email,
    per.city,
    (SELECT COUNT(*) FROM clinlims.sample_human sh WHERE sh.patient_id = p.id) AS sample_count
FROM clinlims.patient p
JOIN clinlims.person per ON p.person_id = per.id
WHERE p.national_id LIKE 'UG-MERGE-%'
ORDER BY p.id;
