package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Virology Dark Room Imaging operations.
 *
 * Handles imaging and fluorescence analysis data for virus culture samples.
 * Part of Page 6 in the Virology & Vaccine Unit workflow.
 *
 * Captures: - Image ID (reference to stored image) - CPE (Cytopathic Effect)
 * observations - Fluorescence intensity measurements - Imaging notes and
 * observations
 */
@RestController
@RequestMapping("/rest/virology/dark-room-imaging")
public class VirologyDarkRoomImagingRestController extends BaseRestController {

    private static final Logger log = LoggerFactory.getLogger(VirologyDarkRoomImagingRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Value("${org.openelisglobal.virology.imaging.upload.path:${java.io.tmpdir}/virology-images}")
    private String imageUploadPath;

    /**
     * Request object for dark room imaging data.
     */
    public static class ImagingDataRequest {
        private Integer notebookPageId;
        private List<Integer> sampleIds;
        private String imageId;
        private String cpeObservation;
        private String fluorescenceIntensity;
        private String notes;
        private List<Map<String, Object>> uploadedImages;

        // Getters and Setters
        public Integer getNotebookPageId() {
            return notebookPageId;
        }

        public void setNotebookPageId(Integer notebookPageId) {
            this.notebookPageId = notebookPageId;
        }

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public String getCpeObservation() {
            return cpeObservation;
        }

        public void setCpeObservation(String cpeObservation) {
            this.cpeObservation = cpeObservation;
        }

        public String getFluorescenceIntensity() {
            return fluorescenceIntensity;
        }

        public void setFluorescenceIntensity(String fluorescenceIntensity) {
            this.fluorescenceIntensity = fluorescenceIntensity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public List<Map<String, Object>> getUploadedImages() {
            return uploadedImages;
        }

        public void setUploadedImages(List<Map<String, Object>> uploadedImages) {
            this.uploadedImages = uploadedImages;
        }
    }

    /**
     * Save dark room imaging data for selected samples with optional base64 image
     * uploads.
     *
     * POST /rest/virology/dark-room-imaging
     *
     * Updates sample status to IN_PROGRESS after saving data. Accepts JSON with
     * optional base64-encoded images in uploadedImages array.
     *
     * @param httpRequest the HTTP request
     * @param request     the imaging data including optional base64 images
     * @return response with success status and updated count
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveImagingData(HttpServletRequest httpRequest,
            @RequestBody ImagingDataRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String sysUserId = getSysUserId(httpRequest);
            if (sysUserId == null) {
                response.put("success", false);
                response.put("error", "User session not found");
                return ResponseEntity.status(401).body(response);
            }

            // Validate request
            if (request.getNotebookPageId() == null) {
                response.put("success", false);
                response.put("error", "Notebook page ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
                response.put("success", false);
                response.put("error", "At least one sample ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build imaging data map
            Map<String, Object> imagingData = new HashMap<>();

            if (request.getImageId() != null && !request.getImageId().trim().isEmpty()) {
                imagingData.put("imageId", request.getImageId().trim());
            }

            if (request.getCpeObservation() != null && !request.getCpeObservation().isEmpty()) {
                imagingData.put("cpeObservation", request.getCpeObservation());
            }

            if (request.getFluorescenceIntensity() != null && !request.getFluorescenceIntensity().trim().isEmpty()) {
                imagingData.put("fluorescenceIntensity", request.getFluorescenceIntensity().trim());
            }

            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                imagingData.put("imagingNotes", request.getNotes().trim());
            }

            // Add uploaded images (stored as base64 in JSONB)
            if (request.getUploadedImages() != null && !request.getUploadedImages().isEmpty()) {
                imagingData.put("uploadedImages", request.getUploadedImages());
                log.info("Storing {} base64-encoded images", request.getUploadedImages().size());
            }

            log.info("Dark room imaging data to apply: {} field(s)", imagingData.size());

            // Apply data to all selected samples
            int updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId(),
                    request.getSampleIds(), imagingData, sysUserId);

            log.info("Successfully updated {} samples with imaging data", updatedCount);

            // Update sample status to IN_PROGRESS
            int statusUpdatedCount = notebookPageSampleService.bulkUpdateStatus(request.getNotebookPageId(),
                    request.getSampleIds(),
                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS, sysUserId);

            log.info("Updated status to IN_PROGRESS for {} samples", statusUpdatedCount);

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Dark room imaging data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving dark room imaging data", e);
            response.put("success", false);
            response.put("error", "An error occurred while saving imaging data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Save uploaded image files to disk and return their paths.
     *
     * @param images         array of uploaded image files
     * @param notebookPageId notebook page ID for organizing files
     * @return list of saved file paths
     * @throws IOException if file save fails
     */
    private List<String> saveUploadedImages(MultipartFile[] images, Integer notebookPageId) throws IOException {
        List<String> savedPaths = new ArrayList<>();

        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(imageUploadPath, "page-" + notebookPageId);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Created image upload directory: {}", uploadDir);
        }

        for (MultipartFile image : images) {
            if (image.isEmpty()) {
                continue;
            }

            // Generate unique filename to avoid conflicts
            String originalFilename = image.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Store relative path
            String relativePath = "page-" + notebookPageId + "/" + uniqueFilename;
            savedPaths.add(relativePath);

            log.info("Saved image file: {} (original: {})", uniqueFilename, originalFilename);
        }

        return savedPaths;
    }
}
