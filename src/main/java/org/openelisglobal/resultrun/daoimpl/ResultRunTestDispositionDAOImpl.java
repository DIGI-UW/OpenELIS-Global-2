package org.openelisglobal.resultrun.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultrun.dao.ResultRunTestDispositionDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultRunTestDispositionDAOImpl extends BaseDAOImpl<ResultRunTestDisposition, Long>
        implements ResultRunTestDispositionDAO {

    public ResultRunTestDispositionDAOImpl() {
        super(ResultRunTestDisposition.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultRunTestDisposition> getDispositions(Long runId) {
        try {
            return entityManager.unwrap(Session.class)
                    .createQuery("FROM ResultRunTestDisposition d WHERE d.runId = :runId ORDER BY d.decidedAt, d.id",
                            ResultRunTestDisposition.class)
                    .setParameter("runId", runId).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the dispositions of a run", e);
        }
    }
}
