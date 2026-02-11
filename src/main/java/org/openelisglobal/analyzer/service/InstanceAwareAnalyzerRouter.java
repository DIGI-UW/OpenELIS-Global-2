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
 * This service implements a 2-stage routing strategy:
 *
 * <ol>
 * <li><b>IP-based routing</b>: Match by source IP address to specific instance
 * <li><b>Plugin routing</b>: Iterate all plugins and call isTargetAnalyzer()
 * </ol>
 *
 * <p>
 * Generic plugins (GenericASTM, GenericHL7) handle their own DB-driven pattern
 * matching inside isTargetAnalyzer() by querying analyzer_configuration. Legacy
 * plugins use hardcoded identification logic. The router is a thin orchestrator
 * that adds IP-based routing before the standard plugin iteration.
 */
@Service
public class InstanceAwareAnalyzerRouter {

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private AnalyzerService analyzerService;

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

        // Stage 2: Plugin routing (handles both generic and legacy plugins)
        return routeByPlugin(context);
    }

    /**
     * Stage 1: Route by source IP address.
     */
    private RouteResult routeByIp(RouteContext context) {
        if (context.getSourceIp() == null || context.getSourceIp().isEmpty()) {
            return new RouteResult(null, null, null, "IP_NO_MATCH");
        }

        // Find analyzer with matching IP
        Optional<Analyzer> analyzerOpt = analyzerService.getByIpAddress(context.getSourceIp());

        if (analyzerOpt.isPresent()) {
            Analyzer analyzer = analyzerOpt.get();
            if (analyzer.isActive()) {
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
     * Stage 2: Route by plugin's isTargetAnalyzer() method.
     *
     * <p>
     * Iterates all registered plugins (both generic and legacy). Generic plugins
     * handle their own DB-driven pattern matching internally. Legacy plugins use
     * hardcoded identification logic.
     */
    private RouteResult routeByPlugin(RouteContext context) {
        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();

        for (AnalyzerImporterPlugin plugin : plugins) {
            try {
                if (plugin.isTargetAnalyzer(context.getLines())) {
                    Analyzer analyzer = findAnalyzerForPlugin(plugin);
                    if (analyzer != null) {
                        LogEvent.logDebug(this.getClass().getSimpleName(), "routeByPlugin",
                                "Routed by plugin to analyzer: " + analyzer.getName());
                        return new RouteResult(analyzer, analyzer.getAnalyzerType(), plugin, "PLUGIN_MATCH");
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "routeByPlugin",
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
