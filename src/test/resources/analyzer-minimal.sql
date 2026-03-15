-- Analyzer Type Safety Net
-- PluginRegistryService auto-creates these at startup when plugin JARs load.
-- These INSERTs are fallback for environments without the full plugin lifecycle
-- (e.g., CI runners where plugins haven't initialized yet).
--
-- Analyzers themselves are NOT created here — they're created via the UI or
-- configuration system, which triggers autoCreateTestMappings() from the
-- profile's default_test_mappings (LOINC-based lookup against the test catalog).

SET search_path TO clinlims;

-- =============================================================================
-- ANALYZER TYPES (Plugin Capability — fallback only)
-- =============================================================================

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic ASTM', 'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic HL7', 'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic File', 'Generic File - Dashboard-configurable file import analyzer',
        'org.openelisglobal.plugins.analyzer.genericfile.GenericFileAnalyzer',
        'FILE', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- VERIFICATION
-- =============================================================================

DO $$
DECLARE
  v_type_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_type_count FROM analyzer_type WHERE name IN ('Generic ASTM', 'Generic HL7', 'Generic File');

  RAISE NOTICE 'analyzer-minimal.sql verification:';
  RAISE NOTICE '  analyzer_types: % / 3 expected', v_type_count;
END $$;
