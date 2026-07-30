-- OGC-285 M2 seed-suite fixture: MALFORMED v1 barcode configuration.
--
-- Purpose
-- -------
-- Proves changeset 030-seed-system-presets.xml normalizes a non-numeric site_information.barcode.* value
-- to NULL (via its `value ~ '^[0-9]+$'` regex guard) and falls back to the canonical default (1) WITHOUT
-- erroring on the `value::INTEGER` cast (FRS §2.7 + data-model.md §2.5).
--
-- Design (isolating malformed-handling from total-absence fallback, per reviewer guidance):
--   * barcode.specimen.default = 'garbage'  -> non-numeric; seed must coerce to NULL then fallback to 1.
--   * barcode.specimen.height/width/max     -> VALID numbers that DIFFER from fallback constants
--                                              (height 25 / width 76 / max 10), so SystemPresetSeedMalformedInputTest
--                                              can assert the specimen row WAS read (height = 42, width = 88, max = 6)
--                                              while default_per_sample fell back to 1. If the seed silently
--                                              skipped the whole specimen row on the bad value, height would be the
--                                              fallback 25 and the test would catch it.
--
-- Only the specimen keys are seeded here; the other four types are absent on purpose, so they exercise the
-- pure-fallback path and the test stays focused on the malformed-specimen case.
--
-- Idempotent: each key is DELETEd before INSERT. Mirrors siteInfo.sql insert idiom.

DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.height', 'v1 specimen label height (mm)', '42');
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.width', 'v1 specimen label width (mm)', '88');
-- Intentionally non-numeric: the seed must NOT crash on value::INTEGER and must fall back to default = 1.
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.default', 'v1 specimen default qty (intentionally malformed)', 'garbage');
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.max', 'v1 specimen max qty', '6');
