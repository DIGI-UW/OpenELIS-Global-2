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
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocationProvider implements IResourceProvider {

    @Autowired
    private FhirUtil util;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Location.class;
    }

    @Read
    public Location readLocation(@IdParam IdType theId) {
        String method = "readLocation";
        try {
            FhirProviderUtils.validateIdParam(theId, "Location", this.getClass().getSimpleName(), method);
            Location location = util.getLocalFhirClient().read().resource(Location.class).withId(theId.getIdPart())
                    .execute();
            if (location == null) {
                throw new ResourceNotFoundException("Location/" + theId.getIdPart());
            }
            return location;
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading Location: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while reading Location", e);
        }
    }

    @Create
    public MethodOutcome createLocation(@ResourceParam Location fhirLocation) {
        String method = "createLocation";
        try {
            if (fhirLocation == null) {
                throw new InvalidRequestException("Location resource cannot be null");
            }
            if (!fhirLocation.hasId()) {
                fhirLocation.setId(UUID.randomUUID().toString());
            }

            util.getLocalFhirClient().update().resource(fhirLocation).execute();
            Location saved = util.getLocalFhirClient().read().resource(Location.class)
                    .withId(fhirLocation.getIdElement().getIdPart()).execute();

            return FhirProviderUtils.buildCreateOutcome(saved);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Location: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while creating Location", e);
        }
    }

    @Update
    public MethodOutcome updateLocation(@IdParam IdType theId, @ResourceParam Location fhirLocation) {
        String method = "updateLocation";
        try {
            FhirProviderUtils.validateIdParam(theId, "Location", this.getClass().getSimpleName(), method);
            if (fhirLocation == null) {
                throw new InvalidRequestException("Location resource cannot be null");
            }

            fhirLocation.setId(theId.getIdPart());
            util.getLocalFhirClient().update().resource(fhirLocation).execute();

            Location updated = util.getLocalFhirClient().read().resource(Location.class).withId(theId.getIdPart())
                    .execute();
            return FhirProviderUtils.buildUpdateOutcome(updated);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Location: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while updating Location", e);
        }
    }

    @Delete
    public MethodOutcome deleteLocation(@IdParam IdType theId) {
        String method = "deleteLocation";
        try {
            FhirProviderUtils.validateIdParam(theId, "Location", this.getClass().getSimpleName(), method);
            util.getLocalFhirClient().delete().resourceById("Location", theId.getIdPart()).execute();
            return FhirProviderUtils.buildDeleteOutcome(theId, "Location");
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Location: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting Location", e);
        }
    }

    @Search
    public Bundle searchLocationBundle(
            @OptionalParam(name = Location.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Location.SP_NAME) StringParam name,
            @OptionalParam(name = Location.SP_ADDRESS) StringAndListParam address,
            @OptionalParam(name = Location.SP_ADDRESS_CITY) StringParam addressCity,
            @OptionalParam(name = Location.SP_ADDRESS_STATE) StringParam addressState,
            @OptionalParam(name = Location.SP_ADDRESS_POSTALCODE) StringParam addressPostalCode,
            @OptionalParam(name = Location.SP_ADDRESS_COUNTRY) StringParam addressCountry,
            @OptionalParam(name = Location.SP_STATUS) TokenParam status,
            @OptionalParam(name = Location.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Location.SP_ORGANIZATION) ReferenceAndListParam organization,
            @OptionalParam(name = Location.SP_PARTOF) ReferenceAndListParam partOf,
            @OptionalParam(name = Location.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
            @IncludeParam(allow = { "Location:" + Location.SP_PARTOF, "Location:" + Location.SP_ORGANIZATION }) HashSet<Include> includes,
            HttpServletRequest request) {
        String method = "searchLocationBundle";
        try {
            return util.forwardSearchToFhirStore(request);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching Locations: " + e.getMessage());
            throw new InternalErrorException("Error searching Locations", e);
        }
    }
}
