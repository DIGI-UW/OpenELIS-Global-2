package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstOverrideEventDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstOverrideEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstOverrideEventDAOImpl extends BaseDAOImpl<MicroAstOverrideEvent, String>
        implements MicroAstOverrideEventDAO {

    public MicroAstOverrideEventDAOImpl() {
        super(MicroAstOverrideEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstOverrideEvent> getByRunId(String runId) {
        Query<MicroAstOverrideEvent> query = entityManager.unwrap(Session.class).createQuery(
                "select e from MicroAstOverrideEvent e, MicroAstReading r "
                        + "where e.readingId = r.id and r.astRunId = :runId order by e.performedAt, e.id",
                MicroAstOverrideEvent.class);
        query.setParameter("runId", runId);
        return query.list();
    }
}
