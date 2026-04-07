package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAProgramTestDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAProgramTestDAOImpl extends BaseDAOImpl<EQAProgramTest, Long> implements EQAProgramTestDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAProgramTestDAOImpl.class);

    public EQAProgramTestDAOImpl() {
        super(EQAProgramTest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramTest> findByProgramId(Long programId) {
        try {
            String hql = "FROM EQAProgramTest pt WHERE pt.eqaProgram.id = :programId";
            Query<EQAProgramTest> query = entityManager.unwrap(Session.class).createQuery(hql, EQAProgramTest.class);
            query.setParameter("programId", programId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving program tests for program: {}", programId, e);
            throw new LIMSRuntimeException("Error retrieving program tests", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramTest> findActiveByProgramId(Long programId) {
        try {
            String hql = "FROM EQAProgramTest pt WHERE pt.eqaProgram.id = :programId AND pt.isActive = true";
            Query<EQAProgramTest> query = entityManager.unwrap(Session.class).createQuery(hql, EQAProgramTest.class);
            query.setParameter("programId", programId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving active program tests for program: {}", programId, e);
            throw new LIMSRuntimeException("Error retrieving active program tests", e);
        }
    }
}
