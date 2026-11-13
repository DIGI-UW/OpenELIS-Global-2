package org.openelisglobal.microbiology.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetAntibiotic;

public interface MicroWhonetAntibioticDAO extends BaseDAO<MicroWhonetAntibiotic, String> {
    void deleteAllRows();
}
