package org.openelisglobal.validationpolicy.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease;

/**
 * OGC-1017: automatic releases are recorded, not inferred. A result that result
 * entry releases without a validator gets a record of who saved it and the rule
 * that released it, as it stood at that moment.
 */
public interface AnalysisAutoReleaseService {

    /**
     * Records each analysis as released at entry by the user, under the rule in
     * force for its lab unit now. Joins the save's transaction.
     */
    void record(Collection<Analysis> analyses, String sysUserId);

    /** Every automatic release of the analysis, oldest first. */
    List<AnalysisAutoRelease> releasesOf(String analysisId);

    /** The latest automatic release of each analysis that has one, by id. */
    Map<String, AnalysisAutoRelease> latestOf(Collection<String> analysisIds);

    /**
     * Whether the analysis's current release is the recorded automatic one: no
     * validator released it after that record.
     */
    boolean isCurrent(AnalysisAutoRelease release, Analysis analysis);

    /** The rule that released it, as a person reads it. */
    String describe(AnalysisAutoRelease release);
}
