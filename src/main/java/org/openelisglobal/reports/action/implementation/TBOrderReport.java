/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.reports.action.implementation;

import net.sf.jasperreports.engine.JRDataSource;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.form.ReportForm;

/** */
public class TBOrderReport extends Report implements IReportCreator, IReportParameterSetter {
    private String unitName;

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.activity.report.base") + " " + MessageUtil.getMessage("report.by.unit"),
                MessageUtil.getMessage("report.instruction.all.fields")).setRequestParameters(form);
        new ReportSpecificationList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE),
                MessageUtil.getMessage("workplan.unit.types")).setRequestParameters(form);
    }

    @Override
    public void initializeReport(ReportForm form) {
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String reportFileName() {
        // TODO Auto-generated method stub
        return null;
    }
}
