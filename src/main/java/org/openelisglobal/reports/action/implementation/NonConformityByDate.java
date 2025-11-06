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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.qaevent.service.NonConformityHelper;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openelisglobal.reports.action.util.ReportUtil;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public abstract class NonConformityByDate extends Report implements IReportCreator {
    private String lowDateStr;
    private String highDateStr;
    private DateRange dateRange;
    private ArrayList<NonConformityReportData> reportItems;

    private ObservationHistoryService observationService = SpringContext.getBean(ObservationHistoryService.class);
    private SampleService sampleService = SpringContext.getBean(SampleService.class);
    private SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);

    private Sample sample;
    private Project project;
    private String service;
    private Patient patient;
    private QaEvent qaEvent;
    private List<SampleQaEvent> sampleQaEvents;

    @Override
    protected void createReportParameters() throws IllegalStateException {
        super.createReportParameters();
        String nonConformity = MessageUtil.getContextualMessage("banner.menu.nonconformity");
        reportParameters.put("status", nonConformity);
        reportParameters.put("reportTitle", nonConformity);
        reportParameters.put("reportPeriod",
                MessageUtil.getContextualMessage("banner.menu.nonconformity") + "  " + dateRange.toString());
        reportParameters.put("supervisorSignature", ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.SIGNATURES_ON_NONCONFORMITY_REPORTS, "true"));
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "CI LNSP")) {
            reportParameters.put("headerName", "CILNSPHeader.jasper");
        } else {
            reportParameters.put("headerName", getHeaderName());
        }
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        dateRange = new DateRange(lowDateStr, highDateStr);

        createReportParameters();
        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }
        reportItems = new ArrayList<>();

        createReportItems();
        if (reportItems.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
        Collections.sort(reportItems, new ReportItemsComparator());
    }

    /** */
    private void createReportItems() {
        List<Sample> samples = sampleService.getSamplesReceivedInDateRange(lowDateStr, highDateStr);
        for (Sample sample : samples) {
            this.sample = sample;
            patient = ReportUtil.findPatient(sample);
            project = ReportUtil.findProject(sample);
            service = findService();
            sampleQaEvents = findSampleQaEvents();
            for (SampleQaEvent sampleQaEvent : sampleQaEvents) {
                QAService qa = new QAService(sampleQaEvent);
                qaEvent = qa.getQAEvent();
                String sampleType = ReportUtil.getSampleType(sampleQaEvent);
                String noteForSampleQaEvent = NonConformityHelper.getNoteForSampleQaEvent(sampleQaEvent);
                String noteForSample = NonConformityHelper.getNoteForSample(sample);

                NonConformityReportData data = new NonConformityReportData();
                if (AccessionFormat.ALPHANUM.toString()
                        .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
                    data.setAccessionNumber(
                            AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(sample.getAccessionNumber()));
                } else {
                    data.setAccessionNumber(sample.getAccessionNumber());
                }
                data.setSubjectNumber(patient.getNationalId());
                data.setSiteSubjectNumber(patient.getExternalId());
                data.setStudy((project != null) ? project.getLocalizedName() : "");
                data.setService(service);
                data.setReceivedDate(sample.getReceivedDateForDisplay() + " " + sample.getReceivedTimeForDisplay());

                data.setNonConformityDate(DateUtil.convertTimestampToStringDate(qa.getLastupdated()));
                data.setSection(qa.getObservationForDisplay(QAObservationType.SECTION));
                data.setNonConformityReason(qaEvent.getLocalizedName());
                data.setSampleType(sampleType);
                data.setBiologist(qa.getObservationForDisplay(QAObservationType.AUTHORIZER));
                data.setQaNote(noteForSampleQaEvent);
                data.setSampleNote(noteForSample);

                reportItems.add(data);
            }
        }
    }

    /**
     * @return a displayable string describing the service.
     */
    private String findService() {
        String service = "";
        List<ObservationHistory> oh = observationService.getAll(null, sample,
                TableIdService.getInstance().SERVICE_OBSERVATION_TYPE_ID);
        if (oh.size() > 0) {
            service = oh.get(0).getValue();
        }
        return service;
    }

    /**
     * @return
     */
    private List<SampleQaEvent> findSampleQaEvents() {
        SampleQaEvent sampleQaEvent = new SampleQaEvent();
        sampleQaEvent.setSample(sample);
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return (dateRange.validateHighLowDate("report.error.message.date.received.missing"));
    }

    static class ReportItemsComparator implements Comparator<NonConformityReportData> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         *      left.get().compareTo(right.get());
         */
        @Override
        public int compare(NonConformityReportData left, NonConformityReportData right) {
            int compare = left.getAccessionNumber().compareTo(right.getAccessionNumber());
            if (compare != 0) {
                return compare;
            }
            compare = StringUtil.compareWithNulls(left.getSubjectNumber(), right.getSubjectNumber());
            if (compare != 0) {
                return compare;
            }
            compare = StringUtil.compareWithNulls(left.getSiteSubjectNumber(), right.getSubjectNumber());
            if (compare != 0) {
                return compare;
            }
            compare = StringUtil.compareWithNulls(left.getSampleType(), right.getSampleType());
            return compare;
        }
    }

    @Override
    protected String reportFileName() {
        return "NonConformityByReceivedDate";
    }

    protected abstract String getHeaderName();
}
