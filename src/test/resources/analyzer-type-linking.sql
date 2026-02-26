-- Analyzer Type Linking SQL
-- Ensures generic plugin AnalyzerType rows exist and links fixture analyzers.
-- Extracted from load-test-fixtures.sh for reuse by multiple fixture modes.
--
-- Generic plugins (GenericASTM, GenericHL7) don't auto-create analyzer_type rows
-- via addAnalyzerDatabaseParts() — they only register in-memory via registerAnalyzer().
-- We must INSERT the analyzer_type rows ourselves so findGenericAnalyzersWithPatterns()
-- can JOIN on analyzer_type.is_generic_plugin = true.

SET search_path TO clinlims;

-- Ensure GenericASTM analyzer_type exists (ON CONFLICT DO NOTHING if already seeded)
INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'GenericASTM', 'Generic ASTM analyzer plugin',
        'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer',
        'ASTM_LIS2_A2', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- Ensure GenericHL7 analyzer_type exists
INSERT INTO analyzer_type (id, name, description, plugin_class_name, protocol, is_generic_plugin, is_active, last_updated)
VALUES (nextval('analyzer_type_seq'), 'GenericHL7', 'Generic HL7 analyzer plugin',
        'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer',
        'HL7_V2_3_1', true, true, NOW())
ON CONFLICT (name) DO NOTHING;

-- Link GenericASTM analyzers (IDs 2006, 2013 from Feature 011)
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'GenericASTM'
) WHERE id IN (2006, 2013) AND analyzer_type_id IS NULL;

-- Link GenericHL7 analyzers (IDs 2007, 2008, 2012 from Feature 011)
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type WHERE name = 'GenericHL7'
) WHERE id IN (2007, 2008, 2012) AND analyzer_type_id IS NULL;
