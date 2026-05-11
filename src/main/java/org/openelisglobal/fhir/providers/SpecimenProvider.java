package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpecimenProvider implements IResourceProvider {

    @Autowired
    private FhirUtil util;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private IStatusService statusService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Specimen.class;
    }

    @Read
    public Specimen getSpecimen(@IdParam IdType theId) {

        final String method = "getSpecimen";

        try {
            FhirProviderUtils.validateIdParam(theId, "Specimen", this.getClass().getSimpleName(), method);

            String specimenUuid = theId.getIdPart();

            if (specimenUuid == null || specimenUuid.isBlank()) {
                throw new InvalidRequestException("Specimen ID cannot be null or blank");
            }

            // Retrieve entity
            SampleItem sampleItem = fhirTransformService.getItemByFhirId(specimenUuid, sampleItemService);

            // Explicit null handling
            if (sampleItem == null) {
                throw new ResourceNotFoundException("Specimen with ID '" + specimenUuid + "' was not found");
            }

            // Transform entity -> FHIR resource
            Specimen specimen = fhirTransformService.transformToSpecimen(sampleItem);

            if (specimen == null) {
                throw new InternalErrorException("Failed to transform SampleItem into Specimen resource");
            }

            return specimen;

        } catch (ResourceNotFoundException | InvalidRequestException e) {

            throw e;

        } catch (IllegalArgumentException e) {

            LogEvent.logError(this.getClass().getSimpleName(), method, "Invalid UUID format: " + e.getMessage());

            throw new InvalidRequestException("Specimen ID must be a valid UUID", e);

        } catch (NullPointerException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Null pointer while reading Specimen: " + e.getMessage());

            throw new InternalErrorException("A required value was unexpectedly null while processing the request", e);

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while reading Specimen", e);
        }
    }

    @Create
    public MethodOutcome createSpecimen(@ResourceParam Specimen specimen, HttpServletRequest request) {

        final String method = "createSpecimen";

        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Specimen");

        try {

            if (specimen == null) {
                throw new InvalidRequestException("Specimen resource cannot be null");
            }

            if (specimen.getIdElement().isEmpty()) {
                specimen.setId(UUID.randomUUID().toString());
            }

            String sysUserId = FhirProviderUtils.getSysUserId(request);

            if (sysUserId == null) {
                throw new InternalErrorException("Unable to resolve authenticated system user");
            }

            SampleItem sampleItem = fhirTransformService.createSampleItemFromSpecimen(specimen, sysUserId);

            if (sampleItem == null) {
                throw new UnprocessableEntityException("Failed to create internal SampleItem from Specimen");
            }
            SampleItem savedItem = sampleItemService.save(sampleItem);

            if (savedItem == null) {
                throw new InternalErrorException("SampleItem save operation returned null");
            }

            Specimen savedSpecimen = fhirTransformService.transformToSpecimen(savedItem);

            if (savedSpecimen == null) {
                throw new InternalErrorException("Failed to transform saved SampleItem into Specimen");
            }

            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, savedSpecimen, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Specimen with UUID: " + savedItem.getFhirUuidAsString());

            return FhirProviderUtils.buildCreateOutcome(savedSpecimen);

        } catch (BaseServerResponseException e) {

            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Specimen", e);
        }
    }

    @Update
    public MethodOutcome updateSpecimen(@IdParam IdType theId, @ResourceParam Specimen specimen,
            HttpServletRequest request) {

        final String method = "updateSpecimen";

        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR UPDATE request for Specimen");

        try {
            if (theId == null || theId.getIdPart() == null || theId.getIdPart().isBlank()) {

                throw new InvalidRequestException("Specimen ID is required for update");
            }

            String specimenUuid = theId.getIdPart();

            try {
                UUID.fromString(specimenUuid);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Specimen ID must be a valid UUID");
            }

            if (specimen == null) {

                throw new InvalidRequestException("Specimen resource cannot be null");
            }
            if (!specimen.getIdElement().isEmpty()) {

                String bodyId = specimen.getIdElement().getIdPart();

                if (bodyId != null && !specimenUuid.equals(bodyId)) {

                    throw new InvalidRequestException("Resource ID in request body does not match URL ID");
                }
            }
            specimen.setId(specimenUuid);

            SampleItem existingItem = fhirTransformService.getItemByFhirId(specimenUuid, sampleItemService);

            if (existingItem == null) {

                throw new ResourceNotFoundException("Specimen with ID '" + specimenUuid + "' not found");
            }

            String sysUserId = FhirProviderUtils.getSysUserId(request);

            if (sysUserId == null) {

                throw new InternalErrorException("Unable to resolve authenticated system user");
            }
            SampleItem sampleItem = fhirTransformService.createSampleItemFromSpecimen(specimen, sysUserId);

            if (sampleItem == null) {

                throw new UnprocessableEntityException("Failed to transform Specimen into SampleItem");
            }

            sampleItem.setId(existingItem.getId());

            SampleItem updatedItem = sampleItemService.update(sampleItem);

            if (updatedItem == null) {

                throw new InternalErrorException("SampleItem update operation returned null");
            }

            Specimen updatedSpecimen = fhirTransformService.transformToSpecimen(updatedItem);

            if (updatedSpecimen == null) {

                throw new InternalErrorException("Failed to transform updated SampleItem into Specimen");
            }

            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, updatedSpecimen, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Specimen with UUID: " + updatedItem.getFhirUuidAsString());

            return FhirProviderUtils.buildUpdateOutcome(updatedSpecimen);

        } catch (BaseServerResponseException e) {

            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Specimen", e);
        }
    }

    @Delete
    public MethodOutcome deleteSpecimen(@IdParam IdType theId, HttpServletRequest request) {

        final String method = "deleteSpecimen";

        try {
            if (theId == null || theId.getIdPart() == null) {
                throw new InvalidRequestException("Specimen ID must be provided for deletion");
            }

            String specimenUuid = theId.getIdPart();

            SampleItem existingItem = fhirTransformService.getItemByFhirId(specimenUuid, sampleItemService);

            if (existingItem == null) {
                throw new ResourceNotFoundException("Specimen with ID " + specimenUuid + " not found");
            }

            existingItem.setStatusId(statusService.getStatusID(SampleStatus.Canceled));
            existingItem.setRejected(true);
            existingItem.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Dictionary rejectReason = dictionaryService
                    .getDictionaryByDictEntry("Free sample request form or vice versa. Please submit another sample.");

            if (rejectReason == null) {
                throw new InternalErrorException("Reject reason dictionary entry not configured");
            }
            existingItem.setRejectReasonId(rejectReason.getId());

            SampleItem updatedItem = sampleItemService.update(existingItem);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Specimen soft-deleted (rejected) with UUID: " + updatedItem.getFhirUuidAsString());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Specimen");

        } catch (UnprocessableEntityException | InvalidRequestException | ResourceNotFoundException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while deleting Specimen", e);
        }
    }

    @Search
    public Bundle searchSpecimenBundle(@OptionalParam(name = Specimen.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Specimen.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = Specimen.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Specimen.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = Specimen.SP_ACCESSION) TokenAndListParam accession,
            @OptionalParam(name = Specimen.SP_COLLECTED) DateRangeParam collected,
            @OptionalParam(name = Specimen.SP_CONTAINER) TokenAndListParam container,
            @OptionalParam(name = "_id") StringParam id,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,

            @IncludeParam(allow = { "ServiceRequest:" + ServiceRequest.SP_PATIENT,
                    "ServiceRequest:" + ServiceRequest.SP_SUBJECT,
                    "ServiceRequest:" + ServiceRequest.SP_REQUESTER }) HashSet<Include> includes,

            @IncludeParam(reverse = true, allow = { "Observation:based-on" }) HashSet<Include> revIncludes,

            HttpServletRequest request) {

        String methodName = "searchSpecimenBundle";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Specimens (returning Bundle)");

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Practitioners: " + e.getMessage());
            throw new InternalErrorException("Error searching Practitioners", e);
        }
    }
}
