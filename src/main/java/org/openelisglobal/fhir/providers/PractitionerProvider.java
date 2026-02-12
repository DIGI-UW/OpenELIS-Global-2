package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistenceService;
    @Autowired
    private ProviderService providerService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Practitioner.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Practitioner practitioner) throws FhirLocalPersistingException {

        Provider provider = fhirTransformService.transformToProviderForPersistance(practitioner);

        providerService.insert(provider);

        practitioner.setId(new IdType("Practitioner", provider.getFhirUuidAsString()));

        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(practitioner.getIdElement());
        outcome.setResource(practitioner);
        outcome.setCreated(true);
        outcome.setResponseStatusCode(201);

        return outcome;
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Practitioner practitioner)
            throws FhirLocalPersistingException {
        practitioner.setId(theId);

        Provider provider = fhirTransformService.transformToProviderForUpdate(practitioner);
        providerService.save(provider);
        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(practitioner.getIdElement());
        outcome.setResource(practitioner);
        outcome.setCreated(false);
        outcome.setResponseStatusCode(200);

        return outcome;
    }

}