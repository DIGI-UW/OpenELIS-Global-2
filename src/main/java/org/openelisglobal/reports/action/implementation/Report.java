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

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperRunManager;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.i18n.LocaleContextHolder;

public abstract class Report implements IReportCreator {

    private ImageService imageService = SpringContext.getBean(ImageService.class);
    private OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    public static final String ERROR_REPORT = "NoticeOfReportError";

    protected static final String CSV = "csv";

    protected boolean initialized = false;
    protected boolean errorFound = false;
    protected List<ErrorMessages> errorMsgs = new ArrayList<>();
    protected HashMap<String, Object> reportParameters = null;
    protected String requestedReport;
    private String fullReportFilename;
    protected String systemUserId;

    @Override
    public void setRequestedReport(String report) {
        requestedReport = report;
    }

    @Override
    public void setSystemUserId(String id) {
        systemUserId = id;
    }

    protected void initializeReport() {
        initialized = true;
    }

    @Override
    public String getResponseHeaderName() {
        return null;
    }

    @Override
    public String getResponseHeaderContent() {
        return null;
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.IReportCreator#getContentType()
     */
    @Override
    public String getContentType() {
        return "application/pdf; charset=UTF-8";
    }

    /**
     * Make sure we have a reportParameters map and make sure there is lab director
     * in that map (for any possible error report). All reports need a director name
     * either in their header including or on their error report page."
     */
    protected void createReportParameters() {
        reportParameters = (reportParameters != null) ? reportParameters : new HashMap<>();
        reportParameters.put("directorName",
                ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName));
        reportParameters.put("siteName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        reportParameters.put("additionalSiteInfo",
                ConfigurationProperties.getInstance().getPropertyValue(Property.ADDITIONAL_SITE_INFO));
        reportParameters.put("usePageNumbers",
                ConfigurationProperties.getInstance().getPropertyValue(Property.USE_PAGE_NUMBERS_ON_REPORTS));
        // reportParameters.put("localization", createLocalizationMap());
        Optional<Image> leftLogo = imageService.getImageBySiteInfoName("headerLeftImage");
        Optional<Image> rightLogo = imageService.getImageBySiteInfoName("headerRightImage");
        if (leftLogo.isPresent()) {
            reportParameters.put("leftHeaderImage", new ByteArrayInputStream(leftLogo.get().getImage()));
        }
        if (rightLogo.isPresent()) {
            reportParameters.put("rightHeaderImage", new ByteArrayInputStream(rightLogo.get().getImage()));
        }
        reportParameters.put(JRParameter.REPORT_LOCALE, LocaleContextHolder.getLocale());
        reportParameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, MessageUtil.getMessageSourceAsResourceBundle());
    }

    @Override
    public byte[] runReport() throws UnsupportedEncodingException, IOException, SQLException, IllegalStateException,
            JRException, ParseException {
        return JasperRunManager.runReportToPdf(fullReportFilename, getReportParameters(), getReportDataSource());
    }

    public abstract JRDataSource getReportDataSource() throws IllegalStateException;

    @Override
    public HashMap<String, Object> getReportParameters() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }
        return reportParameters != null ? reportParameters : new HashMap<>();
    }

    /**
     * Utility routine for a sequence done in many places. Adds a message to the
     * errorMsgs
     *
     * @param messageId - name of resource
     */
    protected void add1LineErrorMessage(String messageId) {
        errorFound = true;
        ErrorMessages msgs = new ErrorMessages();
        msgs.setMsgLine1(MessageUtil.getMessage(messageId));
        errorMsgs.add(msgs);
    }

    /**
     * Utility routine for a sequence done in many places. Adds a message to the
     * errorMsgs
     *
     * @param messageId - name of resource
     */
    protected void add1LineErrorMessage(String messageId, String more) {
        errorFound = true;
        ErrorMessages msgs = new ErrorMessages();
        msgs.setMsgLine1(MessageUtil.getMessage(messageId) + more);
        errorMsgs.add(msgs);
    }

    /**
     * Checks a given date to make sure it is ok, filling in with a default if not
     * found, logging a message, if there is a problem.
     *
     * @param checkDateStr   - date to check
     * @param defaultDateStr - will use this date if the 1st one is null or blank.
     * @param badDateMessage - message to report if the date is bad (blank or not
     *                       valid form).
     * @return Date
     */
    protected Date validateDate(String checkDateStr, String defaultDateStr, String badDateMessage) {
        checkDateStr = isBlankOrNull(checkDateStr) ? defaultDateStr : checkDateStr;
        Date checkDate;
        if (isBlankOrNull(checkDateStr)) {
            add1LineErrorMessage(badDateMessage);
            return null;
        }

        try {
            checkDate = DateUtil.convertStringDateToSqlDate(checkDateStr);
        } catch (LIMSRuntimeException e) {
            add1LineErrorMessage("report.error.message.date.format", " " + checkDateStr);
            return null;
        }
        return checkDate;
    }

    /**
     * @return true, if location is not blank or "0" is is found in the DB; false
     *         otherwise
     */
    protected Organization getValidOrganization(String locationStr) {
        if (isBlankOrNull(locationStr) || "0".equals(locationStr)) {
            add1LineErrorMessage("report.error.message.location.missing");
            return null;
        }
        Organization org = organizationService.getOrganizationById(locationStr);
        if (org == null) {
            add1LineErrorMessage("report.error.message.location.missing");
            return null;
        }
        return org;
    }

    public String getReportFileName() {
        return errorFound ? ERROR_REPORT : reportFileName();
    }

    public class DateRange {
        private String lowDateStr;
        private String highDateStr;
        private Date lowDate;
        private Date highDate;

        public Date getLowDate() {
            return lowDate;
        }

        public Date getHighDate() {
            return highDate;
        }

        /**
         * If you need to compare a Date which started as a date string to a bunch of
         * timestamps, you should move it from 00:00 at the beginning of the day to the
         * end of the day at 23:59:59.999.
         *
         * @return the high date with time set to the end of the day.
         */
        public Date getHighDateAtEndOfDay() {
            // not perfect in areas with Daylight Savings Time. Will over shoot
            // on the spring forward day and undershoot on the fall back day.
            return new Date(highDate.getTime() + 24 * 60 * 60 * 1000);
        }

        public DateRange(String lowDateStr, String highDateStr) {
            this.lowDateStr = lowDateStr;
            this.highDateStr = highDateStr;
        }

        /**
         *
         *
         * <ol>
         * <li>High date picks up low date if it ain't filled in,
         * <li>they can't both be empty
         * <li>they have to be well formed.
         *
         * @return true if valid, false otherwise
         */
        public boolean validateHighLowDate(String missingDateMessage) {
            lowDate = validateDate(lowDateStr, null, missingDateMessage);
            highDate = validateDate(highDateStr, lowDateStr, missingDateMessage);
            if (lowDate == null || highDate == null) {
                return false;
            }
            if (highDate.getTime() < lowDate.getTime()) {
                Date tmpDate = highDate;
                highDate = lowDate;
                lowDate = tmpDate;

                String tmpString = highDateStr;
                highDateStr = lowDateStr;
                lowDateStr = tmpString;
            }
            return true;
        }

        @Override
        public String toString() {
            String range = lowDateStr;
            try {
                if (!GenericValidator.isBlankOrNull(highDateStr)) {
                    range += "  -  " + highDateStr;
                }
            } catch (RuntimeException e) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "persistPatientType", "ignoring exception");
            }
            return range;
        }

        public String getLowDateStr() {
            return lowDateStr;
        }

        public String getHighDateStr() {
            if (isBlankOrNull(highDateStr) && highDate != null) {
                highDateStr = DateUtil.convertSqlDateToStringDate(highDate);
            }
            return highDateStr;
        }
    }

    @Override
    public void setReportPath(String path) {
        fullReportFilename = path + getReportFileName() + ".jasper";
    }

    @Override
    public List<String> getReportedOrders() {
        return new ArrayList<>();
    }

    protected abstract String reportFileName();
}
