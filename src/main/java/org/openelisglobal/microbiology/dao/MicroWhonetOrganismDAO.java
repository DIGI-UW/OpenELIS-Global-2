package org.openelisglobal.microbiology.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;

public interface MicroWhonetOrganismDAO extends BaseDAO<MicroWhonetOrganism, String> {
    void deleteAllRows();

    Optional<MicroWhonetOrganism> findCurrentByCode(String whonetOrgCode);
}
