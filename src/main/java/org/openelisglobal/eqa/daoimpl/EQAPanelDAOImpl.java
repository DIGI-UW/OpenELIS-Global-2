package org.openelisglobal.eqa.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelDAOImpl extends BaseDAOImpl<EQAPanel, Long> implements EQAPanelDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQAPanelDAOImpl.class);

    public EQAPanelDAOImpl() {
        super(EQAPanel.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAPanel> findByCycleId(Long cycleId) {
        try {
            String hql = "FROM EQAPanel p WHERE p.cycle.id = :cycleId";
            Query<EQAPanel> query = entityManager.unwrap(Session.class).createQuery(hql, EQAPanel.class);
            query.setParameter("cycleId", cycleId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving panels for cycle {}", cycleId, e);
            throw new LIMSRuntimeException("Error retrieving panels for cycle", e);
        }
    }
}
