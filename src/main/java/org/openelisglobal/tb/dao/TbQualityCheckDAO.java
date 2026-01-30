package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbQualityCheck;

/**
 * Data access interface for TB quality checks.
 */
public interface TbQualityCheckDAO extends BaseDAO<TbQualityCheck, Integer> {

    /**
     * Find QC result by sample item ID.
     */
    Optional<TbQualityCheck> findBySampleItemId(String sampleItemId);

    /**
     * Find all QC results with a specific overall result.
     */
    List<TbQualityCheck> findByOverallResult(QcResult result);

    /**
     * Find all QC results checked by a specific user.
     */
    List<TbQualityCheck> findByCheckedBy(Integer userId);

    /**
     * Count samples by QC result for reporting.
     */
    Long countByOverallResult(QcResult result);
}
