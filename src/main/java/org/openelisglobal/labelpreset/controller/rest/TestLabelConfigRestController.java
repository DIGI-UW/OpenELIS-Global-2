package org.openelisglobal.labelpreset.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.labelpreset.form.TestLabelConfigForm;
import org.openelisglobal.labelpreset.service.TestLabelConfigService;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for test-level label configuration (OGC-285 M4).
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET /rest/api/tests/{id}/labelConfig}</li>
 * <li>{@code PUT /rest/api/tests/{id}/labelConfig}</li>
 * </ul>
 * Secured with {@code @PreAuthorize("hasRole('ADMIN')")} — consistent with
 * BarcodeConfigurationRestController and existing admin REST endpoints.
 */
@RestController
@RequestMapping("/rest/api/tests")
@PreAuthorize("hasRole('ADMIN')")
public class TestLabelConfigRestController extends BaseRestController {

    @Autowired
    private TestLabelConfigService testLabelConfigService;

    /**
     * GET /rest/api/tests/{id}/labelConfig Returns the current label configuration
     * for the given test, or a default (toggle=true, no links) if none persisted.
     */
    @GetMapping(value = "/{id}/labelConfig")
    public ResponseEntity<Map<String, Object>> getLabelConfig(@PathVariable("id") String testId) {
        Optional<TestLabelConfig> configOpt = testLabelConfigService.getByTestId(testId);
        List<TestLabelPresetLink> links = testLabelConfigService.getLinksByTestId(testId);

        Map<String, Object> body = buildResponseBody(configOpt, links);
        return ResponseEntity.ok(body);
    }

    /**
     * PUT /rest/api/tests/{id}/labelConfig Full-replace the label configuration for
     * the test. Returns 422 with error details if:
     * <ul>
     * <li>any linked preset is order-only (prints_per_sample=false)</li>
     * <li>there are duplicate presetId entries in the links list</li>
     * </ul>
     */
    @PutMapping(value = "/{id}/labelConfig")
    public ResponseEntity<Object> putLabelConfig(@PathVariable("id") String testId,
            @RequestBody @Valid TestLabelConfigForm form, BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("fieldErrors", buildFieldErrors(bindingResult));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
        }

        try {
            String sysUserId = ControllerUtills.getSysUserId(request);
            testLabelConfigService.replace(testId, form, sysUserId);
        } catch (IllegalStateException e) {
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
        }

        // Re-read the persisted state and return it
        Optional<TestLabelConfig> configOpt = testLabelConfigService.getByTestId(testId);
        List<TestLabelPresetLink> links = testLabelConfigService.getLinksByTestId(testId);
        return ResponseEntity.ok(buildResponseBody(configOpt, links));
    }

    private Map<String, Object> buildResponseBody(Optional<TestLabelConfig> configOpt,
            List<TestLabelPresetLink> links) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowOrderEntryOverride", configOpt.map(TestLabelConfig::getAllowOrderEntryOverride).orElse(true));

        List<Map<String, Object>> linkList = new ArrayList<>();
        for (TestLabelPresetLink link : links) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", link.getId());
            entry.put("presetId", link.getPreset().getId());
            entry.put("presetName",
                    link.getPreset().getName() != null ? link.getPreset().getName() : link.getPreset().getId());
            entry.put("defaultQty", link.getDefaultQty());
            entry.put("maxQty", link.getMaxQty());
            entry.put("allowOverride", link.getAllowOverride());
            linkList.add(entry);
        }
        body.put("links", linkList);
        return body;
    }

    private List<Map<String, String>> buildFieldErrors(BindingResult bindingResult) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(fe -> {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("field", fe.getField());
            error.put("code", fe.getCode());
            fieldErrors.add(error);
        });
        return fieldErrors;
    }
}
