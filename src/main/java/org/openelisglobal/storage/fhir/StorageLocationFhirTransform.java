package org.openelisglobal.storage.fhir;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageLocationFhirTransform {

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    private static final String OPENELIS_STORAGE_CODE_SYSTEM = "http://openelis.org/storage-location-code";
    private static final String MCSD_PROFILE = "http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location";
    private static final String STORAGE_HIERARCHY_TAG_SYSTEM = "http://openelis.org/fhir/tag/storage-hierarchy";

    // Extension URLs
    private static final String EXT_STORAGE_TEMPERATURE = "http://openelis.org/fhir/extension/storage-temperature";
    private static final String EXT_STORAGE_CAPACITY = "http://openelis.org/fhir/extension/storage-capacity";
    private static final String EXT_RACK_GRID_DIMENSIONS = "http://openelis.org/fhir/extension/rack-grid-dimensions";
    private static final String EXT_RACK_POSITION_HINT = "http://openelis.org/fhir/extension/rack-position-schema-hint";
    private static final String EXT_POSITION_OCCUPANCY = "http://openelis.org/fhir/extension/position-occupancy";
    private static final String EXT_POSITION_GRID_ROW = "http://openelis.org/fhir/extension/position-grid-row";
    private static final String EXT_POSITION_GRID_COLUMN = "http://openelis.org/fhir/extension/position-grid-column";
    // Device connectivity extensions for network-connected equipment
    private static final String EXT_DEVICE_IP_ADDRESS = "http://openelis.org/fhir/extension/device-ip-address";
    private static final String EXT_DEVICE_PORT = "http://openelis.org/fhir/extension/device-port";
    private static final String EXT_DEVICE_COMMUNICATION_PROTOCOL = "http://openelis.org/fhir/extension/device-communication-protocol";

    public Location transformToFhirLocation(StorageRoom room) {
        Location location = new Location();

        location.setId(room.getFhirUuidAsString());
        location.setStatus(
                room.getActive() != null && room.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(room.getName());
        location.setDescription(room.getDescription());
        location.setMode(LocationMode.INSTANCE);

        // Identifier
        Identifier identifier = new Identifier();
        identifier.setSystem(OPENELIS_STORAGE_CODE_SYSTEM);
        identifier.setValue(room.getCode());
        location.addIdentifier(identifier);

        // Physical Type: Room
        CodeableConcept physicalType = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type");
        coding.setCode("ro");
        coding.setDisplay("Room");
        physicalType.addCoding(coding);
        location.setPhysicalType(physicalType);

        // Meta profile
        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "room", "Room");

        return location;
    }

    public Location transformToFhirLocation(StorageDevice device) {
        Location location = new Location();

        location.setId(device.getFhirUuidAsString());
        location.setStatus(
                device.getActive() != null && device.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(device.getName());
        location.setMode(LocationMode.INSTANCE);

        // Hierarchical identifier: ROOM-DEVICE
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + device.getCode();
        Identifier identifier = new Identifier();
        identifier.setSystem(OPENELIS_STORAGE_CODE_SYSTEM);
        identifier.setValue(hierarchicalCode);
        location.addIdentifier(identifier);

        // Physical Type: Vehicle/Equipment
        CodeableConcept physicalType = new CodeableConcept();
        Coding physTypeCoding = new Coding();
        physTypeCoding.setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type");
        physTypeCoding.setCode("ve");
        physTypeCoding.setDisplay("Vehicle");
        physicalType.addCoding(physTypeCoding);
        physicalType.setText("Storage Equipment");
        location.setPhysicalType(physicalType);

        // Device Type
        CodeableConcept deviceType = new CodeableConcept();
        Coding typeCoding = new Coding();
        typeCoding.setSystem("http://openelis.org/fhir/CodeSystem/storage-device-type");
        typeCoding.setCode(device.getTypeAsString());
        typeCoding.setDisplay(capitalizeFirst(device.getTypeAsString()));
        deviceType.addCoding(typeCoding);
        location.addType(deviceType);

        // Parent reference
        Reference partOf = new Reference();
        partOf.setReference("Location/" + device.getParentRoom().getFhirUuidAsString());
        partOf.setDisplay(device.getParentRoom().getName());
        location.setPartOf(partOf);

        // Extensions
        if (device.getTemperatureSetting() != null) {
            Extension tempExt = new Extension(EXT_STORAGE_TEMPERATURE);
            tempExt.setValue(new org.hl7.fhir.r4.model.DecimalType(device.getTemperatureSetting()));
            location.addExtension(tempExt);
        }
        if (device.getCapacityLimit() != null) {
            Extension capExt = new Extension(EXT_STORAGE_CAPACITY);
            capExt.setValue(new IntegerType(device.getCapacityLimit()));
            location.addExtension(capExt);
        }
        // Add connectivity extensions for network-connected equipment
        if (device.getIpAddress() != null && !device.getIpAddress().trim().isEmpty()) {
            Extension ipExt = new Extension(EXT_DEVICE_IP_ADDRESS);
            ipExt.setValue(new StringType(device.getIpAddress()));
            location.addExtension(ipExt);
        }
        if (device.getPort() != null) {
            Extension portExt = new Extension(EXT_DEVICE_PORT);
            portExt.setValue(new IntegerType(device.getPort()));
            location.addExtension(portExt);
        }
        if (device.getCommunicationProtocol() != null && !device.getCommunicationProtocol().trim().isEmpty()) {
            Extension protocolExt = new Extension(EXT_DEVICE_COMMUNICATION_PROTOCOL);
            protocolExt.setValue(new StringType(device.getCommunicationProtocol()));
            location.addExtension(protocolExt);
        }

        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "device", "Device");

        return location;
    }

    public Location transformToFhirLocation(StorageShelf shelf) {
        Location location = new Location();

        location.setId(shelf.getFhirUuidAsString());
        location.setStatus(
                shelf.getActive() != null && shelf.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(shelf.getLabel());
        location.setMode(LocationMode.INSTANCE);

        // Hierarchical identifier: ROOM-DEVICE-SHELF
        StorageDevice device = shelf.getParentDevice();
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + device.getCode() + "-" + shelf.getLabel();
        Identifier identifier = new Identifier();
        identifier.setSystem(OPENELIS_STORAGE_CODE_SYSTEM);
        identifier.setValue(hierarchicalCode);
        location.addIdentifier(identifier);

        // Physical Type: Container
        CodeableConcept physicalType = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type");
        coding.setCode("co");
        coding.setDisplay("Container");
        physicalType.addCoding(coding);
        physicalType.setText("Storage Shelf");
        location.setPhysicalType(physicalType);

        // Parent reference
        Reference partOf = new Reference();
        partOf.setReference("Location/" + device.getFhirUuidAsString());
        partOf.setDisplay(device.getName());
        location.setPartOf(partOf);

        // Extensions
        if (shelf.getCapacityLimit() != null) {
            Extension capExt = new Extension(EXT_STORAGE_CAPACITY);
            capExt.setValue(new IntegerType(shelf.getCapacityLimit()));
            location.addExtension(capExt);
        }

        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "shelf", "Shelf");

        return location;
    }

    public Location transformToFhirLocation(StorageRack rack) {
        Location location = new Location();

        location.setId(rack.getFhirUuidAsString());
        location.setStatus(
                rack.getActive() != null && rack.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(rack.getLabel());
        location.setMode(LocationMode.INSTANCE);

        // Hierarchical identifier: ROOM-DEVICE-SHELF-RACK
        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf.getParentDevice();
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + device.getCode() + "-" + shelf.getLabel()
                + "-" + rack.getLabel();
        Identifier identifier = new Identifier();
        identifier.setSystem(OPENELIS_STORAGE_CODE_SYSTEM);
        identifier.setValue(hierarchicalCode);
        location.addIdentifier(identifier);

        // Physical Type: Container
        CodeableConcept physicalType = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type");
        coding.setCode("co");
        coding.setDisplay("Container");
        physicalType.addCoding(coding);
        physicalType.setText("Storage Rack");
        location.setPhysicalType(physicalType);

        // Parent reference
        Reference partOf = new Reference();
        partOf.setReference("Location/" + shelf.getFhirUuidAsString());
        partOf.setDisplay(shelf.getLabel());
        location.setPartOf(partOf);

        // Note: Grid dimensions moved to StorageBox (gridded containers)
        // Racks are simple containers now

        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "rack", "Rack");

        return location;
    }

    public Location transformToFhirLocation(StorageBox box) {
        Location location = new Location();

        location.setId(box.getFhirUuidAsString());
        location.setStatus(LocationStatus.ACTIVE);
        location.setMode(LocationMode.INSTANCE);

        StorageRack rack = box.getParentRack();
        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf.getParentDevice();
        StorageRoom room = device.getParentRoom();

        String hierarchicalCode = room.getCode() + "-" + device.getCode() + "-" + shelf.getLabel() + "-"
                + rack.getLabel() + "-" + (box.getLabel() != null ? box.getLabel() : "BOX");
        Reference partOf = new Reference("Location/" + rack.getFhirUuidAsString());
        partOf.setDisplay(rack.getLabel());
        String locationName = box.getLabel() != null ? box.getLabel() : rack.getLabel();

        location.setName(locationName);
        Identifier identifier = new Identifier();
        identifier.setSystem(OPENELIS_STORAGE_CODE_SYSTEM);
        identifier.setValue(hierarchicalCode);
        location.addIdentifier(identifier);

        // Physical Type: Container (gridded container like 96-well plate)
        CodeableConcept physicalType = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type");
        coding.setCode("co");
        coding.setDisplay("Container");
        physicalType.addCoding(coding);
        physicalType.setText(box.getType() != null ? box.getType() : "Storage Box");
        location.setPhysicalType(physicalType);

        location.setPartOf(partOf);

        // Grid dimensions (boxes are gridded containers)
        if (box.getRows() != null && box.getColumns() != null && box.getRows() > 0 && box.getColumns() > 0) {
            Extension gridExt = new Extension(EXT_RACK_GRID_DIMENSIONS);
            gridExt.setValue(new StringType(box.getRows() + " × " + box.getColumns()));
            location.addExtension(gridExt);

            // Capacity
            Extension capExt = new Extension(EXT_STORAGE_CAPACITY);
            capExt.setValue(new IntegerType(box.getCapacity()));
            location.addExtension(capExt);
        }
        if (box.getPositionSchemaHint() != null) {
            Extension hintExt = new Extension(EXT_RACK_POSITION_HINT);
            hintExt.setValue(new StringType(box.getPositionSchemaHint()));
            location.addExtension(hintExt);
        }

        boolean isOccupied = calculateBoxOccupied(box);
        Extension occExt = new Extension(EXT_POSITION_OCCUPANCY);
        occExt.setValue(new BooleanType(isOccupied));
        location.addExtension(occExt);

        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "box", "Box");

        return location;
    }

    /**
     * Calculate if a StorageBox is occupied by checking SampleStorageAssignment
     * records.
     */
    @Transactional(readOnly = true)
    private boolean calculateBoxOccupied(StorageBox box) {
        try {
            return sampleStorageAssignmentDAO.isBoxOccupied(box);
        } catch (Exception e) {
            LogEvent.logError("Error calculating box occupancy: " + e.getMessage(), e);
            return false;
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Transform and persist a storage location entity to FHIR server Called from
     * entity lifecycle hooks (@PostPersist, @PostUpdate)
     */
    @Async
    @Transactional(readOnly = true)
    public void syncToFhir(StorageRoom room, boolean isCreate) {
        try {
            Location location = transformToFhirLocation(room);
            persistLocation(location, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing StorageRoom to FHIR: " + e.getMessage(), e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void syncToFhir(StorageDevice device, boolean isCreate) {
        try {
            Location location = transformToFhirLocation(device);
            persistLocation(location, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing StorageDevice to FHIR: " + e.getMessage(), e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void syncToFhir(StorageShelf shelf, boolean isCreate) {
        try {
            Location location = transformToFhirLocation(shelf);
            persistLocation(location, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing StorageShelf to FHIR: " + e.getMessage(), e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void syncToFhir(StorageRack rack, boolean isCreate) {
        try {
            Location location = transformToFhirLocation(rack);
            persistLocation(location, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing StorageRack to FHIR: " + e.getMessage(), e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void syncToFhir(StorageBox box, boolean isCreate) {
        try {
            Location location = transformToFhirLocation(box);
            persistLocation(location, isCreate);
        } catch (Exception e) {
            LogEvent.logError("Error syncing StorageBox to FHIR: " + e.getMessage(), e);
        }
    }

    private void persistLocation(Location location, boolean isCreate) throws FhirLocalPersistingException {
        try {
            FhirPersistanceService fhirPersistanceService = SpringContext.getBean(FhirPersistanceService.class);
            if (fhirPersistanceService == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistLocation",
                        "FhirPersistanceService not available, skipping FHIR sync");
                return;
            }

            Map<String, Resource> resourceMap = new HashMap<>();
            String locationId = location.getIdElement().getIdPart();
            if (locationId == null || locationId.isEmpty()) {
                locationId = location.getIdElement().getValue();
            }
            resourceMap.put(locationId != null ? locationId : "", location);

            if (isCreate) {
                fhirPersistanceService.createFhirResourcesInFhirStore(resourceMap);
            } else {
                fhirPersistanceService.updateFhirResourcesInFhirStore(resourceMap);
            }
        } catch (Exception e) {
            LogEvent.logError("Error persisting Location to FHIR server: " + e.getMessage(), e);
            throw new FhirLocalPersistingException(e);
        }
    }
}
