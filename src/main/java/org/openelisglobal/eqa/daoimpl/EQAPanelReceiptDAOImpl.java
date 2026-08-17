package org.openelisglobal.eqa.daoimpl;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelReceiptDAOImpl extends BaseDAOImpl<EQAPanelReceipt, Long> implements EQAPanelReceiptDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAPanelReceiptDAOImpl.class);

    public EQAPanelReceiptDAOImpl() {
        super(EQAPanelReceipt.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EQAPanelReceipt> findByCycleAndEnrollment(Long cycleId, Long labEnrollmentId) {
        try {
            String hql = "FROM EQAPanelReceipt r WHERE r.cycle.id = :cycleId"
                    + " AND r.labEnrollmentId = :labEnrollmentId";
            Query<EQAPanelReceipt> query = entityManager.unwrap(Session.class).createQuery(hql, EQAPanelReceipt.class);
            query.setParameter("cycleId", cycleId);
            query.setParameter("labEnrollmentId", labEnrollmentId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            logger.error("Error retrieving panel receipt for cycle {} enrollment {}", cycleId, labEnrollmentId, e);
            throw new LIMSRuntimeException("Error retrieving panel receipt", e);
        }
    }
}
