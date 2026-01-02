package org.openelisglobal.panellabunit.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;

public interface PanelLabUnitService extends BaseObjectService<PanelLabUnit, String> {
    /**
     * Get all PanelLabUnit records for a given panel ID.
     *
     * @param panelId the panel ID
     * @return list of PanelLabUnit records
     */
    List<PanelLabUnit> getPanelLabUnitsByPanelId(String panelId);
}
