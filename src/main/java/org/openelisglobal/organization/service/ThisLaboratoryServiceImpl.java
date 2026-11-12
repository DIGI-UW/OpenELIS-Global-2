package org.openelisglobal.organization.service;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThisLaboratoryServiceImpl implements ThisLaboratoryService {

    private static final String SYSTEM_USER = "1";

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SiteInformationService siteInformationService;

    /**
     * The record named by the setting; before the setting is written (an install
     * that has not started since the upgrade), the record with the short name the
     * system gave it.
     */
    @Override
    public Organization find() {
        String id = recordedId();
        if (id != null) {
            Organization organization = organizationService.getOrganizationById(id);
            if (organization != null) {
                return organization;
            }
        }
        return organizationService.getOrganizationByShortName(FACILITY_ORG_SHORT_NAME, false);
    }

    @Override
    public boolean isThisLaboratory(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return false;
        }
        Organization thisLaboratory = find();
        return thisLaboratory != null && organization.getId().equals(thisLaboratory.getId());
    }

    @Override
    public void record(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        SiteInformation setting = siteInformationService.getSiteInformationByName(SETTING);
        if (setting == null || organization.getId().equals(setting.getValue())) {
            return;
        }
        setting.setValue(organization.getId());
        setting.setSysUserId(GenericValidator.isBlankOrNull(organization.getSysUserId()) ? SYSTEM_USER
                : organization.getSysUserId());
        siteInformationService.update(setting);
    }

    private String recordedId() {
        SiteInformation setting = siteInformationService.getSiteInformationByName(SETTING);
        if (setting == null || GenericValidator.isBlankOrNull(setting.getValue())
                || !setting.getValue().trim().matches("\\d+")) {
            return null;
        }
        return setting.getValue().trim();
    }
}
