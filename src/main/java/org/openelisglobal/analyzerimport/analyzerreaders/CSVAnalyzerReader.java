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
 * <p>Copyright (C) I-TECH UW. All Rights Reserved.
 *
 * <p>Contributor(s): I-TECH, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * CSV analyzer reader for processing CSV-formatted analyzer results.
 * <p>
 * Handles CSV data from file-based analyzers (e.g., QuantStudio, Hain
 * FluoroCycler) forwarded by the Universal Analyzer Bridge.
 * </p>
 */
public class CSVAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerImporterPlugin plugin;
    private AnalyzerLineInserter inserter;
    private String error;

    /**
     * Read CSV content from input stream.
     *
     * @param stream input stream containing CSV data
     * @return true if reading succeeded, false otherwise
     */
    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Skip empty lines
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }

        } catch (IOException e) {
            error = "Unable to read CSV input stream";
            LogEvent.logError(this.getClass().getSimpleName(), "readStream", e.getMessage());
            LogEvent.logError(e);
            return false;
        }

        if (!lines.isEmpty()) {
            return true;
        } else {
            error = "Empty CSV message";
            LogEvent.logWarn(this.getClass().getSimpleName(), "readStream", error);
            return false;
        }
    }

    /**
     * Insert analyzer data into OpenELIS database.
     *
     * @param systemUserId the system user ID performing the insert
     * @return true if insertion succeeded, false otherwise
     */
    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            setInserter();
        }
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the CSV file";
            LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            return false;
        }

        try {
            boolean success = inserter.insert(lines, systemUserId);
            if (!success) {
                error = inserter.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData",
                        "CSV data insertion failed: " + error);
            }
            return success;
        } catch (Exception e) {
            error = "Exception during CSV data insertion: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            LogEvent.logError(e);
            return false;
        }
    }

    /**
     * Get error message from last operation.
     *
     * @return error message, or null if no error
     */
    @Override
    public String getError() {
        return error;
    }

    /**
     * Identify and set the analyzer plugin based on CSV content.
     * <p>
     * Attempts to match CSV data against registered analyzer plugins.
     * </p>
     */
    private void setInserter() {
        PluginAnalyzerService pluginService;
        try {
            pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        } catch (NullPointerException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "setInserter",
                    "Spring context not available (expected in unit tests)");
            return;
        }

        if (pluginService == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "setInserter", "PluginAnalyzerService is not available");
            return;
        }

        List<AnalyzerImporterPlugin> plugins = pluginService.getAnalyzerPlugins();
        if (plugins == null || plugins.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "setInserter", "No analyzer plugins registered");
            return;
        }

        for (AnalyzerImporterPlugin plugin : plugins) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    this.plugin = plugin;
                    this.inserter = plugin.getAnalyzerLineInserter();
                    LogEvent.logDebug(this.getClass().getSimpleName(), "setInserter",
                            "Matched analyzer plugin: " + plugin.getClass().getSimpleName());
                    return;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "setInserter",
                        "Error checking plugin: " + plugin.getClass().getSimpleName());
                LogEvent.logError(e);
            }
        }

        LogEvent.logWarn(this.getClass().getSimpleName(), "setInserter",
                "No matching analyzer plugin found for CSV data. Lines: " + lines.size());
    }

    /**
     * Validate CSV format has minimum required structure.
     *
     * @param lines CSV lines to validate
     * @return true if CSV appears valid
     */
    public static boolean isValidCSV(List<String> lines) {
        if (lines == null || lines.size() < 2) {
            return false; // Need at least header + 1 data row
        }

        // Check header has at least 2 columns
        String header = lines.get(0);
        if (header == null || header.trim().isEmpty()) {
            return false;
        }

        String[] columns = header.split(",");
        return columns.length >= 2;
    }

    /**
     * Get parsed lines for testing/debugging.
     *
     * @return list of CSV lines
     */
    public List<String> getLines() {
        return lines;
    }
}
