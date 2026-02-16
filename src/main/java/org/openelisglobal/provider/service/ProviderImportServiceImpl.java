package org.openelisglobal.provider.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ProviderImportServiceImpl implements ProviderImportService {

    @Value("${org.openelisglobal.providerlist.fhirstore:}")
    private String providerFhirStore;

    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private PersonService personService;

    @Override
    @Async
    @Scheduled(initialDelay = 1000, fixedRateString = "${org.openelisglobal.providerlist.poll.frequency:3600000}")
    public void importPractitionerList() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        if (!GenericValidator.isBlankOrNull(providerFhirStore)) {
            IGenericClient client = fhirUtil.getFhirClient(providerFhirStore);

            List<Bundle> responseBundles = new ArrayList<>();
            Bundle responseBundle = client.search().forResource(org.hl7.fhir.r4.model.Practitioner.class)
                    .where(Practitioner.ACTIVE.exactly().code("true")).returnBundle(Bundle.class).execute();
            responseBundles.add(responseBundle);
            while (responseBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                responseBundle = client.loadPage().next(responseBundle).execute();
                responseBundles.add(responseBundle);
            }
            // providerService.deactivateAllProviders();
            importProvidersFromBundle(client, responseBundles);
        }
        DisplayListService.getInstance().refreshList(ListType.PRACTITIONER_PERSONS);
    }

    private void importProvidersFromBundle(IGenericClient client, List<Bundle> responseBundles)
            throws FhirGeneralException {
        Map<String, Resource> remoteFhirProviders = new HashMap<>();

        for (Bundle responseBundle : responseBundles) {
            for (BundleEntryComponent entry : responseBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource().getResourceType().equals(ResourceType.Practitioner)) {
                    org.hl7.fhir.r4.model.Practitioner fhirPractitioner = (org.hl7.fhir.r4.model.Practitioner) entry
                            .getResource();
                    try {
                        Provider provider = fhirTransformService.transformToProvider(fhirPractitioner);
                        if (providerService.getProviderByFhirId(provider.getFhirUuid()) == null
                                || !providerService.getProviderByFhirId(provider.getFhirUuid()).isDesynchronized()) {
                            providerService.insertOrUpdateProviderByFhirUuid(provider.getFhirUuid(), provider);
                            remoteFhirProviders.put(fhirPractitioner.getIdElement().getIdPart(), fhirPractitioner);
                        }

                    } catch (RuntimeException e) {
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "importProvidersFromBundle",
                                fhirContext.newJsonParser().encodeResourceToString(fhirPractitioner));
                    }
                }
            }
        }

        fhirPersistanceService.updateFhirResourcesInFhirStore(remoteFhirProviders);
    }
}
