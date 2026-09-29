package org.openelisglobal.resultvalidation.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Published for the samples whose analyses a save finalized, by validation or
 * by result entry for a result that needs none, so work that waits on validated
 * results can start once the save commits.
 */
public record ResultsValidatedEvent(Set<Long> sampleIds) {

    /** The samples of the analyses in this batch that are now finalized. */
    public static Set<Long> finalizedSamples(Collection<Analysis> analyses) {
        String finalizedId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        Set<Long> sampleIds = new HashSet<>();
        for (Analysis analysis : analyses) {
            if (finalizedId.equals(analysis.getStatusId()) && analysis.getSampleItem() != null
                    && analysis.getSampleItem().getSample() != null) {
                sampleIds.add(Long.valueOf(analysis.getSampleItem().getSample().getId()));
            }
        }
        return sampleIds;
    }
}
