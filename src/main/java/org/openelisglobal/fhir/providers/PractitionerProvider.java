package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import java.util.UUID;
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

    @Read
    public Practitioner getPractitionerById(@IdParam IdType theId) throws FhirLocalPersistingException {
        String resourceId = theId.getIdPart();
        Provider provider = providerService.getProviderByFhirId(UUID.fromString(resourceId));
        Practitioner practitioner = fhirTransformService.transformProviderToPractitioner(provider);
        return practitioner;
    }

    @Create
    public MethodOutcome create(@ResourceParam Practitioner practitioner) throws FhirLocalPersistingException {

        // Persist domain object
        Provider provider = fhirTransformService.transformToProvider(practitioner);
        providerService.insert(provider);

        // Ensure FHIR ID exists
        if (!practitioner.hasId()) {
            practitioner.setId(new IdType("Practitioner", provider.getFhirUuidAsString()));
        }

        // Persist FHIR resource
        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

        // ✅ REQUIRED MethodOutcome
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

        // Ensure ID consistency
        practitioner.setId(theId);

        // Persist locally
        Provider provider = fhirTransformService.transformToProvider(practitioner);
        providerService.save(provider);

        // Persist to FHIR store
        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

        // ✅ REQUIRED MethodOutcome fields
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(practitioner.getIdElement());
        outcome.setResource(practitioner);
        outcome.setCreated(false); // REQUIRED for UPDATE
        outcome.setResponseStatusCode(200); // REQUIRED

        return outcome; // ❗ MUST return THIS object
    }

}