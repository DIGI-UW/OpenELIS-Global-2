package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.SpecimenSearchParams;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.service.SpecimenSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpecimenProvider implements IResourceProvider {

    @Autowired
    private SpecimenSearchService specimenSearchService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private SampleItemService sampleItemService;

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
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Specimen", e);
            }

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading Specimen: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while reading Specimen", e);
        }
    }

    @Search
    public IBundleProvider searchSpecimens(@OptionalParam(name = Specimen.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = Specimen.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Specimen.SP_ACCESSION) TokenAndListParam accession,
            @OptionalParam(name = Specimen.SP_PATIENT) ReferenceAndListParam patient,
            @OptionalParam(name = Specimen.SP_SUBJECT) ReferenceAndListParam subject,
            @OptionalParam(name = Specimen.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Specimen.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = Specimen.SP_COLLECTED) DateRangeParam collected,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.SPECIMEN_PATIENT_INCLUDE,
                    FhirConstants.SPECIMEN_SUBJECT_INCLUDE }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = { FhirConstants.SERVICE_REQUEST_SPECIMEN_REV_INCLUDE,
                    FhirConstants.OBSERVATION_SPECIMEN_REV_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_SPECIMEN_REV_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchSpecimens";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Specimens");

        try {
            SpecimenSearchParams params = new SpecimenSearchParams(id, identifier, accession,
                    FhirProviderUtils.merge(patient, subject), type, status, collected, lastUpdated, sort, includes,
                    revIncludes);
            return FhirProviderUtils.withPaging(specimenSearchService.searchSpecimens(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Invalid Specimen search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Specimen search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Specimen", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Specimens: " + e.getMessage());
            throw new InternalErrorException("Error searching Specimens", e);
        }
    }
}
