package org.openelisglobal.reportqueue.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reportqueue.valueholder.ReportQueuePreference;

public interface ReportQueuePreferenceDAO extends BaseDAO<ReportQueuePreference, Long> {

    Optional<ReportQueuePreference> findByUser(Long systemUserId);

    ReportQueuePreference save(ReportQueuePreference preference);
}
