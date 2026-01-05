package org.openelisglobal.virology.controller.rest;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.virology.service.VirologyProgressionService;
import org.openelisglobal.virology.form.VirologyProgressionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST Controller for managing virology workflow progression between stages.
 * Handles sample advancement from Stage 1 (Reception) → Stage 2 (Culture) → Stage 3 (Vaccine Development)
 */
@Controller
@RequestMapping("/rest/notebook/virology/progression")
public class VirologyProgressionRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyProgressionRestController.class);

    @Autowired
    private VirologyProgressionService virologyProgressionService;

    /**
     * Advances samples from one virology workflow stage to the next.
     * Validates stage completion requirements before allowing progression.
     *
     * @param form Contains entryId, fromStage, toStage, and sampleIds
     * @param result Validation results
     * @param request HTTP request context
     * @return ResponseEntity with progression result
     */
    @PostMapping("/advance")
    @ResponseBody
    public ResponseEntity<?> advanceSamples(
            @Valid @RequestBody VirologyProgressionForm form,
            BindingResult result,
            HttpServletRequest request) {

        // Validate form input
        if (result.hasErrors()) {
            String errors = result.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
            return ResponseEntity.badRequest()
                .body("Invalid progression request: " + errors);
        }

        try {
            // Validate stage progression rules
            ValidationResult validation = virologyProgressionService.validateProgression(
                form.getEntryId(),
                form.getFromStage(),
                form.getToStage(),
                form.getSampleIds()
            );

            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                    .body("Progression validation failed: " + String.join(", ", validation.getErrors()));
            }

            // Execute the progression
            ProgressionResult progressionResult = virologyProgressionService.advanceSamples(
                form.getEntryId(),
                form.getFromStage(),
                form.getToStage(),
                form.getSampleIds(),
                getSysUserId(request)
            );

            if (progressionResult.isSuccess()) {
                return ResponseEntity.ok()
                    .body("Successfully progressed " + form.getSampleIds().size() +
                          " sample(s) from " + form.getFromStage() + " to " + form.getToStage());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Progression failed: " + progressionResult.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error during virology progression", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error during progression: " + e.getMessage());
        }
    }

    /**
     * Inner class for validation results
     */
    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

    }

    /**
     * Inner class for progression results
     */
    @Getter
    public static class ProgressionResult {
        private final boolean success;
        private final String errorMessage;
        private final int samplesProgressed;

        public ProgressionResult(boolean success, String errorMessage, int samplesProgressed) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.samplesProgressed = samplesProgressed;
        }

    }
}