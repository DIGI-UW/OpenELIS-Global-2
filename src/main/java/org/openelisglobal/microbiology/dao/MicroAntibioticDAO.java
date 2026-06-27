package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;

public interface MicroAntibioticDAO extends BaseDAO<MicroAntibiotic, String> {
    List<MicroAntibiotic> getActiveAntibiotics();
}
