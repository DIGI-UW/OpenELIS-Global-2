package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroReportingRuleHistoryDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportingRuleHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroReportingRuleHistoryDAOImpl extends BaseDAOImpl<MicroReportingRuleHistory, String>
        implements MicroReportingRuleHistoryDAO {
    public MicroReportingRuleHistoryDAOImpl() {
        super(MicroReportingRuleHistory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportingRuleHistory> getByRuleId(String ruleId) {
        Query<MicroReportingRuleHistory> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroReportingRuleHistory h where h.ruleId = :ruleId order by h.revision desc",
                MicroReportingRuleHistory.class);
        query.setParameter("ruleId", ruleId);
        return query.list();
    }
}
