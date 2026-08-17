package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAParticipantResultDAOImpl extends BaseDAOImpl<EQAParticipantResult, Long>
        implements EQAParticipantResultDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAParticipantResultDAOImpl.class);

    public EQAParticipantResultDAOImpl() {
        super(EQAParticipantResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAParticipantResult> findByCycleAndEnrollment(Long cycleId, Long labEnrollmentId) {
        try {
            String hql = "FROM EQAParticipantResult r WHERE r.cycle.id = :cycleId"
                    + " AND r.labEnrollmentId = :labEnrollmentId";
            Query<EQAParticipantResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQAParticipantResult.class);
            query.setParameter("cycleId", cycleId);
            query.setParameter("labEnrollmentId", labEnrollmentId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving participant results for cycle {} enrollment {}", cycleId, labEnrollmentId,
                    e);
            throw new LIMSRuntimeException("Error retrieving participant results", e);
        }
    }
}
