package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerActivationRecordDAOImpl extends BaseDAOImpl<AnalyzerActivationRecord, String>
        implements AnalyzerActivationRecordDAO {

    public AnalyzerActivationRecordDAOImpl() {
        super(AnalyzerActivationRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivationRecord> findByAnalyzerId(String analyzerId) {
        String hql = "SELECT record FROM AnalyzerActivationRecord record "
                + "WHERE record.analyzer.id = :analyzerId ORDER BY record.createdAt, record.id";
        Query<AnalyzerActivationRecord> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerActivationRecord.class);
        query.setParameter("analyzerId", analyzerId);
        return query.getResultList();
    }
}
