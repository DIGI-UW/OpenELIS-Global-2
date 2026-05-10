package org.openelisglobal.sampletyperequest.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.sampletyperequest.dao.SampleTypeRequestDAO;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.stereotype.Repository;

@Repository
public class SampleTypeRequestDAOImpl extends BaseDAOImpl<SampleTypeRequest, Integer> implements SampleTypeRequestDAO {

    public SampleTypeRequestDAOImpl() {
        super(SampleTypeRequest.class);
    }

    @Override
    public List<SampleTypeRequest> getRequestsBySampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session.createQuery(
                "FROM SampleTypeRequest str WHERE str.sample.id = :sampleId ORDER BY str.sortOrder",
                SampleTypeRequest.class);
        query.setParameter("sampleId", sampleId.trim());
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createQuery("FROM SampleTypeRequest str WHERE str.sample.id = :sampleId "
                        + "AND str.status = :status ORDER BY str.sortOrder", SampleTypeRequest.class);
        query.setParameter("sampleId", sampleId.trim());
        query.setParameter("status", SampleTypeRequest.Status.REQUESTED);
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createQuery("FROM SampleTypeRequest str WHERE str.sample.id = :sampleId "
                        + "AND str.status = :status ORDER BY str.sortOrder", SampleTypeRequest.class);
        query.setParameter("sampleId", sampleId.trim());
        query.setParameter("status", SampleTypeRequest.Status.COLLECTED);
        return query.list();
    }
}
