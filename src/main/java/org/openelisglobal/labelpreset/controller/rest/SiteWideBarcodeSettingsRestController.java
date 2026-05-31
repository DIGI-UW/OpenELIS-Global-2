package org.openelisglobal.labelpreset.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for site-wide barcode/pre-print settings. These settings are
 * hosted in Lab Number Management (LNM) per OGC-771 clarification, not in
 * LabelPresetList. The controller itself lives in the labelpreset package.
 *
 * <p>
 * Reads/writes the {@code prePrintUseAltAccession} and
 * {@code prePrintAltAccessionPrefix} keys in {@code site_information}.
 */
@RestController
@RequestMapping("/api/siteSettings/barcode")
@PreAuthorize("hasRole('ADMIN')")
public class SiteWideBarcodeSettingsRestController {

    @Autowired
    private SiteInformationService siteInformationService;

    @GetMapping
    public ResponseEntity<SiteBarcodePreprintSettings> getSettings() {
        Boolean useAltAccession = Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX));
        String altAccessionPrefix = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ALT_ACCESSION_PREFIX);

        SiteBarcodePreprintSettings settings = new SiteBarcodePreprintSettings();
        settings.setPrePrintUseAltAccession(useAltAccession);
        settings.setPrePrintAltAccessionPrefix(altAccessionPrefix);
        return ResponseEntity.ok(settings);
    }

    @PostMapping
    public ResponseEntity<Object> saveSettings(HttpServletRequest request,
            @RequestBody @Valid SiteBarcodePreprintSettings body, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorBody(result));
        }

        String sysUserId = getSysUserId(request);

        // Persist prePrintUseAltAccession
        persistSiteInformationKey(Property.USE_ALT_ACCESSION_PREFIX,
                String.valueOf(body.getPrePrintUseAltAccession() != null ? body.getPrePrintUseAltAccession() : false),
                sysUserId);

        // Persist prePrintAltAccessionPrefix
        if (body.getPrePrintAltAccessionPrefix() != null) {
            persistSiteInformationKey(Property.ALT_ACCESSION_PREFIX, body.getPrePrintAltAccessionPrefix(), sysUserId);
        }

        // Reload cached configuration after update
        ConfigurationProperties.loadDBValuesIntoConfiguration();

        return ResponseEntity.ok(body);
    }

    private void persistSiteInformationKey(Property property, String value, String sysUserId) {
        String keyName = property.getDBName();
        SiteInformation si = siteInformationService.getSiteInformationByName(keyName);
        boolean isNew = (si == null);
        if (isNew) {
            si = new SiteInformation();
            si.setName(keyName);
            si.setValueType("text");
        }
        si.setValue(value);
        si.setSysUserId(sysUserId);
        siteInformationService.persistData(si, isNew);
    }

    private String getSysUserId(HttpServletRequest request) {
        Object sessionData = request.getSession()
                .getAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA);
        if (sessionData instanceof org.openelisglobal.login.valueholder.UserSessionData) {
            return String
                    .valueOf(((org.openelisglobal.login.valueholder.UserSessionData) sessionData).getSytemUserId());
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

    /** DTO for site-wide barcode pre-print settings. */
    public static class SiteBarcodePreprintSettings {

        private Boolean prePrintUseAltAccession;
        private String prePrintAltAccessionPrefix;

        public Boolean getPrePrintUseAltAccession() {
            return prePrintUseAltAccession;
        }

        public void setPrePrintUseAltAccession(Boolean prePrintUseAltAccession) {
            this.prePrintUseAltAccession = prePrintUseAltAccession;
        }

        public String getPrePrintAltAccessionPrefix() {
            return prePrintAltAccessionPrefix;
        }

        public void setPrePrintAltAccessionPrefix(String prePrintAltAccessionPrefix) {
            this.prePrintAltAccessionPrefix = prePrintAltAccessionPrefix;
        }
    }
}
