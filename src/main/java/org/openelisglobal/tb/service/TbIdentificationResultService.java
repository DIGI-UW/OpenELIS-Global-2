package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationResult;
import org.openelisglobal.tb.valueholder.TbIdentificationResult;

/**
 * Service interface for TB identification result operations.
 */
public interface TbIdentificationResultService extends BaseObjectService<TbIdentificationResult, Integer> {

    /**
     * Find identification result by sample item ID.
     */
    Optional<TbIdentificationResult> findBySampleItemId(String sampleItemId);

    /**
     * Find all samples with a specific identification result.
     */
    List<TbIdentificationResult> findByResult(IdentificationResult result);

    /**
     * Find all MTB positive samples.
     */
    List<TbIdentificationResult> findMtbPositive();

    /**
     * Find identification results tested by a specific user.
     */
    List<TbIdentificationResult> findByTestedBy(Integer userId);

    /**
     * Count samples by identification result for reporting.
     */
    Long countByResult(IdentificationResult result);
}
