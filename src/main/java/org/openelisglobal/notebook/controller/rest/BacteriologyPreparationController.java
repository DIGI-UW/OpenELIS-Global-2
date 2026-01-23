package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.BacteriologyPreparationService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Bacteriology Laboratory preparation operations. Handles
 * culture media preparation, biochemical media, and antibiotic IQC tracking.
 *
 * Endpoints: - GET/POST
 * /rest/notebook-entry/{entryId}/preparation/culture-media - GET/POST
 * /rest/notebook-entry/{entryId}/preparation/biochemical-media - GET/POST
 * /rest/notebook-entry/{entryId}/preparation/antibiotic-iqc
 */
@RestController
@RequestMapping(value = "/rest/notebook-entry")
public class BacteriologyPreparationController extends BaseRestController {

    @Autowired
    private BacteriologyPreparationService bacteriologyPreparationService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    // ==========================================
    // CULTURE MEDIA PREPARATION ENDPOINTS
    // ==========================================

    /**
     * Get all culture media preparations for a notebook entry. GET
     * /rest/notebook-entry/{entryId}/preparation/culture-media
     *
     * @param entryId the notebook entry ID
     * @return list of culture media preparations
     */
    @GetMapping(value = "/{entryId}/preparation/culture-media", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCultureMediaPreparations(@PathVariable("entryId") Integer entryId) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Map<String, Object>> preparations = bacteriologyPreparationService
                    .getCultureMediaPreparations(entryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entryId", entryId);
            response.put("preparations", preparations);
            response.put("total", preparations.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get culture media preparations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Save a new culture media preparation. POST
     * /rest/notebook-entry/{entryId}/preparation/culture-media
     *
     * @param entryId     the notebook entry ID
     * @param preparation the preparation data
     * @param httpRequest for getting user session
     * @return the saved preparation
     */
    @PostMapping(value = "/{entryId}/preparation/culture-media", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCultureMediaPreparation(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> preparation, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> saved = bacteriologyPreparationService.saveCultureMediaPreparation(entryId, preparation,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", saved);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to save culture media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Update a culture media preparation. PUT
     * /rest/notebook-entry/{entryId}/preparation/culture-media/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   the updated preparation data
     * @param httpRequest   for getting user session
     * @return the updated preparation
     */
    @PutMapping(value = "/{entryId}/preparation/culture-media/{preparationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCultureMediaPreparation(@PathVariable("entryId") Integer entryId,
            @PathVariable("preparationId") Integer preparationId, @RequestBody Map<String, Object> preparation,
            HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> updated = bacteriologyPreparationService.updateCultureMediaPreparation(entryId,
                    preparationId, preparation, sysUserId);

            if (updated == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", updated);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update culture media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Delete a culture media preparation. DELETE
     * /rest/notebook-entry/{entryId}/preparation/culture-media/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param httpRequest   for getting user session
     * @return success status
     */
    @DeleteMapping(value = "/{entryId}/preparation/culture-media/{preparationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteCultureMediaPreparation(@PathVariable("entryId") Integer entryId,
            @PathVariable("preparationId") Integer preparationId, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            boolean deleted = bacteriologyPreparationService.deleteCultureMediaPreparation(entryId, preparationId,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete culture media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==========================================
    // BIOCHEMICAL MEDIA PREPARATION ENDPOINTS
    // ==========================================

    /**
     * Get all biochemical media preparations for a notebook entry. GET
     * /rest/notebook-entry/{entryId}/preparation/biochemical-media
     *
     * @param entryId the notebook entry ID
     * @return list of biochemical media preparations
     */
    @GetMapping(value = "/{entryId}/preparation/biochemical-media", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBiochemicalMediaPreparations(
            @PathVariable("entryId") Integer entryId) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Map<String, Object>> preparations = bacteriologyPreparationService
                    .getBiochemicalMediaPreparations(entryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entryId", entryId);
            response.put("preparations", preparations);
            response.put("total", preparations.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get biochemical media preparations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Save a new biochemical media preparation. POST
     * /rest/notebook-entry/{entryId}/preparation/biochemical-media
     *
     * @param entryId     the notebook entry ID
     * @param preparation the preparation data
     * @param httpRequest for getting user session
     * @return the saved preparation
     */
    @PostMapping(value = "/{entryId}/preparation/biochemical-media", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveBiochemicalMediaPreparation(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> preparation, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> saved = bacteriologyPreparationService.saveBiochemicalMediaPreparation(entryId,
                    preparation, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", saved);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to save biochemical media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Update a biochemical media preparation. PUT
     * /rest/notebook-entry/{entryId}/preparation/biochemical-media/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   the updated preparation data
     * @param httpRequest   for getting user session
     * @return the updated preparation
     */
    @PutMapping(value = "/{entryId}/preparation/biochemical-media/{preparationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateBiochemicalMediaPreparation(
            @PathVariable("entryId") Integer entryId, @PathVariable("preparationId") Integer preparationId,
            @RequestBody Map<String, Object> preparation, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> updated = bacteriologyPreparationService.updateBiochemicalMediaPreparation(entryId,
                    preparationId, preparation, sysUserId);

            if (updated == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", updated);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update biochemical media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Delete a biochemical media preparation. DELETE
     * /rest/notebook-entry/{entryId}/preparation/biochemical-media/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param httpRequest   for getting user session
     * @return success status
     */
    @DeleteMapping(value = "/{entryId}/preparation/biochemical-media/{preparationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBiochemicalMediaPreparation(
            @PathVariable("entryId") Integer entryId, @PathVariable("preparationId") Integer preparationId,
            HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            boolean deleted = bacteriologyPreparationService.deleteBiochemicalMediaPreparation(entryId, preparationId,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete biochemical media preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==========================================
    // ANTIBIOTIC IQC ENDPOINTS
    // ==========================================

    /**
     * Get all antibiotic IQC preparations for a notebook entry. GET
     * /rest/notebook-entry/{entryId}/preparation/antibiotic-iqc
     *
     * @param entryId the notebook entry ID
     * @return list of antibiotic IQC preparations
     */
    @GetMapping(value = "/{entryId}/preparation/antibiotic-iqc", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAntibioticIqcPreparations(@PathVariable("entryId") Integer entryId) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Map<String, Object>> preparations = bacteriologyPreparationService
                    .getAntibioticIqcPreparations(entryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entryId", entryId);
            response.put("preparations", preparations);
            response.put("total", preparations.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get antibiotic IQC preparations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Save a new antibiotic IQC preparation. POST
     * /rest/notebook-entry/{entryId}/preparation/antibiotic-iqc
     *
     * @param entryId     the notebook entry ID
     * @param preparation the preparation data
     * @param httpRequest for getting user session
     * @return the saved preparation
     */
    @PostMapping(value = "/{entryId}/preparation/antibiotic-iqc", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAntibioticIqcPreparation(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> preparation, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> saved = bacteriologyPreparationService.saveAntibioticIqcPreparation(entryId,
                    preparation, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", saved);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to save antibiotic IQC preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Update an antibiotic IQC preparation. PUT
     * /rest/notebook-entry/{entryId}/preparation/antibiotic-iqc/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   the updated preparation data
     * @param httpRequest   for getting user session
     * @return the updated preparation
     */
    @PutMapping(value = "/{entryId}/preparation/antibiotic-iqc/{preparationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAntibioticIqcPreparation(@PathVariable("entryId") Integer entryId,
            @PathVariable("preparationId") Integer preparationId, @RequestBody Map<String, Object> preparation,
            HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            Map<String, Object> updated = bacteriologyPreparationService.updateAntibioticIqcPreparation(entryId,
                    preparationId, preparation, sysUserId);

            if (updated == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preparation", updated);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update antibiotic IQC preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Delete an antibiotic IQC preparation. DELETE
     * /rest/notebook-entry/{entryId}/preparation/antibiotic-iqc/{preparationId}
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param httpRequest   for getting user session
     * @return success status
     */
    @DeleteMapping(value = "/{entryId}/preparation/antibiotic-iqc/{preparationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAntibioticIqcPreparation(@PathVariable("entryId") Integer entryId,
            @PathVariable("preparationId") Integer preparationId, HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            boolean deleted = bacteriologyPreparationService.deleteAntibioticIqcPreparation(entryId, preparationId,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete antibiotic IQC preparation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==========================================
    // REFERENCE DATA ENDPOINTS
    // ==========================================

    /**
     * Get available culture media types. GET
     * /rest/notebook-entry/preparation/culture-media-types
     *
     * @return list of available culture media types
     */
    @GetMapping(value = "/preparation/culture-media-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCultureMediaTypes() {
        List<Map<String, String>> mediaTypes = bacteriologyPreparationService.getCultureMediaTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("mediaTypes", mediaTypes);
        response.put("total", mediaTypes.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available biochemical test types. GET
     * /rest/notebook-entry/preparation/biochemical-test-types
     *
     * @return list of available biochemical test types
     */
    @GetMapping(value = "/preparation/biochemical-test-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBiochemicalTestTypes() {
        List<Map<String, String>> testTypes = bacteriologyPreparationService.getBiochemicalTestTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("testTypes", testTypes);
        response.put("total", testTypes.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available antibiotic types for IQC. GET
     * /rest/notebook-entry/preparation/antibiotic-types
     *
     * @return list of available antibiotic types
     */
    @GetMapping(value = "/preparation/antibiotic-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAntibioticTypes() {
        List<Map<String, String>> antibioticTypes = bacteriologyPreparationService.getAntibioticTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("antibioticTypes", antibioticTypes);
        response.put("total", antibioticTypes.size());

        return ResponseEntity.ok(response);
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
