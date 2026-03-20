package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAProgramEnrollmentDAOImpl extends BaseDAOImpl<EQAProgramEnrollment, Long>
        implements EQAProgramEnrollmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAProgramEnrollmentDAOImpl.class);

    public EQAProgramEnrollmentDAOImpl() {
        super(EQAProgramEnrollment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramEnrollment> findByProgramId(Long programId) {
        try {
            String hql = "FROM EQAProgramEnrollment e WHERE e.eqaProgram.id = :programId"
                    + " ORDER BY e.enrollmentDate DESC";
            Query<EQAProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQAProgramEnrollment.class);
            query.setParameter("programId", programId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving enrollments for program: {}", programId, e);
            throw new LIMSRuntimeException("Error retrieving enrollments for program", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status) {
        try {
            String hql = "FROM EQAProgramEnrollment e WHERE e.eqaProgram.id = :programId"
                    + " AND e.status = :status ORDER BY e.enrollmentDate DESC";
            Query<EQAProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQAProgramEnrollment.class);
            query.setParameter("programId", programId);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving enrollments for program {} with status {}", programId, status, e);
            throw new LIMSRuntimeException("Error retrieving enrollments by program and status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveEnrollment(Long programId, Long organizationId) {
        try {
            String hql = "SELECT COUNT(e) FROM EQAProgramEnrollment e" + " WHERE e.eqaProgram.id = :programId"
                    + " AND e.organizationId = :orgId" + " AND e.status = 'Active'";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("programId", programId);
            query.setParameter("orgId", organizationId);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            logger.error("Error checking active enrollment for program {} org {}", programId, organizationId, e);
            throw new LIMSRuntimeException("Error checking active enrollment", e);
        }
    }
}
