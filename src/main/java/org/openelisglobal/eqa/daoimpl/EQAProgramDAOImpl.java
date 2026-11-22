package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAProgramDAOImpl extends BaseDAOImpl<EQAProgram, Long> implements EQAProgramDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAProgramDAOImpl.class);

    public EQAProgramDAOImpl() {
        super(EQAProgram.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgram> findByIsActive(Boolean isActive) {
        try {
            String hql = "FROM EQAProgram p WHERE p.isActive = :isActive ORDER BY p.name";
            Query<EQAProgram> query = entityManager.unwrap(Session.class).createQuery(hql, EQAProgram.class);
            query.setParameter("isActive", isActive);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving EQA programs by active status: {}", isActive, e);
            throw new LIMSRuntimeException("Error retrieving EQA programs by active status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EQASchemeType findStoredSchemeType(Long id) {
        try {
            String hql = "SELECT p.schemeType FROM EQAProgram p WHERE p.id = :id";
            Query<EQASchemeType> query = entityManager.unwrap(Session.class).createQuery(hql, EQASchemeType.class);
            query.setParameter("id", id);
            // Suppressing the flush is the whole point of this method. get() would answer
            // from the first-level cache and hand a caller back the very object it just
            // edited, and an ordinary query would flush that pending edit to the database
            // before reading, so both report the new value as though it were the old one.
            // A scalar projection with the flush held off reads the committed column.
            query.setHibernateFlushMode(FlushMode.COMMIT);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error retrieving the stored scheme type for EQA program {}", id, e);
            throw new LIMSRuntimeException("Error retrieving the stored scheme type", e);
        }
    }
}
