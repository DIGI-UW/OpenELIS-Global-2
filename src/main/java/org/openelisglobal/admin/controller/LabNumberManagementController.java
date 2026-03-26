package org.openelisglobal.admin.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.openelisglobal.admin.form.LabNumberManagementForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.TemplateTokenEngine;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabNumberManagementController {

    private static final int MAX_TEMPLATE_LENGTH = 50;
    private static final int MAX_REGEX_LENGTH = 200;
    private static final Pattern SEQ_TOKEN_PATTERN = Pattern.compile(".*\\{SEQ:\\d+\\}.*");
    private static final Pattern ALPHANUMSEQ_TOKEN_PATTERN = Pattern.compile(".*\\{ALPHANUMSEQ:\\d+\\}.*");

    @Autowired
    private SiteInformationService siteInformationService;

    @GetMapping("/rest/labnumbermanagement")
    public LabNumberManagementForm getValues() {
        LabNumberManagementForm form = new LabNumberManagementForm();

        form.setAlphanumPrefix(
                ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.ALPHANUM_ACCESSION_PREFIX));
        form.setLabNumberType(AccessionFormat
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat)));
        form.setUsePrefix("true".equals(
                ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALPHANUM_ACCESSION_PREFIX)));
        form.setCustomAccessionRegex(
                ConfigurationProperties.getInstance().getPropertyValue(Property.CUSTOM_ACCESSION_REGEX));
        form.setCustomAccessionTemplate(
                ConfigurationProperties.getInstance().getPropertyValue(Property.CUSTOM_ACCESSION_TEMPLATE));

        return form;
    }

    @PostMapping("/rest/labnumbermanagement")
    public ResponseEntity<?> setValues(@Valid @RequestBody LabNumberManagementForm form) {
        // Validate CUSTOM configuration
        if (form.getLabNumberType() == AccessionFormat.CUSTOM) {
            String validationError = validateCustomConfig(form);
            if (validationError != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", validationError));
            }
        }

        Map<String, String> map = new HashMap<>();

        map.put(Property.ALPHANUM_ACCESSION_PREFIX.getDBName(),
                form.getAlphanumPrefix() != null ? form.getAlphanumPrefix().toUpperCase() : "");
        map.put(Property.AccessionFormat.getDBName(), form.getLabNumberType().name());
        map.put(Property.USE_ALPHANUM_ACCESSION_PREFIX.getDBName(), form.getUsePrefix().toString());
        map.put(Property.CUSTOM_ACCESSION_REGEX.getDBName(),
                form.getCustomAccessionRegex() != null ? form.getCustomAccessionRegex() : "");
        map.put(Property.CUSTOM_ACCESSION_TEMPLATE.getDBName(),
                form.getCustomAccessionTemplate() != null ? form.getCustomAccessionTemplate() : "");
        siteInformationService.updateSiteInformationByName(map);

        ConfigurationProperties.loadDBValuesIntoConfiguration();
        return ResponseEntity.ok(form);
    }

    @PostMapping("/rest/labNumber/preview")
    public ResponseEntity<List<String>> generatePreview(@RequestBody PreviewRequest request) {
        List<String> previews = new ArrayList<>();
        String template = request.getTemplate();
        long seqStart = request.getSeqStart();
        String prefix = request.getPrefix();

        for (int i = 0; i < 3; i++) {
            String generated = TemplateTokenEngine.processTokens(template, seqStart + i, prefix);
            previews.add(generated);
        }

        return ResponseEntity.ok(previews);
    }

    public static class PreviewRequest {
        private String template;
        private long seqStart;
        private String prefix;

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public long getSeqStart() {
            return seqStart;
        }

        public void setSeqStart(long seqStart) {
            this.seqStart = seqStart;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    private String validateCustomConfig(LabNumberManagementForm form) {
        String regex = form.getCustomAccessionRegex();
        String template = form.getCustomAccessionTemplate();

        // Validate regex is present and not empty
        if (regex == null || regex.trim().isEmpty()) {
            return "Custom validation regex is required when using CUSTOM format";
        }

        // Validate regex length
        if (regex.length() > MAX_REGEX_LENGTH) {
            return "Custom regex exceeds maximum length of " + MAX_REGEX_LENGTH + " characters";
        }

        // Validate template is present and not empty
        if (template == null || template.trim().isEmpty()) {
            return "Custom generation template is required when using CUSTOM format";
        }

        // Validate template length
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            return "Custom template exceeds maximum length of " + MAX_TEMPLATE_LENGTH + " characters";
        }

        // Validate template contains sequence token
        boolean hasSeqToken = SEQ_TOKEN_PATTERN.matcher(template).matches();
        boolean hasAlphaNumToken = ALPHANUMSEQ_TOKEN_PATTERN.matcher(template).matches();

        if (!hasSeqToken && !hasAlphaNumToken) {
            return "Custom template must contain a sequence token like {SEQ:N} or {ALPHANUMSEQ:N}";
        }

        // Validate regex starts with ^ and ends with $
        if (!regex.startsWith("^") || !regex.endsWith("$")) {
            return "Custom regex must start with ^ and end with $ for proper validation";
        }

        // Try to compile regex to verify it's valid
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            return "Invalid regex pattern: " + e.getMessage();
        }

        return null;
    }
}
