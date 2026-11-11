package org.openelisglobal.validationpolicy.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.service.ValidationPolicyRefusedException;
import org.openelisglobal.validationpolicy.service.ValidationPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OGC-1364: the lab-wide default validation policy on Result Configuration. */
@RestController
@RequestMapping("/rest/validation-policy")
@PreAuthorize("hasRole('ADMIN')")
public class ValidationPolicyRestController extends BaseRestController {

    @Autowired
    private ValidationPolicyService validationPolicyService;

    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationPolicyView getLabWideDefault() {
        return validationPolicyService.getLabWideDefault();
    }

    @PutMapping(value = "/default", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationPolicyView saveLabWideDefault(HttpServletRequest request, @RequestBody ValidationPolicyForm form) {
        ValidationPolicyView saved = validationPolicyService.saveLabWideDefault(form, getSysUserId(request));
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        return saved;
    }

    @ExceptionHandler(ValidationPolicyRefusedException.class)
    public ResponseEntity<Map<String, String>> refused(ValidationPolicyRefusedException refusal) {
        return ResponseEntity.badRequest().body(Map.of("field", refusal.getField(), "code", refusal.getCode()));
    }
}
