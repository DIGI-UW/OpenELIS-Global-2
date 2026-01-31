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

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routes incoming analyzer data to the correct Analyzer instance.
 *
 * <p>
 * This service implements a multi-stage routing strategy:
 *
 * <ol>
 * <li><b>IP-based routing</b>: Match by source IP address to specific instance
 * <li><b>Pattern-based routing</b>: Match identifier against AnalyzerType
 * patterns
 * <li><b>Legacy fallback</b>: Use existing plugin's isTargetAnalyzer() method
 * </ol>
 *
 * <p>
 * This enables multiple physical analyzers of the same type (e.g., two Horiba
 * Micros 60 devices in different labs) while maintaining backward compatibility
 * with legacy plugins.
 */
@Service
public class InstanceAwareAnalyzerRouter {

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerConfigurationService analyzerConfigurationService;

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    /**
     * Route context containing information about the source of analyzer data.
     */
    public static class RouteContext {
        private final String sourceIp;
        private final String identifier;
        private final List<String> lines;

        public RouteContext(String sourceIp, String identifier, List<String> lines) {
            this.sourceIp = sourceIp;
            this.identifier = identifier;
            this.lines = lines;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public String getIdentifier() {
            return identifier;
        }

        public List<String> getLines() {
            return lines;
        }
    }

    /**
     * Route result containing the matched analyzer and plugin.
     */
    public static class RouteResult {
        private final Analyzer analyzer;
        private final AnalyzerType analyzerType;
        private final AnalyzerImporterPlugin plugin;
        private final String routeMethod;

        public RouteResult(Analyzer analyzer, AnalyzerType analyzerType, AnalyzerImporterPlugin plugin,
                String routeMethod) {
            this.analyzer = analyzer;
            this.analyzerType = analyzerType;
            this.plugin = plugin;
            this.routeMethod = routeMethod;
        }

        public Analyzer getAnalyzer() {
            return analyzer;
        }

        public AnalyzerType getAnalyzerType() {
            return analyzerType;
        }

        public AnalyzerImporterPlugin getPlugin() {
            return plugin;
        }

        public String getRouteMethod() {
            return routeMethod;
        }

        public boolean isSuccessful() {
            return analyzer != null && plugin != null;
        }
    }

    /**
     * Route incoming data to the appropriate analyzer instance.
     *
     * @param context The routing context with source IP, identifier, and raw data
     * @return RouteResult containing the matched analyzer and plugin
     */
    @Transactional(readOnly = true)
    public RouteResult route(RouteContext context) {
        // Stage 1: Try IP-based routing
        RouteResult result = routeByIp(context);
        if (result.isSuccessful()) {
            return result;
        }

        // Stage 2: Try pattern-based routing
        result = routeByPattern(context);
        if (result.isSuccessful()) {
            return result;
        }

        // Stage 3: Legacy plugin fallback
        result = routeByLegacyPlugin(context);
        return result;
    }

    /**
     * Stage 1: Route by source IP address.
     */
    private RouteResult routeByIp(RouteContext context) {
        if (context.getSourceIp() == null || context.getSourceIp().isEmpty()) {
            return new RouteResult(null, null, null, "IP_NO_MATCH");
        }

        // Find analyzer configuration with matching IP
        Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByIpAddress(context.getSourceIp());

        if (configOpt.isPresent()) {
            AnalyzerConfiguration config = configOpt.get();
            Analyzer analyzer = config.getAnalyzer();
            if (analyzer != null && analyzer.isActive()) {
                AnalyzerImporterPlugin plugin = pluginAnalyzerService.getPluginByAnalyzerId(analyzer.getId());
                if (plugin != null) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "routeByIp",
                            "Routed by IP " + context.getSourceIp() + " to analyzer: " + analyzer.getName());
                    return new RouteResult(analyzer, analyzer.getAnalyzerType(), plugin, "IP_MATCH");
                }
            }
        }

        return new RouteResult(null, null, null, "IP_NO_MATCH");
    }

    /**
     * Stage 2: Route by identifier pattern matching.
     */
    private RouteResult routeByPattern(RouteContext context) {
        if (context.getIdentifier() == null || context.getIdentifier().isEmpty()) {
            return new RouteResult(null, null, null, "PATTERN_NO_MATCH");
        }

        // Find analyzer type matching the identifier pattern
        Optional<AnalyzerType> typeOpt = analyzerTypeService.findMatchingType(context.getIdentifier());

        if (typeOpt.isPresent()) {
            AnalyzerType type = typeOpt.get();
            // Get the first active instance of this type
            List<Analyzer> instances = type.getInstances();
            Analyzer matchedAnalyzer = instances.stream().filter(Analyzer::isActive).findFirst().orElse(null);

            if (matchedAnalyzer != null) {
                AnalyzerImporterPlugin plugin = pluginAnalyzerService.getPluginByAnalyzerId(matchedAnalyzer.getId());
                if (plugin != null) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "routeByPattern", "Routed by pattern '"
                            + type.getIdentifierPattern() + "' to analyzer: " + matchedAnalyzer.getName());
                    return new RouteResult(matchedAnalyzer, type, plugin, "PATTERN_MATCH");
                }
            }
        }

        return new RouteResult(null, null, null, "PATTERN_NO_MATCH");
    }

    /**
     * Stage 3: Legacy plugin fallback using isTargetAnalyzer().
     */
    private RouteResult routeByLegacyPlugin(RouteContext context) {
        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();

        for (AnalyzerImporterPlugin plugin : plugins) {
            try {
                if (plugin.isTargetAnalyzer(context.getLines())) {
                    // Find the analyzer associated with this plugin
                    Analyzer analyzer = findAnalyzerForPlugin(plugin);
                    if (analyzer != null) {
                        LogEvent.logDebug(this.getClass().getSimpleName(), "routeByLegacyPlugin",
                                "Routed by legacy plugin to analyzer: " + analyzer.getName());
                        return new RouteResult(analyzer, analyzer.getAnalyzerType(), plugin, "LEGACY_PLUGIN_MATCH");
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "routeByLegacyPlugin",
                        "Error checking plugin: " + e.getMessage());
            }
        }

        return new RouteResult(null, null, null, "NO_MATCH");
    }

    /**
     * Find the Analyzer entity associated with a plugin.
     */
    private Analyzer findAnalyzerForPlugin(AnalyzerImporterPlugin plugin) {
        // Check if plugin is registered with an analyzer ID
        String pluginClassName = plugin.getClass().getName();

        // Search by plugin class name in analyzer types
        Optional<AnalyzerType> typeOpt = analyzerTypeService.getByPluginClassName(pluginClassName);
        if (typeOpt.isPresent()) {
            List<Analyzer> instances = typeOpt.get().getInstances();
            return instances.stream().filter(Analyzer::isActive).findFirst().orElse(null);
        }

        // Fallback: search all analyzers for one with matching plugin
        List<Analyzer> allAnalyzers = analyzerService.getAll();
        for (Analyzer analyzer : allAnalyzers) {
            AnalyzerImporterPlugin registeredPlugin = pluginAnalyzerService.getPluginByAnalyzerId(analyzer.getId());
            if (registeredPlugin == plugin) {
                return analyzer;
            }
        }

        return null;
    }

    /**
     * Convenience method to route with just identifier and lines (no IP).
     */
    @Transactional(readOnly = true)
    public RouteResult route(String identifier, List<String> lines) {
        return route(new RouteContext(null, identifier, lines));
    }
}
