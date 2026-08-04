package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicroOrganismDAO extends BaseDAO<MicroOrganism, String> {
    List<MicroOrganism> getActiveOrganisms();

    Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName);

    Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode);

    long countWorkflowReferences(String organismId);
}
