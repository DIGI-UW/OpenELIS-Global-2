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
                "from MicroAstReading r where r.astRunId = :runId order by r.entrySequence, r.createdAt, r.id",
                MicroAstReading.class);
        query.setParameter("runId", runId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstReading> getSelectedByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty())
            return List.of();
        return entityManager
                .createQuery("select r from MicroAstReading r, MicroAstReportChoice c, MicroAstRun a, MicroIsolate i "
                        + "where c.readingId=r.id and c.antibioticId=r.antibioticId and c.isolateId=a.isolateId "
                        + "and a.id=r.astRunId and i.id=a.isolateId and i.cancelledAt is null and a.status='REVIEWED' "
                        + "and r.astRunId in (:runIds) order by r.astRunId,r.entrySequence", MicroAstReading.class)
                .setParameter("runIds", runIds).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstReading> getByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return List.of();
        }
        Query<MicroAstReading> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstReading r where r.astRunId in (:runIds) order by r.astRunId, r.entrySequence, r.createdAt, r.id",
                MicroAstReading.class);
        query.setParameterList("runIds", runIds);
        return query.list();
    }
}
