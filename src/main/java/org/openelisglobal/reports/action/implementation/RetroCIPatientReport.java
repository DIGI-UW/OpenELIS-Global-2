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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;

public abstract class RetroCIPatientReport extends RetroCIReport {

    protected static String ANALYSIS_FINALIZED_STATUS_ID;

    protected static List<Integer> READY_FOR_REPORT_STATUS_IDS;
    protected Patient reportPatient;
    protected Sample reportSample;

    private static ObservationHistoryService observationService = SpringContext
            .getBean(ObservationHistoryService.class);
    private SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    private SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected PatientService patientService = SpringContext.getBean(PatientService.class);

    private String lowerNumber;
    private String upperNumber;
    private List<String> handledOrders;

    static {
        READY_FOR_REPORT_STATUS_IDS = new ArrayList<>();
        READY_FOR_REPORT_STATUS_IDS
                .add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished)));
        READY_FOR_REPORT_STATUS_IDS
                .add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Started)));

        ANALYSIS_FINALIZED_STATUS_ID = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.Finalized);
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowerNumber = form.getAccessionDirectNoSuffix();
        upperNumber = form.getHighAccessionDirectNoSuffix();

        handledOrders = new ArrayList<>();

        createReportParameters();

        boolean valid;
        List<Sample> reportSampleList = new ArrayList<>();

        if (form.getAnalysisIds() != null && form.getAnalysisIds().size() > 0) {
            reportSampleList = findReportSamples(form.getAnalysisIds());
        } else if (!GenericValidator.isBlankOrNull(lowerNumber) || !GenericValidator.isBlankOrNull(upperNumber)) {
            valid = validateAccessionNumbers();
            if (valid) {
                reportSampleList = findReportSamples(lowerNumber, upperNumber);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getSelPatient())) {
            List<Patient> patientList = new ArrayList<>();
            valid = findPatientById(form.getSelPatient(), patientList);
            if (valid) {
                reportSampleList = findReportSamplesForReportPatient(patientList);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getPatientNumberDirect())) {
            List<Patient> patientList = new ArrayList<>();
            valid = findPatientByPatientNumber(form.getPatientNumberDirect(), patientList);

            if (valid) {
                reportSampleList = findReportSamplesForReportPatient(patientList);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getReferringSiteId())) {
            if (GenericValidator.isBlankOrNull(form.getUpperDateRange())
                    && !GenericValidator.isBlankOrNull(form.getLowerDateRange())) {
                form.setUpperDateRange(form.getLowerDateRange());
            }
            if (!GenericValidator.isBlankOrNull(form.getUpperDateRange())
                    && !GenericValidator.isBlankOrNull(form.getLowerDateRange())) {
                reportSampleList = findReportSamplesForSite(form.getReferringSiteId(),
                        form.getReferringSiteDepartmentId(), form.isOnlyResults(), form.getDateType(),
                        form.getLowerDateRange(), form.getUpperDateRange());
            }
        }

        initializeReportItems();

        if (reportSampleList.isEmpty()) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        } else {
            Collections.sort(reportSampleList, new Comparator<Sample>() {
                @Override
                public int compare(Sample o1, Sample o2) {
                    return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
                }
            });
            for (Sample sample : reportSampleList) {
                handledOrders.add(sample.getId());
                reportSample = sample;
                findPatientFromSample();
                if (allowSample()) {
                    createReportItems();
                }
            }
        }
    }

    private List<Sample> findReportSamplesForSite(String referringSiteId, String referringSiteDepartmentId,
            boolean onlyResults, DateType dateType, String lowerDateRange, String upperDateRange) {
        List<Sample> sampleList = new ArrayList<>();
        String sampleRequesterOrgId = GenericValidator.isBlankOrNull(referringSiteDepartmentId) ? referringSiteId
                : referringSiteDepartmentId;

        if (DateType.ORDER_DATE.equals(dateType)) {
            sampleList = sampleService.getStudySamplesForSiteBetweenOrderDates(sampleRequesterOrgId,
                    DateUtil.convertStringDateToLocalDate(lowerDateRange),
                    DateUtil.convertStringDateToLocalDate(upperDateRange));
        } else {
            List<Analysis> analysises = analysisService.getStudyAnalysisForSiteBetweenResultDates(sampleRequesterOrgId,
                    DateUtil.convertStringDateToLocalDate(lowerDateRange),
                    DateUtil.convertStringDateToLocalDate(upperDateRange));
            sampleList = sampleService
                    .getSamplesByAnalysisIds(analysises.stream().map(e -> e.getId()).collect(Collectors.toList()));
        }

        if (onlyResults) {
            Set<Integer> analysisStatusIds = new HashSet<>();
            analysisStatusIds.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
            analysisStatusIds.add(Integer
                    .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
            analysisStatusIds.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
            analysisStatusIds.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
            sampleList = sampleList.stream().filter(
                    e -> (analysisService.getAnalysesBySampleIdAndStatusId(e.getId(), analysisStatusIds).size() > 0))
                    .collect(Collectors.toList());
        }

        return sampleList;
    }

    private boolean findPatientById(String patientId, List<Patient> patientList) {
        patientList.add(patientService.get(patientId));
        return !patientList.isEmpty();
    }

    private boolean findPatientByPatientNumber(String patientNumber, List<Patient> patientList) {
        PatientIdentityService patientIdentityService = SpringContext.getBean(PatientIdentityService.class);
        patientList.addAll(patientService.getPatientsByNationalId(patientNumber));

        if (patientList.isEmpty()) {
            List<PatientIdentity> identities = patientIdentityService.getPatientIdentitiesByValueAndType(patientNumber,
                    PatientServiceImpl.getPatientSTIdentity());

            if (identities.isEmpty()) {
                identities = patientIdentityService.getPatientIdentitiesByValueAndType(patientNumber,
                        PatientServiceImpl.getPatientSubjectIdentity());
            }

            if (!identities.isEmpty()) {

                for (PatientIdentity patientIdentity : identities) {
                    String reportPatientId = patientIdentity.getPatientId();
                    Patient patient = new Patient();
                    patient.setId(reportPatientId);
                    patientService.getData(patient);
                    patientList.add(patient);
                }
            }
        }

        return !patientList.isEmpty();
    }

    private List<Sample> findReportSamplesForReportPatient(List<Patient> patientList) {
        List<Sample> sampleList = new ArrayList<>();
        for (Patient searchPatient : patientList) {
            sampleList.addAll(sampleHumanService.getSamplesForPatient(searchPatient.getId()));
        }

        return sampleList;
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

    /*
     * Until the ARV study has a initial and followup project we have to let each
     * study figure out which one the patient is in
     */
    protected boolean allowSample() {
        return true;
    }

    private List<Sample> findReportSamples(String lowerNumber, String upperNumber) {
        return sampleService.getSamplesByProjectAndStatusIDAndAccessionRange(getProjIdsList(getProjectId()),
                READY_FOR_REPORT_STATUS_IDS, lowerNumber, upperNumber);
    }

    private List<Sample> findReportSamples(List<String> analysisIds) {
        List<Sample> sampleList = sampleService.getSamplesByAnalysisIds(analysisIds);
        return sampleList == null ? new ArrayList<>() : sampleList;
    }

    protected abstract String getProjectId();

    protected abstract void initializeReportItems();

    protected abstract void createReportItems();

    protected void findPatientFromSample() {
        reportPatient = sampleHumanService.getPatientForSample(reportSample);
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("studyName", getReportNameForReport());
    }

    protected abstract String getReportNameForReport();

    private int findFirstNumber(String number) {
        for (int i = 0; i < number.length(); i++) {
            if (Character.isDigit(number.charAt(i))) {
                return i;
            }
        }
        return number.length();
    }

    protected String getObservationValues(String observationTypeId) {
        List<ObservationHistory> observationList = observationService.getAll(reportPatient, reportSample,
                observationTypeId);
        return observationList.size() > 0 ? observationList.get(0).getValue() : "";
    }

    @Override
    public List<String> getReportedOrders() {
        return handledOrders;
    }

    protected List<Integer> getProjIdsList(String projID) {

        String[] fields = projID.split(":");
        List<Integer> projIDList = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            projIDList.add(Integer.parseInt(fields[i]));
        }
        return projIDList;
    }
}
