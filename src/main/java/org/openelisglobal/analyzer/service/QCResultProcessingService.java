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

    /** What became of a control that reported an answer. */
    enum Outcome {
        /** Judged against its QC target and recorded PASS or FAIL. */
        RECORDED,
        /** No usable control lot matched; nothing is recorded. */
        NO_LOT,
        /** The test and level have no expected answer to judge against. */
        NO_TARGET
    }

    /**
     * Judge a control that reported an answer rather than a number against the Test
     * Catalog QC target of its lot's level (the lot's own override first), and
     * record PASS when the answer is the expected one, FAIL otherwise. Nothing is
     * guessed: with no lot or no expected answer nothing is recorded.
     *
     * @param componentId        the component the answer belongs to; null for the
     *                           test's primary result
     * @param answerDictionaryId the dictionary entry the analyzer's mapping turned
     *                           the reported value into
     */
    Outcome processQualitativeQCResult(String analyzerId, String testId, String componentId, String accessionNumber,
            String lotNumber, String controlLevel, String answerDictionaryId, LocalDateTime timestamp);
}
