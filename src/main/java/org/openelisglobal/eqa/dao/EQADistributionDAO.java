package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;

public interface EQADistributionDAO extends BaseDAO<EQADistribution, Long> {

    List<EQADistribution> findByProgramId(Long programId);

    List<EQADistribution> findByStatus(EQADistributionStatus status);
}
