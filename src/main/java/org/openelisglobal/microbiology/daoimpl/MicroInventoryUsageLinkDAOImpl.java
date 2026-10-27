package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroInventoryUsageLinkDAO;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroInventoryUsageLinkDAOImpl extends BaseDAOImpl<MicroInventoryUsageLink, String>
        implements MicroInventoryUsageLinkDAO {

    public MicroInventoryUsageLinkDAOImpl() {
        super(MicroInventoryUsageLink.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroInventoryUsageLink> getByCaseId(String caseId) {
        Query<MicroInventoryUsageLink> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroInventoryUsageLink l where l.caseId = :caseId order by l.id", MicroInventoryUsageLink.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getAstRunLotIds(String runId) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select u.lot.id from MicroInventoryUsageLink l, InventoryUsage u"
                        + " where l.inventoryUsageId = u.id and l.usageContext = :usageContext"
                        + " and l.astRunId = :runId order by u.lot.id", Long.class);
        query.setParameter("usageContext", MicroInventoryUsageContext.AST_SETUP.name());
        query.setParameter("runId", runId);
        return query.list();
    }
}
