package org.openelisglobal.storage.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.form.ShortCodeUpdateForm;
import org.openelisglobal.storage.service.LabelManagementService;
import org.openelisglobal.storage.service.ShortCodeValidationService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Label Management Handles short code updates, label
 * generation, and print history
 */
@RestController
@RequestMapping("/rest/storage")
public class LabelManagementRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(LabelManagementRestController.class);

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private ShortCodeValidationService shortCodeValidationService;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    /**
     * Update short code for a storage location PUT
     * /rest/storage/{type}/{id}/short-code Body: { "shortCode": "FRZ01" }
     */
    @PutMapping("/{type}/{id}/short-code")
    public ResponseEntity<Map<String, Object>> updateShortCode(@PathVariable String type, @PathVariable String id,
            @Valid @RequestBody ShortCodeUpdateForm form) {
        try {
            // Validate type
            if (!"device".equals(type) && !"shelf".equals(type) && !"rack".equals(type)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid location type. Must be 'device', 'shelf', or 'rack'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get existing location
            Object location = getLocationById(type, id);
            if (location == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Location not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Get current short code (will be null if field doesn't exist yet)
            String currentShortCode = getCurrentShortCode(location);

            // Validate format
            if (form.getShortCode() != null && !form.getShortCode().trim().isEmpty()) {
                var formatResult = shortCodeValidationService.validateFormat(form.getShortCode());
                if (!formatResult.isValid()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", formatResult.getErrorMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }

                // Validate uniqueness
                var uniquenessResult = shortCodeValidationService.validateUniqueness(formatResult.getNormalizedCode(),
                        type, id);
                if (!uniquenessResult.isValid()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", uniquenessResult.getErrorMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }

                // Check for warning on change
                String warning = shortCodeValidationService.checkShortCodeChangeWarning(currentShortCode,
                        formatResult.getNormalizedCode(), id);
                if (warning != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("warning", warning);
                    response.put("shortCode", formatResult.getNormalizedCode());
                    // Update short code in database
                    updateShortCodeInDatabase(location, formatResult.getNormalizedCode());
                    return ResponseEntity.ok(response);
                }

                // Update short code in database
                updateShortCodeInDatabase(location, formatResult.getNormalizedCode());
                Map<String, Object> response = new HashMap<>();
                response.put("shortCode", formatResult.getNormalizedCode());
                response.put("message", "Short code updated successfully");
                return ResponseEntity.ok(response);
            } else {
                // Clear short code
                updateShortCodeInDatabase(location, null);
                Map<String, Object> response = new HashMap<>();
                response.put("shortCode", null);
                response.put("message", "Short code cleared successfully");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error updating short code", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Generate and return PDF label POST
     * /rest/storage/{type}/{id}/print-label?shortCode=FRZ01
     */
    @PostMapping(value = "/{type}/{id}/print-label", produces = MediaType.APPLICATION_PDF_VALUE)
    public void printLabel(@PathVariable String type, @PathVariable String id,
            @RequestParam(required = false) String shortCode, HttpServletResponse response) throws IOException {
        try {
            // Validate type
            if (!"device".equals(type) && !"shelf".equals(type) && !"rack".equals(type)) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }

            // Get location
            Object location = getLocationById(type, id);
            if (location == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            // Generate label
            ByteArrayOutputStream pdfStream;
            String userId = getCurrentUserId(); // Get from security context

            if (location instanceof StorageDevice) {
                pdfStream = labelManagementService.generateLabel((StorageDevice) location, shortCode);
            } else if (location instanceof StorageShelf) {
                pdfStream = labelManagementService.generateLabel((StorageShelf) location, shortCode);
            } else if (location instanceof StorageRack) {
                pdfStream = labelManagementService.generateLabel((StorageRack) location, shortCode);
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }

            // Track print history
            String currentShortCode = getCurrentShortCode(location);
            labelManagementService.trackPrintHistory(id, type, currentShortCode != null ? currentShortCode : shortCode,
                    userId);

            // Return PDF
            if (pdfStream == null || pdfStream.size() == 0) {
                logger.error("PDF stream is null or empty");
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return;
            }

            // Write directly to response like other PDF controllers in the codebase
            byte[] pdfBytes = pdfStream.toByteArray();
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=label.pdf");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            logger.error("Error generating label: " + e.getClass().getName() + " - " + e.getMessage(), e);
            if (e.getCause() != null) {
                logger.error("Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Get print history for a location GET /rest/storage/{type}/{id}/print-history
     */
    @GetMapping("/{type}/{id}/print-history")
    public ResponseEntity<List<Map<String, Object>>> getPrintHistory(@PathVariable String type,
            @PathVariable String id) {
        try {
            // Validate type
            if (!"device".equals(type) && !"shelf".equals(type) && !"rack".equals(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Verify location exists
            Object location = getLocationById(type, id);
            if (location == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // TODO: Implement when print history table is added in Phase 5.4
            // For now, return empty list
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            logger.error("Error getting print history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to get location by type and ID
     */
    private Object getLocationById(String type, String id) {
        try {
            Integer locationId = Integer.parseInt(id);
            switch (type) {
            case "device":
                return storageDeviceDAO.get(locationId).orElse(null);
            case "shelf":
                return storageShelfDAO.get(locationId).orElse(null);
            case "rack":
                return storageRackDAO.get(locationId).orElse(null);
            default:
                return null;
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid location ID format: " + id, e);
            return null;
        }
    }

    /**
     * Helper method to get current short code from location
     */
    private String getCurrentShortCode(Object location) {
        if (location instanceof StorageDevice) {
            return ((StorageDevice) location).getShortCode();
        } else if (location instanceof StorageShelf) {
            return ((StorageShelf) location).getShortCode();
        } else if (location instanceof StorageRack) {
            return ((StorageRack) location).getShortCode();
        }
        return null;
    }

    /**
     * Update short code in database
     */
    private void updateShortCodeInDatabase(Object location, String shortCode) {
        if (location instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) location;
            device.setShortCode(shortCode);
            storageDeviceDAO.update(device);
        } else if (location instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) location;
            shelf.setShortCode(shortCode);
            storageShelfDAO.update(shelf);
        } else if (location instanceof StorageRack) {
            StorageRack rack = (StorageRack) location;
            rack.setShortCode(shortCode);
            storageRackDAO.update(rack);
        }
    }

    /**
     * Get current user ID from security context TODO: Implement proper security
     * context retrieval
     */
    private String getCurrentUserId() {
        // Placeholder: should get from Spring Security context
        // For now, return default system user
        return "1";
    }
}
