package org.openelisglobal.microbiology.service;

import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroOrderSiteService {
    private final VectorSamplingSiteService sites;
    private final ObservationHistoryService observations;

    public MicroOrderSiteService(VectorSamplingSiteService sites, ObservationHistoryService observations) {
        this.sites = sites;
        this.observations = observations;
    }

    public String resolve(Sample order, String specimenSiteId, Test test) {
        if (Domain.fromRaw(order.getDomain()) != Domain.ENVIRONMENTAL)
            return null;
        requireEnvironmentalUnit(test);
        String siteId = specimenSiteId;
        if (siteId == null || siteId.isBlank())
            siteId = observations.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_ID, order.getId());
        siteName(siteId);
        return siteId.trim();
    }

    public String siteName(String siteId) {
        if (siteId == null || !siteId.trim().matches("[1-9][0-9]*"))
            throw new IllegalArgumentException("An environmental sampling site is required");
        var site = sites.get(Integer.valueOf(siteId.trim()));
        if (site == null)
            throw new IllegalArgumentException("Unknown environmental sampling site");
        return site.getName();
    }

    public static void requireEnvironmentalUnit(Test test) {
        if (test == null || test.getTestSection() == null
                || Domain.fromRaw(test.getTestSection().getDomain()) != Domain.ENVIRONMENTAL)
            throw new IllegalArgumentException("Environmental microbiology requires an environmental lab unit");
    }
}
