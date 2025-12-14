package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.ProcessingStep;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProcessingStepDAOImpl extends BaseDAOImpl<ProcessingStep, Integer> implements ProcessingStepDAO {

    public ProcessingStepDAOImpl() {
        super(ProcessingStep.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessingStep> findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM ProcessingStep WHERE sample.id = :sampleId ORDER BY startedAt";
            Query<ProcessingStep> query = entityManager.unwrap(Session.class).createQuery(hql, ProcessingStep.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding ProcessingSteps by sampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessingStep> findByStepType(ProcessingStep.StepType stepType) {
        try {
            String hql = "FROM ProcessingStep WHERE stepType = :stepType ORDER BY startedAt DESC";
            Query<ProcessingStep> query = entityManager.unwrap(Session.class).createQuery(hql, ProcessingStep.class);
            query.setParameter("stepType", stepType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding ProcessingSteps by stepType", e);
        }
    }
}
