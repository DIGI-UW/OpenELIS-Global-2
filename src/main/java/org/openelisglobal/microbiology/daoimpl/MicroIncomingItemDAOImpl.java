package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroIncomingItemDAO;
import org.openelisglobal.microbiology.valueholder.MicroIncomingItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroIncomingItemDAOImpl extends BaseDAOImpl<MicroIncomingItem, String> implements MicroIncomingItemDAO {
    public MicroIncomingItemDAOImpl() {
        super(MicroIncomingItem.class);
    }

    public List<MicroIncomingItem> forCase(String id) {
        return entityManager.createQuery(
                "select i from MicroIncomingItem i join fetch i.microCase where i.microCase.id=:id order by i.receivedAt,i.id",
                MicroIncomingItem.class).setParameter("id", id).getResultList();
    }

    public MicroIncomingItem findDelivery(String id, String test, String key) {
        return entityManager.createQuery(
                "select i from MicroIncomingItem i join fetch i.microCase where i.originalCaseId=:id and i.testId=:test and i.runSourceKey=:key",
                MicroIncomingItem.class).setParameter("id", id).setParameter("test", test).setParameter("key", key)
                .getResultStream().findFirst().orElse(null);
    }

    public MicroIncomingItem withCase(String id) {
        return entityManager.createQuery("select i from MicroIncomingItem i join fetch i.microCase where i.id=:id",
                MicroIncomingItem.class).setParameter("id", id).getResultStream().findFirst().orElse(null);
    }
}
