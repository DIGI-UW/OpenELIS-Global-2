package org.openelisglobal.qaevent.qiconfig.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.qaevent.qiconfig.dao.QiConfigDAO;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QiConfigDAOImpl extends BaseDAOImpl<QiConfig, Long> implements QiConfigDAO {

    public QiConfigDAOImpl() {
        super(QiConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QiConfig> getAllOrderedByIndicator() {
        String hql = "from QiConfig c order by c.indicatorKey, c.testCategoryId nulls first";
        return entityManager.unwrap(Session.class).createQuery(hql, QiConfig.class).list();
    }

    @Override
    @Transactional(readOnly = true)
    public QiConfig getDefault(String indicatorKey) {
        String hql = "from QiConfig c where c.indicatorKey = :k and c.testCategoryId is null";
        Query<QiConfig> query = entityManager.unwrap(Session.class).createQuery(hql, QiConfig.class);
        query.setParameter("k", indicatorKey);
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public QiConfig getOverride(String indicatorKey, String testSectionId) {
        String hql = "from QiConfig c where c.indicatorKey = :k and c.testCategoryId = :sid";
        Query<QiConfig> query = entityManager.unwrap(Session.class).createQuery(hql, QiConfig.class);
        query.setParameter("k", indicatorKey);
        query.setParameter("sid", testSectionId);
        return query.uniqueResult();
    }
}
