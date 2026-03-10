-- ==============================================================================
-- Horiba Analyzer E2E Test Fixtures
-- ==============================================================================
-- Purpose: Seed Horiba Pentra 60 + Micros 60 for E2E testing
-- Loading: Via load-test-fixtures.sh (Level 3 in dependency chain)
-- Metadata Tier: Test fixtures (idempotent SQL, NOT Liquibase)
--
-- Pattern: Follows metadata-management-analysis-report.md Recommendation #9
--   - Fully idempotent (WHERE NOT EXISTS)
--   - Symbolic references (lookup by name, not hardcoded IDs)
--   - Test IDs use E2E range (2001-2002)
--
-- Prerequisites:
--   - Test definitions from horiba-*.csv must exist (loaded by ConfigurationInitializationService)
--   - Or test definitions must be pre-existing in the database
-- ==============================================================================

-- ============================================================================
-- SECTION 1: Analyzer Definitions
-- ============================================================================

-- Create Horiba Pentra 60 analyzer (ID: 2001)
INSERT INTO clinlims.analyzer (id, name, description, is_active, lastupdated)
SELECT 2001, 'Horiba ABX Pentra 60', '5-Part Differential Hematology Analyzer', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM clinlims.analyzer WHERE name = 'Horiba ABX Pentra 60');

-- Create Horiba Micros 60 analyzer (ID: 2002)
INSERT INTO clinlims.analyzer (id, name, description, is_active, lastupdated)
SELECT 2002, 'Horiba ABX Micros 60', '3-Part Differential Hematology Analyzer', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM clinlims.analyzer WHERE name = 'Horiba ABX Micros 60');

-- ============================================================================
-- SECTION 2: Pentra 60 Test Mappings (20 parameters)
-- Uses symbolic references (lookup test by name), not hardcoded test IDs
-- ============================================================================

-- WBC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'WBC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'WBC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'WBC'
  );

-- RBC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'RBC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'RBC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'RBC'
  );

-- HGB
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'HGB', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'HGB'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'HGB'
  );

-- HCT
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'HCT', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'HCT'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'HCT'
  );

-- MCV
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCV', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MCV'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCV'
  );

-- MCH
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCH', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MCH'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCH'
  );

-- MCHC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCHC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MCHC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCHC'
  );

-- PLT
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'PLT', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'PLT'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'PLT'
  );

-- RDW
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'RDW', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'RDW'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'RDW'
  );

-- MPV
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MPV', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MPV'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MPV'
  );

-- LYM%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'LYM%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'LYM%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'LYM%'
  );

-- LYM#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'LYM#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'LYM#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'LYM#'
  );

-- MON%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MON%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MON%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MON%'
  );

-- MON#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MON#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'MON#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MON#'
  );

-- NEU%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'NEU%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'NEU%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'NEU%'
  );

-- NEU#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'NEU#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'NEU#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'NEU#'
  );

-- EOS%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'EOS%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'EOS%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'EOS%'
  );

-- EOS#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'EOS#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'EOS#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'EOS#'
  );

-- BAS%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'BAS%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'BAS%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'BAS%'
  );

-- BAS#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'BAS#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Pentra 60' AND t.description = 'BAS#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'BAS#'
  );

-- ============================================================================
-- SECTION 3: Micros 60 Test Mappings (16 parameters)
-- Same CBC parameters as Pentra, but with MXD instead of MON/EOS/BAS
-- ============================================================================

-- WBC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'WBC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'WBC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'WBC'
  );

-- RBC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'RBC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'RBC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'RBC'
  );

-- HGB
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'HGB', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'HGB'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'HGB'
  );

-- HCT
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'HCT', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'HCT'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'HCT'
  );

-- MCV
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCV', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MCV'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCV'
  );

-- MCH
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCH', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MCH'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCH'
  );

-- MCHC
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MCHC', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MCHC'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MCHC'
  );

-- PLT
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'PLT', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'PLT'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'PLT'
  );

-- RDW
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'RDW', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'RDW'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'RDW'
  );

-- MPV
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MPV', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MPV'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MPV'
  );

-- LYM%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'LYM%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'LYM%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'LYM%'
  );

-- LYM#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'LYM#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'LYM#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'LYM#'
  );

-- MXD% (3-part differential: Mixed cells = Monocytes + Eosinophils + Basophils)
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MXD%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MXD%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MXD%'
  );

-- MXD#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'MXD#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'MXD#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'MXD#'
  );

-- NEU%
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'NEU%', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'NEU%'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'NEU%'
  );

-- NEU#
INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated)
SELECT a.id, 'NEU#', t.id, NOW()
FROM clinlims.analyzer a, clinlims.test t
WHERE a.name = 'Horiba ABX Micros 60' AND t.description = 'NEU#'
  AND NOT EXISTS (
    SELECT 1 FROM clinlims.analyzer_test_map
    WHERE analyzer_id = a.id AND analyzer_test_name = 'NEU#'
  );

-- ============================================================================
-- VERIFICATION QUERIES (for manual validation)
-- ============================================================================
-- SELECT name, description FROM clinlims.analyzer WHERE name LIKE 'Horiba%';
-- SELECT COUNT(*) FROM clinlims.analyzer_test_map WHERE analyzer_id = 2001;  -- Should be 20
-- SELECT COUNT(*) FROM clinlims.analyzer_test_map WHERE analyzer_id = 2002;  -- Should be 16
