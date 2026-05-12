-- =============================================================================
-- Study Entry Organizations Seed
-- =============================================================================
-- Idempotent SQL that seeds all organizations required by the React study-entry
-- Cypress E2E tests (studyInitialEntry.cy.js / studyDoubleEntry.cy.js).
--
-- Loaded at Cypress startup via the loadStudyOrganizations task in
-- cypress.config.js, which runs:
--   docker exec -i openelisglobal-database psql -U clinlims -d clinlims < this_file
--
-- What is seeded:
--   Organization types (only if absent – these live under CDIRetroCI Liquibase
--   context and may not exist in a plain CI environment):
--     - ARV Service Loc
--     - EID ACONDA-VS CI
--     - EID EGPAF
--
--   ARV organizations (type = ARV Service Loc):
--     - Centre ARV Hopital General  (short_name = '01')
--     - Centre ARV CHU Yopougon     (short_name = '02')
--     - Centre ARV Bouake           (short_name = '03')
--
--   EID organizations:
--     - Site EID ACONDA Abidjan     (short_name = 'EID01', type = EID ACONDA-VS CI)
--     - Site EID EGPAF Yopougon     (short_name = 'EID03', type = EID EGPAF)
--
-- All inserts are guarded with NOT EXISTS so this file is safe to run
-- multiple times on the same database.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- ORGANIZATION TYPES
-- ---------------------------------------------------------------------------
-- ARV Service Loc
INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key, lastupdated)
SELECT
    9002001,
    'ARV Service Loc',
    'ARV Service Location',
    'org.type.ARV',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization_type WHERE short_name = 'ARV Service Loc'
);

-- EID ACONDA-VS CI
INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key, lastupdated)
SELECT
    9002002,
    'EID ACONDA-VS CI',
    'EID ACONDA-VS CI Site',
    'org_type.ACONDA.name',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization_type WHERE short_name = 'EID ACONDA-VS CI'
);

-- EID EGPAF
INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key, lastupdated)
SELECT
    9002003,
    'EID EGPAF',
    'EID EGPAF Site',
    'org_type.EGPAF.name',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization_type WHERE short_name = 'EID EGPAF'
);

-- ---------------------------------------------------------------------------
-- ARV ORGANIZATIONS
-- ---------------------------------------------------------------------------

-- Centre ARV Hopital General  (short_name = '01')
INSERT INTO clinlims.organization (id, name, short_name, is_active, lastupdated, fhir_uuid)
SELECT
    9001001,
    'Centre ARV Hopital General',
    '01',
    'Y',
    CURRENT_TIMESTAMP,
    gen_random_uuid()
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization WHERE name = 'Centre ARV Hopital General'
);

INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM clinlims.organization o, clinlims.organization_type ot
WHERE o.name = 'Centre ARV Hopital General'
  AND ot.short_name = 'ARV Service Loc'
  AND NOT EXISTS (
      SELECT 1
      FROM clinlims.organization_organization_type oot
      JOIN clinlims.organization      org     ON oot.org_id      = org.id
      JOIN clinlims.organization_type orgtype ON oot.org_type_id = orgtype.id
      WHERE org.name          = 'Centre ARV Hopital General'
        AND orgtype.short_name = 'ARV Service Loc'
  );

-- Centre ARV CHU Yopougon  (short_name = '02')
INSERT INTO clinlims.organization (id, name, short_name, is_active, lastupdated, fhir_uuid)
SELECT
    9001002,
    'Centre ARV CHU Yopougon',
    '02',
    'Y',
    CURRENT_TIMESTAMP,
    gen_random_uuid()
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization WHERE name = 'Centre ARV CHU Yopougon'
);

INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM clinlims.organization o, clinlims.organization_type ot
WHERE o.name = 'Centre ARV CHU Yopougon'
  AND ot.short_name = 'ARV Service Loc'
  AND NOT EXISTS (
      SELECT 1
      FROM clinlims.organization_organization_type oot
      JOIN clinlims.organization      org     ON oot.org_id      = org.id
      JOIN clinlims.organization_type orgtype ON oot.org_type_id = orgtype.id
      WHERE org.name          = 'Centre ARV CHU Yopougon'
        AND orgtype.short_name = 'ARV Service Loc'
  );

-- Centre ARV Bouake  (short_name = '03')
INSERT INTO clinlims.organization (id, name, short_name, is_active, lastupdated, fhir_uuid)
SELECT
    9001003,
    'Centre ARV Bouake',
    '03',
    'Y',
    CURRENT_TIMESTAMP,
    gen_random_uuid()
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization WHERE name = 'Centre ARV Bouake'
);

INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM clinlims.organization o, clinlims.organization_type ot
WHERE o.name = 'Centre ARV Bouake'
  AND ot.short_name = 'ARV Service Loc'
  AND NOT EXISTS (
      SELECT 1
      FROM clinlims.organization_organization_type oot
      JOIN clinlims.organization      org     ON oot.org_id      = org.id
      JOIN clinlims.organization_type orgtype ON oot.org_type_id = orgtype.id
      WHERE org.name          = 'Centre ARV Bouake'
        AND orgtype.short_name = 'ARV Service Loc'
  );

-- ---------------------------------------------------------------------------
-- EID ORGANIZATIONS
-- ---------------------------------------------------------------------------

-- Site EID ACONDA Abidjan  (short_name = 'EID01', type = EID ACONDA-VS CI)
INSERT INTO clinlims.organization (id, name, short_name, is_active, lastupdated, fhir_uuid)
SELECT
    9001004,
    'Site EID ACONDA Abidjan',
    'EID01',
    'Y',
    CURRENT_TIMESTAMP,
    gen_random_uuid()
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization WHERE name = 'Site EID ACONDA Abidjan'
);

INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM clinlims.organization o, clinlims.organization_type ot
WHERE o.name = 'Site EID ACONDA Abidjan'
  AND ot.short_name = 'EID ACONDA-VS CI'
  AND NOT EXISTS (
      SELECT 1
      FROM clinlims.organization_organization_type oot
      JOIN clinlims.organization      org     ON oot.org_id      = org.id
      JOIN clinlims.organization_type orgtype ON oot.org_type_id = orgtype.id
      WHERE org.name          = 'Site EID ACONDA Abidjan'
        AND orgtype.short_name = 'EID ACONDA-VS CI'
  );

-- Site EID EGPAF Yopougon  (short_name = 'EID03', type = EID EGPAF)
INSERT INTO clinlims.organization (id, name, short_name, is_active, lastupdated, fhir_uuid)
SELECT
    9001005,
    'Site EID EGPAF Yopougon',
    'EID03',
    'Y',
    CURRENT_TIMESTAMP,
    gen_random_uuid()
WHERE NOT EXISTS (
    SELECT 1 FROM clinlims.organization WHERE name = 'Site EID EGPAF Yopougon'
);

INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
SELECT o.id, ot.id
FROM clinlims.organization o, clinlims.organization_type ot
WHERE o.name = 'Site EID EGPAF Yopougon'
  AND ot.short_name = 'EID EGPAF'
  AND NOT EXISTS (
      SELECT 1
      FROM clinlims.organization_organization_type oot
      JOIN clinlims.organization      org     ON oot.org_id      = org.id
      JOIN clinlims.organization_type orgtype ON oot.org_type_id = orgtype.id
      WHERE org.name          = 'Site EID EGPAF Yopougon'
        AND orgtype.short_name = 'EID EGPAF'
  );

-- ---------------------------------------------------------------------------
-- VERIFICATION
-- ---------------------------------------------------------------------------

DO $$
DECLARE
    arv_type_count INTEGER;
    eid_type_count INTEGER;
    arv_org_count  INTEGER;
    eid_org_count  INTEGER;
BEGIN
    SELECT COUNT(*) INTO arv_type_count
    FROM clinlims.organization_type
    WHERE short_name = 'ARV Service Loc';

    SELECT COUNT(*) INTO eid_type_count
    FROM clinlims.organization_type
    WHERE short_name IN ('EID ACONDA-VS CI', 'EID EGPAF');

    SELECT COUNT(*) INTO arv_org_count
    FROM clinlims.organization
    WHERE name IN ('Centre ARV Hopital General', 'Centre ARV CHU Yopougon', 'Centre ARV Bouake');

    SELECT COUNT(*) INTO eid_org_count
    FROM clinlims.organization
    WHERE name IN ('Site EID ACONDA Abidjan', 'Site EID EGPAF Yopougon');

    RAISE NOTICE 'Study Entry Org Seed:';
    RAISE NOTICE '  ARV org type    : % (expected 1)', arv_type_count;
    RAISE NOTICE '  EID org types   : % (expected 2)', eid_type_count;
    RAISE NOTICE '  ARV orgs        : % (expected 3)', arv_org_count;
    RAISE NOTICE '  EID orgs        : % (expected 2)', eid_org_count;
END $$;
