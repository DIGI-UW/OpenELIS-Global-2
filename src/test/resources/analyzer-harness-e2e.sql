-- Analyzer harness fixtures for CI E2E.
-- Scope: analyzer_type safety net ONLY — no analyzer rows.
-- All analyzers are created via REST API (seed-analyzers.sh) using profile-based
-- defaultConfigId, which triggers autoCreateTestMappings() from LOINC lookup.
SET search_path TO clinlims;

-- ============================================================================
-- ANALYZER TYPES (idempotent fallback — PluginRegistryService creates these
--    at startup from plugin JARs, but CI fixture loading may run before plugins
--    finish initializing)
-- ============================================================================

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic ASTM', 'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic HL7', 'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic File', 'Generic File - Dashboard-configurable file import analyzer',
        'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
        'FILE', true, true, NOW())
ON CONFLICT (name) DO UPDATE SET is_active = true;

-- ============================================================================
-- DEMO WORKFLOW PATIENT FIXTURE (required by Add Order demo tests)
-- ============================================================================
-- Keep this lightweight and idempotent for analyzer harness CI.
-- The Playwright OGC-284 demo searches for "Smith, John" and expects
-- patient-details to include nationalId, birthDateForDisplay, and gender.
INSERT INTO person (id, last_name, first_name, middle_name, city, state, zip_code, country,
                    work_phone, home_phone, cell_phone, primary_phone, email, lastupdated)
SELECT 9001000, 'TEST-Smith', 'John', 'Test', 'Test City', 'Test State', '12345', 'USA',
       '555-0101', '555-0102', '555-0103', '555-0101', 'john.test@openelis.org', NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM person
  WHERE first_name = 'John' AND last_name = 'TEST-Smith'
);

INSERT INTO patient (id, person_id, race, gender, birth_date, birth_time, national_id,
                     ethnicity, external_id, entered_birth_date, lastupdated)
SELECT 9001000, p.id, 'black', 'M', '1990-01-15 00:00:00'::timestamp,
       '1990-01-15 10:00:00'::timestamp, 'E2E-PAT-001', 'U', 'E2E-PAT-001',
       '01/15/1990', NOW()
FROM person p
WHERE p.first_name = 'John'
  AND p.last_name = 'TEST-Smith'
  AND NOT EXISTS (
    SELECT 1
    FROM patient pat
    WHERE pat.id = 9001000
       OR pat.external_id = 'E2E-PAT-001'
       OR pat.national_id = 'E2E-PAT-001'
  );

-- ============================================================================
-- VERIFICATION
-- ============================================================================

DO $$
DECLARE
  v_type_count INTEGER;
  v_demo_patient_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');
  SELECT COUNT(*)
    INTO v_demo_patient_count
    FROM patient pat
    JOIN person per ON per.id = pat.person_id
   WHERE per.first_name = 'John'
     AND per.last_name = 'TEST-Smith'
     AND pat.gender = 'M'
     AND pat.external_id = 'E2E-PAT-001';

  RAISE NOTICE 'analyzer-harness-e2e.sql verification:';
  RAISE NOTICE '  analyzer_types: % / 3 expected', v_type_count;
  RAISE NOTICE '  demo_patient_john_test_smith: % / 1 expected', v_demo_patient_count;
END $$;
