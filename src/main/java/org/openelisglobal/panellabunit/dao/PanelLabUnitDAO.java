package org.openelisglobal.panellabunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;

public interface PanelLabUnitDAO extends BaseDAO<PanelLabUnit, String> {
    /**
     * Get all PanelLabUnit records for a given panel ID.
     *
     * @param panelId the panel ID
     * @return list of PanelLabUnit records
     * @throws LIMSRuntimeException
     */
    List<PanelLabUnit> getPanelLabUnitsByPanelId(String panelId) throws LIMSRuntimeException;

    /**
     * Batch load PanelLabUnits for multiple panel IDs in a single query to avoid
     * N+1 query problem.
     *
     * @param panelIds list of panel IDs (as integers)
     * @return list of all PanelLabUnit records for the given panel IDs
     * @throws LIMSRuntimeException
     */
    List<PanelLabUnit> getPanelLabUnitsByPanelIds(List<Integer> panelIds) throws LIMSRuntimeException;
}
