package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQADistributionDAOImpl extends BaseDAOImpl<EQADistribution, Long> implements EQADistributionDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQADistributionDAOImpl.class);

    public EQADistributionDAOImpl() {
        super(EQADistribution.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQADistribution> findByProgramId(Long programId) {
        try {
            String hql = "FROM EQADistribution d WHERE d.eqaProgram.id = :programId ORDER BY d.distributionDate DESC";
            Query<EQADistribution> query = entityManager.unwrap(Session.class).createQuery(hql, EQADistribution.class);
            query.setParameter("programId", programId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving distributions for program: {}", programId, e);
            throw new LIMSRuntimeException("Error retrieving distributions by program", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQADistribution> findByStatus(EQADistributionStatus status) {
        try {
            String hql = "FROM EQADistribution d WHERE d.status = :status ORDER BY d.distributionDate DESC";
            Query<EQADistribution> query = entityManager.unwrap(Session.class).createQuery(hql, EQADistribution.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving distributions by status: {}", status, e);
            throw new LIMSRuntimeException("Error retrieving distributions by status", e);
        }
    }
}
