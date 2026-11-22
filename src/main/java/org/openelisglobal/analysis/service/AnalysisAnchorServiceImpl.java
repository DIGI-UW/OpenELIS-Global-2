package org.openelisglobal.analysis.service;

import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisAnchorServiceImpl implements AnalysisAnchorService {

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private SampleService sampleService;

    @Override
    @Transactional(readOnly = true)
    public Sample resolveSample(Analysis analysis) {
        if (analysis == null) {
            return null;
        }
        if (analysis.getSampleItem() != null) {
            return analysis.getSampleItem().getSample();
        }
        return resolveSampleFromPool(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisAnchor resolveAnchor(Analysis analysis) {
        if (analysis == null) {
            return null;
        }
        if (analysis.getSampleItem() != null && analysis.getSampleItem().getSample() != null) {
            return new AnalysisAnchor(analysis.getSampleItem().getSample(), analysis.getSampleItem());
        }
        Sample sample = resolveSampleFromPool(analysis);
        if (sample == null) {
            return null;
        }
        return new AnalysisAnchor(sample, resolvePoolMember(analysis));
    }

    private Sample resolveSampleFromPool(Analysis analysis) {
        VectorPool pool = resolvePool(analysis);
        if (pool == null || GenericValidator.isBlankOrNull(pool.getSampleId())) {
            return null;
        }
        return sampleService.get(pool.getSampleId());
    }

    private SampleItem resolvePoolMember(Analysis analysis) {
        VectorPool pool = resolvePool(analysis);
        if (pool == null) {
            return null;
        }
        return vectorPoolService.getFirstNonVoidedMemberByPoolId(pool.getId()).orElse(null);
    }

    private VectorPool resolvePool(Analysis analysis) {
        String poolIdStr = analysis.getVectorPoolId();
        if (GenericValidator.isBlankOrNull(poolIdStr)) {
            return null;
        }
        try {
            return vectorPoolService.get(Integer.valueOf(poolIdStr));
        } catch (NumberFormatException | ObjectNotFoundException e) {
            // Deleted pool or malformed id: degrade gracefully so reporting /
            // display flows don't 500 on a single inconsistent analysis row.
            return null;
        }
    }
}
