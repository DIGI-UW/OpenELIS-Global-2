package org.openelisglobal.sampletyperequest.daoimpl;

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
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session.createNativeQuery(
                "SELECT * FROM clinlims.sample_type_request WHERE sample_id = :sampleId ORDER BY sort_order",
                SampleTypeRequest.class);
        query.setParameter("sampleId", Integer.valueOf(sampleId));
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId) {
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createNativeQuery("SELECT * FROM clinlims.sample_type_request WHERE sample_id = :sampleId "
                        + "AND status = :status ORDER BY sort_order", SampleTypeRequest.class);
        query.setParameter("sampleId", Integer.valueOf(sampleId));
        query.setParameter("status", SampleTypeRequest.Status.REQUESTED.name());
        return query.list();
    }

    @Override
    public List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId) {
        Session session = entityManager.unwrap(Session.class);
        Query<SampleTypeRequest> query = session
                .createNativeQuery("SELECT * FROM clinlims.sample_type_request WHERE sample_id = :sampleId "
                        + "AND status = :status ORDER BY sort_order", SampleTypeRequest.class);
        query.setParameter("sampleId", Integer.valueOf(sampleId));
        query.setParameter("status", SampleTypeRequest.Status.COLLECTED.name());
        return query.list();
    }
}
