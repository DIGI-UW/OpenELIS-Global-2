package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQACycleStateTransitionDAO;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQACycleStateTransitionDAOImpl extends BaseDAOImpl<EQACycleStateTransition, Long>
        implements EQACycleStateTransitionDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQACycleStateTransitionDAOImpl.class);

    public EQACycleStateTransitionDAOImpl() {
        super(EQACycleStateTransition.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQACycleStateTransition> findByCycleId(Long cycleId) {
        try {
            String hql = "FROM EQACycleStateTransition t WHERE t.cycle.id = :cycleId ORDER BY t.occurredAt ASC, t.id ASC";
            Query<EQACycleStateTransition> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQACycleStateTransition.class);
            query.setParameter("cycleId", cycleId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving state transitions for cycle {}", cycleId, e);
            throw new LIMSRuntimeException("Error retrieving cycle state transitions", e);
        }
    }
}
