-- Public synthetic Location UAT records (OGC-1652); requires reporting-repeated-results.sql.
-- The REPORTING-MVP-REPEAT order came from Synthetic Location Clinic (code SYN-LOC), its
-- Synthetic Maternity Ward, in Synthetic District of Synthetic Region: the parent links an
-- installation has from its facility registry or from earlier Organization Management.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;

CREATE FUNCTION pg_temp.fixture_organization(fixture_uuid uuid, fixture_name text, parent_value numeric,
        fixture_code text, type_name text) RETURNS numeric AS $body$
DECLARE
    organization_value numeric;
    type_value numeric;
BEGIN
    SELECT id INTO STRICT type_value FROM clinlims.organization_type WHERE lower(short_name) = lower(type_name);
    SELECT id INTO organization_value FROM clinlims.organization WHERE fhir_uuid = fixture_uuid;
    IF organization_value IS NULL THEN
        organization_value := nextval('clinlims.organization_seq');
        INSERT INTO clinlims.organization (id, fhir_uuid, name, org_id, code, is_active, mls_sentinel_lab_flag,
            lastupdated)
          VALUES (organization_value, fixture_uuid, fixture_name, parent_value, fixture_code, 'Y', 'N',
            timestamp '2026-05-01 12:00:00');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM clinlims.organization_organization_type
                   WHERE org_id = organization_value AND org_type_id = type_value) THEN
        INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
          VALUES (organization_value, type_value);
    END IF;
    RETURN organization_value;
END
$body$ LANGUAGE plpgsql;

DO $fixture$
DECLARE
    sample_value numeric;
    requester_type_value numeric;
    region_value numeric;
    district_value numeric;
    clinic_value numeric;
    ward_value numeric;
    requester_value numeric;
BEGIN
    SELECT id INTO STRICT sample_value FROM sample WHERE accession_number = 'REPORTING-MVP-REPEAT';
    SELECT id INTO STRICT requester_type_value FROM requester_type WHERE lower(requester_type) = 'organization';
    region_value := pg_temp.fixture_organization('47900000-0000-4000-8000-000000000201', 'Synthetic Region',
        NULL, NULL, 'Health Region');
    district_value := pg_temp.fixture_organization('47900000-0000-4000-8000-000000000202', 'Synthetic District',
        region_value, NULL, 'Health District');
    clinic_value := pg_temp.fixture_organization('47900000-0000-4000-8000-000000000203',
        'Synthetic Location Clinic', district_value, 'SYN-LOC', 'referring clinic');
    ward_value := pg_temp.fixture_organization('47900000-0000-4000-8000-000000000204', 'Synthetic Maternity Ward',
        clinic_value, NULL, 'dept');
    FOREACH requester_value IN ARRAY ARRAY[clinic_value, ward_value] LOOP
        IF NOT EXISTS (SELECT 1 FROM sample_requester WHERE sample_id = sample_value
                       AND requester_id = requester_value AND requester_type_id = requester_type_value) THEN
            INSERT INTO sample_requester (id, sample_id, requester_id, requester_type_id, lastupdated)
              VALUES (nextval('sample_requester_seq'), sample_value, requester_value, requester_type_value,
                timestamp '2026-05-01 12:00:00');
        END IF;
    END LOOP;
END $fixture$;
COMMIT;
