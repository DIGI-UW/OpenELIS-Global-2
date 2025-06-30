/**
 * bd* The contents of this file are subject to the Mozilla Public License Version 1.1 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.util;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationSideEffects {
    @Autowired
    private RoleService roleService;
    @Autowired
    private SiteInformationService siteInformationService;

    public void siteInformationChanged(SiteInformation siteInformation) {
        if (Property.DEFAULT_LANG_LOCALE.getDBName().equals(siteInformation.getName())) {
            // this is done in SiteInformationController.java as we need to have the user
            // request to change the locale
        }

        if (Property.roleRequiredForModifyResults.getDBName().equals(siteInformation.getName())) {
            Role modifierRole = roleService.getRoleByName("Results modifier");

            if (modifierRole != null && modifierRole.getId() != null) {
                modifierRole.setActive("true".equals(siteInformation.getValue()));
                modifierRole.setSysUserId(siteInformation.getSysUserId());
                roleService.update(modifierRole);
            }
        }

        if (Property.SiteCode.getDBName().equals(siteInformation.getName())) {
            SiteInformation accessionFormat = siteInformationService.getSiteInformationByName("acessionFormat");
            if ("SITEYEARNUM".equals(accessionFormat.getValue())) {
                SiteInformation accessionPrefix = siteInformationService
                        .getSiteInformationByName("Accession number prefix");
                if (GenericValidator.isBlankOrNull(accessionPrefix.getValue())) {
                    accessionPrefix.setValue(siteInformation.getValue());
                    accessionPrefix.setSysUserId(siteInformation.getSysUserId());
                    siteInformationService.update(accessionPrefix);
                }
            }
        }
    }
}
