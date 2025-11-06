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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.reports.action.implementation.reportBeans.TestSegmentedExportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public class HaitiLNSPExportReport extends CSVExportReport {

    private DateRange dateRange;
    private String lowDateStr;
    private String highDateStr;
    private final SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    private final SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    private final AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private final ResultService resultService = SpringContext.getBean(ResultService.class);
    private final SampleService sampleService = SpringContext.getBean(SampleService.class);
    private final SampleRequesterService sampleRequesterService = SpringContext.getBean(SampleRequesterService.class);
    private final OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    private final RequesterTypeService requesterTypeService = SpringContext.getBean(RequesterTypeService.class);

    private final long ORGANIZTION_REFERRAL_TYPE_ID;
    protected List<TestSegmentedExportBean> testExportList;

    public HaitiLNSPExportReport() {
        String orgTypeId = requesterTypeService.getRequesterTypeByName("organization").getId();
        ORGANIZTION_REFERRAL_TYPE_ID = orgTypeId == null ? -1L : Long.parseLong(orgTypeId);
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();

        errorFound = false;

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        dateRange = new DateRange(lowDateStr, highDateStr);

        createReportParameters();

        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }

        createReportItems();
    }

    private void createReportItems() {
        testExportList = new ArrayList<>();
        List<Sample> orderList = sampleService.getSamplesReceivedInDateRange(lowDateStr, highDateStr);

        for (Sample order : orderList) {
            getResultsForOrder(order);
        }
    }

    private void getResultsForOrder(Sample order) {
        Patient patient = sampleHumanService.getPatientForSample(order);
        List<SampleRequester> requesterList = sampleRequesterService.getRequestersForSampleId(order.getId());
        Organization requesterOrganization = null;

        for (SampleRequester requester : requesterList) {
            if (requester.getRequesterTypeId() == ORGANIZTION_REFERRAL_TYPE_ID) {
                requesterOrganization = organizationService
                        .getOrganizationById(String.valueOf(requester.getRequesterId()));
                break;
            }
        }

        PatientService patientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());

        List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleId(order.getId());

        for (SampleItem sampleItem : sampleItemList) {
            getResultsForSampleItem(requesterOrganization, patientService, patient, sampleItem, order);
        }
    }

    private void getResultsForSampleItem(Organization requesterOrganization, PatientService patientService,
            Patient patient, SampleItem sampleItem, Sample order) {
        List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);

        for (Analysis analysis : analysisList) {
            getResultForAnalysis(requesterOrganization, patientService, patient, order, sampleItem, analysis);
        }
    }

    private void getResultForAnalysis(Organization requesterOrganization, PatientService patientService,
            Patient patient, Sample order, SampleItem sampleItem, Analysis analysis) {
        TestSegmentedExportBean ts = new TestSegmentedExportBean();

        ts.setAccessionNumber(order.getAccessionNumber());
        ts.setReceptionDate(order.getReceivedDateForDisplay());
        ts.setReceptionTime(DateUtil.convertTimestampToStringConfiguredHourTime(order.getReceivedTimestamp()));
        ts.setCollectionDate(DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()));
        ts.setCollectionTime(DateUtil.convertTimestampToStringConfiguredHourTime(sampleItem.getCollectionDate()));
        ts.setAge(createReadableAge(patientService.getDOB(patient)));
        ts.setDOB(patientService.getEnteredDOB(patient));
        ts.setFirstName(patientService.getFirstName(patient));
        ts.setLastName(patientService.getLastName(patient));
        ts.setGender(patientService.getGender(patient));
        ts.setNationalId(patientService.getNationalId(patient));
        ts.setStatus(SpringContext.getBean(IStatusService.class).getStatusName(
                SpringContext.getBean(IStatusService.class).getAnalysisStatusForID(analysis.getStatusId())));
        ts.setSampleType(sampleItem.getTypeOfSample().getLocalizedName());
        ts.setTestBench(analysis.getTestSection() == null ? "" : analysis.getTestSection().getTestSectionName());
        ts.setTestName(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
        ts.setDepartment(StringUtil
                .blankIfNull(patientService.getAddressComponents(patient).get(PatientServiceImpl.ADDRESS_DEPT)));
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String notes = noteService.getNotesAsString(analysis, false, false, "|", false);
        if (notes != null) {
            ts.setNotes(notes);
        }

        if (requesterOrganization != null) {
            ts.setSiteCode(requesterOrganization.getShortName());
            ts.setReferringSiteName(requesterOrganization.getOrganizationName());
        }

        if (SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)
                .equals(analysis.getStatusId())) {
            ts.setResultDate(DateUtil.convertSqlDateToStringDate(analysis.getCompletedDate()));

            List<Result> resultList = resultService.getResultsByAnalysis(analysis);
            if (!resultList.isEmpty()) {
                setAppropriateResults(resultList, analysis, ts);
            }
        }
        testExportList.add(ts);
    }

    @Override
    protected String reportFileName() {
        return "haitiLNSPExport";
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.Report#getContentType()
     */
    @Override
    public String getContentType() {
        if (errorFound) {
            return super.getContentType();
        } else {
            return "application/pdf; charset=UTF-8";
        }
    }

    @Override
    public byte[] runReport() {
        StringBuilder builder = new StringBuilder();
        builder.append(TestSegmentedExportBean.getHeader());
        builder.append("\n");

        for (TestSegmentedExportBean testLine : testExportList) {
            builder.append(testLine.getAsCSVString());
            builder.append("\n");
        }

        return builder.toString().getBytes();
    }

    @Override
    public String getResponseHeaderName() {
        return "Content-Disposition";
    }

    @Override
    public String getResponseHeaderContent() {
        return "attachment;filename=" + getReportFileName() + ".csv";
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing");
    }

    private String createReadableAge(Timestamp dob) {
        if (dob == null) {
            return "";
        }

        Date dobDate = DateUtil.convertTimestampToSqlDate(dob);
        int months = DateUtil.getAgeInMonths(dobDate, DateUtil.getNowAsSqlDate());
        if (months > 35) {
            return (months / 12) + " Ans";
        } else if (months > 0) {
            return months + " M";
        } else {
            int days = DateUtil.getAgeInDays(dobDate, DateUtil.getNowAsSqlDate());
            return days + " J";
        }
    }

    private void setAppropriateResults(List<Result> resultList, Analysis analysis, TestSegmentedExportBean data) {
        Result result = resultList.get(0);
        ResultService resultResultService = SpringContext.getBean(ResultService.class);
        String reportResult = resultResultService.getResultValue(result, true);
        Result quantifiableResult = analysisService.getQuantifiedResult(analysis);
        if (quantifiableResult != null) {
            reportResult += ":" + quantifiableResult.getValue();
        }

        data.setResult(reportResult.replace(",", ";"));
    }
}
