package org.openelisglobal.eqa.daoimpl;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelDAOImpl extends BaseDAOImpl<EQAPanel, Long> implements EQAPanelDAO {

    public EQAPanelDAOImpl() {
        super(EQAPanel.class);
    }

    @Override
    public Optional<EQAPanel> getForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(EQAPanel.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countByCycleIds(Collection<Long> cycleIds) {
        if (cycleIds == null || cycleIds.isEmpty()) {
            return List.of();
        }
        return entityManager.unwrap(Session.class).createQuery(
                "SELECT p.cycle.id, COUNT(p.id) FROM EQAPanel p WHERE p.cycle.id IN :cycleIds" + " GROUP BY p.cycle.id",
                Object[].class).setParameter("cycleIds", cycleIds).list();
    }
}
