package org.openelisglobal.qc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;

/**
 * OGC-1147 — one row of the QC dashboard's bench listing: the control activity
 * for a test in a lab unit over the selected window.
 *
 * <p>
 * Grouped by lab unit and test rather than by analyzer, because that is how a
 * bench works. The dashboard's existing instrument tiles cannot represent this:
 * they are built from the distinct analyzers found on QC results, and a manual
 * or RDT control has none.
 *
 * <p>
 * {@code lastRun} carries an explicit format because the shared ObjectMapper
 * leaves WRITE_DATES_AS_TIMESTAMPS on, which would serialise it as an array.
 */
public record BenchQcSummaryRow(String testSectionId, String testSectionName, String testId, String testName,
        String source, long totalRuns, long failedRuns,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") Timestamp lastRun) {
}
