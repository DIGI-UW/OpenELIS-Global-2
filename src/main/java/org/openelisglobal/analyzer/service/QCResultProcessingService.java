package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Processes a Bridge-recognized control result in OpenELIS operational QC. */
public interface QCResultProcessingService {

    /**
     * Process a normalized result that the pinned Bridge profile classified as a
     * control and OpenELIS mapped to a local Test.
     *
     * @param analyzerId      OpenELIS analyzer ID resolved from the Bridge
     *                        connection
     * @param testId          local Test selected by the current site binding
     * @param accessionNumber Specimen accession number (specimen identifier)
     * @param lotNumber       Canonical {@code qc_control_lot.lot_number} when the
     *                        Bridge extracted from the analyzer message; may be
     *                        null
     * @param controlLevel    clinical control level extracted by Bridge; may be
     *                        null
     * @param resultValue     Numeric result value
     * @param unit            Unit of measure
     * @param timestamp       Run date/time from Observation.effectiveDateTime
     */
    void processQCResult(String analyzerId, String testId, String accessionNumber, String lotNumber,
            String controlLevel, BigDecimal resultValue, String unit, LocalDateTime timestamp);
}
