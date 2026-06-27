package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureSetupDAOImpl extends BaseDAOImpl<MicroCultureSetup, String> implements MicroCultureSetupDAO {

    public MicroCultureSetupDAOImpl() {
        super(MicroCultureSetup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCultureSetup> getActiveSetupsByWorkflowType(String workflowType) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCultureSetup c where c.isActive = 'Y' and c.workflowType = :workflowType"
                        + " order by c.name", MicroCultureSetup.class);
        query.setParameter("workflowType", workflowType);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCultureSetup getActiveSetupForMethod(String methodId, String workflowType) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCultureSetup c where c.isActive = 'Y' and c.methodId = :methodId"
                        + " and c.workflowType = :workflowType", MicroCultureSetup.class);
        query.setParameter("methodId", methodId);
        query.setParameter("workflowType", workflowType);
        return query.uniqueResultOptional().orElse(null);
    }
}
