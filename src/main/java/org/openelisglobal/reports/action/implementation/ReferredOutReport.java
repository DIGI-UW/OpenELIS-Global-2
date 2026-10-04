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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralReason;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Feb 18, 2011
 */
public class ReferredOutReport extends PatientReport implements IReportParameterSetter, IReportCreator {

    private String lowDateStr;
    private String highDateStr;
    private String locationId;
    private DateRange dateRange;

    private OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    private Organization reportLocation;

    /**
     * @see org.openelisglobal.reports.action.implementation.IReportParameterSetter#setRequestParameters(org.openelisglobal.common.action.BaseActionForm)
     */
    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            List<Organization> list = organizationService.getOrganizationsByTypeName("organizationName", "referralLab");
            form.setReportName(getReportNameForParameterPage());
            form.setUseLocationCode(true);
            form.setLocationCodeList(list);
            form.setUseLowerDateRange(true);
            form.setUseUpperDateRange(true);
            form.setInstructions(MessageUtil.getMessage("instructions.report.referral"));
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.IReportCreator#initializeReport(org.openelisglobal.common.action.BaseActionForm)
     */
    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        locationId = form.getLocationCode();
        dateRange = new DateRange(lowDateStr, highDateStr);
        reportLocation = getValidOrganization(locationId);

        errorFound = !validateSubmitParameters();

        if (errorFound) {
            return;
        }

        initializeReportItems();
        createReportItems();
        if (reportItems.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
        Collections.sort(reportItems, new ReportItemsComparator());
        return;
    }

    static class ReportItemsComparator implements Comparator<ClinicalPatientData> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         *      left.get().compareTo(right.get());
         */
        @Override
        public int compare(ClinicalPatientData left, ClinicalPatientData right) {
            int compare = left.getAccessionNumber().compareTo(right.getAccessionNumber());
            if (compare != 0) {
                return compare;
            }
            compare = left.getTestName().compareTo(right.getTestName());
            if (compare != 0) {
                return compare;
            }
            compare = left.getResult().compareTo(right.getResult());
            if (compare != 0) {
                return compare;
            }
            compare = left.getReferralTestName().compareTo(right.getReferralTestName());
            if (compare != 0) {
                return compare;
            }
            compare = left.getReferralResult().compareTo(right.getReferralResult());
            return compare;
        }
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return (dateRange.validateHighLowDate("report.error.message.date.received.missing") && reportLocation != null);
    }

    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    @Override
    protected byte[] renderReport() {
        List<String> nameLines = new ArrayList<>();
        for (String line : new String[] {
                configuredHeaderLine("report.labName.one",
                        ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName)),
                configuredHeaderLine("report.labName.two", "") }) {
            if (!GenericValidator.isBlankOrNull(line)) {
                nameLines.add(line);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize().rotate(), 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out);
        ReportHeaderPdf.add(document,
                MessageUtil.getMessage("report.test.status.referredOut") + ": " + reportLocation.getOrganizationName(),
                nameLines);
        Paragraph period = new Paragraph(
                MessageUtil.getMessage("reports.label.referral.title") + " " + lowDateStr + " - " + highDateStr,
                LABEL_FONT);
        period.setSpacingBefore(6);
        period.setSpacingAfter(8);
        document.add(period);

        PdfPTable table = new PdfPTable(new float[] { 136, 206, 233, 120, 64, 64 });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, MessageUtil.getMessage("report.orderNo"),
                MessageUtil.getMessage("report.test") + " / " + MessageUtil.getMessage("report.referredTest"),
                MessageUtil.getMessage("report.outcome") + " / " + MessageUtil.getMessage("report.referredResult"),
                MessageUtil.getMessage("report.reason"), MessageUtil.getMessage("referral.sent.date"),
                MessageUtil.getMessage("referral.report.date"));
        ClinicalPatientData above = null;
        for (ClinicalPatientData item : reportItems) {
            boolean newTest = above == null || !item.getAccessionNumber().equals(above.getAccessionNumber())
                    || !item.getTestName().equals(above.getTestName());
            if (newTest) {
                table.addCell(cell(item.getAccessionNumber(), LABEL_FONT));
                table.addCell(cell(item.getTestName(), LABEL_FONT));
                table.addCell(cell(withUnits(item.getResult(), item.getUom()), LABEL_FONT));
                PdfPCell reason = cell(item.getReferralReason(), CELL_FONT);
                reason.setColspan(3);
                table.addCell(reason);
            }
            table.addCell(cell(newTest ? MessageUtil.getMessage("report.reception") + ": "
                    + Objects.toString(item.getReceivedDate(), "") + "\n" + MessageUtil.getMessage("report.test") + ": "
                    + Objects.toString(item.getTestDate(), "") : "", CELL_FONT));
            table.addCell(cell(item.getReferralTestName(), CELL_FONT));
            table.addCell(cell(withUnits(item.getReferralResult(), item.getUom()), CELL_FONT));
            table.addCell(cell("", CELL_FONT));
            table.addCell(cell(item.getReferralSentDate(), CELL_FONT));
            table.addCell(cell(item.getReferralResultReportDate(), CELL_FONT));
            above = item;
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private static PdfPCell cell(String text, Font font) {
        return new PdfPCell(new Phrase(text == null ? "" : text, font));
    }

    /** The result followed by its units, or nothing when there is no result. */
    private static String withUnits(String result, String uom) {
        if (GenericValidator.isBlankOrNull(result)) {
            return "";
        }
        return uom == null ? result : result + "  " + uom;
    }

    /**
     * The header line this deployment configured, or {@code fallback} when it
     * configured none. Only site-suffixed variants of these keys ship, so a site
     * without one used to print the key itself across the top of the report.
     */
    private String configuredHeaderLine(String key, String fallback) {
        String configured = MessageUtil.getContextualMessage(key);
        return MessageUtil.messageNotFound(configured, key) ? fallback : configured;
    }

    /** This report prints the Test column as plain text. */
    @Override
    protected boolean escapesTestNameAsHtml() {
        return false;
    }

    @Override
    protected String getHeaderName() {
        return "GeneralHeader.jasper";
    }

    @Override
    protected void createReportItems() {
        List<Referral> referrals = referralService.getReferralsByOrganization(locationId, dateRange.getLowDate(),
                dateRange.getHighDateAtEndOfDay());

        for (Referral referral : referrals) {
            if (!referral.isCanceled()) {
                reportReferral(referral);
            }
        }
    }

    /**
     * Report the local and the referralResults for the given referral.
     *
     * @param referral
     */
    private void reportReferral(Referral referral) {
        currentAnalysis = referral.getAnalysis();
        Sample sample = referralService.getReferralById(referral.getId()).getAnalysis().getSampleItem().getSample();
        currentSample = sample;
        findPatientFromSample();

        String note = analysisService.getNotesAsString(currentAnalysis, false, true, "<br/>", false);
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        for (int i = 0; i < referralResults.size(); i++) {
            i = lastUsedReportReferralResultValue(referralResults, i);
            ReferralResult referralResult = referralResults.get(i);
            ClinicalPatientData data = buildClinicalPatientData(false);
            data.setReferralSentDate(
                    referral.getSentDate() != null ? DateUtil.formatDateAsText(referral.getSentDate()) : "");
            data.setReferralResult(reportReferralResultValue);
            data.setReferralNote(note);
            String testId = referralResult.getTestId();
            if (!GenericValidator.isBlankOrNull(testId)) {
                Test test = new Test();
                test.setId(testId);
                testService.getData(test);
                data.setReferralTestName(TestServiceImpl.getUserLocalizedReportingTestName(test));

                String uom = getUnitOfMeasure(test);
                if (reportReferralResultValue != null) {
                    data.setReferralResult(addIfNotEmpty(reportReferralResultValue, uom));
                }
                data.setReferralRefRange(addIfNotEmpty(getRange(referralResult.getResult()), uom));
                data.setTestSortOrder(GenericValidator.isBlankOrNull(test.getSortOrder()) ? Integer.MAX_VALUE
                        : Integer.parseInt(test.getSortOrder()));
                data.setSectionSortOrder(analysisService.getTestSection(currentAnalysis).getSortOrderInt());
                data.setTestSection(analysisService.getTestSection(currentAnalysis).getLocalizedName());
            }
            Timestamp referralReportDate = referralResult.getReferralReportDate();
            data.setReferralResultReportDate(
                    (referralReportDate == null) ? null : DateUtil.formatDateAsText(referralReportDate));
            ReferralReason reason = referralReasonService.get(referral.getReferralReasonId());
            data.setReferralReason(reason.getLocalizedName());

            reportItems.add(data);
        }
    }

    /**
     * @see PatientReport#getReportNameForParameterPage()
     */
    @Override
    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("openreports.referredOutHaitiReport");
    }

    @Override
    protected void postSampleBuild() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void setReferredResult(ClinicalPatientData data, Result result) {
        data.setResult(data.getResult());
    }
}
