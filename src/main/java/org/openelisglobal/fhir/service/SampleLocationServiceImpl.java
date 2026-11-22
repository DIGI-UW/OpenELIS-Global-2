package org.openelisglobal.fhir.service;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleLocationServiceImpl implements SampleLocationService {

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private VectorSamplingSiteService samplingSiteService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional(readOnly = true)
    public VectorSamplingSite getSamplingSite(Sample sample, SampleItem sampleItem) {
        if (sample == null) {
            return null;
        }
        String siteId = sampleItem != null ? sampleItem.getCollectionLocationId() : null;
        if (GenericValidator.isBlankOrNull(siteId)) {
            siteId = observationHistoryService.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_ID,
                    sample.getId());
        }
        if (GenericValidator.isBlankOrNull(siteId)) {
            siteId = observationHistoryService.getRawValueForSample(ObservationType.VS_COLLECTION_SITE_ID,
                    sample.getId());
        }
        if (GenericValidator.isBlankOrNull(siteId)) {
            return null;
        }
        try {
            return samplingSiteService.get(Integer.valueOf(siteId.trim()));
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplingSite", "sampling site '" + siteId
                    + "' of sample " + sample.getId() + " could not be read: " + e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getSamplingSiteOrganization(VectorSamplingSite site) {
        if (site == null || site.getOrganizationId() == null) {
            return null;
        }
        Organization organization = organizationService.get(String.valueOf(site.getOrganizationId()));
        return organization != null && organization.getFhirUuid() != null ? organization : null;
    }
}
