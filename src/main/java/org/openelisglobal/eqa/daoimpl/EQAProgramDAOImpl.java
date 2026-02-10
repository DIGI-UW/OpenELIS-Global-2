package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
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
}
