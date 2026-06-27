package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstReadingDAOImpl extends BaseDAOImpl<MicroAstReading, String> implements MicroAstReadingDAO {

    public MicroAstReadingDAOImpl() {
        super(MicroAstReading.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstReading> getByRunId(String runId) {
        Query<MicroAstReading> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstReading r where r.astRunId = :runId order by r.antibioticId", MicroAstReading.class);
        query.setParameter("runId", runId);
        return query.list();
    }
}
