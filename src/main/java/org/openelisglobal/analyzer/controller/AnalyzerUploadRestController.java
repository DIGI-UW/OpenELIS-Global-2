package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.analyzer.form.AnalyzerRunPreviewForm;
import org.openelisglobal.analyzer.form.SubmitRequestForm;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.internationalization.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for analyzer file upload (preview + submit). Contract path:
 * /rest/analyzers/{analyzerId}/upload/preview and .../submit (OGC-324).
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping("/rest/analyzers")
public class AnalyzerUploadRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerUploadRestController.class);

    @Autowired
    private FileImportService fileImportService;

    /**
     * POST /rest/analyzers/{analyzerId}/upload/preview — multipart file upload,
     * returns parsed preview with validation.
     */
    @PostMapping(value = "/{analyzerId}/upload/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPreview(@PathVariable Integer analyzerId, @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    MessageUtil.getMessageOrDefault("file.import.rest.error.file_required", null, "File is required"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
            AnalyzerRunPreviewForm preview = fileImportService.parseAndPreview(analyzerId, file.getInputStream(),
                    filename, getSysUserId(request));
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            logger.error("Upload preview failed for analyzer " + analyzerId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    MessageUtil.getMessageOrDefault("file.import.rest.error.generic", null, "An error occurred"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzers/{analyzerId}/upload/submit — submit after preview
     * (previewSessionId = upload ID, optional excludedRows).
     */
    @PostMapping("/{analyzerId}/upload/submit")
    public ResponseEntity<?> uploadSubmit(@PathVariable Integer analyzerId, @RequestBody SubmitRequestForm request,
            HttpServletRequest httpRequest) {
        if (request == null || request.getPreviewSessionId() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", MessageUtil.getMessageOrDefault("file.import.rest.error.preview_session_required", null,
                    "previewSessionId is required"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        try {
            fileImportService.submitResults(analyzerId, request, getSysUserId(httpRequest));
            Map<String, Object> body = new HashMap<>();
            body.put("message", MessageUtil.getMessageOrDefault("file.import.rest.message.queued", null,
                    "Results queued for import"));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Upload submit failed for analyzer " + analyzerId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error",
                    MessageUtil.getMessageOrDefault("file.import.rest.error.generic", null, "An error occurred"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
