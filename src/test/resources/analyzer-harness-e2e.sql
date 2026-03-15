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
-- VERIFICATION
-- ============================================================================

DO $$
DECLARE
  v_type_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');

  RAISE NOTICE 'analyzer-harness-e2e.sql verification:';
  RAISE NOTICE '  analyzer_types: % / 3 expected', v_type_count;
END $$;
