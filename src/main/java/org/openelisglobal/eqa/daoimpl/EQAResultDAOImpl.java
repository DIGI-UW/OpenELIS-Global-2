package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAResultDAOImpl extends BaseDAOImpl<EQAResult, Long> implements EQAResultDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAResultDAOImpl.class);

    public EQAResultDAOImpl() {
        super(EQAResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAResult> findByDistributionId(Long distributionId) {
        try {
            String hql = "FROM EQAResult r WHERE r.eqaDistribution.id = :distributionId "
                    + "ORDER BY r.submissionDate DESC";
            Query<EQAResult> query = entityManager.unwrap(Session.class).createQuery(hql, EQAResult.class);
            query.setParameter("distributionId", distributionId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving results for distribution: {}", distributionId, e);
            throw new LIMSRuntimeException("Error retrieving results by distribution", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EQAResult> findByDistributionAndOrgAndTest(Long distributionId, Long organizationId, Long testId) {
        try {
            String hql = "FROM EQAResult r WHERE r.eqaDistribution.id = :distributionId "
                    + "AND r.participantOrganizationId = :organizationId AND r.testId = :testId";
            Query<EQAResult> query = entityManager.unwrap(Session.class).createQuery(hql, EQAResult.class);
            query.setParameter("distributionId", distributionId);
            query.setParameter("organizationId", organizationId);
            query.setParameter("testId", testId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            logger.error("Error retrieving result for distribution: {}, org: {}, test: {}", distributionId,
                    organizationId, testId, e);
            throw new LIMSRuntimeException("Error retrieving result by distribution/org/test", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDistributionId(Long distributionId) {
        try {
            String hql = "SELECT COUNT(r) FROM EQAResult r WHERE r.eqaDistribution.id = :distributionId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("distributionId", distributionId);
            Long count = query.uniqueResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            logger.error("Error counting results for distribution: {}", distributionId, e);
            throw new LIMSRuntimeException("Error counting results by distribution", e);
        }
    }
}
