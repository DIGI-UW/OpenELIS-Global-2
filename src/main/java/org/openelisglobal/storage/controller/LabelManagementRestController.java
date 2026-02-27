package org.openelisglobal.storage.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.service.LabelManagementService;
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
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    /**
     * Generate and return PDF label POST /rest/storage/{type}/{id}/print-label
     * Validates code exists before printing, returns error if missing
     */
    @PostMapping(value = "/{type}/{id}/print-label", produces = MediaType.APPLICATION_PDF_VALUE)
    public void printLabel(@PathVariable String type, @PathVariable String id, HttpServletResponse response)
            throws IOException {
        try {
            // Validate type
            if (!"device".equals(type) && !"shelf".equals(type) && !"rack".equals(type)) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter()
                        .write("{\"error\":\"Invalid location type. Must be 'device', 'shelf', or 'rack'\"}");
                return;
            }

            // Get location
            Object location = getLocationById(type, id);
            if (location == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Location not found\"}");
                return;
            }

            // Validate that location has a valid code for label printing
            // Code must exist and be ≤10 chars
            if (!labelManagementService.validateCodeExists(id, type)) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter()
                        .write("{\"error\":\"Code is required for label printing and must be ≤10 characters.\"}");
                return;
            }

            // Generate label (uses code from entity)
            ByteArrayOutputStream pdfStream;
            String userId = getCurrentUserId(); // Get from security context

            if (location instanceof StorageDevice) {
                pdfStream = labelManagementService.generateLabel((StorageDevice) location);
            } else if (location instanceof StorageShelf) {
                pdfStream = labelManagementService.generateLabel((StorageShelf) location);
            } else if (location instanceof StorageRack) {
                pdfStream = labelManagementService.generateLabel((StorageRack) location);
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Invalid location type\"}");
                return;
            }

            // Track print history (use code from location)
            String locationCode = getLocationCode(location);
            labelManagementService.trackPrintHistory(id, type, locationCode, userId);

            // Return PDF
            if (pdfStream == null || pdfStream.size() == 0) {
                logger.error("PDF stream is null or empty");
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Failed to generate label PDF\"}");
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
        } catch (IllegalArgumentException e) {
            // Handle code validation errors
            logger.error("Label printing validation error: " + e.getMessage(), e);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Error generating label: " + e.getClass().getName() + " - " + e.getMessage(), e);
            if (e.getCause() != null) {
                logger.error("Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Internal server error generating label\"}");
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
     * Helper method to get code from location
     */
    private String getLocationCode(Object location) {
        if (location instanceof StorageDevice) {
            return ((StorageDevice) location).getCode();
        } else if (location instanceof StorageShelf) {
            return ((StorageShelf) location).getCode();
        } else if (location instanceof StorageRack) {
            return ((StorageRack) location).getCode();
        }
        return null;
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
