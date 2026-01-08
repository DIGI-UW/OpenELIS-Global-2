-- =============================================================================
-- E2E Foundational Test Data
-- =============================================================================
-- This file contains the base data required by ALL E2E tests (not just storage)
-- It mirrors what organizationManagement.cy.js and providerManagement.cy.js create
-- through the UI, but in SQL form for faster loading.
--
-- Usage: Loaded automatically by load-test-fixtures.sh BEFORE storage-test-data.sql
--
-- Data created:
--   - Provider: Optimus Prime (used in Order.json fixture)
--   - Organization: CAMES MAN (referring clinic, used in Order.json fixture)
--   - Organization: CEDRES (referral lab)
-- =============================================================================

-- =============================================================================
-- IDEMPOTENT: Do NOT delete existing foundational rows.
-- =============================================================================
-- This script is executed repeatedly by Cypress and should be safe to rerun.
-- In particular, deleting providers/persons by name is unsafe because some
-- environments may already contain those names as real seeded data referenced
-- by other tables (e.g., sample_human).
-- =============================================================================

-- =============================================================================
-- PROVIDER: Optimus Prime
-- =============================================================================
-- This is the same provider created by providerManagement.cy.js
-- Used in frontend/cypress/fixtures/Order.json as "requester"

-- Create person record for Optimus Prime
INSERT INTO person (id, first_name, last_name, lastupdated)
SELECT 9000001, 'Optimus', 'Prime', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM person WHERE first_name = 'Optimus' AND last_name = 'Prime'
);

-- Create provider record
INSERT INTO provider (id, person_id, lastupdated, fhir_uuid, active)
SELECT
  9000001,
  p.id,
  CURRENT_TIMESTAMP,
  gen_random_uuid(),
  true
FROM person p
WHERE p.first_name = 'Optimus' AND p.last_name = 'Prime'
AND NOT EXISTS (
  SELECT 1 FROM provider pr WHERE pr.person_id = p.id
);

-- =============================================================================
-- PROVIDER: Jim Jam (second provider from providerManagement.cy.js)
-- =============================================================================

INSERT INTO person (id, first_name, last_name, lastupdated)
SELECT 9000002, 'Jim', 'Jam', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM person WHERE first_name = 'Jim' AND last_name = 'Jam'
);

INSERT INTO provider (id, person_id, lastupdated, fhir_uuid, active)
SELECT
  9000002,
  p.id,
  CURRENT_TIMESTAMP,
  gen_random_uuid(),
  true
FROM person p
WHERE p.first_name = 'Jim' AND p.last_name = 'Jam'
AND NOT EXISTS (
  SELECT 1 FROM provider pr WHERE pr.person_id = p.id
);

-- =============================================================================
-- ORGANIZATION: CAMES MAN (Referring Clinic)
-- =============================================================================
-- This is the same organization created by organizationManagement.cy.js
-- Used in frontend/cypress/fixtures/Order.json as "siteName"
-- Properties: prefix=279, is_active=Y, referring_clinic=true

INSERT INTO organization (
  id,
  name,
  short_name,
  is_active,
  org_mlt_org_mlt_id,
  lastupdated,
  fhir_uuid
)
SELECT
  9000100,
  'CAMES MAN',
  '279',
  'Y',
  NULL,
  CURRENT_TIMESTAMP,
  gen_random_uuid()
FROM organization
WHERE NOT EXISTS (
  SELECT 1 FROM organization WHERE name = 'CAMES MAN'
);

-- Add organization type: referring clinic
INSERT INTO organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM organization o, organization_type ot
WHERE o.name = 'CAMES MAN'
AND ot.short_name = 'referring clinic'
AND NOT EXISTS (
  SELECT 1 FROM organization_organization_type oot
  JOIN organization org ON oot.org_id = org.id
  JOIN organization_type orgtype ON oot.org_type_id = orgtype.id
  WHERE org.name = 'CAMES MAN' AND orgtype.short_name = 'referring clinic'
);

-- =============================================================================
-- ORGANIZATION: CEDRES (Referral Lab)
-- =============================================================================
-- This is the second organization created by organizationManagement.cy.js
-- Properties: is_active=Y, referral_lab=true

INSERT INTO organization (
  id,
  name,
  is_active,
  org_mlt_org_mlt_id,
  lastupdated,
  fhir_uuid
)
SELECT
  9000101,
  'CEDRES',
  'Y',
  NULL,
  CURRENT_TIMESTAMP,
  gen_random_uuid()
FROM organization
WHERE NOT EXISTS (
  SELECT 1 FROM organization WHERE name = 'CEDRES'
);

-- Add organization type: referral lab
INSERT INTO organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM organization o, organization_type ot
WHERE o.name = 'CEDRES'
AND ot.short_name = 'referralLab'
AND NOT EXISTS (
  SELECT 1 FROM organization_organization_type oot
  JOIN organization org ON oot.org_id = org.id
  JOIN organization_type orgtype ON oot.org_type_id = orgtype.id
  WHERE org.name = 'CEDRES' AND orgtype.short_name = 'referralLab'
);

-- =============================================================================
-- VERIFICATION
-- =============================================================================

DO $$
DECLARE
  provider_count INTEGER;
  org_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO provider_count FROM provider pr
  JOIN person pe ON pr.person_id = pe.id
  WHERE pe.last_name IN ('Prime', 'Jam');

  SELECT COUNT(*) INTO org_count FROM organization
  WHERE name IN ('CAMES MAN', 'CEDRES');

  RAISE NOTICE 'E2E Foundational Data Loaded:';
  RAISE NOTICE '  - Providers: % (expected: 2)', provider_count;
  RAISE NOTICE '  - Organizations: % (expected: 2)', org_count;
END $$;
