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
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.form.ReportForm;
import org.openpdf.text.Document;
import org.openpdf.text.Rectangle;

public abstract class IndicatorReport extends Report {

    protected String lowerDateRange;
    protected String upperDateRange;
    protected Date lowDate;
    protected Date highDate;

    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE, getNameForReportRequest(),
                null).setRequestParameters(form);
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
        return startPdf(out, PdfExportSupport.pageSize(), period);
    }

    /**
     * As above, on the given page size, with further heading lines after the first.
     */
    protected Document startPdf(ByteArrayOutputStream out, Rectangle pageSize, String period, String... moreLines) {
        Document document = new Document(pageSize, 36, 36, 36, 48);
        List<String> nameLines = new ArrayList<>();
        for (String line : new String[] { getLabNameLine1(), getLabNameLine2() }) {
            // getContextualMessage returns the key itself when the deployment configured
            // none
            if (!GenericValidator.isBlankOrNull(line) && !line.startsWith("report.labName.")) {
                nameLines.add(line);
            }
        }
        List<String> scope = new ArrayList<>();
        String meta = period;
        String siteCode = ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode);
        if (!GenericValidator.isBlankOrNull(siteCode)) {
            meta += "    " + MessageUtil.getMessage("datasubmission.siteid") + ": " + siteCode;
        }
        scope.add(meta);
        scope.addAll(List.of(moreLines));
        ReportHeaderPdf.openRepeatingWithFooter(document, out, getNameForReport(), nameLines,
                scope.toArray(String[]::new),
                MessageUtil.getMessage("referral.report.date") + ": " + DateUtil.getCurrentDateAsText());
        return document;
    }

    protected abstract String getNameForReportRequest();

    protected abstract String getNameForReport();

    protected abstract String getLabNameLine1();

    protected abstract String getLabNameLine2();
}
