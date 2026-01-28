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
package org.openelisglobal.analyzer.horiba;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Line inserter for Horiba ABX Micros 60 analyzer ASTM messages.
 *
 * <p>
 * Feature: 011-madagascar-analyzer-integration Milestone: M10 (Micros 60)
 *
 * <p>
 * Parses ASTM LIS2-A2 messages from the Micros 60 and creates AnalyzerResults
 * for persistence. Supports the 18-parameter CBC with 3-part differential.
 *
 * <p>
 * ASTM Segment parsing:
 * <ul>
 * <li>H - Header: Extract analyzer identification and timestamp</li>
 * <li>P - Patient: Extract patient ID</li>
 * <li>O - Order: Extract sample/accession number</li>
 * <li>R - Result: Extract test code, value, units, reference range</li>
 * <li>L - Terminator: End of message</li>
 * </ul>
 *
 * <p>
 * Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
 */
public class HoribaMicros60AnalyzerLineInserter extends AnalyzerLineInserter {

    /** Date format used in ASTM messages: YYYYMMDDHHmmss */
    private static final String ASTM_DATE_FORMAT = "yyyyMMddHHmmss";

    /** ASTM field delimiter */
    private static final String FIELD_DELIMITER = "\\|";

    /** ASTM component delimiter */
    private static final String COMPONENT_DELIMITER = "\\^";

    /** Analyzer ID cached for performance */
    private String analyzerId;

    /** Error message from last insert operation */
    private String error;

    /**
     * Utility for creating analyzer results (lazy initialized to avoid Spring
     * context issues)
     */
    private AnalyzerReaderUtil readerUtil;

    /**
     * Get reader utility (lazy initialization).
     */
    private AnalyzerReaderUtil getReaderUtil() {
        if (readerUtil == null) {
            readerUtil = new AnalyzerReaderUtil();
        }
        return readerUtil;
    }

    /**
     * Insert analyzer results from ASTM message lines.
     *
     * @param lines         ASTM message lines
     * @param currentUserId User ID for audit
     * @return true if insert succeeded
     */
    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;

        if (lines == null || lines.isEmpty()) {
            error = "Empty message lines";
            return false;
        }

        try {
            // Get analyzer ID
            analyzerId = getAnalyzerId();
            if (analyzerId == null) {
                error = "Analyzer not found in database: " + HoribaMicros60Analyzer.ANALYZER_NAME;
                return false;
            }

            // Parse ASTM message
            String accessionNumber = extractAccessionNumber(lines);
            if (accessionNumber == null || accessionNumber.isEmpty()) {
                error = "Unable to extract accession number from ASTM message";
                return false;
            }

            // Extract results from R segments
            List<AnalyzerResults> results = new ArrayList<>();
            parseResultSegments(lines, accessionNumber, results);

            if (results.isEmpty()) {
                error = "No results found in ASTM message";
                return false;
            }

            // Persist results
            return persistImport(currentUserId, results);

        } catch (Exception e) {
            error = "Error processing Micros 60 ASTM message: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    /**
     * Get the analyzer ID from the database.
     *
     * @return Analyzer ID or null if not found
     */
    private String getAnalyzerId() {
        if (analyzerId == null) {
            AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
            Analyzer analyzer = analyzerService.getAnalyzerByName(HoribaMicros60Analyzer.ANALYZER_NAME);
            if (analyzer != null) {
                analyzerId = analyzer.getId();
            }
        }
        return analyzerId;
    }

    /**
     * Extract accession/sample number from O (Order) segment.
     *
     * <p>
     * Format: O|seq|sampleId^lab|testCode||timestamp
     *
     * @param lines ASTM message lines
     * @return Accession number or null
     */
    private String extractAccessionNumber(List<String> lines) {
        for (String line : lines) {
            if (line != null && line.startsWith("O|")) {
                String[] fields = line.split(FIELD_DELIMITER);
                if (fields.length >= 3 && fields[2] != null) {
                    // Sample ID is in field 2 (0-indexed), may have component separator
                    String sampleField = fields[2];
                    String[] components = sampleField.split(COMPONENT_DELIMITER);
                    if (components.length > 0 && !components[0].isEmpty()) {
                        return components[0].trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parse R (Result) segments and create AnalyzerResults.
     *
     * <p>
     * Format:
     * R|seq|^^^testCode^testName|value|units|refRange|flag||status|timestamp
     *
     * @param lines           ASTM message lines
     * @param accessionNumber Sample accession number
     * @param results         List to add results to
     */
    private void parseResultSegments(List<String> lines, String accessionNumber, List<AnalyzerResults> results) {
        for (String line : lines) {
            if (line != null && line.startsWith("R|")) {
                try {
                    AnalyzerResults result = parseResultLine(line, accessionNumber);
                    if (result != null) {
                        addValueToResults(results, result);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "parseResultSegments",
                            "Error parsing result line: " + line + " - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parse a single R (Result) line.
     *
     * @param line            ASTM R segment line
     * @param accessionNumber Sample accession number
     * @return AnalyzerResults or null if parse failed
     */
    private AnalyzerResults parseResultLine(String line, String accessionNumber) {
        String[] fields = line.split(FIELD_DELIMITER);

        // R|seq|^^^testCode^testName|value|units|refRange|flag||status|timestamp
        // Fields: 0=R, 1=seq, 2=testCode, 3=value, 4=units, 5=refRange, 6=flag,
        // 7=empty, 8=status, 9=timestamp

        if (fields.length < 4) {
            return null;
        }

        // Extract test code from field 2 (^^^testCode^testName)
        String testCodeField = fields[2];
        String testCode = extractTestCode(testCodeField);
        if (testCode == null || testCode.isEmpty()) {
            return null;
        }

        // Extract value from field 3
        String value = fields[3].trim();
        if (value.isEmpty()) {
            return null;
        }

        // Extract units from field 4 (optional)
        String units = fields.length > 4 ? fields[4].trim() : "";

        // Extract timestamp from field 9 (optional)
        Timestamp timestamp = null;
        if (fields.length > 9 && !fields[9].isEmpty()) {
            timestamp = parseAstmTimestamp(fields[9].trim());
        }
        if (timestamp == null) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }

        // Create AnalyzerResults
        AnalyzerResults result = new AnalyzerResults();
        result.setAnalyzerId(analyzerId);
        result.setAccessionNumber(accessionNumber);
        result.setTestName(testCode); // Will be mapped by AnalyzerTestMapping
        result.setResult(value);
        result.setUnits(units);
        result.setCompleteDate(timestamp);
        result.setIsControl(isControlSample(accessionNumber));

        return result;
    }

    /**
     * Extract test code from ASTM test code field.
     *
     * <p>
     * Format: ^^^testCode^testName or ^^^testCode
     *
     * @param testCodeField ASTM field content
     * @return Test code or null
     */
    private String extractTestCode(String testCodeField) {
        if (testCodeField == null || testCodeField.isEmpty()) {
            return null;
        }

        String[] components = testCodeField.split(COMPONENT_DELIMITER);
        // Test code is typically in component 4 (0-indexed: 3)
        // Format: ^^^testCode^testName
        if (components.length >= 4 && !components[3].isEmpty()) {
            return components[3].trim();
        }
        // Fallback: last non-empty component
        for (int i = components.length - 1; i >= 0; i--) {
            if (!components[i].isEmpty()) {
                return components[i].trim();
            }
        }
        return null;
    }

    /**
     * Parse ASTM timestamp string to Timestamp.
     *
     * @param timestampStr ASTM timestamp (YYYYMMDDHHmmss)
     * @return Timestamp or null if parse failed
     */
    private Timestamp parseAstmTimestamp(String timestampStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(ASTM_DATE_FORMAT);
            Date date = sdf.parse(timestampStr);
            return new Timestamp(date.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Check if sample is a QC control sample.
     *
     * @param accessionNumber Sample accession number
     * @return true if control sample
     */
    private boolean isControlSample(String accessionNumber) {
        if (accessionNumber == null) {
            return false;
        }
        String upper = accessionNumber.toUpperCase();
        return upper.contains("CONTROL") || upper.contains("QC") || upper.startsWith("CTL");
    }

    /**
     * Add result to list, including any existing result from DB for update.
     *
     * @param results Result list
     * @param result  New result to add
     */
    private void addValueToResults(List<AnalyzerResults> results, AnalyzerResults result) {
        results.add(result);

        AnalyzerResults resultFromDB = getReaderUtil().createAnalyzerResultFromDB(result);
        if (resultFromDB != null) {
            results.add(resultFromDB);
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
