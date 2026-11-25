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
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.LocationSearchParams;
import org.openelisglobal.fhir.service.OrganizationTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.search.service.LocationSearchService;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.service.StorageShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Read and search Location resources from OpenELIS. */
@Component
public class LocationProvider implements IResourceProvider {

    @Autowired
    private StorageLocationFhirTransform transform;

    @Autowired
    private StorageRoomService roomService;

    @Autowired
    private StorageDeviceService deviceService;

    @Autowired
    private StorageShelfService shelfService;

    @Autowired
    private StorageRackService rackService;

    @Autowired
    private StorageBoxService boxService;

    @Autowired
    private LocationSearchService locationSearchService;

    @Autowired
    private OrganizationService organizationService;

    @Lazy
    @Autowired
    private OrganizationTransformService organizationTransformService;

    private final List<StorageLevel<?>> levels = new ArrayList<>();

    /** Read a storage level through the existing transform. */
    private record StorageLevel<T extends BaseObject<Integer>>(BaseObjectService<T, Integer> service,
            Function<T, Location> toLocation) {
        Location read(UUID uuid, StorageLocationFhirTransform transform) {
            T entity = transform.getItemByFhirId(uuid, service);
            return entity == null ? null : toLocation.apply(entity);
        }
    }

    @PostConstruct
    void registerLevels() {
        levels.add(new StorageLevel<>(roomService, transform::transformToFhirLocation));
        levels.add(new StorageLevel<>(deviceService, transform::transformToFhirLocation));
        levels.add(new StorageLevel<>(shelfService, transform::transformToFhirLocation));
        levels.add(new StorageLevel<>(rackService, transform::transformToFhirLocation));
        levels.add(new StorageLevel<>(boxService, transform::transformToFhirLocation));
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Location.class;
    }

    @Read
    public Location readLocation(@IdParam IdType theId) {
        String method = "readLocation";
        try {
            UUID uuid = requireUuid(theId, method);
            for (StorageLevel<?> level : levels) {
                Location location = level.read(uuid, transform);
                if (location != null) {
                    return location;
                }
            }
            Location site = organizationTransformService
                    .transformToFhirLocation(organizationService.getOrganizationByFhirId(uuid.toString()));
            if (site != null) {
                return site;
            }
            throw new ResourceNotFoundException("Location/" + theId.getIdPart());
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Location", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading Location: " + FhirProviderUtils.safeMessage(e));
            throw new InternalErrorException("Unexpected server error while reading Location", e);
        }
    }

    @Search
    public IBundleProvider searchLocations(@OptionalParam(name = Location.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = Location.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Location.SP_NAME) StringAndListParam name,
            @OptionalParam(name = Location.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = Location.SP_PARTOF) ReferenceAndListParam partOf,
            @OptionalParam(name = "_tag") TokenAndListParam tag,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.LOCATION_PARTOF_INCLUDE }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = {
                    FhirConstants.LOCATION_PARTOF_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        final String methodName = "searchLocations";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Locations");

        try {
            LocationSearchParams params = new LocationSearchParams(id, identifier, name, status, partOf, tag,
                    lastUpdated, sort, includes, revIncludes);
            return FhirProviderUtils.withPaging(locationSearchService.searchLocations(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Invalid Location search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Location search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Location", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Locations: " + FhirProviderUtils.safeMessage(e));
            throw new InternalErrorException("Unexpected server error while searching Locations", e);
        }
    }

    private UUID requireUuid(IdType theId, String method) {
        FhirProviderUtils.validateIdParam(theId, "Location", getClass().getSimpleName(), method);
        try {
            return UUID.fromString(theId.getIdPart());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Location ID must be a valid UUID");
        }
    }

}
