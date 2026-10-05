-- OGC-285 M2 seed-suite fixture: legacy v1 barcode configuration in clinlims.site_information.
--
-- Purpose
-- -------
-- Changeset 030-seed-system-presets.xml builds the 5 system label_preset rows by reading the real
-- per-type site_information keys named in ConfigurationProperties.Property:
--   height{Type}Labels, width{Type}Labels, numDefault{Type}Labels, numMax{Type}Labels
-- for Type in Order, Specimen, Block, Slide, Freezer (FRS §2.7 step 1, corrected by OGC-1219: the
-- first release read a barcode.{type}.{field} namespace that never existed, so every site was
-- seeded from the fallbacks 25 / 76 / 1 / 10).
--
-- The seed tests DELETE the init-seeded system presets, stash whatever legacy keys the test database
-- carries, load THIS fixture, re-run the real 030 <sql> blocks (extracted from the changeset file at
-- runtime), and restore the stashed keys afterwards.
--
-- Value choice (inversion-worthiness): every numeric value below intentionally DIFFERS from the
-- canonical fallback constants (25/76/1/10). If the seed ignored these keys and used fallbacks instead,
-- the assertions in SystemPresetSeedTest would fail, and the MG-6 guard in the changeset would raise.
-- Mirrors siteInfo.sql's insert idiom (id via nextval, "name"/"value" quoted, value_type defaults to
-- 'text').
--
-- This file is idempotent: each key is DELETEd before INSERT so repeated loads across tests are safe.

-- ---- Order Label (per-order scope) ----------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'heightOrderLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightOrderLabels', 'v1 order label height (mm)', '30');
DELETE FROM clinlims.site_information WHERE name = 'widthOrderLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthOrderLabels', 'v1 order label width (mm)', '90');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultOrderLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultOrderLabels', 'v1 order default qty', '2');
DELETE FROM clinlims.site_information WHERE name = 'numMaxOrderLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxOrderLabels', 'v1 order max qty', '8');

-- ---- Specimen Label (per-sample scope) ------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'heightSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightSpecimenLabels', 'v1 specimen label height (mm)', '40');
DELETE FROM clinlims.site_information WHERE name = 'widthSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthSpecimenLabels', 'v1 specimen label width (mm)', '80');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultSpecimenLabels', 'v1 specimen default qty', '3');
DELETE FROM clinlims.site_information WHERE name = 'numMaxSpecimenLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxSpecimenLabels', 'v1 specimen max qty', '7');

-- ---- Block Label (per-sample scope) ---------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'heightBlockLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightBlockLabels', 'v1 block label height (mm)', '35');
DELETE FROM clinlims.site_information WHERE name = 'widthBlockLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthBlockLabels', 'v1 block label width (mm)', '70');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultBlockLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultBlockLabels', 'v1 block default qty', '4');
DELETE FROM clinlims.site_information WHERE name = 'numMaxBlockLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxBlockLabels', 'v1 block max qty', '6');

-- ---- Slide Label (per-sample scope) ---------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'heightSlideLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightSlideLabels', 'v1 slide label height (mm)', '45');
DELETE FROM clinlims.site_information WHERE name = 'widthSlideLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthSlideLabels', 'v1 slide label width (mm)', '85');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultSlideLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultSlideLabels', 'v1 slide default qty', '5');
DELETE FROM clinlims.site_information WHERE name = 'numMaxSlideLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxSlideLabels', 'v1 slide max qty', '9');

-- ---- Freezer Label (per-sample scope) -------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'heightFreezerLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'heightFreezerLabels', 'v1 freezer label height (mm)', '50');
DELETE FROM clinlims.site_information WHERE name = 'widthFreezerLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'widthFreezerLabels', 'v1 freezer label width (mm)', '60');
DELETE FROM clinlims.site_information WHERE name = 'numDefaultFreezerLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numDefaultFreezerLabels', 'v1 freezer default qty', '2');
DELETE FROM clinlims.site_information WHERE name = 'numMaxFreezerLabels';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'numMaxFreezerLabels', 'v1 freezer max qty', '5');
