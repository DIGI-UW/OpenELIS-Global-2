package org.openelisglobal.qaevent.service;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;

public interface NceSpecimenService extends BaseObjectService<NceSpecimen, Integer> {

    List<NceSpecimen> getSpecimenByNceId(Integer nceId);

    List<NceSpecimen> getSpecimenBySampleItemId(Integer sampleId);

    boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId);

    /**
     * Of the given analyses, those held by a still-open QC-failure NCE — the
     * Validation QC-fail signal (OGC-1147 FR-C1). Batched for one query per list.
     */
    List<Integer> findAnalysisIdsWithOpenQcHold(Collection<Integer> analysisIds);
}
