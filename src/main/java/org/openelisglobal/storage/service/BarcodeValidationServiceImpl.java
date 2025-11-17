package org.openelisglobal.storage.service;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StoragePositionDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BarcodeValidationService Implements 5-step validation
 * process per FR-024 through FR-027
 *
 * Key features: - Two-step validation: existence check + hierarchy check -
 * Partial validation: continues through all levels even after failure - Tracks
 * first failure point for user feedback - Populates validComponents for form
 * pre-filling
 */
@Service
@Transactional(readOnly = true)
public class BarcodeValidationServiceImpl implements BarcodeValidationService {

    @Autowired
    private BarcodeParsingService barcodeParsingService;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Autowired
    private StoragePositionDAO storagePositionDAO;

    @Override
    public BarcodeValidationResponse validateBarcode(String barcode) {
        BarcodeValidationResponse response = new BarcodeValidationResponse();
        response.setBarcode(barcode);

        // Detect barcode type first
        String barcodeType = detectBarcodeType(barcode);
        response.setBarcodeType(barcodeType);

        // If it's a sample barcode, return early (don't validate as location)
        if ("sample".equals(barcodeType)) {
            response.setValid(false);
            response.setFailedStep("BARCODE_TYPE_MISMATCH");
            // Try to parse to get components, but use null if parsing fails
            ParsedBarcode sampleParsed = barcodeParsingService.parseBarcode(barcode);
            response.setErrorMessage(formatErrorMessage(barcode, sampleParsed,
                    "Scanned barcode appears to be a sample accession number, not a location barcode"));
            return response;
        }

        // If unknown type, still attempt validation but mark as unknown
        if ("unknown".equals(barcodeType)) {
            // Continue with validation attempt, but type is unknown
        }

        boolean isValid = true; // Assume valid until proven otherwise
        String firstFailedStep = null;
        String firstErrorMessage = null;

        // Step 1: Format Validation
        ParsedBarcode parsed = barcodeParsingService.parseBarcode(barcode);
        if (!parsed.isValid()) {
            response.setValid(false);
            response.setFailedStep("FORMAT_VALIDATION");
            response.setErrorMessage(formatErrorMessage(barcode, parsed, null));
            return response; // Can't continue without valid parse
        }

        // Step 2 & 3: Room validation (existence + hierarchy)
        StorageRoom room = storageRoomDAO.findByCode(parsed.getRoomCode());
        if (room == null) {
            isValid = false;
            firstFailedStep = "LOCATION_EXISTENCE";
            firstErrorMessage = "Room not found: " + parsed.getRoomCode();
        } else {
            response.addValidComponent("room", createComponentMap(room.getId(), room.getName(), room.getCode()));

            // Step 4: Room activity check
            if (room.getActive() == null || !room.getActive()) {
                if (isValid) { // Only record first failure
                    isValid = false;
                    firstFailedStep = "ACTIVITY_CHECK";
                    firstErrorMessage = "Room is inactive: " + room.getName();
                }
            }
        }

        // Step 2 & 3: Device validation (existence + hierarchy) - continue even if room
        // failed
        StorageDevice device = null;
        if (parsed.getDeviceCode() != null) {
            // First check: Does device code exist anywhere?
            StorageDevice deviceAny = storageDeviceDAO.findByCode(parsed.getDeviceCode());
            if (deviceAny == null) {
                if (isValid) { // Only record first failure
                    isValid = false;
                    firstFailedStep = "LOCATION_EXISTENCE";
                    firstErrorMessage = "Device not found: " + parsed.getDeviceCode();
                }
            } else if (room != null) {
                // Second check: Does it exist with correct parent?
                device = storageDeviceDAO.findByCodeAndParentRoom(parsed.getDeviceCode(), room);
                if (device == null) {
                    if (isValid) { // Only record first failure
                        isValid = false;
                        firstFailedStep = "HIERARCHY_VALIDATION";
                        firstErrorMessage = "Device '" + parsed.getDeviceCode()
                                + "' exists but parent hierarchy is incorrect (not in room '"
                                + (room.getName() != null ? room.getName() : room.getCode()) + "')";
                    }
                } else {
                    response.addValidComponent("device",
                            createComponentMap(device.getId(), device.getName(), device.getCode()));

                    // Step 4: Device activity check
                    if (device.getActive() == null || !device.getActive()) {
                        if (isValid) { // Only record first failure
                            isValid = false;
                            firstFailedStep = "ACTIVITY_CHECK";
                            firstErrorMessage = "Device is inactive: " + device.getName();
                        }
                    }
                }
            }
        }

        // Step 2 & 3: Shelf validation (existence + hierarchy) - continue even if
        // device failed
        StorageShelf shelf = null;
        if (parsed.getShelfCode() != null) {
            // First check: Does shelf label exist anywhere?
            StorageShelf shelfAny = storageShelfDAO.findByLabel(parsed.getShelfCode());
            if (shelfAny == null) {
                if (isValid) { // Only record first failure
                    isValid = false;
                    firstFailedStep = "LOCATION_EXISTENCE";
                    firstErrorMessage = "Shelf not found: " + parsed.getShelfCode();
                }
            } else if (device != null) {
                // Second check: Does it exist with correct parent?
                shelf = storageShelfDAO.findByLabelAndParentDevice(parsed.getShelfCode(), device);
                if (shelf == null) {
                    if (isValid) { // Only record first failure
                        isValid = false;
                        firstFailedStep = "HIERARCHY_VALIDATION";
                        firstErrorMessage = "Shelf '" + parsed.getShelfCode()
                                + "' exists but parent hierarchy is incorrect (not in device '"
                                + (device.getName() != null ? device.getName() : device.getCode()) + "')";
                    }
                } else {
                    response.addValidComponent("shelf",
                            createComponentMap(shelf.getId(), shelf.getLabel(), shelf.getLabel()));

                    // Step 4: Shelf activity check
                    if (shelf.getActive() == null || !shelf.getActive()) {
                        if (isValid) { // Only record first failure
                            isValid = false;
                            firstFailedStep = "ACTIVITY_CHECK";
                            firstErrorMessage = "Shelf is inactive: " + shelf.getLabel();
                        }
                    }
                }
            }
        }

        // Step 2 & 3: Rack validation (existence + hierarchy) - continue even if shelf
        // failed
        StorageRack rack = null;
        if (parsed.getRackCode() != null) {
            // First check: Does rack label exist anywhere?
            StorageRack rackAny = storageRackDAO.findByLabel(parsed.getRackCode());
            if (rackAny == null) {
                if (isValid) { // Only record first failure
                    isValid = false;
                    firstFailedStep = "LOCATION_EXISTENCE";
                    firstErrorMessage = "Rack not found: " + parsed.getRackCode();
                }
            } else if (shelf != null) {
                // Second check: Does it exist with correct parent?
                rack = storageRackDAO.findByLabelAndParentShelf(parsed.getRackCode(), shelf);
                if (rack == null) {
                    if (isValid) { // Only record first failure
                        isValid = false;
                        firstFailedStep = "HIERARCHY_VALIDATION";
                        firstErrorMessage = "Rack '" + parsed.getRackCode()
                                + "' exists but parent hierarchy is incorrect (not in shelf '" + shelf.getLabel()
                                + "')";
                    }
                } else {
                    response.addValidComponent("rack",
                            createComponentMap(rack.getId(), rack.getLabel(), rack.getLabel()));

                    // Step 4: Rack activity check
                    if (rack.getActive() == null || !rack.getActive()) {
                        if (isValid) { // Only record first failure
                            isValid = false;
                            firstFailedStep = "ACTIVITY_CHECK";
                            firstErrorMessage = "Rack is inactive: " + rack.getLabel();
                        }
                    }
                }
            }
        }

        // Step 2 & 3: Position validation (existence + hierarchy) - continue even if
        // rack failed
        StoragePosition position = null;
        if (parsed.getPositionCode() != null) {
            // First check: Does position coordinate exist anywhere?
            StoragePosition positionAny = storagePositionDAO.findByCoordinates(parsed.getPositionCode());
            if (positionAny == null) {
                if (isValid) { // Only record first failure
                    isValid = false;
                    firstFailedStep = "LOCATION_EXISTENCE";
                    firstErrorMessage = "Position not found: " + parsed.getPositionCode();
                }
            } else if (rack != null) {
                // Second check: Does it exist with correct parent?
                position = storagePositionDAO.findByCoordinatesAndParentRack(parsed.getPositionCode(), rack);
                if (position == null) {
                    if (isValid) { // Only record first failure
                        isValid = false;
                        firstFailedStep = "HIERARCHY_VALIDATION";
                        firstErrorMessage = "Position '" + parsed.getPositionCode()
                                + "' exists but parent hierarchy is incorrect (not in rack '" + rack.getLabel() + "')";
                    }
                } else {
                    response.addValidComponent("position",
                            createComponentMap(position.getId(), position.getCoordinate(), position.getCoordinate()));

                    // Note: StoragePosition doesn't have an active field - it inherits activity
                    // from its parent hierarchy
                }
            }
        }

        // Step 5: Conflict Check
        // Note: With the polymorphic location model (Phase 4), we check if the exact
        // location
        // (locationId + locationType + optional coordinate) is occupied
        // For barcode validation, we're validating the barcode format and hierarchy,
        // not checking occupancy at this level (that's done during assignment)

        // Set final response
        response.setValid(isValid);
        if (!isValid) {
            response.setFailedStep(firstFailedStep);
            // Format error message with raw barcode and parsed components
            response.setErrorMessage(formatErrorMessage(barcode, parsed, firstErrorMessage));
        }

        return response;
    }

    /**
     * Create a map with component details for form pre-filling
     */
    private Map<String, Object> createComponentMap(Integer id, String name, String code) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("code", code);
        return map;
    }

    /**
     * Format error message per FR-024g specification Format: "Scanned code:
     * {barcode} ({parsed components}). {specific error}" If parsing fails: "Scanned
     * code: {barcode}. Invalid barcode format."
     * 
     * @param rawBarcode    The original barcode string
     * @param parsed        The parsed barcode object (may be invalid)
     * @param specificError The specific error message
     * @return Formatted error message
     */
    private String formatErrorMessage(String rawBarcode, ParsedBarcode parsed, String specificError) {
        StringBuilder message = new StringBuilder();
        message.append("Scanned code: ").append(rawBarcode);

        // If parsing succeeded, include parsed components
        if (parsed != null && parsed.isValid()) {
            message.append(" (");
            boolean first = true;

            if (parsed.getRoomCode() != null) {
                message.append("Room: ").append(parsed.getRoomCode());
                first = false;
            }
            if (parsed.getDeviceCode() != null) {
                if (!first)
                    message.append(", ");
                message.append("Device: ").append(parsed.getDeviceCode());
                first = false;
            }
            if (parsed.getShelfCode() != null) {
                if (!first)
                    message.append(", ");
                message.append("Shelf: ").append(parsed.getShelfCode());
                first = false;
            }
            if (parsed.getRackCode() != null) {
                if (!first)
                    message.append(", ");
                message.append("Rack: ").append(parsed.getRackCode());
                first = false;
            }
            if (parsed.getPositionCode() != null) {
                if (!first)
                    message.append(", ");
                message.append("Position: ").append(parsed.getPositionCode());
            }

            message.append("). ");
        } else {
            // Parsing failed - just show raw barcode
            message.append(". ");
        }

        // Add specific error
        if (specificError != null && !specificError.isEmpty()) {
            message.append(specificError);
        } else if (parsed != null && !parsed.isValid() && parsed.getErrorMessage() != null) {
            message.append(parsed.getErrorMessage());
        } else {
            message.append("Invalid barcode format.");
        }

        return message.toString();
    }

    /**
     * Detect barcode type: location, sample, or unknown Location barcodes:
     * Hierarchical format with hyphens (e.g., "MAIN-FRZ01-SHA-RKR1") Sample
     * barcodes: Accession number formats (e.g., "25-00001", "S-2025-001")
     * 
     * @param barcode The barcode string to analyze
     * @return "location", "sample", or "unknown"
     */
    private String detectBarcodeType(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return "unknown";
        }

        // Try parsing as location barcode (hierarchical format)
        ParsedBarcode parsed = barcodeParsingService.parseBarcode(barcode);
        if (parsed.isValid()) {
            // Valid hierarchical format = location barcode
            return "location";
        }

        // Check if it matches sample accession number patterns
        // Common patterns:
        // - YY-XXXXX (year-based with hyphen, e.g., "25-00001")
        // - YYXXXXX (year-based without hyphen, e.g., "2500001")
        // - S-YYYY-NNNNN (site-based, e.g., "S-2025-001")
        // - Alphanumeric codes (e.g., "ABC123", "PROG-001")

        String trimmed = barcode.trim();

        // Pattern 1: YY-XXXXX or YYXXXXX (2-digit year + numbers)
        if (trimmed.matches("\\d{2}-?\\d{4,}")) {
            return "sample";
        }

        // Pattern 2: S-YYYY-NNNNN or similar site-based formats
        if (trimmed.matches("[A-Z]{1,4}-\\d{4}-\\d{3,}")) {
            return "sample";
        }

        // Pattern 3: Alphanumeric codes (letters + numbers, may have hyphens but not
        // hierarchical)
        // Only match if it looks like a valid sample format (not just any alphanumeric)
        // Valid sample formats typically have:
        // - Site prefix + year + sequence (e.g., "S-2025-001")
        // - Year-based with clear structure (e.g., "25-00001")
        // Exclude generic alphanumeric strings that don't match known patterns
        if (trimmed.matches("[A-Z0-9-]+") && !trimmed.matches(".*-.*-.*-.*")) {
            int hyphenCount = trimmed.length() - trimmed.replace("-", "").length();
            // Only classify as sample if it matches a known sample pattern structure
            // Generic alphanumeric strings should be "unknown"
            if (hyphenCount <= 2 && trimmed.length() <= 20) {
                // Check if it matches a clear sample pattern (year-based or site-based)
                // If it's just random alphanumeric, return unknown
                if (trimmed.matches("\\d{2}-?\\d{4,}") || trimmed.matches("[A-Z]{1,4}-\\d{4}-\\d{3,}")) {
                    return "sample";
                }
                // For other patterns, be conservative - return unknown
            }
        }

        // Pattern 4: Pure numeric (likely sample accession)
        if (trimmed.matches("\\d{5,}")) {
            return "sample";
        }

        // Default: unknown
        return "unknown";
    }
}
