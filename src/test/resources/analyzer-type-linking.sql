-- Analyzer Type Linking SQL
-- Ensures generic plugin AnalyzerType rows exist and links fixture analyzers.
-- Extracted from load-test-fixtures.sh for reuse by multiple fixture modes.
--
-- PluginRegistryService auto-creates analyzer_type rows at startup when plugin
-- JARs are loaded. Names MUST match PluginRegistryService.derivePluginName():
--   GenericASTMAnalyzer → "Generic ASTM"
--   GenericHL7Analyzer  → "Generic HL7"
-- These INSERTs are a fallback for environments without the plugin JAR lifecycle.

SET search_path TO clinlims;

-- Ensure Generic ASTM analyzer_type exists (ON CONFLICT DO NOTHING if already seeded)
INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic ASTM', 'Generic ASTM - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- Ensure Generic HL7 analyzer_type exists
INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'Generic HL7', 'Generic HL7 - Dashboard-configurable analyzer (requires identifier_pattern)',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- Link Generic ASTM analyzers (IDs 2006, 2013 from Feature 011)
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic ASTM'
) WHERE id IN (2006, 2013) AND analyzer_type_id IS NULL;

-- Link Generic HL7 analyzers (IDs 2007, 2008, 2012 from Feature 011)
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'Generic HL7'
) WHERE id IN (2007, 2008, 2012) AND analyzer_type_id IS NULL;
