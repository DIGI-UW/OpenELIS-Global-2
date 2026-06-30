package org.openelisglobal.admin.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.admin.form.LabNumberManagementForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class LabNumberManagementController {

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
        String alphanumRegex = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ALPHANUM_ACCESSION_REGEX);
        String siteYearnumRegex = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SITEYEARNUM_ACCESSION_REGEX);
        form.setAlphanumRegex(alphanumRegex == null ? "" : alphanumRegex);
        form.setSiteYearnumRegex(siteYearnumRegex == null ? "" : siteYearnumRegex);

        return form;
    }

    @PostMapping("/rest/labnumbermanagement")
    public LabNumberManagementForm setValues(@Valid @RequestBody LabNumberManagementForm form) {
        Map<String, String> map = new HashMap<>();

        map.put(Property.ALPHANUM_ACCESSION_PREFIX.getDBName(),
                form.getAlphanumPrefix() != null ? form.getAlphanumPrefix().toUpperCase() : "");
        map.put(Property.AccessionFormat.getDBName(), form.getLabNumberType().name());
        map.put(Property.USE_ALPHANUM_ACCESSION_PREFIX.getDBName(), form.getUsePrefix().toString());
        map.put(Property.ALPHANUM_ACCESSION_REGEX.getDBName(),
                form.getAlphanumRegex() != null ? form.getAlphanumRegex() : "");
        map.put(Property.SITEYEARNUM_ACCESSION_REGEX.getDBName(),
                form.getSiteYearnumRegex() != null ? form.getSiteYearnumRegex() : "");
        siteInformationService.updateSiteInformationByName(map);

        ConfigurationProperties.loadDBValuesIntoConfiguration();
        return form;
    }
}
