package org.openelisglobal.labelpreset.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.service.LabelPresetService;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for label preset admin CRUD (M3). Provides 6 endpoints per
 * openapi.yaml contract. Security: hasRole('ADMIN') — consistent with the
 * rest-of-repo approach.
 */
@RestController
@RequestMapping("/api/labelPresets")
@PreAuthorize("hasRole('ADMIN')")
public class LabelPresetRestController {

    @Autowired
    private LabelPresetService labelPresetService;

    // ── GET /api/labelPresets ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<LabelPreset>> listPresets(@RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "barcodeType", required = false) String barcodeType) {

        Boolean activeOnly = null;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            activeOnly = true;
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            activeOnly = false;
        }

        BarcodeType parsedType = null;
        if (barcodeType != null) {
            try {
                parsedType = BarcodeType.valueOf(barcodeType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok(labelPresetService.list(activeOnly, parsedType));
    }

    // ── POST /api/labelPresets ────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Object> createPreset(HttpServletRequest request, @RequestBody @Valid LabelPresetForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorBody(result));
        }
        try {
            LabelPreset created = labelPresetService.create(form, getSysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(singleError("name", e.getMessage()));
        }
    }

    // ── GET /api/labelPresets/{id} ────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<LabelPreset> getPreset(@PathVariable Integer id) {
        LabelPreset preset = labelPresetService.get(id);
        if (preset == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(preset);
    }

    // ── PUT /api/labelPresets/{id} ────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<Object> updatePreset(HttpServletRequest request, @PathVariable Integer id,
            @RequestBody @Valid LabelPresetForm form, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorBody(result));
        }
        try {
            LabelPreset updated = labelPresetService.update(id, form, getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(singleError("isActive", e.getMessage()));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(singleError("name", msg));
        }
    }

    // ── PATCH /api/labelPresets/{id}/activate ─────────────────────────────────

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Object> activatePreset(HttpServletRequest request, @PathVariable Integer id,
            @RequestBody @Valid ActivateRequest body, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorBody(result));
        }
        try {
            LabelPreset updated = labelPresetService.toggleActive(id, body.getIsActive(), getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(singleError("is_active", e.getMessage()));
        }
    }

    // ── POST /api/labelPresets/{id}/duplicate ─────────────────────────────────

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Object> duplicatePreset(HttpServletRequest request, @PathVariable Integer id,
            @RequestBody @Valid DuplicateRequest body, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorBody(result));
        }
        try {
            LabelPreset copy = labelPresetService.duplicate(id, body.getName(), getSysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(copy);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(singleError("name", msg));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String getSysUserId(HttpServletRequest request) {
        Object sessionData = request.getSession()
                .getAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA);
        if (sessionData instanceof org.openelisglobal.login.valueholder.UserSessionData) {
            return String
                    .valueOf(((org.openelisglobal.login.valueholder.UserSessionData) sessionData).getSystemUserId());
        }
        return "1";
    }

    private Map<String, Object> buildErrorBody(BindingResult result) {
        Map<String, Object> body = new HashMap<>();
        body.put("fieldErrors", result.getFieldErrors().stream().map(fe -> {
            Map<String, String> entry = new HashMap<>();
            entry.put("field", fe.getField());
            entry.put("defaultMessage", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "");
            return entry;
        }).collect(Collectors.toList()));
        body.put("globalErrors",
                result.getGlobalErrors().stream()
                        .map(oe -> oe.getDefaultMessage() != null ? oe.getDefaultMessage() : oe.getCode())
                        .collect(Collectors.toList()));
        return body;
    }

    private Map<String, Object> singleError(String field, String message) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> fe = new HashMap<>();
        fe.put("field", field);
        fe.put("defaultMessage", message != null ? message : "");
        body.put("fieldErrors", List.of(fe));
        body.put("globalErrors", List.of());
        return body;
    }

    // ── Nested request beans ──────────────────────────────────────────────────

    /** Request body for PATCH /{id}/activate */
    public static class ActivateRequest {
        @NotNull(message = "{error.labelpreset.activate.isActive.required}")
        private Boolean isActive;

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }

    /** Request body for POST /{id}/duplicate */
    public static class DuplicateRequest {
        @NotBlank(message = "{error.labelpreset.name.required}")
        @Size(max = 120, message = "{error.labelpreset.name.toolong}")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
