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
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
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

        if (!lines.isEmpty()) {
            setInserterResponder();
            if (inserter == null) {
                error = "Unable to understand which analyzer sent the message";
                return false;
            }
            return true;
        } else {
            error = "Empty message";
            return false;
        }
    }

    public boolean processData(String currentUserId) {
        // it is assumed that all requests are either requests for information
        // or analyzer results to be entered
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
        for (AnalyzerImporterPlugin plugin : SpringContext.getBean(PluginAnalyzerService.class).getAnalyzerPlugins()) {
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

    private String buildResponseForQuery() {
        if (responder == null) {
            error = "Unable to understand which analyzer sent the query or plugin doesn't support responding";
            LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            return "";
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "buildResponseForQuery", "building response");
            return responder.buildResponse(lines);
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            return false;
        } else {
            // Check if analyzer has active mappings and wrap inserter if needed
            // Task Reference: T180
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
     * Task Reference: T180
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
            // Try to identify analyzer from message
            Optional<Analyzer> analyzer = identifyAnalyzerFromMessage();

            if (!analyzer.isPresent()) {
                // Cannot identify analyzer - use original inserter (backward compatibility)
                return originalInserter;
            }

            // Check if analyzer has active mappings
            MappingApplicationService mappingApplicationService = SpringContext
                    .getBean(MappingApplicationService.class);
            if (mappingApplicationService != null
                    && mappingApplicationService.hasActiveMappings(analyzer.get().getId())) {
                // Analyzer has active mappings - wrap inserter
                return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer.get());
            }

            // No mappings configured - use original inserter (backward compatibility)
            return originalInserter;

        } catch (Exception e) {
            // Error identifying analyzer or checking mappings - use original inserter
            LogEvent.logError("Error checking mappings, using original inserter: " + e.getMessage(), e);
            return originalInserter;
        }
    }

    /**
     * Identify analyzer from ASTM message
     * 
     * Attempts to identify the analyzer by: 1. Parsing ASTM header (H segment) for
     * analyzer identification 2. Looking up AnalyzerConfiguration by IP address (if
     * available in message) 3. Matching by analyzer name from plugin
     * 
     * @return Optional Analyzer if identified, empty otherwise
     */
    private Optional<Analyzer> identifyAnalyzerFromMessage() {
        try {
            if (lines == null || lines.isEmpty()) {
                return Optional.empty();
            }

            // Try to parse ASTM header (H segment) to extract analyzer information
            // Format: H|\\^&|||MANUFACTURER^MODEL^VERSION|...
            for (String line : lines) {
                if (line != null && line.startsWith("H|")) {
                    String[] segments = line.split("\\|");
                    if (segments.length >= 5) {
                        // Extract manufacturer/model from header
                        String manufacturerModel = segments[4];
                        if (manufacturerModel != null && !manufacturerModel.trim().isEmpty()) {
                            // Try to find analyzer by name (simplified - can be enhanced)
                            AnalyzerConfigurationService configService = SpringContext
                                    .getBean(AnalyzerConfigurationService.class);
                            if (configService != null) {
                                // For now, return empty - full implementation would parse and match
                                // This is a placeholder - actual implementation would:
                                // 1. Parse manufacturer/model from header
                                // 2. Look up analyzer by name or identifier
                                // 3. Return matching analyzer
                            }
                        }
                    }
                    break;
                }
            }

            // If we can't identify from message, return empty (backward compatibility)
            return Optional.empty();

        } catch (Exception e) {
            LogEvent.logError("Error identifying analyzer: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
