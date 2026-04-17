package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

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
@PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
public interface QCResultProcessingService {

    /**
     * Process a QC result from a FHIR Observation that was tagged as QC by the
     * bridge.
     *
     * @param analyzerId      Analyzer ID (from X-Analyzer-Id header)
     * @param testId          Mapped OE test ID (from AnalyzerTestNameCache)
     * @param accessionNumber Specimen accession number (specimen identifier)
     * @param lotNumber       Canonical {@code qc_control_lot.lot_number} when the
     *                        bridge could extract it (ASTM Q-segment field 3
     *                        component 2). Pass {@code null} when not available —
     *                        resolver falls through to level-based or single-lot
     *                        fallback.
     * @param controlLevel    Clinical level identifier (LPC/HPC/CNEG/CPOS etc.) —
     *                        ASTM Q-segment field 3 component 3, OR the matched
     *                        FILE qcRule SPECIMEN_ID_PREFIX operand. Pass
     *                        {@code null} when not available.
     * @param resultValue     Numeric result value
     * @param unit            Unit of measure
     * @param timestamp       Run date/time from Observation.effectiveDateTime
     */
    void processQCResult(String analyzerId, String testId, String accessionNumber, String lotNumber,
            String controlLevel, BigDecimal resultValue, String unit, LocalDateTime timestamp);
}
