package org.openelisglobal.qc.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dao.QCFrequencyConfigDAO;
import org.openelisglobal.qc.valueholder.QCFrequencyConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QCFrequencyConfigDAOImpl extends BaseDAOImpl<QCFrequencyConfig, Long> implements QCFrequencyConfigDAO {

    public QCFrequencyConfigDAOImpl() {
        super(QCFrequencyConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QCFrequencyConfig> findByInstrumentId(Long instrumentId) {
        try {
            String hql = "FROM QCFrequencyConfig f WHERE f.instrumentId = :instrumentId";
            Query<QCFrequencyConfig> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QCFrequencyConfig.class);
            query.setParameter("instrumentId", instrumentId);
            List<QCFrequencyConfig> results = query.list();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC frequency config by instrument", e);
        }
    }
}
