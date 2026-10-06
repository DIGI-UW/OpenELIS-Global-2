package org.openelisglobal.analyzer.service;

import java.util.List;

/** Rule 7: moving an analyzer to a newer revision of its profile. */
public interface AnalyzerAdoptionService {

    /**
     * What adopting {@code toRevision} does to each record of the analyzer's
     * mapping.
     */
    record AdoptionPlan(String analyzerId, String profileId, int fromRevision, int toRevision,
            List<AnalyzerMappingAdoption.Row> rows) {
    }

    /**
     * Buckets the analyzer's newest mapping against a newer revision of the same
     * profile. A held result counts against a dropped code only when it arrived
     * under the revision being left.
     */
    AdoptionPlan prepareAdoption(String analyzerId, int toRevision);
}
