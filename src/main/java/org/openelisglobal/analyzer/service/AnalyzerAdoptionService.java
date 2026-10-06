package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;

/** Rule 7: moving an analyzer to a newer revision of its profile. */
public interface AnalyzerAdoptionService {

    /**
     * What adopting {@code toRevision} does to each record of the analyzer's
     * mapping.
     */
    record AdoptionPlan(String analyzerId, String profileId, int fromRevision, int toRevision,
            List<AnalyzerMappingAdoption.Row> rows) {

        /** What adoption saves if the operator changes nothing. */
        public AnalyzerMappingDraft proposals() {
            List<AnalyzerMappingTestDraft> tests = new ArrayList<>();
            List<AnalyzerMappingResultDraft> results = new ArrayList<>();
            rows.stream().filter(row -> row.proposed() != null).forEach(row -> {
                tests.add(row.proposed().test());
                results.addAll(row.proposed().results());
            });
            return new AnalyzerMappingDraft(tests, results);
        }
    }

    /**
     * Buckets the analyzer's newest mapping against a newer revision of the same
     * profile. A held result counts against a dropped code only when it arrived
     * under the revision being left.
     */
    AdoptionPlan prepareAdoption(String analyzerId, int toRevision);

    /**
     * Saves the reviewed decisions as the analyzer's next mapping revision on
     * {@code toRevision}. Every record the revision keeps needs a decision; nothing
     * may still be blocked. A decision equal to the plan's proposal keeps its
     * origin, and one the operator changed is an override. The existing Confirm and
     * Apply then put it in force.
     */
    AnalyzerMappingSnapshot adopt(String analyzerId, int toRevision, AnalyzerMappingDraft decisions, String actor);
}
