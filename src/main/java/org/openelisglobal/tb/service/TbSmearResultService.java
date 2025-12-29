package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbEnums.AfbResult;
import org.openelisglobal.tb.valueholder.TbSmearResult;

/**
 * Service interface for TB smear result operations.
 */
public interface TbSmearResultService extends BaseObjectService<TbSmearResult, Integer> {

    /**
     * Find smear result by sample item ID.
     */
    Optional<TbSmearResult> findBySampleItemId(String sampleItemId);

    /**
     * Find all smear results with a specific AFB result.
     */
    List<TbSmearResult> findByAfbResult(AfbResult result);

    /**
     * Find all positive smear results (AFB detected).
     */
    List<TbSmearResult> findPositiveResults();

    /**
     * Find smear results tested by a specific user.
     */
    List<TbSmearResult> findByTestedBy(Integer userId);

    /**
     * Count smear results by AFB grading for reporting.
     */
    Long countByAfbResult(AfbResult result);
}
