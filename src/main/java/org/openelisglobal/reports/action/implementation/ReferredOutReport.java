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

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
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

    @Override
    protected String reportFileName() {
        return "ReferredOutBySite";
    }

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

        createReportParameters();

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

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("reportPeriod",
                MessageUtil.getMessage("reports.label.referral.title") + " " + lowDateStr + " - " + highDateStr);
        reportParameters.put("reportTitle",
                reportLocation == null ? ""
                        : MessageUtil.getMessage("report.test.status.referredOut") + ": "
                                + reportLocation.getOrganizationName());
        reportParameters.put("referralSiteName", reportLocation == null ? "" : reportLocation.getOrganizationName());
        reportParameters.put("directorName",
                ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName));
        reportParameters.put("labName1", MessageUtil.getContextualMessage("report.labName.one"));
        reportParameters.put("labName2", MessageUtil.getContextualMessage("report.labName.two"));
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
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
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
