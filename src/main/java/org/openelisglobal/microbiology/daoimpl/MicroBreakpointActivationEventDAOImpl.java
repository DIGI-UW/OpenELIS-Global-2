package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroBreakpointActivationEventDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroBreakpointActivationEventDAOImpl extends BaseDAOImpl<MicroBreakpointActivationEvent, String>
        implements MicroBreakpointActivationEventDAO {

    public MicroBreakpointActivationEventDAOImpl() {
        super(MicroBreakpointActivationEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointActivationEvent> getByStandardId(String standardId) {
        Query<MicroBreakpointActivationEvent> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointActivationEvent e where e.standardId = :standardId order by e.occurredAt",
                MicroBreakpointActivationEvent.class);
        query.setParameter("standardId", standardId);
        return query.list();
    }
}
