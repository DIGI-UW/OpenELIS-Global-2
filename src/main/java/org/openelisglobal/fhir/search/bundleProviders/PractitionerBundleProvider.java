package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

public class PractitionerBundleProvider extends BaseFhirBundleProvider<Provider, Practitioner> {

    private final PractitionerSearchParams searchParams;

    private final PractitionerSearchDao practitionerSearchDao;

    private final SampleHumanSearchDao sampleHumanSearchDao;

    private final ServiceRequestSearchDao serviceRequestSearchDao;

    private final FhirTransformService fhirTransformService;

    public PractitionerBundleProvider(PractitionerSearchParams searchParams,
            PractitionerSearchDao practitionerSearchDao, SampleHumanSearchDao sampleHumanSearchDao,
            ServiceRequestSearchDao serviceRequestSearchDao, FhirTransformService fhirTransformService) {

        this.searchParams = searchParams;

        this.practitionerSearchDao = Objects.requireNonNull(practitionerSearchDao,
                "PractitionerSearchDao must not be null");

        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");

        this.serviceRequestSearchDao = Objects.requireNonNull(serviceRequestSearchDao,
                "ServiceRequestSearchDao must not be null");

        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<Provider> loadEntities(int offset, int pageSize) {

        return practitionerSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {

        return practitionerSearchDao.count(searchParams);
    }

    @Override
    protected Practitioner transformEntity(Provider provider) {

        return fhirTransformService.transformProviderToPractitioner(provider);
    }

    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {

        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must be zero or greater");
        }

        if (toIndex < fromIndex) {
            throw new IllegalArgumentException("toIndex must be greater than or equal to fromIndex");
        }

        int offset = effectiveOffset(fromIndex);
        int pageSize = effectivePageSize(fromIndex, toIndex);

        if (pageSize == 0) {
            return List.of();
        }

        /*
         * Step 1: Load only the current page of matching Providers.
         */
        List<Provider> providers = practitionerSearchDao.search(searchParams, offset, pageSize);

        List<IBaseResource> resources = new ArrayList<>();
        providers.stream().map(this::transformEntity).filter(Objects::nonNull).forEach(resources::add);
        resources.forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.MATCH));

        boolean wantsServiceRequests = searchParams != null
                && searchParams.hasRevInclude(FhirConstants.SERVICE_REQUEST_REQUESTER_REV_INCLUDE);
        if (providers.isEmpty() || !wantsServiceRequests) {
            return resources;
        }
        int matchCount = resources.size();

        List<SampleHuman> sampleHumans = sampleHumanSearchDao.findByProviders(providers);
        if (sampleHumans.isEmpty()) {
            return resources;
        }
        List<Analysis> analyses = serviceRequestSearchDao.findBySampleHumans(sampleHumans);

        analyses.stream().map(analysis -> fhirTransformService.transformToServiceRequest(analysis.getId()))
                .filter(Objects::nonNull).forEach(resources::add);

        resources.subList(matchCount, resources.size()).forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.INCLUDE));
        return resources;
    }
}