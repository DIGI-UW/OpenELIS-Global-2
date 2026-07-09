package org.openelisglobal.eqa.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAResult;

public interface EQAResultDAO extends BaseDAO<EQAResult, Long> {

    List<EQAResult> findByDistributionId(Long distributionId);

    Optional<EQAResult> findByDistributionAndOrgAndTest(Long distributionId, Long organizationId, Long testId);

    long countByDistributionId(Long distributionId);
}
