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
 * Advances a participant cycle as soon as its results are validated. Until now
 * only the submission sweep did, so a cycle whose last result had been
 * validated waited up to five minutes to read Ready to submit. The sweep still
 * runs and picks up anything this misses.
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
        // After commit the validation's transaction is finished but still bound, and
        // joining it would drop these writes, so each step takes a new one. One
        // cycle each, as in the sweep, so one failure leaves the others advanced.
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
            for (SampleEQA sample : sampleEQADAO.getAllMatching("sampleId", sampleId)) {
                if (Boolean.TRUE.equals(sample.getIsEqaSample()) && sample.getCycleId() != null) {
                    cycleIds.add(sample.getCycleId());
                }
            }
        }
        return cycleIds;
    }
}
