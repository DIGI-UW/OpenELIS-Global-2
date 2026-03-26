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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.HL7MessageService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * AnalyzerReader for HL7 v2.x ORU^R01 result messages.
 *
 * <p>
 * HL7 handling is plugin-only (e.g. GenericHL7). Parses message with
 * HL7MessageService for validation and segment lines, then delegates to the
 * matching plugin's inserter. No built-in/legacy HL7 inserter fallback.
 */
public class HL7AnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private String error;
    /**
     * HL7 analyzer identifier from the bridge X-Analyzer-Id header (e.g., a
     * composite of MSH-3 + MSH-4 like "MINDRAY-BC-5380"). This is an external
     * identifier resolved via identifier-pattern matching — NOT an OpenELIS
     * analyzer database primary key.
     */
    private String bridgeAnalyzerIdentifier;
    private String clientIpAddress;
    private Integer clientPort;

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        lines = null;
        try {
            String raw = IOUtils.toString(stream, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(raw)) {
                error = "Empty HL7 message";
                return false;
            }
            HL7MessageService svc = SpringContext.getBean(HL7MessageService.class);
            svc.parseOruR01(raw);
            lines = svc.toSegmentLines(raw);
            return !lines.isEmpty();
        } catch (HL7MessageService.HL7ParseException e) {
            error = "HL7 parse error: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        } catch (Exception e) {
            error = "Failed to read HL7 stream: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (lines == null || lines.isEmpty()) {
            error = "No HL7 message loaded";
            return false;
        }

        // Try deterministic identification via bridge headers before plugin matching
        Optional<Analyzer> analyzer = identifyAnalyzerFromHeaders();

        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        List<AnalyzerImporterPlugin> plugins = choosePluginOrder(pluginService);

        boolean pluginMatched = false;
        for (AnalyzerImporterPlugin plugin : plugins) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    pluginMatched = true;
                    AnalyzerLineInserter inserter = plugin.getAnalyzerLineInserter();
                    if (inserter != null) {
                        // Inject analyzer context ID if identified from headers
                        if (analyzer.isPresent()) {
                            inserter.setContextAnalyzerId(analyzer.get().getId());
                        }
                        boolean success = inserter.insert(lines, systemUserId);
                        if (!success) {
                            error = inserter.getError();
                            LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
                        }
                        return success;
                    }
                }
            } catch (RuntimeException e) {
                pluginMatched = true;
                error = "Plugin " + plugin.getClass().getSimpleName() + " matched but failed: " + e.getMessage();
                LogEvent.logError(error, e);
            }
        }

        if (!pluginMatched) {
            error = "No HL7 plugin matched this message (e.g. configure GenericHL7 with matching identifier pattern)";
        }
        LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
        return false;
    }

    /**
     * Return the plugin list in default order. (preferGenericPlugin flag has been
     * removed.)
     */
    private List<AnalyzerImporterPlugin> choosePluginOrder(PluginAnalyzerService pluginService) {
        return pluginService.getAnalyzerPlugins();
    }

    /**
     * Set analyzer identifier from bridge X-Analyzer-Id header. For HL7, the bridge
     * sends a composite identifier derived from MSH-3 + MSH-4 (e.g.,
     * "MINDRAY-BC-5380"), which is resolved via
     * {@code findByIdentifierPatternMatch} — not a direct DB lookup.
     */
    public void setRegisteredAnalyzerId(String analyzerId) {
        this.bridgeAnalyzerIdentifier = analyzerId;
    }

    /**
     * Set client IP from bridge X-Source-Id header. Used by
     * {@link #identifyAnalyzerFromHeaders()} for deterministic analyzer lookup.
     */
    public void setClientIpAddress(String ip) {
        this.clientIpAddress = ip;
    }

    /**
     * Set client port from bridge X-Source-Port header. Used by
     * {@link #identifyAnalyzerFromHeaders()} for deterministic analyzer lookup.
     */
    public void setClientPort(Integer port) {
        this.clientPort = port;
    }

    /**
     * Identify analyzer from bridge headers using tiered strategy:
     * <ol>
     * <li>Bridge X-Analyzer-Id (identifier-pattern match against MSH-3+MSH-4
     * composite)</li>
     * <li>IP+port exact match (from X-Source-Id + X-Source-Port)</li>
     * <li>IP-only match (from X-Source-Id)</li>
     * </ol>
     * Falls back to empty if no headers set or no match found.
     */
    private Optional<Analyzer> identifyAnalyzerFromHeaders() {
        try {
            AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
            if (analyzerService == null) {
                return Optional.empty();
            }

            // Strategy 0: Bridge X-Analyzer-Id (highest priority).
            // The bridge sends a composite identifier like "MINDRAY-BC-5380" (from
            // MSH-3+MSH-4),
            // not a numeric DB ID. Use pattern matching to resolve it.
            if (bridgeAnalyzerIdentifier != null && !bridgeAnalyzerIdentifier.trim().isEmpty()) {
                Optional<Analyzer> match = analyzerService
                        .findByIdentifierPatternMatch(bridgeAnalyzerIdentifier.trim());
                if (match.isPresent()) {
                    LogEvent.logInfo(getClass().getSimpleName(), "identifyAnalyzerFromHeaders",
                            "Identified from bridge X-Analyzer-Id: " + match.get().getName());
                    return match;
                }
                LogEvent.logWarn(getClass().getSimpleName(), "identifyAnalyzerFromHeaders",
                        "No analyzer matched X-Analyzer-Id '" + bridgeAnalyzerIdentifier
                                + "' using identifier patterns — falling back to IP-based lookup");
            }

            // Strategy 1: Exact IP+port lookup
            if (clientIpAddress != null && !clientIpAddress.trim().isEmpty() && clientPort != null) {
                Optional<Analyzer> match = analyzerService.getByIpAddressAndPort(clientIpAddress.trim(), clientPort);
                if (match.isPresent()) {
                    LogEvent.logDebug(getClass().getSimpleName(), "identifyAnalyzerFromHeaders",
                            "Identified analyzer from IP+port: " + clientIpAddress + ":" + clientPort);
                    return match;
                }
            }

            // Strategy 1: IP-only lookup
            if (clientIpAddress != null && !clientIpAddress.trim().isEmpty()) {
                Optional<Analyzer> match = analyzerService.getByIpAddress(clientIpAddress.trim());
                if (match.isPresent()) {
                    LogEvent.logDebug(getClass().getSimpleName(), "identifyAnalyzerFromHeaders",
                            "Identified analyzer from IP: " + clientIpAddress);
                    return match;
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            LogEvent.logError("Error identifying HL7 analyzer from headers: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
