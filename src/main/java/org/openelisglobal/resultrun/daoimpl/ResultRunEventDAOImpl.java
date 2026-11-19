package org.openelisglobal.resultrun.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultRunEventDAOImpl extends BaseDAOImpl<ResultRunEvent, Long> implements ResultRunEventDAO {

    public ResultRunEventDAOImpl() {
        super(ResultRunEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultRunEvent> getEvents(Long runId) {
        try {
            return entityManager.unwrap(Session.class)
                    .createQuery("FROM ResultRunEvent e WHERE e.runId = :runId ORDER BY e.occurredAt, e.id",
                            ResultRunEvent.class)
                    .setParameter("runId", runId).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the events of a run", e);
        }
    }
}
