package org.openelisglobal.storage.service;

import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of CodeValidationService Validates code format, length, and
 * uniqueness
 */
@Service
public class CodeValidationServiceImpl implements CodeValidationService {

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    private static final int MAX_CODE_LENGTH = 10;
    private static final String CODE_PATTERN = "^[A-Z0-9][A-Z0-9_-]*$"; // Starts with letter/number, then
                                                                        // alphanumeric/hyphen/underscore

    @Override
    public CodeValidationResult validateFormat(String code) {
        // Null or empty check
        if (StringUtils.isBlank(code)) {
            return CodeValidationResult.invalid("Code cannot be empty");
        }

        // Normalize to uppercase
        String normalized = code.toUpperCase().trim();

        // Length check
        if (normalized.length() > MAX_CODE_LENGTH) {
            return CodeValidationResult.invalid(String.format("Code cannot exceed %d characters", MAX_CODE_LENGTH));
        }

        // Pattern check: must start with letter or number, then
        // alphanumeric/hyphen/underscore
        if (!normalized.matches(CODE_PATTERN)) {
            if (normalized.startsWith("-") || normalized.startsWith("_")) {
                return CodeValidationResult.invalid("Code must start with a letter or number");
            }
            // Check for invalid characters
            if (!normalized.matches("^[A-Z0-9_-]+$")) {
                return CodeValidationResult.invalid("Code can only contain letters, numbers, hyphens, and underscores");
            }
            return CodeValidationResult.invalid("Invalid code format");
        }

        // Valid
        return CodeValidationResult.valid(normalized);
    }

    @Override
    public CodeValidationResult validateLength(String code) {
        if (StringUtils.isBlank(code)) {
            return CodeValidationResult.invalid("Code cannot be empty");
        }

        String normalized = code.toUpperCase().trim();
        if (normalized.length() > MAX_CODE_LENGTH) {
            return CodeValidationResult.invalid(String.format("Code cannot exceed %d characters", MAX_CODE_LENGTH));
        }

        return CodeValidationResult.valid(normalized);
    }

    @Override
    public CodeValidationResult validateUniqueness(String code, String context, String locationId, String parentId) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(context)) {
            return CodeValidationResult.invalid("Code and context are required");
        }

        // Normalize code
        String normalized = code.toUpperCase().trim();

        // Check uniqueness based on context
        switch (context.toLowerCase()) {
        case "room":
            // Room: globally unique
            StorageRoom existingRoom = storageRoomDAO.findByCode(normalized);
            if (existingRoom != null && !String.valueOf(existingRoom.getId()).equals(locationId)) {
                return CodeValidationResult
                        .invalid(String.format("Code '%s' already exists for another room", normalized));
            }
            break;

        case "device":
            // Device: unique within parent room
            if (StringUtils.isBlank(parentId)) {
                return CodeValidationResult.invalid("Parent room ID is required for device code validation");
            }
            StorageRoom parentRoom = storageRoomDAO.get(Integer.parseInt(parentId)).orElse(null);
            if (parentRoom == null) {
                return CodeValidationResult.invalid("Parent room not found");
            }
            StorageDevice existingDevice = storageDeviceDAO.findByCodeAndParentRoom(normalized, parentRoom);
            if (existingDevice != null && !String.valueOf(existingDevice.getId()).equals(locationId)) {
                return CodeValidationResult
                        .invalid(String.format("Code '%s' already exists for another device in this room", normalized));
            }
            break;

        case "shelf":
            // Shelf: unique within parent device
            // TODO: Add findByCodeAndParentDevice method to StorageShelfDAO in
            // implementation phase
            if (StringUtils.isBlank(parentId)) {
                return CodeValidationResult.invalid("Parent device ID is required for shelf code validation");
            }
            StorageDevice parentDevice = storageDeviceDAO.get(Integer.parseInt(parentId)).orElse(null);
            if (parentDevice == null) {
                return CodeValidationResult.invalid("Parent device not found");
            }
            // Placeholder: Will be implemented when DAO method is added
            // StorageShelf existingShelf =
            // storageShelfDAO.findByCodeAndParentDevice(normalized, parentDevice);
            // For now, skip uniqueness check for shelf (will be added in implementation)
            break;

        case "rack":
            // Rack: unique within parent shelf
            // TODO: Add findByCodeAndParentShelf method to StorageRackDAO in implementation
            // phase
            if (StringUtils.isBlank(parentId)) {
                return CodeValidationResult.invalid("Parent shelf ID is required for rack code validation");
            }
            StorageShelf parentShelf = storageShelfDAO.get(Integer.parseInt(parentId)).orElse(null);
            if (parentShelf == null) {
                return CodeValidationResult.invalid("Parent shelf not found");
            }
            // Placeholder: Will be implemented when DAO method is added
            // StorageRack existingRack =
            // storageRackDAO.findByCodeAndParentShelf(normalized, parentShelf);
            // For now, skip uniqueness check for rack (will be added in implementation)
            break;

        default:
            return CodeValidationResult.invalid("Invalid context: " + context);
        }

        // Unique
        return CodeValidationResult.valid(normalized);
    }

    @Override
    public String autoUppercase(String code) {
        if (StringUtils.isBlank(code)) {
            return code;
        }
        return code.toUpperCase().trim();
    }
}
