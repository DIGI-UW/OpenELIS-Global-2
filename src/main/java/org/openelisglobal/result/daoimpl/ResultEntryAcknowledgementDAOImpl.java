package org.openelisglobal.result.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.result.dao.ResultEntryAcknowledgementDAO;
import org.openelisglobal.result.valueholder.ResultEntryAcknowledgement;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultEntryAcknowledgementDAOImpl extends BaseDAOImpl<ResultEntryAcknowledgement, String>
        implements ResultEntryAcknowledgementDAO {

    public ResultEntryAcknowledgementDAOImpl() {
        super(ResultEntryAcknowledgement.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultEntryAcknowledgement> getByAnalysisId(String analysisId) {
        String hql = "from ResultEntryAcknowledgement a where a.analysisId = :analysisId order by a.acknowledgedAt";
        Query<ResultEntryAcknowledgement> query = entityManager.unwrap(Session.class).createQuery(hql,
                ResultEntryAcknowledgement.class);
        query.setParameter("analysisId", analysisId);
        return query.list();
    }
}
