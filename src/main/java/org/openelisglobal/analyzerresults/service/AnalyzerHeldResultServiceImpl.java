package org.openelisglobal.analyzerresults.service;

import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerHeldResultServiceImpl implements AnalyzerHeldResultService {

    private final AnalyzerResultsService analyzerResultsService;

    public AnalyzerHeldResultServiceImpl(AnalyzerResultsService analyzerResultsService) {
        this.analyzerResultsService = analyzerResultsService;
    }

    @Override
    @Transactional
    public void dismiss(String stagedResultId, String actor) {
        AnalyzerResults held = analyzerResultsService.get(stagedResultId);
        // Held for its mapping or for placement: either way it carries the reason.
        if (held.getImportIssueReason() == null) {
            throw new IllegalStateException("Only a held result can be dismissed");
        }
        analyzerResultsService.delete(held.getId(), actor);
    }
}
