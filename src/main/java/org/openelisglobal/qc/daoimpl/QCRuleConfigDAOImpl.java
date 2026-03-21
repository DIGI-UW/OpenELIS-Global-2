package org.openelisglobal.qc.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dao.QCRuleConfigDAO;
import org.openelisglobal.qc.valueholder.QCRuleConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QCRuleConfigDAOImpl extends BaseDAOImpl<QCRuleConfig, Long> implements QCRuleConfigDAO {

    public QCRuleConfigDAOImpl() {
        super(QCRuleConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleConfig> findByTestTypeId(Long testTypeId) {
        try {
            String hql = "FROM QCRuleConfig r WHERE r.testTypeId = :testTypeId ORDER BY r.ruleCode";
            Query<QCRuleConfig> query = entityManager.unwrap(Session.class).createQuery(hql, QCRuleConfig.class);
            query.setParameter("testTypeId", testTypeId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC rule configs by test type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleConfig> findEnabledByTestTypeId(Long testTypeId) {
        try {
            String hql = "FROM QCRuleConfig r WHERE r.testTypeId = :testTypeId AND r.enabled = true ORDER BY r.ruleCode";
            Query<QCRuleConfig> query = entityManager.unwrap(Session.class).createQuery(hql, QCRuleConfig.class);
            query.setParameter("testTypeId", testTypeId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding enabled QC rule configs by test type", e);
        }
    }
}
