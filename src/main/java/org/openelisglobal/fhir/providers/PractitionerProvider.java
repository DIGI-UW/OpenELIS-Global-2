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
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.search.service.PractitionerSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerProvider implements IResourceProvider {

    @Autowired
    private PractitionerSearchService practitionerSearchService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private ProviderService providerService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Practitioner.class;
    }

    @Read
    public Practitioner getPractitionerByUUID(@IdParam IdType theId) {
        String method = "Read";
        try {
            Provider provider = providerService
                    .getProviderByFhirId(FhirProviderUtils.requireUuidId(theId, "Practitioner"));
            if (provider == null) {
                throw new ResourceNotFoundException("Provider is null " + theId.getIdPart());
            }
            Practitioner practitioner = fhirTransformService.transformProviderToPractitioner(provider);
            return practitioner;
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Practitioner", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Practitioner: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Practitioner", e);

        }

    }

    @Search
    public IBundleProvider searchPractitioner(

            @OptionalParam(name = Practitioner.SP_RES_ID) TokenAndListParam id,

            @OptionalParam(name = Practitioner.SP_IDENTIFIER) TokenAndListParam identifier,

            @OptionalParam(name = Practitioner.SP_NAME) StringAndListParam name,

            @OptionalParam(name = Practitioner.SP_GIVEN) StringAndListParam given,

            @OptionalParam(name = Practitioner.SP_FAMILY) StringAndListParam family,

            @OptionalParam(name = Practitioner.SP_ADDRESS_CITY) StringAndListParam city,

            @OptionalParam(name = Practitioner.SP_ADDRESS_STATE) StringAndListParam state,

            @OptionalParam(name = Practitioner.SP_ADDRESS_POSTALCODE) StringAndListParam postalCode,

            @OptionalParam(name = Practitioner.SP_ADDRESS_COUNTRY) StringAndListParam country,

            @OptionalParam(name = Practitioner.SP_TELECOM) TokenAndListParam telecom,

            @OptionalParam(name = Practitioner.SP_EMAIL) TokenAndListParam email,

            @OptionalParam(name = Practitioner.SP_PHONE) TokenAndListParam phone,

            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,

            @Sort SortSpec sort, @Offset Integer offset, @Count Integer count,

            @IncludeParam(reverse = true, allow = { FhirConstants.SERVICE_REQUEST_REQUESTER_REV_INCLUDE,
                    FhirConstants.OBSERVATION_PERFORMER_REV_INCLUDE }) HashSet<Include> revIncludes,

            HttpServletRequest request) {

        final String methodName = "searchPractitioner";

        LogEvent.logDebug(getClass().getSimpleName(), methodName, "Searching for Practitioners");

        try {
            PractitionerSearchParams params = new PractitionerSearchParams(identifier, name, given, family, city, state,
                    postalCode, country, telecom, email, phone, id, lastUpdated, sort, revIncludes);

            return FhirProviderUtils.withPaging(practitionerSearchService.searchPractitioners(params), offset, count);

        } catch (InvalidRequestException exception) {
            throw exception;

        } catch (IllegalArgumentException exception) {
            LogEvent.logError(getClass().getSimpleName(), methodName,
                    "Invalid Practitioner search parameter: " + exception.getMessage());

            throw new InvalidRequestException("Invalid Practitioner search parameter: " + exception.getMessage(),
                    exception);

        } catch (Exception exception) {
            LogEvent.logError(getClass().getSimpleName(), methodName,
                    "Error searching Practitioners: " + exception.getMessage());

            throw new InternalErrorException("Error searching Practitioners", exception);
        }
    }
}
