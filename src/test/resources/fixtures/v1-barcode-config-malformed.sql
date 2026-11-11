-- OGC-285 M2 seed-suite fixture: a MALFORMED legacy value next to valid ones (SystemPresetSeedMalformedInputTest).
--
-- The seed changeset normalises a non-numeric site_information value to NULL (value ~ '^[0-9]+$')
-- and falls back to the canonical default instead of failing on value::INTEGER. This fixture pairs
-- numDefaultSpecimenLabels = 'garbage' with valid, non-fallback specimen height / width / max so the
-- test can prove both that the numeric keys were read and that the bad one fell back to 1.
--
-- Key names are the real ConfigurationProperties.Property names (OGC-1219).
-- Idempotent: each key is DELETEd before INSERT.

DELETE FROM clinlims.site_information WHERE name = 'heightSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightSpecimenLabels', 'v1 specimen label height (mm)', '42');
DELETE FROM clinlims.site_information WHERE name = 'widthSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthSpecimenLabels', 'v1 specimen label width (mm)', '88');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultSpecimenLabels', 'v1 specimen default qty (intentionally malformed)', 'garbage');
DELETE FROM clinlims.site_information WHERE name = 'numMaxSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxSpecimenLabels', 'v1 specimen max qty', '6');
