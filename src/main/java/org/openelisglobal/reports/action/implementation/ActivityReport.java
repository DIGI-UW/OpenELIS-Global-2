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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.reports.action.implementation.reportBeans.ActivityReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openpdf.text.PageSize;

public abstract class ActivityReport extends Report implements IReportCreator {
    protected List<ActivityReportBean> testsResults;
    protected DateRange dateRange;

    @Override
    protected byte[] renderReport() {
        List<String> headers = Arrays.asList(MessageUtil.getContextualMessage("quick.entry.accession.number"),
                MessageUtil.getMessage("barcode.label.info.collectionDate"),
                MessageUtil.getMessage("report.receptionDate"),
                MessageUtil.getMessage("barcode.label.info.patientName"),
                isReportByTest() ? MessageUtil.getMessage("report.patientCode")
                        : MessageUtil.getMessage("report.patientCode") + " / "
                                + MessageUtil.getMessage("report.testName"),
                MessageUtil.getMessage("report.status"), MessageUtil.getMessage("report.results"),
                MessageUtil.getMessage("report.label.datetype.resultdate"),
                MessageUtil.getMessage("report.turnaround") + " (" + MessageUtil.getMessage("report.days") + ")",
                MessageUtil.getMessage("report.turnaround") + " (" + MessageUtil.getMessage("report.hours") + ")");
        List<List<String>> rows = new ArrayList<>();
        ActivityReportBean above = new ActivityReportBean();
        for (ActivityReportBean item : testsResults) {
            boolean firstOfOrder = !Objects.equals(item.getAccessionNumber(), above.getAccessionNumber());
            rows.add(Arrays.asList(item.getAccessionNumber(),
                    ManagementReportPdf.whenChanged(item.getCollectionDate(), above.getCollectionDate(), firstOfOrder),
                    ManagementReportPdf.whenChanged(item.getReceivedDate(), above.getReceivedDate(), firstOfOrder),
                    ManagementReportPdf.whenChanged(patientName(item), patientName(above), firstOfOrder),
                    item.getPatientOrTestName(), item.getSampleStatus(), item.getResultValue(), item.getResultDate(),
                    item.getTurnaroundDays(), item.getTurnaroundHours()));
            above = item;
        }
        return ManagementReportPdf.render(PageSize.A4.rotate(), MessageUtil.getMessage("report.activity"),
                getActivityLabel(), dateRange, headers,
                new float[] { 2.4f, 1.1f, 1.1f, 1.8f, 2.2f, 1.2f, 1.5f, 1.7f, 1.3f, 1.3f }, rows);
    }

    private static String patientName(ActivityReportBean item) {
        return GenericValidator.isBlankOrNull(item.getPatientLastName())
                || GenericValidator.isBlankOrNull(item.getPatientFirstName()) ? ""
                        : item.getPatientLastName() + ", " + item.getPatientFirstName();
    }

    protected boolean isReportByTest() {
        return false;
    }

    protected abstract String getActivityLabel();

    protected abstract void buildReportContent(ReportSpecificationList testSelection);

    @Override
    public void initializeReport(ReportForm form) {
        initialized = true;
        ReportSpecificationList selection = form.getSelectList();
        dateRange = new DateRange(form.getLowerDateRange(), form.getUpperDateRange());

        errorFound = !validateSubmitParameters(selection);
        if (errorFound) {
            return;
        }

        buildReportContent(selection);
        if (testsResults.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
    }

    private boolean validateSubmitParameters(ReportSpecificationList selectList) {

        return (dateRange.validateHighLowDate("report.error.message.date.received.missing")
                && validateSelection(selectList));
    }

    private boolean validateSelection(ReportSpecificationList selectList) {
        boolean complete = !GenericValidator.isBlankOrNull(selectList.getSelection())
                && !"0".equals(selectList.getSelection());

        if (!complete) {
            add1LineErrorMessage("report.error.message.activity.missing");
        }

        return complete;
    }

    protected ActivityReportBean createActivityReportBean(Result result, boolean useTestName) {
        ActivityReportBean item = new ActivityReportBean();

        ResultService resultService = SpringContext.getBean(ResultService.class);
        SampleService sampleService = SpringContext.getBean(SampleService.class);
        Sample sample = result.getAnalysis().getSampleItem().getSample();
        PatientService patientService = SpringContext.getBean(PatientService.class);
        SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
        Patient patient = sampleHumanService.getPatientForSample(sample);
        Person person = patient == null ? null : patient.getPerson();
        item.setResultValue(resultService.getResultValueForDisplay(result, "\n", true, true));
        item.setSampleStatus(activityStatus(sampleService.getSampleStatusForDisplay(sample), result));
        item.setTechnician(resultService.getSignature(result));

        // item.setAccessionNumber(sampleService.getAccessionNumber(sample).substring(PREFIX_LENGTH));
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            item.setAccessionNumber(AlphanumAccessionValidator
                    .convertAlphaNumLabNumForDisplay(sampleService.getAccessionNumber(sample)));
        } else {
            item.setAccessionNumber(sampleService.getAccessionNumber(sample));
        }

        item.setReceivedDate(sampleService.getReceivedDateWithTwoYearDisplay(sample));
        Timestamp start = sample.getReceivedTimestamp();
        Timestamp stop = result.getLastupdated();
        float diff = stop.getTime() - start.getTime();
        float diffSeconds = diff / 1000;
        float diffMinutes = diff / (60 * 1000);
        float diffHours = diff / (60 * 60 * 1000);
        float diffDays = diffHours / 24;
        DecimalFormat df = new DecimalFormat("0.00");

        item.setTurnaroundHours(df.format(diffHours));
        item.setTurnaroundDays(df.format(diffDays));

        item.setResultDate(DateUtil.convertTimestampToStringDateAndTime(result.getLastupdated()));
        item.setCollectionDate(
                DateUtil.convertTimestampToTwoYearStringDate(result.getAnalysis().getSampleItem().getCollectionDate()));

        item.setPatientLastName(person == null || person.getLastName() == null ? "" : person.getLastName());
        item.setPatientFirstName(person == null || person.getFirstName() == null ? "" : person.getFirstName());
        item.setPatientId(patient == null || patient.getStringId() == null ? "" : patient.getStringId());

        List<String> values = new ArrayList<>();
        // values.add(
        // patientService.getLastName(patient) == null ? "" :
        // patientService.getLastName(patient).toUpperCase());
        values.add(patientService.getNationalId(patient));

        String referringPatientId = SpringContext.getBean(ObservationHistoryService.class)
                .getValueForSample(ObservationType.REFERRERS_PATIENT_ID, sample.getId());
        values.add(referringPatientId == null ? "" : referringPatientId);

        String name = StringUtil.buildDelimitedStringFromList(values, " / ", true);

        if (useTestName) {
            item.setPatientOrTestName(
                    resultService.getReportingTestName(result) != null ? resultService.getReportingTestName(result)
                            : "");
            item.setNonPrintingPatient(name);
        } else {
            item.setPatientOrTestName(name);
        }

        return item;
    }

    /**
     * A test sent to a reference laboratory still appears here, because the
     * activity listing is a record of what happened to the sample, but its status
     * says so rather than reading as work this laboratory finished. The column is
     * narrow, so this replaces the sample status instead of qualifying it.
     */
    private String activityStatus(String sampleStatus, Result result) {
        if (result.getAnalysis() == null || !result.getAnalysis().isReferredOut()) {
            return sampleStatus;
        }
        return MessageUtil.getMessage("report.activity.referredOut");
    }

    protected ActivityReportBean createIdentityActivityBean(ActivityReportBean item, boolean blankCollectionDate) {
        ActivityReportBean filler = new ActivityReportBean();

        filler.setAccessionNumber(item.getAccessionNumber());
        filler.setReceivedDate(item.getReceivedDate());
        filler.setCollectionDate(blankCollectionDate ? " " : item.getCollectionDate());
        filler.setPatientOrTestName(item.getNonPrintingPatient());

        return filler;
    }
}
