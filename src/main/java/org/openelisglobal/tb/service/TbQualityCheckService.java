package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbQualityCheck;

/**
 * Service interface for TB quality check operations.
 */
public interface TbQualityCheckService extends BaseObjectService<TbQualityCheck, Integer> {

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
