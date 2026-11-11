package org.openelisglobal.eqa.service;

import java.util.LinkedHashSet;
import java.util.Set;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.openelisglobal.resultvalidation.event.ResultsValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Advances participant cycles once their results are validated; the submission
 * sweep is the fallback.
 */
@Component
public class EQAResultsValidatedListener {

    private static final Logger logger = LoggerFactory.getLogger(EQAResultsValidatedListener.class);

    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;

    @Autowired
    private SampleEQADAO sampleEQADAO;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TransactionalEventListener
    public void advanceCycles(ResultsValidatedEvent event) {
        if (event.sampleIds().isEmpty()) {
            return;
        }
        // After commit the old transaction is still bound, so each step needs
        // REQUIRES_NEW; one per cycle so a failure leaves the rest advanced.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Set<Long> cycleIds = tx.execute(status -> cycleIdsOf(event.sampleIds()));
        for (Long cycleId : cycleIds) {
            try {
                tx.execute(status -> cycleSubmissionService.advanceCycle(cycleId));
            } catch (RuntimeException e) {
                logger.warn("EQA cycle {} not advanced after validation; the sweep will retry", cycleId, e);
            }
        }
    }

    private Set<Long> cycleIdsOf(Set<Long> sampleIds) {
        Set<Long> cycleIds = new LinkedHashSet<>();
        for (Long sampleId : sampleIds) {
            sampleEQADAO.findBySampleId(sampleId)
                    .filter(sample -> Boolean.TRUE.equals(sample.getIsEqaSample()) && sample.getCycleId() != null)
                    .map(SampleEQA::getCycleId).ifPresent(cycleIds::add);
        }
        return cycleIds;
    }
}
