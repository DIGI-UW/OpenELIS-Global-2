package org.openelisglobal.qachecklist.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Sample QA Checklist operations. Handles QA verification
 * status for orders in Step 4 of the workflow. Checklist items are configured
 * via the Dictionary system (category: QAChecklistItem).
 */
@RestController
@RequestMapping("/rest/qa-checklist")
public class SampleQaChecklistRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SampleQaChecklistRestController.class);

    @Autowired
    private SampleQaChecklistService sampleQaChecklistService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private HttpServletRequest httpRequest;

    /**
     * Get all active checklist item configurations from the Dictionary. GET
     * /rest/qa-checklist/config
     *
     * @return list of active checklist items
     */
    @GetMapping("/config")
    public ResponseEntity<?> getChecklistConfig() {
        try {
            List<Dictionary> items = sampleQaChecklistService.getActiveChecklistItems();

            List<Map<String, Object>> response = new ArrayList<>();
            for (Dictionary item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("itemKey", item.getDictEntry());
                itemMap.put("displayOrder", item.getSortOrder());
                itemMap.put("isActive", "Y".equals(item.getIsActive()));
                // Use localAbbreviation as the display label
                itemMap.put("label", item.getLocalAbbreviation());
                // Also provide the localized name if available
                itemMap.put("localizedName", item.getLocalizedName());
                response.add(itemMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting QA checklist config", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get checklist config: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get QA checklist by sample ID. GET /rest/qa-checklist/{sampleId}
     *
     * @param sampleId the sample ID
     * @return the QA checklist or empty checklist if not found
     */
    @GetMapping("/{sampleId}")
    public ResponseEntity<?> getQaChecklist(@PathVariable String sampleId) {
        try {
            // Validate that sampleId is numeric
            try {
                Integer.parseInt(sampleId.trim());
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid sampleId: must be a numeric value");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            logger.info("Getting QA checklist for sample: {}", sampleId);

            SampleQaChecklist checklist = sampleQaChecklistService.findBySampleId(sampleId);
            List<Dictionary> activeItems = sampleQaChecklistService.getActiveChecklistItems();

            Map<String, Object> response = new HashMap<>();
            response.put("sampleId", sampleId);

            // Build verified items map with all active items
            Map<String, Boolean> verifiedItems = new HashMap<>();
            if (checklist != null) {
                verifiedItems = checklist.getVerifiedItems();
                response.put("id", checklist.getId());
                response.put("allRequiredVerified", checklist.getAllRequiredVerified());
                response.put("verifiedByUserId", checklist.getVerifiedByUserId());
                response.put("verifiedDate", checklist.getVerifiedDate());
            } else {
                response.put("allRequiredVerified", false);
            }

            // Ensure all active items are in the response
            for (Dictionary item : activeItems) {
                if (!verifiedItems.containsKey(item.getDictEntry())) {
                    verifiedItems.put(item.getDictEntry(), false);
                }
            }
            response.put("verifiedItems", verifiedItems);

            // Include config for frontend
            List<Map<String, Object>> configItems = new ArrayList<>();
            for (Dictionary item : activeItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemKey", item.getDictEntry());
                itemMap.put("label", item.getLocalAbbreviation());
                itemMap.put("localizedName", item.getLocalizedName());
                itemMap.put("displayOrder", item.getSortOrder());
                configItems.add(itemMap);
            }
            response.put("checklistItems", configItems);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting QA checklist for sample: {}", sampleId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get QA checklist: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get QA checklist by lab number (accession number). GET
     * /rest/qa-checklist/by-lab-number/{labNumber}
     *
     * @param labNumber the lab/accession number
     * @return the QA checklist or empty checklist if not found
     */
    @GetMapping("/by-lab-number/{labNumber}")
    public ResponseEntity<?> getQaChecklistByLabNumber(@PathVariable String labNumber) {
        try {
            logger.info("Getting QA checklist for lab number: {}", labNumber);

            // Find sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
            if (sample == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Sample not found for lab number: " + labNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            return getQaChecklist(sample.getId());
        } catch (Exception e) {
            logger.error("Error getting QA checklist for lab number: {}", labNumber, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get QA checklist: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Save or update QA checklist. POST /rest/qa-checklist
     *
     * Request body: { "sampleId": 123, "verifiedItems": { "patientInfoVerified":
     * true, "samplesVerified": true, "labelsVerified": false, "storageVerified":
     * false } }
     *
     * OR with labNumber: { "labNumber": "24050001234", "verifiedItems": { ... } }
     *
     * @param requestBody the QA checklist data
     * @return the saved checklist
     */
    @PostMapping("")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> saveQaChecklist(@RequestBody Map<String, Object> requestBody) {
        try {
            Integer sampleId = null;

            // Support both sampleId and labNumber in the request
            if (requestBody.containsKey("sampleId")) {
                Object sampleIdObj = requestBody.get("sampleId");
                if (sampleIdObj instanceof Number) {
                    sampleId = ((Number) sampleIdObj).intValue();
                } else if (sampleIdObj instanceof String) {
                    try {
                        sampleId = Integer.parseInt(((String) sampleIdObj).trim());
                    } catch (NumberFormatException e) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Invalid sampleId: must be a numeric value");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                    }
                }
            } else if (requestBody.containsKey("labNumber")) {
                String labNumber = (String) requestBody.get("labNumber");
                Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
                if (sample == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Sample not found for lab number: " + labNumber);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
                try {
                    sampleId = Integer.parseInt(sample.getId());
                } catch (NumberFormatException e) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid sample data");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            if (sampleId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Either sampleId or labNumber is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            logger.info("Saving QA checklist for sample: {}", sampleId);

            // Parse verified items
            Map<String, Boolean> verifiedItems = new HashMap<>();
            Object verifiedItemsObj = requestBody.get("verifiedItems");
            if (verifiedItemsObj instanceof Map) {
                Map<String, Object> itemsMap = (Map<String, Object>) verifiedItemsObj;
                for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                    verifiedItems.put(entry.getKey(), getBooleanValue(entry.getValue()));
                }
            }

            // Get current user ID
            Integer userId = null;
            try {
                String sysUserId = getSysUserId(httpRequest);
                if (sysUserId != null) {
                    userId = Integer.parseInt(sysUserId);
                }
            } catch (Exception e) {
                logger.warn("Could not get user ID: {}", e.getMessage());
            }

            SampleQaChecklist checklist = sampleQaChecklistService.saveOrUpdateChecklist(sampleId, verifiedItems,
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", checklist.getId());
            response.put("sampleId", checklist.getSampleId());
            response.put("verifiedItems", checklist.getVerifiedItems());
            response.put("allRequiredVerified", checklist.getAllRequiredVerified());
            response.put("verifiedByUserId", checklist.getVerifiedByUserId());
            response.put("verifiedDate", checklist.getVerifiedDate());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving QA checklist", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to save QA checklist");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Helper to convert various types to Boolean.
     */
    private Boolean getBooleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value);
        }
        return false;
    }
}
