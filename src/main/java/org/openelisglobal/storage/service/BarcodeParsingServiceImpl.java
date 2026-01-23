package org.openelisglobal.storage.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Implementation of BarcodeParsingService Parses storage location barcodes per
 * FR-023
 */
@Service
public class BarcodeParsingServiceImpl implements BarcodeParsingService {

    private static final String DELIMITER = "-";
    private static final int MIN_LEVELS = 2;
    private static final int MAX_LEVELS = 5;

    @Override
    public ParsedBarcode parseBarcode(String barcode) {
        ParsedBarcode parsed = new ParsedBarcode();

        // Validate input
        if (barcode == null || barcode.trim().isEmpty()) {
            parsed.setValid(false);
            parsed.setErrorMessage("Barcode cannot be null or empty");
            return parsed;
        }

        // Validate format
        if (!validateFormat(barcode)) {
            parsed.setValid(false);
            parsed.setErrorMessage("Invalid barcode format. Expected format: ROOM-DEVICE[-SHELF[-RACK[-POSITION]]]");
            return parsed;
        }

        // Extract components
        List<String> components = extractComponents(barcode);
        if (components.isEmpty()) {
            parsed.setValid(false);
            parsed.setErrorMessage("Failed to extract components from barcode");
            return parsed;
        }

        // Populate ParsedBarcode based on number of components
        parsed.setComponents(components);
        parsed.setRoomCode(components.get(0));

        if (components.size() >= 2) {
            parsed.setDeviceCode(components.get(1));
        }
        if (components.size() >= 3) {
            parsed.setShelfCode(components.get(2));
        }
        if (components.size() >= 4) {
            parsed.setRackCode(components.get(3));
        }
        if (components.size() >= 5) {
            parsed.setPositionCode(components.get(4));
        }

        parsed.setValid(true);
        return parsed;
    }

    @Override
    public boolean validateFormat(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }

        // Check for invalid delimiters (underscore, dot, slash, etc.)
        if (barcode.contains("_") || barcode.contains(".") || barcode.contains("/") || barcode.contains("\\")) {
            return false;
        }

        // Must contain hyphen delimiter
        if (!barcode.contains(DELIMITER)) {
            return false;
        }

        // Count components
        String[] parts = barcode.split(DELIMITER);
        int componentCount = parts.length;

        // Must have 2-5 levels
        if (componentCount < MIN_LEVELS || componentCount > MAX_LEVELS) {
            return false;
        }

        // Each component must be non-empty
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<String> extractComponents(String barcode) {
        List<String> components = new ArrayList<>();

        if (!validateFormat(barcode)) {
            return components; // Return empty list for invalid barcode
        }

        String[] parts = barcode.split(DELIMITER);
        components.addAll(Arrays.asList(parts));

        return components;
    }
}
