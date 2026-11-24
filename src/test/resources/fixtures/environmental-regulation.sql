-- Public synthetic environmental sample type and sampling site for the regulation-driven order
-- spec (OGC-768).
-- "Synthetic Drinking Water" (domain E) gives environmental order entry a sample type; the spec
-- imports its own tests on it and creates the standards through the admin API, so no shared
-- catalog test gains a sample type.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;

INSERT INTO clinlims.localization (id, description, lastupdated)
SELECT 99768, 'Synthetic Drinking Water', timestamp '2026-05-01 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM clinlims.localization WHERE id = 99768);

INSERT INTO clinlims.localization_value (id, localization_id, locale, value, origin)
SELECT nextval('clinlims.localization_value_seq'), 99768, 'en', 'Synthetic Drinking Water', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM clinlims.localization_value WHERE localization_id = 99768 AND locale = 'en');

INSERT INTO clinlims.type_of_sample (id, description, domain, lastupdated, local_abbrev, is_active, sort_order,
    name_localization_id, microbiology_isolate)
SELECT 99768, 'Synthetic Drinking Water', 'E', timestamp '2026-05-01 12:00:00', 'SYNDW', 'Y', 9999, 99768, false
WHERE NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE id = 99768);

INSERT INTO clinlims.vector_sampling_site (id, code, name, type, active, lastupdated)
SELECT 99768, 'SYN-RIVER', 'Synthetic Riverside Intake', 'WATER', true, timestamp '2026-05-01 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM clinlims.vector_sampling_site WHERE code = 'SYN-RIVER');

COMMIT;
