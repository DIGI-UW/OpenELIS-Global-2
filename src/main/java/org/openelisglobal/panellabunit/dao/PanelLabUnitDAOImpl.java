package org.openelisglobal.panellabunit.dao;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PanelLabUnitDAOImpl extends BaseDAOImpl<PanelLabUnit, String> implements PanelLabUnitDAO {
    PanelLabUnitDAOImpl() {
        super(PanelLabUnit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelLabUnit> getPanelLabUnitsByPanelId(String panelId) throws LIMSRuntimeException {
        List<PanelLabUnit> list = new ArrayList<>();
        String sql = "from PanelLabUnit plu where plu.panelId = :panelId";

        try {
            if (panelId == null || panelId.equals("null")) {
                // Handle null case
                panelId = "0";
            }
            Query<PanelLabUnit> query = entityManager.unwrap(Session.class).createQuery(sql, PanelLabUnit.class);
            // Parse String panelId to Integer for numeric column comparison (matches
            // TypeOfSamplePanelDAOImpl pattern)
            query.setParameter("panelId", Integer.parseInt(panelId));
            List<PanelLabUnit> panelLabUnits = query.list();
            return panelLabUnits;
        } catch (HibernateException e) {
            handleException(e, "getPanelLabUnitsByPanelId");
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelLabUnitDAOImpl getPanelLabUnitsByPanelId()", e);
        }

        return list;
    }
}
