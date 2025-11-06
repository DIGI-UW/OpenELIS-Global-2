/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class HaitiNonConformityByDate extends NonConformityByDate implements IReportCreator {

    public HaitiNonConformityByDate() {
    }

    @Override
    protected String getHeaderName() {
        return "GeneralHeader.jasper";
    }

    @Override
    protected void createReportParameters() throws IllegalStateException {
        super.createReportParameters();
        reportParameters.put("supportStudy", "false");
        reportParameters.put("supportService", "false");
        reportParameters.put("supportSiteSubject", "false");
        reportParameters.put("labName1", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        reportParameters.put("labName2", "");
    }
}
