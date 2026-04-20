package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service interface for processing QC results received via the FHIR import
 * pipeline.
 *
 * <p>
 * When the analyzer bridge tags an Observation with {@code meta.tag code="QC"},
 * the
 * {@link org.openelisglobal.analyzerimport.action.AnalyzerFhirImportController}
 * calls this service to create a {@code QCResult} and trigger Westgard rule
 * evaluation.
 *
 * <p>
 * The control lot is identified by matching the specimen accession number
 * (which for QC samples is typically the lot number or control ID) against
 * {@code QCControlLot.lotNumber} for the given test and instrument.
 *
 * <p>
 * Transaction Boundary: Runs within the same transaction as the FHIR import
 * (REQUIRED propagation) so that both the staging {@code AnalyzerResults} row
 * and the {@code QCResult} row are committed atomically.
 */
public interface QCResultProcessingService {

    /**
     * Process a QC result from a FHIR Observation that was tagged as QC by the
     * bridge.
     *
     * @param analyzerId      Analyzer ID (from X-Analyzer-Id header)
     * @param testId          Mapped OE test ID (from AnalyzerTestNameCache)
     * @param accessionNumber Specimen accession number (used to look up control lot
     *                        by lot number)
     * @param resultValue     Numeric result value
     * @param unit            Unit of measure
     * @param timestamp       Run date/time from Observation.effectiveDateTime
     */
    void processQCResult(String analyzerId, String testId, String accessionNumber, BigDecimal resultValue, String unit,
            LocalDateTime timestamp);
}
