package org.openelisglobal.eqa.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAResult;

public interface EQAResultDAO extends BaseDAO<EQAResult, Long> {

    List<EQAResult> findByDistributionId(Long distributionId);

    /** The row for a test that answers no panel sample. */
    Optional<EQAResult> findByDistributionAndOrgAndTest(Long distributionId, Long organizationId, Long testId);

    Optional<EQAResult> findByDistributionAndOrgAndPanelSample(Long distributionId, Long organizationId,
            Long panelSampleId);

    long countByDistributionId(Long distributionId);
}
