package org.openelisglobal.eqa.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleEQADAOImpl extends BaseDAOImpl<SampleEQA, Long> implements SampleEQADAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleEQADAOImpl.class);

    public SampleEQADAOImpl() {
        super(SampleEQA.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleEQA> findBySampleId(Long sampleId) {
        try {
            String hql = "FROM SampleEQA s WHERE s.sampleId = :sampleId";
            Query<SampleEQA> query = entityManager.unwrap(Session.class).createQuery(hql, SampleEQA.class);
            query.setParameter("sampleId", sampleId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            logger.error("Error retrieving SampleEQA for sample: {}", sampleId, e);
            throw new LIMSRuntimeException("Error retrieving SampleEQA by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByDeadlineBefore(Timestamp deadline) {
        try {
            String hql = "FROM SampleEQA s WHERE s.isEqaSample = true AND s.eqaDeadline IS NOT NULL "
                    + "AND s.eqaDeadline <= :deadline ORDER BY s.eqaDeadline ASC";
            Query<SampleEQA> query = entityManager.unwrap(Session.class).createQuery(hql, SampleEQA.class);
            query.setParameter("deadline", deadline);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving SampleEQA by deadline before: {}", deadline, e);
            throw new LIMSRuntimeException("Error retrieving SampleEQA by deadline", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByProgramId(Long programId) {
        try {
            String hql = "FROM SampleEQA s WHERE s.eqaProgram.id = :programId ORDER BY s.eqaDeadline ASC";
            Query<SampleEQA> query = entityManager.unwrap(Session.class).createQuery(hql, SampleEQA.class);
            query.setParameter("programId", programId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving SampleEQA for program: {}", programId, e);
            throw new LIMSRuntimeException("Error retrieving SampleEQA by program", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByIsEqaSample(Boolean isEqaSample) {
        try {
            String hql = "FROM SampleEQA s WHERE s.isEqaSample = :isEqaSample ORDER BY s.eqaDeadline ASC";
            Query<SampleEQA> query = entityManager.unwrap(Session.class).createQuery(hql, SampleEQA.class);
            query.setParameter("isEqaSample", isEqaSample);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving SampleEQA by isEqaSample: {}", isEqaSample, e);
            throw new LIMSRuntimeException("Error retrieving SampleEQA by EQA flag", e);
        }
    }
}
