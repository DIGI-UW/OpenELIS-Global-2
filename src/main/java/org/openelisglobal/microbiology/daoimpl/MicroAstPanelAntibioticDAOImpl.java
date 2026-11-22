package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstPanelAntibioticDAOImpl extends BaseDAOImpl<MicroAstPanelAntibiotic, String>
        implements MicroAstPanelAntibioticDAO {

    public MicroAstPanelAntibioticDAOImpl() {
        super(MicroAstPanelAntibiotic.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanelAntibiotic> getByPanelId(String panelId) {
        Query<MicroAstPanelAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstPanelAntibiotic p where p.panelId = :panelId order by p.displayOrder, p.id",
                MicroAstPanelAntibiotic.class);
        query.setParameter("panelId", panelId);
        return query.list();
    }
}
