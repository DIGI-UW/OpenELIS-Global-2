package org.openelisglobal.panellabunit.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panellabunit.dao.PanelLabUnitDAO;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelLabUnitServiceImpl extends AuditableBaseObjectServiceImpl<PanelLabUnit, String>
        implements PanelLabUnitService {
    @Autowired
    protected PanelLabUnitDAO baseObjectDAO;

    PanelLabUnitServiceImpl() {
        super(PanelLabUnit.class);
    }

    @Override
    protected PanelLabUnitDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelLabUnit> getPanelLabUnitsByPanelId(String panelId) {
        return getBaseObjectDAO().getPanelLabUnitsByPanelId(panelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelLabUnit> getPanelLabUnitsByPanelIds(List<Integer> panelIds) {
        return getBaseObjectDAO().getPanelLabUnitsByPanelIds(panelIds);
    }
}
