package org.openelisglobal.microbiology.daoimpl;

import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstQcLotDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcLot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstQcLotDAOImpl extends BaseDAOImpl<MicroAstQcLot, String> implements MicroAstQcLotDAO {

    public MicroAstQcLotDAOImpl() {
        super(MicroAstQcLot.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getControlIdsByLotIds(Collection<Long> lotIds) {
        if (lotIds == null || lotIds.isEmpty()) {
            return List.of();
        }
        Query<String> query = entityManager.unwrap(Session.class).createQuery(
                "select distinct l.controlId from MicroAstQcLot l where l.inventoryLotId in :lotIds", String.class);
        query.setParameter("lotIds", lotIds);
        return query.list();
    }
}
