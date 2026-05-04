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
        String method = "getSpecimen";

        try {
            FhirProviderUtils.validateIdParam(theId, "Specimen", this.getClass().getSimpleName(), method);

            String specimenUuid = theId.getIdPart();

            SampleItem sampleItem = fhirTransformService.getItemByFhirId(specimenUuid, sampleItemService);

            Specimen specimen = fhirTransformService.transformToSpecimen(sampleItem);
            if (specimen == null) {
                throw new InternalErrorException("Failed to transform Analysis to Specimen");
            }
            return specimen;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Specimen ID must be a valid UUID");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Specimen : " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Specimen", e);
        }
    }

    @Create
    public MethodOutcome createSpecimen(@ResourceParam Specimen specimen, HttpServletRequest request) {
        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Specimen");

        try {

            if (specimen == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Practitioner resource is null");
                throw new InvalidRequestException("Practitioner resource cannot be null");

            } else if (specimen.getIdElement().getIdPart() == null) {
                specimen.setId(UUID.randomUUID().toString());
            }

            SampleItem sampleItem = fhirTransformService.createSampleItemFromSpecimen(specimen,
                    FhirProviderUtils.getSysUserId(request));
            SampleItem sampleItemTosave = sampleItemService.save(sampleItem);

            Specimen specimenToSave = fhirTransformService.transformToSpecimen(sampleItemTosave);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, specimenToSave, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Specimen with UUID: " + sampleItemTosave.getFhirUuidAsString());

            return FhirProviderUtils.buildCreateOutcome(specimenToSave);

        } catch (UnprocessableEntityException | InvalidRequestException e) {
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
        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR UPDATE request for Specimen");

        try {

            if (specimen == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Practitioner resource is null");
                throw new InvalidRequestException("Practitioner resource cannot be null");

            } else if (specimen.getIdElement().getIdPart() == null) {
                specimen.setId(UUID.randomUUID().toString());
            }

            SampleItem sampleItem = fhirTransformService.createSampleItemFromSpecimen(specimen,
                    FhirProviderUtils.getSysUserId(request));
            SampleItem sampleItemToUpdate = sampleItemService.update(sampleItem);

            Specimen specimenToSave = fhirTransformService.transformToSpecimen(sampleItemToUpdate);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, specimenToSave, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Specimen with UUID: " + sampleItemToUpdate.getFhirUuidAsString());

            return FhirProviderUtils.buildUpdateOutcome(specimenToSave);

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Specimen", e);
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
            existingItem.setRejectReasonId(dictionaryService
                    .getDictionaryByDictEntry("Free sample request form or vice versa. Please submit another sample.")
                    .getId());

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
