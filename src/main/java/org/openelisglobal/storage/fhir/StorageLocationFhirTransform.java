package org.openelisglobal.storage.fhir;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.BooleanType;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.stereotype.Service;

@Service
public class StorageLocationFhirTransform {

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

    public Location transformToFhirLocation(StorageRoom room) {
        Location location = new Location();
        
        location.setId(room.getFhirUuidAsString());
        location.setStatus(room.getActive() != null && room.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
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
        location.setStatus(device.getActive() != null && device.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
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
        
        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "device", "Device");
        
        return location;
    }

    public Location transformToFhirLocation(StorageShelf shelf) {
        Location location = new Location();
        
        location.setId(shelf.getFhirUuidAsString());
        location.setStatus(shelf.getActive() != null && shelf.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(shelf.getLabel());
        location.setMode(LocationMode.INSTANCE);
        
        // Hierarchical identifier: ROOM-DEVICE-SHELF
        StorageDevice device = shelf.getParentDevice();
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + 
            device.getCode() + "-" + shelf.getLabel();
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
        location.setStatus(rack.getActive() != null && rack.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);
        location.setName(rack.getLabel());
        location.setMode(LocationMode.INSTANCE);
        
        // Hierarchical identifier: ROOM-DEVICE-SHELF-RACK
        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf.getParentDevice();
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + 
            device.getCode() + "-" + shelf.getLabel() + "-" + rack.getLabel();
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
        
        // Extensions: Grid dimensions
        if (rack.getRows() != null && rack.getColumns() != null && rack.getRows() > 0 && rack.getColumns() > 0) {
            Extension gridExt = new Extension(EXT_RACK_GRID_DIMENSIONS);
            gridExt.setValue(new StringType(rack.getRows() + " × " + rack.getColumns()));
            location.addExtension(gridExt);
            
            // Capacity
            Extension capExt = new Extension(EXT_STORAGE_CAPACITY);
            capExt.setValue(new IntegerType(rack.getCapacity()));
            location.addExtension(capExt);
        }
        if (rack.getPositionSchemaHint() != null) {
            Extension hintExt = new Extension(EXT_RACK_POSITION_HINT);
            hintExt.setValue(new StringType(rack.getPositionSchemaHint()));
            location.addExtension(hintExt);
        }
        
        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "rack", "Rack");
        
        return location;
    }

    public Location transformToFhirLocation(StoragePosition position) {
        Location location = new Location();
        
        location.setId(position.getFhirUuidAsString());
        location.setStatus(LocationStatus.ACTIVE);
        location.setName(position.getCoordinate());
        location.setMode(LocationMode.INSTANCE);
        
        // Hierarchical identifier: ROOM-DEVICE-SHELF-RACK-POSITION
        StorageRack rack = position.getParentRack();
        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf.getParentDevice();
        String hierarchicalCode = device.getParentRoom().getCode() + "-" + 
            device.getCode() + "-" + shelf.getLabel() + "-" + 
            rack.getLabel() + "-" + position.getCoordinate();
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
        physicalType.setText("Storage Position");
        location.setPhysicalType(physicalType);
        
        // Parent reference
        Reference partOf = new Reference();
        partOf.setReference("Location/" + rack.getFhirUuidAsString());
        partOf.setDisplay(rack.getLabel());
        location.setPartOf(partOf);
        
        // Extensions: Occupancy and grid position
        Extension occExt = new Extension(EXT_POSITION_OCCUPANCY);
        occExt.setValue(new BooleanType(position.getOccupied() != null && position.getOccupied()));
        location.addExtension(occExt);
        
        if (position.getRowIndex() != null) {
            Extension rowExt = new Extension(EXT_POSITION_GRID_ROW);
            rowExt.setValue(new IntegerType(position.getRowIndex()));
            location.addExtension(rowExt);
        }
        if (position.getColumnIndex() != null) {
            Extension colExt = new Extension(EXT_POSITION_GRID_COLUMN);
            colExt.setValue(new IntegerType(position.getColumnIndex()));
            location.addExtension(colExt);
        }
        
        location.getMeta().addProfile(MCSD_PROFILE);
        location.getMeta().addTag(STORAGE_HIERARCHY_TAG_SYSTEM, "position", "Position");
        
        return location;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}

