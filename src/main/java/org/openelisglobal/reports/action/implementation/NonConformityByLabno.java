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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.ARVReportData;
import org.openelisglobal.reports.action.util.ReportUtil;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public abstract class NonConformityByLabno extends Report implements IReportCreator {

    protected ObservationHistoryTypeService observationTypeService = SpringContext
            .getBean(ObservationHistoryTypeService.class);
    private ObservationHistoryService observationService = SpringContext.getBean(ObservationHistoryService.class);
    private SampleService sampleService = SpringContext.getBean(SampleService.class);
    private SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);
    private SampleOrganizationService orgService = SpringContext.getBean(SampleOrganizationService.class);

    ObservationHistoryType observationType = observationTypeService.getByName("nameOfDoctor");
    private String OBSERVATION_DOCTOR_ID = observationType.getId();

    // private String lowDateStr;
    // private String highDateStr;
    private String lowerNumber;
    private String upperNumber;
    // private DateRange dateRange;

    private ArrayList<ARVReportData> reportItems;

    private Sample sample;
    // private Project project;
    // private String service;
    private Patient patient;
    // private QaEvent qaEvent;
    // private List<SampleQaEvent> sampleQaEvents;

    @Override
    protected void createReportParameters() throws IllegalStateException {
        super.createReportParameters();
        String nonConformity = MessageUtil.getContextualMessage("banner.menu.nonconformity");
        reportParameters.put("status", nonConformity);
        reportParameters.put("reportTitle", nonConformity);
        // reportParameters.put("reportPeriod",
        // MessageUtil.getContextualMessage("banner.menu.nonconformity") + " " +
        // dateRange.toString());
        // reportParameters.put("supervisorSignature",
        // ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SIGNATURES_ON_NONCONFORMITY_REPORTS,
        // "true"));
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "CI_GENERAL")) {
            reportParameters.put("headerName", "CILNSPHeader.jasper");
        } else {
            // reportParameters.put("headerName", getHeaderName());
        }
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        lowerNumber = form.getAccessionDirectNoSuffix();
        upperNumber = form.getHighAccessionDirectNoSuffix();
        // dateRange = new DateRange(lowDateStr, highDateStr);
        createReportParameters();
        errorFound = !validateAccessionNumbers();
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
        List<Sample> samples = sampleService.getSamplesByAccessionRange(lowerNumber, upperNumber);
        for (Sample sample : samples) {
            this.sample = sample;
            patient = ReportUtil.findPatient(sample);
            // project = ReportUtil.findProject(sample);
            // service = findService();
            // sampleQaEvents = findSampleQaEvents();

            ARVReportData data = new ARVReportData();

            String subjectNumber = patient.getNationalId();
            if (GenericValidator.isBlankOrNull(subjectNumber)) {
                subjectNumber = patient.getExternalId();
            }

            data.setLabNo(sample.getAccessionNumber());
            data.setSubjectNumber(subjectNumber);
            data.setBirth_date(patient.getBirthDateForDisplay());
            data.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), sample.getCollectionDate()));
            data.setGender(patient.getGender());
            data.setCollectiondate(sample.getCollectionDateForDisplay() + " " + sample.getCollectionTimeForDisplay());
            data.setReceptiondate(DateUtil.convertTimestampToStringDate(sample.getReceivedTimestamp()));

            SampleOrganization sampleOrg = new SampleOrganization();
            sampleOrg.setSample(sample);
            orgService.getDataBySample(sampleOrg);
            data.setOrgname(sampleOrg.getId() == null ? "" : sampleOrg.getOrganization().getOrganizationName());

            data.setDoctor(getObservationValues(OBSERVATION_DOCTOR_ID));

            data.getSampleQaEventItems(sample);
            reportItems.add(data);
            /*
             * for (SampleQaEvent sampleQaEvent : sampleQaEvents) { QAService qa = new
             * QAService( sampleQaEvent); this.qaEvent = qa.getQAEvent(); String sampleType
             * = ReportUtil.getSampleType(sampleQaEvent); String noteForSampleQaEvent =
             * NonConformityAction.getNoteForSampleQaEvent(sampleQaEvent); String
             * noteForSample = NonConformityAction.getNoteForSample(sample);
             *
             * ARVReportData data = new ARVReportData();
             * data.setLabNo(sample.getAccessionNumber());
             * data.setSubjectNumber(patient.getNationalId());
             * data.setSiteSubjectNumber(patient.getExternalId()); data.setStudy((project !=
             * null)?project.getLocalizedName():""); data.setService(service);
             * data.setReceivedDate(sample.getReceivedDateForDisplay() + " " +
             * sample.getReceivedTimeForDisplay( ));
             *
             * data.setNonConformityDate(DateUtil.convertTimestampToStringDate(
             * qa.getLastupdated())); data.setSection(qa.getObservationForDisplay(
             * QAObservationType.SECTION )); data.setNonConformityReason(
             * qaEvent.getLocalizedName() ); data.setSampleType( sampleType );
             * data.setBiologist( qa.getObservationForDisplay( QAObservationType.AUTHORIZER
             * ) ); data.setQaNote(noteForSampleQaEvent); data.setSampleNote(noteForSample);
             *
             *
             * data.getSampleQaEventItems(sample);
             *
             * reportItems.add(data); }
             */
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

    static class ReportItemsComparator implements Comparator<ARVReportData> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         *      left.get().compareTo(right.get());
         */
        @Override
        public int compare(ARVReportData left, ARVReportData right) {
            int compare = left.getLabNo().compareTo(right.getLabNo());
            if (compare != 0) {
                return compare;
            }
            /*
             * compare = StringUtil.compareWithNulls(left.getSubjectNumber(),
             * right.getSubjectNumber()); if (compare != 0) return compare; compare =
             * StringUtil.compareWithNulls(left.getSiteSubjectNumber(),
             * right.getSubjectNumber()); if (compare != 0) return compare; compare =
             * StringUtil.compareWithNulls(left.getSampleType(),right.getSampleType());
             */
            return compare;
        }
    }

    @Override
    protected String reportFileName() {
        return "retroCINonConformityByLabno";
    }

    protected abstract String getHeaderName();

    protected String getObservationValues(String observationTypeId) {
        List<ObservationHistory> observationList = observationService.getAll(patient, sample, observationTypeId);
        return observationList.size() > 0 ? observationList.get(0).getValue() : "";
    }

    private boolean validateAccessionNumbers() {

        if (GenericValidator.isBlankOrNull(lowerNumber) && GenericValidator.isBlankOrNull(upperNumber)) {
            add1LineErrorMessage("report.error.message.noParameters");
            return false;
        }

        if (GenericValidator.isBlankOrNull(lowerNumber)) {
            lowerNumber = upperNumber;
        } else if (GenericValidator.isBlankOrNull(upperNumber)) {
            upperNumber = lowerNumber;
        }

        int lowIndex = findFirstNumber(lowerNumber);
        int highIndex = findFirstNumber(upperNumber);

        if (lowIndex == lowerNumber.length() || highIndex == upperNumber.length()) {
            add1LineErrorMessage("report.error.message.noParameters");
            return false;
        }

        String lowPrefix = (String) lowerNumber.subSequence(0, lowIndex);
        String highPrefix = (String) upperNumber.subSequence(0, highIndex);

        if (!lowPrefix.equals(highPrefix)) {
            add1LineErrorMessage("report.error.message.samePrefix");
            return false;
        }

        double lowBounds = Double.parseDouble(lowerNumber.substring(lowIndex));
        double highBounds = Double.parseDouble(upperNumber.substring(highIndex));

        if (highBounds < lowBounds) {
            String temp = upperNumber;
            upperNumber = lowerNumber;
            lowerNumber = temp;
        }

        return true;
    }

    private int findFirstNumber(String number) {
        for (int i = 0; i < number.length(); i++) {
            if (Character.isDigit(number.charAt(i))) {
                return i;
            }
        }
        return number.length();
    }
}
