package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroIsolateIdentificationEventDAO;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroIsolateIdentificationEventDAOImpl extends BaseDAOImpl<MicroIsolateIdentificationEvent, String>
        implements MicroIsolateIdentificationEventDAO {

    public MicroIsolateIdentificationEventDAOImpl() {
        super(MicroIsolateIdentificationEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolateIdentificationEvent> getByIsolateId(String isolateId) {
        Query<MicroIsolateIdentificationEvent> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroIsolateIdentificationEvent e where e.isolateId = :isolateId order by e.changedAt, e.id",
                MicroIsolateIdentificationEvent.class);
        query.setParameter("isolateId", isolateId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolateIdentificationEvent> getByAmendmentId(String amendmentId) {
        Query<MicroIsolateIdentificationEvent> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroIsolateIdentificationEvent e where e.amendmentId = :amendmentId order by e.changedAt, e.id",
                MicroIsolateIdentificationEvent.class);
        query.setParameter("amendmentId", amendmentId);
        return query.list();
    }
}
