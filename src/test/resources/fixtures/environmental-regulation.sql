-- Public synthetic environmental sample type and sampling site for the regulation-driven order
-- spec (OGC-768).
-- "Synthetic Drinking Water" (domain E) runs two catalog tests, so environmental order entry
-- has a sample type whose tests a compliance standard can link; the spec creates the
-- standards through the admin API.
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

INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id)
SELECT 99768 + ordinal, 99768, t.id
FROM (VALUES (1, 'GPT/ALAT-Serum'), (2, 'Creatinine-Serum')) AS wanted(ordinal, code)
JOIN clinlims.test t ON t.local_code = wanted.code AND t.is_active = 'Y'
WHERE NOT EXISTS (SELECT 1 FROM clinlims.sampletype_test st WHERE st.sample_type_id = 99768 AND st.test_id = t.id);

INSERT INTO clinlims.vector_sampling_site (id, code, name, type, active, lastupdated)
SELECT 99768, 'SYN-RIVER', 'Synthetic Riverside Intake', 'WATER', true, timestamp '2026-05-01 12:00:00'
WHERE NOT EXISTS (SELECT 1 FROM clinlims.vector_sampling_site WHERE code = 'SYN-RIVER');

COMMIT;
