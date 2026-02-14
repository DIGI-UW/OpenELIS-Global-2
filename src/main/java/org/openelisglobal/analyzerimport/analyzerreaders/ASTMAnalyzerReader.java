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
        List<AnalyzerImporterPlugin> plugins = choosePluginOrder(pluginService);
        for (AnalyzerImporterPlugin plugin : plugins) {
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
     * Return the plugin list in default order. (preferGenericPlugin flag has been
     * removed.)
     */
    private List<AnalyzerImporterPlugin> choosePluginOrder(PluginAnalyzerService pluginService) {
        return pluginService.getAnalyzerPlugins();
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
                return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer.get());
            }

            return originalInserter;

        } catch (Exception e) {
            // Error identifying analyzer or checking mappings - use original inserter
            LogEvent.logError("Error checking mappings, using original inserter: " + e.getMessage(), e);
            return originalInserter;
        }
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
     * Identify analyzer from ASTM message
     * 
     * Attempts to identify the analyzer by: 1. Parsing ASTM header (H segment) for
     * analyzer identification 2. Looking up Analyzer by IP address (if available)
     * 3. Matching by analyzer name from plugin
     * 
     * @return Optional Analyzer if identified, empty otherwise
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

            // Strategy 2: Client IP address (for direct HTTP push)
            if (clientIpAddress != null && !clientIpAddress.trim().isEmpty()) {
                Optional<Analyzer> analyzerOpt = analyzerService.getByIpAddress(clientIpAddress.trim());
                if (analyzerOpt.isPresent()) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "identifyAnalyzerFromMessage",
                            "Identified analyzer from IP address: " + clientIpAddress);
                    return analyzerOpt;
                }
            }

            // Strategy 3: Plugin fallback not available
            // Note: AnalyzerImporterPlugin interface doesn't provide getAnalyzerName()
            // method
            // Identification relies on Strategies 1 (ASTM header) and 2 (IP address)
            // If both fail, analyzer cannot be identified from message alone

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
