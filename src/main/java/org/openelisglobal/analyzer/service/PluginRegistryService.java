/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that auto-discovers loaded analyzer plugins and registers them in the
 * analyzer_type table.
 *
 * <p>
 * Feature: 004-analyzer-management + 011-madagascar-analyzer-integration
 *
 * <p>
 * This service bridges the gap between JAR-loaded plugins (PluginLoader) and
 * the database-driven analyzer type system. It runs after PluginLoader has
 * loaded all plugins, iterates through the registered plugins, and creates
 * corresponding AnalyzerType records for any plugins not yet in the database.
 *
 * <p>
 * Architecture rationale:
 *
 * <ul>
 * <li>Uses @PostConstruct + @DependsOn to ensure plugins are loaded first
 * <li>Preserves idempotency: existing records are not modified
 * <li>Separates plugin capability (AnalyzerType) from physical device
 * (Analyzer)
 * <li>Enables UI to display all available plugin types in the dropdown
 * </ul>
 *
 * <p>
 * This approach was chosen over Liquibase data inserts because:
 *
 * <ul>
 * <li>Plugin JAR files can be added/removed without database changes
 * <li>Plugin metadata (isGenericPlugin, protocol) comes from the plugin itself
 * <li>Auto-discovery ensures consistency between loaded plugins and database
 * </ul>
 */
@Service
@DependsOn("pluginLoader")
public class PluginRegistryService {

    private static final String GENERIC_ASTM_CLASS = "org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer";
    private static final String GENERIC_HL7_CLASS = "org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer";

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    /**
     * Auto-discover and register all loaded analyzer plugins in the analyzer_type
     * table.
     *
     * <p>
     * This method runs after PluginLoader has loaded all JAR plugins. It:
     *
     * <ol>
     * <li>Gets all registered plugins from PluginAnalyzerService
     * <li>For each plugin, checks if an AnalyzerType record exists
     * <li>If not, creates one with metadata derived from the plugin
     * </ol>
     */
    @PostConstruct
    @Transactional
    public void registerLoadedPlugins() {
        LogEvent.logInfo(this.getClass().getName(), "registerLoadedPlugins",
                "Auto-discovering loaded analyzer plugins...");

        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();
        int registered = 0;
        int skipped = 0;

        for (AnalyzerImporterPlugin plugin : plugins) {
            String className = plugin.getClass().getName();

            Optional<AnalyzerType> existing = analyzerTypeService.getByPluginClassName(className);
            if (existing.isPresent()) {
                LogEvent.logDebug(this.getClass().getName(), "registerLoadedPlugins",
                        "Plugin already registered: " + className);
                skipped++;
                continue;
            }

            AnalyzerType analyzerType = createAnalyzerTypeFromPlugin(plugin);
            try {
                analyzerTypeService.insert(analyzerType);
                LogEvent.logInfo(this.getClass().getName(), "registerLoadedPlugins",
                        "Registered analyzer type: " + analyzerType.getName() + " (plugin: " + className + ")");
                registered++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "registerLoadedPlugins",
                        "Failed to register analyzer type for plugin: " + className);
                LogEvent.logError(e);
            }
        }

        // Link legacy analyzers (created by connect()) to their AnalyzerType
        linkLegacyAnalyzersToTypes();

        int total = analyzerTypeService.getAll().size();
        LogEvent.logInfo(this.getClass().getName(), "registerLoadedPlugins", String.format(
                "Plugin registry complete: %d new, %d existing, %d total analyzer types", registered, skipped, total));
    }

    /**
     * Link legacy analyzer rows to their analyzer_type using name-based matching.
     *
     * <p>
     * Legacy plugin connect() creates analyzer rows via addAnalyzerDatabaseParts()
     * without setting analyzer_type_id. This method matches analyzer names against
     * AnalyzerType plugin class names using two strategies:
     *
     * <ol>
     * <li>Exact match: analyzer.name == simple class name (e.g.,
     * "SysmexXNLAnalyzer")
     * <li>Suffix-stripped match: analyzer.name == class name without "Analyzer"
     * suffix (e.g., "Mindray" matches "MindrayAnalyzer")
     * <li>Prefix match: for plugins that create multiple analyzers (e.g., Cobas4800
     * creates "Cobas4800VLAnalyzer" and "Cobas4800EIDAnalyzer")
     * </ol>
     */
    void linkLegacyAnalyzersToTypes() {
        List<Analyzer> allAnalyzers = analyzerService.getAll();
        List<AnalyzerType> allTypes = analyzerTypeService.getAll();
        int linked = 0;

        // Build lookup maps from plugin class simple names to AnalyzerType
        Map<String, AnalyzerType> nameToType = new HashMap<>();
        for (AnalyzerType type : allTypes) {
            if (type.getPluginClassName() == null) {
                continue;
            }
            String simpleName = type.getPluginClassName().substring(type.getPluginClassName().lastIndexOf('.') + 1);
            nameToType.put(simpleName, type);
            String stripped = simpleName.replaceAll("Analyzer$", "");
            if (!stripped.equals(simpleName)) {
                nameToType.put(stripped, type);
            }
        }

        for (Analyzer analyzer : allAnalyzers) {
            if (analyzer.getAnalyzerType() != null) {
                continue;
            }

            String analyzerName = analyzer.getName();
            AnalyzerType matched = nameToType.get(analyzerName);

            // Prefix match for multi-analyzer plugins (e.g., Cobas4800 →
            // Cobas4800VLAnalyzer)
            if (matched == null) {
                String strippedAnalyzerName = analyzerName.replaceAll("Analyzer$", "");
                int bestLen = 0;
                for (AnalyzerType type : allTypes) {
                    if (type.getPluginClassName() == null) {
                        continue;
                    }
                    String simpleName = type.getPluginClassName()
                            .substring(type.getPluginClassName().lastIndexOf('.') + 1);
                    String base = simpleName.replaceAll("Analyzer$", "");
                    if (strippedAnalyzerName.startsWith(base) && base.length() > bestLen) {
                        matched = type;
                        bestLen = base.length();
                    }
                }
            }

            if (matched != null) {
                analyzer.setAnalyzerType(matched);
                analyzer.setSysUserId("1");
                analyzerService.save(analyzer);
                LogEvent.logInfo(this.getClass().getName(), "linkLegacyAnalyzersToTypes",
                        "Linked analyzer '" + analyzerName + "' to type '" + matched.getName() + "'");
                linked++;
            }
        }

        if (linked > 0) {
            LogEvent.logInfo(this.getClass().getName(), "linkLegacyAnalyzersToTypes",
                    "Linked " + linked + " legacy analyzers to their types");
        }
    }

    /**
     * Create an AnalyzerType entity from a plugin instance.
     *
     * @param plugin The analyzer plugin to create a type for
     * @return A new AnalyzerType entity (not yet persisted)
     */
    private AnalyzerType createAnalyzerTypeFromPlugin(AnalyzerImporterPlugin plugin) {
        String className = plugin.getClass().getName();
        boolean isGeneric = plugin.isGenericPlugin();

        AnalyzerType type = new AnalyzerType();
        type.setName(derivePluginName(className));
        type.setDescription(derivePluginDescription(className, isGeneric));
        type.setProtocol(detectProtocol(className));
        type.setPluginClassName(className);
        type.setGenericPlugin(isGeneric);
        type.setActive(true);
        type.setSysUserId("1");

        if (isGeneric) {
            type.setIdentifierPattern(getDefaultIdentifierPattern(className));
        }

        return type;
    }

    /**
     * Derive a human-readable name from the plugin class name.
     *
     * <p>
     * Examples:
     *
     * <ul>
     * <li>HoribaMicros60Analyzer → Horiba Micros 60
     * <li>SysmexXNLAnalyzer → Sysmex XNL
     * <li>GenericASTMAnalyzer → Generic ASTM
     * <li>ABXMicros60Plugin → ABX Micros 60
     * <li>GenericHL7Analyzer → Generic HL7
     * <li>MindrayBC5380Analyzer → Mindray BC 5380
     * </ul>
     *
     * @param className Fully qualified class name
     * @return Human-readable name
     */
    String derivePluginName(String className) {
        // Get simple class name
        String simpleName = className.substring(className.lastIndexOf('.') + 1);

        // Remove common suffixes
        simpleName = simpleName.replaceAll("Analyzer$", "");
        simpleName = simpleName.replaceAll("Importer$", "");
        simpleName = simpleName.replaceAll("Plugin$", "");

        // Insert spaces before uppercase letters and numbers
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (i > 0) {
                char prev = simpleName.charAt(i - 1);
                // Lookahead for transition detection
                boolean hasNext = (i + 1) < simpleName.length();
                char next = hasNext ? simpleName.charAt(i + 1) : '\0';

                // Add space before uppercase if previous was lowercase (basic CamelCase)
                if (Character.isUpperCase(c) && Character.isLowerCase(prev)) {
                    result.append(' ');
                }
                // Add space before uppercase that starts a new word after an abbreviation
                // Pattern: UPPERCASE followed by Uppercase+lowercase (e.g., ABX -> Micros)
                else if (Character.isUpperCase(c) && Character.isUpperCase(prev) && hasNext
                        && Character.isLowerCase(next)) {
                    result.append(' ');
                }
                // Add space before number if previous was lowercase (model number after word)
                else if (Character.isDigit(c) && Character.isLowerCase(prev)) {
                    result.append(' ');
                }
                // Add space before digit sequences (3+ digits = model number, not abbreviation)
                // This handles BC5380 → BC 5380 but keeps HL7 together
                else if (Character.isDigit(c) && Character.isUpperCase(prev) && isLongDigitSequence(simpleName, i)) {
                    result.append(' ');
                }
                // Add space after number sequence if current is uppercase
                else if (Character.isDigit(prev) && Character.isUpperCase(c)) {
                    result.append(' ');
                }
            }
            result.append(c);
        }

        return result.toString().trim();
    }

    /**
     * Check if a digit sequence starting at position i has 3 or more digits.
     *
     * <p>
     * Model numbers (5380, 1000, 311) typically have 3+ digits, while abbreviation
     * suffixes (like 7 in HL7) have 1-2 digits.
     */
    private boolean isLongDigitSequence(String s, int start) {
        int count = 0;
        for (int i = start; i < s.length() && Character.isDigit(s.charAt(i)); i++) {
            count++;
        }
        return count >= 3;
    }

    /**
     * Generate a description for the analyzer type.
     *
     * @param className Fully qualified class name
     * @param isGeneric Whether this is a generic plugin
     * @return Description string
     */
    private String derivePluginDescription(String className, boolean isGeneric) {
        String name = derivePluginName(className);
        if (isGeneric) {
            return name + " - Dashboard-configurable analyzer (requires identifier_pattern)";
        }
        return name + " - Legacy plugin with hardcoded mappings";
    }

    /**
     * Detect the protocol based on the plugin class name.
     *
     * @param className Fully qualified class name
     * @return Protocol string (ASTM, HL7, or FILE)
     */
    String detectProtocol(String className) {
        String lowerName = className.toLowerCase();

        if (lowerName.contains("hl7")) {
            return "HL7";
        }
        if (lowerName.contains("file") || lowerName.contains("csv") || lowerName.contains("import")) {
            // Check if it's clearly a file-based importer
            if (!lowerName.contains("astm") && !lowerName.contains("serial")) {
                return "FILE";
            }
        }
        // Default to ASTM (most common protocol)
        return "ASTM";
    }

    /**
     * Get a default identifier pattern for generic plugins.
     *
     * @param className Fully qualified class name
     * @return Default identifier pattern, or null for non-generic plugins
     */
    private String getDefaultIdentifierPattern(String className) {
        if (className.equals(GENERIC_ASTM_CLASS)) {
            return ".*"; // Match any - to be configured per-analyzer via UI
        }
        if (className.equals(GENERIC_HL7_CLASS)) {
            return ".*"; // Match any - to be configured per-analyzer via UI
        }
        return null; // Non-generic plugins don't use patterns
    }
}
