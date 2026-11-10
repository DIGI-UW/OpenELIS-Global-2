package org.openelisglobal.reportqueue.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reportqueue.dao.ReportQueuePreferenceDAO;
import org.openelisglobal.reportqueue.valueholder.ReportQueuePreference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportQueuePreferenceDAOImpl extends BaseDAOImpl<ReportQueuePreference, Long>
        implements ReportQueuePreferenceDAO {

    public ReportQueuePreferenceDAOImpl() {
        super(ReportQueuePreference.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportQueuePreference> findByUser(Long systemUserId) {
        try {
            TypedQuery<ReportQueuePreference> query = entityManager.createQuery(
                    "FROM ReportQueuePreference p WHERE p.systemUserId = :userId", ReportQueuePreference.class);
            query.setParameter("userId", systemUserId);
            query.setMaxResults(1);
            List<ReportQueuePreference> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueuePreference findByUser()", e);
        }
    }

    @Override
    public ReportQueuePreference save(ReportQueuePreference preference) {
        try {
            if (preference.getId() == null) {
                entityManager.persist(preference);
                return preference;
            }
            return entityManager.merge(preference);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportQueuePreference save()", e);
        }
    }
}
