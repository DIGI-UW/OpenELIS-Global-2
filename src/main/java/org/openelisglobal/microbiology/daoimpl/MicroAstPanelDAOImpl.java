package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstPanelDAOImpl extends BaseDAOImpl<MicroAstPanel, String> implements MicroAstPanelDAO {

    public MicroAstPanelDAOImpl() {
        super(MicroAstPanel.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> getActivePanelsByWorkflowType(String workflowType) {
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstPanel p where p.isActive = 'Y' and p.workflowType = :workflowType" + " order by p.name",
                MicroAstPanel.class);
        query.setParameter("workflowType", workflowType);
        return query.list();
    }
}
