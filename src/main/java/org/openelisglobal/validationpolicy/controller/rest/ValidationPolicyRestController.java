package org.openelisglobal.validationpolicy.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.validationpolicy.form.LabUnitActiveForm;
import org.openelisglobal.validationpolicy.form.LabUnitPoliciesView;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.LevelRoleOption;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.service.ValidationPolicyRefusedException;
import org.openelisglobal.validationpolicy.service.ValidationPolicyService;
import org.openelisglobal.validationpolicy.service.ValidationPolicyStaleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1364: the lab-wide default validation policy and the lab unit overrides
 * on Result Configuration. A save from a page that loaded an older version is
 * answered 409 with who changed the policy and when (OGC-1017).
 */
@RestController
@RequestMapping("/rest/validation-policy")
@PreAuthorize("hasRole('ADMIN')")
public class ValidationPolicyRestController extends BaseRestController {

    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Autowired
    private StaleSaveGuard staleSaveGuard;

    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationPolicyView getLabWideDefault() {
        return validationPolicyService.getLabWideDefault();
    }

    @PutMapping(value = "/default", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationPolicyView saveLabWideDefault(HttpServletRequest request, @RequestBody ValidationPolicyForm form) {
        ValidationPolicyView saved = lostRaceIsStale(null,
                () -> validationPolicyService.saveLabWideDefault(form, getSysUserId(request)));
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        return saved;
    }

    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LevelRoleOption> getLevelRoleOptions(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String labUnitId) {
        return validationPolicyService.levelRoleOptions(labUnitId);
    }

    @GetMapping(value = "/lab-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public LabUnitPoliciesView getLabUnitPolicies() {
        return validationPolicyService.getLabUnitPolicies();
    }

    @PostMapping(value = "/lab-units", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public LabUnitPolicyView addLabUnitOverride(HttpServletRequest request, @RequestBody LabUnitPolicyForm form) {
        return validationPolicyService.addLabUnitOverride(form, getSysUserId(request));
    }

    @PutMapping(value = "/lab-units/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public LabUnitPolicyView saveLabUnitOverride(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ValidationPolicyForm form) {
        return lostRaceIsStale(id, () -> validationPolicyService.saveLabUnitOverride(id, form, getSysUserId(request)));
    }

    @PutMapping(value = "/lab-units/{id}/active", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public LabUnitPolicyView setLabUnitOverrideActive(HttpServletRequest request, @PathVariable Long id,
            @RequestBody LabUnitActiveForm form) {
        if (form == null || form.active() == null) {
            throw new ValidationPolicyRefusedException("active", "activeRequired");
        }
        return lostRaceIsStale(id, () -> validationPolicyService.setLabUnitOverrideActive(id, form.active(),
                form.lastupdated(), getSysUserId(request)));
    }

    /**
     * A save that passed the version check but lost to another save committed in
     * the meantime fails Hibernate's optimistic lock; it is the same stale save,
     * refused with the policy as it is now stored ({@code null} is the default).
     */
    private <T> T lostRaceIsStale(Long labUnitOverrideId, Supplier<T> save) {
        try {
            return save.get();
        } catch (RuntimeException e) {
            if (StaleSaveGuard.isOptimisticLockFailure(e)) {
                throw validationPolicyService.lostRace(labUnitOverrideId);
            }
            throw e;
        }
    }

    @ExceptionHandler(ValidationPolicyStaleException.class)
    public ResponseEntity<Map<String, Object>> stale(ValidationPolicyStaleException stale) {
        Map<String, Object> body = new LinkedHashMap<>(
                staleSaveGuard.conflictBody("admin.validationPolicy.refused.stale", "validation_policy",
                        stale.getPolicyId() == null ? null : String.valueOf(stale.getPolicyId()), stale.getCurrent()));
        body.put("conflict", "stale");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ValidationPolicyRefusedException.class)
    public ResponseEntity<Map<String, String>> refused(ValidationPolicyRefusedException refusal) {
        return ResponseEntity.badRequest().body(Map.of("field", refusal.getField(), "code", refusal.getCode()));
    }
}
