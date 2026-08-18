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
     * Order status derived live from the linked order's analyses (OGC-609):
     * COMPLETED when every non-cancelled analysis is finalized, else OVERDUE once
     * the deadline has passed, IN_PROGRESS once any analysis has left NotStarted,
     * else PENDING. Read-side only — no column stores this; the EQA cycle state
     * model replaces it once orders are cycle-linked.
     */
    String deriveOrderStatus(SampleEQA sampleEQA);
}
