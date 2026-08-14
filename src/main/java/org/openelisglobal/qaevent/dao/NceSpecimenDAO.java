package org.openelisglobal.qaevent.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;

public interface NceSpecimenDAO extends BaseDAO<NceSpecimen, Integer> {

    List<NceSpecimen> getSpecimenByNceId(Integer nceId) throws LIMSRuntimeException;

    List<NceSpecimen> getSpecimenBySampleId(Integer sampleId);

    boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId);

    /**
     * Of the given analyses, those linked to a still-open QC-failure NCE — the
     * Validation QC-fail signal (OGC-1147 FR-C1). Batched: one query per validation
     * list, never one per row.
     */
    List<Integer> findAnalysisIdsWithOpenQcHold(Collection<Integer> analysisIds);
}
