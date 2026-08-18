package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.SampleEQA;

public interface SampleEQAService extends BaseObjectService<SampleEQA, Long> {

    Optional<SampleEQA> findBySampleId(Long sampleId);

    List<SampleEQA> findByDeadlineBefore(Timestamp deadline);

    List<SampleEQA> findByProgramId(Long programId);

    List<SampleEQA> findEqaSamples();

    /**
     * Order status derived live from the linked order's analyses (D-LIVE-2):
     * COMPLETED when every non-cancelled analysis is finalized, else OVERDUE once
     * the deadline has passed, IN_PROGRESS once any analysis has left NotStarted,
     * else PENDING. Read-side only — V2 cycle state (T-10) supersedes this.
     */
    String deriveOrderStatus(SampleEQA sampleEQA);
}
