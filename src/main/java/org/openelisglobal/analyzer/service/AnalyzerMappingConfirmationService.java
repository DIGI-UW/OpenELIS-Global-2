package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

public interface AnalyzerMappingConfirmationService {

    AnalyzerMappingConfirmationView confirm(AnalyzerMappingSnapshot candidate, String recognitionFingerprint,
            AnalyzerMappingConfirmationRequest request, String actor);

    AnalyzerMappingConfirmationView getStatus(AnalyzerMappingSnapshot candidate, String recognitionFingerprint);

    /**
     * Checks recorded review of the selected configuration. Current catalog
     * usability is evaluated separately for each incoming observation.
     */
    boolean hasMatchingConfirmation(AnalyzerMappingSnapshot candidate, String recognitionFingerprint);

    AnalyzerMappingVerificationAssessment assessCurrent(AnalyzerMappingSnapshot candidate,
            String recognitionFingerprint);

    Optional<AnalyzerMappingConfirmation> findForMapping(String mappingId);

    /**
     * How the served profile reads compared with the analyzer's confirmation, for a
     * delivery read with the given profile content (null when the Bridge did not
     * say), and how that content read the delivery when it is known. Asks the
     * Bridge only when that content is not the one the readings were taken from; an
     * edit that reads every row alike is remembered.
     */
    AnalyzerConfirmationReading readingOnDelivery(AnalyzerMappingSnapshot binding, String deliveredProfileFingerprint);

    /**
     * Whether a held result's record, as it read on arrival, reads the way the
     * mapping's confirmation does: its parsing, the record itself and every value
     * both know.
     */
    boolean readsAsConfirmed(String mappingId, String code, String subIdentity, String arrivalReading);

    /**
     * Whether the mapping has a confirmation that still stands against the profile
     * the Bridge serves for the pin; false when the Bridge cannot serve it.
     */
    boolean standsAsServed(String mappingId, org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin pin);
}
