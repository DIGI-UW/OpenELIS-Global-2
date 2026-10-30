package org.openelisglobal.resultvalidation.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.spring.util.SpringContext;

/** Samples and analyses a save finalized, for after-commit listeners. */
public record ResultsValidatedEvent(Set<Long> sampleIds, Set<Long> analysisIds) {

    public static ResultsValidatedEvent finalized(Collection<Analysis> analyses) {
        String finalizedId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        Set<Long> sampleIds = new HashSet<>();
        Set<Long> analysisIds = new HashSet<>();
        for (Analysis analysis : analyses) {
            if (!finalizedId.equals(analysis.getStatusId())) {
                continue;
            }
            if (analysis.getId() != null) {
                analysisIds.add(Long.valueOf(analysis.getId()));
            }
            if (analysis.getSampleItem() != null && analysis.getSampleItem().getSample() != null) {
                sampleIds.add(Long.valueOf(analysis.getSampleItem().getSample().getId()));
            }
        }
        return new ResultsValidatedEvent(sampleIds, analysisIds);
    }
}
