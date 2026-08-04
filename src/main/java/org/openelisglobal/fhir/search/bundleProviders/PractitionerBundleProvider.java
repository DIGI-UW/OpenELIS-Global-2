package org.openelisglobal.fhir.search.bundleProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
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

        int pageSize = toIndex - fromIndex;

        if (pageSize == 0) {
            return List.of();
        }

        /*
         * Step 1: Load only the current page of matching Providers.
         */
        List<Provider> providers = practitionerSearchDao.search(searchParams, fromIndex, pageSize);

        List<IBaseResource> resources = new ArrayList<>();

        /*
         * Step 2: Transform the Providers into matching Practitioners.
         */
        providers.stream().map(this::transformEntity).forEach(resources::add);

        /*
         * Step 3: Return only Practitioners when _revinclude was not requested.
         */
        if (!hasServiceRequestRequesterRevInclude()) {
            return resources;
        }

        /*
         * Step 4: Find SampleHuman rows connected to these Providers.
         *
         * The DAO internally extracts Provider.id.
         */
        List<SampleHuman> sampleHumans = sampleHumanSearchDao.findByProviders(providers);

        if (sampleHumans.isEmpty()) {
            return resources;
        }

        /*
         * Step 5: Find Analysis records connected to the SampleHuman samples.
         *
         * The DAO internally extracts SampleHuman.sampleId.
         */
        List<Analysis> analyses = serviceRequestSearchDao.findBySampleHumans(sampleHumans);

        /*
         * Step 6: Transform Analysis records into ServiceRequests.
         */
        analyses.forEach(analysis -> {
            ServiceRequest serviceRequest = fhirTransformService.transformToServiceRequest(analysis.getId());

            if (serviceRequest != null) {
                resources.add(serviceRequest);
            }
        });

        return resources;
    }

    private boolean hasServiceRequestRequesterRevInclude() {

        return searchParams != null && searchParams.hasRevInclude(FhirConstants.SERVICE_REQUEST_REQUESTER_REV_INCLUDE);
    }
}