package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroIncomingReflexPolicy {
    private final MicroCaseAnalysisDAO links;
    private final MicroIncomingItemDAO incoming;

    public MicroIncomingReflexPolicy(MicroCaseAnalysisDAO links, MicroIncomingItemDAO incoming) {
        this.links = links;
        this.incoming = incoming;
    }

    public boolean alreadyOnCaseOrWaiting(String triggerAnalysisId, String testId) {
        var owner = links.getActiveByAnalysisId(triggerAnalysisId);
        if (owner == null)
            return false;
        return links.getAnalyses(owner.getCaseId()).stream().anyMatch(a -> testId.equals(a.getTest().getId()))
                || incoming.forCase(owner.getCaseId()).stream()
                        .anyMatch(i -> testId.equals(i.getTestId()) && !"PLACED".equals(i.getState()));
    }
}
