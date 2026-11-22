package org.openelisglobal.eqa.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQARoundDAOImpl extends BaseDAOImpl<EQARound, Long> implements EQARoundDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQARoundDAOImpl.class);

    public EQARoundDAOImpl() {
        super(EQARound.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQARound> findWithSubmissionDeadlineBetween(Timestamp from, Timestamp to) {
        try {
            String hql = "FROM EQARound r JOIN FETCH r.cycle JOIN FETCH r.cycle.scheme"
                    + " WHERE r.submissionDeadline >= :from AND r.submissionDeadline < :to";
            Query<EQARound> query = entityManager.unwrap(Session.class).createQuery(hql, EQARound.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving rounds with deadline between {} and {}", from, to, e);
            throw new LIMSRuntimeException("Error retrieving rounds by deadline window", e);
        }
    }
}
