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

    /**
     * Batch load PanelLabUnits for multiple panel IDs in a single query to avoid
     * N+1 query problem.
     *
     * @param panelIds list of panel IDs (as integers)
     * @return list of all PanelLabUnit records for the given panel IDs
     */
    List<PanelLabUnit> getPanelLabUnitsByPanelIds(List<Integer> panelIds);
}
