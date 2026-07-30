package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.service.StorageShelfService;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private StorageLocationService locationService;

    @Autowired
    private FhirUtil util;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Location.class;
    }

    @Read
    public Location readLocation(@IdParam IdType theId) {
        String method = "readLocation";
        if (theId == null || theId.getIdPart() == null || theId.getIdPart().isBlank()) {
            throw new ResourceNotFoundException("Valid Location ID is required");
        }

        try {
            return handleGetLocation(theId.getIdPart());
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Location: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Location", e);

        }
    }

    @Create
    public MethodOutcome createLocation(@ResourceParam Location location, HttpServletRequest request)
            throws InvalidRequestException {
        String method = "createLocation";

        if (location == null) {
            throw new InvalidRequestException("Location resource cannot be null");
        }

        try {
            Location created = persistOrUpdateLocation(location, FhirProviderUtils.getSysUserId(request), true);

            MethodOutcome outcome = new MethodOutcome();
            outcome.setResource(created);
            outcome.setId(new IdType("Location", created.getIdElement().getIdPart()));
            outcome.setCreated(true);

            return outcome;
        } catch (ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, safeMessage(e));
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, safeMessage(e));
            throw new InternalErrorException("Unexpected server error while creating Location: " + safeMessage(e), e);
        }
    }

    @Update
    public MethodOutcome updateLocation(@IdParam IdType id, @ResourceParam Location location,
            HttpServletRequest request) throws InvalidRequestException {
        String method = "updateLocation";

        if (id == null || id.getIdPart() == null || id.getIdPart().isBlank()) {
            throw new InvalidRequestException("Valid Location ID is required for update");
        }

        if (location == null) {
            throw new InvalidRequestException("Location resource cannot be null");
        }

        try {
            location.setId(id.getIdPart());
            Location updated = persistOrUpdateLocation(location, FhirProviderUtils.getSysUserId(request), false);

            MethodOutcome outcome = new MethodOutcome();
            outcome.setResource(updated);
            outcome.setId(id);
            outcome.setCreated(false);

            return outcome;
        } catch (ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, safeMessage(e));
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, safeMessage(e));
            throw new InternalErrorException("Unexpected server error while updating Location: " + safeMessage(e), e);
        }
    }

    @Delete
    public MethodOutcome deleteLocation(@IdParam IdType theId, HttpServletRequest request)
            throws InvalidRequestException {
        String method = "deleteLocation";

        if (theId == null || theId.getIdPart() == null || theId.getIdPart().isBlank()) {
            throw new InvalidRequestException("Valid Location ID is required for deletion");
        }

        try {
            String locationUuid = theId.getIdPart();
            Location location = handleDeleteLocation(locationUuid, FhirProviderUtils.getSysUserId(request));

            if (location == null) {
                throw new ResourceNotFoundException("Location not found for ID: " + locationUuid);
            }

            MethodOutcome outcome = new MethodOutcome();
            outcome.setCreated(false);
            outcome.setResponseStatusCode(204);

            return outcome;
        } catch (ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));
            throw e;
        } catch (InternalErrorException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Internal error: " + safeMessage(e));
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));
            throw new InternalErrorException("Unexpected server error while deleting Location: " + e.getMessage(), e);
        }
    }

    @Search
    public Bundle searchLocationBundle(

            @OptionalParam(name = Location.SP_IDENTIFIER) TokenAndListParam identifier,

            @OptionalParam(name = Location.SP_NAME) StringAndListParam name,

            @OptionalParam(name = Location.SP_STATUS) TokenParam status,

            @OptionalParam(name = Location.SP_PARTOF) ReferenceAndListParam partOf,

            @OptionalParam(name = Location.SP_ORGANIZATION) ReferenceAndListParam organization,

            @OptionalParam(name = "physical-type") TokenAndListParam physicalType,

            @OptionalParam(name = Location.SP_TYPE) TokenAndListParam type,

            @OptionalParam(name = "_tag") TokenAndListParam tag,

            HttpServletRequest request) {

        final String methodName = "searchLocationBundle";

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            if (bundle == null) {
                bundle = new Bundle();
                bundle.setType(Bundle.BundleType.SEARCHSET);
            }

            return bundle;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Locations: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while searching Locations", e);
        }
    }

    private Location persistOrUpdateLocation(Location location, String sysUserId, boolean isCreate)
            throws InvalidRequestException {

        if (location == null) {
            throw new InvalidRequestException("Location cannot be null");
        }

        if (sysUserId == null || sysUserId.isBlank()) {
            throw new InvalidRequestException("System user ID is required");
        }

        String category = getStorageCategory(location);

        try {
            switch (category) {
            case "Storage Room":
                return persistRoom(location, sysUserId, isCreate);
            case "Storage Equipment":
                return persistDevice(location, sysUserId, isCreate);
            case "Storage Shelf":
                return persistShelf(location, sysUserId, isCreate);
            case "Storage Rack":
                return persistRack(location, sysUserId, isCreate);
            case "Storage Box":
                return persistBox(location, sysUserId, isCreate);
            default:
                throw new InvalidRequestException("Unsupported Location type: " + category);
            }
        } catch (InvalidRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalErrorException("Error persisting Location: " + safeMessage(e), e);
        }
    }

    private Location persistRoom(Location location, String sysUserId, boolean isCreate) {
        StorageRoom room = transform.createStorageRoomFromLocation(location);
        if (room == null) {
            throw new InternalErrorException("Failed to create StorageRoom from Location");
        }
        room.setSysUserId(sysUserId);

        StorageRoom saved;
        if (isCreate) {
            saved = locationService.createRoom(room);
        } else {
            saved = locationService.updateRoom(room.getId(), room);
        }

        if (saved == null) {
            throw new InternalErrorException("Failed to save StorageRoom");
        }

        transform.syncToFhir(saved, isCreate);
        Location result = transform.transformToFhirLocation(saved);
        if (result == null) {
            throw new InternalErrorException("Failed to transform StorageRoom to FHIR Location");
        }
        return result;
    }

    private Location persistDevice(Location location, String sysUserId, boolean isCreate) {
        StorageDevice device = transform.createOrUpdateStorageDeviceFromLocation(location);
        if (device == null) {
            throw new InternalErrorException("Failed to create StorageDevice from Location");
        }
        device.setSysUserId(sysUserId);

        StorageDevice saved;
        if (isCreate) {
            Integer deviceId = locationService.insert(device);
            saved = (StorageDevice) locationService.get(deviceId, StorageDevice.class);
        } else {
            locationService.update(device);
            saved = (StorageDevice) locationService.get(device.getId(), StorageDevice.class);
        }

        if (saved == null) {
            throw new InternalErrorException("Failed to save StorageDevice");
        }

        transform.syncToFhir(saved, isCreate);
        Location result = transform.transformToFhirLocation(saved);
        if (result == null) {
            throw new InternalErrorException("Failed to transform StorageDevice to FHIR Location");
        }
        return result;
    }

    private Location persistShelf(Location location, String sysUserId, boolean isCreate) {
        StorageShelf shelf = transform.createOrUpdateStorageShelfFromLocation(location);
        if (shelf == null) {
            throw new InternalErrorException("Failed to create StorageShelf from Location");
        }
        shelf.setSysUserId(sysUserId);

        StorageShelf saved;
        if (isCreate) {
            Integer shelfId = locationService.insert(shelf);
            saved = (StorageShelf) locationService.get(shelfId, StorageShelf.class);
        } else {
            locationService.update(shelf);
            saved = (StorageShelf) locationService.get(shelf.getId(), StorageShelf.class);
        }

        if (saved == null) {
            throw new InternalErrorException("Failed to save StorageShelf");
        }

        transform.syncToFhir(saved, isCreate);
        Location result = transform.transformToFhirLocation(saved);
        if (result == null) {
            throw new InternalErrorException("Failed to transform StorageShelf to FHIR Location");
        }
        return result;
    }

    private Location persistRack(Location location, String sysUserId, boolean isCreate) {
        StorageRack rack = transform.createOrUpdateStorageRackFromLocation(location);
        if (rack == null) {
            throw new InternalErrorException("Failed to create StorageRack from Location");
        }
        rack.setSysUserId(sysUserId);

        StorageRack saved;
        if (isCreate) {
            Integer rackId = locationService.insert(rack);
            saved = (StorageRack) locationService.get(rackId, StorageRack.class);
        } else {
            locationService.update(rack);
            saved = (StorageRack) locationService.get(rack.getId(), StorageRack.class);
        }

        if (saved == null) {
            throw new InternalErrorException("Failed to save StorageRack");
        }

        transform.syncToFhir(saved, isCreate);
        Location result = transform.transformToFhirLocation(saved);
        if (result == null) {
            throw new InternalErrorException("Failed to transform StorageRack to FHIR Location");
        }
        return result;
    }

    private Location persistBox(Location location, String sysUserId, boolean isCreate) {
        StorageBox box = transform.createOrUpdateStorageBoxFromLocation(location);
        if (box == null) {
            throw new InternalErrorException("Failed to create StorageBox from Location");
        }
        box.setSysUserId(sysUserId);

        StorageBox saved;
        if (isCreate) {
            Integer boxId = locationService.insert(box);
            saved = (StorageBox) locationService.get(boxId, StorageBox.class);
        } else {
            locationService.update(box);
            saved = (StorageBox) locationService.get(box.getId(), StorageBox.class);
        }

        if (saved == null) {
            throw new InternalErrorException("Failed to save StorageBox");
        }

        transform.syncToFhir(saved, isCreate);
        Location result = transform.transformToFhirLocation(saved);
        if (result == null) {
            throw new InternalErrorException("Failed to transform StorageBox to FHIR Location");
        }
        return result;
    }

    private String getStorageCategory(Location location) throws InvalidRequestException {
        if (location == null) {
            throw new InvalidRequestException("Location cannot be null");
        }

        if (!location.hasPhysicalType()) {
            throw new InvalidRequestException("Location missing required field: physicalType");
        }

        String text = location.getPhysicalType().getText();

        if (text == null || text.isBlank()) {
            throw new InvalidRequestException("physicalType.text is required and cannot be blank");
        }

        return text.trim();
    }

    private Location handleGetLocation(String uuidString) {
        return transformStorageItem(findStorageItem(uuidString));
    }

    private Location handleDeleteLocation(String uuidString, String sysUserId) {
        validateSysUserId(sysUserId);

        StorageItem item = findStorageItem(uuidString);
        StorageItem updatedItem = deactivateStorageItem(item, sysUserId);

        syncStorageItem(updatedItem);
        return transformStorageItem(updatedItem);
    }

    private StorageItem findStorageItem(String uuidString) {
        UUID uuid = parseUuid(uuidString);

        StorageRoom room = transform.getItemByFhirId(uuid, roomService);
        if (room != null) {
            return new StorageItem(StorageType.ROOM, room);
        }

        StorageDevice device = transform.getItemByFhirId(uuid, deviceService);
        if (device != null) {
            return new StorageItem(StorageType.DEVICE, device);
        }

        StorageShelf shelf = transform.getItemByFhirId(uuid, shelfService);
        if (shelf != null) {
            return new StorageItem(StorageType.SHELF, shelf);
        }

        StorageRack rack = transform.getItemByFhirId(uuid, rackService);
        if (rack != null) {
            return new StorageItem(StorageType.RACK, rack);
        }

        StorageBox box = transform.getItemByFhirId(uuid, boxService);
        if (box != null) {
            return new StorageItem(StorageType.BOX, box);
        }

        throw new ResourceNotFoundException("No storage location found for UUID: " + uuidString);
    }

    private StorageItem deactivateStorageItem(StorageItem item, String sysUserId) {
        return switch (item.type()) {
        case ROOM -> {
            StorageRoom room = (StorageRoom) item.entity();
            markInactive(room, sysUserId);

            StorageRoom updated = roomService.update(room);
            validateUpdatedEntity(updated, "StorageRoom");

            yield new StorageItem(StorageType.ROOM, updated);
        }
        case DEVICE -> {
            StorageDevice device = (StorageDevice) item.entity();
            markInactive(device, sysUserId);

            StorageDevice updated = deviceService.update(device);
            validateUpdatedEntity(updated, "StorageDevice");

            yield new StorageItem(StorageType.DEVICE, updated);
        }
        case SHELF -> {
            StorageShelf shelf = (StorageShelf) item.entity();
            markInactive(shelf, sysUserId);

            StorageShelf updated = shelfService.update(shelf);
            validateUpdatedEntity(updated, "StorageShelf");

            yield new StorageItem(StorageType.SHELF, updated);
        }
        case RACK -> {
            StorageRack rack = (StorageRack) item.entity();
            markInactive(rack, sysUserId);

            StorageRack updated = rackService.update(rack);
            validateUpdatedEntity(updated, "StorageRack");

            yield new StorageItem(StorageType.RACK, updated);
        }
        case BOX -> {
            StorageBox box = (StorageBox) item.entity();
            markInactive(box, sysUserId);

            StorageBox updated = boxService.update(box);
            validateUpdatedEntity(updated, "StorageBox");

            yield new StorageItem(StorageType.BOX, updated);
        }
        };
    }

    private void markInactive(StorageRoom room, String sysUserId) {
        room.setActive(false);
        room.setSysUserId(sysUserId);
    }

    private void markInactive(StorageDevice device, String sysUserId) {
        device.setActive(false);
        device.setSysUserId(sysUserId);
    }

    private void markInactive(StorageShelf shelf, String sysUserId) {
        shelf.setActive(false);
        shelf.setSysUserId(sysUserId);
    }

    private void markInactive(StorageRack rack, String sysUserId) {
        rack.setActive(false);
        rack.setSysUserId(sysUserId);
    }

    private void markInactive(StorageBox box, String sysUserId) {
        box.setActive(false);
        box.setSysUserId(sysUserId);
    }

    private void syncStorageItem(StorageItem item) {
        switch (item.type()) {
        case ROOM:
            transform.syncToFhir((StorageRoom) item.entity(), false);
            break;
        case DEVICE:
            transform.syncToFhir((StorageDevice) item.entity(), false);
            break;
        case SHELF:
            transform.syncToFhir((StorageShelf) item.entity(), false);
            break;
        case RACK:
            transform.syncToFhir((StorageRack) item.entity(), false);
            break;
        case BOX:
            transform.syncToFhir((StorageBox) item.entity(), false);
            break;
        default:
            throw new InternalErrorException("Unsupported storage type: " + item.type());
        }
    }

    private Location transformStorageItem(StorageItem item) {
        Location location = switch (item.type()) {
        case ROOM -> transform.transformToFhirLocation((StorageRoom) item.entity());
        case DEVICE -> transform.transformToFhirLocation((StorageDevice) item.entity());
        case SHELF -> transform.transformToFhirLocation((StorageShelf) item.entity());
        case RACK -> transform.transformToFhirLocation((StorageRack) item.entity());
        case BOX -> transform.transformToFhirLocation((StorageBox) item.entity());
        };

        if (location == null) {
            throw new InternalErrorException(
                    "Failed to transform " + storageTypeName(item.type()) + " to FHIR Location");
        }

        return location;
    }

    private UUID parseUuid(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new InvalidRequestException("Location UUID cannot be null or blank");
        }

        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid UUID format: " + uuidString);
        }
    }

    private void validateSysUserId(String sysUserId) {
        if (sysUserId == null || sysUserId.isBlank()) {
            throw new InvalidRequestException("System user ID is required for deletion");
        }
    }

    private void validateUpdatedEntity(Object entity, String entityName) {
        if (entity == null) {
            throw new InternalErrorException("Failed to update " + entityName + " during deletion");
        }
    }

    private String storageTypeName(StorageType type) {
        return switch (type) {
        case ROOM -> "StorageRoom";
        case DEVICE -> "StorageDevice";
        case SHELF -> "StorageShelf";
        case RACK -> "StorageRack";
        case BOX -> "StorageBox";
        };
    }

    private enum StorageType {
        ROOM, DEVICE, SHELF, RACK, BOX
    }

    private record StorageItem(StorageType type, Object entity) {
    }

    private String safeMessage(Exception e) {
        return (e == null || e.getMessage() == null) ? "No error message" : e.getMessage();
    }

}