package org.openelisglobal.storage.service;

import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of ShortCodeValidationService Validates short code format,
 * uniqueness, and generates change warnings
 */
@Service
public class ShortCodeValidationServiceImpl implements ShortCodeValidationService {

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    private static final int MAX_SHORT_CODE_LENGTH = 10;
    private static final String SHORT_CODE_PATTERN = "^[A-Z0-9][A-Z0-9_-]*$"; // Starts with letter/number, then
                                                                              // alphanumeric/hyphen/underscore

    @Override
    public ShortCodeValidationResult validateFormat(String shortCode) {
        // Null or empty check
        if (StringUtils.isBlank(shortCode)) {
            return ShortCodeValidationResult.invalid("Short code cannot be empty");
        }

        // Normalize to uppercase
        String normalized = shortCode.toUpperCase().trim();

        // Length check
        if (normalized.length() > MAX_SHORT_CODE_LENGTH) {
            return ShortCodeValidationResult
                    .invalid(String.format("Short code cannot exceed %d characters", MAX_SHORT_CODE_LENGTH));
        }

        // Pattern check: must start with letter or number, then
        // alphanumeric/hyphen/underscore
        if (!normalized.matches(SHORT_CODE_PATTERN)) {
            if (normalized.startsWith("-") || normalized.startsWith("_")) {
                return ShortCodeValidationResult.invalid("Short code must start with a letter or number");
            }
            // Check for invalid characters
            if (!normalized.matches("^[A-Z0-9_-]+$")) {
                return ShortCodeValidationResult
                        .invalid("Short code can only contain letters, numbers, hyphens, and underscores");
            }
            return ShortCodeValidationResult.invalid("Invalid short code format");
        }

        // Valid
        return ShortCodeValidationResult.valid(normalized);
    }

    @Override
    public ShortCodeValidationResult validateUniqueness(String shortCode, String context, String locationId) {
        if (StringUtils.isBlank(shortCode) || StringUtils.isBlank(context)) {
            return ShortCodeValidationResult.invalid("Short code and context are required");
        }

        // Normalize short code
        String normalized = shortCode.toUpperCase().trim();

        // Check uniqueness based on context
        switch (context.toLowerCase()) {
        case "device":
            StorageDevice existingDevice = storageDeviceDAO.findByShortCode(normalized);
            if (existingDevice != null && !String.valueOf(existingDevice.getId()).equals(locationId)) {
                return ShortCodeValidationResult
                        .invalid(String.format("Short code '%s' already exists for another device", normalized));
            }
            break;

        case "shelf":
            StorageShelf existingShelf = storageShelfDAO.findByShortCode(normalized);
            if (existingShelf != null && !String.valueOf(existingShelf.getId()).equals(locationId)) {
                return ShortCodeValidationResult
                        .invalid(String.format("Short code '%s' already exists for another shelf", normalized));
            }
            break;

        case "rack":
            StorageRack existingRack = storageRackDAO.findByShortCode(normalized);
            if (existingRack != null && !String.valueOf(existingRack.getId()).equals(locationId)) {
                return ShortCodeValidationResult
                        .invalid(String.format("Short code '%s' already exists for another rack", normalized));
            }
            break;

        default:
            return ShortCodeValidationResult.invalid("Invalid context: " + context);
        }

        // Unique
        return ShortCodeValidationResult.valid(normalized);
    }

    @Override
    public String checkShortCodeChangeWarning(String oldCode, String newCode, String locationId) {
        // No warning if old code is null (new location)
        if (StringUtils.isBlank(oldCode)) {
            return null;
        }

        // No warning if codes are the same
        if (oldCode.equalsIgnoreCase(newCode)) {
            return null;
        }

        // Generate warning message
        return String.format(
                "Changing short code from '%s' to '%s' will invalidate existing printed labels. "
                        + "Ensure all labels are reprinted with the new code.",
                oldCode.toUpperCase(), newCode.toUpperCase());
    }
}
