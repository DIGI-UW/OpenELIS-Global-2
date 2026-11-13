package org.openelisglobal.microbiology.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;

public interface MicroWhonetOrganismDAO extends BaseDAO<MicroWhonetOrganism, String> {
    void deleteAllRows();
}
