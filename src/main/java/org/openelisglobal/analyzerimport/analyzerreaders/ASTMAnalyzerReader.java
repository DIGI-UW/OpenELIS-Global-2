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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import com.ibm.icu.text.CharsetDetector;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.MappingApplicationService;
import org.openelisglobal.analyzer.service.MappingAwareAnalyzerLineInserter;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

public class ASTMAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerImporterPlugin plugin;
    private AnalyzerLineInserter inserter;
    private AnalyzerResponder responder;
    private String error;
    private boolean hasResponse = false;
    private String responseBody;
    private String clientIpAddress;
    private Integer clientPort;
    private String registeredAnalyzerId;

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();
        BufferedInputStream bis = new BufferedInputStream(stream);
        CharsetDetector detector = new CharsetDetector();
        try {
            detector.setText(bis);
            String charsetName = detector.detect().getName();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis, charsetName));

            try {
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    lines.add(line);
                }
            } catch (IOException e) {
                error = "Unable to read input stream";
                LogEvent.logError(e);
                return false;
            }
        } catch (IOException e) {
            error = "Unable to determine message encoding";
            LogEvent.logError("an error occured detecting the encoding of the analyzer message", e);
            return false;
        }

        if (lines.isEmpty()) {
            error = "Empty message";
            return false;
        }
        return true;
    }

    /**
     * Resolve plugin/inserter/responder from message lines. Call before processData
     * or insertAnalyzerData so that "no plugin matched" is reported at process time
     * (HL7-aligned).
     */
    private void ensureInserterResponder() {
        if (plugin != null) {
            return;
        }
        setInserterResponder();
    }

    public boolean processData(String currentUserId) {
        error = null;
        ensureInserterResponder();
        if (plugin == null) {
            error = "No ASTM plugin matched this message (e.g. configure GenericASTM with matching identifier pattern)";
            LogEvent.logError(getClass().getSimpleName(), "processData", error);
            return false;
        }
        if (plugin.isAnalyzerResult(lines)) {
            return insertAnalyzerData(currentUserId);
        } else {
            responseBody = buildResponseForQuery();
            hasResponse = true;
            return true;
        }
    }

    public boolean hasResponse() {
        return hasResponse;
    }

    public String getResponse() {
        return responseBody;
    }

    private void setInserterResponder() {
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);

        // Database-configured analyzers are authoritative. If the database
        // identifies this message (via IP+port, name, or identifier_pattern),
        // use GenericASTM directly — no plugin loop, no ambiguity.
        Optional<Analyzer> dbAnalyzer = identifyAnalyzerFromMessage();
        if (dbAnalyzer.isPresent()) {
            for (AnalyzerImporterPlugin p : pluginService.getAnalyzerPlugins()) {
                if (p.isGenericPlugin() && p.isTargetAnalyzer(lines)) {
                    this.plugin = p;
                    inserter = p.getAnalyzerLineInserter();
                    responder = p.getAnalyzerResponder();
                    LogEvent.logInfo(getClass().getSimpleName(), "setInserterResponder", "Database analyzer matched: "
                            + dbAnalyzer.get().getName() + " — routed to plugin: " + p.getClass().getSimpleName());
                    return;
                }
            }
            // DB identified the analyzer but no generic plugin could handle it.
            // Do NOT fall through to legacy — the DB match is authoritative.
            LogEvent.logError(getClass().getSimpleName(), "setInserterResponder",
                    "Database identified analyzer '" + dbAnalyzer.get().getName()
                            + "' but no GenericASTM plugin matched. Check plugin JARs are loaded.");
            return;
        }

        // Fallback: legacy plugin loop (for analyzers not yet configured in DB)
        for (AnalyzerImporterPlugin plugin : pluginService.getAnalyzerPlugins()) {
            if (plugin.isTargetAnalyzer(lines)) {
                try {
                    this.plugin = plugin;
                    inserter = plugin.getAnalyzerLineInserter();
                    responder = plugin.getAnalyzerResponder();
                    return;
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    /**
     * ASTM H-segment field 4 (manufacturer^model^version). Same as
     * GenericASTM.parseAnalyzerIdentifier.
     */
    private String parseIdentifierFromAstmHeader() {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        for (String line : lines) {
            if (line != null && line.startsWith("H|")) {
                String[] fields = line.split("\\|");
                if (fields.length > 4 && fields[4] != null && !fields[4].trim().isEmpty()) {
                    return fields[4].trim();
                }
                break;
            }
        }
        return null;
    }

    private String buildResponseForQuery() {
        if (responder == null) {
            error = "No ASTM plugin matched this message or plugin doesn't support responding (e.g. configure GenericASTM with matching identifier pattern)";
            LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            return "";
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "buildResponseForQuery", "building response");
            return responder.buildResponse(lines);
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        ensureInserterResponder();
        if (inserter == null) {
            error = "No ASTM plugin matched this message (e.g. configure GenericASTM with matching identifier pattern)";
            LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            return false;
        } else {
            AnalyzerLineInserter finalInserter = wrapInserterIfMappingsExist(inserter);

            boolean success = finalInserter.insert(lines, systemUserId);
            if (!success) {
                error = finalInserter.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            }
            return success;
        }
    }

    /**
     * Wrap inserter with MappingAwareAnalyzerLineInserter if analyzer has active
     * mappings
     * 
     * 
     * Per research.md Section 7: Conditional wrapping logic - Check if analyzer has
     * active mappings before wrapping - If analyzer has active mappings: Wrap
     * plugin inserter with MappingAwareAnalyzerLineInserter - If analyzer has no
     * mappings: Use original plugin inserter directly (backward compatibility)
     * 
     * @param originalInserter The original plugin inserter
     * @return Wrapped inserter if mappings exist, original inserter otherwise
     */
    private AnalyzerLineInserter wrapInserterIfMappingsExist(AnalyzerLineInserter originalInserter) {
        try {
            Optional<Analyzer> analyzer = identifyAnalyzerFromMessage();

            if (!analyzer.isPresent()) {
                return originalInserter;
            }

            MappingApplicationService mappingApplicationService = SpringContext
                    .getBean(MappingApplicationService.class);
            if (mappingApplicationService != null
                    && mappingApplicationService.hasActiveMappings(analyzer.get().getId())) {
                // MappingAwareAnalyzerLineInserter constructor injects context ID
                return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer.get());
            }

            // No mappings but analyzer identified — inject context ID for result stamping
            originalInserter.setContextAnalyzerId(analyzer.get().getId());
            return originalInserter;

        } catch (Exception e) {
            // Error identifying analyzer or checking mappings - use original inserter
            LogEvent.logError("Error checking mappings, using original inserter: " + e.getMessage(), e);
            return originalInserter;
        }
    }

    /**
     * Set registered analyzer ID from bridge X-Analyzer-Id header. When set, this
     * takes highest priority in identification — direct DB lookup, no pattern
     * matching.
     */
    public void setRegisteredAnalyzerId(String analyzerId) {
        this.registeredAnalyzerId = analyzerId;
    }

    /**
     * Set client IP address for analyzer identification
     *
     * @param ip The client IP address
     */
    public void setClientIpAddress(String ip) {
        this.clientIpAddress = ip;
    }

    /**
     * Set client port from bridge X-Source-Port header for analyzer identification
     */
    public void setClientPort(Integer port) {
        this.clientPort = port;
    }

    /**
     * Identify analyzer from ASTM message using a tiered strategy:
     * <ol>
     * <li>Strategy 0: Exact IP+port from bridge headers (deterministic)</li>
     * <li>Strategy 1: ASTM H-segment name → getByName()</li>
     * <li>Strategy 2: Client IP only → getByIpAddress()</li>
     * <li>Strategy 3: Identifier pattern regex → fallback only</li>
     * </ol>
     */
    private Optional<Analyzer> identifyAnalyzerFromMessage() {
        try {
            if (lines == null || lines.isEmpty()) {
                return Optional.empty();
            }

            AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);

            if (analyzerService == null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                        "AnalyzerService not available for analyzer identification");
                return Optional.empty();
            }

            // Strategy -1: Bridge-registered analyzer ID (highest priority, deterministic)
            // Set from X-Analyzer-Id header — the bridge looked up the source in its
            // registry and resolved the OE analyzer ID before forwarding.
            if (registeredAnalyzerId != null && !registeredAnalyzerId.trim().isEmpty()) {
                Analyzer analyzer = analyzerService.get(registeredAnalyzerId.trim());
                if (analyzer != null) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from bridge registration (X-Analyzer-Id): " + analyzer.getName());
                    return Optional.of(analyzer);
                }
                LogEvent.logWarn(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage", "X-Analyzer-Id '"
                        + registeredAnalyzerId + "' not found in database — falling back to other strategies");
            }

            // Strategy 0: Exact IP+port lookup from bridge headers (deterministic)
            if (clientIpAddress != null && !clientIpAddress.trim().isEmpty() && clientPort != null) {
                Optional<Analyzer> analyzerOpt = analyzerService.getByIpAddressAndPort(clientIpAddress.trim(),
                        clientPort);
                if (analyzerOpt.isPresent()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from IP+port: " + clientIpAddress + ":" + clientPort);
                    return analyzerOpt;
                }
            }

            // Strategy 1: Parse ASTM H-segment for manufacturer/model
            String analyzerName = parseAnalyzerNameFromHeader();
            if (analyzerName != null && !analyzerName.trim().isEmpty()) {
                Optional<Analyzer> analyzerOpt = analyzerService.getByName(analyzerName.trim());
                if (analyzerOpt.isPresent()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from header: " + analyzerName);
                    return analyzerOpt;
                }
            }

            // Strategy 2: Client IP address only (from bridge X-Source-Id header)
            if (clientIpAddress != null && !clientIpAddress.trim().isEmpty()) {
                Optional<Analyzer> analyzerOpt = analyzerService.getByIpAddress(clientIpAddress.trim());
                if (analyzerOpt.isPresent()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from IP address: " + clientIpAddress);
                    return analyzerOpt;
                }
            }

            // Strategy 3: Identifier pattern match (regex fallback)
            String identifier = parseIdentifierFromAstmHeader();
            if (identifier != null && !identifier.trim().isEmpty()) {
                Optional<Analyzer> analyzerOpt = analyzerService.findByIdentifierPatternMatch(identifier.trim());
                if (analyzerOpt.isPresent()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from identifier pattern: " + identifier);
                    return analyzerOpt;
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            LogEvent.logError("Error identifying analyzer: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Parse analyzer name from ASTM H-segment header
     * 
     * Format: H|\\^&|||MANUFACTURER^MODEL^VERSION|...
     * 
     * @return Analyzer name as "MANUFACTURER MODEL" or null if not found
     */
    private String parseAnalyzerNameFromHeader() {
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        for (String line : lines) {
            if (line != null && line.startsWith("H|")) {
                String[] segments = line.split("\\|");
                if (segments.length >= 5 && segments[4] != null) {
                    String manufacturerModel = segments[4].trim();
                    if (!manufacturerModel.isEmpty()) {
                        String[] parts = manufacturerModel.split("\\^");
                        if (parts.length >= 2) {
                            return parts[0].trim() + " " + parts[1].trim();
                        } else if (parts.length == 1) {
                            return parts[0].trim();
                        }
                    }
                }
                break;
            }
        }
        return null;
    }

    @Override
    public String getError() {
        return error;
    }
}
