package org.openelisglobal.analyzerresults.service;

public interface AnalyzerResultPlacementService {

    /**
     * Resolves an instrument-reported specimen ID to the tube and current analysis
     * a result for {@code testId} would land on. A tube ID wins over an accession.
     */
    AnalyzerResultPlacement place(String instrumentId, String testId);

    /**
     * As {@link #place(String, String)}, and says whether the patient the
     * instrument reported is the order's patient. The instrument's patient is only
     * compared, never used to create or change a patient.
     */
    AnalyzerResultPlacement place(String instrumentId, String testId, String instrumentPatientId,
            String instrumentPatientName);

    /**
     * The accession a result for this instrument ID belongs to: the order of the
     * tube it names, else the ID itself, which may be an accession not yet
     * registered.
     */
    String accessionFor(String instrumentId);
}
