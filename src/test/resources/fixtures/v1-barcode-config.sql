-- OGC-285 M2 seed-suite fixture: legacy v1 barcode configuration in clinlims.site_information.
--
-- Purpose
-- -------
-- Changeset 030-seed-system-presets.xml builds the 5 system label_preset rows by reading
-- clinlims.site_information rows named barcode.{order,specimen,block,slide,freezer}.{default,max,height,width}
-- (FRS §2.7), normalizing non-numeric values to NULL and falling back to canonical constants
-- (height 25 / width 76 / default 1 / max 10).
--
-- The test DB's init data (postgre-db-init/OpenELIS-Global.sql + siteInfo.sql) seeds NO barcode.* keys,
-- so the Liquibase-init run of 030 produced 5 presets entirely from fallbacks. To exercise the real
-- key-reading + scope-mapping + fallback logic, the seed tests DELETE the init-seeded system presets,
-- load THIS fixture, then re-run the real 030 <sql> block (extracted from the changeset file at runtime).
--
-- Value choice (inversion-worthiness): every numeric value below intentionally DIFFERS from the
-- canonical fallback constants (25/76/1/10). If the seed ignored these keys and used fallbacks instead,
-- the assertions in SystemPresetSeedTest would fail. Mirrors siteInfo.sql's insert idiom
-- (id via nextval, "name"/"value" quoted, value_type defaults to 'text').
--
-- site_information.value is VARCHAR(200); all values below are short numeric strings (or the literal
-- 'garbage' sentinel) and fit comfortably.
--
-- This file is idempotent: each key is DELETEd before INSERT so repeated loads across tests are safe.

-- ---- Order Label (per-order scope) ----------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'barcode.order.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.order.height', 'v1 order label height (mm)', '30');
DELETE FROM clinlims.site_information WHERE name = 'barcode.order.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.order.width', 'v1 order label width (mm)', '90');
DELETE FROM clinlims.site_information WHERE name = 'barcode.order.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.order.default', 'v1 order default qty', '2');
DELETE FROM clinlims.site_information WHERE name = 'barcode.order.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.order.max', 'v1 order max qty', '8');

-- ---- Specimen Label (per-sample scope) ------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.height', 'v1 specimen label height (mm)', '40');
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.width', 'v1 specimen label width (mm)', '80');
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.default', 'v1 specimen default qty', '3');
DELETE FROM clinlims.site_information WHERE name = 'barcode.specimen.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.specimen.max', 'v1 specimen max qty', '7');

-- ---- Block Label (per-sample scope) ---------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'barcode.block.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.block.height', 'v1 block label height (mm)', '35');
DELETE FROM clinlims.site_information WHERE name = 'barcode.block.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.block.width', 'v1 block label width (mm)', '70');
DELETE FROM clinlims.site_information WHERE name = 'barcode.block.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.block.default', 'v1 block default qty', '4');
DELETE FROM clinlims.site_information WHERE name = 'barcode.block.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.block.max', 'v1 block max qty', '6');

-- ---- Slide Label (per-sample scope) ---------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'barcode.slide.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.slide.height', 'v1 slide label height (mm)', '45');
DELETE FROM clinlims.site_information WHERE name = 'barcode.slide.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.slide.width', 'v1 slide label width (mm)', '85');
DELETE FROM clinlims.site_information WHERE name = 'barcode.slide.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.slide.default', 'v1 slide default qty', '5');
DELETE FROM clinlims.site_information WHERE name = 'barcode.slide.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.slide.max', 'v1 slide max qty', '9');

-- ---- Freezer Label (per-sample scope) -------------------------------------------------------------
DELETE FROM clinlims.site_information WHERE name = 'barcode.freezer.height';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.freezer.height', 'v1 freezer label height (mm)', '50');
DELETE FROM clinlims.site_information WHERE name = 'barcode.freezer.width';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.freezer.width', 'v1 freezer label width (mm)', '60');
DELETE FROM clinlims.site_information WHERE name = 'barcode.freezer.default';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.freezer.default', 'v1 freezer default qty', '2');
DELETE FROM clinlims.site_information WHERE name = 'barcode.freezer.max';
INSERT INTO clinlims.site_information (id, lastupdated, "name", description, "value")
    VALUES (nextval('clinlims.site_information_seq'), now(), 'barcode.freezer.max', 'v1 freezer max qty', '5');
