package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicroOrganismDAO extends BaseDAO<MicroOrganism, String> {
    List<MicroOrganism> getActiveOrganisms();

    List<MicroOrganism> getByIds(List<String> ids);

    Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName);

    Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode);

    long countWorkflowReferences(String organismId);

    List<MicroOrganism> search(String q, String status, String category, String sort, int offset, int limit);

    long countSearch(String q, String status, String category);
}
