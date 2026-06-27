package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicroOrganismDAO extends BaseDAO<MicroOrganism, String> {
    List<MicroOrganism> getActiveOrganisms();
}
