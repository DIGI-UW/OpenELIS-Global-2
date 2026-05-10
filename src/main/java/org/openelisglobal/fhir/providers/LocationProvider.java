package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.ToString.Include;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.jsmpp.InvalidRequestException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocationProvider implements IResourceProvider {

    @Autowired
    private StorageLocationFhirTransform transform;

    @Autowired
    private StorageRoomDAO roomDAO;

    @Autowired
    private StorageDeviceDAO deviceDAO;

    @Autowired
    private StorageShelfDAO shelfDAO;

    @Autowired
    private StorageRackDAO rackDAO;

    @Autowired
    private StorageBoxDAO boxDAO;

    @Autowired
    private FhirUtil util;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Location.class;
    }

    @Read
    public Location readLocation(@IdParam IdType theId) {
        if (theId == null || theId.getIdPart() == null || theId.getIdPart().isBlank()) {
            throw new ResourceNotFoundException("Valid Location ID is required");
        }

        try {
            return handleGetLocation(theId.getIdPart());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid UUID format: " + theId.getIdPart());
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
    public Bundle searchLocationBundle(@OptionalParam(name = Location.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Location.SP_NAME) StringAndListParam name,
            @OptionalParam(name = Location.SP_OPERATIONAL_STATUS) TokenParam active,
            @OptionalParam(name = Location.SP_TYPE) TokenAndListParam type,
            @IncludeParam(allow = { "Location:" + Location.SP_ORGANIZATION }) HashSet<Include> includes,
            HttpServletRequest request) throws InvalidRequestException {

        String methodName = "searchPractitionerBundle";

        if (request == null) {
            throw new InvalidRequestException("HTTP request cannot be null");
        }

        try {
            Bundle bundle = util.forwardSearchToFhirStore(request);

            if (bundle == null) {
                throw new InternalErrorException("Search returned null bundle");
            }

            return bundle;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Practitioners: " + e.getMessage());
            throw new InternalErrorException("Error searching Practitioners", e);
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
            case "Storage Room": {
                StorageRoom room = transform.createStorageRoomFromLocation(location);
                if (room == null) {
                    throw new InternalErrorException("Failed to create StorageRoom from Location");
                }
                room.setSysUserId(sysUserId);

                StorageRoom saved = isCreate ? roomDAO.get(List.of(roomDAO.insert(room))).getFirst()
                        : roomDAO.update(room);

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

            case "Storage Equipment": {
                StorageDevice device = transform.createOrUpdateStorageDeviceFromLocation(location);
                if (device == null) {
                    throw new InternalErrorException("Failed to create StorageDevice from Location");
                }
                device.setSysUserId(sysUserId);

                StorageDevice saved = isCreate ? deviceDAO.get(List.of(deviceDAO.insert(device))).getFirst()
                        : deviceDAO.update(device);

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

            case "Storage Shelf": {
                StorageShelf shelf = transform.createOrUpdateStorageShelfFromLocation(location);
                if (shelf == null) {
                    throw new InternalErrorException("Failed to create StorageShelf from Location");
                }
                shelf.setSysUserId(sysUserId);

                StorageShelf saved = isCreate ? shelfDAO.get(List.of(shelfDAO.insert(shelf))).getFirst()
                        : shelfDAO.update(shelf);

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

            case "Storage Rack": {
                StorageRack rack = transform.createOrUpdateStorageRackFromLocation(location);
                if (rack == null) {
                    throw new InternalErrorException("Failed to create StorageRack from Location");
                }
                rack.setSysUserId(sysUserId);

                StorageRack saved = isCreate ? rackDAO.get(List.of(rackDAO.insert(rack))).getFirst()
                        : rackDAO.update(rack);

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

            case "Storage Box": {
                StorageBox box = transform.createOrUpdateStorageBoxFromLocation(location);
                if (box == null) {
                    throw new InternalErrorException("Failed to create StorageBox from Location");
                }
                box.setSysUserId(sysUserId);

                StorageBox saved = isCreate ? boxDAO.get(List.of(boxDAO.insert(box))).getFirst() : boxDAO.update(box);

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

            default:
                throw new InvalidRequestException("Unsupported Location type: " + category);
            }
        } catch (InvalidRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalErrorException("Error persisting Location: " + safeMessage(e), e);
        }
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

    private Location handleGetLocation(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) {
            throw new ResourceNotFoundException("Location UUID cannot be null or blank");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid UUID format: " + uuidStr);
        }

        StorageRoom room = transform.getItemByFhirId(uuid, roomDAO);
        if (room != null) {
            Location result = transform.transformToFhirLocation(room);
            if (result == null) {
                throw new InternalErrorException("Failed to transform StorageRoom to FHIR Location");
            }
            return result;
        }

        StorageDevice device = transform.getItemByFhirId(uuid, deviceDAO);
        if (device != null) {
            Location result = transform.transformToFhirLocation(device);
            if (result == null) {
                throw new InternalErrorException("Failed to transform StorageDevice to FHIR Location");
            }
            return result;
        }

        StorageShelf shelf = transform.getItemByFhirId(uuid, shelfDAO);
        if (shelf != null) {
            Location result = transform.transformToFhirLocation(shelf);
            if (result == null) {
                throw new InternalErrorException("Failed to transform StorageShelf to FHIR Location");
            }
            return result;
        }

        StorageRack rack = transform.getItemByFhirId(uuid, rackDAO);
        if (rack != null) {
            Location result = transform.transformToFhirLocation(rack);
            if (result == null) {
                throw new InternalErrorException("Failed to transform StorageRack to FHIR Location");
            }
            return result;
        }

        StorageBox box = transform.getItemByFhirId(uuid, boxDAO);
        if (box != null) {
            Location result = transform.transformToFhirLocation(box);
            if (result == null) {
                throw new InternalErrorException("Failed to transform StorageBox to FHIR Location");
            }
            return result;
        }

        throw new ResourceNotFoundException("No storage location found for UUID: " + uuidStr);
    }

    private Location handleDeleteLocation(String uuidStr, String sysUserId) throws InvalidRequestException {
        if (uuidStr == null || uuidStr.isBlank()) {
            throw new InvalidRequestException("Location UUID cannot be null or blank for deletion");
        }

        if (sysUserId == null || sysUserId.isBlank()) {
            throw new InvalidRequestException("System user ID is required for deletion");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid UUID format: " + uuidStr);
        }

        StorageRoom room = transform.getItemByFhirId(uuid, roomDAO);
        if (room != null) {
            room.setActive(false);
            room.setSysUserId(sysUserId);

            StorageRoom updated = roomDAO.update(room);
            if (updated == null) {
                throw new InternalErrorException("Failed to update StorageRoom during deletion");
            }

            transform.syncToFhir(updated, false);
            Location result = transform.transformToFhirLocation(updated);
            if (result == null) {
                throw new InternalErrorException("Failed to transform deleted StorageRoom to FHIR Location");
            }
            return result;
        }

        StorageDevice device = transform.getItemByFhirId(uuid, deviceDAO);
        if (device != null) {
            device.setActive(false);
            device.setSysUserId(sysUserId);

            StorageDevice updated = deviceDAO.update(device);
            if (updated == null) {
                throw new InternalErrorException("Failed to update StorageDevice during deletion");
            }

            transform.syncToFhir(updated, false);
            Location result = transform.transformToFhirLocation(updated);
            if (result == null) {
                throw new InternalErrorException("Failed to transform deleted StorageDevice to FHIR Location");
            }
            return result;
        }

        StorageShelf shelf = transform.getItemByFhirId(uuid, shelfDAO);
        if (shelf != null) {
            shelf.setActive(false);
            shelf.setSysUserId(sysUserId);

            StorageShelf updated = shelfDAO.update(shelf);
            if (updated == null) {
                throw new InternalErrorException("Failed to update StorageShelf during deletion");
            }

            transform.syncToFhir(updated, false);
            Location result = transform.transformToFhirLocation(updated);
            if (result == null) {
                throw new InternalErrorException("Failed to transform deleted StorageShelf to FHIR Location");
            }
            return result;
        }

        StorageRack rack = transform.getItemByFhirId(uuid, rackDAO);
        if (rack != null) {
            rack.setActive(false);
            rack.setSysUserId(sysUserId);

            StorageRack updated = rackDAO.update(rack);
            if (updated == null) {
                throw new InternalErrorException("Failed to update StorageRack during deletion");
            }

            transform.syncToFhir(updated, false);
            Location result = transform.transformToFhirLocation(updated);
            if (result == null) {
                throw new InternalErrorException("Failed to transform deleted StorageRack to FHIR Location");
            }
            return result;
        }

        StorageBox box = transform.getItemByFhirId(uuid, boxDAO);
        if (box != null) {
            box.setActive(false);
            box.setSysUserId(sysUserId);

            StorageBox updated = boxDAO.update(box);
            if (updated == null) {
                throw new InternalErrorException("Failed to update StorageBox during deletion");
            }

            transform.syncToFhir(updated, false);
            Location result = transform.transformToFhirLocation(updated);
            if (result == null) {
                throw new InternalErrorException("Failed to transform deleted StorageBox to FHIR Location");
            }
            return result;
        }

        throw new ResourceNotFoundException("No storage location found for UUID: " + uuidStr);
    }

    private String safeMessage(Exception e) {
        return (e == null || e.getMessage() == null) ? "No error message" : e.getMessage();
    }
}