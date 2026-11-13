package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;

/**
 * Which analyses on a program order a case release owns, and when the order is
 * done.
 *
 * <p>
 * A pathology case referred to immunohistochemistry shares its order with the
 * referral: the referral adds its tests to the order in the
 * Immunohistochemistry section. Each case finalizes only its own tests on
 * release, so neither release reports the other's work.
 */
public final class ProgramCaseAnalyses {

    /** The lab unit a case's referral to immunohistochemistry is tested in. */
    static final String IMMUNOHISTOCHEMISTRY_SECTION = "Immunohistochemistry";

    private ProgramCaseAnalyses() {
    }

    static boolean isImmunohistochemistryReferral(Analysis analysis) {
        return analysis.getTestSection() != null
                && IMMUNOHISTOCHEMISTRY_SECTION.equals(analysis.getTestSection().getTestSectionName());
    }

    /**
     * The order is finished once none of its tests is still to be reported, the
     * same rule result validation applies.
     */
    public static boolean allFinished(List<Analysis> analyses, IStatusService statusService) {
        return analyses.stream()
                .allMatch(analysis -> statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                        || statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)
                        || statusService.matches(analysis.getStatusId(), AnalysisStatus.NonConforming_depricated));
    }
}
