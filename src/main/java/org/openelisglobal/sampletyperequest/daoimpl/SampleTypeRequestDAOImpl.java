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

    private Integer parseSampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(sampleId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<SampleTypeRequest> getRequestsBySampleId(String sampleId) {
        Integer id = parseSampleId(sampleId);
        if (id == null) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session.createQuery(
                "FROM SampleTypeRequest str WHERE str.sample.id = :sampleId ORDER BY str.sortOrder",
                SampleTypeRequest.class);
        query.setParameter("sampleId", id);
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId) {
        Integer id = parseSampleId(sampleId);
        if (id == null) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createQuery("FROM SampleTypeRequest str WHERE str.sample.id = :sampleId "
                        + "AND str.status = :status ORDER BY str.sortOrder", SampleTypeRequest.class);
        query.setParameter("sampleId", id);
        query.setParameter("status", SampleTypeRequest.Status.REQUESTED);
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId) {
        Integer id = parseSampleId(sampleId);
        if (id == null) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createQuery("FROM SampleTypeRequest str WHERE str.sample.id = :sampleId "
                        + "AND str.status = :status ORDER BY str.sortOrder", SampleTypeRequest.class);
        query.setParameter("sampleId", id);
        query.setParameter("status", SampleTypeRequest.Status.COLLECTED);
        return query.list();
    }
}
