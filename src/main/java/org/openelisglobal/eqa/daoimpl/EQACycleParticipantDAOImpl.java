package org.openelisglobal.eqa.daoimpl;

import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQACycleParticipantDAO;
import org.openelisglobal.eqa.valueholder.EQACycleParticipant;
import org.openelisglobal.eqa.valueholder.EQACycleParticipantStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQACycleParticipantDAOImpl extends BaseDAOImpl<EQACycleParticipant, Long>
        implements EQACycleParticipantDAO {

    public EQACycleParticipantDAOImpl() {
        super(EQACycleParticipant.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQACycleParticipant> findActiveByCycleIds(Collection<Long> cycleIds) {
        if (cycleIds == null || cycleIds.isEmpty()) {
            return List.of();
        }
        return entityManager.unwrap(Session.class)
                .createQuery("FROM EQACycleParticipant p WHERE p.cycle.id IN :cycleIds AND p.status = :status"
                        + " ORDER BY p.cycle.id, p.id", EQACycleParticipant.class)
                .setParameter("cycleIds", cycleIds).setParameter("status", EQACycleParticipantStatus.ACTIVE).list();
    }
}
