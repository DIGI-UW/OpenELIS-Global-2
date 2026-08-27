package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;

public interface MicroAstPanelAntibioticDAO extends BaseDAO<MicroAstPanelAntibiotic, String> {

    List<MicroAstPanelAntibiotic> getByPanelId(String panelId);
}
