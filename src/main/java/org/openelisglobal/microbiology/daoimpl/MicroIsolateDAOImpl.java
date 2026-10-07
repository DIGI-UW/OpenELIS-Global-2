package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroIsolateDAOImpl extends BaseDAOImpl<MicroIsolate, String> implements MicroIsolateDAO {

    public MicroIsolateDAOImpl() {
        super(MicroIsolate.class);
    }

    @Override
    public MicroIsolate getForUpdate(String isolateId) {
        MicroIsolate isolate = entityManager.find(MicroIsolate.class, isolateId);
        if (isolate == null) {
            throw new IllegalArgumentException("Isolate not found");
        }
        entityManager.refresh(isolate, LockModeType.PESSIMISTIC_WRITE);
        return isolate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getByCaseId(String caseId) {
        Query<MicroIsolate> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroIsolate i where i.caseId = :caseId and i.cancelledAt is null order by i.isolateLabel",
                MicroIsolate.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getByCaseIds(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return List.of();
        }
        Query<MicroIsolate> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroIsolate i where i.caseId in (:caseIds) and i.cancelledAt is null"
                        + " order by i.caseId, i.isolateLabel", MicroIsolate.class);
        query.setParameterList("caseIds", caseIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getByAmendmentId(String amendmentId) {
        Query<MicroIsolate> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroIsolate i where i.amendmentId = :amendmentId and i.cancelledAt is null"
                        + " order by i.isolateLabel", MicroIsolate.class);
        query.setParameter("amendmentId", amendmentId);
        return query.list();
    }
}
