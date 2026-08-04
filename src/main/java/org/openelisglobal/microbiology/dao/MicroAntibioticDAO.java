package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;

public interface MicroAntibioticDAO extends BaseDAO<MicroAntibiotic, String> {
    List<MicroAntibiotic> getActiveAntibiotics();

    Optional<MicroAntibiotic> findByDisplayNameIgnoreCase(String displayName);

    Optional<MicroAntibiotic> findByWhonetCodeIgnoreCase(String whonetCode);

    long countWorkflowReferences(String antibioticId);
}
