package org.openelisglobal.storage.fhir;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
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
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.CodeGenerationService;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.service.StorageShelfService;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageLocationFhirTransform {

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private StorageRoomService storageRoomService;

    @Autowired
    private StorageBoxService storageBoxService;

    @Autowired
    private StorageDeviceService storageDeviceService;

    @Autowired
    private StorageShelfService storageShelfService;

    @Autowired
    private StorageRackService storageRackService;

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

    public StorageRoom createStorageRoomFromLocation(Location location) {
        StorageRoom room = new StorageRoom();
        if (location.hasId()) {
            String roomId = location.getIdElement().getIdPart();
            room = getItemByFhirId(UUID.fromString(roomId), storageRoomService);
            if (room == null) {
                room = new StorageRoom();
                room.setFhirUuid(UUID.fromString(roomId));
            }

        } else {
            room.setFhirUuid(UUID.randomUUID());
        }
        if (location.hasName()) {
            room.setName(location.getName());
            String generatedCode = codeGenerationService.generateCodeFromName(location.getName(), "room");
            if (GenericValidator.isBlankOrNull(room.getCode())) {
                room.setCode(generatedCode);
            }
        }
        if (location.hasDescription()) {
            room.setDescription(location.getDescription());
        }
        if (location.hasStatus()) {
            room.setActive(location.getStatus() == LocationStatus.ACTIVE);
        }
        return room;

    }

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

    public StorageDevice createOrUpdateStorageDeviceFromLocation(Location location) {

        StorageDevice device;

        if (location.hasId()) {
            UUID uuid = UUID.fromString(location.getIdElement().getIdPart());
            device = getItemByFhirId(uuid, storageDeviceService);
            if (device == null) {
                device = new StorageDevice();
                device.setFhirUuid(uuid);
            }
        } else {
            device = new StorageDevice();
            device.setFhirUuid(UUID.randomUUID());
        }

        if (location.hasName()) {
            device.setName(location.getName());
            String generatedCode = codeGenerationService.generateCodeFromName(location.getName(), "shelf");
            if (GenericValidator.isBlankOrNull(device.getCode())) {
                device.setCode(generatedCode);
            }

        }

        if (location.hasStatus()) {
            device.setActive(location.getStatus() == LocationStatus.ACTIVE);
        }

        if (!location.getIdentifier().isEmpty()) {
            device.setCode(location.getIdentifierFirstRep().getValue());
        }

        if (location.hasPartOf() && location.getPartOf().hasReference()) {
            String refId = location.getPartOf().getReferenceElement().getIdPart();
            if (refId != null) {
                StorageRoom room = getItemByFhirId(UUID.fromString(refId), storageRoomService);
                device.setParentRoom(room);
            }
        }

        if (!location.getType().isEmpty()) {
            CodeableConcept typeConcept = location.getTypeFirstRep();
            if (!typeConcept.getCoding().isEmpty()) {
                String typeCode = typeConcept.getCodingFirstRep().getCode();
                device.setType(typeCode);
            }
        }

        for (Extension ext : location.getExtension()) {

            switch (ext.getUrl()) {

            case EXT_STORAGE_TEMPERATURE:
                if (ext.getValue() instanceof org.hl7.fhir.r4.model.DecimalType val) {
                    device.setTemperatureSetting(new BigDecimal(val.getValue().doubleValue()));
                }
                break;

            case EXT_STORAGE_CAPACITY:
                if (ext.getValue() instanceof IntegerType val) {
                    device.setCapacityLimit(val.getValue());
                }
                break;

            case EXT_DEVICE_IP_ADDRESS:
                if (ext.getValue() instanceof StringType val) {
                    device.setIpAddress(val.getValue());
                }
                break;

            case EXT_DEVICE_PORT:
                if (ext.getValue() instanceof IntegerType val) {
                    device.setPort(val.getValue());
                }
                break;

            case EXT_DEVICE_COMMUNICATION_PROTOCOL:
                if (ext.getValue() instanceof StringType val) {
                    device.setCommunicationProtocol(val.getValue());
                }
                break;
            }
        }

        return device;
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

    public StorageShelf createOrUpdateStorageShelfFromLocation(Location location) {

        StorageShelf shelf;

        if (location.hasId()) {
            UUID uuid = UUID.fromString(location.getIdElement().getIdPart());
            shelf = getItemByFhirId(uuid, storageShelfService);

            if (shelf == null) {
                shelf = new StorageShelf();
                shelf.setFhirUuid(uuid);
            }
        } else {
            shelf = new StorageShelf();
            shelf.setFhirUuid(UUID.randomUUID());
        }

        if (location.hasName()) {
            shelf.setLabel(location.getName());
            String generatedCode = codeGenerationService.generateCodeFromName(location.getName(), "shelf");
            if (GenericValidator.isBlankOrNull(shelf.getCode())) {
                shelf.setCode(generatedCode);
            }
        }

        if (location.hasStatus()) {
            shelf.setActive(location.getStatus() == LocationStatus.ACTIVE);
        }

        if (location.hasPartOf() && location.getPartOf().hasReference()) {
            String refId = location.getPartOf().getReferenceElement().getIdPart();

            if (refId != null) {
                StorageDevice device = getItemByFhirId(UUID.fromString(refId), storageDeviceService);
                shelf.setParentDevice(device);
            }
        }
        for (Extension ext : location.getExtension()) {

            if (EXT_STORAGE_CAPACITY.equals(ext.getUrl()) && ext.getValue() instanceof IntegerType val) {

                shelf.setCapacityLimit(val.getValue());
            }
        }

        return shelf;
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

    public StorageRack createOrUpdateStorageRackFromLocation(Location location) {

        StorageRack rack;

        if (location.hasId()) {
            UUID uuid = UUID.fromString(location.getIdElement().getIdPart());
            rack = getItemByFhirId(uuid, storageRackService);

            if (rack == null) {
                rack = new StorageRack();
                rack.setFhirUuid(uuid);
            }
        } else {
            rack = new StorageRack();
            rack.setFhirUuid(UUID.randomUUID());
        }

        if (location.hasName()) {
            rack.setLabel(location.getName());
            String generatedCode = codeGenerationService.generateCodeFromName(location.getName(), "rack");
            if (GenericValidator.isBlankOrNull(rack.getCode())) {
                rack.setCode(generatedCode);
            }
        }

        if (location.hasStatus()) {
            rack.setActive(location.getStatus() == LocationStatus.ACTIVE);
        }

        if (location.hasPartOf() && location.getPartOf().hasReference()) {
            String refId = location.getPartOf().getReferenceElement().getIdPart();

            if (refId != null) {
                StorageShelf shelf = getItemByFhirId(UUID.fromString(refId), storageShelfService);
                rack.setParentShelf(shelf);
            }
        }

        return rack;
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

    public StorageBox createOrUpdateStorageBoxFromLocation(Location location) {

        StorageBox box;

        if (location.hasId()) {
            UUID uuid = UUID.fromString(location.getIdElement().getIdPart());
            box = getItemByFhirId(uuid, storageBoxService);

            if (box == null) {
                box = new StorageBox();
                box.setFhirUuid(uuid);
            }
        } else {
            box = new StorageBox();
            box.setFhirUuid(UUID.randomUUID());
        }

        if (location.hasName()) {
            box.setLabel(location.getName());
            String generatedCode = codeGenerationService.generateCodeFromName(location.getName(), "box");
            if (GenericValidator.isBlankOrNull(box.getCode())) {
                box.setCode(generatedCode);
            }
        }
        if (location.hasStatus()) {
            box.setActive(location.getStatus() == LocationStatus.ACTIVE);
        }

        if (location.hasPartOf() && location.getPartOf().hasReference()) {
            String refId = location.getPartOf().getReferenceElement().getIdPart();

            if (refId != null) {
                StorageRack rack = getItemByFhirId(UUID.fromString(refId), storageRackService);
                box.setParentRack(rack);
            }
        }

        if (location.hasPhysicalType() && location.getPhysicalType().hasText()) {
            box.setType(location.getPhysicalType().getText());
        }

        Integer rows = null;
        Integer columns = null;

        for (Extension ext : location.getExtension()) {

            String url = ext.getUrl();

            if (EXT_RACK_GRID_DIMENSIONS.equals(url) && ext.getValue() instanceof StringType val) {

                String grid = val.getValue(); // e.g. "8 × 12"

                if (grid != null && grid.contains("×")) {
                    String[] parts = grid.split("×");

                    if (parts.length == 2) {
                        try {
                            rows = Integer.parseInt(parts[0].trim());
                            columns = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            LogEvent.logWarn(getClass().getSimpleName(), "createOrUpdateStorageBoxFromLocation",
                                    "Invalid grid format: " + grid);
                        }
                    }
                }
            }

            else if (EXT_RACK_POSITION_HINT.equals(url) && ext.getValue() instanceof StringType val) {

                box.setPositionSchemaHint(val.getValue());
            }
        }

        if (rows != null && columns != null && rows > 0 && columns > 0) {
            box.setRows(rows);
            box.setColumns(columns);
        }

        return box;
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

    public <T extends BaseObject<?>> T getItemByFhirId(UUID fhirUuid, BaseObjectService<T, ?> service) {

        if (fhirUuid == null) {
            return null;
        }

        List<T> matches = service.getAllMatching("fhirUuid", fhirUuid);

        if (matches == null || matches.isEmpty()) {
            return null;
        }

        if (matches.size() > 1) {
            LogEvent.logWarn(getClass().getSimpleName(), "getItemByFhirId",
                    "Multiple records found for fhirUuid: " + fhirUuid);
        }

        return matches.get(0);
    }
}
