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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.form.ReportForm;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;

public abstract class IndicatorReport extends Report {

    protected String lowerDateRange;
    protected String upperDateRange;
    protected Date lowDate;
    protected Date highDate;

    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE, getNameForReportRequest(),
                null).setRequestParameters(form);
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("startDate", lowerDateRange);
        reportParameters.put("stopDate", upperDateRange);
        reportParameters.put("siteId", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        reportParameters.put("directorName",
                ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName));
        reportParameters.put("labName1", getLabNameLine1());
        reportParameters.put("labName2", getLabNameLine2());
        reportParameters.put("reportTitle", getNameForReport());
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "CI LNSP")) {
            reportParameters.put("headerName", "CILNSPHeader.jasper");
        } else {
            reportParameters.put("headerName", "GeneralHeader.jasper");
        }
    }

    protected void setDateRange(ReportForm form) {
        errorFound = false;
        lowerDateRange = form.getLowerDateRange();
        upperDateRange = form.getUpperDateRange();

        if (GenericValidator.isBlankOrNull(lowerDateRange)) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.noPrintableItems"));
            errorMsgs.add(msgs);
        }

        if (GenericValidator.isBlankOrNull(upperDateRange)) {
            upperDateRange = lowerDateRange;
        }

        try {
            lowDate = DateUtil.convertStringDateToSqlDate(lowerDateRange);
            highDate = DateUtil.convertStringDateToSqlDate(upperDateRange);
        } catch (LIMSRuntimeException e) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.date.format"));
            errorMsgs.add(msgs);
        }
    }

    /**
     * Opens the report's PDF under the shared header, followed by the period and
     * the site code when one is configured.
     */
    protected Document startPdf(ByteArrayOutputStream out, String period) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out);
        List<String> nameLines = new ArrayList<>();
        for (String line : new String[] { getLabNameLine1(), getLabNameLine2() }) {
            // getContextualMessage returns the key itself when the deployment configured
            // none
            if (!GenericValidator.isBlankOrNull(line) && !line.startsWith("report.labName.")) {
                nameLines.add(line);
            }
        }
        ReportHeaderPdf.add(document, getNameForReport(), nameLines);
        Paragraph meta = new Paragraph(period, new Font(Font.HELVETICA, 10, Font.BOLD));
        String siteCode = ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode);
        if (!GenericValidator.isBlankOrNull(siteCode)) {
            meta.add(new Phrase("    " + MessageUtil.getMessage("datasubmission.siteid") + ": " + siteCode,
                    new Font(Font.HELVETICA, 10)));
        }
        meta.setSpacingBefore(6);
        meta.setSpacingAfter(8);
        document.add(meta);
        return document;
    }

    protected abstract String getNameForReportRequest();

    protected abstract String getNameForReport();

    protected abstract String getLabNameLine1();

    protected abstract String getLabNameLine2();
}
