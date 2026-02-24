package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQALabProgramEnrollmentDAOImpl extends BaseDAOImpl<EQALabProgramEnrollment, Long>
        implements EQALabProgramEnrollmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQALabProgramEnrollmentDAOImpl.class);

    public EQALabProgramEnrollmentDAOImpl() {
        super(EQALabProgramEnrollment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findAll() {
        try {
            String hql = "FROM EQALabProgramEnrollment e ORDER BY e.programName";
            Query<EQALabProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQALabProgramEnrollment.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving all lab program enrollments", e);
            throw new LIMSRuntimeException("Error retrieving all lab program enrollments", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findByIsActive(Boolean isActive) {
        try {
            String hql = "FROM EQALabProgramEnrollment e WHERE e.isActive = :isActive ORDER BY e.programName";
            Query<EQALabProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQALabProgramEnrollment.class);
            query.setParameter("isActive", isActive);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving lab program enrollments by active status: {}", isActive, e);
            throw new LIMSRuntimeException("Error retrieving lab program enrollments by active status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistinctProviders() {
        try {
            String hql = "SELECT DISTINCT e.provider FROM EQALabProgramEnrollment e"
                    + " WHERE e.provider IS NOT NULL ORDER BY e.provider";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving distinct providers from lab enrollments", e);
            throw new LIMSRuntimeException("Error retrieving distinct providers", e);
        }
    }
}
